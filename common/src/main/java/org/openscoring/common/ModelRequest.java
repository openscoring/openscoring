/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class ModelRequest {

	private Map<String, String> parameters = null;


	public String getParameter(String key){
		return this.parameters.get(key);
	}

	public Map<String, String> getParameters(){
		return this.parameters;
	}

	public void setParameters(Map<String, String> parameters){
		this.parameters = parameters;
	}
}