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
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.jpmml.evaluator.*;
import org.jpmml.manager.*;
import org.jpmml.model.*;

import com.google.common.collect.*;

import org.dmg.pmml.*;

import org.jvnet.hk2.annotations.*;

import org.xml.sax.*;

@Service
@Singleton
public class ModelRegistry {

	private Map<String, ModelEvaluator<?>> models = Maps.<String, ModelEvaluator<?>>newConcurrentMap();


	public Set<String> keySet(){
		return Collections.unmodifiableSet(this.models.keySet());
	}

	public ModelEvaluator<?> get(String id){
		return this.models.get(id);
	}

	public ModelEvaluator<?> put(String id, PMML pmml){
		return put(id, createModelEvaluator(pmml));
	}

	public ModelEvaluator<?> put(String id, ModelEvaluator<?> evaluator){
		return this.models.put(id, evaluator);
	}

	public ModelEvaluator<?> remove(String id){
		return this.models.remove(id);
	}

	static
	public PMML unmarshal(InputStream is) throws SAXException, JAXBException {
		Source source = ImportFilter.apply(new InputSource(is));

		return JAXBUtil.unmarshalPMML(source);
	}

	static
	public void marshal(PMML pmml, OutputStream os) throws JAXBException {
		Result result = new StreamResult(os);

		JAXBUtil.marshalPMML(pmml, result);
	}

	static
	private ModelEvaluator<?> createModelEvaluator(PMML pmml){
		PMMLManager pmmlManager = new PMMLManager(pmml);

		return (ModelEvaluator<?>)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
	}
}