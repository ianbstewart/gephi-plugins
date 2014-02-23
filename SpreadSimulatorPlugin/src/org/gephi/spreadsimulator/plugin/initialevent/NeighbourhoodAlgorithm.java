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
package org.gephi.spreadsimulator.plugin.initialevent;

import java.util.*;
import java.util.Map.Entry;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import org.gephi.spreadsimulator.api.SimulationData;
import org.gephi.spreadsimulator.spi.TransitionAlgorithm;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
public class NeighbourhoodAlgorithm implements TransitionAlgorithm {
	private String[] states;

	public NeighbourhoodAlgorithm(String[] states) {
		this.states = states;
	}

	@Override
	public Edge tryDoTransition(SimulationData simulationData, Map<Edge, Double> probs) {
		Node ceNode = simulationData.getCurrentlyExaminedNode();
		String ceLocation = (String)ceNode.getNodeData().getAttributes().getValue(SimulationData.NM_CURRENT_LOCATION);
		List<Node> neighbors = new LinkedList<Node>();
		for (Node node : simulationData.getSnapshotGraphForCurrentStep().getNeighbors(ceNode)) {
			String location = (String)node.getNodeData().getAttributes().getValue(SimulationData.NM_CURRENT_LOCATION);
			if (!simulationData.isNodesLocations() || ceLocation.equals(location))
				neighbors.add(node);
		}
		if (simulationData.isEdgesActivation()) {
			int min = simulationData.getMinActivatedEdges();
			int max = simulationData.getMaxActivatedEdges();
			int x = new Random().nextInt(max - min + 1) + min;
			if (x < neighbors.size()) {
				int y = neighbors.size() - x;
				Collections.shuffle(neighbors);
				while (y > 0) {
					neighbors.remove(0);
					y--;
				}
			}
		}
		for (Node node : neighbors) {
			double quality = 1.0;
			if (simulationData.isNodesQualities())
				quality = (Double)node.getNodeData().getAttributes().getValue(SimulationData.NM_QUALITY);
			for (String state : states)
				if (state.equals(node.getNodeData().getAttributes().getValue(SimulationData.NM_CURRENT_STATE))) {
					double p = new Random().nextDouble();
					double sum = 0.0;
					for (Entry<Edge, Double> prob : probs.entrySet()) {
						sum += prob.getValue() * quality;
						if (p <= sum)
							return prob.getKey();
					}
				}
		}
		return null;
	}
}
