/*
 * Copyright (c) 2016 Villu Ruusmann
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

import com.google.common.annotations.GwtIncompatible;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import org.dmg.pmml.MiningFunctionType;
import org.fusesource.restygwt.rebind.JsonEncoderDecoderClassCreator;
import org.fusesource.restygwt.rebind.RestyJsonSerializerGenerator;

@GwtIncompatible (
	value = "MiningFunctionTypeRestySerializerGenerator"
)
public class MiningFunctionTypeRestySerializerGenerator implements RestyJsonSerializerGenerator {

	@Override
	public Class<? extends JsonEncoderDecoderClassCreator> getGeneratorClass(){
		return EnumSerializerGenerator.class;
	}

	@Override
	public JType getType(TypeOracle typeOracle){
		return typeOracle.findType(MiningFunctionType.class.getName());
	}
}