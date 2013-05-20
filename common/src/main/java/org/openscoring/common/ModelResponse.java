/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.common;

import java.util.*;

public class ModelResponse {

	private Map<String, Object> result = null;


	public Map<String, Object> getResult(){
		return this.result;
	}

	public void setResult(Map<String, Object> result){
		this.result = result;
	}
}