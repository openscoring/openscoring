/*
 * Copyright (c) 2019 Villu Ruusmann
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
package org.openscoring.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.openscoring.common.TableEvaluationRequest;
import org.openscoring.common.TableEvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.prefs.CsvPreference;

@Provider
@Consumes({"application/csv", "text/csv", MediaType.TEXT_PLAIN, "text/*"})
@Produces(MediaType.TEXT_PLAIN)
public class TableProvider implements MessageBodyReader<TableEvaluationRequest>, MessageBodyWriter<TableEvaluationResponse> {

	@Context
	private UriInfo uriInfo = null;


	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
		return (TableEvaluationRequest.class).equals(type);
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
		return (TableEvaluationResponse.class).equals(type);
	}

	@Override
	public TableEvaluationRequest readFrom(Class<TableEvaluationRequest> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
		String charset = getCharset(mediaType);

		MultivaluedMap<String, String> queryParameters = this.uriInfo.getQueryParameters();

		String delimiterChar = queryParameters.getFirst("delimiterChar");
		String quoteChar = queryParameters.getFirst("quoteChar");

		BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream, charset)){

			@Override
			public void close(){
				// The closing of the underlying java.io.InputStream is handled elsewhere
			}
		};

		TableEvaluationRequest tableRequest;

		try {
			CsvPreference format;

			if(delimiterChar != null){
				format = CsvUtil.getFormat(delimiterChar, quoteChar);
			} else

			{
				format = CsvUtil.getFormat(reader);
			}

			tableRequest = CsvUtil.readTable(reader, format);
		} catch(Exception e){
			logger.error("Failed to load CSV document", e);

			throw new BadRequestException(e);
		} finally {
			reader.close();
		}

		tableRequest.setCharset(charset);

		return tableRequest;
	}

	@Override
	public void writeTo(TableEvaluationResponse tableResponse, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
		String charset = getCharset(mediaType);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(entityStream, charset)){

			@Override
			public void close() throws IOException {
				flush();

				// The closing of the underlying java.io.OutputStream is handled elsewhere
			}
		};

		try {
			CsvUtil.writeTable(tableResponse, writer);
		} finally {
			writer.close();
		}
	}

	static
	private String getCharset(MediaType mediaType){
		Map<String, String> parameters = mediaType.getParameters();

		String charset = parameters.get(MediaType.CHARSET_PARAMETER);
		if(charset == null){
			charset = "UTF-8";
		}

		return charset;
	}

	private static final Logger logger = LoggerFactory.getLogger(TableProvider.class);
}