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
import java.util.concurrent.*;

import javax.inject.*;
import javax.xml.bind.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.jpmml.evaluator.*;
import org.jpmml.manager.*;
import org.jpmml.model.*;

import com.google.common.base.*;
import com.google.common.collect.*;

import org.dmg.pmml.*;

import org.jvnet.hk2.annotations.*;

import org.xml.sax.*;

@Service
@Singleton
public class ModelRegistry {

	private ConcurrentMap<String, ModelEvaluator<?>> models = Maps.<String, ModelEvaluator<?>>newConcurrentMap();


	public Collection<Map.Entry<String, ModelEvaluator<?>>> entries(){
		return this.models.entrySet();
	}

	public ModelEvaluator<?> get(String id){
		return this.models.get(id);
	}

	public boolean put(String id, ModelEvaluator<?> evaluator){
		ModelEvaluator<?> oldEvaluator = this.models.putIfAbsent(id, Preconditions.checkNotNull(evaluator));

		return (oldEvaluator == null);
	}

	public boolean replace(String id, ModelEvaluator<?> oldEvaluator, ModelEvaluator<?> evaluator){
		return this.models.replace(id, oldEvaluator, Preconditions.checkNotNull(evaluator));
	}

	public boolean remove(String id, ModelEvaluator<?> evaluator){
		return this.models.remove(id, evaluator);
	}

	static
	public boolean validateId(String id){
		return (id != null && (id).matches(ID_REGEX));
	}

	static
	public ModelEvaluator<?> unmarshal(InputStream is) throws SAXException, JAXBException {
		Source source = ImportFilter.apply(new InputSource(is));

		PMML pmml = JAXBUtil.unmarshalPMML(source);

		PMMLManager pmmlManager = new PMMLManager(pmml);

		return (ModelEvaluator<?>)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
	}

	static
	public void marshal(ModelEvaluator<?> evaluator, OutputStream os) throws JAXBException {
		PMML pmml = evaluator.getPMML();

		Result result = new StreamResult(os);

		JAXBUtil.marshalPMML(pmml, result);
	}

	public static final String ID_REGEX = "[a-zA-Z0-9][a-zA-Z0-9\\_\\-]*";
}