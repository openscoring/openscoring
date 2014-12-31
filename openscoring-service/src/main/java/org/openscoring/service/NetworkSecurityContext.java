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

import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.SecurityContext;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class NetworkSecurityContext implements SecurityContext {

	private ServletRequest request = null;


	public NetworkSecurityContext(ServletRequest request){
		setRequest(request);
	}

	@Override
	public String getAuthenticationScheme(){
		return "NETWORK";
	}

	@Override
	public boolean isSecure(){
		ServletRequest request = getRequest();

		return request.isSecure();
	}

	@Override
	public Principal getUserPrincipal(){
		return Anonymous.INSTANCE;
	}

	@Override
	public boolean isUserInRole(String role){
		ServletRequest request = getRequest();

		if(("admin").equals(role)){
			String address = request.getRemoteAddr();

			return (NetworkSecurityContext.addresses).contains(address);
		}

		return false;
	}

	public ServletRequest getRequest(){
		return this.request;
	}

	private void setRequest(ServletRequest request){
		this.request = request;
	}

	static
	private Set<String> discoverLocalAddresses() throws IOException {
		Set<String> result = Sets.newLinkedHashSet();

		InetAddress address = InetAddress.getLocalHost();
		result.add(address.getHostAddress());

		InetAddress[] resolvedAddresses = InetAddress.getAllByName("localhost");
		for(InetAddress resolvedAddress : resolvedAddresses){
			result.add(resolvedAddress.getHostAddress());
		}

		return result;
	}

	private static final Set<String> addresses;

	static {

		try {
			addresses = ImmutableSet.copyOf(discoverLocalAddresses());
		} catch(IOException ioe){
			throw new ExceptionInInitializerError(ioe);
		}
	}

	static
	private class Anonymous implements Principal {

		private Anonymous(){
		}

		@Override
		public String getName(){
			return "ANONYMOUS";
		}

		private static final Anonymous INSTANCE = new Anonymous();
	}
}