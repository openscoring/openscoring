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
package org.openscoring.service.filters;

import java.security.Principal;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.openscoring.service.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract
public class NetworkSecurityContext implements SecurityContext {

	private ServletRequest request = null;


	public NetworkSecurityContext(ServletRequest request){
		setRequest(request);
	}

	abstract
	public boolean isAdmin(String address);

	@Override
	public Principal getUserPrincipal(){
		return Anonymous.INSTANCE;
	}

	@Override
	public boolean isUserInRole(String role){
		ServletRequest request = getRequest();

		if((Roles.ADMIN).equals(role)){
			String address = null;

			if(request != null){
				address = request.getRemoteAddr();
			}

			boolean trusted = isAdmin(address);

			logger.info("Admin role {} to network address {}", (trusted ? "granted" : "denied"), address);

			return trusted;
		}

		return false;
	}

	@Override
	public boolean isSecure(){
		ServletRequest request = getRequest();

		return request.isSecure();
	}

	@Override
	public String getAuthenticationScheme(){
		return "REMOTE_ADDR";
	}

	public ServletRequest getRequest(){
		return this.request;
	}

	private void setRequest(ServletRequest request){
		this.request = request;
	}

	static
	private class Anonymous implements Principal {

		private Anonymous(){
		}

		@Override
		public String getName(){
			return null;
		}

		private static final Anonymous INSTANCE = new Anonymous();
	}

	private static final Logger logger = LoggerFactory.getLogger(NetworkSecurityContext.class);
}