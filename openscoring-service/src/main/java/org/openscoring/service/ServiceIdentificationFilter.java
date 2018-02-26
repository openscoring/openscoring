/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.openscoring.common.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority (
	Priorities.HEADER_DECORATOR
)
public class ServiceIdentificationFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext){
		MultivaluedMap<String, Object> headers = responseContext.getHeaders();

		List<Object> services = headers.get(Headers.SERVICE);
		if(services == null){
			services = new ArrayList<>();

			headers.put(Headers.SERVICE, services);
		}

		services.add(ServiceIdentificationFilter.nameAndVersion);
	}

	static
	private String discoverNameAndVersion(){
		Package _package = ModelResource.class.getPackage();

		String result = (_package.getSpecificationTitle() + "/" + _package.getSpecificationVersion());

		logger.info("Service name and version: {}", result);

		return result;
	}

	private static final Logger logger = LoggerFactory.getLogger(ServiceIdentificationFilter.class);

	private static final String nameAndVersion = discoverNameAndVersion();
}