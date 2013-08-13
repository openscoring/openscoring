/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class SummaryResponse {

	private List<String> activeFields = null;

	private List<String> groupFields = null;

	private List<String> predictedFields = null;

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

	public List<String> getPredictedFields(){
		return this.predictedFields;
	}

	public void setPredictedFields(List<String> predictedFields){
		this.predictedFields = predictedFields;
	}

	public List<String> getOutputFields(){
		return this.outputFields;
	}

	public void setOutputFields(List<String> outputFields){
		this.outputFields = outputFields;
	}
}