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

import java.util.List;

import javax.inject.Singleton;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class Openscoring extends ResourceConfig {

	public Openscoring(){
		super(ModelResource.class);

		final
		Config config = ConfigFactory.load();

		Binder binder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(config).to(Config.class).named("openscoring");

				bind(ModelRegistry.class).to(ModelRegistry.class).in(Singleton.class);
			}
		};
		register(binder);

		// JSON support
		register(JacksonJsonProvider.class);
		register(ObjectMapperProvider.class);

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

		Config applicationConfig = config.getConfig("application");

		List<String> componentClassNames = applicationConfig.getStringList("componentClasses");
		for(String componentClassName : componentClassNames){
			Class<?> clazz;

			try {
				clazz = Class.forName(componentClassName);
			} catch(ClassNotFoundException cnfe){
				throw new IllegalArgumentException(cnfe);
			}

			register(clazz);
		}
	}
}