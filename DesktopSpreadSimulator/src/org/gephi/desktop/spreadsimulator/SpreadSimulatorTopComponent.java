/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.desktop.spreadsimulator;

import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JPanel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import org.gephi.spreadsimulator.api.*;
import org.gephi.spreadsimulator.api.SimulationEvent.EventType;
import org.gephi.spreadsimulator.spi.StopCondition;
import org.gephi.spreadsimulator.spi.StopConditionBuilder;
import org.gephi.spreadsimulator.spi.StopConditionUI;
import org.gephi.ui.components.SimpleHTMLReport;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
//import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.gephi.desktop.spreadsimulator//SpreadSimulator//EN",
					 autostore = false)
public final class SpreadSimulatorTopComponent extends TopComponent {
	private static SpreadSimulatorTopComponent instance;
	/** path to the icon used by the component and its open action */
//    static final String ICON_PATH = "SET/PATH/TO/ICON/HERE";
	private static final String PREFERRED_ID = "SpreadSimulatorTopComponent";

	private Map<String, StopCondition>   scMap;
	private Map<String, StopConditionUI> scUIMap;
	private ComboBoxModel    scComboBoxModel;
	private DefaultListModel scListModel;

	private StateChangeStrategy scs;
	private StateChangeStrategyUI scsUI;
	private RemovalStrategy rs;
	private RemovalStrategyUI rsUI;
	private LocationChangeStrategy lcs;
	private LocationChangeStrategyUI lcsUI;

	private int simcount = 1;
	private Simulation simulation;

