/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class SummaryResponse {

	private List<String> activeFields = null;

	private List<String> predictedFields = null;


	public List<String> getActiveFields(){
		return this.activeFields;
	}

	public void setActiveFields(List<String> activeFields){
		this.activeFields = activeFields;
	}

	public List<String> getPredictedFields(){
		return this.predictedFields;
	}

	public void setPredictedFields(List<String> predictedFields){
		this.predictedFields = predictedFields;
	}
}