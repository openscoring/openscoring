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

import com.google.common.annotations.GwtIncompatible;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import org.fusesource.restygwt.rebind.JsonEncoderDecoderClassCreator;
import org.fusesource.restygwt.rebind.JsonEncoderDecoderInstanceLocator;

@GwtIncompatible (
	value = "EnumSerializerGenerator"
)
public class EnumSerializerGenerator extends JsonEncoderDecoderClassCreator {

	public EnumSerializerGenerator(TreeLogger logger, GeneratorContext context, JClassType source){
		super(logger, context, source);
	}

	@Override
	public void generate() throws UnableToCompleteException {
		super.locator = new JsonEncoderDecoderInstanceLocator(super.context, getLogger());

		generateSingleton(super.shortName);

		generateEncodeMethod();
		generateDecodeMethod();
	}

	private void generateEncodeMethod(){
		p("public " + JSON_VALUE_CLASS + " encode(" + super.source.getParameterizedQualifiedSourceName() + " value){").i(1);
			p("throw new EncodingException(\"Not supported\");").i(-1);
		p("}");

		p();
	}

	private void generateDecodeMethod(){
		p("public " + super.source.getName() + " decode(" + JSON_VALUE_CLASS + " value){").i(1);
			p("if(value == null || value.isNull() != null){").i(1);
				p("return null;").i(-1);
			p("}");

			p(JSON_STRING_CLASS + " string = value.isString();");
			p("if(string == null){").i(1);
				p("throw new DecodingException(\"The value must be a string object\");").i(-1);
			p("}");

			p("return " + super.source.getName() + ".fromValue(string.stringValue());").i(-1);
		p("}");

		p();
	}
}