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
package org.gephi.ui.spreadsimulator.plugin.stopcondition;

import javax.swing.JPanel;
import org.gephi.spreadsimulator.plugin.stopcondition.EndOfStructuralEvolution;
import org.gephi.spreadsimulator.spi.StopCondition;
import org.gephi.spreadsimulator.spi.StopConditionUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * 
 *
 * @author Cezary Bartosiak
 */
@ServiceProvider(service = StopConditionUI.class)
public class EndOfStructuralEvolutionUI implements StopConditionUI {
	private EndOfStructuralEvolutionPanel panel;
	private EndOfStructuralEvolution endOfStructuralEvolution;

	@Override
	public JPanel getSettingsPanel() {
		panel = new EndOfStructuralEvolutionPanel();
		return panel;
	}

	@Override
	public void setup(StopCondition stopCondition) {
		endOfStructuralEvolution = (EndOfStructuralEvolution)stopCondition;
		if (panel != null)
			panel.setInertia(endOfStructuralEvolution.getInertia());
	}

	@Override
	public void unsetup() {
		if (panel != null)
			endOfStructuralEvolution.setInertia(panel.getInertia());
		panel = null;
		endOfStructuralEvolution = null;
	}

	@Override
	public Class<? extends StopCondition> getStopConditionClass() {
		return EndOfStructuralEvolution.class;
	}

	@Override
	public String getDisplayName() {
		return NbBundle.getMessage(EndOfStructuralEvolutionUI.class, "EndOfStructuralEvolution.name");
	}
}
