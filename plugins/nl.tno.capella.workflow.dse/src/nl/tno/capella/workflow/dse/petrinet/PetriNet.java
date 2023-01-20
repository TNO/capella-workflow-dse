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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.core.data.fa.ControlNode;
import org.polarsys.capella.core.data.fa.ControlNodeKind;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementFunction;
import org.polarsys.capella.core.data.fa.SequenceLink;

import nl.tno.capella.workflow.dse.graphviz.Graphviz;
import nl.tno.capella.workflow.dse.petrinet.Place.PlaceType;
import nl.tno.capella.workflow.dse.petrinet.Transition.TransitionType;
import nl.tno.capella.workflow.dse.python.PythonInterpeter;
import nl.tno.capella.workflow.dse.walker.Walker;

public class PetriNet {
	private final Map<String, Transition> transitions = new HashMap<String, Transition>();
	private final Map<String, Place> places = new HashMap<String, Place>();
	private final List<Input> inputs = new ArrayList<Input>();
	private final List<Output> outputs = new ArrayList<Output>();

	private final Walker walker;
	private final AtomicInteger currentId = new AtomicInteger(1);

	public PetriNet(FunctionalChain functionalChain) {
		this(new Walker(functionalChain));
	}
	
	public PetriNet(Walker walker) {
		this.walker = walker;
		
		if (walker.hasError()) {
			throw new RuntimeException("Cannot generate due to errors in model, see 'Problems' view");
		}
		
		walker.functions.stream().forEach(f -> addFunction(f));
		walker.controlNodes.stream().forEach(c -> addControlNode(c));
		walker.links.stream().forEach(l -> addLink(l));
		
		var startTransition = transitions.get(walker.startFunction.getId());
		var endTransition = transitions.get(walker.endFunction.getId());
		var startPlace = addPlace("start", new Place(currentId.getAndIncrement(), PlaceType.START, 1));
		var endPlace = addPlace("end", new Place(currentId.getAndIncrement(), PlaceType.END));
		inputs.add(new Input(startPlace, startTransition));
		outputs.add(new Output(endPlace, endTransition));
	}
	
	private void addControlNode(ControlNode node) {
		var props = walker.getProperties(node);
		var isStart = walker.isStart(node);
		var branchDepth = walker.getBranchDepth(node);
		if (node.getKind() == ControlNodeKind.ITERATE) {
			var type = isStart ? PlaceType.ITS : PlaceType.ITE;
			var beforeOrIntermediatePlace = addPlace(isStart ? node.getId()+"I" : getPlaceTargetId(node), new Place(currentId.getAndIncrement(), type, props));
			var afterPlace = addPlace(getPlaceSourceId(node), new Place(currentId.getAndIncrement(), type, props));
			var transition = addTransition(node.getId(), new Transition(currentId.getAndIncrement(), isStart ? TransitionType.ITS : TransitionType.ITE, branchDepth));
			inputs.add(new Input(beforeOrIntermediatePlace, transition));
			outputs.add(new Output(afterPlace, transition));
			// Create generator for iteration start
			if (isStart) {
				var generatorTransition = addTransition(node.getId()+"G", new Transition(currentId.getAndIncrement(), TransitionType.ITG, branchDepth));
				var beforePlace = addPlace(getPlaceTargetId(node), new Place(currentId.getAndIncrement(), PlaceType.ITG, props)); 
				inputs.add(new Input(beforePlace, generatorTransition));
				outputs.add(new Output(beforeOrIntermediatePlace, generatorTransition, walker.getIterateRepitition(node)));
			}
		} else if (node.getKind() == ControlNodeKind.AND) {
			var place = addPlace(node.getId(), new Place(currentId.getAndIncrement(), isStart ? PlaceType.ANDS : PlaceType.ANDE, props));
			var transition = addTransition(node.getId(), new Transition(currentId.getAndIncrement(), TransitionType.AND, branchDepth));
			inputs.add(new Input(place, transition, isStart ? 1 : walker.getAndOrBranches(node)));
		} else if (node.getKind() == ControlNodeKind.OR) {
			addPlace(node.getId(), new Place(currentId.getAndIncrement(), isStart ? PlaceType.ORS : PlaceType.ORE, props));
		} else {
			throw new RuntimeException(String.format("Operator '%s' is not supported", node.getKind().getName()));
		}
	}
	
