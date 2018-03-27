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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
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
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.typesafe.config.Config;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.HasPMML;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.VisitorBattery;
import org.jpmml.model.filters.ImportFilter;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

@Service
@Singleton
public class ModelRegistry {

	private ModelEvaluatorFactory modelEvaluatorFactory = null;

	private VisitorBattery visitorBattery = new VisitorBattery();

	private boolean validate = false;

	private ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();


	@Inject
	public ModelRegistry(@Named("openscoring") Config config){
		Config modelRegistryConfig = config.getConfig("modelRegistry");

		String modelEvaluatorFactoryClassName = modelRegistryConfig.getString("modelEvaluatorFactoryClass");
		if(modelEvaluatorFactoryClassName != null){
			Class<? extends ModelEvaluatorFactory> modelEvaluatorFactoryClazz = loadClass(ModelEvaluatorFactory.class, modelEvaluatorFactoryClassName);

			ModelEvaluatorFactory modelEvaluatorFactory = newInstance(modelEvaluatorFactoryClazz);

			this.modelEvaluatorFactory = modelEvaluatorFactory;
		} else

		{
			this.modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
		}

		String valueFactoryFactoryClassName = modelRegistryConfig.getString("valueFactoryFactoryClass");
		if(valueFactoryFactoryClassName != null){
			Class<? extends ValueFactoryFactory> valueFactoryFactoryClazz = loadClass(ValueFactoryFactory.class, valueFactoryFactoryClassName);

			ValueFactoryFactory valueFactoryFactory = newInstance(valueFactoryFactoryClazz);

			this.modelEvaluatorFactory.setValueFactoryFactory(valueFactoryFactory);
		}

		List<String> visitorClassNames = modelRegistryConfig.getStringList("visitorClasses");
		for(String visitorClassName : visitorClassNames){
			Class<? extends Visitor> visitorClazz = loadClass(Visitor.class, visitorClassName);

			this.visitorBattery.add(visitorClazz);
		}

		this.validate = modelRegistryConfig.getBoolean("validate");
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

		PMML pmml = unmarshal(hashingIs, this.validate);

		this.visitorBattery.applyTo(pmml);

		ModelEvaluatorFactory modelEvaluatorFactory = this.modelEvaluatorFactory;

		Evaluator evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);

		evaluator.verify();

		Model model = new Model(evaluator);
		model.putProperty(Model.PROPERTY_FILE_SIZE, countingIs.getCount());
		model.putProperty(Model.PROPERTY_FILE_MD5SUM, (hashingIs.hash()).toString());

		return model;
	}

	public void store(Model model, OutputStream os) throws JAXBException {
		Evaluator evaluator = model.getEvaluator();

		if(evaluator instanceof HasPMML){
			HasPMML hasPMML = (HasPMML)evaluator;

			PMML pmml = hasPMML.getPMML();

			marshal(pmml, os);
		}
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
	private PMML unmarshal(InputStream is, boolean validate) throws IOException, SAXException, JAXBException {
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

		ImportFilter filter = new ImportFilter(reader);

		Source source = new SAXSource(filter, new InputSource(is));

		Unmarshaller unmarshaller = JAXBUtil.createUnmarshaller();
		unmarshaller.setEventHandler(new SimpleValidationEventHandler());

		if(validate){
			Schema schema = JAXBUtil.getSchema();

			unmarshaller.setSchema(schema);
		}

		return (PMML)unmarshaller.unmarshal(source);
	}

	static
	private void marshal(PMML pmml, OutputStream os) throws JAXBException {
		Result result = new StreamResult(os);

		Marshaller marshaller = JAXBUtil.createMarshaller();

		marshaller.marshal(pmml, result);
	}

	static
	private <E> Class<? extends E> loadClass(Class<? extends E> superClazz, String name){

		try {
			Class<?> clazz = Class.forName(name);

			return clazz.asSubclass(superClazz);
		} catch(ClassCastException cce){
			throw new IllegalArgumentException(cce);
		} catch(ClassNotFoundException cnfe){
			throw new IllegalArgumentException(cnfe);
		}
	}

	static
	private <E> E newInstance(Class<? extends E> clazz){

		try {
			try {
				Method method = clazz.getDeclaredMethod("newInstance");

				Object result = method.invoke(null);

				return clazz.cast(result);
			} catch(NoSuchMethodException nsme){
				return clazz.newInstance();
			}
		} catch(ReflectiveOperationException roe){
			throw new IllegalArgumentException(roe);
		}
	}

	static
	private class SimpleValidationEventHandler implements ValidationEventHandler {

		@Override
		public boolean handleEvent(ValidationEvent event){
			int severity = event.getSeverity();

			switch(severity){
				case ValidationEvent.ERROR:
				case ValidationEvent.FATAL_ERROR:
					return false;
				default:
					return true;
			}
		}
	}

	public static final String ID_REGEX = "[a-zA-Z0-9][a-zA-Z0-9\\_\\-]*";
}