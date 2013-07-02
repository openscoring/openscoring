/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.server;

import java.io.*;
import java.util.*;

import org.openscoring.common.*;

import org.supercsv.io.*;
import org.supercsv.prefs.*;

public class CsvUtil {

	private CsvUtil(){
	}

	static
	public CsvPreference getFormat(BufferedReader reader) throws IOException {
		reader.mark(10 * 1024);

		CsvPreference[] formats = {CsvPreference.EXCEL_PREFERENCE, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE, CsvPreference.TAB_PREFERENCE};
		for(CsvPreference format : formats){

			try {
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
		CsvListReader parser = new CsvListReader(reader, format);

		int columns = 0;

		// Check the header line and the first ten lines
		for(int line = 0; line < (1 + 10); line++){
			List<String> row = parser.read();
			if(row == null){
				break;
			} // End if

			if(columns == 0 || columns == row.size()){
				columns = row.size();
			} else

			{
				columns = -1;

				break;
			}
		}

		return (columns > 2);
	}

	static
	public List<EvaluationRequest> readTable(BufferedReader reader, CsvPreference format) throws IOException {
		List<EvaluationRequest> requests = new ArrayList<EvaluationRequest>();

		CsvMapReader parser = new CsvMapReader(reader, format);

		String[] header = parser.getHeader(true);

		while(true){
			Map<String, String> arguments = parser.read(header);
			if(arguments == null){
				break;
			}

			EvaluationRequest request = new EvaluationRequest();
			request.setArguments(arguments);

			requests.add(request);
		}

		parser.close();

		return requests;
	}

	static
	public void writeTable(BufferedWriter writer, CsvPreference format, List<EvaluationResponse> responses) throws IOException {
		CsvMapWriter formatter = new CsvMapWriter(writer, format);

		String[] header = null;

		for(EvaluationResponse response : responses){
			Map<String, Object> result = response.getResult();

			if(header == null){
				Set<String> keys = result.keySet();

				header = (keys).toArray(new String[keys.size()]);

				formatter.writeHeader(header);
			}

			formatter.write(result, header);
		}

		formatter.flush();
		formatter.close();
	}
}