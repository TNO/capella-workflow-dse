/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.pvmt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.polarsys.capella.core.data.fa.ControlNode;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementFunction;
import org.polarsys.capella.core.data.fa.FunctionalChainReference;
import org.polarsys.capella.core.data.fa.SequenceLink;

public class Util {
	
	static List<EObject> getFunctionalChainElements(FunctionalChain fc) {
		var elements = new ArrayList<EObject>();
		fc.eAllContents().forEachRemaining(obj -> {
			if (obj instanceof FunctionalChainReference) {
				elements.add((FunctionalChain)((FunctionalChainReference) obj).getInvolved());
			} else if (obj instanceof SequenceLink || obj instanceof FunctionalChainInvolvementFunction || 
					obj instanceof ControlNode) {
				elements.add(obj);
			}
		});
		return elements;
	}
}
