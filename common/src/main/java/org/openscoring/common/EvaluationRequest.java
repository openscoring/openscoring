/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class EvaluationRequest {

	private Map<String, ?> parameters = null;


	public Object getParameter(String key){
		return this.parameters.get(key);
	}

	public Map<String, ?> getParameters(){
		return this.parameters;
	}

	public void setParameters(Map<String, ?> parameters){
		this.parameters = parameters;
	}
}