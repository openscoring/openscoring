/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.server;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

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

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ModelResponse evaluate(@PathParam("id") String id, ModelRequest request){
		PMML pmml = ModelService.cache.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		Object result;

		try {
			PMMLManager manager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)manager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			Map<FieldName, Object> parameters = new LinkedHashMap<FieldName, Object>();

			List<FieldName> names = evaluator.getActiveFields();
			for(FieldName name : names){
				DataField field = evaluator.getDataField(name);

				String value = request.getParameter((field.getName()).getValue());
				if(value == null){
					continue;
				}

				parameters.put(name, ParameterUtil.parse(field, value));
			}

			result = evaluator.evaluate(parameters);
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		ModelResponse response = new ModelResponse();
		response.setResult(result);

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

	private static final Map<String, PMML> cache = new HashMap<String, PMML>();
}