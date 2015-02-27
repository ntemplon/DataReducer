/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.constants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nfa.drs.constants.ModelConstants.Constants;
import com.nfa.gui.JNumberTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Nathan Templon
 */
public class ModelConstantsPanel extends JPanel {

    // Constants
    public static final int BORDER_WIDTH = 5;
    public static final int ENTRY_BORDER_WIDTH = 4;

    private static final FileFilter JSON_FILTER = new FileFilter() {
        @Override
        public String getDescription() {
            return "Json Files (*.json)";
        }

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || file.getAbsoluteFile().toString().endsWith(".json");
        }
    };


    // Fields
    private final Map<Constants, JNumberTextField> constantBoxes = new HashMap<>();

    private JButton importButton;
    private JButton exportButton;


    // Properties
    public ModelConstants getModelConstants() {
        ModelConstants mcs = new ModelConstants();

        this.constantBoxes.keySet().stream()
                .forEach((Constants constant) -> {
                    Double value = null;
                    try {
                        value = this.constantBoxes.get(constant).getDouble();
                        if (value == null) {
                            value = 0.0;
                        }
                    }
                    catch (Exception ex) {
                        value = 0.0;
                    }
                    mcs.setConstant(constant, value);
                });

        return mcs;
    }

    public void setModelConstants(ModelConstants constants) {
        Arrays.asList(Constants.values()).stream()
                .forEach((Constants constant) -> {
                    JNumberTextField field = this.constantBoxes.get(constant);
                    if (field != null) {
                        double value = constants.getConstant(constant);
                        field.setNumber(value);
                    }
                });
    }


    // Initialization
    public ModelConstantsPanel() {

        this.initComponents();

    }


    // Private Methods
    private void initComponents() {
        this.setLayout(new GridBagLayout());

        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(BORDER_WIDTH / 2, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH)));

        int row = 0;
        int col = 0;
        for (Constants mc : Constants.values()) {
            JLabel label = new JLabel(mc.getDisplayName());
            label.setToolTipText(mc.getDescription());

            GridBagConstraints cLabel = new GridBagConstraints();
            cLabel.gridx = col;
            cLabel.gridy = row;
            cLabel.anchor = GridBagConstraints.FIRST_LINE_START;
            cLabel.insets = new Insets(ENTRY_BORDER_WIDTH, col * ENTRY_BORDER_WIDTH, 0, (1 - col) * ENTRY_BORDER_WIDTH);
            cLabel.fill = GridBagConstraints.HORIZONTAL;

            this.add(label, cLabel);

            JNumberTextField textField = new JNumberTextField();
            textField.setToolTipText(mc.getDescription());
            textField.setFormat(JNumberTextField.DECIMAL);
            textField.setNumber(0.0);

            GridBagConstraints cText = new GridBagConstraints();
            cText.gridx = col;
            cText.gridy = row + 1;
            cText.anchor = GridBagConstraints.FIRST_LINE_START;
            cText.insets = new Insets(ENTRY_BORDER_WIDTH, col * ENTRY_BORDER_WIDTH, 0, (1 - col) * ENTRY_BORDER_WIDTH);
            cText.fill = GridBagConstraints.HORIZONTAL;

            this.add(textField, cText);
            this.constantBoxes.put(mc, textField);

            // Increment for the next entry
            row += 2 * col;
            col = (col + 1) % 2;
        }

        this.importButton = new JButton("Import");
        this.exportButton = new JButton("Export");

        this.importButton.addActionListener(this::onImportClick);
        this.exportButton.addActionListener(this::onExportClick);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.0;
        buttonPanel.add(Box.createHorizontalGlue(), c);
        buttonPanel.add(this.importButton);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(this.exportButton);

        GridBagConstraints cBtn = new GridBagConstraints();
        cBtn.gridx = 0;
        cBtn.gridy = row + 1;
        cBtn.weightx = 1.0;
        cBtn.gridwidth = GridBagConstraints.REMAINDER;
        cBtn.fill = GridBagConstraints.HORIZONTAL;
        cBtn.insets = new Insets(ENTRY_BORDER_WIDTH, 0, 0, 0);
        this.add(buttonPanel, cBtn);

        GridBagConstraints cGlue = new GridBagConstraints();
        cGlue.gridx = 0;
        cGlue.gridy = row + 2;
        cGlue.weighty = 1.0;
        cGlue.gridwidth = GridBagConstraints.REMAINDER;
        this.add(Box.createVerticalGlue(), cGlue);
    }

    private void onImportClick(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(JSON_FILTER);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();

            try {
                Gson gson = new Gson();
                String json = Files.readAllLines(file).stream()
                        .reduce("", (String first, String second) -> first + System.lineSeparator() + second);
                ModelConstants mcs = gson.fromJson(json, ModelConstants.class);

                if (mcs != null) {
                    this.setModelConstants(mcs);
                }
            }
            catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "There was an error while importing the model constants.", "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onExportClick(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(JSON_FILTER);

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();

            if (chooser.getFileFilter().equals(JSON_FILTER) && !file.toString().endsWith(".json")) {
                file = Paths.get(file.toString() + ".json");
            }

            ModelConstants mcs = this.getModelConstants();
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            String json = gson.toJson(mcs);

            try {
                Files.write(file, Arrays.asList(new String[]{json}), StandardOpenOption.CREATE);
            }
            catch (IOException ex) {

            }
        }
    }

}
