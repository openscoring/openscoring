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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.typesafe.config.Config;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.HasPMML;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.VisitorBattery;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.SAXException;

@Service
@Singleton
public class ModelRegistry {

	private LoadingModelEvaluatorBuilder modelEvaluatorBuilder = null;

	private ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();


	@Inject
	public ModelRegistry(@Named("openscoring") Config config){
		Config modelRegistryConfig = config.getConfig("modelRegistry");

		LoadingModelEvaluatorBuilder modelEvaluatorBuilder = new LoadingModelEvaluatorBuilder();

		boolean validate = modelRegistryConfig.getBoolean("validate");

		if(validate){
			Schema schema;

			try {
				schema = JAXBUtil.getSchema();
			} catch(SAXException | IOException e){
				throw new RuntimeException(e);
			}

			modelEvaluatorBuilder
				.setSchema(schema)
				.setValidationEventHandler(new SimpleValidationEventHandler());
		}

		VisitorBattery visitors = new VisitorBattery();

		List<String> visitorClassNames = modelRegistryConfig.getStringList("visitorClasses");
		for(String visitorClassName : visitorClassNames){
			Class<?> clazz = loadClass(Object.class, visitorClassName);

			if((Visitor.class).isAssignableFrom(clazz)){
				Class<? extends Visitor> visitorClazz = clazz.asSubclass(Visitor.class);

				visitors.add(visitorClazz);
			} else

			if((VisitorBattery.class).isAssignableFrom(clazz)){
				Class<? extends VisitorBattery> visitorBatteryClazz = clazz.asSubclass(VisitorBattery.class);

				VisitorBattery visitorBattery = newInstance(visitorBatteryClazz);

				visitors.addAll(visitorBattery);
			} else

			{
				throw new IllegalArgumentException(new ClassCastException(clazz.toString()));
			}
		}

		modelEvaluatorBuilder.setVisitors(visitors);

		String modelEvaluatorFactoryClassName = modelRegistryConfig.getString("modelEvaluatorFactoryClass");
		if(modelEvaluatorFactoryClassName != null){
			Class<? extends ModelEvaluatorFactory> modelEvaluatorFactoryClazz = loadClass(ModelEvaluatorFactory.class, modelEvaluatorFactoryClassName);

			modelEvaluatorBuilder.setModelEvaluatorFactory(newInstance(modelEvaluatorFactoryClazz));
		}

		String valueFactoryFactoryClassName = modelRegistryConfig.getString("valueFactoryFactoryClass");
		if(valueFactoryFactoryClassName != null){
			Class<? extends ValueFactoryFactory> valueFactoryFactoryClazz = loadClass(ValueFactoryFactory.class, valueFactoryFactoryClassName);

			modelEvaluatorBuilder.setValueFactoryFactory(newInstance(valueFactoryFactoryClazz));
		}

		Function<FieldName, FieldName> resultMapper = new Function<FieldName, FieldName>(){

			@Override
			public FieldName apply(FieldName name){

				// A "phantom" default target field
				if(name == null){
					return ModelResource.DEFAULT_NAME;
				}

				return name;
			}
		};

		modelEvaluatorBuilder.setResultMapper(resultMapper);

		this.modelEvaluatorBuilder = modelEvaluatorBuilder;
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

		Evaluator evaluator = this.modelEvaluatorBuilder.clone()
			.load(hashingIs)
			.build();

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

			Result result = new StreamResult(os);

			Marshaller marshaller = JAXBUtil.createMarshaller();

			marshaller.marshal(pmml, result);
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
		Model oldModel = this.models.putIfAbsent(id, Objects.requireNonNull(model));

		return (oldModel == null);
	}

	public boolean replace(String id, Model oldModel, Model model){
		return this.models.replace(id, oldModel, Objects.requireNonNull(model));
	}

	public boolean remove(String id, Model model){
		return this.models.remove(id, model);
	}

	static
	public boolean validateId(String id){
		return (id != null && (id).matches(ID_REGEX));
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