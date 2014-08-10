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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.beust.jcommander.Parameter;

public class Undeployer extends Application {

	@Parameter (
		names = {"--model"},
		description = "The URI of the model",
		required = true
	)
	private String model = null;


	static
	public void main(String... args) throws Exception {
		run(Undeployer.class, args);
	}

	@Override
	public void run(){
		Client client = ClientBuilder.newClient();

		WebTarget target = client.target(this.model);

		Invocation invocation = target.request(MediaType.TEXT_PLAIN_TYPE).buildDelete();

		String result = invocation.invoke(String.class);

		System.out.println(result);

		client.close();
	}
}