/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.Collection;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import org.jpmml.evaluator.ModelEvaluator;

@Path("metric")
@PermitAll
public class MetricResource {

	private ModelRegistry modelRegistry = null;

	private MetricRegistry metricRegistry = null;


	@Inject
	private MetricResource(ModelRegistry modelRegistry, MetricRegistry metricRegistry){
		this.modelRegistry = modelRegistry;
		this.metricRegistry = metricRegistry;
	}

	@GET
	@Path("model")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces(MediaType.APPLICATION_JSON)
	public MetricRegistry queryModelBatch(){
		String prefix = ModelResource.createNamePrefix();

		return doMetrics(prefix);
	}

	@GET
	@Path("model/{id:" + ModelRegistry.ID_REGEX + "}")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces(MediaType.APPLICATION_JSON)
	public MetricRegistry queryModel(@PathParam("id") String id){
		ModelEvaluator<?> evaluator = this.modelRegistry.get(id);
		if(evaluator == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		String prefix = ModelResource.createNamePrefix(id);

		return doMetrics(prefix);
	}

	private MetricRegistry doMetrics(final String prefix){
		MetricRegistry result = new MetricRegistry();

		MetricFilter filter = new MetricFilter(){

			@Override
			public boolean matches(String name, Metric metric){
				return name.startsWith(prefix);
			}
		};

		Map<String, Metric> metrics = this.metricRegistry.getMetrics();

		Collection<Map.Entry<String, Metric>> entries = metrics.entrySet();
		for(Map.Entry<String, Metric> entry : entries){
			String name = entry.getKey();
			Metric metric = entry.getValue();

			if(!filter.matches(name, metric)){
				continue;
			}

			// Strip prefix
			name = name.substring(prefix.length());

			result.register(name, metric);
		}

		return result;
	}
}