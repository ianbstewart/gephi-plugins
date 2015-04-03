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
package org.gephi.ui.complexstatistics.plugin;

import javax.swing.JPanel;
import org.gephi.complexstatistics.plugin.BestConnectedMetric;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
@ServiceProvider(service = StatisticsUI.class)
public class BestConnectedMetricUI implements StatisticsUI {
	private BestConnectedMetricPanel panel;
	private BestConnectedMetric bc;

	public JPanel getSettingsPanel() {
		if (panel == null)
			panel = new BestConnectedMetricPanel();
		return panel;
	}

	public void setup(Statistics statistics) {
		bc = (BestConnectedMetric)statistics;
		
		if (panel == null)
			panel = new BestConnectedMetricPanel();
			
		panel.setMetrics(bc.getMetrics());
		panel.setK(bc.getK());
		panel.setEpsilon(bc.getEpsilon());
		panel.setMetric(bc.getMetric());
	}

	public void unsetup() {
		bc.setK(panel.getK());
		bc.setEpsilon(panel.getEpsilon());
		bc.setMetric(panel.getMetric());
		panel = null;
	}

	public Class<? extends Statistics> getStatisticsClass() {
		return BestConnectedMetric.class;
	}

	public String getValue() {
		return "";
	}

	public String getDisplayName() {
		return "Best Connected Metric";
	}

	public String getShortDescription() {
		return "Best Connected Metric";
	}

	public String getCategory() {
		return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
	}

	public int getPosition() {
		return 8;
	}
}
