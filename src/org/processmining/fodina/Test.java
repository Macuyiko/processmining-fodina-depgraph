package org.processmining.fodina;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.bpmnminer.dependencygraph.DependencyNet;
import org.processmining.plugins.bpmnminer.types.MinerSettings;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;

public class Test {
	public static void main(String... args) {
		Map<String, Integer> traces = new HashMap<String, Integer>();
		
		traces.put("0:1:2:5:-1", 10);
		traces.put("0:1:3:5:-1", 5);
		traces.put("0:1:3:4:5:-1", 5);
		
		int logSize = traces.values().stream().reduce(0, Integer::sum);
		
		MinerSettings settings = new MinerSettings(logSize);
		
		DependencyNet dependencyGraph = Fodina.getDependencyGraph(traces, settings);
		System.out.println(dependencyGraph);
		
		BitSet bitsetGraph = Fodina.dependencyNetToBitSet(dependencyGraph);
		System.out.println(bitsetGraph);
		
		DependencyNet newDependencyGraph = Fodina.bitSetToDependencyNet(bitsetGraph, dependencyGraph.getTasks().size());
		System.out.println(newDependencyGraph);
		
		Object[] petrinetAndMarking = Fodina.getPetriNet(traces, newDependencyGraph, settings);
		
		PnmlExportNetToPNML exporter = new PnmlExportNetToPNML();
		try {
			exporter.exportPetriNetToPNMLFile(new FakePluginContext(), (Petrinet) petrinetAndMarking[0], new File("test.pnml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
