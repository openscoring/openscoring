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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import com.google.common.net.HttpHeaders;
import com.typesafe.config.Config;
import org.openscoring.service.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
@Priority (
	value = Priorities.AUTHENTICATION
)
public class TokenSecurityContextFilter implements ContainerRequestFilter {

	@Context
	private HttpServletRequest request = null;

	private String adminToken = null;


	@Inject
	public TokenSecurityContextFilter(@Named("openscoring") Config config){
		Config filterConfig = config.getConfig("tokenSecurityContextFilter");

		String adminToken = filterConfig.getString("adminToken");

		if((adminToken).equals("random")){
			adminToken = generateRandomToken(32);
		}

		this.adminToken = adminToken;

		logger.info("Admin token: {}", this.adminToken);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		HttpServletRequest request = getRequest();

		SecurityContext securityContext = new SecurityContext(){

			@Override
			public Principal getUserPrincipal(){
				return Anonymous.INSTANCE;
			}

			@Override
			public boolean isUserInRole(String role){
				String token = getToken();

				if((Roles.ADMIN).equals(role)){
					String adminToken = getAdminToken();

					return (adminToken).equals(token);
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
				return "TOKEN";
			}

			private String getToken(){

				if(request != null){
					Cookie[] cookies = request.getCookies();
					if(cookies != null){

						for(Cookie cookie : cookies){

							if(("token").equals(cookie.getName())){
								return cookie.getValue();
							}
						}
					}

					String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
					if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
						return authorizationHeader.substring("Bearer ".length());
					}
				}

				return null;
			}
		};

		requestContext.setSecurityContext(securityContext);
	}

	private HttpServletRequest getRequest(){
		return this.request;
	}

	private String getAdminToken(){
		return this.adminToken;
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