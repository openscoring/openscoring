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
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Value;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.TypeUtil;
import org.openscoring.common.BatchEvaluationRequest;
import org.openscoring.common.BatchEvaluationResponse;
import org.openscoring.common.BatchModelResponse;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.Field;
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

		Collection<Map.Entry<String, ModelEvaluator<?>>> entries = this.modelRegistry.entries();
		for(Map.Entry<String, ModelEvaluator<?>> entry : entries){
			responses.add(createModelResponse(entry.getKey(), false, entry.getValue()));
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
		ModelEvaluator<?> evaluator = this.modelRegistry.get(id);
		if(evaluator == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		return createModelResponse(id, true, evaluator);
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
		ModelEvaluator<?> evaluator;

		try {
			evaluator = this.modelRegistry.load(is);
		} catch(Exception e){
			logger.error("Failed to load PMML document", e);

			throw new BadRequestException(e);
		}

		boolean success;

		ModelEvaluator<?> oldEvaluator = this.modelRegistry.get(id);
		if(oldEvaluator != null){
			success = this.modelRegistry.replace(id, oldEvaluator, evaluator);
		} else

		{
			success = this.modelRegistry.put(id, evaluator);
		} // End if

		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		ModelResponse entity = createModelResponse(id, true, evaluator);

		if(oldEvaluator != null){
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
		ModelEvaluator<?> evaluator = this.modelRegistry.get(id);
		if(evaluator == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		try {
			response.setContentType(MediaType.TEXT_XML);
			response.setHeader("Content-Disposition", "attachment; filename=" + id + ".pmml.xml"); // XXX

			OutputStream os = response.getOutputStream();

			try {
				this.modelRegistry.store(evaluator, os);
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

		List<EvaluationResponse> responses = doEvaluate(id, requests, "evaluateBatch");

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

		List<EvaluationResponse> responses = doEvaluate(id, requests, "evaluateCsv");

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
		ModelEvaluator<?> evaluator = this.modelRegistry.get(id);
		if(evaluator == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		List<EvaluationResponse> responses = Lists.newArrayList();

		Timer timer = this.metricRegistry.timer(createName(id, method));

		Timer.Context context = timer.time();

		try {
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

		Counter counter = this.metricRegistry.counter(createName(id, "records"));

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
		ModelEvaluator<?> evaluator = this.modelRegistry.get(id);
		if(evaluator == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		boolean success = this.modelRegistry.remove(id, evaluator);
		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		final
		String prefix = createName(id) + ".";

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
	String createName(String... names){
		return MetricRegistry.name(ModelResource.class, names);
	}

	static
	protected List<EvaluationRequest> aggregateRequests(FieldName groupField, List<EvaluationRequest> requests){
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
	protected EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
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
	private ModelResponse createModelResponse(String id, boolean expand, ModelEvaluator<?> evaluator){
		ModelResponse response = new ModelResponse(id);
		response.setSummary(evaluator.getSummary());

		if(expand){
			response.setSchema(encodeSchema(evaluator));
		}

		return response;
	}

	static
	private Map<String, List<Field>> encodeSchema(ModelEvaluator<?> evaluator){
		Map<String, List<Field>> result = Maps.newLinkedHashMap();

		List<FieldName> activeFields = evaluator.getActiveFields();
		List<FieldName> groupFields = evaluator.getGroupFields();
		List<FieldName> targetFields = evaluator.getTargetFields();

		if(targetFields.isEmpty()){
			targetFields = Collections.singletonList(evaluator.getTargetField());
		}

		result.put("activeFields", encodeMiningFields(activeFields, evaluator));
		result.put("groupFields", encodeMiningFields(groupFields, evaluator));
		result.put("targetFields", encodeMiningFields(targetFields, evaluator));

		List<FieldName> outputFields = evaluator.getOutputFields();

		result.put("outputFields", encodeOutputFields(outputFields, evaluator));

		return result;
	}

	static
	private List<Field> encodeMiningFields(List<FieldName> names, ModelEvaluator<?> evaluator){
		List<Field> fields = Lists.newArrayList();

		for(FieldName name : names){
			DataField dataField = evaluator.getDataField(name);

			// A "phantom" default target field
			if(dataField == null){
				continue;
			}

			DataType dataType = dataField.getDataType();

			OpType opType = null;

			MiningField miningField = evaluator.getMiningField(name);
			if(miningField != null){
				opType = miningField.getOpType();
			} // End if

			if(opType == null){
				opType = dataField.getOpType();
			} // End if

			if(name == null){
				name = ModelResource.DEFAULT_NAME;
			}

			Field field = new Field(name.getValue());
			field.setName(dataField.getDisplayName());
			field.setDataType(dataType);
			field.setOpType(opType);
			field.setValues(encodeValues(dataField));

			fields.add(field);
		}

		return fields;
	}

	static
	private List<Field> encodeOutputFields(List<FieldName> names, ModelEvaluator<?> evaluator){
		List<Field> fields = Lists.newArrayList();

		for(FieldName name : names){
			OutputField outputField = evaluator.getOutputField(name);

			DataType dataType = null;

			OpType opType = null;

			try {
				dataType = OutputUtil.getDataType(outputField, evaluator);

				opType = outputField.getOpType();
				if(opType == null){
					opType = TypeUtil.getOpType(dataType);
				}
			} catch(Exception e){
				// Ignored
			}

			Field field = new Field(name.getValue());
			field.setName(outputField.getDisplayName());
			field.setDataType(dataType);
			field.setOpType(opType);

			fields.add(field);
		}

		return fields;
	}

	static
	private List<String> encodeValues(DataField dataField){
		List<String> result = Lists.newArrayList();

		List<Interval> intervals = dataField.getIntervals();
		for(Interval interval : intervals){
			StringBuffer sb = new StringBuffer();

			Double leftMargin = interval.getLeftMargin();
			sb.append(leftMargin != null ? leftMargin : "-\u221e");

			sb.append(", ");

			Double rightMargin = interval.getRightMargin();
			sb.append(rightMargin != null ? rightMargin : "\u221e");

			String value = sb.toString();

			Interval.Closure closure = interval.getClosure();
			switch(closure){
				case OPEN_OPEN:
					result.add("(" + value + ")");
					break;
				case OPEN_CLOSED:
					result.add("(" + value + "]");
					break;
				case CLOSED_OPEN:
					result.add("[" + value + ")");
					break;
				case CLOSED_CLOSED:
					result.add("[" + value + "]");
					break;
				default:
					break;
			}
		}

		List<Value> values = dataField.getValues();
		for(Value value : values){
			Value.Property property = value.getProperty();

			switch(property){
				case VALID:
					result.add(value.getValue());
					break;
				default:
					break;
			}
		}

		return result;
	}

	private static final FieldName DEFAULT_NAME = FieldName.create("_default");

	private static final Logger logger = LoggerFactory.getLogger(ModelResource.class);
}