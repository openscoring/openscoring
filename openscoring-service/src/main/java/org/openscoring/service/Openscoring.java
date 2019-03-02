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
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Visitor;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputFilters;
import org.jpmml.evaluator.ResultMapper;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.VisitorBattery;
import org.openscoring.common.ModelResponse;
import org.openscoring.service.filters.ApplicationHeaderFilter;
import org.openscoring.service.providers.ModelProvider;
import org.openscoring.service.providers.ModelRefConverterProvider;
import org.openscoring.service.providers.ModelRefProvider;
import org.openscoring.service.providers.TableProvider;
import org.xml.sax.SAXException;

public class Openscoring extends ResourceConfig {

	private Config config = null;


	public Openscoring(){
		Config config = ConfigFactory.load();

		setConfig(config);

		Binder configBinder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(config).to(Config.class).named("openscoring");
			}
		};
		register(configBinder);

		ModelRegistry modelRegistry = new ModelRegistry();

		Binder modelRegistryBinder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(modelRegistry).to(ModelRegistry.class).named("openscoring");
			}
		};
		register(modelRegistryBinder);

		register(ModelRefProvider.class);
		register(ModelRefConverterProvider.class);

		register(ModelResource.class);

		LoadingModelEvaluatorBuilder loadingModelEvaluatorBuilder = createLoadingModelEvaluatorBuilder(config);

		Binder loadingModelEvaluatorBuilderBinder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(loadingModelEvaluatorBuilder).to(LoadingModelEvaluatorBuilder.class);
			}
		};
		register(loadingModelEvaluatorBuilderBinder);

		// PMML support
		register(ModelProvider.class);

		// JSON support
		register(JacksonJsonProvider.class);
		register(ObjectMapperProvider.class);

		// CSV support
		register(TableProvider.class);

		// Convert exceptions to JSON objects
		register(WebApplicationExceptionMapper.class);

		// Permit the HTTP POST method to be changed to HTTP PUT or DELETE methods
		register(HttpMethodOverrideFilter.class);

		// File upload support
		register(MultiPartFeature.class);

		// Security support
		register(RolesAllowedDynamicFeature.class);

		// GZip and Deflate encoding support
		register(EncodingFilter.class);
		register(GZipEncoder.class);
		register(DeflateEncoder.class);

		// Application identification
		register(ApplicationHeaderFilter.class);

		Config applicationConfig = config.getConfig("application");

		List<String> componentClassNames = applicationConfig.getStringList("componentClasses");
		for(String componentClassName : componentClassNames){
			Class<?> clazz = loadClass(Object.class, componentClassName);

			register(clazz);
		}
	}

	public Config getConfig(){
		return this.config;
	}

	private void setConfig(Config config){
		this.config = config;
	}

	static
	private LoadingModelEvaluatorBuilder createLoadingModelEvaluatorBuilder(Config config){
		Config modelEvaluatorBuilderConfig = config.getConfig("modelEvaluatorBuilder");

		LoadingModelEvaluatorBuilder modelEvaluatorBuilder = new LoadingModelEvaluatorBuilder();

		String modelEvaluatorFactoryClassName = modelEvaluatorBuilderConfig.getString("modelEvaluatorFactoryClass");
		if(modelEvaluatorFactoryClassName != null){
			Class<? extends ModelEvaluatorFactory> modelEvaluatorFactoryClazz = loadClass(ModelEvaluatorFactory.class, modelEvaluatorFactoryClassName);

			modelEvaluatorBuilder.setModelEvaluatorFactory(newInstance(modelEvaluatorFactoryClazz));
		}

		String valueFactoryFactoryClassName = modelEvaluatorBuilderConfig.getString("valueFactoryFactoryClass");
		if(valueFactoryFactoryClassName != null){
			Class<? extends ValueFactoryFactory> valueFactoryFactoryClazz = loadClass(ValueFactoryFactory.class, valueFactoryFactoryClassName);

			modelEvaluatorBuilder.setValueFactoryFactory(newInstance(valueFactoryFactoryClazz));
		}

		modelEvaluatorBuilder.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS);

		// Jackson does not support the JSON serialization of <code>null</code> map keys
		ResultMapper resultMapper = new ResultMapper(){

			private FieldName defaultTargetName = FieldName.create(ModelResponse.DEFAULT_TARGET_NAME);


			@Override
			public FieldName apply(FieldName name){

				// A "phantom" default target field
				if(name == null){
					return this.defaultTargetName;
				}

				return name;
			}
		};

		modelEvaluatorBuilder.setResultMapper(resultMapper);

		boolean validate = modelEvaluatorBuilderConfig.getBoolean("validate");

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

		boolean locatable = modelEvaluatorBuilderConfig.getBoolean("locatable");

		modelEvaluatorBuilder.setLocatable(locatable);

		VisitorBattery visitors = new VisitorBattery();

		List<String> visitorClassNames = modelEvaluatorBuilderConfig.getStringList("visitorClasses");
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

		return modelEvaluatorBuilder;
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
}
