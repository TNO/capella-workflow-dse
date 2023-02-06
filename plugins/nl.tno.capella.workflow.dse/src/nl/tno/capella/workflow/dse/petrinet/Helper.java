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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class Helper {
	static String propertiesToSnakes(Map<String, Object> properties) {
		var result = "{";
		if (properties != null && properties.size() > 0) {
			result += "\n";
			for (var prop : properties.entrySet()) {
				result += String.format("\t'%s': ", prop.getKey());
				
				var value = prop.getValue();
				if (value == null) {
					result += "''";
				} else if (value instanceof Integer) {
					result += String.format(Locale.ROOT, "%d", value);
				} else if (value instanceof Float) {
					result += String.format(Locale.ROOT, "%f", value);
				} else if (value instanceof List<?>) {
					result += String.format("[%s]", ((List<?>) value).stream()
							.map(s -> String.format("'%s'", s)).collect(Collectors.joining(", ")));
				} else if (value instanceof String) {
					result += String.format("'%s'", value);
				}
				result += ",\n";
			}
		}

		result += "}";
		return result;
	}
}
