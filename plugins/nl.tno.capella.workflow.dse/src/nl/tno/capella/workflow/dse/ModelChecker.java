/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.transaction.ResourceSetChangeEvent;
import org.eclipse.emf.transaction.ResourceSetListenerImpl;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.sirius.business.api.query.EObjectQuery;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.diagram.business.internal.metamodel.spec.DEdgeSpec;
import org.eclipse.sirius.diagram.business.internal.metamodel.spec.DNodeSpec;
import org.eclipse.sirius.diagram.business.internal.metamodel.spec.DSemanticDiagramSpec;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.sirius.viewpoint.ViewpointPackage;
import org.polarsys.capella.common.ef.domain.IEditingDomainListener;
import org.polarsys.capella.common.platform.sirius.ted.SemanticEditingDomainFactory.SemanticEditingDomain;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.core.data.fa.FunctionalChainInvolvementFunction;
import org.polarsys.capella.core.data.fa.FunctionalChainReference;

import nl.tno.capella.workflow.dse.walker.Diagnostic;
import nl.tno.capella.workflow.dse.walker.Severity;
import nl.tno.capella.workflow.dse.walker.Walker;

@SuppressWarnings("restriction")
public class ModelChecker extends ResourceSetListenerImpl implements IEditingDomainListener {
	
	final static String MARKER_TYPE = "org.eclipse.sirius.diagram.ui.diagnostic";
	
	@Override
	public void resourceSetChanged(ResourceSetChangeEvent event) {
		var chains = new HashSet<FunctionalChain>();
		IResource resource = null;
		for (var notification : event.getNotifications()) {
		 	if (notification.getNotifier() instanceof EObject) {
		 		if (resource == null) {
		 			var session = SessionManager.INSTANCE.getSession((EObject)notification.getNotifier());
		 			if (session != null) {
		 				resource = WorkspaceSynchronizer.getFile(session.getSessionResource());
		 			}
		 		}

		 		discoverFunctionalChains((EObject) notification.getNotifier(), chains);
		 	}
		}
		
		if (!chains.isEmpty() && resource == null) {
			Util.showPopupError("Model checker error", "no resource");
		}

		for (var chain : chains) {
			try {
	    		var descriptor = new EObjectQuery(chain)
	    				.getInverseReferences(ViewpointPackage.Literals.DREPRESENTATION_DESCRIPTOR__TARGET).stream().findAny();
	    		if (descriptor.isPresent()) {
					checkFunctionalChain(chain, (DRepresentationDescriptor) descriptor.get(), resource);
	    		} else {
	    			// Deleted, clear notifications
	    			clearMarkers(chain, resource);
	    			
	    		}
			} catch (Exception e) {
				Util.showPopupError("Model checker error", e);
				e.printStackTrace();
			}
		}
	}
	
	private void discoverFunctionalChains(EObject obj, Set<FunctionalChain> chains) {
		var refs = obj.eCrossReferences().stream().filter(r -> r instanceof FunctionalChain).map(r -> (FunctionalChain) r).collect(Collectors.toList());
		if (!refs.isEmpty()) {
			for (var ref : refs) {
				chains.add(ref);
				discoverFunctionalChains(ref, chains);
			}
		} else if (obj instanceof FunctionalChain) {
			var chain = (FunctionalChain) obj;
			chains.add(chain);
			chain.eCrossReferences().stream().filter(r -> r instanceof FunctionalChainReference).forEach(r -> {
				chains.add((FunctionalChain) ((FunctionalChainReference) r).eContainer());
			});
		} else if (obj.eContainer() != null) {
			discoverFunctionalChains(obj.eContainer(), chains);
		}
	}
	
