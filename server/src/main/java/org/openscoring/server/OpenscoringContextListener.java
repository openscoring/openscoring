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
package org.openscoring.server;

import java.util.*;

import com.google.common.collect.*;
import com.google.inject.*;
import com.google.inject.servlet.*;

import com.sun.jersey.guice.*;
import com.sun.jersey.guice.spi.container.servlet.*;

public class OpenscoringContextListener extends GuiceServletContextListener {

	@Override
	public Injector getInjector(){
		Module module = new JerseyServletModule(){

			@Override
			protected void configureServlets(){
				bind(ModelService.class);

				Map<String, String> config = Maps.newLinkedHashMap();
				config.put("com.sun.jersey.api.json.POJOMappingFeature", "true");

				serve("/*").with(GuiceContainer.class, config);
			}
		};

		return Guice.createInjector(module);
	}
}