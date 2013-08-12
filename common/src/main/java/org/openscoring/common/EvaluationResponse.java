/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class EvaluationResponse {

	private Map<String, ?> result = null;


	public Map<String, ?> getResult(){
		return this.result;
	}

	public void setResult(Map<String, ?> result){
		this.result = result;
	}
}