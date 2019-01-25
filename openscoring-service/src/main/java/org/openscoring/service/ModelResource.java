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

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.dmg.pmml.FieldName;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jpmml.evaluator.EvaluationException;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.InputField;
import org.openscoring.common.BatchEvaluationRequest;
import org.openscoring.common.BatchEvaluationResponse;
import org.openscoring.common.BatchModelResponse;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.ModelResponse;
import org.openscoring.common.SimpleResponse;
import org.openscoring.common.TableEvaluationRequest;
import org.openscoring.common.TableEvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("model")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ModelResource {

	@Context
	private UriInfo uriInfo = null;

	private ModelRegistry modelRegistry = null;


	@Inject
	public ModelResource(ModelRegistry modelRegistry){
		this.modelRegistry = modelRegistry;
	}

	@GET
	public BatchModelResponse queryBatch(@Context SecurityContext securityContext){
		Principal owner = securityContext.getUserPrincipal();

		List<ModelResponse> responses = new ArrayList<>();

		Collection<Map.Entry<ModelRef, Model>> entries = this.modelRegistry.entries();
		for(Map.Entry<ModelRef, Model> entry : entries){
			ModelRef modelRef = entry.getKey();
			Model model = entry.getValue();

			if(!Objects.equals(owner, modelRef.getOwner())){
				continue;
			}

			ModelResponse response = createModelResponse(modelRef, model, false);

			responses.add(response);
		}

		Comparator<ModelResponse> comparator = new Comparator<ModelResponse>(){

			@Override
			public int compare(ModelResponse left, ModelResponse right){
				return (left.getId()).compareToIgnoreCase(right.getId());
			}
		};
		Collections.sort(responses, comparator);

		BatchModelResponse batchResponse = new BatchModelResponse()
			.setResponses(responses);

		return batchResponse;
	}

	@GET
	@Path(ModelRef.PATH_VALUE_ID)
	public ModelResponse query(@PathParam("id") ModelRef modelRef){
		Model model = this.modelRegistry.get(modelRef);
		if(model == null){
			throw new NotFoundException("Model \"" + modelRef.getId() + "\" not found");
		}

		return createModelResponse(modelRef, model, true);
	}

	@PUT
	@Path(ModelRef.PATH_VALUE_ID)
	@RolesAllowed (
		value = {"admin"}
	)
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	public Response deploy(@PathParam("id") ModelRef modelRef, Model model){
		return doDeploy(modelRef, model);
	}

	@POST
	@RolesAllowed (
		value = {"admin"}
	)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response deployForm(@FormDataParam("id") ModelRef modelRef, @FormDataParam("pmml") Model model){
		return doDeploy(modelRef, model);
	}

	private Response doDeploy(ModelRef modelRef, Model model){
		boolean success;

		Model oldModel = this.modelRegistry.get(modelRef);
		if(oldModel != null){
			success = this.modelRegistry.replace(modelRef, oldModel, model);
		} else

		{
			success = this.modelRegistry.put(modelRef, model);
		} // End if

		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		ModelResponse entity = createModelResponse(modelRef, model, true);

		if(oldModel != null){
			return (Response.ok().entity(entity)).build();
		} else

		{
			UriBuilder uriBuilder = (this.uriInfo.getBaseUriBuilder()).path(ModelResource.class).path(modelRef.getId());

			URI uri = uriBuilder.build();

			return (Response.created(uri).entity(entity)).build();
		}
	}

	@GET
	@Path(ModelRef.PATH_VALUE_ID + "/pmml")
	@RolesAllowed (
		value = {"admin"}
	)
	@Produces(MediaType.APPLICATION_XML)
	public Model download(@PathParam("id") ModelRef modelRef){
		Model model = this.modelRegistry.get(modelRef, true);
		if(model == null){
			throw new NotFoundException("Model \"" + modelRef.getId() + "\" not found");
		}

		return model;
	}

	@POST
	@Path(ModelRef.PATH_VALUE_ID)
	public EvaluationResponse evaluate(@PathParam("id") ModelRef modelRef, EvaluationRequest request){
		List<EvaluationRequest> requests = Collections.singletonList(request);

		List<EvaluationResponse> responses = doEvaluate(modelRef, requests, true);

		return responses.get(0);
	}

	@POST
	@Path(ModelRef.PATH_VALUE_ID + "/batch")
	public BatchEvaluationResponse evaluateBatch(@PathParam("id") ModelRef modelRef, BatchEvaluationRequest batchRequest){
		List<EvaluationRequest> requests = batchRequest.getRequests();

		List<EvaluationResponse> responses = doEvaluate(modelRef, requests, false);

		BatchEvaluationResponse batchResponse = new BatchEvaluationResponse(batchRequest.getId())
			.setResponses(responses);

		return batchResponse;
	}

	@POST
	@Path(ModelRef.PATH_VALUE_ID + "/csv")
	@Consumes({"application/csv", "text/csv", MediaType.TEXT_PLAIN})
	@Produces(MediaType.TEXT_PLAIN)
	public TableEvaluationResponse evaluateCsv(@PathParam("id") ModelRef modelRef, TableEvaluationRequest tableRequest){
		return doEvaluateCsv(modelRef, tableRequest);
	}

	@POST
	@Path(ModelRef.PATH_VALUE_ID + "/csv")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public TableEvaluationResponse evaluateCsvForm(@PathParam("id") ModelRef modelRef, @FormDataParam("csv") TableEvaluationRequest tableRequest){
		return doEvaluateCsv(modelRef, tableRequest);
	}

	private TableEvaluationResponse doEvaluateCsv(ModelRef modelRef, TableEvaluationRequest tableRequest){
		List<EvaluationRequest> requests = tableRequest.getRequests();

		List<EvaluationResponse> responses = doEvaluate(modelRef, requests, true);

		List<String> columns = new ArrayList<>();

		String idColumn = tableRequest.getIdColumn();
		if(idColumn != null){
			columns.add(idColumn);
		}

		responses:
		for(EvaluationResponse response : responses){
			String message = response.getMessage();

			if(message != null){
				continue;
			}

			Map<String, ?> results = response.getResults();

			columns.addAll(results.keySet());

			break responses;
		}

		TableEvaluationResponse tableResponse = new TableEvaluationResponse()
			.setFormat(tableRequest.getFormat())
			.setColumns(columns)
			.setResponses(responses);

		return tableResponse;
	}

	private List<EvaluationResponse> doEvaluate(ModelRef modelRef, List<EvaluationRequest> requests, boolean allOrNothing){
		Model model = this.modelRegistry.get(modelRef, true);
		if(model == null){
			throw new NotFoundException("Model \"" + modelRef.getId() + "\" not found");
		}

		List<EvaluationResponse> responses = new ArrayList<>();

		try {
			Evaluator evaluator = model.getEvaluator();

			if(evaluator instanceof HasGroupFields){
				HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

				List<InputField> groupFields = hasGroupFields.getGroupFields();
				if(groupFields.size() == 1){
					InputField groupField = groupFields.get(0);

					requests = aggregateRequests(groupField.getName(), requests);
				} else

				if(groupFields.size() > 1){
					throw new EvaluationException("Too many group fields");
				}
			}

			for(EvaluationRequest request : requests){
				EvaluationResponse response;

				try {
					response = evaluate(evaluator, request);
				} catch(Exception e){

					if(allOrNothing){
						throw e;
					}

					response = new EvaluationResponse(request.getId());
					response.setMessage(e.toString());
				}

				responses.add(response);
			}
		} catch(Exception e){
			logger.error("Failed to evaluate", e);

			throw new BadRequestException(e);
		}

		return responses;
	}

	@DELETE
	@Path(ModelRef.PATH_VALUE_ID)
	@RolesAllowed (
		value = {"admin"}
	)
	public SimpleResponse undeploy(@PathParam("id") ModelRef modelRef){
		Model model = this.modelRegistry.get(modelRef);
		if(model == null){
			throw new NotFoundException("Model \"" + modelRef.getId() + "\" not found");
		}

		boolean success = this.modelRegistry.remove(modelRef, model);
		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		SimpleResponse response = new SimpleResponse();

		return response;
	}

	static
	protected List<EvaluationRequest> aggregateRequests(FieldName groupName, List<EvaluationRequest> requests){
		Map<Object, ListMultimap<String, Object>> groupedArguments = new LinkedHashMap<>();

		String key = groupName.getValue();

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

		List<EvaluationRequest> resultRequests = new ArrayList<>();

		Collection<Map.Entry<Object, ListMultimap<String, Object>>> entries = groupedArguments.entrySet();
		for(Map.Entry<Object, ListMultimap<String, Object>> entry : entries){
			Map<String, Object> arguments = new LinkedHashMap<>();
			arguments.putAll((entry.getValue()).asMap());

			// The value of the "group by" column is a single Object, not a Collection (ie. java.util.List) of Objects
			arguments.put(key, entry.getKey());

			EvaluationRequest resultRequest = new EvaluationRequest()
				.setArguments(arguments);

			resultRequests.add(resultRequest);
		}

		return resultRequests;
	}

	static
	protected EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
		logger.info("Received {}", request);

		Map<String, ?> requestArguments = request.getArguments();

		EvaluationResponse response = new EvaluationResponse(request.getId());

		Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

		List<InputField> inputFields = evaluator.getInputFields();
		for(InputField inputField : inputFields){
			FieldName inputName = inputField.getName();

			String key = inputName.getValue();

			Object value = requestArguments.get(key);
			if(value == null && !requestArguments.containsKey(key)){
				logger.warn("Evaluation request {} does not specify an input field {}", request.getId(), key);
			}

			FieldValue inputValue = inputField.prepare(value);

			arguments.put(inputName, inputValue);
		}

		logger.debug("Evaluation request {} has prepared arguments: {}", request.getId(), arguments);

		Map<FieldName, ?> results = evaluator.evaluate(arguments);

		// Jackson does not support the JSON serialization of <code>null</code> map keys
		results = replaceNullKey(results);

		logger.debug("Evaluation response {} has result: {}", response.getId(), results);

		response.setResults(EvaluatorUtil.decode(results));

		logger.info("Returned {}", response);

		return response;
	}

	static
	private <V> Map<FieldName, V> replaceNullKey(Map<FieldName, V> map){

		if(map.containsKey(null)){
			Map<FieldName, V> result = new LinkedHashMap<>(map);
			result.put(ModelResource.DEFAULT_NAME, result.remove(null));

			return result;
		}

		return map;
	}

	static
	private ModelResponse createModelResponse(ModelRef modelRef, Model model, boolean expand){
		ModelResponse response = new ModelResponse(modelRef.getId())
			.setMiningFunction(model.getMiningFunction())
			.setSummary(model.getSummary())
			.setProperties(model.getProperties());

		if(expand){
			response.setSchema(model.getSchema());
		}

		return response;
	}

	public static final FieldName DEFAULT_NAME = FieldName.create("_default");

	private static final Logger logger = LoggerFactory.getLogger(ModelResource.class);
}
