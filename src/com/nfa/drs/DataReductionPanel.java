/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jupiter.ganymede.property.Property;
import com.jupiter.ganymede.property.Property.PropertyChangedArgs;
import com.nfa.drs.constants.ModelConstants;
import com.nfa.drs.data.DataContainerViewer;
import com.nfa.drs.data.DataFormat;
import com.nfa.drs.data.Datapoint;
import com.nfa.drs.data.Run;
import com.nfa.drs.data.StudentWindTunnelFormat;
import com.nfa.drs.data.Test;
import com.nfa.drs.reduction.ThermalBiasSettings;
import com.nfa.drs.reduction.ThermalBiasSettings.ThermalBiasLinearity;
import com.nfa.drs.reduction.ThermalBiasTimeTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Nathan Templon
 */
public class DataReductionPanel extends javax.swing.JPanel {

    // Static Fields
    private static final Map<String, DataFormat> formats = new HashMap<>();

    static {
        DataFormat wsu3by4 = new StudentWindTunnelFormat();
        formats.put(wsu3by4.getName(), wsu3by4);
    }

    private static final FileFilter JSON_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.toString().endsWith(".json");
        }

        @Override
        public String getDescription() {
            return "Json Files (*.json)";
        }

    };


    // Fields
    private final Property<Test> test = new Property<>();
    private final Map<String, ThermalBiasSettings> thermalBiasSettings = new HashMap<>();
    private final DataContainerViewer thermalDataView = new DataContainerViewer();
    private final ThermalBiasTimeTable thermalTimeTable = new ThermalBiasTimeTable();
    private Defaults defaults = new Defaults();


    // Properties
    public final Defaults getDefaults() {
        return this.defaults;
    }

    public final void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    private ThermalBiasSettings getCurrentThermalBiasSettings() {
        if (this.thermalBiasRunCombo.getSelectedItem() != null) {
            String currentRun = this.thermalBiasRunCombo.getSelectedItem().toString();
            return this.thermalBiasSettings.get(currentRun);
        }
        return null;
    }


    /**
     * Creates new form DataReductionPanel
     */
    public DataReductionPanel() {
        this.thermalDataView.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        this.initComponents();

        this.thermalViewPane.setLayout(new BorderLayout());
        this.thermalViewPane.add(this.thermalDataView.getTableHeader(), BorderLayout.PAGE_START);
        this.thermalViewPane.add(this.thermalDataView, BorderLayout.CENTER);

        this.thermalTimingPanel.setLayout(new BorderLayout());
        this.thermalTimingPanel.add(this.thermalTimeTable.getTableHeader(), BorderLayout.PAGE_START);
        this.thermalTimingPanel.add(this.thermalTimeTable, BorderLayout.CENTER);

        // Add Items to ComboBoxes
        this.formatCombo.removeAllItems();
        formats.keySet().stream()
                .forEach((String name) -> this.formatCombo.addItem(name));

        this.thermalBiasRunCombo.removeAllItems();

        this.biasLinearityCombo.removeAllItems();
        for (ThermalBiasLinearity lin : ThermalBiasLinearity.values()) {
            this.biasLinearityCombo.addItem(lin);
        }

        // Add Listeners
        this.importButton.addActionListener(this::importButtonActionPerformed);
        this.thermalBiasRunCombo.addItemListener(this::thermalBiasItemEvent);
        this.computeThermalBiasCheckBox.addItemListener(this::computeThermalBiasItemEvent);
        this.startingPointSpinner.addChangeListener(this::startingPointChanged);
        this.endingPointSpinner.addChangeListener(this::endingPointChanged);
        this.biasLinearityCombo.addItemListener(this::biasLinearityItemEvent);
        this.importThermalButton.addActionListener(this::importBiasButtonAction);
        this.exportThermalButton.addActionListener(this::exportBiasButtonAction);

        this.thermalDataView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                DataReductionPanel.this.resizeThermalBiasView();
            }
        });
        this.thermalDataView.getModel().addTableModelListener((TableModelEvent e) -> this.resizeThermalBiasView());
        this.thermalTimeTable.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                DataReductionPanel.this.resizeThermalTimingPanel();
            }
        });
        this.thermalTimeTable.getModel().addTableModelListener((TableModelEvent e) -> {
            this.resizeThermalTimingPanel();
            this.getCurrentThermalBiasSettings().setTimes(this.thermalTimeTable.getTimes());
        });

        this.test.addPropertyChangedListener(this::resetThermalBias);
        this.test.addPropertyChangedListener((PropertyChangedArgs<Test> e) -> this.thermalBiasItemEvent(null));
    }


    // Private Methods
    private void importButtonActionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.toString().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "CSV Files (*.csv)";
            }

        });
        if (this.getDefaults() != null && this.getDefaults().getImportDirectory() != null) {
            chooser.setCurrentDirectory(new File(this.getDefaults().getImportDirectory()));
        }

        if (chooser.showDialog(this, "Import") == JFileChooser.APPROVE_OPTION) {
            Path dir = chooser.getSelectedFile().toPath();
            if (Files.isDirectory(dir)) {
                this.importDataFrom(dir);
            }
        }
    }

    private void importDataFrom(Path dir) {
        this.getDefaults().setImportDirectory(dir.toString());
        DataFormat format = formats.get(this.formatCombo.getSelectedItem().toString());
        this.test.set(format.fromDirectory(dir));
        this.test.get().getRuns().stream()
                .forEach((Run run) ->
                        run.getDatapoints().stream()
                        .forEach((point) ->
                                this.dataViewer.addData(run.getName(), point)
                        )
                );

        // Attempt to Import Model Constants
        this.modelConstantsPanel.setDefaultDirectory(dir);
        Path mcFile = dir.resolve(ModelConstants.DEFAULT_FILE_NAME);
        if (Files.exists(mcFile)) {
            this.modelConstantsPanel.importModelConstantsFile(mcFile);
        }

        // Attempt to Import Thermal Bias
        Path tbFile = dir.resolve(ThermalBiasSettings.DEFAULT_FILE_NAME);
        if (Files.exists(tbFile)) {
            this.importThermalBias(tbFile);
        }

        this.resizeThermalBiasView();
    }

    private void resetThermalBias(PropertyChangedArgs<Test> e) {
        this.thermalBiasSettings.clear();

        this.thermalBiasRunCombo.removeAllItems();
        this.test.get().getRuns().stream()
                .forEach((Run run) -> {
                    this.thermalBiasRunCombo.addItem(run.getName());
                    this.thermalBiasSettings.put(run.getName(), new ThermalBiasSettings(run.getPointCount()));
                });
    }

    private void thermalBiasItemEvent(ItemEvent e) {
        if (e == null || e.getStateChange() == ItemEvent.SELECTED) {
            if (this.test.get() != null) {
                // Load Data
                String runName = this.thermalBiasRunCombo.getSelectedItem().toString();
                Run run = this.test.get().getRun(runName);

                int maxRunNumber = 1;

                if (run != null) {
                    this.thermalDataView.clear();
                    run.getDatapoints().stream()
                            .forEach((Datapoint point) ->
                                    this.thermalDataView.addData(runName, point)
                            );
                    maxRunNumber = run.getPointCount();
                }

                ThermalBiasSettings tbs = this.getCurrentThermalBiasSettings();

                // Update Spinners
                SpinnerModel startModel = this.startingPointSpinner.getModel();
                if (startModel instanceof SpinnerNumberModel) {
                    SpinnerNumberModel startNum = (SpinnerNumberModel) startModel;
                    startNum.setMaximum(maxRunNumber);
                }
                SpinnerModel endModel = this.endingPointSpinner.getModel();
                if (endModel instanceof SpinnerNumberModel) {
                    SpinnerNumberModel endNum = (SpinnerNumberModel) endModel;
                    endNum.setMaximum(maxRunNumber);
                }

                // Set Values
                if (tbs != null) {
                    this.startingPointSpinner.setValue(tbs.getStartPoint());
                    this.endingPointSpinner.setValue(tbs.getEndPoint());
                    this.biasLinearityCombo.setSelectedItem(tbs.getLinearity());
                    this.thermalTimeTable.setTimes(tbs.getTimes());
                }

                // Update Check Box
                if (tbs != null) {
                    this.computeThermalBiasCheckBox.setSelected(tbs.getComputeThermalBias());
                }

                // Fire Faux Related Events
                this.biasLinearityItemEvent(null);

                this.revalidate();
//                this.repaint();
            }
        }
    }

    private void computeThermalBiasItemEvent(ItemEvent e) {
        if (this.computeThermalBiasCheckBox.isSelected()) {
            this.startingPointSpinner.setEnabled(true);
            this.endingPointSpinner.setEnabled(true);
            this.biasLinearityCombo.setEnabled(true);
            this.thermalTimeTable.setEnabled(this.biasLinearityCombo.getSelectedItem().equals(ThermalBiasLinearity.TIME));
        }
        else {
            this.startingPointSpinner.setValue(1);
            this.startingPointSpinner.setEnabled(false);
            this.endingPointSpinner.setValue(1);
            this.endingPointSpinner.setEnabled(false);
            this.biasLinearityCombo.setEnabled(false);
            this.thermalTimeTable.clearSelection();
            this.thermalTimeTable.setEnabled(false);
        }

        ThermalBiasSettings tbs = this.getCurrentThermalBiasSettings();
        if (tbs != null) {
            tbs.setComputeThermalBias(this.computeThermalBiasCheckBox.isSelected());
        }
    }

    private void resizeThermalBiasView() {
        this.thermalViewPane.setPreferredSize(new Dimension(
                this.thermalDataView.getPreferredSize().width,
                this.thermalDataView.getPreferredSize().height + DataReductionPanel.this.thermalDataView.getTableHeader().getHeight()
                + this.thermalViewPane.getInsets().top
        ));
    }

    private void resizeThermalTimingPanel() {
        this.thermalTimingPanel.setPreferredSize(new Dimension(
                this.thermalTimeTable.getPreferredSize().width,
                this.thermalTimeTable.getPreferredSize().height + DataReductionPanel.this.thermalTimeTable.getTableHeader().getHeight()
                + this.thermalTimingPanel.getInsets().top
        ));
    }

    private void startingPointChanged(ChangeEvent e) {
        ThermalBiasSettings tbs = this.getCurrentThermalBiasSettings();
        if (tbs != null) {
            tbs.setStartPoint((Integer) this.startingPointSpinner.getValue());
        }
    }

    private void endingPointChanged(ChangeEvent e) {
        ThermalBiasSettings tbs = this.getCurrentThermalBiasSettings();
        if (tbs != null) {
            tbs.setEndPoint((Integer) this.endingPointSpinner.getValue());
        }
    }

    private void biasLinearityItemEvent(ItemEvent e) {
        ThermalBiasSettings tbs = this.getCurrentThermalBiasSettings();

        if (tbs != null) {
            Object o = this.biasLinearityCombo.getSelectedItem();
            if (o instanceof ThermalBiasLinearity) {
                ThermalBiasLinearity lin = (ThermalBiasLinearity) o;
                tbs.setLinearity(lin);
            }
        }

        boolean inTime = this.biasLinearityCombo.getSelectedItem().equals(ThermalBiasLinearity.TIME);
        if (!inTime) {
            this.thermalTimeTable.clearSelection();
        }
        this.thermalTimeTable.setEnabled(inTime);
    }

    private void importBiasButtonAction(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(JSON_FILTER);
        if (this.getDefaults() != null && this.getDefaults().getImportDirectory() != null && Files.exists(Paths.get(this.getDefaults().getImportDirectory()))) {
            chooser.setCurrentDirectory(new File(this.getDefaults().getImportDirectory()));
        }

        Path defaultMcFile = chooser.getCurrentDirectory().toPath().resolve(ThermalBiasSettings.DEFAULT_FILE_NAME);
        if (Files.exists(defaultMcFile)) {
            chooser.setSelectedFile(defaultMcFile.toFile());
        }

        if (chooser.showDialog(this, "Import") == JFileChooser.APPROVE_OPTION) {
            Path mcFile = chooser.getSelectedFile().toPath();
            if (Files.exists(mcFile)) {
                this.importThermalBias(mcFile);
            }
        }
    }

    private void exportBiasButtonAction(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(JSON_FILTER);

        Path defaultDir = Paths.get(this.getDefaults().getImportDirectory());
        if (defaultDir != null && Files.exists(defaultDir)) {
            chooser.setCurrentDirectory(defaultDir.toFile());
        }
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), ThermalBiasSettings.DEFAULT_FILE_NAME));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();

            if (chooser.getFileFilter().equals(JSON_FILTER) && !file.toString().endsWith(".json")) {
                file = Paths.get(file.toString() + ".json");
            }

            this.exportThermalBias(file);
        }
    }

    private void importThermalBias(Path mcFile) {
        try {
            String json = Files.readAllLines(mcFile).stream()
                    .reduce("", (String first, String second) -> first + System.lineSeparator() + second);
            JsonObject map = new JsonParser().parse(json).getAsJsonObject();

            this.thermalBiasSettings.clear();
            map.entrySet().stream()
                    .forEach((Entry<String, JsonElement> entry) -> {
                        String name = entry.getKey();
                        JsonElement element = entry.getValue();
                        try {
                            ThermalBiasSettings tbs = DataReducer.GSON.fromJson(element, ThermalBiasSettings.class);
                            this.thermalBiasSettings.put(name, tbs);
                        }
                        catch (Exception ex) {
                            System.out.println(ex.getClass().getName());
                        }
                    });

            this.thermalBiasItemEvent(null);
            this.resizeThermalBiasView();
        }
        catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "There was an error while importing the thermal bias.", "Import Error", JOptionPane.ERROR_MESSAGE);
        }

        this.resizeThermalBiasView();
    }

    private void exportThermalBias(Path mcFile) {
        String json = DataReducer.GSON.toJson(this.thermalBiasSettings);

        try {
            Files.write(mcFile, Arrays.asList(new String[]{json}));
        }
        catch (IOException ex) {

        }
    }

    // Designer  
    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        dataPanel = new javax.swing.JPanel();
        inputFormatLabel = new javax.swing.JLabel();
        formatCombo = new javax.swing.JComboBox();
        dataScrollPane = new javax.swing.JScrollPane();
        dataViewer = new com.nfa.drs.data.DataContainerViewer();
        importButton = new javax.swing.JButton();
        mcConfigPanel = new javax.swing.JPanel();
        botGlue = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        modelConstantsLabel = new javax.swing.JLabel();
        modelConstantsPanel = new com.nfa.drs.constants.ModelConstantsPanel();
        thermalPanel = new javax.swing.JPanel();
        thermalRunLabel = new javax.swing.JLabel();
        thermalBiasRunCombo = new javax.swing.JComboBox();
        thermalBiasGlue = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        startingPointLabel = new javax.swing.JLabel();
        computeThermalBiasCheckBox = new javax.swing.JCheckBox();
        endingPointSpinner = new javax.swing.JSpinner();
        thermalViewPane = new javax.swing.JPanel();
        endingPointLabel = new javax.swing.JLabel();
        startEndStrut = new javax.swing.Box.Filler(new java.awt.Dimension(40, 0), new java.awt.Dimension(40, 0), new java.awt.Dimension(40, 32767));
        startingPointSpinner = new javax.swing.JSpinner();
        biasLinearityLabel = new javax.swing.JLabel();
        biasLinearityCombo = new javax.swing.JComboBox();
        thermalTimingPanel = new javax.swing.JPanel();
        buttonPanel = new javax.swing.JPanel();
        importThermalButton = new javax.swing.JButton();
        exportThermalButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        reductionPanel = new javax.swing.JPanel();
        dataTab = new javax.swing.JPanel();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setPreferredSize(new java.awt.Dimension(950, 600));
        setLayout(new java.awt.BorderLayout());

        dataPanel.setLayout(new java.awt.GridBagLayout());

        inputFormatLabel.setText("Data Format");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        dataPanel.add(inputFormatLabel, gridBagConstraints);

        formatCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        formatCombo.setPreferredSize(new java.awt.Dimension(275, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        dataPanel.add(formatCombo, gridBagConstraints);

        dataViewer.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        dataScrollPane.setViewportView(dataViewer);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        dataPanel.add(dataScrollPane, gridBagConstraints);

        importButton.setText("Import Data");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        dataPanel.add(importButton, gridBagConstraints);

        tabbedPane.addTab("Raw Data", dataPanel);

        mcConfigPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mcConfigPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 50;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mcConfigPanel.add(botGlue, gridBagConstraints);

        modelConstantsLabel.setText("Model Constants");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        mcConfigPanel.add(modelConstantsLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        mcConfigPanel.add(modelConstantsPanel, gridBagConstraints);

        tabbedPane.addTab("Model Constants", mcConfigPanel);

        thermalPanel.setLayout(new java.awt.GridBagLayout());

        thermalRunLabel.setText("Select Run");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(thermalRunLabel, gridBagConstraints);

        thermalBiasRunCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        thermalBiasRunCombo.setPreferredSize(new java.awt.Dimension(200, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(thermalBiasRunCombo, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 100;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        thermalPanel.add(thermalBiasGlue, gridBagConstraints);

        startingPointLabel.setText("Starting Point:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(startingPointLabel, gridBagConstraints);

        computeThermalBiasCheckBox.setSelected(true);
        computeThermalBiasCheckBox.setText("Compute Thermal Bias for This Run");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        thermalPanel.add(computeThermalBiasCheckBox, gridBagConstraints);

        endingPointSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 1, 1));
        endingPointSpinner.setPreferredSize(new java.awt.Dimension(75, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(endingPointSpinner, gridBagConstraints);

        thermalViewPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        thermalViewPane.setPreferredSize(new java.awt.Dimension(100, 150));

        javax.swing.GroupLayout thermalViewPaneLayout = new javax.swing.GroupLayout(thermalViewPane);
        thermalViewPane.setLayout(thermalViewPaneLayout);
        thermalViewPaneLayout.setHorizontalGroup(
            thermalViewPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 923, Short.MAX_VALUE)
        );
        thermalViewPaneLayout.setVerticalGroup(
            thermalViewPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        thermalPanel.add(thermalViewPane, gridBagConstraints);

        endingPointLabel.setText("Ending Point:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(endingPointLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        thermalPanel.add(startEndStrut, gridBagConstraints);

        startingPointSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 1, 1));
        startingPointSpinner.setPreferredSize(new java.awt.Dimension(75, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        thermalPanel.add(startingPointSpinner, gridBagConstraints);

        biasLinearityLabel.setText("Bias Linear In:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(biasLinearityLabel, gridBagConstraints);

        biasLinearityCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        biasLinearityCombo.setPreferredSize(new java.awt.Dimension(175, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        thermalPanel.add(biasLinearityCombo, gridBagConstraints);

        thermalTimingPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        thermalTimingPanel.setPreferredSize(new java.awt.Dimension(925, 150));

        javax.swing.GroupLayout thermalTimingPanelLayout = new javax.swing.GroupLayout(thermalTimingPanel);
        thermalTimingPanel.setLayout(thermalTimingPanelLayout);
        thermalTimingPanelLayout.setHorizontalGroup(
            thermalTimingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 923, Short.MAX_VALUE)
        );
        thermalTimingPanelLayout.setVerticalGroup(
            thermalTimingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        thermalPanel.add(thermalTimingPanel, gridBagConstraints);

        buttonPanel.setPreferredSize(new java.awt.Dimension(100, 25));
        buttonPanel.setLayout(new java.awt.GridBagLayout());

        importThermalButton.setText("Import");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        buttonPanel.add(importThermalButton, gridBagConstraints);

        exportThermalButton.setText("Export");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        buttonPanel.add(exportThermalButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        buttonPanel.add(filler1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        thermalPanel.add(buttonPanel, gridBagConstraints);

        tabbedPane.addTab("Thermal Bias", thermalPanel);

        javax.swing.GroupLayout reductionPanelLayout = new javax.swing.GroupLayout(reductionPanel);
        reductionPanel.setLayout(reductionPanelLayout);
        reductionPanelLayout.setHorizontalGroup(
            reductionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 935, Short.MAX_VALUE)
        );
        reductionPanelLayout.setVerticalGroup(
            reductionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 563, Short.MAX_VALUE)
        );

        tabbedPane.addTab("Flow Corrections", reductionPanel);

        javax.swing.GroupLayout dataTabLayout = new javax.swing.GroupLayout(dataTab);
        dataTab.setLayout(dataTabLayout);
        dataTabLayout.setHorizontalGroup(
            dataTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        dataTabLayout.setVerticalGroup(
            dataTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        tabbedPane.addTab("Reduced Data", dataTab);

        add(tabbedPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox biasLinearityCombo;
    private javax.swing.JLabel biasLinearityLabel;
    private javax.swing.Box.Filler botGlue;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JCheckBox computeThermalBiasCheckBox;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JScrollPane dataScrollPane;
    private javax.swing.JPanel dataTab;
    private com.nfa.drs.data.DataContainerViewer dataViewer;
    private javax.swing.JLabel endingPointLabel;
    private javax.swing.JSpinner endingPointSpinner;
    private javax.swing.JButton exportThermalButton;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JComboBox formatCombo;
    private javax.swing.JButton importButton;
    private javax.swing.JButton importThermalButton;
    private javax.swing.JLabel inputFormatLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel mcConfigPanel;
    private javax.swing.JLabel modelConstantsLabel;
    private com.nfa.drs.constants.ModelConstantsPanel modelConstantsPanel;
    private javax.swing.JPanel reductionPanel;
    private javax.swing.Box.Filler startEndStrut;
    private javax.swing.JLabel startingPointLabel;
    private javax.swing.JSpinner startingPointSpinner;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.Box.Filler thermalBiasGlue;
    private javax.swing.JComboBox thermalBiasRunCombo;
    private javax.swing.JPanel thermalPanel;
    private javax.swing.JLabel thermalRunLabel;
    private javax.swing.JPanel thermalTimingPanel;
    private javax.swing.JPanel thermalViewPane;
    // End of variables declaration//GEN-END:variables
}
