/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.util.IdentityHashMap;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.openscoring.common.SimpleResponse;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

	@Override
	public Response toResponse(WebApplicationException exception){
		Response response = exception.getResponse();

		Throwable throwable = exception;

		Map<Throwable, Throwable> throwableMap = new IdentityHashMap<>();

		while(true){
			Throwable cause = throwable.getCause();
			throwableMap.put(throwable, cause);

			if((cause == null) || throwableMap.containsKey(cause)){
				break;
			}

			throwable = cause;
		}

		String message = throwable.getMessage();
		if((message == null) || ("").equals(message)){
			Response.Status status = (Response.Status)response.getStatusInfo();

			message = status.getReasonPhrase();
		} // End if

		// Strip the HTTP status code prefix
		if(message.startsWith("HTTP ")){
			message = message.replaceFirst("^HTTP (\\d)+ ", "");
		}

		SimpleResponse entity = new SimpleResponse();
		entity.setMessage(message);

		return (Response.fromResponse(response).entity(entity).type(MediaType.APPLICATION_JSON)).build();
	}
}