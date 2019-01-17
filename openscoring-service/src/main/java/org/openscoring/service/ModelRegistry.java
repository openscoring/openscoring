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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.HasPMML;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.model.JAXBUtil;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class ModelRegistry {

	private LoadingModelEvaluatorBuilder modelEvaluatorBuilder = null;

	private ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();


	@Inject
	public ModelRegistry(LoadingModelEvaluatorBuilder modelEvaluatorBuilder){
		this.modelEvaluatorBuilder = modelEvaluatorBuilder;
	}

	public Collection<Map.Entry<String, Model>> entries(){
		return this.models.entrySet();
	}

	@SuppressWarnings (
		value = {"resource"}
	)
	public Model load(InputStream is) throws Exception {
		CountingInputStream countingIs = new CountingInputStream(is);

		HashingInputStream hashingIs = new HashingInputStream(Hashing.md5(), countingIs);

		Evaluator evaluator = this.modelEvaluatorBuilder.clone()
			.load(hashingIs)
			.build();

		evaluator.verify();

		Model model = new Model(evaluator);
		model.putProperty(Model.PROPERTY_FILE_SIZE, countingIs.getCount());
		model.putProperty(Model.PROPERTY_FILE_MD5SUM, (hashingIs.hash()).toString());

		return model;
	}

	public void store(Model model, OutputStream os) throws JAXBException {
		Evaluator evaluator = model.getEvaluator();

		if(evaluator instanceof HasPMML){
			HasPMML hasPMML = (HasPMML)evaluator;

			PMML pmml = hasPMML.getPMML();

			Result result = new StreamResult(os);

			Marshaller marshaller = JAXBUtil.createMarshaller();

			marshaller.marshal(pmml, result);
		}
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