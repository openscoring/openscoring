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
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.beust.jcommander.Parameter;
import org.openscoring.common.SimpleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvEvaluator extends ModelApplication {

	@Parameter (
		names = {"--input"},
		description = "Input CSV file",
		required = true
	)
	private File input = null;

	@Parameter (
		names = "--csv-delimiter",
		description = "CSV delimiter character"
	)
	private String delimiterChar = null;

	@Parameter (
		names = "--csv-quote",
		description = "CSV quote character"
	)
	private String quoteChar = null;

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
	public void run() throws Exception {
		SimpleResponse response = evaluate();

		String message = (response != null ? response.getMessage() : null);
		if(message != null){
			logger.warn("CSV evaluation failed: {}", message);

			return;
		}

		logger.info("CSV evaluation succeeded");
	}

	/**
	 * @return <code>null</code> If the operation was successful.
	 */
	public SimpleResponse evaluate() throws Exception {
		Operation<SimpleResponse> operation = new Operation<SimpleResponse>(){

			@Override
			public SimpleResponse perform(WebTarget target) throws Exception {
				String delimiterChar = getDelimiterChar();
				String quoteChar = getQuoteChar();

				if(delimiterChar != null){
					target = target.queryParam("delimiterChar", delimiterChar);

					if(quoteChar != null){
						target = target.queryParam("quoteChar", quoteChar);
					}
				}

				try(InputStream is = new FileInputStream(getInput())){

					try(OutputStream os = new FileOutputStream(getOutput())){
						Invocation invocation = target.request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN).buildPost(Entity.text(is));

						Response response = invocation.invoke();

						Response.StatusType status = response.getStatusInfo();
						switch(status.getFamily()){
							case CLIENT_ERROR:
							case SERVER_ERROR:
								return response.readEntity(SimpleResponse.class);
							default:
								break;
						}

						try(InputStream result = response.readEntity(InputStream.class)){
							byte[] buffer = new byte[10 * 1024];

							while(true){
								int count = result.read(buffer);
								if(count < 0){
									break;
								}

								os.write(buffer, 0, count);
							}

							return null;
						}
					}
				}
			}
		};

		return execute(operation);
	}

	@Override
	public String getURI(){
		return ensureSuffix(getModel(), "/csv");
	}

	public File getInput(){
		return this.input;
	}

	public void setInput(File input){
		this.input = input;
	}

	public String getDelimiterChar(){
		return this.delimiterChar;
	}

	public void setDelimiterChar(String delimiterChar){
		this.delimiterChar = delimiterChar;
	}

	public String getQuoteChar(){
		return this.quoteChar;
	}

	public void setQuoteChar(String quoteChar){
		this.quoteChar = quoteChar;
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

	private static final Logger logger = LoggerFactory.getLogger(CsvEvaluator.class);
}