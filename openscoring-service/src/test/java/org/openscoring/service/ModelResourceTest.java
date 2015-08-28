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
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dmg.pmml.FieldName;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.openscoring.common.BatchEvaluationRequest;
import org.openscoring.common.BatchEvaluationResponse;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.ModelResponse;
import org.supercsv.prefs.CsvPreference;

import static org.junit.Assert.assertEquals;

public class ModelResourceTest extends JerseyTest {

	@Override
	protected Application configure(){
		Openscoring openscoring = new Openscoring();

		return openscoring;
	}

	@Override
	protected void configureClient(ClientConfig clientConfig){
		// Ideally, should use the client-side ObjectMapperProvider class instead of the server-side one
		clientConfig.register(ObjectMapperProvider.class);
	}

	@Test
	public void decisionTreeIris() throws Exception {
		String id = "DecisionTreeIris";

		deploy(id);

		List<EvaluationRequest> records = loadRecords("Iris");

		EvaluationRequest request = records.get(0);

		EvaluationResponse response = evaluate(id, request);

		undeploy(id);
	}

	@Test
	public void associationRulesShopping() throws Exception {
		String id = "AssociationRulesShopping";

		deploy(id);

		List<EvaluationRequest> records = loadRecords("Shopping");

		BatchEvaluationRequest request = new BatchEvaluationRequest();
		request.setRequests(records);

		BatchEvaluationResponse response = evaluateBatch(id, request);

		List<EvaluationRequest> aggregatedRecords = ModelResource.aggregateRequests(FieldName.create("transaction"), records);

		request = new BatchEvaluationRequest("aggregate");
		request.setRequests(aggregatedRecords);

		response = evaluateBatch(id, request);

		assertEquals(request.getId(), response.getId());

		undeploy(id);
	}

	private ModelResponse deploy(String id) throws IOException {
		Response response;

		InputStream is = openPMML(id);

		try {
			Entity<InputStream> entity = Entity.entity(is, MediaType.APPLICATION_XML);

			response = target("model/" + id).request(MediaType.APPLICATION_JSON).put(entity);
		} finally {
			is.close();
		}

		assertEquals(201, response.getStatus());

		return response.readEntity(ModelResponse.class);
	}

	private EvaluationResponse evaluate(String id, EvaluationRequest request){
		Entity<EvaluationRequest> entity = Entity.json(request);

		Response response = target("model/" + id).request(MediaType.APPLICATION_JSON).post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(EvaluationResponse.class);
	}

	private BatchEvaluationResponse evaluateBatch(String id, BatchEvaluationRequest request){
		Entity<BatchEvaluationRequest> entity = Entity.json(request);

		Response response = target("model/" + id + "/batch").request(MediaType.APPLICATION_JSON).post(entity);

		assertEquals(200, response.getStatus());

		return response.readEntity(BatchEvaluationResponse.class);
	}

	private void undeploy(String id){
		Response response = target("model/" + id).request(MediaType.APPLICATION_JSON).delete();

		assertEquals(204, response.getStatus());
	}

	static
	private List<EvaluationRequest> loadRecords(String id) throws Exception {
		InputStream is = openCSV(id);

		try {
			CsvUtil.Table<EvaluationRequest> table;

			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			try {
				table = CsvUtil.readTable(reader, CsvPreference.TAB_PREFERENCE);
			} finally {
				reader.close();
			}

			return table.getRows();
		} finally {
			is.close();
		}
	}

	static
	private InputStream openPMML(String id){
		return ModelResourceTest.class.getResourceAsStream("/pmml/" + id + ".pmml");
	}

	static
	private InputStream openCSV(String id){
		return ModelResourceTest.class.getResourceAsStream("/csv/" + id + ".csv");
	}
}