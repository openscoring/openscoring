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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.Maps;
import org.dmg.pmml.FieldName;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.openscoring.common.BatchEvaluationRequest;
import org.openscoring.common.BatchEvaluationResponse;
import org.openscoring.common.BatchModelResponse;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.Headers;
import org.openscoring.common.ModelResponse;
import org.openscoring.common.SimpleResponse;
import org.supercsv.prefs.CsvPreference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ModelResourceTest extends JerseyTest {

	@Override
	protected Application configure(){
		Openscoring openscoring = new Openscoring();

		return openscoring;
	}

	@Override
	protected void configureClient(ClientConfig clientConfig){
		clientConfig.register(MultiPartFeature.class);

		// Ideally, should use the client-side ObjectMapperProvider class instead of the server-side one
		clientConfig.register(ObjectMapperProvider.class);
	}

	@Test
	public void decisionTreeIris() throws Exception {
		String id = "DecisionTreeIris";

		assertEquals("Iris", extractSuffix(id));

		BatchModelResponse batchModelResponse = queryBatch();

		List<ModelResponse> modelResponses = batchModelResponse.getResponses();

		assertNull(modelResponses);

		deploy(id);

		batchModelResponse = queryBatch();

		modelResponses = batchModelResponse.getResponses();

		assertEquals(1, modelResponses.size());

		download(id);

		List<EvaluationRequest> evaluationRequests = loadRecords(id);

		EvaluationRequest evaluationRequest = evaluationRequests.get(0);

		EvaluationResponse evaluationResponse = evaluate(id, evaluationRequest);

		assertEquals(evaluationRequest.getId(), evaluationResponse.getId());

		EvaluationRequest invalidEvaluationRequest = invalidate(evaluationRequests.get(50));

		evaluationRequests = Arrays.asList(evaluationRequests.get(0), invalidEvaluationRequest, evaluationRequests.get(100));

		BatchEvaluationRequest batchEvaluationRequest = new BatchEvaluationRequest();
		batchEvaluationRequest.setRequests(evaluationRequests);

		BatchEvaluationResponse batchEvaluationResponse = evaluateBatch(id, batchEvaluationRequest);

		assertEquals(batchEvaluationRequest.getId(), batchEvaluationResponse.getId());

		List<EvaluationResponse> evaluationResponses = batchEvaluationResponse.getResponses();

		assertEquals(evaluationRequests.size(), evaluationResponses.size());

		EvaluationResponse invalidEvaluationResponse = evaluationResponses.get(1);

		assertEquals(invalidEvaluationRequest.getId(), invalidEvaluationResponse.getId());
		assertNotNull(invalidEvaluationResponse.getMessage());

		undeploy(id);
	}

	@Test
	public void associationRulesShopping() throws Exception {
		String id = "AssociationRulesShopping";

		assertEquals("Shopping", extractSuffix(id));

		deployForm(id);

		query(id);

		List<EvaluationRequest> evaluationRequests = loadRecords(id);

		BatchEvaluationRequest batchEvaluationRequest = new BatchEvaluationRequest();
		batchEvaluationRequest.setRequests(evaluationRequests);

		BatchEvaluationResponse batchEvaluationResponse = evaluateBatch(id, batchEvaluationRequest);

		assertEquals(batchEvaluationRequest.getId(), batchEvaluationResponse.getId());

		List<EvaluationRequest> aggregatedEvaluationRequests = ModelResource.aggregateRequests(FieldName.create("transaction"), evaluationRequests);

		batchEvaluationRequest = new BatchEvaluationRequest("aggregate");
		batchEvaluationRequest.setRequests(aggregatedEvaluationRequests);

		batchEvaluationResponse = evaluateBatch(id, batchEvaluationRequest);

		assertEquals(batchEvaluationRequest.getId(), batchEvaluationResponse.getId());

		evaluateCsv(id);

		evaluateCsvForm(id);

		undeployForm(id);
	}

	@Test
	public void linearRegressionAuto() throws Exception {
		String id = "LinearRegressionAuto";

		assertEquals("Auto", extractSuffix(id));

		deploy(id);

		List<EvaluationRequest> evaluationRequests = loadRecords(id);

		EvaluationRequest evaluationRequest = evaluationRequests.get(0);

		EvaluationResponse evaluationResponse = evaluate(id, evaluationRequest);

		assertEquals(evaluationRequest.getId(), evaluationResponse.getId());

		Map<String, ?> result = evaluationResponse.getResult();

		assertEquals(3, result.size());

		String report = (String)result.get("report(Predicted_mpg)");

		assertTrue(report.startsWith("<math xmlns=\"http://www.w3.org/1998/Math/MathML\">") && report.endsWith("</math>"));

		undeploy(id);
	}

	private ModelResponse deploy(String id) throws IOException {
		Response response;

		try(InputStream is = openPMML(id)){
			Entity<InputStream> entity = Entity.entity(is, MediaType.APPLICATION_XML);

			response = target("model/" + id).request(MediaType.APPLICATION_JSON).put(entity);
		}

		assertEquals(201, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.SERVICE));

		return response.readEntity(ModelResponse.class);
	}

	private ModelResponse deployForm(String id) throws IOException {
		Response response;

		try(InputStream is = openPMML(id)){
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("id", id);
			formData.bodyPart(new FormDataBodyPart("pmml", is, MediaType.APPLICATION_XML_TYPE));

			Entity<FormDataMultiPart> entity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);

			response = target("model").request(MediaType.APPLICATION_JSON).post(entity);

			formData.close();
		}

		assertEquals(201, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.SERVICE));

		URI location = response.getLocation();

		assertEquals("/model/" + id, location.getPath());

		return response.readEntity(ModelResponse.class);
	}

	private BatchModelResponse queryBatch(){
		Response response = target("model").request(MediaType.APPLICATION_JSON).get();

		assertEquals(200, response.getStatus());

		return response.readEntity(BatchModelResponse.class);
	}

	private ModelResponse query(String id){
		Response response = target("model/" + id).request(MediaType.APPLICATION_JSON).get();

		assertEquals(200, response.getStatus());

		return response.readEntity(ModelResponse.class);
	}

	private Response download(String id){
		Response response = target("model/" + id + "/pmml").request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get();

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_XML_TYPE.withCharset(CHARSET_UTF_8), response.getMediaType());

		return response;
	}

	private EvaluationResponse evaluate(String id, EvaluationRequest request){
		Entity<EvaluationRequest> entity = Entity.json(request);

		Response response = target("model/" + id).request(MediaType.APPLICATION_JSON).post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(EvaluationResponse.class);
	}

	private BatchEvaluationResponse evaluateBatch(String id, BatchEvaluationRequest batchRequest){
		Entity<BatchEvaluationRequest> entity = Entity.json(batchRequest);

		Response response = target("model/" + id + "/batch").request(MediaType.APPLICATION_JSON).post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(BatchEvaluationResponse.class);
	}

	private Response evaluateCsv(String id) throws IOException {
		Response response;

		try(InputStream is = openCSV(id)){
			Entity<InputStream> entity = Entity.entity(is, MediaType.TEXT_PLAIN_TYPE.withCharset(CHARSET_ISO_8859_1));

			response = target("model/" + id + "/csv").queryParam("delimiterChar", "\\t").queryParam("quoteChar", "\\\"").request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN).post(entity);
		}

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.TEXT_PLAIN_TYPE.withCharset(CHARSET_ISO_8859_1), response.getMediaType());

		return response;
	}

	private Response evaluateCsvForm(String id) throws IOException {
		Response response;

		try(InputStream is = openCSV(id)){
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.bodyPart(new FormDataBodyPart("csv", is, MediaType.TEXT_PLAIN_TYPE));

			Entity<FormDataMultiPart> entity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);

			response = target("model/" + id + "/csv").request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN).post(entity);

			formData.close();
		}

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.TEXT_PLAIN_TYPE.withCharset(CHARSET_UTF_8), response.getMediaType());

		return response;
	}

	private SimpleResponse undeploy(String id){
		Response response = target("model/" + id).request(MediaType.APPLICATION_JSON).delete();

		assertEquals(200, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.SERVICE));

		return response.readEntity(SimpleResponse.class);
	}

	private SimpleResponse undeployForm(String id){
		Response response = target("model/" + id).request(MediaType.APPLICATION_JSON).header("X-HTTP-Method-Override", "DELETE").post(null);

		assertEquals(200, response.getStatus());

		return response.readEntity(SimpleResponse.class);
	}

	static
	private EvaluationRequest invalidate(EvaluationRequest record){
		Maps.EntryTransformer<String, Object, String> transformer = new Maps.EntryTransformer<String, Object, String>(){

			@Override
			public String transformEntry(String key, Object value){
				StringBuilder sb = new StringBuilder(key);
				sb.reverse();

				return sb.toString();
			}
		};

		EvaluationRequest invalidRecord = new EvaluationRequest(record.getId());
		invalidRecord.setArguments(Maps.transformEntries(record.getArguments(), transformer));

		return invalidRecord;
	}

	static
	private List<EvaluationRequest> loadRecords(String id) throws Exception {

		try(InputStream is = openCSV(id)){
			CsvUtil.Table<EvaluationRequest> table;

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))){
				table = CsvUtil.readTable(reader, CsvPreference.TAB_PREFERENCE);
			}

			return table.getRows();
		}
	}

	static
	private InputStream openPMML(String id){
		return ModelResourceTest.class.getResourceAsStream("/pmml/" + id + ".pmml");
	}

	static
	private InputStream openCSV(String id){
		return ModelResourceTest.class.getResourceAsStream("/csv/" + extractSuffix(id) + ".csv");
	}

	static
	private String extractSuffix(String id){

		for(int i = id.length() - 1; i > -1; i--){
			char c = id.charAt(i);

			if(Character.isUpperCase(c)){
				return id.substring(i);
			}
		}

		throw new IllegalArgumentException();
	}

	private static final String CHARSET_UTF_8 = "UTF-8";
	private static final String CHARSET_ISO_8859_1 = "ISO-8859-1";
}