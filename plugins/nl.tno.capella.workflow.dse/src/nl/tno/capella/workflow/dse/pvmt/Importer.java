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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.business.api.dialect.DialectManager;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.polarsys.capella.common.ef.command.AbstractReadWriteCommand;
import org.polarsys.capella.common.helpers.TransactionHelper;
import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.core.data.capellacore.CapellacoreFactory;
import org.polarsys.capella.core.data.fa.FunctionalChain;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Importer {
	public static void import_(Resource resource, String jsonText, Consumer<List<String>> callback) {
		var session = SessionManager.INSTANCE.getExistingSession(resource.getURI());
		var functionalChains = DialectManager.INSTANCE.getAllRepresentationDescriptors(session)
				.stream().filter(d -> d.getTarget() instanceof FunctionalChain)
				.map(d -> (FunctionalChain) d.getTarget()).collect(Collectors.toList());
		
		TransactionHelper.getExecutionManager(functionalChains).execute((new AbstractReadWriteCommand() {
	        @Override
			public void run() {
	    		var warnings = new ArrayList<String>();

	    		try {
		    		var json = new Gson().fromJson(jsonText, JsonObject.class);
		    		for (var fcE : json.getAsJsonArray("functionalChains")) {
		    			var fcJ = fcE.getAsJsonObject();
		    			var fcId = fcJ.get("id").getAsString();
		    			var fc = functionalChains.stream().filter(d -> d.getId().equals(fcId)).findFirst();
		    			if (fc.isEmpty()) {
		    				warnings.add(String.format("Functional chain '%s' does not exist", fcJ.get("name").getAsString()));
		    				continue;
		    			}
		    			
		    			var elements = Util.getFunctionalChainElements(fc.get()).stream()
		    					.filter(e -> e instanceof CapellaElement).map(e -> (CapellaElement) e)
		    					.collect(Collectors.toList());
		    			for (var elementE : fcJ.getAsJsonArray("elements")) {
		    				var elementJ = elementE.getAsJsonObject();
		    				var id = elementJ.get("id").getAsString();
		    				var element = elements.stream().filter(e -> e.getId().equals(id)).findFirst();
		    				if (element.isEmpty()) {
			    				var elementLabelOrId = elementJ.get(elementJ.has("label") ? "label" : "id").getAsString();
		    					warnings.add(String.format("Element '%s' does not exist", elementLabelOrId));
		    					continue;
		    				}
		    				
		    				applyPropertyValueGroups(element.get(), elementJ.getAsJsonArray("propertyValueGroups"));
		    			}
		    		}		    		
	    		} catch (Exception ex) {
	    			warnings.add("Exception while importing: " + ex.getMessage());
	    			ex.printStackTrace();
	    		}
	    		
	    		callback.accept(warnings);
	        }
		}));
	}
	
	private static void applyPropertyValueGroups(CapellaElement element, JsonArray groups) {
		element.getAppliedPropertyValueGroups().clear();
		element.getOwnedPropertyValueGroups().clear();
		for (var groupE : groups) {
			var groupJ = groupE.getAsJsonObject();
			var groupLabel = groupJ.get("label").getAsString();
			var group = CapellacoreFactory.eINSTANCE.createPropertyValueGroup(groupLabel);
			element.getAppliedPropertyValueGroups().add(group); 
			element.getOwnedPropertyValueGroups().add(group);

			for (var propE : groupJ.get("propertyValues").getAsJsonArray()) {
				var propJ = propE.getAsJsonObject();
				var propLabel = propJ.get("label").getAsString();
				var propType = propJ.get("type").getAsJsonObject();
				var value = propJ.get("value");
				if (value.isJsonNull()) {
					// Do nothing
				} else if (propType.get("type").getAsString().equals("string")) {
					var prop = CapellacoreFactory.eINSTANCE.createStringPropertyValue(propLabel);
					prop.setValue(value.getAsString());
					group.getOwnedPropertyValues().add(prop);
				} else if (propType.get("type").getAsString().equals("int")) {
					var prop = CapellacoreFactory.eINSTANCE.createIntegerPropertyValue(propLabel);
					prop.setValue(value.getAsInt());
					group.getOwnedPropertyValues().add(prop);
				} else if (propType.get("type").getAsString().equals("float")) {
					var prop = CapellacoreFactory.eINSTANCE.createFloatPropertyValue(propLabel);
					prop.setValue(value.getAsFloat());
					group.getOwnedPropertyValues().add(prop);
				} else if (propType.get("type").getAsString().equals("enum")) {
					var prop = CapellacoreFactory.eINSTANCE.createEnumerationPropertyValue(propLabel);
					var type = CapellacoreFactory.eINSTANCE.createEnumerationPropertyType();
					prop.setType(type);
					prop.getOwnedEnumerationPropertyTypes().add(type);
					for (var v : propType.getAsJsonArray("values")) {
						var literal = CapellacoreFactory.eINSTANCE.createEnumerationPropertyLiteral(v.getAsString());
						type.getOwnedLiterals().add(literal);
						if (v.getAsString().equals(value.getAsString())) {
							prop.setValue(literal);
						}
					}
					group.getOwnedPropertyValues().add(prop);
				} else {
					throw new RuntimeException("Unsupported type: " + propType.get("type").getAsString());
				}
			}
	    }
	}
}
