/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.python;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

public class PythonInterpeter {
	
	private static String executable;
	
	public static String execute(String script, Map<String, String> environment) {
		loadExecutableIfNotLoaded();
		
		try {
			File tempFile = File.createTempFile("capella-workflow-dse", "py");
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
		    writer.write(script);
		    writer.close();
			tempFile.deleteOnExit();
			
			ProcessBuilder pb = new ProcessBuilder(executable, tempFile.getAbsolutePath());
			var env = pb.environment();
			environment.entrySet().forEach(e -> env.put(e.getKey(), e.getValue()));
			Process process = pb.start();
			
			StringBuilder stdout = new StringBuilder();
			StringBuilder stderr = new StringBuilder();
			startOutConsumerThread(process.getInputStream(), stdout);
			startOutConsumerThread(process.getErrorStream(), stderr);
			
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new Exception(stderr.toString());
			} else {
				return stdout.toString();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new RuntimeException("Python execution failed: " + e.getMessage(), e);
		}
	}
	
	private static void startOutConsumerThread(InputStream stream, StringBuilder builder) {
		new Thread() {
			@Override
			public void run() {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream)); 
				String line = null;
				try {
					while ((line = reader.readLine()) != null) {
						if (builder.length() != 0) {
							builder.append("\n");
						}
						
						builder.append(line);
					}
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public static String getExecutablePath() {
		loadExecutableIfNotLoaded();
		return executable.substring(1);
	}
	
	private static void loadExecutableIfNotLoaded() {
		if (executable != null) return;
		
		String os = System.getProperty("os.name").toLowerCase();
		String exec = null;
		
		if (os.startsWith("windows")) {
			exec = "dist/python-win32-x64/python.exe";
		} else {
			throw new RuntimeException("PythonInterpreter does not support OS: " + os);
		}
		
		try {
			var rsc = Platform.getBundle(PythonInterpeter.class.getPackageName()).getResource(exec);
			executable = FileLocator.toFileURL(rsc).getPath();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load Python interpeter", e);
		}
	}
}
