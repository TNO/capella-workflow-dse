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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.core.data.capellacore.EnumerationPropertyValue;
import org.polarsys.capella.core.data.capellacore.FloatPropertyValue;
import org.polarsys.capella.core.data.capellacore.IntegerPropertyValue;
import org.polarsys.capella.core.data.capellacore.StringPropertyValue;
import org.polarsys.capella.core.data.fa.ControlNode;
import org.polarsys.capella.core.data.fa.ControlNodeKind;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementFunction;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementLink;
import org.polarsys.capella.core.data.fa.FunctionalChainReference;
import org.polarsys.capella.core.data.fa.SequenceLink;

public class Walker {
	private final Map<ControlNode, ControlNode> startEndLookup = new HashMap<>();
	private final Map<ControlNode, Integer> iterateRepition = new HashMap<>();
	private final Map<ControlNode, Integer> andOrBranches = new HashMap<>();
	private final Map<CapellaElement, Integer> branchDepth = new HashMap<>();
	private final Map<FunctionalChainInvolvementFunction, List<String>> functionParentFCs = new HashMap<>();
	private final FunctionalChain functionalChain;
	private final Map<String, Map<String, Object>> overrideProperties;
	
	final Map<EObject, Map<String, Object>> properties = new HashMap<>();
	
	public final List<Diagnostic> diagnostics = new ArrayList<>();
	public final FunctionalChainInvolvementFunction startFunction;
	public final FunctionalChainInvolvementFunction endFunction;
	public final List<FunctionalChainInvolvementFunction> functions = new ArrayList<>();
	public final List<ControlNode> controlNodes = new ArrayList<ControlNode>();
	public final List<SequenceLink> links = new ArrayList<SequenceLink>();
	
	public Walker(FunctionalChain functionalChain) {
		this(functionalChain, null);
	}
	
	public Walker(FunctionalChain functionalChain, Map<String, Map<String, Object>> overrideProperties) {
		this.functionalChain = functionalChain;
		this.overrideProperties = overrideProperties;
		this.discoverFunctionalChainElements(functionalChain, new ArrayList<String>());
		this.startFunction = getStartFunction();
		this.endFunction = getEndFunction();
		if (hasError()) return;
		this.walk();
	}

	private FunctionalChainInvolvementFunction getStartFunction() {
		var start = functions.stream().filter(f -> !links.stream().anyMatch(l -> l.getTarget() == f)).collect(Collectors.toList());
		return check(start.size() == 1, Severity.ERROR, "No or multiple start functions found, exactly 1 function with no incoming links should exist", functionalChain) ?
				start.get(0) : null;
	}
	
	private FunctionalChainInvolvementFunction getEndFunction() {
		var end = functions.stream().filter(f -> !links.stream().anyMatch(l -> l.getSource() == f)).collect(Collectors.toList());
		return check(end.size() == 1, Severity.ERROR, "No or multiple end functions found, exactly 1 function with no outgoing links should exist", functionalChain) ?
				end.get(0) : null;
	}
	
	public int getBranchDepth(CapellaElement element) {
		return this.branchDepth.get(element);
	}
	
	public boolean isStartEndPair(ControlNode start, ControlNode end) {
		return startEndLookup.containsKey(start) && startEndLookup.get(start) == end;
	}
	
	public List<String> getParentFunctionalChains(FunctionalChainInvolvementFunction function) {
		return this.functionParentFCs.get(function);
	}
	
	public boolean isStart(ControlNode node) {
		return startEndLookup.containsKey(node);
	}
	
	public boolean isEnd(ControlNode node) {
		return startEndLookup.containsValue(node);
	}
	
	public int getIterateRepitition(ControlNode node) {
		return iterateRepition.get(node);
	}
	
	public int getAndOrBranches(ControlNode node) {
		return andOrBranches.get(node);
	}
	
