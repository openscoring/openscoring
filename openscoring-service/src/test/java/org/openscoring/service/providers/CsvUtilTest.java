/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.openscoring.service.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;
import org.openscoring.service.providers.CsvUtil;
import org.supercsv.prefs.CsvPreference;

import static org.junit.Assert.assertNotSame;

public class CsvUtilTest {

	@Test
	public void getFormat() throws IOException {
		CsvPreference first;
		CsvPreference second;

		String csv = "1\tone\n" +
			"2\ttwo\n" +
			"3\tthree";

		try(BufferedReader reader = new BufferedReader(new StringReader(csv))){
			first = CsvUtil.getFormat(reader);
			second = CsvUtil.getFormat(reader);
		}

		assertNotSame(first.getEncoder(), second.getEncoder());
	}
}