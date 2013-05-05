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
package org.gephi.spreadsimulator.plugin.stopcondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import org.gephi.data.attributes.type.Interval;
import org.gephi.data.attributes.type.TimeInterval;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.spreadsimulator.api.SimulationData;
import org.gephi.spreadsimulator.spi.StopCondition;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
public class EndOfStructuralEvolution implements StopCondition {
	private int inertia = 10;
	
	private int maxSteps = -1;
	
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
	
	@Override
	public boolean isOccuring(SimulationData simulationData) {
		if (maxSteps < 0 && simulationData.getNetworkDynamicModel().isDynamicGraph()) {
			double granularity = simulationData.getGranularity();
			double window = calculateWindow(simulationData.getNetworkModel().getGraph(), granularity);
			double min = simulationData.getNetworkDynamicModel().getMin();
			double max = simulationData.getNetworkDynamicModel().getMax();
			double steps = (max - min) / window;
			maxSteps = (int)steps + inertia;
		}
		if (simulationData.getCurrentStep() >= maxSteps)
			return true;
		return false;
	}

	public int getInertia() {
		return inertia;
	}

	public void setInertia(int inertia) {
		if (maxSteps > -1)
			maxSteps += inertia - this.inertia;
		this.inertia = inertia;
	}
}
