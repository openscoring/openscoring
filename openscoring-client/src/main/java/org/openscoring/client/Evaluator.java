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

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.beust.jcommander.DynamicParameter;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Maps;
import org.glassfish.jersey.client.ClientConfig;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;

public class Evaluator extends ModelApplication {

	@DynamicParameter (
		names = {"-X"},
		description = "Model arguments. For example, -Xkey=value"
	)
	private Map<String, String> arguments = Maps.newLinkedHashMap();


	static
	public void main(String... args) throws Exception {
		run(Evaluator.class, args);
	}

	@Override
	public void run(){
		ClientConfig config = new ClientConfig();
		config.register(JacksonJsonProvider.class);

		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target(getModel());

		EvaluationRequest request = new EvaluationRequest();
		request.setArguments(getArguments());

		Invocation invocation = target.request(MediaType.APPLICATION_JSON_TYPE).buildPost(Entity.json(request));

		EvaluationResponse response = invocation.invoke(EvaluationResponse.class);

		System.out.println(response.getResult());

		client.close();
	}

	public Map<String, String> getArguments(){
		return this.arguments;
	}

	public void setArguments(Map<String, String> arguments){
		this.arguments = arguments;
	}
}