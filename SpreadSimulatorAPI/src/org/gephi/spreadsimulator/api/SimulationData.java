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
package org.gephi.spreadsimulator.api;

import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.openide.util.NbBundle;

/**
 *
 * 
 * @author Cezary Bartosiak
 */
public interface SimulationData {
	public static final String NM_QUALITY                  = "Quality";
	public static final String NM_CURRENT_STATE            = "CurrentState";
	public static final String NM_CURRENT_LOCATION         = "CurrentLocation";
	public static final String NM_STEPS_TO_CHANGE_LOCATION = "StepsToChangeLocation";
	public static final String NM_CURRENT_LATENCY          = "CurrentLatency";
	
	public static final String NM_CURRENT_STATE_TITLE =
			NbBundle.getMessage(SimulationData.class, "SimulationData.CurrentStateTitle");
	public static final String NM_CURRENT_LOCATION_TITLE =
			NbBundle.getMessage(SimulationData.class, "SimulationData.CurrentLocationTitle");
	public static final String NM_STEPS_TO_CHANGE_LOCATION_TITLE =
			NbBundle.getMessage(SimulationData.class, "SimulationData.StepsToChangeLocationTitle");
	public static final String NM_CURRENT_LATENCY_TITLE =
			NbBundle.getMessage(SimulationData.class, "SimulationData.CurrentLatencyTitle");
	
	public static final String SM_STATE_NAME    = "StateName";
	public static final String SM_DEFAULT_STATE = "DefaultState";
	public static final String SM_LATENCY_MIN   = "LatencyMin";
	public static final String SM_LATENCY_MAX   = "LatencyMax";
	public static final String SM_INITIAL_EVENT = "InitialEvent";
	public static final String SM_CHOICE        = "Choice";
	public static final String SM_PROBABILITY   = "Probability";
	
	public static final String LM_LOCATION_NAME    = "LocationName";
	public static final String LM_DEFAULT_LOCATION = "DefaultLocation";
	public static final String LM_PROBABILITY      = "Probability";

	public GraphModel getNetworkModel();
	
	public DynamicModel getNetworkDynamicModel();

	public GraphModel getStateMachineModel();
	
	public GraphModel getLocationMachineModel();
	
	public Graph getSnapshotGraphForCurrentStep();
	
	public boolean isNodesQualities();
	
	public boolean isNodesLocations();
	
	public boolean isEdgesActivation();
	
	public int getMinActivatedEdges();
	
	public int getMaxActivatedEdges();
	
	public double getGranularity();

	public String getDefaultState();
	
	public String getDefaultLocation();
	
	public int getLatencyForState(String state);

	public int getNodesCountInStateAndStep(String state, int step);

	public int getCurrentStep();

	public Node getCurrentlyExaminedNode();
}