	private void checkFunctionalChain(FunctionalChain fc, DRepresentationDescriptor descriptor, IResource resource) throws CoreException {
		var walker = new Walker(fc);
	    var location = fc.getName();		
		var resourceUri = String.format("%s:%s#%s", descriptor.getRepPath().getResourceURI().scheme(), 
				descriptor.getRepPath().getResourceURI().path(), descriptor.getUid());
		var diagramSpec = descriptor.eCrossReferences().stream().filter(r -> r instanceof DSemanticDiagramSpec).findFirst().get();
	    
	    clearMarkers(fc, resource);
	    
	    var diagnostics = new ArrayList<Diagnostic>(walker.diagnostics);
	    
	    // Show information messages for hidden function and control nodes
	    for (var function : walker.functions) {
	    	if (!isVisible(getDiagramElement(diagramSpec, function))) {
	    		var message = String.format("Function '%s' is not visible in diagram, this may produce unexpected behavior", ((FunctionalChainInvolvementFunction) function).getInvolved().getLabel());
	    		diagnostics.add(new Diagnostic(Severity.INFO, message, function));
	    	}
	    }
	    
	    for (var controlNode : walker.controlNodes) {
	    	if (!isVisible(getDiagramElement(diagramSpec, controlNode))) {
	    		var message = String.format("Control Node '%s' is not visible in diagram, this may produce unexpected behavior", controlNode.getId());
	    		diagnostics.add(new Diagnostic(Severity.INFO, message, controlNode));
	    	}
	    }
	    
	    // Show diagnostics
	    @SuppressWarnings("serial")
		var severityLookup  = new HashMap<Severity, Integer>() {{
	        put(Severity.ERROR, IMarker.SEVERITY_ERROR);
	        put(Severity.WARNING, IMarker.SEVERITY_WARNING);
	        put(Severity.INFO, IMarker.SEVERITY_INFO);
	    }};
	    for (var diag : diagnostics) {
	    	var marker = resource.createMarker(MARKER_TYPE);
	    	marker.setAttribute(IMarker.MESSAGE, diag.message);
	    	marker.setAttribute("org.eclipse.ui.editorID", "org.eclipse.sirius.diagram.ui.part.SiriusDiagramEditorID");
	    	marker.setAttribute(IMarker.SEVERITY, severityLookup.get(diag.serverity));
    		marker.setAttribute("DIAGRAM_DESCRIPTOR_URI", resourceUri);
    		marker.setAttribute("location", location);
    		
    		var diagramElement = getDiagramElement(diagramSpec, diag.element);
    		if (diagramElement.isEmpty()) continue;
    		var viewElementRef = new EObjectQuery(diagramElement.get()).getInverseReferences(NotationPackage.Literals.VIEW__ELEMENT).stream().findFirst();
    		if (viewElementRef.isEmpty()) continue;
    		var uriFragment = ((EObject) viewElementRef.get()).eResource().getURIFragment((EObject) viewElementRef.get());
    		marker.setAttribute("elementId", uriFragment);
	    }
	}
	
	private boolean isVisible(Optional<EObject> element) {
		if (element.isEmpty()) return false;
		else if (element.get() instanceof DNodeSpec) return ((DNodeSpec) element.get()).isVisible();
		else if (element.get() instanceof DEdgeSpec) return ((DEdgeSpec) element.get()).isVisible();
		return false;
	}
	
	private Optional<EObject> getDiagramElement(EObject diagramSpec, EObject element) {
		Optional<EObject> diagramElement = null;
		if (element instanceof FunctionalChain) {
			diagramElement = Optional.ofNullable(diagramSpec);
		} else {
			diagramElement = diagramSpec.eCrossReferences().stream().filter(d -> {
    			return (d instanceof DNodeSpec && ((DNodeSpec) d).getTarget() == element) ||
    				(d instanceof DEdgeSpec && ((DEdgeSpec) d).getTarget() == element);
    		}).findFirst();
		}
		return diagramElement;
	}
	
	private void clearMarkers(FunctionalChain fc, IResource resource) throws CoreException {
		for (var marker : resource.findMarkers(MARKER_TYPE, true, 1)) {
	    	if (marker.getAttribute("location", "").equals(fc.getName())) {
	    		marker.delete();
	    	}
	    }
	}
	
	@Override
	public void createdEditingDomain(EditingDomain editingDomain) {
		((SemanticEditingDomain) editingDomain).addResourceSetListener(this);
	}

	@Override
	public void disposedEditingDomain(EditingDomain editingDomain) {
		((SemanticEditingDomain) editingDomain).removeResourceSetListener(this);
	}
}
