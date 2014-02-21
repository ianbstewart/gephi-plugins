/*
 * Copyright 2008-2010 Gephi
 * Authors : Cezary Bartosiak
 * Website : http://www.gephi.org
 * 
 * This file is part of Gephi.
 *
 * Gephi is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gephi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.spreadsimulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.gephi.data.attributes.type.Interval;
import org.gephi.data.attributes.type.TimeInterval;
import org.gephi.dynamic.api.DynamicGraph;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.spreadsimulator.api.SimulationData;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
public class SimulationDataImpl implements SimulationData {
	private final SimulationImpl simulation;
	
	private final GraphModel networkModel;
	private final DynamicModel networkDynamicModel;
	private final GraphModel stateMachineModel;
	
	private Graph currentSnapshot;
	
	private String defaultState;
	private List<String> states;
	private Map<String, Node> smNodes;
	private Map<String, Float> rMap;
	private Map<String, Float> gMap;
	private Map<String, Float> bMap;
	
	private List<Map<String, Integer>> nodesCount;

	private int currentStep;
	private Node ceNode;

	public SimulationDataImpl(SimulationImpl simulation, GraphModel networkModel, DynamicModel networkDynamicModel,
																				  GraphModel stateMachineModel) {
		this.simulation = simulation;
		
		this.networkModel = networkModel;
		this.networkDynamicModel = networkDynamicModel;
		this.stateMachineModel = stateMachineModel;
		
		if (networkDynamicModel.isDynamicGraph()) {
			DynamicGraph dynamicGraph = networkDynamicModel.createDynamicGraph(networkModel.getGraph());
			double window = calculateWindow(networkModel.getGraph(), simulation.getGranularity());
			double min = networkDynamicModel.getMin();
			currentSnapshot = dynamicGraph.getSnapshotGraph(min, min + window);
		}
		else currentSnapshot = networkModel.getGraph();

		states = new ArrayList<String>();
		smNodes = new HashMap<String, Node>();
		rMap = new HashMap<String, Float>();
		gMap = new HashMap<String, Float>();
		bMap = new HashMap<String, Float>();
		for (Node node : stateMachineModel.getGraph().getNodes()) {
			String state = (String)node.getNodeData().getAttributes().getValue(SM_STATE_NAME);
			if ((Boolean)node.getNodeData().getAttributes().getValue(SM_DEFAULT_STATE))
				defaultState = state;
			states.add(state);
			smNodes.put(state, node);
			rMap.put(state, node.getNodeData().r());
			gMap.put(state, node.getNodeData().g());
			bMap.put(state, node.getNodeData().b());
		}

		nodesCount = new ArrayList<Map<String, Integer>>();
		Map<String, Integer> map = new HashMap<String, Integer>();
		nodesCount.add(map);
		for (String state : states)
			if (state.equals(defaultState))
				nodesCount.get(0).put(state, currentSnapshot.getNodeCount());
			else nodesCount.get(0).put(state, 0);

		currentStep = 0;
		ceNode = networkModel.getGraph().getNodes().toArray()[0];
	}

	@Override
	public GraphModel getNetworkModel() {
		return networkModel;
	}
	
	@Override
	public DynamicModel getNetworkDynamicModel() {
		return networkDynamicModel;
	}

	@Override
	public GraphModel getStateMachineModel() {
		return stateMachineModel;
	}

	@Override
	public Graph getSnapshotGraphForCurrentStep() {
		return currentSnapshot;
	}
	
	@Override
	public boolean isNodesQualities() {
		return simulation.isNodesQualities();
	}
	
	@Override
	public boolean isEdgesActivation() {
		return simulation.isEdgesActivation();
	}
	
	@Override
	public int getMinActivatedEdges() {
		return simulation.getMinActivatedEdges();
	}
	
	@Override
	public int getMaxActivatedEdges() {
		return simulation.getMaxActivatedEdges();
	}
	
	@Override
	public double getGranularity() {
		return simulation.getGranularity();
	}

	@Override
	public String getDefaultState() {
		return defaultState;
	}
	
	@Override
	public int getLatencyForState(String state) {
		Integer latency = (Integer)smNodes.get(state).getNodeData().getAttributes().getValue(SM_LATENCY);
		return latency != null ? latency : 0;
	}

	@Override
	public int getNodesCountInStateAndStep(String state, int step) {
		return nodesCount.get(step).get(state);
	}

	@Override
	public int getCurrentStep() {
		return currentStep;
	}

	@Override
	public Node getCurrentlyExaminedNode() {
		return ceNode;
	}

	public String[] getStates() {
		return states.toArray(new String[0]);
	}

	public Node getStateMachineNodeForState(String state) {
		return smNodes.get(state);
	}

	public float getRForState(String state) {
		return rMap.get(state);
	}

	public float getGForState(String state) {
		return gMap.get(state);
	}

	public float getBForState(String state) {
		return bMap.get(state);
	}

	/*** [GCD + Window] ***/
	
	private final double MAX_K_INTEGER = 1e14;
	
	private double epsilon = 0.00000000000005;
	
	private double gcd(Double a, Double b) {
		int count;
		double larger, divisor, remainder, lowerLimit;
		
		if (a.isInfinite() || b.isInfinite())
			return 0.0;
		a = Math.abs(a);
		b = Math.abs(b);
		if (a == 0)
			return b;
		if (b == 0)
			return a;
		if (a > b) {
			larger = a;
			divisor = b;
		}
		else {
			larger = b;
			divisor = a;
		}
		lowerLimit = larger * epsilon;
		if (divisor <= lowerLimit || larger >= MAX_K_INTEGER)
			return 0.0;
		for (count = 1; count < 50; count++) {
			remainder = Math.abs(larger % divisor);
			if (remainder <= lowerLimit || Math.abs(divisor - remainder) <= lowerLimit) {
				if (remainder != 0.0 && divisor <= (100.0 * lowerLimit))
					return 0.0;
				return divisor;
			}
			larger = divisor;
			divisor = remainder;
		}
		return 0.0;
	}
	
	private double gcdVerified(double a, double b) {
		double divisor, c, d;
		
		divisor = gcd(a, b);
		if (divisor != 0.0) {
			c = a / divisor;
			d = b / divisor;
			if (c % 1.0 != 0.0 || d % 1.0 != 0.0)
				return 0.0;
			if (gcd(c, d) != 1.0)
				return 0.0;
		}
		return divisor;
	}
	
	private double calculateWindow(Graph graph, double granularity) {
		double window = 1.0;
		List<Interval> intervals = new ArrayList<Interval>();
		for (Node node : graph.getNodes()) {
			TimeInterval timeInterval = (TimeInterval)node.getAttributes().getValue("time_interval");
			if (timeInterval != null)
				intervals.addAll(timeInterval.getIntervals());
		}
		for (Edge edge : graph.getEdges()) {
			TimeInterval timeInterval = (TimeInterval)edge.getAttributes().getValue("time_interval");
			if (timeInterval != null)
				intervals.addAll(timeInterval.getIntervals());
		}
		HashSet<Double> hmoments = new HashSet<Double>();
		for (Interval interval : intervals) {
			Double low = interval.getLow();
			Double high = interval.getHigh();
			if (!hmoments.contains(low) && !low.isInfinite())
				hmoments.add(low);
			if (!hmoments.contains(high) && !high.isInfinite())
				hmoments.add(high);
		}
		Double[] moments = hmoments.toArray(new Double[0]);
		Arrays.sort(moments);
		Stack<Double> lens = new Stack<Double>();
		for (int i = 0; i < moments.length - 1; i++)
			lens.push(moments[i + 1] - moments[i]);
		while (lens.size() > 1) {
			double last = lens.pop();
			double prevlast = lens.pop();
			double gcd = gcdVerified(last, prevlast);
			lens.push(gcd);
		}
		window = lens.pop();
		return window * granularity;
	}
	
	/*** [/GCD + Window] ***/
	
	public void incrementCurrentStep() {
		currentStep++;
		if (networkDynamicModel.isDynamicGraph()) {
			DynamicGraph dynamicGraph = networkDynamicModel.createDynamicGraph(networkModel.getGraph());
			double window = calculateWindow(networkModel.getGraph(), simulation.getGranularity());
			double min = networkDynamicModel.getMin();
			double max = networkDynamicModel.getMax();
			double low = min + currentStep * window;
			double high = min + (currentStep + 1) * window;
			if (low >= max) {
				low = max - window;
				high = max;
			}
			currentSnapshot = dynamicGraph.getSnapshotGraph(low, high);
		}
		else currentSnapshot = networkModel.getGraph();
		Map<String, Integer> map = new HashMap<String, Integer>();
		nodesCount.add(map);
		refreshNodesCount();
	}

	public void setCurrentlyExaminedNode(Node ceNode) {
		this.ceNode = ceNode;
	}

	private void refreshNodesCount() {
		for (String state : states)
			nodesCount.get(currentStep).put(state, 0);
		for (Node node : currentSnapshot.getNodes()) {
			String state = (String)node.getNodeData().getAttributes().getValue(NM_CURRENT_STATE);
			if (state != null) {
				Integer value = nodesCount.get(currentStep).get(state);
				nodesCount.get(currentStep).put(state, value + 1);
			}
		}
	}
}
