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

import com.codahale.metrics.MetricRegistry;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class Openscoring extends ResourceConfig {

	final
	private ModelRegistry modelRegistry = new ModelRegistry();

	final
	private MetricRegistry metricRegistry = new MetricRegistry();


	public Openscoring(){
		super(ModelResource.class);

		Binder binder = new AbstractBinder(){

			@Override
			public void configure(){
				bind(getModelRegistry()).to(ModelRegistry.class);
				bind(getMetricRegistry()).to(MetricRegistry.class);
			}
		};
		register(binder);

		// JSON support
		register(JacksonFeature.class);
		register(ObjectMapperProvider.class);

		// File upload support
		register(MultiPartFeature.class);

		// Security support
		register(RolesAllowedDynamicFeature.class);
	}

	public ModelRegistry getModelRegistry(){
		return this.modelRegistry;
	}

	public MetricRegistry getMetricRegistry(){
		return this.metricRegistry;
	}
}