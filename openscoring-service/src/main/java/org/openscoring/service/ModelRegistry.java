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

	private ConcurrentMap<ModelRef, Model> models = new ConcurrentHashMap<>();


	public ModelRegistry(){
	}

	public Collection<Map.Entry<ModelRef, Model>> entries(){
		return this.models.entrySet();
	}

	public Model get(ModelRef modelRef){
		return get(modelRef, false);
	}

	public Model get(ModelRef modelRef, boolean touch){
		Model model = this.models.get(modelRef);

		if(model != null && touch){
			model.putProperty(Model.PROPERTY_ACCESSED_TIMESTAMP, new Date());
		}

		return model;
	}

	public boolean put(ModelRef modelRef, Model model){
		Model oldModel = this.models.putIfAbsent(modelRef, Objects.requireNonNull(model));

		return (oldModel == null);
	}

	public boolean replace(ModelRef modelRef, Model oldModel, Model model){
		return this.models.replace(modelRef, oldModel, Objects.requireNonNull(model));
	}

	public boolean remove(ModelRef modelRef, Model model){
		return this.models.remove(modelRef, model);
	}
}