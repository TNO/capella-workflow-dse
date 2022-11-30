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

import java.lang.reflect.Type;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.business.api.dialect.DialectManager;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.polarsys.capella.core.data.capellacore.AbstractPropertyValue;
import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.core.data.capellacore.EnumerationPropertyValue;
import org.polarsys.capella.core.data.capellacore.FloatPropertyValue;
import org.polarsys.capella.core.data.capellacore.IntegerPropertyValue;
import org.polarsys.capella.core.data.capellacore.PropertyValueGroup;
import org.polarsys.capella.core.data.capellacore.StringPropertyValue;
import org.polarsys.capella.core.data.fa.ControlNode;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class Exporter {
	public static String export(DRepresentationDescriptor representation) {
		var json = new JsonObject();
		var gson = getGson();
		var functionalChain = (FunctionalChain) representation.getTarget();
		var functionalChains = Util.getFunctionalChainElements(functionalChain).stream()
				.filter(ee -> ee instanceof FunctionalChain).collect(Collectors.toList());
		functionalChains.add(functionalChain);
		json.addProperty("name", representation.getName());
        json.add("functionalChains", gson.toJsonTree(functionalChains));
		return getGson().toJson(json);
	}
	
	public static String export(Resource resource) {
		return getGson().toJson(resource);
	}
	
	private static Gson getGson() {
		var builder = new GsonBuilder().serializeNulls().setPrettyPrinting();
		builder.registerTypeHierarchyAdapter(Resource.class, new JsonSerializer<Resource>() {
			@Override
			public JsonElement serialize(Resource e, Type t, JsonSerializationContext c) {
		        var json = new JsonObject();
		        json.addProperty("name", e.getURI().toString());
				var session = SessionManager.INSTANCE.getExistingSession(e.getURI());
				var functionalChains = DialectManager.INSTANCE.getAllRepresentationDescriptors(session).stream()
						.filter(i -> i.getTarget() instanceof FunctionalChain)
						.map(i -> (FunctionalChain) i.getTarget()).collect(Collectors.toList());
		        json.add("functionalChains", c.serialize(functionalChains));
		        return json;
			}
		});
		
		builder.registerTypeHierarchyAdapter(CapellaElement.class, new JsonSerializer<CapellaElement>() {
			@Override
			public JsonElement serialize(CapellaElement e, Type t, JsonSerializationContext c) {
		        var json = new JsonObject();
	        	json.addProperty("id", e.getId());

		        if (e instanceof FunctionalChain) {
		        	var fc = (FunctionalChain) e;
		        	json.addProperty("name", fc.getName());
			        var elements = Util.getFunctionalChainElements(fc).stream()
			        		.filter(ee -> !(ee instanceof FunctionalChain)).collect(Collectors.toList());
			        json.add("elements", c.serialize(elements));
		        } else {
			        if (e instanceof FunctionalChainInvolvementFunction) {
				        json.addProperty("label", ((FunctionalChainInvolvementFunction) e).getInvolved().getLabel());
			        } else if (e instanceof ControlNode) {
			        	json.addProperty("controlNodeKind", ((ControlNode) e).getKind().getLiteral());
			        }
			        
			        json.addProperty("type", e.getClass().getSimpleName().replaceAll("Impl", ""));
			        json.add("propertyValueGroups", c.serialize(e.getAppliedPropertyValueGroups().toArray()));
		        }
		        
		        return json;
			}
		});
		
		builder.registerTypeHierarchyAdapter(PropertyValueGroup.class, new JsonSerializer<PropertyValueGroup>() {
			@Override
			public JsonElement serialize(PropertyValueGroup e, Type t, JsonSerializationContext c) {
		        var json = new JsonObject();
		        json.addProperty("id", e.getId());
		        json.addProperty("label", e.getLabel());
		        var pvs = e.getOwnedPropertyValues();
		        json.add("propertyValues", c.serialize(pvs));
		        return json;
			}
		});
		
		builder.registerTypeHierarchyAdapter(AbstractPropertyValue.class, new JsonSerializer<AbstractPropertyValue>() {
			@Override
			public JsonElement serialize(AbstractPropertyValue e, Type t, JsonSerializationContext c) {
		        var json = new JsonObject();
		        json.addProperty("label", e.getLabel());
		        
		        Object value = null;
		        var type = new JsonObject();
    			if (e instanceof IntegerPropertyValue) {
    				value = ((IntegerPropertyValue) e).getValue();
    				type.addProperty("type", "int");
    			} else if (e instanceof FloatPropertyValue) {
    				value = ((FloatPropertyValue) e).getValue();
    				type.addProperty("type", "float");
    			} else if (e instanceof StringPropertyValue) {
    				value = ((StringPropertyValue) e).getValue();
    				type.addProperty("type", "string");
    			} else if (e instanceof EnumerationPropertyValue) {
    				if (((EnumerationPropertyValue) e).getValue() != null) {
        				value = ((EnumerationPropertyValue) e).getValue().getName();
    				}
    				type.addProperty("type", "enum");
    				type.add("values",  c.serialize(((EnumerationPropertyValue) e).getType().getOwnedLiterals()
    						.stream().map(i -> i.getName()).collect(Collectors.toList())));
    			} else {
    				throw new RuntimeException(String.format("Property of type '%s' is not supported", e.getClass().getName()));
    			}
		        json.addProperty("id", e.getId());
    			json.add("value", c.serialize(value));
    			json.add("type", type);

    			return json;
			}
		});
		
		return builder.create();
	}
}
