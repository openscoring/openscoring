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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.beust.jcommander.Parameter;

public class CsvEvaluator extends ModelApplication {

	@Parameter (
		names = {"--input"},
		description = "Input CSV file",
		required = true
	)
	private File input = null;

	@Parameter (
		names = {"--output"},
		description = "Output CSV file",
		required = true
	)
	private File output = null;


	static
	public void main(String... args) throws Exception {
		run(CsvEvaluator.class, args);
	}

	@Override
	public void run() throws IOException {
		Client client = ClientBuilder.newClient();

		WebTarget target = client.target(ensureSuffix(getModel(), "/csv"));

		InputStream is = new FileInputStream(getInput());

		try {
			OutputStream os = new FileOutputStream(getOutput());

			try {
				Invocation invocation = target.request(MediaType.TEXT_PLAIN).buildPost(Entity.text(is));

				InputStream result = invocation.invoke(InputStream.class);

				try {
					copy(result, os);
				} finally {
					result.close();
				}
			} finally {
				os.close();
			}
		} finally {
			is.close();
		}

		client.close();
	}

	public File getInput(){
		return this.input;
	}

	public void setInput(File input){
		this.input = input;
	}

	public File getOutput(){
		return this.output;
	}

	public void setOutput(File output){
		this.output = output;
	}

	static
	private String ensureSuffix(String string, String suffix){

		if(!string.endsWith(suffix)){
			string += suffix;
		}

		return string;
	}

	static
	private void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[512];

		while(true){
			int count = is.read(buffer);
			if(count < 0){
				break;
			}

			os.write(buffer, 0, count);
		}

		os.flush();
	}
}