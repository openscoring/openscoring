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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jpmml.model.ToStringHelper;

@JsonInclude (
	value = JsonInclude.Include.NON_EMPTY
)
public class TableEvaluationResponse extends SimpleResponse {

	private char delimiterChar;

	private char quoteChar;

	private List<String> columns = null;

	private List<EvaluationResponse> responses = null;


	public TableEvaluationResponse(char delimiterChar, char quoteChar){
		setDelimiterChar(delimiterChar);
		setQuoteChar(quoteChar);
	}

	public String getIdColumn(){
		List<String> columns = getColumns();

		if(columns != null && columns.size() > 0){
			String column = columns.get(0);

			if(("id").equalsIgnoreCase(column)){
				return column;
			}
		}

		return null;
	}

	@Override
	protected ToStringHelper toStringHelper(){
		return super.toStringHelper()
			.add("delimiterChar", getDelimiterChar())
			.add("quoteChar", getQuoteChar())
			.add("columns", getColumns())
			.add("responses", getResponses());
	}

	public char getDelimiterChar(){
		return this.delimiterChar;
	}

	public TableEvaluationResponse setDelimiterChar(char delimiterChar){
		this.delimiterChar = delimiterChar;

		return this;
	}

	public char getQuoteChar(){
		return this.quoteChar;
	}

	public TableEvaluationResponse setQuoteChar(char quoteChar){
		this.quoteChar = quoteChar;

		return this;
	}

	public List<String> getColumns(){
		return this.columns;
	}

	public TableEvaluationResponse setColumns(List<String> columns){
		this.columns = columns;

		return this;
	}

	public List<EvaluationResponse> getResponses(){
		return this.responses;
	}

	public TableEvaluationResponse setResponses(List<EvaluationResponse> responses){
		this.responses = responses;

		return this;
	}
}