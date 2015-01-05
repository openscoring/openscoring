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
import java.util.List;

import com.beust.jcommander.Parameter;

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


	static
	public void main(String... args) throws Exception {
		run(DirectoryDeployer.class, args);
	}

	@Override
	public void run() throws Exception {
		Path root = getRoot();

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

		// Remove file name extension
		// Start the search from the beginning of the file name, in case there are multiple extensions (eg. ".pmml.xml")
		int dot = id.indexOf('.');
		if(dot > -1){
			id = id.substring(0, dot);
		} // End if

		if((StandardWatchEventKinds.ENTRY_CREATE).equals(kind)){
			Deployer deployer = new Deployer();
			deployer.setModel(getModelCollection() + "/" + id);
			deployer.setFile(path.toFile());
			deployer.run();
		} else

		if((StandardWatchEventKinds.ENTRY_DELETE).equals(kind)){
			Undeployer undeployer = new Undeployer();
			undeployer.setModel(getModelCollection() + "/" + id);
			undeployer.run();
		}
	}

	private Path getRoot(){
		File dir = getDir();

		return dir.toPath();
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
}