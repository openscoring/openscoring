/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.io.Serializable;

import org.jpmml.model.ToStringHelper;

public class TableFormat implements Serializable {

	private String charset = null;

	private char delimiterChar;

	private char quoteChar;


	public TableFormat(){
	}

	@Override
	public String toString(){
		ToStringHelper helper = new ToStringHelper(this)
			.add("charset", getCharset())
			.add("delimiterChar", getDelimiterChar())
			.add("quoteChar", getQuoteChar());

		return helper.toString();
	}

	public String getCharset(){
		return this.charset;
	}

	public TableFormat setCharset(String charset){
		this.charset = charset;

		return this;
	}

	public char getDelimiterChar(){
		return this.delimiterChar;
	}

	public TableFormat setDelimiterChar(char delimiterChar){
		this.delimiterChar = delimiterChar;

		return this;
	}

	public char getQuoteChar(){
		return this.quoteChar;
	}

	public TableFormat setQuoteChar(char quoteChar){
		this.quoteChar = quoteChar;

		return this;
	}
}