	public SpreadSimulatorTopComponent() {
		StopConditionBuilder[] scBuilders = Lookup.getDefault().lookupAll(StopConditionBuilder.class).toArray(new StopConditionBuilder[0]);
		StopConditionUI[]      scUIs      = Lookup.getDefault().lookupAll(StopConditionUI.class).toArray(new StopConditionUI[0]);
		scMap   = new HashMap<String, StopCondition>();
		scUIMap = new HashMap<String, StopConditionUI>();
		for (StopConditionBuilder scb : scBuilders)
			scMap.put(scb.getName(), scb.getStopCondition());
		for (StopConditionUI scui : scUIs)
			scUIMap.put(scui.getDisplayName(), scui);
		scComboBoxModel = new DefaultComboBoxModel(scMap.keySet().toArray());
		scListModel = new DefaultListModel();

		scs = Lookup.getDefault().lookup(StateChangeStrategy.class);
		scsUI = Lookup.getDefault().lookup(StateChangeStrategyUI.class);
		rs = Lookup.getDefault().lookup(RemovalStrategy.class);
		rsUI = Lookup.getDefault().lookup(RemovalStrategyUI.class);
		lcs = Lookup.getDefault().lookup(LocationChangeStrategy.class);
		lcsUI = Lookup.getDefault().lookup(LocationChangeStrategyUI.class);

		simulation = Lookup.getDefault().lookup(Simulation.class);
		simulation.addSimulationListener(new SimulationListener() {
			@Override
			public void simulationChanged(SimulationEvent event) {
				if (event.is(EventType.INIT)) {
					scListModel.clear();
					addSCButton.setEnabled(true);
					removeSCButton.setEnabled(false);
					fireSCSButton.setEnabled(true);
					fireRSButton.setEnabled(true);
					fireLCSButton.setEnabled(simulation.isNodesLocations());
					nodesQualitiesCheckBox.setSelected(simulation.isNodesQualities());
					nodesQualitiesCheckBox.setEnabled(true);
					nodesLocationsCheckBox.setSelected(simulation.isNodesLocations());
					nodesLocationsCheckBox.setEnabled(true);
					minLocationChangeIntervalSpinner.setValue(simulation.getMinLocationChangeInterval());
					minLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations());
					maxLocationChangeIntervalSpinner.setValue(simulation.getMaxLocationChangeInterval());
					maxLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations());
					edgesActivationCheckBox.setSelected(simulation.isEdgesActivation());
					edgesActivationCheckBox.setEnabled(true);
					minActivatedEdgesSpinner.setValue(simulation.getMinActivatedEdges());
					minActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation());
					maxActivatedEdgesSpinner.setValue(simulation.getMaxActivatedEdges());
					maxActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation());
					granularityFormattedTextField.setText(simulation.getGranularity() + "");
					granularityFormattedTextField.setEnabled(true);
					delayFormattedTextField.setText(simulation.getDelay() + "");
					delayFormattedTextField.setEnabled(true);
					simcount = 1;
					simcountFormattedTextField.setText("1");
					simcountFormattedTextField.setEnabled(true);
					stopButton.setEnabled(false);
					startButton.setEnabled(false);
					previousStepButton.setEnabled(false);
					nextStepButton.setEnabled(false);
					stepLabel.setText(NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.stepLabel.text")
						+ " " + simulation.getSimulationData().getCurrentStep());
				}
				else if (event.is(EventType.ADD_STOP_CONDITION)) {
					removeSCButton.setEnabled(true);
					startButton.setEnabled(!simulation.isFinished());
					nextStepButton.setEnabled(simcount == 1 && !simulation.isFinished());
				}
				else if (event.is(EventType.REMOVE_STOP_CONDITION)) {
					removeSCButton.setEnabled(!scListModel.isEmpty());
					startButton.setEnabled(!scListModel.isEmpty() && !simulation.isFinished());
					nextStepButton.setEnabled(!scListModel.isEmpty() && simcount == 1 && !simulation.isFinished());
				}
				else if (event.is(EventType.CANCEL)) {
					addSCButton.setEnabled(true);
					removeSCButton.setEnabled(true);
					fireSCSButton.setEnabled(true);
					fireRSButton.setEnabled(true);
					fireLCSButton.setEnabled(simulation.isNodesLocations());
					nodesQualitiesCheckBox.setEnabled(simcount > 1 && !simulation.isFinished()
															|| simulation.getSimulationData().getCurrentStep() == 0);
					nodesLocationsCheckBox.setEnabled(simcount > 1 && !simulation.isFinished()
															|| simulation.getSimulationData().getCurrentStep() == 0);
					minLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations()
														&& simcount > 1 && !simulation.isFinished()
														|| simulation.getSimulationData().getCurrentStep() == 0);
					maxLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations()
														&& simcount > 1 && !simulation.isFinished()
														|| simulation.getSimulationData().getCurrentStep() == 0);
					edgesActivationCheckBox.setEnabled(simcount > 1 && !simulation.isFinished()
															|| simulation.getSimulationData().getCurrentStep() == 0);
					minActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation()
														&& simcount > 1 && !simulation.isFinished()
														|| simulation.getSimulationData().getCurrentStep() == 0);
					maxActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation()
														&& simcount > 1 && !simulation.isFinished()
														|| simulation.getSimulationData().getCurrentStep() == 0);
					granularityFormattedTextField.setEnabled(simcount > 1 && !simulation.isFinished()
															|| simulation.getSimulationData().getCurrentStep() == 0);
					delayFormattedTextField.setEnabled(true);
					simcountFormattedTextField.setEnabled(!simulation.isFinished());
					stopButton.setEnabled(false);
					startButton.setEnabled(!simulation.isFinished());
					nextStepButton.setEnabled(simcount == 1 && !simulation.isFinished());
				}
				else if (event.is(EventType.START)) {
					stopButton.setEnabled(true);
				}
				else if (event.is(EventType.PREVIOUS_STEP)) {
					addSCButton.setEnabled(true);
					removeSCButton.setEnabled(true);
					fireSCSButton.setEnabled(true);
					fireRSButton.setEnabled(true);
					fireLCSButton.setEnabled(simulation.isNodesLocations());
					nodesQualitiesCheckBox.setEnabled(simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					nodesLocationsCheckBox.setEnabled(simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					minLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations()
														&& simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					maxLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations()
														&& simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					edgesActivationCheckBox.setEnabled(simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					minActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation()
														&& simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					maxActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation()
														&& simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					granularityFormattedTextField.setEnabled(simcount == 1 && simulation.getSimulationData().getCurrentStep() == 0);
					simcountFormattedTextField.setEnabled(true);
					stopButton.setEnabled(false);
					startButton.setEnabled(true);
					previousStepButton.setEnabled(simcount == 1 && simulation.getSimulationData().getCurrentStep() > 0);
					nextStepButton.setEnabled(simcount == 1);
				}
				else if (event.is(EventType.NEXT_STEP)) {
					addSCButton.setEnabled(simulation.isCancelled());
					removeSCButton.setEnabled(simulation.isCancelled());
					fireSCSButton.setEnabled(simulation.isCancelled());
					fireRSButton.setEnabled(simulation.isCancelled());
					fireLCSButton.setEnabled(simulation.isNodesLocations() && simulation.isCancelled());
					nodesQualitiesCheckBox.setEnabled(simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					nodesLocationsCheckBox.setEnabled(simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					minLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations()
														&& simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					maxLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations()
														&& simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					edgesActivationCheckBox.setEnabled(simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					minActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation()
														&& simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					maxActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation()
														&& simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					granularityFormattedTextField.setEnabled(simulation.isCancelled() && simulation.getSimulationData().getCurrentStep() == 0);
					delayFormattedTextField.setEnabled(simulation.isCancelled());
					simcountFormattedTextField.setEnabled(simulation.isCancelled());
					stopButton.setEnabled(!simulation.isCancelled());
					startButton.setEnabled(simulation.isCancelled());
					stepLabel.setText(NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.stepLabel.text")
							+ " " + simulation.getSimulationData().getCurrentStep());
					// previousStepButton.setEnabled(true);
					nextStepButton.setEnabled(simcount == 1 && simulation.isCancelled());
				}
				else if (event.is(EventType.FINISHED)) {
					addSCButton.setEnabled(true);
					removeSCButton.setEnabled(true);
					fireSCSButton.setEnabled(true);
					fireRSButton.setEnabled(true);
					fireLCSButton.setEnabled(simulation.isNodesLocations());
					nodesQualitiesCheckBox.setEnabled(false);
					nodesLocationsCheckBox.setEnabled(false);
					minLocationChangeIntervalSpinner.setEnabled(false);
					maxLocationChangeIntervalSpinner.setEnabled(false);
					edgesActivationCheckBox.setEnabled(false);
					minActivatedEdgesSpinner.setEnabled(false);
					maxActivatedEdgesSpinner.setEnabled(false);
					granularityFormattedTextField.setEnabled(false);
					delayFormattedTextField.setEnabled(false);
					simcountFormattedTextField.setEnabled(false);
					stopButton.setEnabled(false);
					startButton.setEnabled(false);
					stepLabel.setText(NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.stepLabel.text")
							+ " " + simulation.getSimulationData().getCurrentStep());
					// previousStepButton.setEnabled(true);
					nextStepButton.setEnabled(false);
					WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
						public void run() {
							String report = simulation.getReport();
							SimpleHTMLReport dialog = new SimpleHTMLReport(WindowManager.getDefault().getMainWindow(), report);
						}
					});
				}
			}
		});

		initComponents();
		setName(NbBundle.getMessage(SpreadSimulatorTopComponent.class, "CTL_SpreadSimulatorTopComponent"));
		// setToolTipText(NbBundle.getMessage(SpreadSimulatorTopComponent.class, "HINT_SpreadSimulatorTopComponent"));
//        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
		putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
	}

	public void StartSimulations(final Simulation simulation, final int count) {
		LongTaskExecutor executor = new LongTaskExecutor(true, "Simulation", 10);
		executor.execute(simulation, new Runnable() {
			@Override
			public void run() {
				simulation.start(count);
			}
		}, "Simulation", null);
	}

	private AbstractFormatterFactory getSimcountFormatterFactory() {
		NumberFormatter formatter = new NumberFormatter(new DecimalFormat("#0"));
		formatter.setMinimum(1);
		formatter.setAllowsInvalid(false);
		formatter.setCommitsOnValidEdit(true);
		return new DefaultFormatterFactory(formatter);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        fireSCSButton = new javax.swing.JButton();
        initButton = new javax.swing.JButton();
        fireRSButton = new javax.swing.JButton();
        stepLabel = new javax.swing.JLabel();
        addSCButton = new javax.swing.JButton();
        removeSCButton = new javax.swing.JButton();
        scScrollPane = new javax.swing.JScrollPane();
        scList = new javax.swing.JList();
        nextStepButton = new javax.swing.JButton();
        scComboBox = new javax.swing.JComboBox();
        previousStepButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        startButton = new javax.swing.JButton();
        simcountLabel = new javax.swing.JLabel();
        simcountFormattedTextField = new javax.swing.JFormattedTextField();
        delayFormattedTextField = new javax.swing.JFormattedTextField();
        delayLabel = new javax.swing.JLabel();
        granularityLabel = new javax.swing.JLabel();
        granularityFormattedTextField = new javax.swing.JFormattedTextField();
        nodesQualitiesCheckBox = new javax.swing.JCheckBox();
        edgesActivationCheckBox = new javax.swing.JCheckBox();
        minActivatedEdgesLabel = new javax.swing.JLabel();
        maxActivatedEdgesLabel = new javax.swing.JLabel();
        minActivatedEdgesSpinner = new javax.swing.JSpinner();
        maxActivatedEdgesSpinner = new javax.swing.JSpinner();
        fireLCSButton = new javax.swing.JButton();
        nodesLocationsCheckBox = new javax.swing.JCheckBox();
        minLocationChangeIntervalLabel = new javax.swing.JLabel();
        minLocationChangeIntervalSpinner = new javax.swing.JSpinner();
        maxLocationChangeIntervalLabel = new javax.swing.JLabel();
        maxLocationChangeIntervalSpinner = new javax.swing.JSpinner();

        setMinimumSize(new java.awt.Dimension(300, 536));
        setName(""); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(fireSCSButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.fireSCSButton.text")); // NOI18N
        fireSCSButton.setEnabled(false);
        fireSCSButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fireSCSActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(fireSCSButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(initButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.initButton.text")); // NOI18N
        initButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                initButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(initButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(fireRSButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.fireRSButton.text")); // NOI18N
        fireRSButton.setEnabled(false);
        fireRSButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fireRSActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(fireRSButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(stepLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.stepLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 10, 10);
        add(stepLabel, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(addSCButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.addSCButton.text")); // NOI18N
        addSCButton.setEnabled(false);
        addSCButton.setMaximumSize(new java.awt.Dimension(71, 23));
        addSCButton.setMinimumSize(new java.awt.Dimension(71, 23));
        addSCButton.setPreferredSize(new java.awt.Dimension(71, 23));
        addSCButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSCButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 2, 0, 0);
        add(addSCButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(removeSCButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.removeSCButton.text")); // NOI18N
        removeSCButton.setEnabled(false);
        removeSCButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSCButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 2, 0, 10);
        add(removeSCButton, gridBagConstraints);

        scScrollPane.setMaximumSize(new java.awt.Dimension(35, 100));
        scScrollPane.setMinimumSize(new java.awt.Dimension(35, 100));
        scScrollPane.setPreferredSize(new java.awt.Dimension(35, 100));

        scList.setModel(scListModel);
        scList.setEnabled(false);
        scScrollPane.setViewportView(scList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(scScrollPane, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(nextStepButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.nextStepButton.text")); // NOI18N
        nextStepButton.setEnabled(false);
        nextStepButton.setMaximumSize(new java.awt.Dimension(99, 23));
        nextStepButton.setMinimumSize(new java.awt.Dimension(99, 23));
        nextStepButton.setPreferredSize(new java.awt.Dimension(99, 23));
        nextStepButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextStepButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 10, 0);
        add(nextStepButton, gridBagConstraints);

        scComboBox.setModel(scComboBoxModel);
        scComboBox.setMaximumSize(new java.awt.Dimension(100, 18));
        scComboBox.setMinimumSize(new java.awt.Dimension(100, 18));
        scComboBox.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(scComboBox, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(previousStepButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.previousStepButton.text")); // NOI18N
        previousStepButton.setEnabled(false);
        previousStepButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousStepButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 10, 0);
        add(previousStepButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(stopButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.stopButton.text")); // NOI18N
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(stopButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(startButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.startButton.text")); // NOI18N
        startButton.setEnabled(false);
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(startButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(simcountLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.simcountLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
        add(simcountLabel, gridBagConstraints);

        simcountFormattedTextField.setFormatterFactory(getSimcountFormatterFactory());
        simcountFormattedTextField.setText(org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.simcountFormattedTextField.text")); // NOI18N
        simcountFormattedTextField.setEnabled(false);
        simcountFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                simcountFormattedTextFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(simcountFormattedTextField, gridBagConstraints);

        delayFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#"))));
        delayFormattedTextField.setText(org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.delayFormattedTextField.text")); // NOI18N
        delayFormattedTextField.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(delayFormattedTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(delayLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.delayLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
        add(delayLabel, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(granularityLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.granularityLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(granularityLabel, gridBagConstraints);

        granularityFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#.##"))));
        granularityFormattedTextField.setText(org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.granularityFormattedTextField.text")); // NOI18N
        granularityFormattedTextField.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(granularityFormattedTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(nodesQualitiesCheckBox, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.nodesQualitiesCheckBox.text")); // NOI18N
        nodesQualitiesCheckBox.setEnabled(false);
        nodesQualitiesCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(nodesQualitiesCheckBox, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(edgesActivationCheckBox, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.edgesActivationCheckBox.text")); // NOI18N
        edgesActivationCheckBox.setEnabled(false);
        edgesActivationCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        edgesActivationCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                edgesActivationCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(edgesActivationCheckBox, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(minActivatedEdgesLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.minActivatedEdgesLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(minActivatedEdgesLabel, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(maxActivatedEdgesLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.maxActivatedEdgesLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
        add(maxActivatedEdgesLabel, gridBagConstraints);

        minActivatedEdgesSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        minActivatedEdgesSpinner.setEnabled(false);
        minActivatedEdgesSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minActivatedEdgesSpinnerStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(minActivatedEdgesSpinner, gridBagConstraints);

        maxActivatedEdgesSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(100), Integer.valueOf(0), null, Integer.valueOf(1)));
        maxActivatedEdgesSpinner.setEnabled(false);
        maxActivatedEdgesSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maxActivatedEdgesSpinnerStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(maxActivatedEdgesSpinner, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(fireLCSButton, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.fireLCSButton.text")); // NOI18N
        fireLCSButton.setEnabled(false);
        fireLCSButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fireLCSActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(fireLCSButton, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(nodesLocationsCheckBox, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.nodesLocationsCheckBox.text")); // NOI18N
        nodesLocationsCheckBox.setEnabled(false);
        nodesLocationsCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        nodesLocationsCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                nodesLocationsCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(nodesLocationsCheckBox, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(minLocationChangeIntervalLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.minLocationChangeIntervalLabel.text")); // NOI18N
        minLocationChangeIntervalLabel.setToolTipText(org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.minLocationChangeIntervalLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(minLocationChangeIntervalLabel, gridBagConstraints);

        minLocationChangeIntervalSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        minLocationChangeIntervalSpinner.setEnabled(false);
        minLocationChangeIntervalSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minLocationChangeIntervalSpinnerStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(minLocationChangeIntervalSpinner, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(maxLocationChangeIntervalLabel, org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.maxLocationChangeIntervalLabel.text")); // NOI18N
        maxLocationChangeIntervalLabel.setToolTipText(org.openide.util.NbBundle.getMessage(SpreadSimulatorTopComponent.class, "SpreadSimulatorTopComponent.maxLocationChangeIntervalLabel.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
        add(maxLocationChangeIntervalLabel, gridBagConstraints);

        maxLocationChangeIntervalSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        maxLocationChangeIntervalSpinner.setEnabled(false);
        maxLocationChangeIntervalSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maxLocationChangeIntervalSpinnerStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(maxLocationChangeIntervalSpinner, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

	private void initButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_initButtonActionPerformed
		simulation.init();
	}//GEN-LAST:event_initButtonActionPerformed

	private void addSCButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSCButtonActionPerformed
		String selectedSC = (String)scComboBoxModel.getSelectedItem();
		StopCondition sc = scMap.get(selectedSC);
		StopConditionUI scui = scUIMap.get(selectedSC);
		if (selectedSC != null && !scListModel.contains(selectedSC)) {
			JPanel settingsPanel = scui.getSettingsPanel();
			scui.setup(sc);
			DialogDescriptor dd = new DialogDescriptor(settingsPanel, selectedSC);
			if (DialogDisplayer.getDefault().notify(dd).equals(NotifyDescriptor.OK_OPTION)) {
				scui.unsetup();
				scListModel.addElement(selectedSC);
				simulation.addStopCondition(sc);
			}
		}
	}//GEN-LAST:event_addSCButtonActionPerformed

	private void removeSCButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSCButtonActionPerformed
		String selectedSC = (String)scComboBoxModel.getSelectedItem();
		if (selectedSC != null && scListModel.contains(selectedSC)) {
			scListModel.removeElement(selectedSC);
			simulation.removeStopCondition(scMap.get(selectedSC));
		}
	}//GEN-LAST:event_removeSCButtonActionPerformed

	private void fireSCSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fireSCSActionPerformed
		JPanel settingsPanel = scsUI.getSettingsPanel();
		scsUI.setup(scs);
		DialogDescriptor dd = new DialogDescriptor(settingsPanel, "State Change Strategy");
		if (DialogDisplayer.getDefault().notify(dd).equals(NotifyDescriptor.OK_OPTION)) {
			scsUI.unsetup();
			scs.changeStates();
		}
	}//GEN-LAST:event_fireSCSActionPerformed

	private void fireRSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fireRSActionPerformed
		JPanel settingsPanel = rsUI.getSettingsPanel();
		rsUI.setup(rs);
		DialogDescriptor dd = new DialogDescriptor(settingsPanel, "Removal Strategy");
		if (DialogDisplayer.getDefault().notify(dd).equals(NotifyDescriptor.OK_OPTION)) {
			rsUI.unsetup();
			rs.removeNodes();
		}
	}//GEN-LAST:event_fireRSActionPerformed

    private void fireLCSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fireLCSActionPerformed
        JPanel settingsPanel = lcsUI.getSettingsPanel();
		lcsUI.setup(lcs);
		DialogDescriptor dd = new DialogDescriptor(settingsPanel, "Location Change Strategy");
		if (DialogDisplayer.getDefault().notify(dd).equals(NotifyDescriptor.OK_OPTION)) {
			lcsUI.unsetup();
			lcs.changeLocations();
		}
    }//GEN-LAST:event_fireLCSActionPerformed

    private void nodesLocationsCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_nodesLocationsCheckBoxItemStateChanged
		if (evt.getStateChange() == ItemEvent.SELECTED || evt.getStateChange() == ItemEvent.DESELECTED) {
			boolean nodesLocations = nodesLocationsCheckBox.isSelected();
			simulation.setNodesLocations(nodesLocations);
			fireLCSButton.setEnabled(simulation.isNodesLocations());
			minLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations());
			maxLocationChangeIntervalSpinner.setEnabled(simulation.isNodesLocations());
		}
    }//GEN-LAST:event_nodesLocationsCheckBoxItemStateChanged

    private void minLocationChangeIntervalSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minLocationChangeIntervalSpinnerStateChanged
        int minLocationChangeInterval = (Integer)minLocationChangeIntervalSpinner.getValue();
		int maxLocationChangeInterval = (Integer)maxLocationChangeIntervalSpinner.getValue();
		if (minLocationChangeInterval > maxLocationChangeInterval)
			minLocationChangeInterval = maxLocationChangeInterval;
		simulation.setMinLocationChangeInterval(minLocationChangeInterval);
		minLocationChangeIntervalSpinner.setValue(simulation.getMinLocationChangeInterval());
    }//GEN-LAST:event_minLocationChangeIntervalSpinnerStateChanged

    private void maxLocationChangeIntervalSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maxLocationChangeIntervalSpinnerStateChanged
        int minLocationChangeInterval = (Integer)minLocationChangeIntervalSpinner.getValue();
		int maxLocationChangeInterval = (Integer)maxLocationChangeIntervalSpinner.getValue();
		if (minLocationChangeInterval > maxLocationChangeInterval)
			maxLocationChangeInterval = minLocationChangeInterval;
		simulation.setMaxLocationChangeInterval(maxLocationChangeInterval);
		maxLocationChangeIntervalSpinner.setValue(simulation.getMaxLocationChangeInterval());
    }//GEN-LAST:event_maxLocationChangeIntervalSpinnerStateChanged

    private void edgesActivationCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_edgesActivationCheckBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED || evt.getStateChange() == ItemEvent.DESELECTED) {
			boolean edgesActivation = edgesActivationCheckBox.isSelected();
			simulation.setEdgesActivation(edgesActivation);
			minActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation());
			maxActivatedEdgesSpinner.setEnabled(simulation.isEdgesActivation());
		}
    }//GEN-LAST:event_edgesActivationCheckBoxItemStateChanged
	
	private void minActivatedEdgesSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minActivatedEdgesSpinnerStateChanged
		int minActivatedEdges = (Integer)minActivatedEdgesSpinner.getValue();
		int maxActivatedEdges = (Integer)maxActivatedEdgesSpinner.getValue();
		if (minActivatedEdges > maxActivatedEdges)
			minActivatedEdges = maxActivatedEdges;
		simulation.setMinActivatedEdges(minActivatedEdges);
		minActivatedEdgesSpinner.setValue(simulation.getMinActivatedEdges());
    }//GEN-LAST:event_minActivatedEdgesSpinnerStateChanged
	
    private void maxActivatedEdgesSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maxActivatedEdgesSpinnerStateChanged
        int minActivatedEdges = (Integer)minActivatedEdgesSpinner.getValue();
		int maxActivatedEdges = (Integer)maxActivatedEdgesSpinner.getValue();
		if (minActivatedEdges > maxActivatedEdges)
			maxActivatedEdges = minActivatedEdges;
		simulation.setMaxActivatedEdges(maxActivatedEdges);
		maxActivatedEdgesSpinner.setValue(simulation.getMaxActivatedEdges());
    }//GEN-LAST:event_maxActivatedEdgesSpinnerStateChanged
	
	private void simcountFormattedTextFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_simcountFormattedTextFieldPropertyChange
		if (evt.getPropertyName().equals("value"))
			try {
				simcount = Integer.parseInt(simcountFormattedTextField.getText());
				if (simcount > 1)
					nextStepButton.setEnabled(false);
				else nextStepButton.setEnabled(!scListModel.isEmpty() && !simulation.isFinished());
			}
			catch (Exception ex) {
				nextStepButton.setEnabled(false);
			}
	}//GEN-LAST:event_simcountFormattedTextFieldPropertyChange

	private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
		simulation.cancel();
	}//GEN-LAST:event_stopButtonActionPerformed

	private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
		addSCButton.setEnabled(false);
		removeSCButton.setEnabled(false);
		fireSCSButton.setEnabled(false);
		fireRSButton.setEnabled(false);
		fireLCSButton.setEnabled(false);
		nodesQualitiesCheckBox.setEnabled(false);
		nodesLocationsCheckBox.setEnabled(false);
		minLocationChangeIntervalSpinner.setEnabled(false);
		maxLocationChangeIntervalSpinner.setEnabled(false);
		edgesActivationCheckBox.setEnabled(false);
		minActivatedEdgesSpinner.setEnabled(false);
		maxActivatedEdgesSpinner.setEnabled(false);
		granularityFormattedTextField.setEnabled(false);
		delayFormattedTextField.setEnabled(false);
		simcountFormattedTextField.setEnabled(false);
		startButton.setEnabled(false);
		nextStepButton.setEnabled(false);

		boolean nodesQualities = nodesQualitiesCheckBox.isSelected();
		double granularity = Double.parseDouble(granularityFormattedTextField.getText());
		long delay = Long.parseLong(delayFormattedTextField.getText());
		simulation.setNodesQualities(nodesQualities);
		simulation.setGranularity(granularity);
		simulation.setDelay(delay);
		StartSimulations(simulation, simcount);
	}//GEN-LAST:event_startButtonActionPerformed

	private void previousStepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousStepButtonActionPerformed
		addSCButton.setEnabled(false);
		removeSCButton.setEnabled(false);
		fireSCSButton.setEnabled(false);
		fireRSButton.setEnabled(false);
		fireLCSButton.setEnabled(false);
		nodesQualitiesCheckBox.setEnabled(false);
		nodesLocationsCheckBox.setEnabled(false);
		minLocationChangeIntervalSpinner.setEnabled(false);
		maxLocationChangeIntervalSpinner.setEnabled(false);
		edgesActivationCheckBox.setEnabled(false);
		minActivatedEdgesSpinner.setEnabled(false);
		maxActivatedEdgesSpinner.setEnabled(false);
		granularityFormattedTextField.setEnabled(false);
		delayFormattedTextField.setEnabled(false);
		simcountFormattedTextField.setEnabled(false);
		startButton.setEnabled(false);
		previousStepButton.setEnabled(false);
		nextStepButton.setEnabled(false);
		
		simulation.previousStep();
	}//GEN-LAST:event_previousStepButtonActionPerformed

	private void nextStepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextStepButtonActionPerformed
		addSCButton.setEnabled(false);
		removeSCButton.setEnabled(false);
		fireSCSButton.setEnabled(false);
		fireRSButton.setEnabled(false);
		fireLCSButton.setEnabled(false);
		nodesQualitiesCheckBox.setEnabled(false);
		nodesLocationsCheckBox.setEnabled(false);
		minLocationChangeIntervalSpinner.setEnabled(false);
		maxLocationChangeIntervalSpinner.setEnabled(false);
		edgesActivationCheckBox.setEnabled(false);
		minActivatedEdgesSpinner.setEnabled(false);
		maxActivatedEdgesSpinner.setEnabled(false);
		granularityFormattedTextField.setEnabled(false);
		delayFormattedTextField.setEnabled(false);
		simcountFormattedTextField.setEnabled(false);
		startButton.setEnabled(false);
		nextStepButton.setEnabled(false);

		boolean nodesQualities = nodesQualitiesCheckBox.isSelected();
		double granularity = Double.parseDouble(granularityFormattedTextField.getText());
		long delay = Long.parseLong(delayFormattedTextField.getText());
		simulation.setNodesQualities(nodesQualities);
		simulation.setGranularity(granularity);
		simulation.setDelay(delay);
		simulation.nextStep();
	}//GEN-LAST:event_nextStepButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addSCButton;
    private javax.swing.JFormattedTextField delayFormattedTextField;
    private javax.swing.JLabel delayLabel;
    private javax.swing.JCheckBox edgesActivationCheckBox;
    private javax.swing.JButton fireLCSButton;
    private javax.swing.JButton fireRSButton;
    private javax.swing.JButton fireSCSButton;
    private javax.swing.JFormattedTextField granularityFormattedTextField;
    private javax.swing.JLabel granularityLabel;
    private javax.swing.JButton initButton;
    private javax.swing.JLabel maxActivatedEdgesLabel;
    private javax.swing.JSpinner maxActivatedEdgesSpinner;
    private javax.swing.JLabel maxLocationChangeIntervalLabel;
    private javax.swing.JSpinner maxLocationChangeIntervalSpinner;
    private javax.swing.JLabel minActivatedEdgesLabel;
    private javax.swing.JSpinner minActivatedEdgesSpinner;
    private javax.swing.JLabel minLocationChangeIntervalLabel;
    private javax.swing.JSpinner minLocationChangeIntervalSpinner;
    private javax.swing.JButton nextStepButton;
    private javax.swing.JCheckBox nodesLocationsCheckBox;
    private javax.swing.JCheckBox nodesQualitiesCheckBox;
    private javax.swing.JButton previousStepButton;
    private javax.swing.JButton removeSCButton;
    private javax.swing.JComboBox scComboBox;
    private javax.swing.JList scList;
    private javax.swing.JScrollPane scScrollPane;
    private javax.swing.JFormattedTextField simcountFormattedTextField;
    private javax.swing.JLabel simcountLabel;
    private javax.swing.JButton startButton;
    private javax.swing.JLabel stepLabel;
    private javax.swing.JButton stopButton;
    // End of variables declaration//GEN-END:variables
	/**
	 * Gets default instance. Do not use directly: reserved for *.settings files only,
	 * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
	 * To obtain the singleton instance, use {@link #findInstance}.
	 */
	public static synchronized SpreadSimulatorTopComponent getDefault() {
		if (instance == null) {
			instance = new SpreadSimulatorTopComponent();
		}
		return instance;
	}

	/**
	 * Obtain the SpreadSimulatorTopComponent instance. Never call {@link #getDefault} directly!
	 */
	public static synchronized SpreadSimulatorTopComponent findInstance() {
		TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
		if (win == null) {
			Logger.getLogger(SpreadSimulatorTopComponent.class.getName()).warning(
					"Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
			return getDefault();
		}
		if (win instanceof SpreadSimulatorTopComponent) {
			return (SpreadSimulatorTopComponent)win;
		}
		Logger.getLogger(SpreadSimulatorTopComponent.class.getName()).warning(
				"There seem to be multiple components with the '" + PREFERRED_ID
				+ "' ID. That is a potential source of errors and unexpected behavior.");
		return getDefault();
	}

	@Override
	public int getPersistenceType() {
		return TopComponent.PERSISTENCE_ALWAYS;
	}

	@Override
	public void componentOpened() {
		// TODO add custom code on component opening
	}

	@Override
	public void componentClosed() {
		// TODO add custom code on component closing
	}

	void writeProperties(java.util.Properties p) {
		// better to version settings since initial version as advocated at
		// http://wiki.apidesign.org/wiki/PropertyFiles
		p.setProperty("version", "1.0");
		// TODO store your settings
	}

	Object readProperties(java.util.Properties p) {
		if (instance == null) {
			instance = this;
		}
		instance.readPropertiesImpl(p);
		return instance;
	}

	private void readPropertiesImpl(java.util.Properties p) {
		String version = p.getProperty("version");
		// TODO read your settings according to their version
	}

	@Override
	protected String preferredID() {
		return PREFERRED_ID;
	}
}
