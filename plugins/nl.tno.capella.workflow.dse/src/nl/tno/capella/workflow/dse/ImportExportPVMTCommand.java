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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.polarsys.capella.common.ef.ExecutionManagerRegistry;
import org.polarsys.capella.common.ef.command.AbstractReadWriteCommand;
import org.polarsys.capella.common.ef.command.ICommand;
import org.polarsys.capella.common.helpers.EcoreUtil2;
import org.polarsys.capella.common.helpers.TransactionHelper;

import nl.tno.capella.workflow.dse.pvmt.Exporter;
import nl.tno.capella.workflow.dse.pvmt.Importer;

public class ImportExportPVMTCommand extends AbstractHandler {	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var type = event.getParameter("nl.tno.capella.workflow.dse.commandImportExportPVMT.type");
		var selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		var emanager = ExecutionManagerRegistry.getInstance().addNewManager();
	    var res = emanager.getEditingDomain().getResourceSet().getResource(EcoreUtil2.getURI((IFile)selection.getFirstElement()), true);
		var cmd = createCommand(res, type);
		TransactionHelper.getExecutionManager(res).execute(cmd);
		return null;
	}

	protected ICommand createCommand(Resource res, String type) {
		return new AbstractReadWriteCommand() {
			@Override
			public void run() {
				var job = new Job("PVMT " + type) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							monitor.beginTask("PVMT " + type, 100);
							monitor.worked(10);
							
						    // Show file dialog
						    var selectedFile = new File[] { null };
						    Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									var display = Display.getCurrent();
									var shell = new Shell(display);
									var dialog = new FileDialog(shell, type.equals("export") ? SWT.SAVE : SWT.OPEN);
									dialog.setFileName("pvmt_export.json");
									dialog.setOverwrite(true);
									dialog.setFilterExtensions(new String[] {"*.json"});
								    var result = dialog.open();
								    selectedFile[0] = new File(result);
								}
							});
						    monitor.worked(20);
							
						    if (type.equals("export")) {
							    var json = Exporter.exportResourceToString(res);
							    monitor.worked(90);
							    Util.writeTextToFile(selectedFile[0], json, StandardCharsets.UTF_8);
				    			Util.showPopupOk(String.format("Successfully exported PVMT values to '%s'", selectedFile[0].getAbsolutePath()));
						    } else {
						    	var json = Util.readTextFromFile(selectedFile[0]);
						    	Importer.import_(res, json, (warnings) -> {
						    		if (warnings.isEmpty()) {
						    			Util.showPopupOk("Successfully imported PVMT values");
						    		} else {
						    			Util.showPopupError("Partially imported PVMT values", warnings.stream().collect(Collectors.joining("\n")));
						    		}
						    	});
						    }
						    
						    monitor.done();
						} catch (Exception e) {
							Util.showPopupError("PVMT export failed", e);
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
