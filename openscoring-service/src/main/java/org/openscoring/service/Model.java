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
package org.openscoring.service;

import java.util.List;
import java.util.Map;

import org.jpmml.evaluator.ModelEvaluator;
import org.openscoring.common.Field;

public class Model {

	private ModelEvaluator<?> evaluator = null;

	private Map<String, List<Field>> schema = null;


	public Model(ModelEvaluator<?> evaluator){
		setEvaluator(evaluator);
		setSchema(ModelUtil.encodeSchema(evaluator));
	}

	public ModelEvaluator<?> getEvaluator(){
		return this.evaluator;
	}

	private void setEvaluator(ModelEvaluator<?> evaluator){
		this.evaluator = evaluator;
	}

	public String getSummary(){
		ModelEvaluator<?> evaluator = getEvaluator();

		return evaluator.getSummary();
	}

	public Map<String, List<Field>> getSchema(){
		return this.schema;
	}

	private void setSchema(Map<String, List<Field>> schema){
		this.schema = schema;
	}
}