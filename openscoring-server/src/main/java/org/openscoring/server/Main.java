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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jetty9.InstrumentedHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.openscoring.service.ModelRegistry;
import org.openscoring.service.ModelResource;

public class Main {

	@Parameter (
		names = {"--host"},
		description = "Server host name or ip address"
	)
	private String host = null;

	@Parameter (
		names = {"--port"},
		description = "Server port"
	)
	private int port = 8080;

	@Parameter (
		names = {"--context-path"},
		description = "Context path"
	)
	private String contextPath = "/openscoring";

	@Parameter (
		names = {"--component-classes"},
		description = "JAX-RS component classes",
		converter = ClassConverter.class,
		hidden = true
	)
	private List<Class<?>> componentClasses = Lists.newArrayList();

	@Parameter (
		names = {"--console-war"},
		description = "Console web application (WAR) file or directory",
		hidden = true
	)
	private File consoleWar = null;

	@Parameter (
		names = {"--model-dir"},
		description = "Model auto-deployment directory"
	)
	private File modelDir = null;

	@Parameter (
		names = {"--visitor-classes"},
		description = "PMML class model visitor classes",
		converter = ClassConverter.class,
		hidden = true
	)
	private List<Class<?>> visitorClasses = Lists.newArrayList();

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
		InetSocketAddress address;

		if(this.host != null){
			address = new InetSocketAddress(this.host, this.port);
		} else

		{
			address = new InetSocketAddress(this.port);
		}

		Server server = new Server(address);

		ContextHandlerCollection handlerCollection = new ContextHandlerCollection();

		final
		ModelRegistry modelRegistry = new ModelRegistry();
		modelRegistry.registerClasses(Sets.newLinkedHashSet(this.visitorClasses));

		final
		MetricRegistry metricRegistry = new MetricRegistry();

		Binder binder = new AbstractBinder(){

			@Override
			protected void configure(){
				bind(modelRegistry).to(ModelRegistry.class);
				bind(metricRegistry).to(MetricRegistry.class);
			}
		};

		ResourceConfig config = new ResourceConfig(ModelResource.class);
		config.registerClasses(Sets.newLinkedHashSet(this.componentClasses));
		config.register(binder);
		config.register(JacksonFeature.class);
		config.register(MultiPartFeature.class);
		config.register(ObjectMapperProvider.class);
		config.register(RolesAllowedDynamicFeature.class);

		// Naive implementation that grants the "admin" role to all local network users
		config.register(NetworkSecurityContextFilter.class);

		ServletContextHandler servletHandler = new ServletContextHandler();
		servletHandler.setContextPath(this.contextPath);

		ServletContainer jerseyServlet = new ServletContainer(config);

		servletHandler.addServlet(new ServletHolder(jerseyServlet), "/*");

		InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry);
		instrumentedHandler.setHandler(servletHandler);

		handlerCollection.addHandler(instrumentedHandler);

		if(this.consoleWar != null){
			WebAppContext consoleHandler = new WebAppContext();
			consoleHandler.setContextPath(this.contextPath + "/console"); // XXX
			consoleHandler.setWar(this.consoleWar.getAbsolutePath());

			handlerCollection.addHandler(consoleHandler);
		}

		server.setHandler(handlerCollection);

		DirectoryDeployer deployer = null;

		if(this.modelDir != null){

			if(!this.modelDir.isDirectory()){
				throw new IOException(this.modelDir.getAbsolutePath() + " is not a directory");
			}

			deployer = new DirectoryDeployer(modelRegistry, this.modelDir.toPath());
		}

		server.start();

		if(deployer != null){
			deployer.start();
		}

		server.join();

		if(deployer != null){
			deployer.interrupt();

			deployer.join();
		}
	}
}