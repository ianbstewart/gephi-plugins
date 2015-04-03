/*
 * Copyright 2008-2012 Gephi
 * Authors : Cezary Bartosiak
 * Website : http://www.gephi.org
 *
 * This file is part of Gephi.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Gephi Consortium. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 3 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://gephi.org/about/legal/license-notice/
 * or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License files at
 * /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 3, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 3] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 3 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 3 code and therefore, elected the GPL
 * Version 3 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Gephi Consortium.
 */
package org.gephi.complexstatistics.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
public class BestConnectedMetric implements Statistics, LongTask {
	private final String[] METRICS = {
		"Disjoin Metric Simple",
		"Disjoin Metric Size v1",
		"Disjoin Metric Size v2",
		"Disjoin Metric Distance",
		"Reach Metric"
	};
	private final String COLUMN_NAME = "set";
	private Boolean cancel = false;
	private ProgressTicket progressTicket;
	
	private int k = 1;
	private double epsilon = 0.0;
	private String metric = METRICS[0];

	public void execute(GraphModel graphModel, AttributeModel attributeModel) {
		HierarchicalGraph undirectedGraph = graphModel.getHierarchicalUndirectedGraphVisible();
		execute(undirectedGraph, attributeModel);
	}

	public void execute(HierarchicalGraph graph, AttributeModel attributeModel) {
		HierarchicalGraph undirectedGraph = graph.getView().getGraphModel().getHierarchicalUndirectedGraphVisible();
		calculateValue(undirectedGraph, attributeModel);
	}

	private void calculateValue(HierarchicalGraph graph, AttributeModel attributeModel) {
		cancel = false;

		Progress.start(progressTicket);
		
		if (!attributeModel.getNodeTable().hasColumn(COLUMN_NAME))
			attributeModel.getNodeTable().addColumn(COLUMN_NAME, AttributeType.BOOLEAN);
		else if (!attributeModel.getNodeTable().getColumn(COLUMN_NAME).getType().equals(AttributeType.BOOLEAN)) {
			attributeModel.getNodeTable().removeColumn(attributeModel.getNodeTable().getColumn(COLUMN_NAME));
			attributeModel.getNodeTable().addColumn(COLUMN_NAME, AttributeType.BOOLEAN);
		}

		Node[] nodes = graph.getNodes().toArray();
		int n = graph.getNodeCount();
		for (int i = 0; i < n && !cancel; i++)
			nodes[i].getNodeData().getAttributes().setValue(COLUMN_NAME, false);

		// 1.
		TreeSet<Integer> set = new TreeSet<Integer>();
		TreeSet<Integer> outset = new TreeSet<Integer>();
		List<Integer> temp = new ArrayList<Integer>();
		for (int i = 0; i < n && !cancel; i++)
			temp.add(i);
		Collections.shuffle(temp);
		for (int i = 0; i < n && !cancel; i++)
			if (i < k)
				set.add(temp.get(i));
			else outset.add(temp.get(i));
		
		// 2.
		GraphModel model = graph.getGraphModel();
		GraphView tempView = model.newView();
		HierarchicalGraph subGraph = model.getHierarchicalGraph(tempView);
		for (Integer i : set) {
			if (cancel)
				break;
			if (metric.equals(METRICS[4])) // Reach Metric
				nodes[i].getNodeData().getAttributes().setValue(COLUMN_NAME, true);
			else subGraph.removeNode(nodes[i]);
		}
		double f = calculateF(subGraph, attributeModel);
		if (metric.equals(METRICS[4])) // Reach Metric
			for (Integer i : set) {
				if (cancel)
					break;
				nodes[i].getNodeData().getAttributes().setValue(COLUMN_NAME, false);
			}
		model.destroyView(tempView);
		
		// 3. 4. 5.
		double df;
		do {
			df = Double.NEGATIVE_INFINITY;
			int ii = -1;
			int jj = -1;
			for (Integer i : set) {
				if (cancel)
					break;
				for (Integer j : outset) {
					if (cancel)
						break;
					tempView = model.newView();
					subGraph = model.getHierarchicalGraph(tempView);
					for (Integer x : set) {
						if (cancel)
							break;
						if (metric.equals(METRICS[4])) // Reach Metric
							if (x != i)
								nodes[x].getNodeData().getAttributes().setValue(COLUMN_NAME, true);
							else nodes[j].getNodeData().getAttributes().setValue(COLUMN_NAME, true);
						else if (x != i)
							subGraph.removeNode(nodes[x]);
						else subGraph.removeNode(nodes[j]);
					}
					double newf = calculateF(subGraph, attributeModel);
					if (metric.equals(METRICS[4])) // Reach Metric
						for (Integer x : set) {
							if (cancel)
								break;
							if (x != i)
								nodes[x].getNodeData().getAttributes().setValue(COLUMN_NAME, false);
							else nodes[j].getNodeData().getAttributes().setValue(COLUMN_NAME, false);
						}
					model.destroyView(tempView);
					if (df < newf - f) {
						df = newf - f;
						ii = i;
						jj = j;
					}
				}
			}
			if (df > epsilon) {
				set.remove(ii);
				set.add(jj);
				outset.remove(jj);
				outset.add(ii);
				f += df;
			}
		} while (df > epsilon && !cancel);
		
		// Finish
		for (Integer i : set) {
			if (cancel)
				break;
			nodes[i].getNodeData().getAttributes().setValue(COLUMN_NAME, true);
		}
		
		Progress.progress(progressTicket);
	}
	
	private double calculateF(HierarchicalGraph graph, AttributeModel attributeModel) {
		int n = graph.getNodeCount();
		double[][] d = Utils.floydWarshall(graph, cancel, progressTicket);
		if (metric.equals(METRICS[1]))
			return Utils.disjoinMetricSize1(n, d, cancel, progressTicket);
		if (metric.equals(METRICS[3]))
			return Utils.disjoinMetricDistance(n, d, cancel, progressTicket);
		if (metric.equals(METRICS[4]))
			return Utils.reachMetric(graph, attributeModel, COLUMN_NAME, d, cancel, progressTicket);
		int[] componentIndex = new int[n];
		int count = Utils.connectedComponents(graph, componentIndex, cancel, progressTicket);
		if (metric.equals(METRICS[2]))
			return Utils.disjoinMetricSize2(n, count, componentIndex, cancel, progressTicket);
		return Utils.disjoinMetricSimple(n, count);
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}
	
	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}
	
	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}
	
	public String[] getMetrics() {
		return METRICS;
	}

	public String getReport() {
		String report = "<html><body><h1>Reach Metric Report</h1>"
						+ "<hr>"
						+ "</body></html>";

		return report;
	}

	public boolean cancel() {
		cancel = true;
		return true;
	}

	public void setProgressTicket(ProgressTicket progressTicket) {
		this.progressTicket = progressTicket;
	}
}
