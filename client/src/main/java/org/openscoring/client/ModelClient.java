/*
 * Copyright (c) 2013 Villu Ruusmann
 */
package org.openscoring.client;

import java.util.*;

import javax.ws.rs.core.*;

import org.openscoring.common.*;

import com.beust.jcommander.*;
import com.google.common.collect.*;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.*;

import org.codehaus.jackson.jaxrs.*;

public class ModelClient {

	@Parameter (
		names = {"--model"},
		description = "The URI of the model"
	)
	private String model = null;

	@DynamicParameter (
		names = {"-P"},
		description = "Model arguments. For example, -Pkey=value"
	)
	private Map<String, String> arguments = Maps.newLinkedHashMap();


	static
	public void main(String... args) throws Exception {
		ModelClient client = new ModelClient();

		JCommander commander = new JCommander(client);
		commander.setProgramName(ModelClient.class.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			commander.usage();

			System.exit(-1);
		}

		client.run();
	}

	public void run(){
		ClientConfig config = new DefaultClientConfig();
		(config.getClasses()).add(JacksonJsonProvider.class);

		Client client = Client.create(config);

		WebResource resource = client.resource(this.model);

		EvaluationRequest request = new EvaluationRequest();
		request.setArguments(this.arguments);

		EvaluationResponse response = resource.accept(MediaType.APPLICATION_JSON).entity(request, MediaType.APPLICATION_JSON).post(EvaluationResponse.class);

		System.out.println(response.getResult());
	}
}