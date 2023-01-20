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

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.polarsys.capella.common.ef.command.AbstractReadWriteCommand;
import org.polarsys.capella.common.ef.command.ICommand;
import org.polarsys.capella.common.helpers.EcoreUtil2;
import org.polarsys.capella.common.helpers.TransactionHelper;
import org.polarsys.capella.core.data.fa.FunctionalChain;

import nl.tno.capella.workflow.dse.petrinet.PetriNet;
import nl.tno.capella.workflow.dse.python.PythonInterpeter;

public class ExportCommand extends AbstractHandler {	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var fc = Util.getFunctionalChain("Export failed", event);
		if (fc == null) return null;
		var cmd = createCommand(fc);
		TransactionHelper.getExecutionManager(fc).execute(cmd);
		return null;
	}

	protected ICommand createCommand(FunctionalChain functionalChain) {
		return new AbstractReadWriteCommand() {
			@Override
			public void run() {
				var job = new Job("Export") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Export", 100);
						monitor.worked(10);
						try {
							var net = new PetriNet(functionalChain);
							monitor.worked(50);
							var project = EcoreUtil2.getProject(functionalChain);
							var projectPath = project.getLocation().toFile();
							var genPath = Paths.get(projectPath.getAbsolutePath(), "gen");
							var pooslPath = Paths.get(genPath.toString(), "poosl");
							var snakesPath = Paths.get(genPath.toString(), "snakes");

							Util.removeDirectory(pooslPath);
							Util.removeDirectory(snakesPath);

							Util.copyResourceDir("poosl", Paths.get(genPath.toString(), "poosl"));
							Util.copyResourceDir("snakes", Paths.get(genPath.toString(), "snakes"));
							
							String bat = String.format("\"%s\" \"%s\" --verbose --loop", PythonInterpeter.getExecutablePath(), Paths.get(genPath.toString(), "snakes", "sim.py").toFile());
							Util.writeTextToFile(Paths.get(genPath.toString(), "snakes", "sim.bat").toFile(), bat, StandardCharsets.UTF_8);

							Util.writeTextToFile(Paths.get(genPath.toString(), "snakes", "net.py").toFile(), net.toSnakes(), StandardCharsets.UTF_8, true);
							net.draw(Paths.get(genPath.toString(), "snakes", "net.png").toFile());
							Util.writeTextToFile(Paths.get(genPath.toString(), "poosl", "net.poosl").toFile(), net.toPOOSL(), StandardCharsets.ISO_8859_1, true);
							
							monitor.worked(90);

							project.refreshLocal(IResource.DEPTH_INFINITE, null);
							Util.showPopupOk("Export finished");
							monitor.done();
						} catch (Exception e) {
							Util.showPopupError("Export failed", e.getMessage());
							e.printStackTrace();
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		};
	}
}
