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
package org.openscoring.client;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jpmml.model.PMMLModule;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

	private ObjectMapper mapper = null;


	public ObjectMapperProvider(){
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new PMMLModule());
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		setMapper(mapper);
	}

	@Override
	public ObjectMapper getContext(Class<?> clazz){
		return getMapper();
	}

	public ObjectMapper getMapper(){
		return this.mapper;
	}

	private void setMapper(ObjectMapper mapper){
		this.mapper = mapper;
	}
}