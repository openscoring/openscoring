/*
 * Copyright (c) 2013 Villu Ruusmann
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

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class SchemaResponse implements Serializable {

	private List<Field> activeFields = null;

	private List<Field> groupFields = null;

	private List<Field> targetFields = null;

	private List<Field> outputFields = null;


	public SchemaResponse(){
	}

	/**
	 * @see EvaluationRequest#getArguments()
	 */
	public List<Field> getActiveFields(){
		return this.activeFields;
	}

	public void setActiveFields(List<Field> activeFields){
		this.activeFields = activeFields;
	}

	/**
	 * @see EvaluationRequest#getArguments()
	 */
	public List<Field> getGroupFields(){
		return this.groupFields;
	}

	public void setGroupFields(List<Field> groupFields){
		this.groupFields = groupFields;
	}

	/**
	 * @see EvaluationResponse#getResult()
	 */
	public List<Field> getTargetFields(){
		return this.targetFields;
	}

	public void setTargetFields(List<Field> targetFields){
		this.targetFields = targetFields;
	}

	/**
	 * @see EvaluationResponse#getResult()
	 */
	public List<Field> getOutputFields(){
		return this.outputFields;
	}

	public void setOutputFields(List<Field> outputFields){
		this.outputFields = outputFields;
	}
}