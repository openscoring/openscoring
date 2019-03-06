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

import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class ModelRegistry {

	private ConcurrentMap<Principal, ConcurrentMap<String, Model>> models = new ConcurrentHashMap<>();

	private Function<Principal, ConcurrentMap<String, Model>> initializer = new Function<Principal, ConcurrentMap<String, Model>>(){

		@Override
		public ConcurrentMap<String, Model> apply(Principal principal){
			return new ConcurrentHashMap<>();
		}
	};


	public ModelRegistry(){
	}

	public Map<String, Model> getModels(Principal owner){
		return this.models.computeIfAbsent(owner, getInitializer());
	}

	public Model get(ModelRef modelRef){
		return get(modelRef, false);
	}

	public Model get(ModelRef modelRef, boolean touch){
		Map<String, Model> models = getModels(modelRef.getOwner());

		Model model = models.get(modelRef.getId());
		if(model != null && touch){
			model.putProperty(Model.PROPERTY_ACCESSED_TIMESTAMP, new Date());
		}

		return model;
	}

	public boolean put(ModelRef modelRef, Model model){
		Map<String, Model> models = getModels(modelRef.getOwner());

		Model oldModel = models.putIfAbsent(modelRef.getId(), Objects.requireNonNull(model));

		return (oldModel == null);
	}

	public boolean replace(ModelRef modelRef, Model oldModel, Model model){
		Map<String, Model> models = getModels(modelRef.getOwner());

		return models.replace(modelRef.getId(), oldModel, Objects.requireNonNull(model));
	}

	public boolean remove(ModelRef modelRef, Model model){
		Map<String, Model> models = getModels(modelRef.getOwner());

		return models.remove(modelRef.getId(), model);
	}

	public Function<Principal, ConcurrentMap<String, Model>> getInitializer(){
		return this.initializer;
	}

	public void setInitializer(Function<Principal, ConcurrentMap<String, Model>> initializer){
		this.initializer = Objects.requireNonNull(initializer);
	}
}