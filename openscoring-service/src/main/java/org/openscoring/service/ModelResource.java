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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.dmg.pmml.FieldName;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluator;
import org.openscoring.common.BatchEvaluationRequest;
import org.openscoring.common.BatchEvaluationResponse;
import org.openscoring.common.BatchModelResponse;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.prefs.CsvPreference;

@Path("model")
@PermitAll
public class ModelResource {

	@Context
	private UriInfo uriInfo = null;

	private ModelRegistry modelRegistry = null;

	private MetricRegistry metricRegistry = null;


	@Inject
	public ModelResource(ModelRegistry modelRegistry, MetricRegistry metricRegistry){
		this.modelRegistry = modelRegistry;
		this.metricRegistry = metricRegistry;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public BatchModelResponse queryBatch(){
		BatchModelResponse response = new BatchModelResponse();

		List<ModelResponse> responses = Lists.newArrayList();

		Collection<Map.Entry<String, Model>> entries = this.modelRegistry.entries();
		for(Map.Entry<String, Model> entry : entries){
			responses.add(createModelResponse(entry.getKey(), entry.getValue(), false));
		}

		Comparator<ModelResponse> comparator = new Comparator<ModelResponse>(){

			@Override
			public int compare(ModelResponse left, ModelResponse right){
				return (left.getId()).compareToIgnoreCase(right.getId());
			}
		};
		Collections.sort(responses, comparator);

		response.setResponses(responses);

		return response;
	}

	@GET
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public ModelResponse query(@PathParam("id") String id){
		Model model = this.modelRegistry.get(id);
		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		return createModelResponse(id, model, true);
	}

	@POST
	@RolesAllowed (
		value = {"admin"}
	)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deploy(@FormDataParam("id") String id, @FormDataParam("pmml") InputStream is){

		if(!ModelRegistry.validateId(id)){
			throw new BadRequestException("Invalid identifier");
		}

		return doDeploy(id, is);
	}

	@PUT
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@RolesAllowed (
		value = {"admin"}
	)
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.APPLICATION_JSON)
	public Response deploy(@PathParam("id") String id, @Context HttpServletRequest request){

		try {
			InputStream is = request.getInputStream();

			try {
				return doDeploy(id, is);
			} finally {
				is.close();
			}
		} catch(WebApplicationException wae){
			throw wae;
		} catch(Exception e){
			throw new InternalServerErrorException(e);
		}
	}

	private Response doDeploy(String id, InputStream is){
		Model model;

		try {
			model = this.modelRegistry.load(is);
		} catch(Exception e){
			logger.error("Failed to load PMML document", e);

			throw new BadRequestException(e);
		}

		boolean success;

		Model oldModel = this.modelRegistry.get(id);
		if(oldModel != null){
			success = this.modelRegistry.replace(id, oldModel, model);
		} else

		{
			success = this.modelRegistry.put(id, model);
		} // End if

		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		ModelResponse entity = createModelResponse(id, model, true);

		if(oldModel != null){
			return (Response.ok().entity(entity)).build();
		} else

		{
			UriBuilder uriBuilder = (this.uriInfo.getBaseUriBuilder()).path(ModelResource.class).path(id);

			URI uri = uriBuilder.build();

			return (Response.created(uri).entity(entity)).build();
		}
	}

	@GET
	@Path("{id:" + ModelRegistry.ID_REGEX + "}/pmml")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
	public Response download(@PathParam("id") String id, @Context HttpServletResponse response){
		Model model = this.modelRegistry.get(id, true);
		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		try {
			response.setContentType(MediaType.TEXT_XML);
			response.setHeader("Content-Disposition", "attachment; filename=" + id + ".pmml.xml"); // XXX

			OutputStream os = response.getOutputStream();

			try {
				this.modelRegistry.store(model, os);
			} catch(Exception e){
				logger.error("Failed to store PMML document", e);

				throw e;
			} finally {
				os.close();
			}
		} catch(Exception e){
			throw new InternalServerErrorException(e);
		}

		return (Response.ok()).build();
	}

