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

import java.util.HashMap;
import java.util.Stack;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
class Utils {
	public static double[][] floydWarshall(HierarchicalGraph graph, Boolean cancel, ProgressTicket progressTicket) {
		int n = graph.getNodeCount();
		double[][] d = new double[n][n];
		Node[] nodes = graph.getNodes().toArray();
		for (int i = 0; i < n && !cancel; i++)
			for (int j = 0; j < n && !cancel; j++) {
				if (i == j)
					d[i][j] = 0.0;
				else if (graph.isAdjacent(nodes[i], nodes[j]))
					d[i][j] = 1.0; // TODO: consider edge weight
				else d[i][j] = Double.POSITIVE_INFINITY;
				Progress.progress(progressTicket);
			}
		for (int k = 0; k < n && !cancel; k++)
			for (int i = 0; i < n && !cancel; i++) {
				for (int j = 0; j < n && !cancel; j++)
					d[i][j] = Math.min(d[i][j], d[i][k] + d[k][j]);
				Progress.progress(progressTicket);
			}
		return d;
	}
	
	public static int connectedComponents(HierarchicalGraph graph, int[] componentIndex,
										Boolean cancel, ProgressTicket progressTicket) {
		int n = graph.getNodeCount();
		int count = 0;
		
		int index = 0;
		HashMap<Node, Integer> indices = new HashMap<Node, Integer>();
		for (Node node : graph.getNodes()) {
			if (cancel)
				break;
			indices.put(node, index++);
		}

		boolean[] visited = new boolean[n];
		for (Node node : graph.getNodes()) {
			if (cancel)
				break;
			if (!visited[indices.get(node)]) {
				Stack<Node> stack = new Stack<Node>();
				stack.push(node);
				while (!stack.empty()) {
					if (cancel)
						break;
					Progress.progress(progressTicket);
					Node v = stack.pop();
					visited[indices.get(v)] = true;
					componentIndex[indices.get(v)] = count;
					for (Node w : graph.getNeighbors(v)) {
						if (cancel)
							break;
						if (!visited[indices.get(w)])
							stack.push(w);
					}
				}
				count++;
			}
		}
		return count;
	}
	
	public static double disjoinMetricSimple(int n, int count) {
		return (double)count / n;
	}
	
	public static double disjoinMetricSize1(int n, double[][] d, Boolean cancel, ProgressTicket progressTicket) {
		double sum = 0.0;
		for (int i = 0; i < n && !cancel; i++)
			for (int j = 0; j < n && !cancel; j++) {
				if (i != j)
					sum += d[i][j] < Double.POSITIVE_INFINITY ? 1 : 0;
				Progress.progress(progressTicket);
			}
		return 1 - sum / (double)(n * (n - 1));
	}
	
	public static double disjoinMetricSize2(int n, int count, int[] componentIndex,
											Boolean cancel, ProgressTicket progressTicket) {
		int[] size = new int[count];
		for (int i = 0; i < n; i++) {
			if (cancel)
				break;
			Progress.progress(progressTicket);
			size[componentIndex[i]]++;
		}
		
		double sum = 0.0;
		for (int k = 0; k < count && !cancel; k++)
			sum += size[k] * (size[k] - 1);
		return 1 - sum / (double)(n * (n - 1));
	}
	
	public static double disjoinMetricDistance(int n, double[][] d, Boolean cancel, ProgressTicket progressTicket) {
		double sum = 0.0;
		for (int i = 0; i < n && !cancel; i++)
			for (int j = 0; j < n && !cancel; j++) {
				if (i != j)
					sum += 1.0 / d[i][j];
				Progress.progress(progressTicket);
			}
		return 1 - sum / (double)(n * (n - 1));
	}
	
	public static double reachMetric(HierarchicalGraph graph, AttributeModel attributeModel, String columnName,
									double[][] d, Boolean cancel, ProgressTicket progressTicket) {
		int n = graph.getNodeCount();
		int size = 0;
		
		Node[] nodes = graph.getNodes().toArray();
		boolean[] inset = new boolean[n];
		if (attributeModel.getNodeTable().hasColumn(columnName) && attributeModel.getNodeTable().getColumn(columnName).
				getType().equals(AttributeType.BOOLEAN))
			for (int i = 0; i < n && !cancel; i++) {
				Object val = nodes[i].getNodeData().getAttributes().getValue(columnName);
				if (val != null && Boolean.valueOf(val.toString())) {
					inset[i] = true;
					size++;
				}
				Progress.progress(progressTicket);
			}

		double[] dset = new double[n];
		for (int i = 0; i < n && !cancel; i++) {
			dset[i] = Double.POSITIVE_INFINITY;
			Progress.progress(progressTicket);
		}
		for (int i = 0; i < n && !cancel; i++) {
			if (!inset[i])
				for (int j = 0; j < n && !cancel; j++) {
					if (inset[j]) {
						if (d[i][j] < dset[i])
							dset[i] = d[i][j];
					}
				}
			else dset[i] = 0;
			Progress.progress(progressTicket);
		}

		double sum = 0.0;
		for (int j = 0; j < n && !cancel; j++) {
			if (!inset[j])
				sum += 1 / dset[j];
			Progress.progress(progressTicket);
		}
		return sum / (n - size);
	}
}
