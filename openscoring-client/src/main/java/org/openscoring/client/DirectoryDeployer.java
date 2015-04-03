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

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryDeployer extends Application {

	@Parameter (
		names = {"--model-collection"},
		description = "The URI of the model collection",
		required = true
	)
	private String modelCollection = null;

	@Parameter (
		names = {"--dir"},
		description = "The directory to watch for PMML file additions and removals",
		required = true
	)
	private File dir = null;

	/**
	 * A set of "managed" model identifiers.
	 */
	private Set<String> identifiers = new LinkedHashSet<String>();


	static
	public void main(String... args) throws Exception {
		run(DirectoryDeployer.class, args);
	}

	@Override
	public void run() throws Exception {
		Path root = (getDir()).toPath();

		DirectoryStream<Path> children = Files.newDirectoryStream(root);

		try {
			for(Path child : children){
				process(StandardWatchEventKinds.ENTRY_CREATE, child);
			}
		} finally {
			children.close();
		}

		FileSystem fileSystem = root.getFileSystem();

		try {
			WatchService watcher = fileSystem.newWatchService();

			try {
				WatchKey key = root.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
				if(key.isValid()){
					processKey(key, root);
				}

				while(key.reset()){

					try {
						key = watcher.take();
					} catch(InterruptedException ie){
						break;
					}

					if(key.isValid()){
						processKey(key, root);
					}
				}
			} finally {
				watcher.close();
			}
		} finally {
			fileSystem.close();
		}
	}

	private void processKey(WatchKey key, Path root) throws Exception {

		List<WatchEvent<?>> events = key.pollEvents();
		for(WatchEvent<?> event : events){
			Path child = root.resolve((Path)event.context());

			process((WatchEvent.Kind<Path>)event.kind(), child);
		}
	}

	private void process(WatchEvent.Kind<Path> kind, Path path) throws Exception {
		String id = (path.getFileName()).toString();

		// Use the name part of the file name (ie. everything to the left of the first dot character) as the model identifier
		int dot = id.indexOf('.');
		if(dot > -1){
			id = id.substring(0, dot);
		} // End if

		if(("").equals(id)){
			return;
		} // End if

		if((StandardWatchEventKinds.ENTRY_CREATE).equals(kind)){

			if(!Files.isRegularFile(path)){
				return;
			}

			logger.info("Deploying model {}", id);

			Deployer deployer = new Deployer();
			deployer.setModel(getModelCollection() + "/" + id);
			deployer.setFile(path.toFile());
			deployer.run();

			this.identifiers.add(id);
		} else

		if((StandardWatchEventKinds.ENTRY_DELETE).equals(kind)){
			boolean status = this.identifiers.remove(id);
			if(!status){
				return;
			}

			logger.info("Undeploying model {}", id);

			Undeployer undeployer = new Undeployer();
			undeployer.setModel(getModelCollection() + "/" + id);
			undeployer.run();
		}
	}

	public String getModelCollection(){
		return this.modelCollection;
	}

	public void setModelCollection(String modelCollection){
		this.modelCollection = modelCollection;
	}

	public File getDir(){
		return this.dir;
	}

	public void setDir(File dir){
		this.dir = dir;
	}

	private static final Logger logger = LoggerFactory.getLogger(DirectoryDeployer.class);
}