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
package org.openscoring.client;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openscoring.common.SimpleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Undeployer extends ModelApplication {

	static
	public void main(String... args) throws Exception {
		run(Undeployer.class, args);
	}

	@Override
	public void run() throws Exception {
		SimpleResponse response = undeploy();

		String message = (response != null ? response.getMessage() : null);
		if(message != null){
			logger.warn("Undeployment failed: {}", message);

			return;
		}

		logger.info("Undeployment succeeded");
	}

	/**
	 * @return <code>null</code> If the operation was successful.
	 */
	public SimpleResponse undeploy() throws Exception {
		Operation<SimpleResponse> operation = new Operation<SimpleResponse>(){

			@Override
			public SimpleResponse perform(WebTarget target){
				Invocation invocation = target.request(MediaType.APPLICATION_JSON).buildDelete();

				Response response = invocation.invoke();

				return response.readEntity(SimpleResponse.class);
			}
		};

		return execute(operation);
	}

	private static final Logger logger = LoggerFactory.getLogger(Undeployer.class);
}