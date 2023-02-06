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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Transition {
	enum TransitionType { 
		N, // NORMAL
		FUNCS, // FUNCTION START (SNAKES only)
		FUNCE, // FUNCTION END (SNAKES only)
		FUNC, // FUNCTION
		AND, // AND
		ITS, // ITERATE START
		ITE, // ITERATE END
		ITL, // ITERATE LOOP
		ITG, // ITERATE GENERATOR
		OR, // OR
		ORC, // OR CHOICE (POOSL only)
		RES, // RESOURCE
	}

	final int id;
	final String name;
	final TransitionType type;
	final Map<String, Object> properties;
	final String guard;
	final int branchDepth;
	final List<String> levels;
	
	Transition(int id, TransitionType type, int branchDepth) {
		this(id, type, branchDepth, null, null, null, null);
	}
	
	Transition(int id, TransitionType type, int branchDepth, String guard) {
		this(id, type, branchDepth, null, null, null, guard);
	}
	
	Transition(int id, TransitionType type, int branchDepth, String name, Map<String, Object> properties, List<String> levels) {
		this(id, type, branchDepth, name, properties, levels, null);
	}
	
	Transition(int id, TransitionType type, int branchDepth, String name, Map<String, Object> properties, List<String> levels, String guard) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.properties = properties;
		this.guard = guard;
		this.branchDepth = branchDepth;
		this.levels = levels;
	}
	
	String pooslName() {
		return this.name != null ? name : String.format("%s%d", this.type.name(), id);
	}
	
	String snakesName() {
		return String.format("%s%d%s", this.type.name(), id, this.name == null ? "" : "_" + this.name);
	}
	
	List<String> getLevelNames(int maxLevel) {
		var level = getLevel();
		return IntStream.range(0, maxLevel).mapToObj(index -> {
			var levelName = "x";
			if (levels != null) {
				if (levels.size() > index) levelName = levels.get(index);
				else if (levels.size() == index) levelName = name;
				else levelName = String.format("level%d", level);
			}
			return levelName;
		}).collect(Collectors.toList());
	}
	
	int getLevel() {
		return levels != null ? levels.size() + 1 : 1;
	}
	
	String toSnakes(int maxLevel) {
		var guard = this.guard != null ? String.format(", Expression('%s')", this.guard) : "";
		var result = String.format("t = Transition('%s'%s)\n", snakesName(), guard);
		var props = this.properties != null ? new HashMap<>(properties) : new HashMap<String, Object>();
		if (!props.containsKey("ResourceID") || props.get("ResourceID") == null || props.get("ResourceID").toString().strip().equals("")) {
			props.put("ResourceID", String.format("t%dUnknown", this.id));
		}
		props.put("Level", getLevel());
		props.put("LevelNames", getLevelNames(maxLevel));
		
		result += String.format("t.props = %s\n", Helper.propertiesToSnakes(props));
		result += String.format("t.branch_depth = %d\n", branchDepth);
		result += "n.add_transition(t)";
		return result;
	}
}
