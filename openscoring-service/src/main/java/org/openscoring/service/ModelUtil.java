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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.OutputUtil;
import org.jpmml.evaluator.TypeUtil;
import org.openscoring.common.Field;

public class ModelUtil {

	private ModelUtil(){
	}

	static
	public Map<String, List<Field>> encodeSchema(ModelEvaluator<?> evaluator){
		Map<String, List<Field>> result = Maps.newLinkedHashMap();

		List<FieldName> activeFields = evaluator.getActiveFields();
		List<FieldName> groupFields = evaluator.getGroupFields();
		List<FieldName> targetFields = evaluator.getTargetFields();

		if(targetFields.isEmpty()){
			targetFields = Collections.singletonList(evaluator.getTargetField());
		}

		result.put("activeFields", encodeMiningFields(activeFields, evaluator));
		result.put("groupFields", encodeMiningFields(groupFields, evaluator));
		result.put("targetFields", encodeMiningFields(targetFields, evaluator));

		List<FieldName> outputFields = evaluator.getOutputFields();

		result.put("outputFields", encodeOutputFields(outputFields, evaluator));

		return result;
	}

	static
	private List<Field> encodeMiningFields(List<FieldName> names, ModelEvaluator<?> evaluator){
		List<Field> fields = Lists.newArrayList();

		for(FieldName name : names){
			DataField dataField = evaluator.getDataField(name);

			// A "phantom" default target field
			if(dataField == null){
				continue;
			}

			DataType dataType = dataField.getDataType();

			OpType opType = null;

			MiningField miningField = evaluator.getMiningField(name);
			if(miningField != null){
				opType = miningField.getOpType();
			} // End if

			if(opType == null){
				opType = dataField.getOpType();
			} // End if

			if(name == null){
				name = ModelResource.DEFAULT_NAME;
			}

			Field field = new Field(name.getValue());
			field.setName(dataField.getDisplayName());
			field.setDataType(dataType);
			field.setOpType(opType);
			field.setValues(encodeValues(dataField));

			fields.add(field);
		}

		return fields;
	}

	static
	private List<Field> encodeOutputFields(List<FieldName> names, ModelEvaluator<?> evaluator){
		List<Field> fields = Lists.newArrayList();

		for(FieldName name : names){
			OutputField outputField = evaluator.getOutputField(name);

			DataType dataType = null;

			OpType opType = null;

			try {
				dataType = OutputUtil.getDataType(outputField, evaluator);

				opType = outputField.getOpType();
				if(opType == null){
					opType = TypeUtil.getOpType(dataType);
				}
			} catch(Exception e){
				// Ignored
			}

			Field field = new Field(name.getValue());
			field.setName(outputField.getDisplayName());
			field.setDataType(dataType);
			field.setOpType(opType);

			fields.add(field);
		}

		return fields;
	}

	static
	private List<String> encodeValues(DataField dataField){
		List<String> result = Lists.newArrayList();

		List<Interval> intervals = dataField.getIntervals();
		for(Interval interval : intervals){
			StringBuffer sb = new StringBuffer();

			Double leftMargin = interval.getLeftMargin();
			sb.append(leftMargin != null ? leftMargin : "-\u221e");

			sb.append(", ");

			Double rightMargin = interval.getRightMargin();
			sb.append(rightMargin != null ? rightMargin : "\u221e");

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