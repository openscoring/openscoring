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

import com.sun.jersey.api.*;

@Path("/model/{id}")
public class ModelService {

	@PUT
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.TEXT_PLAIN)
	public String putModel(@PathParam("id") String id, @Context HttpServletRequest request){
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
	public SummaryResponse summary(@PathParam("id") String id){
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
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public EvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request){
		PMML pmml = ModelService.cache.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		EvaluationResponse response = new EvaluationResponse();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			Map<FieldName, Object> parameters = new LinkedHashMap<FieldName, Object>();

			List<FieldName> activeFields = evaluator.getActiveFields();
			for(FieldName activeField : activeFields){
				Object value = request.getParameter(activeField.getValue());

				parameters.put(activeField, evaluator.prepare(activeField, value));
			}

			Map<FieldName, ?> result = evaluator.evaluate(parameters);

			// XXX
			response.setResult((Map)EvaluatorUtil.decode(result));
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@DELETE
	@Produces(MediaType.TEXT_PLAIN)
	public String deleteModel(@PathParam("id") String id){
		PMML pmml = ModelService.cache.remove(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		return "Model " + id + " undeployed successfully";
	}

	static
	private List<String> toValueList(List<FieldName> names){
		List<String> result = new ArrayList<String>(names.size());

		for(FieldName name : names){
			result.add(name.getValue());
		}

		return result;
	}

	private static final Map<String, PMML> cache = new HashMap<String, PMML>();
}