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

import java.util.*;
import java.util.Map.Entry;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceProvider;
import org.gephi.spreadsimulator.api.*;
import static org.gephi.spreadsimulator.api.SimulationData.LM_DEFAULT_LOCATION;
import static org.gephi.spreadsimulator.api.SimulationData.LM_LOCATION_NAME;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
@ServiceProvider(service = LocationChangeStrategy.class)
public class LocationChangeStrategyImpl implements LocationChangeStrategy {
	private Map<String, Double> locationsProbabilities;
	
	@Override
	public void changeLocations() {
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		Workspace[] workspaces = pc.getCurrentProject().getLookup().lookup(WorkspaceProvider.class).getWorkspaces();
		GraphController gc = Lookup.getDefault().lookup(GraphController.class);
		GraphModel gm = gc.getModel(workspaces[0]);
		
		List<Node> nodes = new ArrayList<Node>();
		for (Node node : gm.getGraph().getNodes())
			nodes.add(node);
		Collections.shuffle(nodes);
		
		int index = 0;
		for (Entry<String, Double> entry : locationsProbabilities.entrySet()) {
			String location = entry.getKey();
			long count = Math.round(entry.getValue() * nodes.size());
			for (int i = 0; i < count && index < nodes.size(); i++, index++) {
				String temp = nodes.get(index).getNodeData().getAttributes().getValue(SimulationData.NM_CURRENT_LOCATION) + "-";
				nodes.get(index).getNodeData().getAttributes().setValue(SimulationData.NM_CURRENT_LOCATION, temp);
				nodes.get(index).getNodeData().getAttributes().setValue(SimulationData.NM_CURRENT_LOCATION, location);
			}
			if (index >= nodes.size())
				break;
		}
	}
	
	private boolean areLocationsDifferent(Node[] nodes) {
		if (nodes.length != locationsProbabilities.size())
			return true;
		for (Node node : nodes) {
			String location = (String)node.getNodeData().getAttributes().getValue(LM_LOCATION_NAME);
			if (!locationsProbabilities.containsKey(location))
				return true;
		}
		return false;
	}
	
	public Map<String, Double> getLocationsProbabilities() {
		if (locationsProbabilities == null)
			locationsProbabilities = new HashMap<String, Double>();

		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		Workspace[] workspaces = pc.getCurrentProject().getLookup().lookup(WorkspaceProvider.class).getWorkspaces();
		if (workspaces.length > 2) {
			GraphController gc = Lookup.getDefault().lookup(GraphController.class);
			GraphModel gm = gc.getModel(workspaces[2]);
			
			Node[] nodes = gm.getGraph().getNodes().toArray();
			if (areLocationsDifferent(nodes)) {
				locationsProbabilities.clear();
				for (Node node : nodes) {
					String location = (String)node.getNodeData().getAttributes().getValue(LM_LOCATION_NAME);
					if ((Boolean)node.getNodeData().getAttributes().getValue(LM_DEFAULT_LOCATION))
						locationsProbabilities.put(location, 1.0);
					else locationsProbabilities.put(location, 0.0);
				}
			}
		}
		else if (locationsProbabilities.isEmpty())
			locationsProbabilities.put("L1", 1.0);

		return locationsProbabilities;
	}
	
	public void setLocationsProbabilities(Map<String, Double> locationsProbabilities) {
		this.locationsProbabilities.clear();
		for (Entry<String, Double> entry : locationsProbabilities.entrySet())
			this.locationsProbabilities.put(entry.getKey(), entry.getValue());
	}
}
