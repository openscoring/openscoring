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
import java.net.InetSocketAddress;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.openscoring.service.Openscoring;

public class Main {

	@Parameter (
		names = {"--application-class", "--application"},
		description = "Application class name"
	)
	private String applicationClazz = Openscoring.class.getName();

	@Parameter (
		names = {"--application-path"},
		description = "Application context path"
	)
	private String applicationPath = "/openscoring";

	@Parameter (
		names = {"--console-war"},
		description = "Web administration console WAR file or directory",
		hidden = true
	)
	private File consoleWar = null;

	@Parameter (
		names = {"--console-path"},
		description = "Web administration console context path",
		hidden = true
	)
	private String consolePath = "/console";

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true
	)
	private boolean help = false;

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


	static
	public void main(String... args) throws Exception {
		execute(Main.class, args);
	}

	static
	public void execute(Class<? extends Main> clazz, String... args) throws Exception {
		Main main = clazz.newInstance();

		JCommander commander = new JCommander(main);
		commander.setProgramName(clazz.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			commander.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			commander.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	public void run() throws Exception {
		InetSocketAddress address;

		if(this.host != null){
			address = new InetSocketAddress(this.host, this.port);
		} else

		{
			address = new InetSocketAddress(this.port);
		}

		Server server = createServer(address);

		server.start();
		server.join();
	}

	private Server createServer(InetSocketAddress address) throws Exception {
		Server server = new Server(address);

		ContextHandlerCollection handlerCollection = new ContextHandlerCollection();

		Handler applicationHandler = createApplicationHandler();
		handlerCollection.addHandler(applicationHandler);

		Handler adminConsoleHandler = createAdminConsoleHandler();
		if(adminConsoleHandler != null){
			handlerCollection.addHandler(adminConsoleHandler);
		}

		server.setHandler(handlerCollection);

		return server;
	}

	private Handler createApplicationHandler() throws Exception {
		Class<?> applicationClazz = Class.forName(this.applicationClazz);

		Class<? extends Openscoring> openscoringClazz = (applicationClazz).asSubclass(Openscoring.class);

		Openscoring application = openscoringClazz.newInstance();

		ServletContainer jerseyServlet = new ServletContainer(application);

		ServletHolder servletHolder = new ServletHolder(jerseyServlet);

		ServletContextHandler servletHandler = new ServletContextHandler(null, this.applicationPath);
		servletHandler.addServlet(servletHolder, "/*");

		return servletHandler;
	}

	private Handler createAdminConsoleHandler() throws Exception {

		if(this.consoleWar != null){
			WebAppContext webappHandler = new WebAppContext(null, this.consoleWar.getAbsolutePath(), this.consolePath);

			return webappHandler;
		}

		return null;
	}
}