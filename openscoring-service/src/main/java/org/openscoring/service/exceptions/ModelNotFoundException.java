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
package org.openscoring.service.exceptions;

import javax.ws.rs.NotFoundException;

import org.openscoring.service.ModelRef;

public class ModelNotFoundException extends NotFoundException {

	private ModelRef modelRef = null;


	public ModelNotFoundException(ModelRef modelRef){
		super("Model \"" + modelRef.getId() + "\" not found");

		setModelRef(modelRef);
	}

	public ModelRef getModelRef(){
		return this.modelRef;
	}

	private void setModelRef(ModelRef modelRef){
		this.modelRef = modelRef;
	}
}