	@POST
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public EvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request){
		List<EvaluationRequest> requests = Collections.singletonList(request);

		List<EvaluationResponse> responses = doEvaluate(id, requests, "evaluate");

		return responses.get(0);
	}

	@POST
	@Path("{id: " + ModelRegistry.ID_REGEX + "}/batch")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BatchEvaluationResponse evaluateBatch(@PathParam("id") String id, BatchEvaluationRequest request){
		BatchEvaluationResponse response = new BatchEvaluationResponse(request.getId());

		List<EvaluationRequest> requests = request.getRequests();

		List<EvaluationResponse> responses = doEvaluate(id, requests, "evaluate.batch");

		response.setResponses(responses);

		return response;
	}

	@POST
	@Path("{id:" + ModelRegistry.ID_REGEX + "}/csv")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	public Response evaluateCsv(@PathParam("id") String id, @FormDataParam("csv") InputStream is, @Context HttpServletResponse response){
		return doEvaluateCsv(id, is, response);
	}

	@POST
	@Path("{id:" + ModelRegistry.ID_REGEX + "}/csv")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	public Response evaluateCsv(@PathParam("id") String id, @Context HttpServletRequest request, @Context HttpServletResponse response){

		try {
			InputStream is = request.getInputStream();

			try {
				return doEvaluateCsv(id, is, response);
			} finally {
				is.close();
			}
		} catch(WebApplicationException wae){
			throw wae;
		} catch(Exception e){
			throw new InternalServerErrorException(e);
		}
	}

	private Response doEvaluateCsv(String id, InputStream is, HttpServletResponse response){
		CsvPreference format;

		CsvUtil.Table<EvaluationRequest> requestTable;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8")){ // XXX

				@Override
				public void close(){
					// The closing of the underlying java.io.InputStream is handled elsewhere
				}
			};

			try {
				format = CsvUtil.getFormat(reader);

				requestTable = CsvUtil.readTable(reader, format);
			} finally {
				reader.close();
			}
		} catch(Exception e){
			logger.error("Failed to load CSV document", e);

			throw new BadRequestException(e);
		}

		List<EvaluationRequest> requests = requestTable.getRows();

		List<EvaluationResponse> responses = doEvaluate(id, requests, "evaluate.csv");

		CsvUtil.Table<EvaluationResponse> responseTable = new CsvUtil.Table<EvaluationResponse>();
		responseTable.setId(requestTable.getId());
		responseTable.setRows(responses);

		try {
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setHeader("Content-Disposition", "attachment; filename=" + id + ".csv"); // XXX

			OutputStream os = response.getOutputStream();

			try {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8")); // XXX

				try {
					CsvUtil.writeTable(writer, format, responseTable);
				} catch(Exception e){
					logger.error("Failed to store CSV document", e);

					throw e;
				} finally {
					writer.close();
				}
			} finally {
				os.close();
			}
		} catch(Exception e){
			throw new InternalServerErrorException(e);
		}

		return (Response.ok()).build();
	}

	@SuppressWarnings (
		value = "resource"
	)
	private List<EvaluationResponse> doEvaluate(String id, List<EvaluationRequest> requests, String method){
		Model model = this.modelRegistry.get(id, true);
		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		List<EvaluationResponse> responses = Lists.newArrayList();

		Timer timer = this.metricRegistry.timer(createName(id, method));

		Timer.Context context = timer.time();

		try {
			ModelEvaluator<?> evaluator = model.getEvaluator();

			List<FieldName> groupFields = evaluator.getGroupFields();
			if(groupFields.size() == 1){
				FieldName groupField = groupFields.get(0);

				requests = aggregateRequests(groupField, requests);
			} else

			if(groupFields.size() > 1){
				throw new EvaluationException("Too many group fields");
			}

			for(EvaluationRequest request : requests){
				EvaluationResponse response = evaluate(evaluator, request);

				responses.add(response);
			}
		} catch(Exception e){
			logger.error("Failed to evaluate", e);

			throw new BadRequestException(e);
		}

		context.stop();

		Counter counter = this.metricRegistry.counter(createName(id, "evaluate"));

		counter.inc(responses.size());

		return responses;
	}

	@DELETE
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces(MediaType.APPLICATION_JSON)
	public Response undeploy(@PathParam("id") String id){
		Model model = this.modelRegistry.get(id);
		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		boolean success = this.modelRegistry.remove(id, model);
		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		final
		String prefix = createNamePrefix(id);

		MetricFilter filter = new MetricFilter(){

			@Override
			public boolean matches(String name, Metric metric){
				return name.startsWith(prefix);
			}
		};

		this.metricRegistry.removeMatching(filter);

		return (Response.noContent()).build();
	}

	ModelRegistry getModelRegistry(){
		return this.modelRegistry;
	}

	MetricRegistry getMetricRegistry(){
		return this.metricRegistry;
	}

	static
	String createName(String... strings){
		return MetricRegistry.name(ModelResource.class, strings);
	}

	static
	String createNamePrefix(String... strings){
		return createName(strings) + ".";
	}

	static
	List<EvaluationRequest> aggregateRequests(FieldName groupField, List<EvaluationRequest> requests){
		Map<Object, ListMultimap<String, Object>> groupedArguments = Maps.newLinkedHashMap();

		String key = groupField.getValue();

		for(EvaluationRequest request : requests){
			Map<String, ?> requestArguments = request.getArguments();

			Object value = requestArguments.get(key);
			if(value == null && !requestArguments.containsKey(key)){
				logger.warn("Evaluation request {} does not specify a group field {}", request.getId(), key);
			}

			ListMultimap<String, Object> groupedArgumentMap = groupedArguments.get(value);
			if(groupedArgumentMap == null){
				groupedArgumentMap = ArrayListMultimap.create();

				groupedArguments.put(value, groupedArgumentMap);
			}

			Collection<? extends Map.Entry<String, ?>> entries = requestArguments.entrySet();
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
			arguments.put(key, entry.getKey());

			EvaluationRequest resultRequest = new EvaluationRequest();
			resultRequest.setArguments(arguments);

			resultRequests.add(resultRequest);
		}

		return resultRequests;
	}

	static
	EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
		logger.info("Received {}", request);

		Map<String, ?> requestArguments = request.getArguments();

		EvaluationResponse response = new EvaluationResponse(request.getId());

		Map<FieldName, Object> arguments = Maps.newLinkedHashMap();

		List<FieldName> activeFields = evaluator.getActiveFields();
		for(FieldName activeField : activeFields){
			String key = activeField.getValue();

			Object value = requestArguments.get(key);
			if(value == null && !requestArguments.containsKey(key)){
				logger.warn("Evaluation request {} does not specify an active field {}", request.getId(), key);
			}

			arguments.put(activeField, EvaluatorUtil.prepare(evaluator, activeField, value));
		}

		logger.debug("Evaluation request {} has prepared arguments: {}", request.getId(), arguments);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		// Jackson does not support the JSON serialization of <code>null</code> map keys
		result = replaceNullKey(result);

		logger.debug("Evaluation response {} has result: {}", response.getId(), result);

		response.setResult(EvaluatorUtil.decode(result));

		logger.info("Returned {}", response);

		return response;
	}

	static
	private <V> Map<FieldName, V> replaceNullKey(Map<FieldName, V> result){

		if(result.containsKey(null)){
			result.put(ModelResource.DEFAULT_NAME, result.remove(null));
		}

		return result;
	}

	static
	private ModelResponse createModelResponse(String id, Model model, boolean expand){
		ModelResponse response = new ModelResponse(id);
		response.setSummary(model.getSummary());
		response.setProperties(model.getProperties());

		if(expand){
			response.setSchema(model.getSchema());
		}

		return response;
	}

	public static final FieldName DEFAULT_NAME = FieldName.create("_default");

	private static final Logger logger = LoggerFactory.getLogger(ModelResource.class);
}