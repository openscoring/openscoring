/*
 * Copyright (c) 2014 Villu Ruusmann
 *
 * This file is part of Openscoring
 *
 * Openscoring is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Openscoring is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Openscoring.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openscoring.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.typesafe.config.Config;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

@Service
@Singleton
public class ModelRegistry {

	private List<Class<? extends Visitor>> visitorClazzes = new ArrayList<>();

	private ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();


	@Inject
	public ModelRegistry(@Named("openscoring") Config config){
		Config modelRegistryConfig = config.getConfig("modelRegistry");

		List<String> visitorClassNames = modelRegistryConfig.getStringList("visitorClasses");
		for(String visitorClassName : visitorClassNames){
			Class<?> clazz;

			try {
				clazz = Class.forName(visitorClassName);
			} catch(ClassNotFoundException cnfe){
				throw new IllegalArgumentException(cnfe);
			}

			Class<? extends Visitor> visitorClazz;

			try {
				visitorClazz = clazz.asSubclass(Visitor.class);
			} catch(ClassCastException cce){
				throw new IllegalArgumentException(cce);
			}

			this.visitorClazzes.add(visitorClazz);
		}
	}

	public Collection<Map.Entry<String, Model>> entries(){
		return this.models.entrySet();
	}

	@SuppressWarnings (
		value = {"resource"}
	)
	public Model load(InputStream is) throws Exception {
		CountingInputStream countingIs = new CountingInputStream(is);

		HashingInputStream hashingIs = new HashingInputStream(Hashing.md5(), countingIs);

		ModelEvaluator<?> evaluator = unmarshal(hashingIs);

		PMML pmml = evaluator.getPMML();

		for(Class<? extends Visitor> visitorClazz : this.visitorClazzes){
			Visitor visitor = visitorClazz.newInstance();

			visitor.applyTo(pmml);
		}

		evaluator.verify();

		Model model = new Model(evaluator);
		model.putProperty(Model.PROPERTY_FILE_SIZE, countingIs.getCount());
		model.putProperty(Model.PROPERTY_FILE_MD5SUM, (hashingIs.hash()).toString());

		return model;
	}

	public void store(Model model, OutputStream os) throws JAXBException {
		ModelEvaluator<?> evaluator = model.getEvaluator();

		marshal(evaluator, os);
	}

	public Model get(String id){
		return get(id, false);
	}

	public Model get(String id, boolean touch){
		Model model = this.models.get(id);

		if(model != null && touch){
			model.putProperty(Model.PROPERTY_ACCESSED_TIMESTAMP, new Date());
		}

		return model;
	}

	public boolean put(String id, Model model){
		Model oldModel = this.models.putIfAbsent(id, Preconditions.checkNotNull(model));

		return (oldModel == null);
	}

	public boolean replace(String id, Model oldModel, Model model){
		return this.models.replace(id, oldModel, Preconditions.checkNotNull(model));
	}

	public boolean remove(String id, Model model){
		return this.models.remove(id, model);
	}

	static
	public boolean validateId(String id){
		return (id != null && (id).matches(ID_REGEX));
	}

	static
	private ModelEvaluator<?> unmarshal(InputStream is) throws SAXException, JAXBException {
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

		ImportFilter filter = new ImportFilter(reader);

		Source source = new SAXSource(filter, new InputSource(is));

		PMML pmml = JAXBUtil.unmarshalPMML(source);

		PMMLManager pmmlManager = new PMMLManager(pmml);

		return (ModelEvaluator<?>)pmmlManager.getModelManager(ModelEvaluatorFactory.getInstance());
	}

	static
	private void marshal(ModelEvaluator<?> evaluator, OutputStream os) throws JAXBException {
		PMML pmml = evaluator.getPMML();

		Result result = new StreamResult(os);

		JAXBUtil.marshalPMML(pmml, result);
	}

	public static final String ID_REGEX = "[a-zA-Z0-9][a-zA-Z0-9\\_\\-]*";
}