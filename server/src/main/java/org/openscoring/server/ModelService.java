/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.server;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.openscoring.common.*;

import org.jpmml.evaluator.*;
import org.jpmml.manager.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;
import com.sun.jersey.api.*;

import org.supercsv.prefs.*;

@Path("model")
public class ModelService {

	@PUT
	@Path("{id}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.TEXT_PLAIN)
	public String deploy(@PathParam("id") String id, @Context HttpServletRequest request){
		PMML pmml;

		try {
			InputStream is = request.getInputStream();

			try {
				pmml = IOUtil.unmarshal(is);
			} finally {
				is.close();
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		ModelService.cache.put(id, pmml);

		return "Model " + id + " deployed successfully";
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getDeployedIds(){
		List<String> result = new ArrayList<String>(ModelService.cache.keySet());

		return result;
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public SummaryResponse getSummary(@PathParam("id") String id){
		PMML pmml = ModelService.cache.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		SummaryResponse response = new SummaryResponse();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			response.setActiveFields(toValueList(evaluator.getActiveFields()));
			response.setPredictedFields(toValueList(evaluator.getPredictedFields()));
			response.setOutputFields(toValueList(evaluator.getOutputFields()));
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public EvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request){
		return (evaluateBatch(id, Collections.singletonList(request))).get(0);
	}

	@POST
	@Path("{id}/batch")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<EvaluationResponse> evaluateBatch(@PathParam("id") String id, List<EvaluationRequest> requests){
		PMML pmml = ModelService.cache.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		List<EvaluationResponse> responses = new ArrayList<EvaluationResponse>();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			for(EvaluationRequest request : requests){
				EvaluationResponse response = evaluate(evaluator, request);

				responses.add(response);
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return responses;
	}

	@POST
	@Path("{id}/csv")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public void evaluateCsv(@PathParam("id") String id, @Context HttpServletRequest request, @Context HttpServletResponse response){
		CsvPreference format;

		List<EvaluationRequest> requests;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8")); // XXX

			try {
				format = CsvUtil.getFormat(reader);

				requests = CsvUtil.readTable(reader, format);
			} finally {
				reader.close();
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		List<EvaluationResponse> responses = evaluateBatch(id, requests);

		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8")); // XXX

			try {
				CsvUtil.writeTable(writer, format, responses);
			} finally {
				writer.close();
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@DELETE
	@Path("{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public String undeploy(@PathParam("id") String id){
		PMML pmml = ModelService.cache.remove(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		return "Model " + id + " undeployed successfully";
	}

	static
	private EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
		EvaluationResponse response = new EvaluationResponse();

		Map<FieldName, Object> arguments = Maps.newLinkedHashMap();

		List<FieldName> activeFields = evaluator.getActiveFields();
		for(FieldName activeField : activeFields){
			Object value = request.getArgument(activeField.getValue());

			arguments.put(activeField, evaluator.prepare(activeField, value));
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

	private static final Map<String, PMML> cache = Maps.newLinkedHashMap();
}