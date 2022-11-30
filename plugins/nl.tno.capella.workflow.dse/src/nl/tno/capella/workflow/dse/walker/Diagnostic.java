/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.walker;

import org.eclipse.emf.ecore.EObject;

public class Diagnostic {
	public final Severity serverity;
	public final String message;
	public final EObject element;
	
	public Diagnostic(Severity severity, String message, EObject element) {
		this.serverity = severity;
		this.message = message;
		this.element = element;
	}
}
