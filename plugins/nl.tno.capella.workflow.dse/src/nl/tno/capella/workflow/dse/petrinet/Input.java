/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.petrinet;

import java.util.Collections;
import java.util.Map;

public class Input {
	final Place place;
	final Transition transition;
	final int consumedTokens;
	final Map<String, Object> properties;

	Input(Place place, Transition transition) {
		this(place, transition, 1);
	}
	
	Input(Place place, Transition transition, int consumedTokens) {
		this(place, transition, consumedTokens, null);
	}
	
	Input(Place place, Transition transition, Map<String, Object> properties) {
		this(place, transition, 1, properties);
	}
	
	Input(Place place, Transition transition, int consumedTokens, Map<String, Object> properties) {
		if (place == null || transition == null) throw new RuntimeException("Place or transitions is null");
		if (consumedTokens < 1) throw new RuntimeException("Must consume at least 1 token");
		this.place = place;
		this.transition = transition;
		this.consumedTokens = consumedTokens;
		this.properties = properties;
	}
	
	String toSnakes() {
		var label = "Value(1)";
		if (consumedTokens > 1) {
			label = String.format("MultiArc([%s])", String.join(",", Collections.nCopies(consumedTokens, label)));
		}
		var props = "";
		if (properties != null && properties.size() > 0) {
			props = String.format("\nn.transition('%s').input_props.append(%s)", 
					transition.snakesName(), Helper.propertiesToSnakes(properties));
		}
		return String.format("n.add_input('%s', '%s', %s)%s", place.snakesName(), transition.snakesName(), label, props);
	}
}
