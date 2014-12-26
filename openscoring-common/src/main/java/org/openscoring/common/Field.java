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

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class Field implements Serializable {

	private String id = null;

	private String name = null;

	@JsonSerialize (
		using = DataTypeSerializer.class
	)
	@JsonDeserialize (
		using = DataTypeDeserializer.class
	)
	private DataType dataType = null;

	@JsonSerialize (
		using = OpTypeSerializer.class
	)
	@JsonDeserialize (
		using = OpTypeDeserializer.class
	)
	private OpType opType = null;

	private List<String> values = null;


	public Field(){
	}

	public Field(String id){
		setId(id);
	}

	public String getId(){
		return this.id;
	}

	public void setId(String id){
		this.id = id;
	}

	public String getName(){
		return this.name;
	}

	public void setName(String name){
		this.name = name;
	}

	public DataType getDataType(){
		return this.dataType;
	}

	public void setDataType(DataType dataType){
		this.dataType = dataType;
	}

	public OpType getOpType(){
		return this.opType;
	}

	public void setOpType(OpType opType){
		this.opType = opType;
	}

	public List<String> getValues(){
		return this.values;
	}

	public void setValues(List<String> values){
		this.values = values;
	}
}