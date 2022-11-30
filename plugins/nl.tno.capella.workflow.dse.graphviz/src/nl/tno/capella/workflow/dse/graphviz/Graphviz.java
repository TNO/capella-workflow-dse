/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.graphviz;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

public class Graphviz {
	private static String binPath;

	public static String getBinPath() {
		loadBinPathIfNotLoaded();
		return binPath;
	}
	
	private static void loadBinPathIfNotLoaded() {
		if (binPath != null) return;
		
		var os = System.getProperty("os.name").toLowerCase();
		String bin = null;
		
		if (os.startsWith("windows")) {
			bin = "dist/graphviz-win32-x64/Graphviz/bin";
		} else {
			throw new RuntimeException("Graphviz does not support OS: " + os);
		}
		
		try {
			var rsc = Platform.getBundle(Graphviz.class.getPackageName()).getResource(bin);
			binPath = FileLocator.toFileURL(rsc).getPath();
			if (os.startsWith("windows")) {
				binPath = binPath.substring(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load Graphviz", e);
		}
	}
}
