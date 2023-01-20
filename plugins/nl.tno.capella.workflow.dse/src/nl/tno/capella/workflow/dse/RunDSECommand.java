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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sirius.business.api.dialect.DialectManager;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.polarsys.capella.common.ef.command.AbstractReadWriteCommand;
import org.polarsys.capella.common.ef.command.ICommand;
import org.polarsys.capella.common.helpers.EcoreUtil2;
import org.polarsys.capella.common.helpers.TransactionHelper;
import org.polarsys.capella.core.data.epbs.ConfigurationItemPkg;
import org.polarsys.capella.core.data.fa.FunctionalChain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import nl.tno.capella.workflow.dse.app.DSE;
import nl.tno.capella.workflow.dse.petrinet.PetriNet;
import nl.tno.capella.workflow.dse.pvmt.Exporter;
import nl.tno.capella.workflow.dse.python.PythonInterpeter;
import nl.tno.capella.workflow.dse.walker.Walker;

public class RunDSECommand extends AbstractHandler {
	
	private enum RunMode { POOSL, PYTHON }
	
	private final Object processLock = new Object();
	private Process process = null;
	private Thread simulationThread = null;	
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	public RunDSECommand() {
		// Make sure the DSE tool is stopped when Eclipse is stopped
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				synchronized (processLock) {
					if (process != null) {
						process.destroy();
						process = null;
					}
				}
			}
		});
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var fc = Util.getFunctionalChain("Run DSE failed", event);
		if (fc == null) return null;
		var cmd = createCommand(fc);
		TransactionHelper.getExecutionManager(fc).execute(cmd);
		return null;
	}

	protected ICommand createCommand(FunctionalChain functionalChain) {
		return new AbstractReadWriteCommand() {
			@Override
			public void run() {
				var job = new Job("Running DSE...") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						synchronized (processLock) {
							if (process != null) {
								Util.showPopupError("DSE already running", "");
								return Status.CANCEL_STATUS;
							}
						}

						monitor.beginTask("Run DSE", 100);
						monitor.worked(10);
						try {
							var walker = new Walker(functionalChain);
							if (walker.hasError()) {
								throw new RuntimeException("Cannot generate due to errors in model, see 'Problems' view");
							}
							monitor.worked(25);
							
							var project = EcoreUtil2.getProject(functionalChain);
							var projectPath = project.getLocation().toFile();
							var session = SessionManager.INSTANCE.getSession(functionalChain);
							var genPath = Paths.get(projectPath.getAbsolutePath(), "gen");
							var dsePath = Paths.get(genPath.toString(), "dse");
							var representationDescriptor = DialectManager.INSTANCE.getAllRepresentationDescriptors(session)
					        		.stream().filter(d -> d.getTarget() == functionalChain).findFirst().get();
							Util.removeDirectory(dsePath);
					        Files.createDirectories(dsePath);
							
							// dse.json
					        var gson = Exporter.getGson();
					        var dseFile = Paths.get(dsePath.toString(), "dse.json").toFile();
							var pvmtJson = Exporter.exportRepresentationToJsonObject(representationDescriptor);
							var dseJson = new JsonObject();
							dseJson.add("configurationItems", gson.toJsonTree(getConfigurationItemNames(session)));
							dseJson.add("pvmt", pvmtJson);
							Util.writeTextToFile(dseFile, gson.toJson(dseJson), StandardCharsets.UTF_8);
							
							// svg
							var svgFile = Paths.get(dsePath.toString(), "diagram.svg").toFile();
							Util.exportDiagramAsSvg(representationDescriptor, project, svgFile, session);
							
							monitor.worked(50);
							startDSETool(functionalChain, dsePath, project);
							
							monitor.done();
						} catch (Exception e) {
							Util.showPopupError("Run DSE failed", e.getMessage());
							e.printStackTrace();
						}
						
						synchronized (processLock) {
							process = null;
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	private void startDSETool(FunctionalChain fc, Path dsePath, IProject prj) throws IOException, InterruptedException {
		var projectPath = prj.getLocation().toFile().toString();
		var pb = new ProcessBuilder(DSE.getExecutable(), projectPath);
		
		synchronized (processLock) {
			this.process = pb.start();
		}
		startOutConsumerThread(process.getErrorStream(), (line) -> {});
		startOutConsumerThread(process.getInputStream(), (line) -> {
			if (line.equals("")) return;
			var action = new Gson().fromJson(line, Map.class);
			if (action.get("action").toString().equals("run")) {
				if (simulationThread != null && simulationThread.isAlive()) {
					simulationThread.interrupt();
				}
				simulationThread = new Thread(() -> {
					var overrideProperties = new HashMap<String, Map<String, Object>>();
					var pvmt = (Map<String, Object>) action.get("pvmt");
					((List<Map<String, Object>>) pvmt.get("functionalChains")).forEach(f -> {
						((List<Map<String, Object>>) f.get("elements")).forEach(e -> {
							((List<Map<String, Object>>) e.get("propertyValueGroups")).forEach(pvg -> {
								((List<Map<String, Object>>) pvg.get("propertyValues")).forEach(pv -> {
									overrideProperties.put((String) pv.get("id"), pv);
								});
							});
						});
					});
					
					var walker = new Walker(fc, overrideProperties);
					var net = new PetriNet(walker);
					try {
						var name = action.get("name").toString();
						var mode = RunMode.valueOf(action.get("mode").toString().toUpperCase());
						outputAndRunNet(net, dsePath, name, line, mode);
					} catch (Exception e) {
						e.printStackTrace();
						Util.showPopupError("Run DSE failed", e.getMessage());
					}
					Util.refreshProject(prj);
				});
				simulationThread.start();
			} else if (action.get("action").toString().equals("clear-runs")) {
				try {
					for (var file : dsePath.toFile().listFiles()) {
						if (file.isDirectory()) {
							Util.removeDirectory(file.toPath());
						}
					}
					Util.refreshProject(prj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (action.get("action").toString().equals("cancel-run")) {
				if (simulationThread != null && simulationThread.isAlive()) {
					simulationThread.interrupt();
				}
			} else if (action.get("action").toString().equals("save-definitions")) {
				var definitions = action.get("definitions");
				var json = gson.toJson(definitions);
				var jsonFile = Paths.get(projectPath, "DSE-definitions.json");
				try {
					Util.writeTextToFile(jsonFile.toFile(), json, StandardCharsets.UTF_8);
					Util.refreshProject(prj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		this.process.waitFor();
	}
	
	private List<String> getConfigurationItemNames(Session session) {
		return DialectManager.INSTANCE.getAllRepresentationDescriptors(session).stream()
				.filter((t) -> t.getTarget() instanceof ConfigurationItemPkg).map((t) -> (ConfigurationItemPkg) t.getTarget())
				.map((t) -> t.getOwnedConfigurationItems()).flatMap(Collection::stream).map((t) -> t.getName()).collect(Collectors.toList());
	}
	
	private void outputAndRunNet(PetriNet net, Path dsePath, String name, String pvmt, RunMode mode) throws URISyntaxException, IOException, CoreException, InterruptedException {
		var location = Paths.get(dsePath.toString(), name);
		Util.removeDirectory(location);

		if (mode == RunMode.POOSL) {
			var pooslFile = Paths.get(location.toString(), "net.poosl").toFile();
			var rotalumisFile = Paths.get(location.toString(), "rotalumis.txt").toFile();
			Util.copyResourceDir("poosl", location);
			Util.writeTextToFile(pooslFile, net.toPOOSL(), StandardCharsets.ISO_8859_1);
			var output = Util.runRotalumis(pooslFile);
			Util.writeTextToFile(rotalumisFile, output, StandardCharsets.ISO_8859_1);
		} else {
			Util.copyResourceDir("snakes", location);
			var simFile = Paths.get(location.toString(), "sim.py").toFile();
			String bat = String.format("\"%s\" \"%s\" --verbose --loop", PythonInterpeter.getExecutablePath(), simFile);
			Util.writeTextToFile(Paths.get(location.toString(), "sim.bat").toFile(), bat, StandardCharsets.UTF_8);
			Util.writeTextToFile(Paths.get(location.toString(), "net.py").toFile(), net.toSnakes(), StandardCharsets.UTF_8, true);
			PythonInterpeter.execute(simFile, new HashMap<String, String>());
		}
		
		var pvmtFile = Paths.get(location.toString(), "pvmt.json").toFile();
		Util.writeTextToFile(pvmtFile, gson.toJson(gson.fromJson(pvmt, JsonObject.class)), StandardCharsets.UTF_8);
	}
	
	private static void startOutConsumerThread(InputStream stream, Consumer<String> handleLine) {
		new Thread() {
			@Override
			public void run() {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream)); 
				String line = null;
				try {
					while ((line = reader.readLine()) != null) {
						handleLine.accept(line);
					}
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