	private void addFunction(FunctionalChainInvolvementFunction function) {
		var props = walker.getProperties(function);
		var branchDepth = walker.getBranchDepth(function);
		
		Place resource = null;
		if (getResourceID(props) != null) {
			var key = "resource_" + getResourceID(props);
			if (!places.containsKey(key)) addPlace(key, new Place(currentId.getAndIncrement(), PlaceType.RES, 1, props.get("ResourceID").toString()));
			resource = places.get(key);
		}
		
		var levels = walker.getParentFunctionalChains(function);
 		var transition = addTransition(function.getId(), new Transition(currentId.getAndIncrement(), TransitionType.FUNC, 
 					branchDepth, function.getInvolved().getLabel(), props, levels));
		if (resource != null) {
			inputs.add(new Input(resource, transition));
			outputs.add(new Output(resource, transition));
		}
	}
	
	private void addLink(SequenceLink link) {
		var source = link.getSource();
		var target = link.getTarget();
		var props = walker.getProperties(link);
		var branchDepth = walker.getBranchDepth(link);
		if (source instanceof FunctionalChainInvolvementFunction && target instanceof FunctionalChainInvolvementFunction) {
			addPlaceWithInputOutput(link.getId(), new Place(currentId.getAndIncrement(), PlaceType.N), source, target, props);
		} else if (source instanceof FunctionalChainInvolvementFunction && target instanceof ControlNode) {
			outputs.add(new Output(places.get(getPlaceTargetId(link.getTarget())), transitions.get(link.getSource().getId()), props));
		} else if (source instanceof ControlNode && target instanceof FunctionalChainInvolvementFunction) {
			var node = (ControlNode) source;
			if (node.getKind() == ControlNodeKind.AND) {
				var type = places.get(node.getId()).type;
				addPlaceWithInputOutput(link.getId(), new Place(currentId.getAndIncrement(), type), link.getSource(), link.getTarget(), props);
			} else if (node.getKind() == ControlNodeKind.OR) {
				inputs.add(new Input(places.get(link.getSource().getId()), transitions.get(link.getTarget().getId()), props));
			} else if (node.getKind() == ControlNodeKind.ITERATE) {
				var place = places.get(getPlaceSourceId(link.getSource()));
				inputs.add(new Input(place, transitions.get(link.getTarget().getId()), getInputTokens(link), props));
			} else {
				throw new RuntimeException(String.format("Operator '%s' is not supported", node.getKind().getName()));
			}
		} else if (source instanceof ControlNode && target instanceof ControlNode) {
			var sourceN = (ControlNode) source;
			var targetN = (ControlNode) target;
			if (sourceN.getKind() == ControlNodeKind.ITERATE && targetN.getKind() == ControlNodeKind.ITERATE) {
				if (walker.isStart(sourceN) == walker.isStart(targetN)) {
					addTransitionWithInputOutput(link.getId(), new Transition(currentId.getAndIncrement(), TransitionType.N, branchDepth), 
							link.getSource(), link.getTarget(), getInputTokens(link), props);
				} else if (walker.isStartEndPair(targetN, sourceN)) { // Iterate end -> start link
					addPlaceWithInputOutput(link.getId(), new Place(currentId.getAndIncrement(), PlaceType.ITL, 1), link.getSource(), link.getTarget(), props);
				} else {
					throw new RuntimeException("Unexpected iteration link");
				}
			} else if (sourceN.getKind() == ControlNodeKind.AND) {
				var output = new Output(places.get(getPlaceTargetId(link.getTarget())), transitions.get(source.getId()), props);
				var existingOutput = outputs.stream().filter(o -> o.transition == output.transition && o.place == output.place).findFirst();
				if (existingOutput.isPresent()) {
					existingOutput.get().producedTokens += 1;
				} else {
					outputs.add(output);
				}
			} else if (sourceN.getKind() == ControlNodeKind.ITERATE) {
				addTransitionWithInputOutput(link.getId(), new Transition(currentId.getAndIncrement(), TransitionType.N, branchDepth), 
						link.getSource(), link.getTarget(), getInputTokens(link), props);
			} else if (sourceN.getKind() == ControlNodeKind.OR) {
				var type = walker.isStartEndPair(sourceN, targetN) ? TransitionType.OR : TransitionType.N;
				addTransitionWithInputOutput(link.getId(), new Transition(currentId.getAndIncrement(), type, branchDepth), 
						link.getSource(), link.getTarget(), 1, props);
			} else {
				throw new RuntimeException(String.format("Link between '%s' and '%s' is not supported", 
						sourceN.getKind().getName(), targetN.getKind().getName()));
			}
		} else {
			throw new RuntimeException(
					String.format("Unsupported link from '%s' to '%s'", source.getClass().getName(), target.getClass().getName()));
		}
	}
	
