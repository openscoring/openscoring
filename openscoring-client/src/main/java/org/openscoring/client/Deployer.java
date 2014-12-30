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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.beust.jcommander.Parameter;

public class Deployer extends ModelApplication {

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

		WebTarget target = client.target(getModel());

		InputStream is = new FileInputStream(getFile());

		try {
			Invocation invocation = target.request(MediaType.APPLICATION_JSON_TYPE).buildPut(Entity.xml(is));

			String result = invocation.invoke(String.class);

			System.out.println(result);
		} finally {
			is.close();
		}

		client.close();
	}

	public File getFile(){
		return this.file;
	}

	public void setFile(File file){
		this.file = file;
	}
}