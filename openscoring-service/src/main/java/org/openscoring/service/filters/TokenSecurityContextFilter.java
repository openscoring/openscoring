/*
 * Copyright (c) 2020 Villu Ruusmann
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
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;

import com.google.common.net.HttpHeaders;
import com.typesafe.config.Config;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedMap;
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
public class TokenSecurityContextFilter implements ContainerRequestFilter {

	private String userToken = null;

	private String adminToken = null;


	@Inject
	public TokenSecurityContextFilter(@Named("openscoring") Config config){
		Config filterConfig = config.getConfig("tokenSecurityContextFilter");

		this.userToken = prepareToken(filterConfig, "userToken");
		this.adminToken = prepareToken(filterConfig, "adminToken");

		logger.info("User token: {}", this.userToken);
		logger.info("Admin token: {}", this.adminToken);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		SecurityContext requestSecurityContext = requestContext.getSecurityContext();

		SecurityContext securityContext = new SecurityContext(){

			@Override
			public Principal getUserPrincipal(){
				return Anonymous.INSTANCE;
			}

			@Override
			public boolean isUserInRole(String role){
				String token = getToken();

				String roleToken;

				switch(role){
					case Roles.USER:
						roleToken = getUserToken();
						break;
					case Roles.ADMIN:
						roleToken = getAdminToken();
						break;
					default:
						return false;
				}

				return (roleToken).equals(token) || (roleToken).equals("");
			}

			@Override
			public boolean isSecure(){
				return requestSecurityContext != null && requestSecurityContext.isSecure();
			}

			@Override
			public String getAuthenticationScheme(){
				return "TOKEN";
			}

			private String getToken(){
				Map<String, Cookie> cookies = requestContext.getCookies();
				MultivaluedMap<String, String> headers = requestContext.getHeaders();

				Cookie tokenCookie = cookies.get("token");
				if(tokenCookie != null){
					return tokenCookie.getValue();
				}

				String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
				if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
					return authorizationHeader.substring("Bearer ".length());
				}

				return null;
			}
		};

		requestContext.setSecurityContext(securityContext);
	}

	private String getUserToken(){
		return this.userToken;
	}

	private String getAdminToken(){
		return this.adminToken;
	}

	static
	private String prepareToken(Config config, String path){
		String result = config.getString(path);

		if((result).equals("random")){
			result = generateRandomToken(32);
		}

		return result;
	}

	static
	private String generateRandomToken(int length){
		byte[] bytes = new byte[length];

		TokenSecurityContextFilter.random.nextBytes(bytes);

		Encoder encoder = (Base64.getUrlEncoder()).withoutPadding();

		return encoder.encodeToString(bytes);
	}

	private static final Logger logger = LoggerFactory.getLogger(TokenSecurityContextFilter.class);

	private static final SecureRandom random;

	static {

		try {
			random = SecureRandom.getInstanceStrong();
		} catch(NoSuchAlgorithmException nsae){
			throw new ExceptionInInitializerError(nsae);
		}
	}
}