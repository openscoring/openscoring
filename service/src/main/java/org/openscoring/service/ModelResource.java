/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.io.*;
import java.util.*;

import javax.annotation.security.*;
import javax.inject.*;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.openscoring.common.*;

import org.jpmml.evaluator.*;
import org.jpmml.manager.*;

import com.google.common.base.Predicate;
import com.google.common.collect.*;

import org.dmg.pmml.*;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;

import org.supercsv.prefs.*;

@Path("model")
@PermitAll
public class ModelResource {

	private ModelRegistry modelRegistry = null;

	private MetricRegistry metricRegistry = null;


	@Inject
	public ModelResource(ModelRegistry modelRegistry, MetricRegistry metricRegistry){
		this.modelRegistry = modelRegistry;
		this.metricRegistry = metricRegistry;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> list(){
		List<String> result = new ArrayList<String>(this.modelRegistry.keySet());

		Comparator<String> comparator = new Comparator<String>(){

			@Override
			public int compare(String left, String right){
				return (left).compareToIgnoreCase(right);
			}
		};
		Collections.sort(result, comparator);

		return result;
	}

	@PUT
	@Path("{id}")
	@RolesAllowed (
		value = {"admin"}
	)
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.TEXT_PLAIN)
	public String deploy(@PathParam("id") String id, @Context HttpServletRequest request){

		try {
			PMML pmml;

			InputStream is = request.getInputStream();

			try {
				pmml = ModelRegistry.unmarshal(is);
			} finally {
				is.close();
			}

			this.modelRegistry.put(id, pmml);
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		return "Model " + id + " deployed successfully";
	}

	@GET
	@Path("{id}")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	public Response download(@PathParam("id") String id, @Context HttpServletResponse response){
		final
		PMML pmml = this.modelRegistry.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		StreamingOutput entity = new StreamingOutput(){

			@Override
			public void write(OutputStream os){

				try {
					ModelRegistry.marshal(pmml, os);
				} catch(Exception e){
					throw new WebApplicationException(e);
				}
			}
		};

		return Response.ok(entity, MediaType.TEXT_XML)
			.header("Content-Disposition", "attachment; filename=" + id + ".pmml.xml") // XXX
			.build();
	}

	@GET
	@Path("{id}/schema")
	@Produces(MediaType.APPLICATION_JSON)
	public SchemaResponse getSchema(@PathParam("id") String id){
		PMML pmml = this.modelRegistry.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		SchemaResponse response = new SchemaResponse();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			response.setActiveFields(toValueList(evaluator.getActiveFields()));
			response.setGroupFields(toValueList(evaluator.getGroupFields()));
			response.setTargetFields(toValueList(evaluator.getTargetFields()));
			response.setOutputFields(toValueList(evaluator.getOutputFields()));
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@GET
	@Path("{id}/metrics")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces(MediaType.APPLICATION_JSON)
	public MetricRegistry getMetrics(@PathParam("id") String id){
		PMML pmml = this.modelRegistry.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		final
		String prefix = MetricRegistry.name(getClass(), id) + ".";

		Predicate<String> filter = new Predicate<String>(){

			@Override
			public boolean apply(String name){
				return name.startsWith(prefix);
			}
		};

		Map<String, Metric> metrics = Maps.filterKeys(this.metricRegistry.getMetrics(), filter);

		MetricRegistry result = new MetricRegistry();

		Collection<Map.Entry<String, Metric>> entries = metrics.entrySet();
		for(Map.Entry<String, Metric> entry : entries){
			String name = entry.getKey();

			// Strip prefix
			name = name.substring(prefix.length());

			result.register(name, entry.getValue());
		}

		return result;
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public EvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request){
		Timer timer = getTimer(id, "evaluate");

		List<EvaluationRequest> requests = Collections.singletonList(request);

		List<EvaluationResponse> responses = doBatch(id, requests, timer);

		return responses.get(0);
	}

	@POST
	@Path("{id}/batch")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<EvaluationResponse> evaluateBatch(@PathParam("id") String id, List<EvaluationRequest> requests){
		Timer timer = getTimer(id, "evaluateBatch");

		return doBatch(id, requests, timer);
	}

	@POST
	@Path("{id}/csv")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public void evaluateCsv(@PathParam("id") String id, @Context HttpServletRequest request, @QueryParam("idColumn") String idColumn, @Context HttpServletResponse response){
		CsvPreference format;

		List<EvaluationRequest> requests;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8")); // XXX

			try {
				format = CsvUtil.getFormat(reader);

				requests = CsvUtil.readTable(reader, format, idColumn);
			} finally {
				reader.close();
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		Timer timer = getTimer(id, "evaluateCsv");

		List<EvaluationResponse> responses = doBatch(id, requests, timer);

		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8")); // XXX

			try {
				CsvUtil.writeTable(writer, format, ((requests.size() == responses.size()) ? idColumn : null), responses);
			} finally {
				writer.close();
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@DELETE
	@Path("{id}")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces(MediaType.TEXT_PLAIN)
	public String undeploy(@PathParam("id") String id){
		PMML pmml = this.modelRegistry.remove(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		return "Model " + id + " undeployed successfully";
	}

	private List<EvaluationResponse> doBatch(String id, List<EvaluationRequest> requests, Timer timer){
		PMML pmml = this.modelRegistry.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		List<EvaluationResponse> responses = new ArrayList<EvaluationResponse>();

		Timer.Context context = timer.time();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			List<FieldName> groupFields = evaluator.getGroupFields();
			if(groupFields.size() == 1){
				FieldName groupField = groupFields.get(0);

				requests = aggregateRequests(groupField.getValue(), requests);
			} else

			if(groupFields.size() > 1){
				throw new EvaluationException();
			}

			for(EvaluationRequest request : requests){
				EvaluationResponse response = evaluate(evaluator, request);

				responses.add(response);
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		context.stop();

		Counter counter = getCounter(id, "records");

		counter.inc(responses.size());

		return responses;
	}

	private Counter getCounter(String... names){
		return this.metricRegistry.counter(createName(names));
	}

	private Timer getTimer(String... names){
		return this.metricRegistry.timer(createName(names));
	}

	static
	private String createName(String... names){
		return MetricRegistry.name(ModelResource.class, names);
	}

	static
	protected List<EvaluationRequest> aggregateRequests(String groupKey, List<EvaluationRequest> requests){
		Map<Object, ListMultimap<String, Object>> groupedArguments = Maps.newLinkedHashMap();

		for(EvaluationRequest request : requests){
			Map<String, ?> arguments = request.getArguments();

			Object groupValue = arguments.get(groupKey);

			ListMultimap<String, Object> groupedArgumentMap = groupedArguments.get(groupValue);
			if(groupedArgumentMap == null){
				groupedArgumentMap = ArrayListMultimap.create();

				groupedArguments.put(groupValue, groupedArgumentMap);
			}

			Collection<? extends Map.Entry<String, ?>> entries = arguments.entrySet();
			for(Map.Entry<String, ?> entry : entries){
				groupedArgumentMap.put(entry.getKey(), entry.getValue());
			}
		}

		// Only continue with request modification if there is a clear need to do so
		if(groupedArguments.size() == requests.size()){
			return requests;
		}

		List<EvaluationRequest> resultRequests = Lists.newArrayList();

		Collection<Map.Entry<Object, ListMultimap<String, Object>>> entries = groupedArguments.entrySet();
		for(Map.Entry<Object, ListMultimap<String, Object>> entry : entries){
			Map<String, Object> arguments = Maps.newLinkedHashMap();
			arguments.putAll((entry.getValue()).asMap());

			// The value of the "group by" column is a single Object, not a Collection (ie. java.util.List) of Objects
			arguments.put(groupKey, entry.getKey());

			EvaluationRequest resultRequest = new EvaluationRequest();
			resultRequest.setArguments(arguments);

			resultRequests.add(resultRequest);
		}

		return resultRequests;
	}

	static
	protected EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
		EvaluationResponse response = new EvaluationResponse(request.getId());

		Map<FieldName, Object> arguments = Maps.newLinkedHashMap();

		List<FieldName> activeFields = evaluator.getActiveFields();
		for(FieldName activeField : activeFields){
			Object value = request.getArgument(activeField.getValue());

			arguments.put(activeField, EvaluatorUtil.prepare(evaluator, activeField, value));
		}

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		response.setResult(EvaluatorUtil.decode(result));

		return response;
	}

	static
	private List<String> toValueList(List<FieldName> names){
		List<String> result = Lists.newArrayListWithCapacity(names.size());

		for(FieldName name : names){
			result.add(name.getValue());
		}

		return result;
	}
}