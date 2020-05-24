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
package org.openscoring.common.providers;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jpmml.model.jackson.PMMLModule;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

	private ObjectMapper objectMapper = null;


	public ObjectMapperProvider(){
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new PMMLModule());
		objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
		objectMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
		objectMapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		setObjectMapper(objectMapper);
	}

	@Override
	public ObjectMapper getContext(Class<?> clazz){
		return getObjectMapper();
	}

	public ObjectMapper getObjectMapper(){
		return this.objectMapper;
	}

	private void setObjectMapper(ObjectMapper objectMapper){
		this.objectMapper = objectMapper;
	}
}