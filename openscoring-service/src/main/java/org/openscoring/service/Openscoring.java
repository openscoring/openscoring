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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.validation.Schema;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
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
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.visitors.VisitorBattery;
import org.openscoring.common.providers.ObjectMapperProvider;
import org.openscoring.service.filters.ApplicationHeaderFilter;
import org.openscoring.service.providers.ModelProvider;
import org.openscoring.service.providers.ModelRefConverterProvider;
import org.openscoring.service.providers.TableProvider;
import org.xml.sax.SAXException;

public class Openscoring extends ResourceConfig {

	private Config config = null;

	private ModelRegistry modelRegistry = null;

	private LoadingModelEvaluatorBuilder loadingModelEvaluatorBuilder = null;


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

		ModelRegistry modelRegistry = createModelRegistry(config);

		setModelRegistry(modelRegistry);

		Binder modelRegistryBinder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(modelRegistry).to(ModelRegistry.class).named("openscoring");
			}
		};
		register(modelRegistryBinder);

		LoadingModelEvaluatorBuilder loadingModelEvaluatorBuilder = createLoadingModelEvaluatorBuilder(config);

		setLoadingModelEvaluatorBuilder(loadingModelEvaluatorBuilder);

		Binder loadingModelEvaluatorBuilderBinder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(loadingModelEvaluatorBuilder).to(LoadingModelEvaluatorBuilder.class);
			}
		};
		register(loadingModelEvaluatorBuilderBinder);

		Config applicationConfig = config.getConfig("application");

		register(ModelResource.class);

		// Convert path variables to ModelRef objects
		register(loadClass(ModelRefConverterProvider.class, applicationConfig));

		// PMML support
		register(loadClass(ModelProvider.class, applicationConfig));

		// JSON support
		register(JacksonJsonProvider.class);
		register(ObjectMapperProvider.class);

		// CSV support
		register(loadClass(TableProvider.class, applicationConfig));

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

	public ModelRegistry getModelRegistry(){
		return this.modelRegistry;
	}

	private void setModelRegistry(ModelRegistry modelRegistry){
		this.modelRegistry = modelRegistry;
	}

	public LoadingModelEvaluatorBuilder getLoadingModelEvaluatorBuilder(){
		return this.loadingModelEvaluatorBuilder;
	}

	private void setLoadingModelEvaluatorBuilder(LoadingModelEvaluatorBuilder loadingModelEvaluatorBuilder){
		this.loadingModelEvaluatorBuilder = loadingModelEvaluatorBuilder;
	}

	static
	private ModelRegistry createModelRegistry(Config config){
		ModelRegistry modelRegistry = new ModelRegistry();

		return modelRegistry;
	}

	static
	private LoadingModelEvaluatorBuilder createLoadingModelEvaluatorBuilder(Config config){
		Config modelEvaluatorBuilderConfig = config.getConfig("modelEvaluatorBuilder");

		LoadingModelEvaluatorBuilder modelEvaluatorBuilder = new LoadingModelEvaluatorBuilder();

		Class<? extends ModelEvaluatorFactory> modelEvaluatorFactoryClazz = loadClass(ModelEvaluatorFactory.class, modelEvaluatorBuilderConfig);
		modelEvaluatorBuilder.setModelEvaluatorFactory(newInstance(modelEvaluatorFactoryClazz));

		Class<? extends ValueFactoryFactory> valueFactoryFactoryClazz = loadClass(ValueFactoryFactory.class, modelEvaluatorBuilderConfig);
		modelEvaluatorBuilder.setValueFactoryFactory(newInstance(valueFactoryFactoryClazz));

		modelEvaluatorBuilder.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS);

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
	protected <E> Class<? extends E> loadClass(Class<? extends E> superClazz, Config config){
		String path = superClazz.getSimpleName();

		if(path.length() > 0){
			path = path.substring(0, 1).toLowerCase() + path.substring(1);
		}

		path += "Class";

		return loadClass(superClazz, config.getString(path));
	}

	static
	protected <E> Class<? extends E> loadClass(Class<? extends E> superClazz, String name){

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
	protected <E> E newInstance(Class<? extends E> clazz){

		try {
			try {
				Method method = clazz.getDeclaredMethod("newInstance");

				Object result = method.invoke(null);

				return clazz.cast(result);
			} catch(NoSuchMethodException nsme){
				Constructor<? extends E> constructor = clazz.getDeclaredConstructor();

				return constructor.newInstance();
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
