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

import java.util.ArrayList;
import java.util.List;

import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.core.data.fa.ControlNode;

class StackEntry {
	CapellaElement node;
	List<ControlNode> controlNodes;
	
	StackEntry(CapellaElement node) {
		this(node, new ArrayList<>());
	}
	
	StackEntry(CapellaElement node, List<ControlNode> controlNodes) {
		this.node = node;
		this.controlNodes = controlNodes;
	}
	
	StackEntry clone(CapellaElement node) {
		return new StackEntry(node, controlNodes);
	}
			
	StackEntry addControlNodeAndClone(ControlNode node) {
		var controlNodes = new ArrayList<>(this.controlNodes);
		controlNodes.add(node);
		return new StackEntry(node, controlNodes);
	}
	
	StackEntry removeLastControlNodeAndClone(ControlNode node) {
		var controlNodes = new ArrayList<>(this.controlNodes.subList(0, this.controlNodes.size() - 1));
		return new StackEntry(node, controlNodes);
	}
	
	ControlNode lastControlNode() {
		return controlNodes.size() > 0 ? controlNodes.get(controlNodes.size() - 1) : null;
	}
}