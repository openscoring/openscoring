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
package org.openscoring.service.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.dmg.pmml.Header;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorBuilder;
import org.jpmml.evaluator.HasModel;
import org.jpmml.evaluator.HasPMML;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.model.JAXBUtil;
import org.openscoring.service.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Provider
@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN, "text/*", "*/*"})
@Produces(MediaType.APPLICATION_XML)
public class ModelProvider implements MessageBodyReader<Model>, MessageBodyWriter<Model> {

	@Context
	private UriInfo uriInfo = null;

	private LoadingModelEvaluatorBuilder modelEvaluatorBuilder = null;


	@Inject
	public ModelProvider(LoadingModelEvaluatorBuilder modelEvaluatorBuilder){
		this.modelEvaluatorBuilder = modelEvaluatorBuilder;
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
		return (Model.class).equals(type);
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
		return (Model.class).equals(type);
	}

	@Override
	public Model readFrom(Class<Model> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
		MultivaluedMap<String, String> queryParameters = this.uriInfo.getQueryParameters();

		String modelName = queryParameters.getFirst("modelName");

		CountingInputStream countingIs = new CountingInputStream(entityStream);

		HashingInputStream hashingIs = new HashingInputStream(Hashing.sha256(), countingIs);

		EvaluatorBuilder evaluatorBuilder;

		try {
			evaluatorBuilder = this.modelEvaluatorBuilder.clone()
				.load(hashingIs, modelName);
		} catch(SAXException | JAXBException e){
			logger.error("Failed to load the PMML document", e);

			throw new BadRequestException(e);
		} catch(/*PMML*/Exception pe){
			logger.error("Failed to find a scorable model element", pe);

			throw new BadRequestException(pe);
		}

		Evaluator evaluator;

		try {
			evaluator = evaluatorBuilder.build();

			evaluator.verify();
		} catch(/*PMML*/Exception pe){
			logger.error("Failed to build a model evaluator", pe);

			throw new BadRequestException(pe);
		}

		Model model = new Model(evaluator);
		model.putProperty(Model.PROPERTY_FILE_SIZE, countingIs.getCount());
		model.putProperty(Model.PROPERTY_FILE_CHECKSUM, (hashingIs.hash()).toString());

		HasModel<?> hasModel = (HasModel<?>)evaluator;

		PMML pmml = hasModel.getPMML();

		Header header = pmml.getHeader();

		model.putProperty(Model.PROPERTY_MODEL_VERSION, header != null ? header.getModelVersion() : null);

		return model;
	}

	@Override
	public void writeTo(Model model, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
		Evaluator evaluator = model.getEvaluator();

		HasPMML hasPMML = (HasPMML)evaluator;

		httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_TYPE.withCharset("UTF-8"));
		httpHeaders.putSingle(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=model.pmml.xml"); // XXX

		PMML pmml = hasPMML.getPMML();

		try {
			Result result = new StreamResult(entityStream);

			Marshaller marshaller = JAXBUtil.createMarshaller();

			marshaller.marshal(pmml, result);
		} catch(JAXBException je){
			throw new InternalServerErrorException(je);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(ModelProvider.class);
}