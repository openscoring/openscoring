/*
 * Copyright (c) 2014 Villu Ruusmann
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;

abstract
public class ModelApplication extends Application {

	@Parameter (
		names = {"--model"},
		description = "The URI of the model",
		required = true
	)
	private String model = null;


	public <V> V execute(Operation<V> operation) throws Exception {
		ClientConfig config = new ClientConfig();
		config.register(JacksonJsonProvider.class);

		Client client = ClientBuilder.newClient(config);

		try {
			WebTarget target = client.target(getURI());

			return operation.perform(target);
		} finally {
			client.close();
		}
	}

	public String getURI(){
		return getModel();
	}

	public String getModel(){
		return this.model;
	}

	public void setModel(String model){
		this.model = model;
	}
}