/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Nathan Templon
 */
public class ModelConstants {
    
    // Constants
    public static final String DEFAULT_FILE_NAME = "modelconstants.json";
    

    // Enumerations
    public enum Constants {

        WingArea("Wing Area (sq ft)", "The reference area for coefficients."),
        FrontalArea("Frontal Area (sq ft)", "The frontal area of the vehicle seen by the flow."),
        Chord("Wing Chord (ft)", "The reference length for coefficients."),
        Volume("Volume (ft^3)", "The total model volume."),
        JetWidth("Jet Width (ft)", "The width of the jet in which the test was conducted."),
        JetHeight("Jet Height (ft)", "The height of the jet in wich the test was conducted."),
        Span("Wing Span (ft)", "The span of the wing."),
        KTau("K\u03C4", "The product of the K and \u03C4 constants from Rae and Pope for solid blockage."),
        Delta("\u03B4", "A boundary correction factor for a closed test section. See Rae, Pope, and Barlow (3rd ed) figure 10.36 or 2nd ed page 385."),
        AspectRatio("Aspect Ratio", "The aspect ratio of the main wing."),
        Xmrc("Xmrc (ft)", "The distance from the balance resolution center to the aircraft cg along the x axis (positive upstream)."),
        Zmrc("Zmrc (ft)", "The distance from the balance resolution center to the aircraft cg along the z axis (positive down)."),
        k("k", "A factor from Rae, Pope, and Barlow for horizontal buoyancy."),
        Tau2("\u03C42", "Tail parameter for pitching moment coefficient correction.  See Rae and Pope (2nd ed) section 6.21."),
        Density("Density (slugs/ft^3)", "Local density."),
        OswaldEfficiency("Oswald Efficiency (e)", "Span efficiency factor.");

        private static final Map<String, Constants> byDisplayName = new HashMap<>();
        private static final Lock bdnLock = new ReentrantLock();

        public static Constants getByDisplayName(String displayName) {
            if (byDisplayName.isEmpty()) {
                bdnLock.lock();

                try {
                    if (byDisplayName.isEmpty()) {
                        Arrays.asList(Constants.values()).stream()
                                .forEach((Constants constant) -> {
                                    byDisplayName.put(constant.getDisplayName(), constant);
                                });
                    }
                }
                finally {
                    bdnLock.unlock();
                }
            }

            return byDisplayName.get(displayName);
        }

        private final String displayName;
        private final String description;

        public final String getDisplayName() {
            return this.displayName;
        }

        public final String getDescription() {
            return this.description;
        }

        Constants(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }


    // Fields
    private final Map<Constants, Double> constants = new HashMap<>();


    // Properties
    public Double get(Constants constant) {
        Double val = this.constants.get(constant);
        if (val != null) {
            return val;
        }
        return 0.0;
    }

    public void setConstant(Constants constant, Double value) {
        this.constants.put(constant, value);
    }


    // Public Methods
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof ModelConstants) {
            ModelConstants other = (ModelConstants) o;
            for (Constants constant : Constants.values()) {
                Double myVal = this.get(constant);
                Double otherVal = other.get(constant);
                if (myVal == null && otherVal == null) {

                }
                else if (myVal != null) {
                    if (!myVal.equals(otherVal)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.constants);
        return hash;
    }

}
