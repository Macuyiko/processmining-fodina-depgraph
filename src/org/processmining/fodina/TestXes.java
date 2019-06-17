package org.processmining.fodina;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.bpmnminer.dependencygraph.DependencyNet;
import org.processmining.plugins.bpmnminer.types.MinerSettings;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.kutoolbox.utils.ImportUtils;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;

public class TestXes {
	public static void main(String... args) {
		XLog log = ImportUtils.openLog(new File("c:/users/seppe/Desktop/BPI_Challenge_2012.xes")); 
		
		Map<String, Integer> traces = Fodina.xLogToSimpleLog(log, XLogInfoImpl.STANDARD_CLASSIFIER);
		
		MinerSettings settings = new MinerSettings(log.size());
		
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