	private void discoverFunctionalChainElements(FunctionalChain functionalChain, List<String> parentFCs) {
		for (var child : functionalChain.eContents()) {
			if (child instanceof FunctionalChainInvolvementFunction) {
				var function = (FunctionalChainInvolvementFunction) child;
				functions.add(function);
				functionParentFCs.put(function, parentFCs);
				var props = getProperties((CapellaElement) child);
				properties.put(child, props);
				var label = ((FunctionalChainInvolvementFunction) child).getInvolved().getLabel();
				var message = String.format("Function '%s' defines no 'Duration', using default of 1.0", label);
				if (!check(props.containsKey("Duration"), Severity.WARNING, message, child)) {
					props.put("Duration", 1.0f);
				}
			} else if (child instanceof SequenceLink) {
				links.add((SequenceLink) child);
				properties.put(child, getProperties((CapellaElement) child));
			} else if (child instanceof ControlNode) {
				controlNodes.add((ControlNode) child);
				properties.put(child, getProperties((CapellaElement) child));
			} else if (child instanceof FunctionalChainReference) {
				var childFC = (FunctionalChain) ((FunctionalChainReference) child).getInvolved();
				var nextParentFCs = new ArrayList<>(parentFCs);
				nextParentFCs.add(childFC.getName());
				discoverFunctionalChainElements(childFC, nextParentFCs);
			} else if (child instanceof FunctionalChainInvolvementLink) {
				// Ignored on purpose
			} else {
				check(false, Severity.ERROR, "Unsupported type '%s' found in functional chain", child);
			}
		}
	}
	
	public boolean hasError() {
		return this.diagnostics.stream().filter(d -> d.serverity == Severity.ERROR).findAny().isPresent();
	}
	
