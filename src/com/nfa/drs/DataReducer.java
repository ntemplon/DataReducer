/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nfa.drs.reduction.ThermalBiasSettings;
import com.nfa.drs.reduction.ThermalBiasSettings.ThermalBiasSettingsDeserializer;
import com.nfa.drs.reduction.ThermalBiasSettings.ThermalBiasSettingsSerializer;
import com.nfa.io.FileLocations;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Nathan Templon
 */
public class DataReducer {
    
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ThermalBiasSettings.class, new ThermalBiasSettingsSerializer())
            .registerTypeAdapter(ThermalBiasSettings.class, new ThermalBiasSettingsDeserializer())
            .create();
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        }
        catch (ClassNotFoundException ex) {

        }
        catch (InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {

        }

        DataReductionFrame form = new DataReductionFrame();

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // Load Defaults
        if (Files.exists(FileLocations.DEFAULTS_FILE)) {
            try {
                String json = Files.readAllLines(FileLocations.DEFAULTS_FILE).stream()
                        .reduce("", (String first, String second) -> first + System.lineSeparator() + second);
                Defaults defaults = gson.fromJson(json, Defaults.class);
                form.setDefaults(defaults);
            }
            catch (IOException ex) {

            }
        }

        form.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                String json = gson.toJson(form.getDefaults());
                try {
                    Files.write(FileLocations.DEFAULTS_FILE, Arrays.asList(new String[]{json}), StandardOpenOption.CREATE);
                }
                catch (IOException ex) {
                    
                }
            }
        });

        form.setVisible(true);

    }

}
