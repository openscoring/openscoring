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
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.openscoring.common.BatchEvaluationRequest;
import org.openscoring.common.BatchEvaluationResponse;
import org.openscoring.common.BatchModelResponse;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.Field;
import org.openscoring.common.Headers;
import org.openscoring.common.ModelResponse;
import org.openscoring.common.SimpleResponse;
import org.openscoring.common.TableEvaluationRequest;
import org.openscoring.common.providers.ObjectMapperProvider;
import org.openscoring.service.providers.CsvUtil;
import org.supercsv.prefs.CsvPreference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModelResourceTest extends JerseyTest {

	@Override
	protected TestContainerFactory getTestContainerFactory(){
		return new JdkHttpServerTestContainerFactory();
	}

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
	public void badContent() throws Exception {
		String pmmlOpenTag = "<PMML xmlns=\"http://www.dmg.org/PMML-4_3\" version=\"4.3\">";
		String headerTag = "<Header/>";
		String dataDictionaryTag = "<DataDictionary><DataField name=\"x\" dataType=\"double\" optype=\"continuous\"/></DataDictionary>";
		String treeModelTag = "<TreeModel functionName=\"regression\"><MiningSchema><MiningField name=\"x\"/><MiningField name=\"x\"/></MiningSchema><Node><False/></Node></TreeModel>";
		String pmmlCloseTag = "</PMML>";

		ModelResponse modelResponse = deployBadString("invalid_pmml", pmmlOpenTag);

		assertNotNull(modelResponse.getMessage());

		modelResponse = deployBadString("empty_pmml", pmmlOpenTag + headerTag + dataDictionaryTag + pmmlCloseTag);

		assertNotNull(modelResponse.getMessage());

		modelResponse = deployBadString("invalid_model", pmmlOpenTag + headerTag + dataDictionaryTag + treeModelTag + pmmlCloseTag);

		assertNotNull(modelResponse.getMessage());
	}

	@Test
	public void decisionTreeIris() throws Exception {
		String id = "DecisionTreeIris";

		assertEquals("Iris", extractSuffix(id));

		BatchModelResponse batchModelResponse = queryBatch(ModelResourceTest.USER_TOKEN);

		List<ModelResponse> modelResponses = batchModelResponse.getResponses();

		assertNull(modelResponses);

		ModelResponse modelResponse = deploy(id);

		Map<String, List<Field>> schema = modelResponse.getSchema();

		List<Field> inputFields = schema.get("inputFields");
		List<Field> groupFields = schema.get("groupFields");
		List<Field> targetFields = schema.get("targetFields");
		List<Field> outputFields = schema.get("outputFields");

		assertEquals(4, inputFields.size());
		assertNull(groupFields);
		assertEquals(1, targetFields.size());
		assertEquals(4, outputFields.size());

		for(Field inputField : inputFields){
			assertNotNull(inputField.getId());
			assertNotNull(inputField.getName());

			if((DataType.DOUBLE).equals(inputField.getDataType()) && (OpType.CONTINUOUS).equals(inputField.getOpType())){
				List<String> values = inputField.getValues();

				assertEquals(1, values.size());
			} else

			{
				fail();
			}
		}

		batchModelResponse = queryBatch(ModelResourceTest.ADMIN_TOKEN);

		modelResponses = batchModelResponse.getResponses();

		assertEquals(1, modelResponses.size());

		download(id);

		BatchEvaluationRequest batchRequest = loadRecords(id);

		EvaluationRequest request = batchRequest.getRequest(0);

		EvaluationResponse response = evaluate(id, request);

		assertEquals(request.getId(), response.getId());

		EvaluationRequest invalidRequest = invalidate(batchRequest.getRequest(50));

		List<EvaluationRequest> requests = Arrays.asList(batchRequest.getRequest(0), invalidRequest, batchRequest.getRequest(100));

		batchRequest = new BatchEvaluationRequest()
			.setRequests(requests);

		BatchEvaluationResponse batchResponse = evaluateBatch(id, ModelResourceTest.ADMIN_TOKEN, batchRequest);

		assertEquals(batchRequest.getId(), batchResponse.getId());

		List<EvaluationResponse> responses = batchResponse.getResponses();

		assertEquals(requests.size(), responses.size());

		EvaluationResponse invalidResponse = batchResponse.getResponse(1);

		assertEquals(invalidRequest.getId(), invalidResponse.getId());
		assertNotNull(invalidResponse.getMessage());

		undeploy(id);
	}

	@Test
	public void associationRulesShopping() throws Exception {
		String id = "AssociationRulesShopping";

		assertEquals("Shopping", extractSuffix(id));

		ModelResponse modelResponse = deployForm(id);

		Map<String, List<Field>> schema = modelResponse.getSchema();

		List<Field> inputFields = schema.get("inputFields");
		List<Field> groupFields = schema.get("groupFields");
		List<Field> targetFields = schema.get("targetFields");
		List<Field> outputFields = schema.get("outputFields");

		assertEquals(1, inputFields.size());
		assertEquals(1, groupFields.size());
		assertNull(targetFields);
		assertEquals(3, outputFields.size());

		query(id);

		BatchEvaluationRequest batchRequest = loadRecords(id);

		BatchEvaluationResponse batchResponse = evaluateBatch(id, ModelResourceTest.USER_TOKEN, batchRequest);

		assertEquals(batchRequest.getId(), batchResponse.getId());

		List<EvaluationRequest> requests = batchRequest.getRequests();

		List<EvaluationRequest> aggregatedRequests = ModelResource.aggregateRequests("transaction", requests);

		batchRequest = new BatchEvaluationRequest("aggregate")
			.setRequests(aggregatedRequests);

		batchResponse = evaluateBatch(id, ModelResourceTest.USER_TOKEN, batchRequest);

		assertEquals(batchRequest.getId(), batchResponse.getId());

		evaluateCsv(id);

		evaluateCsvForm(id);

		undeployForm(id);
	}

	@Test
	public void linearRegressionAuto() throws Exception {
		String id = "LinearRegressionAuto";

		assertEquals("Auto", extractSuffix(id));

		ModelResponse modelResponse = deploy(id);

		Map<String, List<Field>> schema = modelResponse.getSchema();

		List<Field> inputFields = schema.get("inputFields");
		List<Field> groupFields = schema.get("groupFields");
		List<Field> targetFields = schema.get("targetFields");
		List<Field> outputFields = schema.get("outputFields");

		assertEquals(7, inputFields.size());
		assertNull(groupFields);
		assertEquals(1, targetFields.size());
		assertEquals(1, outputFields.size());

		for(Field inputField : inputFields){
			assertNotNull(inputField.getId());
			assertNull(inputField.getName());

			DataType dataType = inputField.getDataType();
			OpType opType = inputField.getOpType();

			List<String> values = inputField.getValues();

			if((DataType.STRING).equals(dataType) && (OpType.CATEGORICAL).equals(opType)){
				assertTrue(values.size() > 0);
			} else

			if((DataType.DOUBLE).equals(dataType) && (OpType.CONTINUOUS).equals(opType)){
				assertNull(values);
			} else

			{
				fail();
			}
		}

		{
			Field targetField = targetFields.get(0);

			assertEquals(ModelResponse.DEFAULT_TARGET_NAME, targetField.getId());

			assertEquals(DataType.DOUBLE, targetField.getDataType());
			assertEquals(OpType.CONTINUOUS, targetField.getOpType());
		}

		BatchEvaluationRequest batchRequest = loadRecords(id);

		EvaluationRequest request = batchRequest.getRequest(0);

		EvaluationResponse response = evaluate(id, request);

		assertEquals(request.getId(), response.getId());

		Map<String, ?> results = response.getResults();

		assertEquals(2, results.size());

		String report = (String)results.get("report(Predicted_mpg)");

		assertTrue(report.startsWith("<math xmlns=\"http://www.w3.org/1998/Math/MathML\">") && report.endsWith("</math>"));

		undeploy(id);
	}

	private ModelResponse deploy(String id) throws IOException {
		Response response;

		try(InputStream is = openPMML(id)){
			Entity<InputStream> entity = Entity.entity(is, MediaType.APPLICATION_XML);

			response = target("model/" + id)
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.ADMIN_TOKEN)
				.put(entity);
		}

		assertEquals(201, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.APPLICATION));

		return response.readEntity(ModelResponse.class);
	}

	private ModelResponse deployBadString(String id, String string){
		Entity<String> entity = Entity.entity(string, MediaType.APPLICATION_XML);

		Response response = target("model/" + id)
			.request(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.ADMIN_TOKEN)
			.put(entity);

		assertEquals(400, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.APPLICATION));

		return response.readEntity(ModelResponse.class);
	}

	private ModelResponse deployForm(String id) throws IOException {
		Response response;

		try(InputStream is = openPMML(id)){
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.bodyPart(new FormDataBodyPart("pmml", is, MediaType.APPLICATION_XML_TYPE));

			Entity<FormDataMultiPart> entity = Entity.entity(formData, MediaType.MULTIPART_FORM_DATA);

			response = target("model/" + id)
				.queryParam("_method", "PUT")
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.ADMIN_TOKEN)
				.post(entity);

			formData.close();
		}

		assertEquals(201, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.APPLICATION));

		URI location = response.getLocation();

		assertEquals("/model/" + id, location.getPath());

		return response.readEntity(ModelResponse.class);
	}

	private BatchModelResponse queryBatch(String token){
		Response response = target("model")
			.request(MediaType.APPLICATION_JSON)
			.cookie("token", token)
			.get();

		assertEquals(200, response.getStatus());

		return response.readEntity(BatchModelResponse.class);
	}

	private ModelResponse query(String id){
		Response response = target("model/" + id)
			.request(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.USER_TOKEN)
			.get();

		assertEquals(200, response.getStatus());

		return response.readEntity(ModelResponse.class);
	}

	private Response download(String id){
		Response response = target("model/" + id + "/pmml")
			.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
			.cookie("token", ModelResourceTest.ADMIN_TOKEN)
			.get();

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_XML_TYPE.withCharset(CHARSET_UTF_8), response.getMediaType());

		return response;
	}

	private EvaluationResponse evaluate(String id, EvaluationRequest request){
		Entity<EvaluationRequest> entity = Entity.json(request);

		Response response = target("model/" + id)
			.request(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.USER_TOKEN)
			.post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(EvaluationResponse.class);
	}

	private BatchEvaluationResponse evaluateBatch(String id, String token, BatchEvaluationRequest batchRequest){
		Entity<BatchEvaluationRequest> entity = Entity.json(batchRequest);

		Response response = target("model/" + id + "/batch")
			.request(MediaType.APPLICATION_JSON)
			.cookie("token", token)
			.post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(BatchEvaluationResponse.class);
	}

	private Response evaluateCsv(String id) throws IOException {
		Response response;

		try(InputStream is = openCSV(id)){
			Entity<InputStream> entity = Entity.entity(is, MediaType.TEXT_PLAIN_TYPE.withCharset(CHARSET_ISO_8859_1));

			response = target("model/" + id + "/csv")
				.queryParam("delimiterChar", "\\t")
				.queryParam("quoteChar", "\\\"")
				.request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.USER_TOKEN)
				.post(entity);
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

			response = target("model/" + id + "/csv")
				.request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.USER_TOKEN)
				.post(entity);

			formData.close();
		}

		assertEquals(200, response.getStatus());
		assertEquals(MediaType.TEXT_PLAIN_TYPE.withCharset(CHARSET_UTF_8), response.getMediaType());

		return response;
	}

	private SimpleResponse undeploy(String id){
		Response response = target("model/" + id)
			.request(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.ADMIN_TOKEN)
			.delete();

		assertEquals(200, response.getStatus());
		assertNotNull(response.getHeaderString(Headers.APPLICATION));

		return response.readEntity(SimpleResponse.class);
	}

	private SimpleResponse undeployForm(String id){
		Entity<Form> entity = Entity.form(new Form());

		Response response = target("model/" + id)
			.request(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + ModelResourceTest.ADMIN_TOKEN)
			.header("X-HTTP-Method-Override", "DELETE")
			.post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(SimpleResponse.class);
	}

	static
	private EvaluationRequest invalidate(EvaluationRequest request){
		Function<String, String> function = new Function<String, String>(){

			@Override
			public String apply(String string){
				StringBuilder sb = new StringBuilder(string);

				sb = sb.reverse();

				return sb.toString();
			}
		};

		Map<String, ?> arguments = request.getArguments();

		arguments = (arguments.entrySet()).stream()
			.collect(Collectors.toMap(entry -> entry.getKey(), entry -> function.apply(entry.getKey())));

		EvaluationRequest invalidRequest = new EvaluationRequest(request.getId())
			.setArguments(arguments);

		return invalidRequest;
	}

	static
	private BatchEvaluationRequest loadRecords(String id) throws Exception {

		try(InputStream is = openCSV(id)){
			TableEvaluationRequest tableRequest;

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))){
				tableRequest = CsvUtil.readTable(reader, CsvPreference.TAB_PREFERENCE);
			}

			BatchEvaluationRequest batchRequest = new BatchEvaluationRequest()
				.setRequests(tableRequest.getRequests());

			return batchRequest;
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

	private static String USER_TOKEN = "little secret";
	private static String ADMIN_TOKEN = "big secret";

	private static final String CHARSET_UTF_8 = "UTF-8";
	private static final String CHARSET_ISO_8859_1 = "ISO-8859-1";
}