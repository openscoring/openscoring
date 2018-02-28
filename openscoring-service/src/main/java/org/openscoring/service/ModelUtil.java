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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.openscoring.common.Field;

public class ModelUtil {

	private ModelUtil(){
	}

	static
	public Map<String, List<Field>> encodeSchema(ModelEvaluator<?> evaluator){
		Map<String, List<Field>> result = new LinkedHashMap<>();

		List<InputField> activeFields = evaluator.getActiveFields();
		List<InputField> groupFields = Collections.emptyList();

		if(evaluator instanceof HasGroupFields){
			HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

			groupFields = hasGroupFields.getGroupFields();
		}

		result.put("activeFields", encodeInputFields(activeFields));
		result.put("groupFields", encodeInputFields(groupFields));

		List<TargetField> targetFields = evaluator.getTargetFields();

		result.put("targetFields", encodeTargetFields(targetFields));

		List<OutputField> outputFields = evaluator.getOutputFields();

		result.put("outputFields", encodeOutputFields(outputFields, evaluator));

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
				field.setValues(encodeValues(dataField));

				return field;
			}
		};

		List<Field> fields = new ArrayList<>(Lists.transform(inputFields, function));

		return fields;
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
				field.setValues(encodeValues(dataField));

				return field;
			}
		};

		List<Field> fields = new ArrayList<>(Lists.transform(targetFields, function));

		return fields;
	}

	static
	private List<Field> encodeOutputFields(List<OutputField> outputFields, final ModelEvaluator<?> evaluator){
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

		List<Field> fields = new ArrayList<>(Lists.transform(outputFields, function));

		return fields;
	}

	static
	private List<String> encodeValues(DataField dataField){
		List<String> result = new ArrayList<>();

		List<Interval> intervals = dataField.getIntervals();
		for(Interval interval : intervals){
			StringBuilder sb = new StringBuilder();

			Double leftMargin = interval.getLeftMargin();
			sb.append(Double.toString(leftMargin != null ? leftMargin : Double.NEGATIVE_INFINITY));

			sb.append(", ");

			Double rightMargin = interval.getRightMargin();
			sb.append(Double.toString(rightMargin != null ? rightMargin : Double.POSITIVE_INFINITY));

			String value = sb.toString();

			Interval.Closure closure = interval.getClosure();
			switch(closure){
				case OPEN_OPEN:
					result.add("(" + value + ")");
					break;
				case OPEN_CLOSED:
					result.add("(" + value + "]");
					break;
				case CLOSED_OPEN:
					result.add("[" + value + ")");
					break;
				case CLOSED_CLOSED:
					result.add("[" + value + "]");
					break;
				default:
					break;
			}
		}

		List<Value> values = dataField.getValues();
		for(Value value : values){
			Value.Property property = value.getProperty();

			switch(property){
				case VALID:
					result.add(value.getValue());
					break;
				default:
					break;
			}
		}

		return result;
	}
}