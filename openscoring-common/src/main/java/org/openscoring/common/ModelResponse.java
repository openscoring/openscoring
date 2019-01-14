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
import org.dmg.pmml.MiningFunction;
import org.jpmml.model.ToStringHelper;

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
	protected ToStringHelper toStringHelper(){
		return super.toStringHelper()
			.add("id", getId())
			.add("miningFunction", getMiningFunction())
			.add("summary", getSummary())
			.add("properties", getProperties())
			.add("schema", getSchema());
	}

	public String getId(){
		return this.id;
	}

	public ModelResponse setId(String id){
		this.id = id;

		return this;
	}

	public MiningFunction getMiningFunction(){
		return this.miningFunction;
	}

	public ModelResponse setMiningFunction(MiningFunction miningFunction){
		this.miningFunction = miningFunction;

		return this;
	}

	public String getSummary(){
		return this.summary;
	}

	public ModelResponse setSummary(String summary){
		this.summary = summary;

		return this;
	}

	public Map<String, Object> getProperties(){
		return this.properties;
	}

	public ModelResponse setProperties(Map<String, Object> properties){
		this.properties = properties;

		return this;
	}

	public Map<String, List<Field>> getSchema(){
		return this.schema;
	}

	public ModelResponse setSchema(Map<String, List<Field>> schema){
		this.schema = schema;

		return this;
	}
}