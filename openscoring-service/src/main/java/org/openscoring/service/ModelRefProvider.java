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
package org.openscoring.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.google.common.io.CharStreams;

@Provider
@Consumes({MediaType.TEXT_PLAIN, "text/*"})
public class ModelRefProvider implements MessageBodyReader<ModelRef> {

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
		return (ModelRef.class).equals(type);
	}

	@Override
	public ModelRef readFrom(Class<ModelRef> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
		Map<String, String> parameters = mediaType.getParameters();

		String charset = parameters.get(MediaType.CHARSET_PARAMETER);
		if(charset == null){
			charset = "UTF-8";
		}

		String id;

		try(Reader reader = new InputStreamReader(entityStream, charset)){
			id = CharStreams.toString(reader);
		}

		if(!ModelRef.validateId(id)){
			throw new BadRequestException("Invalid identifier");
		}

		ModelRef modelRef = new ModelRef()
			.setId(id);

		return modelRef;
	}
}