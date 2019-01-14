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
package org.openscoring.client;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.beust.jcommander.DynamicParameter;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Evaluator extends ModelApplication {

	@DynamicParameter (
		names = {"-X"},
		description = "Model arguments. For example, -Xkey=value"
	)
	private Map<String, String> arguments = new LinkedHashMap<>();


	static
	public void main(String... args) throws Exception {
		run(Evaluator.class, args);
	}

	@Override
	public void run() throws Exception {
		EvaluationResponse response = evaluate();

		String message = response.getMessage();
		if(message != null){
			logger.warn("Evaluation failed: {}", message);

			return;
		}

		logger.info("Evaluation succeeded: {}", response);
	}

	public EvaluationResponse evaluate() throws Exception {
		Operation<EvaluationResponse> operation = new Operation<EvaluationResponse>(){

			@Override
			public EvaluationResponse perform(WebTarget target) throws Exception {
				EvaluationRequest request = new EvaluationRequest()
					.setArguments(getArguments());

				Invocation invocation = target.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(request));

				Response response = invocation.invoke();

				return response.readEntity(EvaluationResponse.class);
			}
		};

		return execute(operation);
	}

	public Map<String, String> getArguments(){
		return this.arguments;
	}

	public void setArguments(Map<String, String> arguments){
		this.arguments = arguments;
	}

	private static final Logger logger = LoggerFactory.getLogger(Evaluator.class);
}