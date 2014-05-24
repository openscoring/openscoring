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

import java.io.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import com.beust.jcommander.*;

public class Deployer extends Application {

	@Parameter (
		names = {"--model"},
		description = "The URI of the model",
		required = true
	)
	private String model = null;

	@Parameter (
		names = {"--file"},
		description = "The PMML file",
		required = true
	)
	private File file = null;


	static
	public void main(String... args) throws Exception {
		run(Deployer.class, args);
	}

	@Override
	public void run() throws IOException {
		Client client = ClientBuilder.newClient();

		WebTarget target = client.target(this.model);

		InputStream is = new FileInputStream(this.file);

		try {
			Invocation invocation = target.request(MediaType.TEXT_PLAIN).buildPut(Entity.xml(is));

			String result = invocation.invoke(String.class);

			System.out.println(result);
		} finally {
			is.close();
		}

		client.close();
	}
}