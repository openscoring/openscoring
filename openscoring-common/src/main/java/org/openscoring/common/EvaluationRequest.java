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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jpmml.model.ToStringHelper;

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class EvaluationRequest extends SimpleRequest {

	private String id = null;

	private Map<String, ?> arguments = null;


	public EvaluationRequest(){
	}

	public EvaluationRequest(String id){
		setId(id);
	}

	@Override
	protected ToStringHelper toStringHelper(){
		return super.toStringHelper()
			.add("id", getId())
			.add("arguments", getArguments());
	}

	public String getId(){
		return this.id;
	}

	public EvaluationRequest setId(String id){
		this.id = id;

		return this;
	}

	public Map<String, ?> getArguments(){
		return this.arguments;
	}

	public EvaluationRequest setArguments(Map<String, ?> arguments){
		this.arguments = arguments;

		return this;
	}
}