package org.processmining.fodina;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.bpmnminer.converter.CausalNetToPetrinet;
import org.processmining.plugins.bpmnminer.dependencygraph.DependencyNet;
import org.processmining.plugins.bpmnminer.miner.FodinaMiner;
import org.processmining.plugins.bpmnminer.types.EventLogTaskMapper;
import org.processmining.plugins.bpmnminer.types.IntegerEventLog;
import org.processmining.plugins.bpmnminer.types.MinerSettings;

public class Fodina {

	public static Map<String, Integer> xLogToSimpleLog(XLog log, XEventClassifier xEventClassifier) {
		Map<String, Integer> simpleLog = new HashMap<String, Integer>();
		Map<String, Integer> activitiesToInteger = new HashMap<String, Integer>();
		for (XTrace trace : log) {
			List<String> traceElements = new ArrayList<String>();
			for (XEvent event : trace) {
				String eventName = xEventClassifier.getClassIdentity(event);
				if (!activitiesToInteger.containsKey(eventName)) {
					activitiesToInteger.put(eventName, activitiesToInteger.size() + 1);
				}
				traceElements.add("" + activitiesToInteger.get(eventName));
			}
			traceElements.add(0, "0");
			traceElements.add("-1");
			String finalTrace = String.join(":", traceElements);
			if (!simpleLog.containsKey(finalTrace))
				simpleLog.put(finalTrace, 0);
			simpleLog.put(finalTrace, simpleLog.get(finalTrace) + 1);
		}
		return simpleLog;
	}

	public static IntegerEventLog simpleLogToIntegerEventLog(Map<String, Integer> simpleTraces) {
		IntegerEventLog ieLog = new IntegerEventLog();
		for (Entry<String, Integer> entry : simpleTraces.entrySet()) {
			String[] split = entry.getKey().split(":");
			int[] spliti = new int[split.length];
			for (int i = 0; i < split.length; i++) {
				spliti[i] = Integer.parseInt(split[i]);
				ieLog.setLabel(spliti[i], spliti[i] + "");
			}
			ieLog.addRow(spliti);
			ieLog.setRowCount(spliti, ieLog.getRowCount(spliti) + entry.getValue() - 1);
		}
		return ieLog;
	}

	public static BitSet dependencyNetToBitSet(DependencyNet depnet) {
		int size = depnet.getTasks().size();
		BitSet dfg = new BitSet(size * size);
		for (int a : depnet.getTasks()) {
			for (int b : depnet.getTasks()) {
				if (!depnet.isArc(a, b))
					continue;
				int src = (a == -1) ? size - 1 : a;
				int tgt = (b == -1) ? size - 1 : b;
				dfg.set(src * size + tgt);
			}
		}
		return dfg;
	}

	public static DependencyNet bitSetToDependencyNet(BitSet dfg, int nrtasks) {
		DependencyNet net = new DependencyNet();
		for (int a = 0; a < nrtasks; a++) {
			for (int b = 0; b < nrtasks; b++) {
				if (!dfg.get(a * nrtasks + b))
					continue;
				int src = (a == nrtasks - 1) ? -1 : a;
				int tgt = (b == nrtasks - 1) ? -1 : b;
				net.addTask(src);
				net.addTask(tgt);
				net.setArc(src, tgt, true);
			}
		}
		net.setStartTask(0);
		net.setEndTask(-1);
		return net;
	}

	public static DependencyNet getDependencyGraph(Map<String, Integer> simpleTraces, MinerSettings settings) {
		IntegerEventLog ieLog = simpleLogToIntegerEventLog(simpleTraces);
		return getDependencyGraph(ieLog, settings);
	}

	public static DependencyNet getDependencyGraph(XLog log, MinerSettings settings) {
		EventLogTaskMapper mapper = new EventLogTaskMapper(log, settings.classifier);
		mapper.setup(settings.backwardContextSize, settings.forwardContextSize, settings.useUniqueStartEndTasks,
				settings.collapseL1l, settings.duplicateThreshold);
		IntegerEventLog ieLog = mapper.getIntegerLog();
		return getDependencyGraph(ieLog, settings);
	}

	public static DependencyNet getDependencyGraph(IntegerEventLog ieLog, MinerSettings settings) {
		FodinaMiner miner = new FodinaMiner(ieLog, settings);
		miner.mineDependencyNet();
		DependencyNet depnet = miner.getDependencyNet();
		return depnet;
	}

	public static Object[] getPetriNet(Map<String, Integer> simpleTraces, DependencyNet depnet,
			MinerSettings settings) {
		IntegerEventLog ieLog = simpleLogToIntegerEventLog(simpleTraces);
		return getPetriNet(ieLog, depnet, settings);
	}

	public static Object[] getPetriNet(XLog log, DependencyNet depnet, MinerSettings settings) {
		EventLogTaskMapper mapper = new EventLogTaskMapper(log, settings.classifier);
		mapper.setup(settings.backwardContextSize, settings.forwardContextSize, settings.useUniqueStartEndTasks,
				settings.collapseL1l, settings.duplicateThreshold);
		IntegerEventLog ieLog = mapper.getIntegerLog();
		return getPetriNet(ieLog, depnet, settings);
	}

	public static Object[] getPetriNet(IntegerEventLog ieLog, DependencyNet depnet, MinerSettings settings) {
		FodinaMiner miner = new FodinaMiner(ieLog, settings);
		miner.clear();
		miner.getDependencyNet().setStartTask(depnet.getStartTask());
		miner.getDependencyNet().setEndTask(depnet.getEndTask());
		for (int a : depnet.getTasks())
			for (int b : depnet.getTasks())
				miner.getDependencyNet().setArc(a, b, depnet.isArc(a, b));
		miner.mineCausalNet(false);
		return CausalNetToPetrinet.toPetrinet(miner.getCausalNet());
	}

}
