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

public class Place {
	
	enum PlaceType { 
		N, // NORMAL
		ITS, // ITERATE START
		ITE, // ITERATE END
		ITG, // ITERATE GENERATOR
		ITL, // ITERATE LOOP
		START, // START
		END, // END
		ANDS, // AND START
		ANDE, // AND END
		ORS, // OR START
		ORST, // OR START TRANSITION (POOSL only)
		ORE, // OR END
		RES, // RESOURCE
	}
	
	final int id;
	final PlaceType type;
	final int tokens;
	final String name;
	final Map<String, Object> properties;
	
	Place(int id, PlaceType type) {
		this(id, type, 0);
	}
	
	Place(int id, PlaceType type, Map<String, Object> properties) {
		this(id, type, 0, properties);
	}
	
	Place(int id, PlaceType type, int tokens) {
		this(id, type, tokens, null, null);
	}
	
	Place(int id, PlaceType type, int tokens, String name) {
		this(id, type, tokens, null, name);
	}
	
	Place(int id, PlaceType type, int tokens, Map<String, Object> properties) {
		this(id, type, tokens, properties, null);
	}
	
	Place(int id, PlaceType type, int tokens, Map<String, Object> properties, String name) {
		this.id = id;
		this.type = type;
		this.tokens = tokens;
		this.properties = properties;
		this.name = name;
	}

	String snakesName() {
		var name = this.name != null ? "_" + this.name : "";
		return String.format("%s%d%s", this.type.name(), id, name);
	}
	
	String toSnakes() {
		var token = "";
		if (tokens > 0) {
			token = String.format(", [%s]", String.join(",", Collections.nCopies(tokens, "1")));
		}
		var result = String.format("p = Place('%s'%s)\n", snakesName(), token);
		result += String.format("p.props = %s\n", Helper.propertiesToSnakes(properties));
		return  result + "n.add_place(p)";
	}
}
