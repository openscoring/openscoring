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
package org.openscoring.common;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.dmg.pmml.MiningFunction;

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class ModelResponse extends SimpleResponse {

	private String id = null;

	private MiningFunction miningFunction = null;

	private String summary = null;

	private Map<String, Object> properties = null;

	private Map<String, List<Field>> schema = null;


	public ModelResponse(){
	}

	public ModelResponse(String id){
		setId(id);
	}

	@Override
	public String toString(){
		String message = getMessage();
		if(message != null){
			return super.toString();
		}

		ToStringHelper stringHelper = MoreObjects.toStringHelper(getClass())
			.add("id", getId())
			.add("miningFunction", getMiningFunction())
			.add("summary", getSummary())
			.add("properties", getProperties())
			.add("schema", getSchema());

		return stringHelper.toString();
	}

	public String getId(){
		return this.id;
	}

	public void setId(String id){
		this.id = id;
	}

	public MiningFunction getMiningFunction(){
		return this.miningFunction;
	}

	public void setMiningFunction(MiningFunction miningFunction){
		this.miningFunction = miningFunction;
	}

	public String getSummary(){
		return this.summary;
	}

	public void setSummary(String summary){
		this.summary = summary;
	}

	public Map<String, Object> getProperties(){
		return this.properties;
	}

	public void setProperties(Map<String, Object> properties){
		this.properties = properties;
	}

	public Map<String, List<Field>> getSchema(){
		return this.schema;
	}

	public void setSchema(Map<String, List<Field>> schema){
		this.schema = schema;
	}
}