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
package org.openscoring.service.providers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.openscoring.service.ModelRef;

@Provider
public class ModelRefConverterProvider implements ParamConverterProvider {

	@Context
	private SecurityContext securityContext = null;


	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation annotations[]){

		if((ModelRef.class).equals(rawType)){
			ParamConverter<ModelRef> paramConverter = new ModelRefConverter(getSecurityContext());

			return (ParamConverter)paramConverter;
		}

		return null;
	}

	public SecurityContext getSecurityContext(){
		return this.securityContext;
	}
}