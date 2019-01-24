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
package org.openscoring.service;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ModelRegistry {

	private ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();


	public ModelRegistry(){
	}

	public Collection<Map.Entry<String, Model>> entries(){
		return this.models.entrySet();
	}

	public Model get(String id){
		return get(id, false);
	}

	public Model get(String id, boolean touch){
		Model model = this.models.get(id);

		if(model != null && touch){
			model.putProperty(Model.PROPERTY_ACCESSED_TIMESTAMP, new Date());
		}

		return model;
	}

	public boolean put(String id, Model model){
		Model oldModel = this.models.putIfAbsent(id, Objects.requireNonNull(model));

		return (oldModel == null);
	}

	public boolean replace(String id, Model oldModel, Model model){
		return this.models.replace(id, oldModel, Objects.requireNonNull(model));
	}

	public boolean remove(String id, Model model){
		return this.models.remove(id, model);
	}

	static
	public boolean validateId(String id){
		return (id != null && (id).matches(ID_REGEX));
	}

	public static final String ID_REGEX = "[a-zA-Z0-9][a-zA-Z0-9\\_\\-]*";
}