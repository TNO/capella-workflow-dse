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

public class Output {
	final Place place;
	final Transition transition;
	final Map<String, Object> properties;
	int producedTokens;

	Output(Place place, Transition transition) {
		this(place, transition, 1);
	}
	
	Output(Place place, Transition transition, int producedTokens) {
		this(place, transition, producedTokens, null);
	}
	
	Output(Place place, Transition transition, Map<String, Object> properties) {
		this(place, transition, 1, properties);
	}
	
	Output(Place place, Transition transition, int producedTokens, Map<String, Object> properties) {
		if (place == null || transition == null) throw new RuntimeException("Place or transitions is null");
		if (producedTokens < 1) throw new RuntimeException("Must produce at least 1 token");
		this.place = place;
		this.transition = transition;
		this.producedTokens = producedTokens;
		this.properties = properties;
	}

	String toSnakes() {
		var label = "Value(1)";
		if (producedTokens > 1) {
			label = String.format("MultiArc([%s])", String.join(",", Collections.nCopies(producedTokens, label)));
		}
		var props = "";
		if (properties != null && properties.size() > 0) {
			props = String.format("\nn.transition('%s').output_props.append(%s)", 
					transition.snakesName(), Helper.propertiesToSnakes(properties));
		}
		return String.format("n.add_output('%s', '%s', %s)%s", place.snakesName(), transition.snakesName(), label, props);
	}
}
