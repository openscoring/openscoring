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

import com.typesafe.config.Config;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
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

	private Set<String> userAddresses = Collections.emptySet();

	private Set<String> adminAddresses = Collections.emptySet();


	@Inject
	public NetworkSecurityContextFilter(@Named("openscoring") Config config){
		Config filterConfig = config.getConfig("networkSecurityContextFilter");

		this.userAddresses = prepareAddresses(filterConfig, "userAddresses");
		this.adminAddresses = prepareAddresses(filterConfig, "adminAddresses");

		logger.info("User network addresses: {}", this.userAddresses);
		logger.info("Admin network addresses: {}", this.adminAddresses);
	}

	@Override
	public void filter(ContainerRequestContext requestContext){
		HttpServletRequest request = getRequest();

		SecurityContext requestSecurityContext = requestContext.getSecurityContext();

		SecurityContext securityContext = new SecurityContext(){

			@Override
			public Principal getUserPrincipal(){
				return Anonymous.INSTANCE;
			}

			@Override
			public boolean isUserInRole(String role){
				String address = getAddress();

				Set<String> roleAddresses;

				switch(role){
					case Roles.USER:
						roleAddresses = getUserAddresses();
						break;
					case Roles.ADMIN:
						roleAddresses = getAdminAddresses();
						break;
					default:
						return false;
				}

				return (roleAddresses).contains(address) || (roleAddresses).contains("*");
			}

			@Override
			public boolean isSecure(){
				return requestSecurityContext != null && requestSecurityContext.isSecure();
			}

			@Override
			public String getAuthenticationScheme(){
				return "REMOTE_ADDR";
			}

			private String getAddress(){

				if(request == null){
					return null;
				}

				return request.getRemoteAddr();
			}
		};

		requestContext.setSecurityContext(securityContext);
	}

	private HttpServletRequest getRequest(){
		return this.request;
	}

	private Set<String> getUserAddresses(){
		return this.userAddresses;
	}

	private Set<String> getAdminAddresses(){
		return this.adminAddresses;
	}

	static
	private Set<String> prepareAddresses(Config config, String path){
		Set<String> result = new LinkedHashSet<>();
		result.addAll(config.getStringList(path));

		if(result.remove("localhost")){
			result.addAll(NetworkSecurityContextFilter.localAddresses);
		}

		return Set.copyOf(result);
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
			localAddresses = Set.copyOf(discoverLocalAddresses());
		} catch(IOException ioe){
			throw new ExceptionInInitializerError(ioe);
		}
	}
}