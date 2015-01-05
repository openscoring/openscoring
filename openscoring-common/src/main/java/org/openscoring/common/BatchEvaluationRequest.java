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
package org.openscoring.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class BatchEvaluationRequest extends SimpleRequest {

	private String id = null;

	private List<EvaluationRequest> requests = null;


	public BatchEvaluationRequest(){
	}

	public BatchEvaluationRequest(String id){
		setId(id);
	}

	@Override
	public String toString(){
		ToStringHelper stringHelper = MoreObjects.toStringHelper(getClass())
			.add("id", getId())
			.add("requests", getRequests());

		return stringHelper.toString();
	}

	public String getId(){
		return this.id;
	}

	public void setId(String id){
		this.id = id;
	}

	public List<EvaluationRequest> getRequests(){
		return this.requests;
	}

	public void setRequests(List<EvaluationRequest> requests){
		this.requests = requests;
	}
}