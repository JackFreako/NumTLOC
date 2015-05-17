package com.jackfreako.mavenPluginDevmt.NumLOC;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.thoughtworks.xstream.XStream;
import com.jackfreako.mavenPluginDevmt.NumLOC.*;

/**
 * Goal which counts the total lines of code
 *
 * @goal tlocc
 * 
 * @phase test
 */
public class LinesOfCodeCounterMojo
    extends AbstractMojo
{
	
	/** For each file type (extension) one entry in the map**/
	private final Map<String,CountResult> fileTypes = new HashMap<String,CountResult>(100);

	/** List with all files to be counted **/
	private final List<String> files = new ArrayList<String>(10000);
	
	/** Empty lines of current file**/
	private  int currentFileEmptyLines = 0;
	
	/** Total Code lines of current file**/
	private  int currentFileTotalLines = 0;
	
		
	/**
     * Location of the file.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private final File outputDirectory = new File("");
    
    
    /**
     * Project's source directory as specified in the POM.
     * @parameter expression="${project.source.directory}"
     * @required
     */
    private final File sourceDirectory = new File("");


    /**
     * Project's source directory for test code as specified in the POM.
     * @parameter expression="${project.build.testSourceDirectory}"
     * @required
     */
    private final File testSourceDirectory = new File("");

    
    /**
     *  @parameter expression="${encoding}"
	 *             default-value="${project.build.sourceEncoding}"
     */
    private final String encoding = "UTF-8";
    
    

    
    public void execute() throws MojoExecutionException
    {
        
    	if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}

		if (!sourceDirectory.exists()) {
			getLog().error("Source directory \"" + sourceDirectory + "\" is not valid.");
			return;
		}

		fillListWithAllFilesRecursiveTask(sourceDirectory, files);
		fillListWithAllFilesRecursiveTask(testSourceDirectory, files);

		for (final String filePath : files) {
			resetCurrentCounts();
			countCurrentFile(filePath);
			writeCurrentResultsToMavenLogger(filePath);
			storeCurrentResults(filePath);
		}

		writeOutputFileFromStoredResults();
       
    }
    
    
    public void storeCurrentResults(final String filePath) {
		final String extension = getExtension(filePath);
		if (fileTypes.containsKey(extension)) {
			fileTypes.get(extension).addEmpty(currentFileEmptyLines);
			fileTypes.get(extension).addTotal(currentFileTotalLines);
			fileTypes.get(extension).incrementFiles();
		} else {
			final CountResult item = new CountResult();
			item.addEmpty(currentFileEmptyLines);
			item.addTotal(currentFileTotalLines);
			item.incrementFiles();
			fileTypes.put(extension, item);
		}
	}
    
    private boolean ensureTargetDirectoryExists(){
		
		if(this.outputDirectory.exists()){
			return true;
		}
		
		return this.outputDirectory.mkdirs();
	}
	
	
	public void resetCurrentCounts(){
		this.currentFileEmptyLines = 0;
		this.currentFileTotalLines = 0;		
	}
	
	
	/** This is the backbone of the Counting Mechanism**/
	public void countCurrentFile(final String filePath){
		
		Scanner scanner = null;
		
		try{
			
			scanner = new Scanner(new File(filePath), this.encoding);
			String line = scanner.nextLine();
			
			while(scanner.hasNext()){
				
				this.currentFileTotalLines++;
				
				if(line.trim().isEmpty()){
					this.currentFileEmptyLines++;
				}
				
				line = scanner.nextLine();
			}
		}
		catch(final IOException e){
			getLog().error(e.getMessage());
		}
		
		finally{
			if(scanner!=null)
				scanner.close();
		}
		
	}
	
	
	
	/** Reflect the result in the MavenLogger **/
    public void writeCurrentResultsToMavenLogger(final String filePath){
    	
    	final StringBuffer message = new StringBuffer(100);
    	
    	message.append(this.currentFileTotalLines).append('\t');    	
    	message.append(this.currentFileEmptyLines).append('\t');
    	message.append(filePath);
    	
    	getLog().info(message);
    }
    
    
    
    private void writeOutputFileFromStoredResults() {
		OutputStreamWriter out = null;
		try {
			final StringBuffer path = new StringBuffer();
			path.append(outputDirectory);
			path.append(System.getProperty("file.separator"));
			path.append("tlocc-result.xml");

			final FileOutputStream fos = new FileOutputStream(path.toString());
			out = new OutputStreamWriter(fos, encoding);

			final XStream xstream = new XStream();
			xstream.alias("data", CountResult.class);
			out.write(xstream.toXML(fileTypes));

		} catch (final IOException e) {
			getLog().error(e.getMessage());
		} finally {
			if (null != out) {
				try {
					out.close();
				} catch (final IOException e) {
					getLog().error(e.getMessage());
				}
			}
		}
	}

    
    
    public static void fillListWithAllFilesRecursiveTask(final File root, final List<String> files){
    	
    	if (root.isFile()) {
			files.add(root.getPath());
			return;
		}
    	
		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				fillListWithAllFilesRecursiveTask(file, files);
			} else {
				files.add(file.getPath());
			}
		}
    }
    
    public static String getExtension(final String filePath) {
		final int dotPos = filePath.lastIndexOf(".");
		if (-1 == dotPos) {
			return "undefined";
		} else {
			return filePath.substring(dotPos);
		}
	}

    
    
    
}
