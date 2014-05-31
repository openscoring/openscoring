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

import java.io.*;
import java.util.*;

import javax.inject.*;
import javax.xml.bind.*;

import org.jpmml.model.*;

import com.google.common.collect.*;

import org.dmg.pmml.*;

import org.jvnet.hk2.annotations.*;

import org.xml.sax.*;

@Service
@Singleton
public class ModelRegistry {

	private Map<String, PMML> models = Maps.<String, PMML>newConcurrentMap();


	public Set<String> idSet(){
		return Collections.unmodifiableSet(this.models.keySet());
	}

	public PMML get(String id){
		return this.models.get(id);
	}

	public PMML put(String id, InputStream is) throws SAXException, JAXBException {
		InputSource source = new InputSource(is);

		PMML pmml = JAXBUtil.unmarshalPMML(ImportFilter.apply(source));

		return put(id, pmml);
	}

	public PMML put(String id, PMML pmml){
		return this.models.put(id, pmml);
	}

	public PMML remove(String id){
		return this.models.remove(id);
	}
}