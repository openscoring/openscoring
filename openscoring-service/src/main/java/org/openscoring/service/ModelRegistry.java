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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import jersey.repackaged.com.google.common.collect.Lists;
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

@Service
@Singleton
public class ModelRegistry {

	private List<Class<? extends Visitor>> visitorClazzes = Lists.newArrayList();

	private ConcurrentMap<String, ModelEvaluator<?>> models = Maps.newConcurrentMap();


	@Inject
	public ModelRegistry(@Named("openscoring") Config config){
		List<String> visitorClassNames = config.getStringList("modelregistry.visitorClasses");
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

	public Collection<Map.Entry<String, ModelEvaluator<?>>> entries(){
		return this.models.entrySet();
	}

	public ModelEvaluator<?> load(InputStream is) throws Exception {
		ModelEvaluator<?> evaluator = unmarshal(is);

		PMML pmml = evaluator.getPMML();

		for(Class<? extends Visitor> visitorClazz : this.visitorClazzes){
			Visitor visitor = visitorClazz.newInstance();

			pmml.accept(visitor);
		}

		evaluator.verify();

		return evaluator;
	}

	public void store(ModelEvaluator<?> evaluator, OutputStream os) throws Exception {
		marshal(evaluator, os);
	}

	public ModelEvaluator<?> get(String id){
		return this.models.get(id);
	}

	public boolean put(String id, ModelEvaluator<?> evaluator){
		ModelEvaluator<?> oldEvaluator = this.models.putIfAbsent(id, Preconditions.checkNotNull(evaluator));

		return (oldEvaluator == null);
	}

	public boolean replace(String id, ModelEvaluator<?> oldEvaluator, ModelEvaluator<?> evaluator){
		return this.models.replace(id, oldEvaluator, Preconditions.checkNotNull(evaluator));
	}

	public boolean remove(String id, ModelEvaluator<?> evaluator){
		return this.models.remove(id, evaluator);
	}

	static
	public boolean validateId(String id){
		return (id != null && (id).matches(ID_REGEX));
	}

	static
	private ModelEvaluator<?> unmarshal(InputStream is) throws SAXException, JAXBException {
		Source source = ImportFilter.apply(new InputSource(is));

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