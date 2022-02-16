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

import java.io.EOFException;
import java.io.IOException;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.UnmarshalException;
import org.junit.Test;
import org.openscoring.common.SimpleResponse;

import static org.junit.Assert.assertEquals;

public class WebApplicationExceptionMapperTest {

	@Test
	public void toResponse(){
		assertEquals("Not Found", getMessage(new NotFoundException()));
		assertEquals("Resource \"id\" not found", getMessage(new NotFoundException("Resource \"id\" not found")));

		assertEquals("Bad Request", getMessage(new BadRequestException(new IllegalArgumentException())));
		assertEquals("Bad \"id\" value", getMessage(new BadRequestException(new IllegalArgumentException("Bad \"id\" value"))));

		assertEquals("Bad Request", getMessage(new BadRequestException(new UnmarshalException(new IOException()))));
		assertEquals("Resource \"id\" is incomplete", getMessage(new BadRequestException(new UnmarshalException(new EOFException("Resource \"id\" is incomplete")))));

		UnmarshalException selfRefException = new UnmarshalException((Throwable)null);
		selfRefException.setLinkedException(new UnmarshalException(selfRefException));

		assertEquals("Bad Request", getMessage(new BadRequestException(selfRefException)));
	}

	static
	private String getMessage(WebApplicationException exception){
		WebApplicationExceptionMapper exceptionMapper = new WebApplicationExceptionMapper();

		Response response = exceptionMapper.toResponse(exception);

		SimpleResponse entity = (SimpleResponse)response.getEntity();

		return entity.getMessage();
	}
}