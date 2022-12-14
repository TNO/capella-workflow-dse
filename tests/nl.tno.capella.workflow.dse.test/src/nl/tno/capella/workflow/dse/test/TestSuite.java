/*
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package nl.tno.capella.workflow.dse.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.business.api.dialect.DialectManager;
import org.polarsys.capella.core.data.fa.FunctionalChain;
import org.polarsys.capella.test.framework.api.BasicTestArtefact;
import org.polarsys.capella.test.framework.api.BasicTestCase;
import org.polarsys.capella.test.framework.api.BasicTestSuite;
import org.polarsys.capella.test.framework.api.ModelProviderHelper;

import nl.tno.capella.workflow.dse.petrinet.PetriNet;
import nl.tno.capella.workflow.dse.pvmt.Exporter;
import nl.tno.capella.workflow.dse.walker.Walker;
import junit.framework.Test;

public class TestSuite extends BasicTestSuite {
	
	final String MODEL = "3D Reconstruction";
	
	enum TestType { POOSL, PY, DIAGNOSTICS }
	
	// Required to launch tests via Maven
	public static Test suite() {
		return new TestSuite();
	}
	
	public class FCTest extends BasicTestCase {
		final FunctionalChain fc;
		final TestType type;
		
		@Override
		public String getName() {
			return type.name() + ": " + fc.getName();
		}
		
		public FCTest(FunctionalChain fc, TestType type) {
			this.fc = fc;
			this.type = type;
		}

		@Override
		public void test() throws Exception {
    		var expected = String.join("\n", Files.readAllLines(Paths.get("expected", fc.getName() + "." + this.type.toString().toLowerCase()))).trim();
			if (this.type == TestType.DIAGNOSTICS) {
				var walker = new Walker(fc);
	    		var actual = walker.diagnostics.stream().map(d -> String.format("%s - %s", d.serverity.toString(), d.message)).collect(Collectors.joining("\n"));
	            assertEquals(expected, actual);
			} else {
	    		var net = new PetriNet(fc);
	    		var actual = (type == TestType.PY ? net.toSnakes() : net.toPOOSL()).strip();
	            assertEquals(expected, actual);
			}
		}
	}
	
	public class ExportTest extends BasicTestCase {
		private Resource resource;
		
		public ExportTest(Resource resource) {
			this.resource = resource;
		}
		
		@Override
		public void test() throws Exception {
    		var expected = String.join("\n", Files.readAllLines(Paths.get("expected", "export.json"))).trim();
    		var actual = Exporter.exportResourceToString(resource);
            assertEquals(expected, actual);
		}
	}

	public void test() {}

	@Override
	protected List<BasicTestArtefact> getTests() {
		try {
			ModelProviderHelper.getInstance().getModelProvider().requireTestModel(Arrays.asList(MODEL), this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		var session = ModelProviderHelper.getInstance().getModelProvider().getSessionForTestModel(MODEL, this);
		var resource = (Resource) session.getAllSessionResources().toArray()[0];
		
		var result = new ArrayList<BasicTestArtefact>();
		result.add(new ExportTest(resource));

	    for (var repr : DialectManager.INSTANCE.getAllRepresentationDescriptors(session)) {
	    	if (repr.getTarget() instanceof FunctionalChain) {
	    		var fc = (FunctionalChain) repr.getTarget();
	    		result.add(new FCTest(fc, TestType.PY));
	    		result.add(new FCTest(fc, TestType.POOSL));
	    		result.add(new FCTest(fc, TestType.DIAGNOSTICS));
	    	}
	    }
	    return result;
	}
}
