/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class EvaluationRequest {

	private Map<String, ?> arguments = null;


	public Object getArgument(String key){
		return this.arguments.get(key);
	}

	public Map<String, ?> getArguments(){
		return this.arguments;
	}

	public void setArguments(Map<String, ?> arguments){
		this.arguments = arguments;
	}
}