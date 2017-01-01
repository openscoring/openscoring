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
package org.openscoring.common;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.annotations.GwtIncompatible;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;

@GwtIncompatible (
	value = "OpenscoringModule"
)
public class OpenscoringModule extends SimpleModule {

	public OpenscoringModule(){
		addSerializer(DataType.class, new DataTypeSerializer());
		addDeserializer(DataType.class, new DataTypeDeserializer());

		addSerializer(MiningFunction.class, new MiningFunctionSerializer());
		addDeserializer(MiningFunction.class, new MiningFunctionDeserializer());

		addSerializer(OpType.class, new OpTypeSerializer());
		addDeserializer(OpType.class, new OpTypeDeserializer());
	}
}