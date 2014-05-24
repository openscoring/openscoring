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

import java.util.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.openscoring.common.*;

import com.google.common.collect.*;

import com.beust.jcommander.*;
import com.fasterxml.jackson.jaxrs.json.*;

import org.glassfish.jersey.client.*;

public class Evaluator extends Application {

	@Parameter (
		names = {"--model"},
		description = "The URI of the model",
		required = true
	)
	private String model = null;

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

		WebTarget target = client.target(this.model);

		EvaluationRequest request = new EvaluationRequest();
		request.setArguments(this.arguments);

		Invocation invocation = target.request(MediaType.APPLICATION_JSON_TYPE).buildPost(Entity.json(request));

		EvaluationResponse response = invocation.invoke(EvaluationResponse.class);

		System.out.println(response.getResult());

		client.close();
	}
}