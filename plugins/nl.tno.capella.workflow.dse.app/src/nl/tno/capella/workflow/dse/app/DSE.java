/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.app;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

public class DSE {
	private static String executable;
	
	public static String getExecutable() {
		loadExecutableIfNotLoaded();
		return executable;
	}
	
	private static void loadExecutableIfNotLoaded() {
		if (executable != null) return;
		
		var os = System.getProperty("os.name").toLowerCase();
		String exec = null;
		
		if (os.startsWith("windows")) {
			exec = "dist/dse-win32-x64/dse.exe";
		} else {
			throw new RuntimeException("DSE does not support OS: " + os);
		}
		
		try {
			var rsc = Platform.getBundle(DSE.class.getPackageName()).getResource(exec);
			executable = FileLocator.toFileURL(rsc).getPath();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load Python interpeter", e);
		}
	}
} 
