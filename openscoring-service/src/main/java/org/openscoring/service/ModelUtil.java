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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.HasContinuousDomain;
import org.dmg.pmml.HasDiscreteDomain;
import org.dmg.pmml.Interval;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelField;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TypeUtil;
import org.openscoring.common.Field;

public class ModelUtil {

	private ModelUtil(){
	}

	static
	public Map<String, List<Field>> encodeSchema(Evaluator evaluator){
		Map<String, List<Field>> result = new LinkedHashMap<>();

		List<InputField> inputFields = evaluator.getInputFields();

		result.put("inputFields", encodeModelFields(inputFields));

		List<InputField> groupFields = Collections.emptyList();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			groupFields = hasGroupFields.getGroupFields();
		}

		result.put("groupFields", encodeModelFields(groupFields));

		List<TargetField> targetFields = evaluator.getTargetFields();

		result.put("targetFields", encodeModelFields(targetFields));

		List<OutputField> outputFields = evaluator.getOutputFields();

		result.put("outputFields", encodeModelFields(outputFields));

		return result;
	}

	static
	private List<Field> encodeModelFields(List<? extends ModelField> modelFields){
		Function<ModelField, Field> function = new Function<ModelField, Field>(){

			@Override
			public Field apply(ModelField modelField){
				org.dmg.pmml.Field<?> pmmlField = modelField.getField();

				FieldName name = modelField.getName();

				Field field = new Field(name.getValue());
				field.setName(modelField.getDisplayName());
				field.setOpType(modelField.getOpType());
				field.setDataType(modelField.getDataType());

				List<String> values = new ArrayList<>();

				if(pmmlField instanceof HasContinuousDomain){
					values.addAll(encodeContinuousDomain((org.dmg.pmml.Field & HasContinuousDomain)pmmlField));
				} // End if

				if(pmmlField instanceof HasDiscreteDomain){
					values.addAll(encodeDiscreteDomain((org.dmg.pmml.Field & HasDiscreteDomain)pmmlField));
				}

				field.setValues(values);

				return field;
			}
		};

		return modelFields.stream()
			.map(function)
			.collect(Collectors.toList());
	}

	static
	private <F extends org.dmg.pmml.Field<F> & HasContinuousDomain<F>> List<String> encodeContinuousDomain(F field){

		if(field.hasIntervals()){
			List<Interval> intervals = field.getIntervals();

			Function<Interval, String> function = new Function<Interval, String>(){

				@Override
				public String apply(Interval interval){
					Number leftMargin = interval.getLeftMargin();
					Number rightMargin = interval.getRightMargin();

					String value = (leftMargin != null ? leftMargin : Double.NEGATIVE_INFINITY) + ", " + (rightMargin != null ? rightMargin : Double.POSITIVE_INFINITY);

					Interval.Closure closure = interval.getClosure();
					switch(closure){
						case OPEN_OPEN:
							return "(" + value + ")";
						case OPEN_CLOSED:
							return "(" + value + "]";
						case CLOSED_OPEN:
							return "[" + value + ")";
						case CLOSED_CLOSED:
							return "[" + value + "]";
						default:
							throw new IllegalArgumentException();
					}
				}
			};

			return intervals.stream()
				.map(function)
				.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	static
	private <F extends org.dmg.pmml.Field<F> & HasDiscreteDomain<F>> List<String> encodeDiscreteDomain(F field){

		if(field.hasValues()){
			List<Value> values = field.getValues();

			return values.stream()
				.filter(value -> (Value.Property.VALID).equals(value.getProperty()))
				.map(value -> TypeUtil.format(value.getValue()))
				.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}
}