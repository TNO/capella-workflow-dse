/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.common.tools.api.resource.ImageFileFormat;
import org.eclipse.sirius.ui.business.api.dialect.DialectUIManager;
import org.eclipse.sirius.ui.business.api.dialect.ExportFormat;
import org.eclipse.sirius.ui.business.api.dialect.ExportFormat.ExportDocumentFormat;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.model.handler.helpers.CapellaAdapterHelper;

@SuppressWarnings("restriction")
class Util {
	static void showPopupOk(String message) {
		showPopup("ok", message, null);
	}
	
	static void showPopupError(String message, String details) {
		showPopup("error", message, details);
	}
	
	static void showPopupError(String message, Exception exception) {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		showPopup("error", message, exception.getMessage() + "\n" + sw.toString());
	}
	
	private static void showPopup(String type, String message, String details) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				var display = Display.getCurrent();
				var shell = new Shell(display);
				if (type.equals("ok")) {
					var dialog = new MessageDialog(shell, "OK", null, message,  MessageDialog.NONE, 0, new String[]{IDialogConstants.OK_LABEL});
					dialog.open();
				} else {
					var statuses = Arrays.stream(details.split("\n")).map(l -> new Status(IStatus.ERROR, Activator.PLUGIN_ID, l)).collect(Collectors.toList());
					var status = new MultiStatus(Activator.PLUGIN_ID, IStatus.WARNING, statuses.toArray(new Status[] {}), null, null);
					ErrorDialog.openError(shell, "Error", message, status);
				}
			}
		});
	}
	
	static String getVersion() throws IOException {
		var file = new File(Platform.getInstallLocation().getURL().getFile().toString() + "version.txt");
		return file.exists() ? readTextFromFile(file) : "unknown";
	}
	
	static void writeTextToFile(File file, String text, Charset charSet) throws IOException {
		writeTextToFile(file, text, charSet, false);
	}
	
	static void writeTextToFile(File file, String text, Charset charSet, boolean addVersionHeader) throws IOException {
		if (addVersionHeader) {
			var commentMark = "";
			if (file.toString().endsWith(".py")) commentMark = "#";
			else if (file.toString().endsWith(".poosl")) commentMark = "//";
			else throw new RuntimeException("Unsupported file extension for version header: " + file.toString());
			text = commentMark + " Generated by capella-tfs " + getVersion() + "\n" + text;
		}
		
		try (var bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charSet))) {
			bw.write(text);
		}
	}
	
	static String readTextFromFile(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}
	
	static void refreshProject(IProject prj) {
		try {
			prj.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	static void waitTillFileExists(File file, int timeout) throws InterruptedException {
		var interval = 500;
		for (int i = 0; i < ((timeout * 1000) / interval); i++) {
			if (file.exists()) return;
			Thread.sleep(interval);
		}
		
		throw new RuntimeException("Timeout while waiting for file to be created: " + file.toString());
	}

	static void copyResourceDir(String resourceDir, Path target) throws URISyntaxException, IOException {
		var packageName = Util.class.getPackage().getName();
		var resourceURL = Platform.getBundle(packageName).getResource(resourceDir);
		var source = Paths.get(FileLocator.toFileURL(resourceURL).getPath().substring(1));
		Files.walk(source).forEach(sourceFile -> {
			if (Files.isRegularFile(sourceFile)) {
				var targetFile = target.resolve(source.relativize(sourceFile));
				try {
					Files.createDirectories(targetFile.getParent());
					Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	
	static void exportDiagramAsSvg(DRepresentationDescriptor descriptor, IProject project, File svgFile, Session session) throws InterruptedException {
		var output = new org.eclipse.core.runtime.Path(svgFile.toString());
		var exportFormat = new ExportFormat(ExportDocumentFormat.NONE, ImageFileFormat.SVG, ExportFormat.ScalingPolicy.AUTO_SCALING);
		exportFormat.setSemanticTraceabilityEnabled(true);
		var attempts = 3;
		
		Display.getDefault().syncExec(new Runnable(){
			public void run() {
				for (int i = 0; i < attempts; i++) {
					try {
						Util.refreshProject(project);
						DialectUIManager.INSTANCE.export(descriptor.getRepresentation(), session, output, exportFormat, new NullProgressMonitor(), false);
						break;
					} catch (Exception e) {
						System.err.println(String.format("SVG export attempt %d failed", i));
						e.printStackTrace();
					}
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		Util.waitTillFileExists(svgFile, 5);
	}
	
	static FunctionalChain getFunctionalChain(String errorTitle, ExecutionEvent event) {
		var selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof IStructuredSelection)) {
			Util.showPopupError(errorTitle, "No functional chain selected");
			return null;
		}
		var structuredSelection = (IStructuredSelection) selection;
		var semanticElements = CapellaAdapterHelper.resolveSemanticObjects(structuredSelection.toList()).stream().collect(Collectors.toList());
		var functionalChain = semanticElements.stream().filter(s -> s instanceof FunctionalChain).findFirst();
		if (!functionalChain.isPresent()) {
			Util.showPopupError(errorTitle, "No functional chain selected");
			return null;
		} 
		return (FunctionalChain) functionalChain.get();
	}
	
	
	static void removeDirectory(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}
	
	static String runRotalumis(File file) throws IOException, InterruptedException {
		var bundle = Platform.getBundle("nl.tue.rotalumis.executables");
		var path = new org.eclipse.core.runtime.Path("windows/64bit/rotalumis.exe");
		var fileURL = FileLocator.find(bundle, path, null);
		var t = FileLocator.toFileURL(fileURL).getFile().toString();
		var pb = new ProcessBuilder(t.substring(1), "--stdlib", "--poosl", "../" + file.getName());
		var simulatorPath = Paths.get(file.getParentFile().toString(), "simulator").toFile();
		if (!simulatorPath.exists()) simulatorPath.mkdir();
		pb.directory(simulatorPath);
		var process = pb.start();
		var in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		var lines = new ArrayList<String>();
		String line;
		while ((line = in.readLine()) != null) {
			lines.add(line);
		}
		process.waitFor();
		return String.join("\n", lines);
	}
}