	private String getResourceID(Map<String, Object> props) {
		return props != null && props.containsKey("ResourceID") && props.get("ResourceID") != null && !props.get("ResourceID").toString().strip().equals("") ? 
				props.get("ResourceID").toString() : null;
	}
	
	private String getPlaceSourceId(CapellaElement element) {
		if (element instanceof ControlNode && ((ControlNode) element).getKind() == ControlNodeKind.ITERATE) {
			return element.getId() + "A";
		}
		return element.getId();
	}
	
	private String getPlaceTargetId(CapellaElement element) {
		if (element instanceof ControlNode && ((ControlNode) element).getKind() == ControlNodeKind.ITERATE) {
			return element.getId() + "B";
		}
		return element.getId();
	}
	
	private void addTransitionWithInputOutput(String id, Transition transition, CapellaElement source, CapellaElement target, int inputTokens, Map<String, Object> properties) {
		addTransition(id, transition);
		inputs.add(new Input(places.get(getPlaceSourceId(source)), transition, inputTokens, properties));
		outputs.add(new Output(places.get(getPlaceTargetId(target)), transition, properties));
	}
	
	private void addPlaceWithInputOutput(String id, Place place, CapellaElement source, CapellaElement target, Map<String, Object> properties) {
		addPlace(id, place);
		outputs.add(new Output(place, transitions.get(source.getId()), properties));
		inputs.add(new Input(place, transitions.get(target.getId()), properties));
	}
	
	private int getInputTokens(SequenceLink link) {
		if (link.getSource() instanceof ControlNode) {
			var node = (ControlNode) link.getSource();
			if (node.getKind() == ControlNodeKind.ITERATE && walker.isEnd(node)) {
				return walker.getIterateRepitition(node);
			}
		}
		
		return 1;
	}

	private Transition addTransition(String key, Transition transition) {
		if (transitions.containsKey(key)) throw new RuntimeException(String.format("Duplicate transition key: '%s'", key));
		transitions.put(key, transition);
		return transition;
	}

	private Place addPlace(String key, Place place) {
		if (places.containsKey(key)) throw new RuntimeException(String.format("Duplicate place key: '%s'", key));
		places.put(key, place);
		return place;
	}
	
	public void draw(File file) {
		var script = toSnakes() + String.format("n.draw('%s')", file.toString().replace('\\', '/'));
		var env = new HashMap<String, String>();
		env.put("PATH", Graphviz.getBinPath());
		PythonInterpeter.execute(script, env);
	}

	public String toSnakes() {
		var builder = new StringBuilder();
		
		var currentId = new AtomicInteger(this.currentId.get());
		var places = new ArrayList<>(this.places.values());
		var transitions = new  ArrayList<>(this.transitions.values());
		List<Input> inputs = new ArrayList<>(this.inputs);
		List<Output> outputs = new ArrayList<>(this.outputs);
		
		// Add function start/end
		for (int idx = 0; idx < transitions.size(); idx++) {
			var t = transitions.get(idx);
			if (t.type == TransitionType.FUNC) {
				var start = new Transition(t.id, TransitionType.FUNCS, t.branchDepth, t.name, t.properties, t.levels, t.guard);
				var end = new Transition(t.id, TransitionType.FUNCE, t.branchDepth, t.name, t.properties, t.levels, t.guard);
				var place = new Place(currentId.getAndIncrement(), PlaceType.N);
				places.add(place);
				outputs.add(new Output(place, start));
				inputs.add(new Input(place, end));
				transitions.set(idx, start);
				transitions.add(end);
				inputs = inputs.stream().map(i -> i.transition == t ? new Input(i.place, start) : i)
						.collect(Collectors.toList());
				outputs = outputs.stream().map(o -> o.transition == t ? new Output(o.place, end) : o)
						.collect(Collectors.toList());
			}
		}
		
		builder.append("import snakes.plugins\n" + 
				"snakes.plugins.load('gv', 'snakes.nets', 'nets')\n" + 
				"from nets import *\n" + 
				"\n" + 
				"n = PetriNet('N')\n");
		
		builder.append("\n# Places\n");
		places.stream().sorted((a, b) -> a.id - b.id).forEach(p -> builder.append(p.toSnakes() + "\n"));
		
		builder.append("\n# Transitions\n");
		transitions.stream().sorted((a, b) -> a.id - b.id).forEach(p -> builder.append(p.toSnakes() + "\n"));
		builder.append("for t in n.transition():\n");
		builder.append("    t.input_props = []\n");
		builder.append("    t.output_props = []\n");

		builder.append("\n# Inputs\n");
		inputs.forEach(p -> builder.append(p.toSnakes() + "\n"));
		
		builder.append("\n# Outputs\n");
		outputs.forEach(p -> builder.append(p.toSnakes() + "\n"));
		
		return builder.toString();
	}
	
