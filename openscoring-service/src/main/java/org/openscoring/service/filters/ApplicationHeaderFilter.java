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
package org.openscoring.service.filters;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.openscoring.common.Headers;
import org.openscoring.service.ModelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority (
	Priorities.HEADER_DECORATOR
)
public class ApplicationHeaderFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext){
		MultivaluedMap<String, Object> headers = responseContext.getHeaders();

		List<Object> applications = headers.get(Headers.APPLICATION);
		if(applications == null){
			applications = new ArrayList<>();

			headers.put(Headers.APPLICATION, applications);
		}

		applications.add(ApplicationHeaderFilter.nameAndVersion);
	}

	static
	private String discoverNameAndVersion(){
		Package _package = ModelResource.class.getPackage();

		String result = (_package.getSpecificationTitle() + "/" + _package.getSpecificationVersion());

		logger.info("Application name and version: {}", result);

		return result;
	}

	private static final Logger logger = LoggerFactory.getLogger(ApplicationHeaderFilter.class);

	private static final String nameAndVersion = discoverNameAndVersion();
}