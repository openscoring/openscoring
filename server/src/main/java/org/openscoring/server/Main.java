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
package org.openscoring.server;

import java.util.*;

import javax.servlet.*;

import org.openscoring.service.*;

import com.google.common.collect.*;
import com.google.inject.*;
import com.google.inject.servlet.*;

import com.sun.jersey.api.json.*;
import com.sun.jersey.guice.*;
import com.sun.jersey.guice.spi.container.servlet.*;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.*;

import com.beust.jcommander.*;

public class Main {

	@Parameter (
		names = {"--port"},
		description = "Port"
	)
	private int port = 8080;

	@Parameter (
		names = {"--context-path"},
		description = "Context path"
	)
	private String contextPath = "/openscoring";

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true
	)
	private boolean help = false;


	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			commander.usage();

			System.exit(-1);
		}

		if(main.help){
			commander.usage();

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		Server server = new Server(this.port);

		ServletContextHandler contextHandler = new ServletContextHandler();
		contextHandler.setContextPath(this.contextPath);

		contextHandler.addFilter(GuiceFilter.class, "/*", null);

		Module module = new JerseyServletModule(){

			@Override
			public void configureServlets(){
				bind(ModelService.class);

				Map<String, String> config = Maps.newLinkedHashMap();
				config.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");

				serve("/*").with(GuiceContainer.class, config);
			}
		};

		final
		Injector injector = Guice.createInjector(module);

		ServletContextListener contextListener = new GuiceServletContextListener(){

			@Override
			protected Injector getInjector(){
				return injector;
			}
		};
		contextHandler.addEventListener(contextListener);

		contextHandler.addServlet(DefaultServlet.class, "/");

		server.setHandler(contextHandler);

		server.start();
		server.join();
	}
}