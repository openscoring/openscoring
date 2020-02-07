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

import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import org.openscoring.service.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
@Priority (
	value = Priorities.AUTHENTICATION
)
public class NetworkSecurityContextFilter implements ContainerRequestFilter {

	@Context
	private HttpServletRequest request = null;

	private Set<String> adminAddresses = Collections.emptySet();


	@Inject
	public NetworkSecurityContextFilter(@Named("openscoring") Config config){
		Config filterConfig = config.getConfig("networkSecurityContextFilter");

		Set<String> adminAddresses = new LinkedHashSet<>();
		adminAddresses.addAll(filterConfig.getStringList("adminAddresses"));

		if(adminAddresses.remove("localhost")){
			adminAddresses.addAll(NetworkSecurityContextFilter.localAddresses);
		}

		this.adminAddresses = ImmutableSet.copyOf(adminAddresses);

		logger.info("Admin network addresses: {}", this.adminAddresses);
	}

	@Override
	public void filter(ContainerRequestContext requestContext){
		HttpServletRequest request = getRequest();

		SecurityContext securityContext = new SecurityContext(){

			@Override
			public Principal getUserPrincipal(){
				return Anonymous.INSTANCE;
			}

			@Override
			public boolean isUserInRole(String role){
				String address = getAddress();

				if((Roles.ADMIN).equals(role)){
					Set<String> adminAddresses = getAdminAddresses();

					return (adminAddresses).contains(address) || (adminAddresses).contains("*");
				}

				return false;
			}

			@Override
			public boolean isSecure(){

				if(request != null){
					return request.isSecure();
				}

				return false;
			}

			@Override
			public String getAuthenticationScheme(){
				return "REMOTE_ADDR";
			}

			private String getAddress(){

				if(request != null){
					return request.getRemoteAddr();
				}

				return null;
			}
		};

		requestContext.setSecurityContext(securityContext);
	}

	private HttpServletRequest getRequest(){
		return this.request;
	}

	private Set<String> getAdminAddresses(){
		return this.adminAddresses;
	}

	static
	private Set<String> discoverLocalAddresses() throws IOException {
		Set<String> result = new LinkedHashSet<>();

		InetAddress address = InetAddress.getLocalHost();
		result.add(address.getHostAddress());

		InetAddress[] resolvedAddresses = InetAddress.getAllByName("localhost");
		for(InetAddress resolvedAddress : resolvedAddresses){
			result.add(resolvedAddress.getHostAddress());
		}

		logger.info("Local network addresses: {}", result);

		return result;
	}

	private static final Logger logger = LoggerFactory.getLogger(NetworkSecurityContextFilter.class);

	private static final Set<String> localAddresses;

	static {

		try {
			localAddresses = ImmutableSet.copyOf(discoverLocalAddresses());
		} catch(IOException ioe){
			throw new ExceptionInInitializerError(ioe);
		}
	}
}