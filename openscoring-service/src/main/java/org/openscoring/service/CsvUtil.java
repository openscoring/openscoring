/*
 * Copyright (c) 2013 Villu Ruusmann
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.TableEvaluationRequest;
import org.openscoring.common.TableEvaluationResponse;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.prefs.CsvPreference;

public class CsvUtil {

	private CsvUtil(){
	}

	static
	public CsvPreference getFormat(String delimiterChar, String quoteChar){
		char delimiter = ',';
		char quote = '\"';

		if(delimiterChar != null){
			delimiterChar = decodeDelimiter(delimiterChar);

			if(delimiterChar.length() != 1){
				throw new IllegalArgumentException("Invalid CSV delimiter character: \"" + delimiterChar + "\"");
			}

			delimiter = delimiterChar.charAt(0);
		} // End if

		if(quoteChar != null){
			quoteChar = decodeQuote(quoteChar);

			if(quoteChar.length() != 1){
				throw new IllegalArgumentException("Invalid CSV quote character: \"" + quoteChar + "\"");
			}

			quote = quoteChar.charAt(0);
		}

		CsvPreference format = createFormat(delimiter, quote);

		return format;
	}

	static
	public CsvPreference getFormat(BufferedReader reader) throws IOException {
		reader.mark(10 * 1024);

		for(int i = 0; i < CsvUtil.DELIMITERS.length; i++){
			char delimiter = CsvUtil.DELIMITERS[i];

			try {
				CsvPreference format = createFormat(delimiter, '\"');

				if(checkFormat(reader, format)){
					return format;
				}
			} finally {
				reader.reset();
			}
		}

		throw new IOException("Unrecognized CSV format");
	}

	static
	private boolean checkFormat(BufferedReader reader, CsvPreference format) throws IOException {
		CsvListReader parser = new CsvListReader(reader, format){

			@Override
			public void close(){
			}
		};

		int columns = 0;

		// Check the header line and the first ten lines
		for(int line = 0; line < (1 + 10); line++){
			List<String> row = parser.read();

			if(row == null){
				break;
			}

			int rowColumns = row.size();
			if((rowColumns > 1) && (columns == 0 || columns == rowColumns)){
				columns = rowColumns;
			} else

			{
				return false;
			}
		}

		parser.close();

		return (columns > 1);
	}

	static
	public TableEvaluationRequest readTable(BufferedReader reader, CsvPreference format) throws IOException {
		CsvMapReader parser = new CsvMapReader(reader, format);

		String[] header = parser.getHeader(true);

		List<String> columns = Arrays.asList(header);

		TableEvaluationRequest tableRequest = new TableEvaluationRequest((char)format.getDelimiterChar(), format.getQuoteChar())
			.setColumns(columns);

		String idColumn = tableRequest.getIdColumn();

		List<EvaluationRequest> requests = new ArrayList<>();

		while(true){
			Map<String, String> row = parser.read(header);
			if(row == null){
				break;
			}

			String id = null;

			if(idColumn != null){
				id = row.remove(idColumn);
			}

			EvaluationRequest request = new EvaluationRequest(id)
				.setArguments(row);

			requests.add(request);
		}

		tableRequest.setRequests(requests);

		parser.close();

		return tableRequest;
	}

	static
	public void writeTable(TableEvaluationResponse tableResponse, BufferedWriter writer) throws IOException {
		CsvPreference format = createFormat(tableResponse.getDelimiterChar(), tableResponse.getQuoteChar());

		CsvMapWriter formatter = new CsvMapWriter(writer, format);

		String idColumn = tableResponse.getIdColumn();
		List<String> columns = tableResponse.getColumns();

		String[] header = columns.toArray(new String[columns.size()]);

		formatter.writeHeader(header);

		List<EvaluationResponse> responses = tableResponse.getResponses();

		for(EvaluationResponse response : responses){
			Map<String, Object> row = (Map)response.getResults();

			if(idColumn != null){
				row.put(idColumn, response.getId());
			}

			formatter.write(row, header);
		}

		formatter.flush();
		formatter.close();
	}

	static
	private CsvPreference createFormat(char delimiterChar, char quoteChar){
		CsvPreference.Builder builder = new CsvPreference.Builder(quoteChar, delimiterChar, "\n");
		builder.useEncoder(new DefaultCsvEncoder());

		return builder.build();
	}

	static
	private String decodeDelimiter(String delimiterChar){

		if(("\\t").equals(delimiterChar)){
			return "\t";
		}

		return delimiterChar;
	}

	static
	private String decodeQuote(String quoteChar){

		if(("\\'").equals(quoteChar)){
			return "\'";
		} else

		if(("\\\"").equals(quoteChar)){
			return "\"";
		}

		return quoteChar;
	}

	private static final char[] DELIMITERS = {',', ';', '\t'};
}