	private void walk() {
		var stack = new ArrayDeque<StackEntry>();
		var visited = new HashSet<CapellaElement>();
		stack.add(new StackEntry(startFunction));
		branchDepth.put(startFunction, 0);
		while (!stack.isEmpty()) {
			var entry = stack.removeFirst();
			for (var link : getOutgoingLinks(entry.node)) {
				var linkProps = properties.get(link);
				var node = link.getTarget();
				branchDepth.put(link, entry.controlNodes.size());
				branchDepth.put(node, entry.controlNodes.size());
				if (visited.contains(node)) continue;
				visited.add(node);

				ControlNode startNode = null;
				var nextEntry = entry.clone(node);
				if (node instanceof ControlNode) {
					var n = (ControlNode) node;
					var nodeProps = properties.get(n);
					var previous = entry.lastControlNode();
					if (previous != null && previous.getKind() == n.getKind()) {
						if (n.getKind() == ControlNodeKind.ITERATE && linkExists(n, previous)) {
							startNode = previous;
						} else if ((n.getKind() == ControlNodeKind.AND || n.getKind() == ControlNodeKind.OR) && 
								getOutgoingLinks(n).size() == 1) {
							startNode = previous;
						}
					}
					
					if (startNode == null) {
						if (n.getKind() == ControlNodeKind.ITERATE) {
							check(getIncomingLinksOfKind(n, ControlNodeKind.ITERATE).size() >= 1, Severity.ERROR, 
									"ITERATE start without end", n);
							
							var message = "ITERATE start defines no 'Repititions', using default of 2";
							if (!check(nodeProps.containsKey("Repetitions"), Severity.WARNING, message, n)) {
								nodeProps.put("Repetitions", 2);
							}
							iterateRepition.put(n, (Integer) nodeProps.get("Repetitions"));
						} else if (n.getKind() == ControlNodeKind.AND || n.getKind() == ControlNodeKind.OR) {
							var incoming = getIncomingLinks(node);
							var outgoing = getOutgoingLinks(n);
							if (check(incoming.size() == 1, Severity.ERROR, n.getKind().getName() + " start must have exactly one incoming link", n)) {
								check(outgoing.size() >= 2, Severity.ERROR, n.getKind().getName() + " start must have multiple outgoing links", n);
							}
							andOrBranches.put(n, outgoing.size());
							
							if (n.getKind() == ControlNodeKind.OR) {
								for (var l : outgoing) {
									var props = properties.get(l);
									if (!check(props.containsKey("Weight"), Severity.WARNING, "OR link defines no 'Weight', using default of 1", l)) {
										props.put("Weight", 1);
									}
								}
							}
						}
						
						check(!startEndLookup.containsKey(n), Severity.ERROR, "Control node already seen", n);
						check(!startEndLookup.containsValue(n), Severity.ERROR, "Control node start is also an end", n);
						startEndLookup.put(n, null);
						nextEntry = entry.addControlNodeAndClone(n);
					} else {
						check(startEndLookup.containsKey(startNode), Severity.ERROR, "Control node end without start", n);
						check(startEndLookup.get(startNode) == null, Severity.ERROR, "Already found end for control node start", n);
						if (n.getKind() == ControlNodeKind.AND || n.getKind() == ControlNodeKind.OR) {
							var msg = String.format("%s start outgoing links (%d) do not match end incoming links (%d)", 
									n.getKind().getName(), andOrBranches.get(startNode), getIncomingLinks(n).size());
							check(andOrBranches.get(startNode) == getIncomingLinks(n).size(), Severity.ERROR, msg, startNode);
							andOrBranches.put(n, andOrBranches.get(startNode));
						} else if (n.getKind() == ControlNodeKind.ITERATE) {
							iterateRepition.put(n, iterateRepition.get(startNode));
						}
						
						startEndLookup.put(startNode, n);
						nextEntry = entry.removeLastControlNodeAndClone(n);
					}

					check(!nodeProps.containsKey("Repetitions") || (startEndLookup.containsKey(n) && n.getKind() == ControlNodeKind.ITERATE), 
							Severity.WARNING, "Repititions defined on non IRERATE start", n);
				}
				
				check(!linkProps.containsKey("Weight") || (startEndLookup.containsKey(link.getSource()) && ((ControlNode) link.getSource()).getKind() == ControlNodeKind.OR), 
						Severity.WARNING, "Weight defined on non OR start link", link);
				
				if (!hasError()) {
					stack.add(nextEntry);
				}
			}
		}
		
		if (!hasError()) {
			for (var entry : startEndLookup.entrySet()) {
				if (check(entry.getValue() != null, Severity.ERROR, "Control node has no end", entry.getKey())) {
					check(entry.getKey().getKind() == entry.getValue().getKind(), Severity.ERROR, "Control node kinds do not match", entry.getKey());
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getProperties(CapellaElement element) {
		var result = new HashMap<String, Object>();
		
		for (var pvg : element.getAppliedPropertyValueGroups()) {
			for (var pv : pvg.getOwnedPropertyValues()) {
				var name = pv.getName();
				check(!result.containsKey(name), Severity.WARNING, String.format("Found duplicate property with name '%s'", name), element);

				Object value;
				if (pv instanceof IntegerPropertyValue) {
					value = ((IntegerPropertyValue) pv).getValue();
				} else if (pv instanceof FloatPropertyValue) {
					value = ((FloatPropertyValue) pv).getValue();
				} else if (pv instanceof StringPropertyValue) {
					value = ((StringPropertyValue) pv).getValue();
				} else if (pv instanceof EnumerationPropertyValue) {
					value = ((EnumerationPropertyValue) pv).getValue().getName();
				} else {
					throw new RuntimeException(String.format("Property of type '%s' is not supported", pv.getClass().getName()));
				}
				
				if (this.overrideProperties != null && this.overrideProperties.containsKey(pv.getId())) {
					var pvO = this.overrideProperties.get(pv.getId());
					var type = (String) ((Map<String, Object>) pvO.get("type")).get("type");
					var valueO = pvO.get("value");
					if (valueO == null) value = null;
					else if (type.equals("int")) value = (int) Float.parseFloat(valueO.toString());
					else if (type.equals("float")) value = Float.parseFloat(valueO.toString());
					else value = valueO;
				}
				
				result.put(name, value);
			}
		}
		
		return result;
	}
	
	public Map<String, Object> getProperties(Object element) {
		return this.properties.get(element);
	}
	
	private boolean check(boolean condition, Severity severity, String message, EObject element) {
		if (!condition) {
			diagnostics.add(new Diagnostic(severity, message, element));
		}
		return condition;
	}

	private boolean linkExists(CapellaElement source, CapellaElement target) {
		return links.stream().filter(l -> l.getSource() == source && l.getTarget() == target).findAny().isPresent();
	}
	
	private List<SequenceLink> getOutgoingLinks(CapellaElement node) {
		return links.stream().filter(l -> l.getSource() == node).collect(Collectors.toList());
	}
	
	private List<SequenceLink> getIncomingLinks(CapellaElement node) {
		return links.stream().filter(l -> l.getTarget() == node).collect(Collectors.toList());
	}
	
	private List<SequenceLink> getIncomingLinksOfKind(CapellaElement node, ControlNodeKind kind) {
		return getIncomingLinks(node).stream()
				.filter(l -> l.getSource() instanceof ControlNode && (((ControlNode) l.getSource()).getKind() == kind))
				.collect(Collectors.toList());
	}
}
