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

import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelField;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.openscoring.common.Field;

public class ModelUtil {

	private ModelUtil(){
	}

	static
	public Map<String, List<Field>> encodeSchema(Evaluator evaluator){
		Map<String, List<Field>> result = new LinkedHashMap<>();

		List<InputField> inputFields = evaluator.getInputFields();

		result.put("inputFields", encodeInputFields(inputFields));

		List<InputField> groupFields = Collections.emptyList();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			groupFields = hasGroupFields.getGroupFields();
		}

		result.put("groupFields", encodeInputFields(groupFields));

		List<TargetField> targetFields = evaluator.getTargetFields();

		result.put("targetFields", encodeTargetFields(targetFields));

		List<OutputField> outputFields = evaluator.getOutputFields();

		result.put("outputFields", encodeOutputFields(outputFields));

		return result;
	}

	static
	private List<Field> encodeInputFields(List<InputField> inputFields){
		Function<InputField, Field> function = new Function<InputField, Field>(){

			@Override
			public Field apply(InputField inputField){
				FieldName name = inputField.getName();

				DataField dataField = (DataField)inputField.getField();

				Field field = new Field(name.getValue());
				field.setName(dataField.getDisplayName());
				field.setDataType(inputField.getDataType());
				field.setOpType(inputField.getOpType());
				field.setValues(encodeDomain(dataField));

				return field;
			}
		};

		return encodeFields(inputFields, function);
	}

	static
	private List<Field> encodeTargetFields(List<TargetField> targetFields){
		Function<TargetField, Field> function = new Function<TargetField, Field>(){

			@Override
			public Field apply(TargetField targetField){
				FieldName name = targetField.getName();

				// A "phantom" default target field
				if(targetField.isSynthetic()){
					name = ModelResource.DEFAULT_NAME;
				}

				DataField dataField = targetField.getDataField();

				Field field = new Field(name.getValue());
				field.setName(dataField.getDisplayName());
				field.setDataType(targetField.getDataType());
				field.setOpType(targetField.getOpType());
				field.setValues(encodeDomain(dataField));

				return field;
			}
		};

		return encodeFields(targetFields, function);
	}

	static
	private List<Field> encodeOutputFields(List<OutputField> outputFields){
		Function<OutputField, Field> function = new Function<OutputField, Field>(){

			@Override
			public Field apply(OutputField outputField){
				FieldName name = outputField.getName();

				org.dmg.pmml.OutputField pmmlOutputField = outputField.getOutputField();

				Field field = new Field(name.getValue());
				field.setName(pmmlOutputField.getDisplayName());
				field.setDataType(outputField.getDataType());
				field.setOpType(outputField.getOpType());

				return field;
			}
		};

		return encodeFields(outputFields, function);
	}

	static
	private <F extends ModelField> List<Field> encodeFields(List<? extends F> fields, Function<F, Field> function){
		return fields.stream()
			.map(function)
			.collect(Collectors.toList());
	}

	static
	private List<String> encodeDomain(DataField dataField){
		List<String> result = new ArrayList<>();

		if(dataField.hasIntervals()){
			List<Interval> intervals = dataField.getIntervals();

			Function<Interval, String> function = new Function<Interval, String>(){

				@Override
				public String apply(Interval interval){
					Double leftMargin = interval.getLeftMargin();
					Double rightMargin = interval.getRightMargin();

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

			intervals.stream()
				.map(function)
				.forEach(result::add);
		} // End if

		if(dataField.hasValues()){
			List<Value> values = dataField.getValues();

			values.stream()
				.filter(value -> (Value.Property.VALID).equals(value.getProperty()))
				.map(Value::getValue)
				.forEach(result::add);
		}

		return result;
	}
}