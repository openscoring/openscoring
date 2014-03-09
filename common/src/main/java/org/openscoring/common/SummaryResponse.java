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

import java.util.*;

public class SummaryResponse {

	private List<String> activeFields = null;

	private List<String> groupFields = null;

	private List<String> targetFields = null;

	private List<String> outputFields = null;


	public List<String> getActiveFields(){
		return this.activeFields;
	}

	public void setActiveFields(List<String> activeFields){
		this.activeFields = activeFields;
	}

	public List<String> getGroupFields(){
		return this.groupFields;
	}

	public void setGroupFields(List<String> groupFields){
		this.groupFields = groupFields;
	}

	public List<String> getTargetFields(){
		return this.targetFields;
	}

	public void setTargetFields(List<String> targetFields){
		this.targetFields = targetFields;
	}

	public List<String> getOutputFields(){
		return this.outputFields;
	}

	public void setOutputFields(List<String> outputFields){
		this.outputFields = outputFields;
	}
}