	public String toPOOSL() {
		var result = "";

		result += "import \"lib/Places.poosl\"\n" + 
				"import \"lib/Transition.poosl\"\n" + 
				"import \"lib/ChoiceStartTransition.poosl\"\n" + 
				"import \"lib/datatypes/PlaceChange.poosl\"\n" + 
				"import \"lib/TRACE/Logger.poosl\"\n" +
				"system\n" + 
				"ports\n\n" +
				"instances\n";
		
		var currentId = new AtomicInteger(this.currentId.get());
		var places = new ArrayList<>(this.places.values());
		var transitions = new  ArrayList<>(this.transitions.values());
		var inputs = new ArrayList<>(this.inputs);
		var outputs = new ArrayList<>(this.outputs);
		var maxLevel = this.transitions.values().stream().map(t -> t.levels != null ? t.levels.size() : 0).max(Integer::compare).get() + 1;
		
		// POOSL requires a transition for OR (ChoiceStartTransition) while in the net it is a place.
		// This code inserts the additional transition
		for (var i = 0; i < places.size(); i++) {
			var place = places.get(i);
			if (place.type == PlaceType.ORS) {
				var transition = new Transition(currentId.getAndIncrement(), TransitionType.ORC, -1);
				var placeInputs = inputs.stream().filter(e -> e.place == place).collect(Collectors.toList());
				for (var input : placeInputs) {
					var p = new Place(place.id, PlaceType.ORST);
					inputs.set(inputs.indexOf(input), new Input(p, input.transition, input.consumedTokens, input.properties));
					outputs.add(new Output(p, transition, input.properties));
					places.add(p);
				}
				transitions.add(transition);
				inputs.add(new Input(place, transition));
			}
		}
		
		var tokensPlacement = places.stream().filter(p -> p.tokens > 0)
				.map(p -> String.format("putAt(%d,%d)", places.indexOf(p)+1, p.tokens)).collect(Collectors.joining(" "));
		var resourcePlaces = this.places.values().stream().filter(p -> p.type == PlaceType.RES).collect(Collectors.toList());
		transitions.stream().filter(t -> t.type == TransitionType.FUNC && getResourceID(t.properties) == null).forEach(t -> {
			resourcePlaces.add(new Place(currentId.getAndIncrement(), PlaceType.RES, 1, String.format("t%dUnknown", t.id)));
		});
		var resources = resourcePlaces.stream().map(p -> String.format("putAt(%d,\"%s\")", resourcePlaces.indexOf(p) + 1, p.name))
				.collect(Collectors.joining(" "));
		result += String.format("places : Places(numberOfTransitions := %d,\n", transitions.size());
		result += String.format("                numberOfPlaces := %d,\n", places.size());
		result += String.format("                initialMarking := new(Array) resize(%d) putAll(0) \n", places.size());
		result += String.format("                                  %s)\n\n", tokensPlacement);
		
		result += "// Places:\n" + places.stream()
			.map(p -> String.format("// %d: %s", places.indexOf(p)+1, p.snakesName())).collect(Collectors.joining("\n")) + "\n\n";
		

		for (var transition : transitions) {
			if (transition.type == TransitionType.FUNCS || transition.type == TransitionType.FUNCE) {
				throw new RuntimeException("FUNC start/end not supported for POOSL export");
			}
			
			var type = transition.type == TransitionType.ORC ? "ChoiceStartTransition" : "Transition";
			var prePlaceChangesIndex = new AtomicInteger(1);
			var prePlaceChanges = inputs.stream().filter(i -> i.transition == transition)
					.map(i -> String.format("%sputAt(%d, new(PlaceChange) setPlace(%d) setChange(%d))", 
							" ".repeat(21), prePlaceChangesIndex.getAndIncrement(), places.indexOf(i.place)+1, i.consumedTokens))
					.collect(Collectors.toList());
			var postPlaceChangesIndex = new AtomicInteger(1);
			var transitionOutputs = outputs.stream().filter(i -> i.transition == transition).collect(Collectors.toList());
			var postPlaceChanges = transitionOutputs.stream().map(i -> String.format("%sputAt(%d, new(PlaceChange) setPlace(%d) setChange(%d))", 
							" ".repeat(21), postPlaceChangesIndex.getAndIncrement(), places.indexOf(i.place)+1, i.producedTokens))
					.collect(Collectors.toList());
			var duration = transition.type == TransitionType.FUNC ? (float) transition.properties.get("Duration") : 0.0f;
			var logging = transition.type == TransitionType.FUNC ? "true" : "false";
			var resourceNames = String.format("new(Array) resize(1) putAt(1, \"%s\")", String.format("t%dUnknown", transition.id));
			var resourceID = getResourceID(transition.properties);
			if (resourceID != null) {
				resourceNames = String.format("new(Array) resize(1) putAt(1, \"%s\")", resourceID);
			}

			result += String.format("t%d : %s(number := %d,\n", transition.id, type, transitions.indexOf(transition)+1);
			result += String.format("                 prePlaceChanges := new(Array) resize(%d)\n%s,\n", 
					prePlaceChanges.size(), String.join("\n", prePlaceChanges));
			if (transition.type != TransitionType.ORC) {
				result += String.format(Locale.ROOT, "                 duration := %f,\n", duration);
			}
			result += String.format("                 postPlaceChanges := new(Array) resize(%d)\n%s,\n", 
					postPlaceChanges.size(), String.join("\n", postPlaceChanges));
			if (transition.type == TransitionType.ORC) {
				var weights = transitionOutputs.stream().map(o -> {
					var value = (o.properties != null ? o.properties.getOrDefault("Weight", 1) : 1);
					if (value instanceof Float) value = Math.round((float) value);
					return String.format("putAt(%d, %d)", transitionOutputs.indexOf(o) + 1, (int) value);					
				}).collect(Collectors.joining(" "));
				result += String.format("                 weights := new(Array) resize(%d) %s)\n\n", transitionOutputs.size(), weights);
			} else {
				var level = transition.levels != null ? transition.levels.size() + 1 : 1;
				var levelNames = IntStream.range(0, maxLevel).mapToObj(index -> {
					var levelName = "x";
					if (transition.levels != null) {
						if (transition.levels.size() > index) levelName = transition.levels.get(index);
						else if (transition.levels.size() == index) levelName = transition.name;
						else levelName = String.format("level%d", level);
					}
					return String.format("putAt(%d,\"%s\")", index + 1, levelName);
				}).collect(Collectors.joining(" "));
				
				result += String.format("                 resourcenames := %s,\n", resourceNames);
				result += String.format("                 name := \"%s\",\n", transition.pooslName());
				result += String.format("                 level := %d,\n", level);
				result += String.format("                 levelnames := new(Array) resize(%d) %s,\n", maxLevel, levelNames);
				result += String.format("                 logging := %s)\n\n", logging);
			}
		}
		
		result += "logger : Logger(resourcenames := new(Array)\n";
		result += String.format("                resize(%d) %s)\n\n", resourcePlaces.size(), resources);
		result += "channels\n";
	
		result += String.format("	{ places.placesIO, %s }\n", transitions.stream()
				.map(t -> String.format("t%d.placesIO", t.id)).collect(Collectors.joining(",")));
		result += String.format("	{ logger.log, %s }\n", transitions.stream()
				.filter(t -> t.type != TransitionType.ORC)
				.map(t -> String.format("t%d.log", t.id)).collect(Collectors.joining(",")));		
		return result;
	}
}
