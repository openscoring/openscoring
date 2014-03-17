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
package org.openscoring.server;

import java.util.*;

import org.dmg.pmml.*;

import com.google.common.collect.*;

public class ModelRegistry {

	private Map<String, PMML> models = Maps.newTreeMap(new Comparator<String>(){

		@Override
		public int compare(String left, String right){
			return (left).compareToIgnoreCase(right);
		}
	});


	public Set<String> idSet(){
		return Collections.unmodifiableSet(this.models.keySet());
	}

	public PMML get(String id){
		return this.models.get(id);
	}

	public PMML put(String id, PMML pmml){
		return this.models.put(id, pmml);
	}

	public PMML remove(String id){
		return this.models.remove(id);
	}
}