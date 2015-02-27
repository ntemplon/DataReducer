/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.constants;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nathan Templon
 */
public class ModelConstants {
    
    // Enumerations
    public enum Constants {
        WingArea("Wing Area (sq ft)", "The reference area for coefficients."),
        FrontalArea("Frontal Area (sq ft)", "The frontal area of the vehicle seen by the flow."),
        Chord("Wing Chord (ft)", "The reference length for coefficients."),
        BodyThickness("Body Thickness (ft)", "The maximum thickness of the body, in it's own body axis."),
        JetWidth("Jet Width (ft)", "The width of the jet in which the test was conducted."),
        JetHeight("Jet Height (ft)", "The height of the jet in wich the test was conducted."),
        Span("Wing Span (ft)", "The span of the wing."),
        Delta("\u03B4", "A boundary correction factor for a closed test section. See Rae, Pope, and Barlow (3rd ed) figure 10.36."),
        AspectRatio("Aspect Ratio", "The aspect ratio of the main wing."),
        Xmrc("Xmrc (ft)", "The distance from the balance resolution center to the aircraft cg along the x axis (positive upstream)."),
        Ymrc("Ymrc (ft)", "The distance from the balance resolution center to the aircraft cg along the y axis (positive down)."),
        Lambda2("\u03BB2 Shape Factor", "A shape factor.  See Rae, Pope, and Barlow (3rd ed) figure 9.16."),
        Lambda3("\u03BB3 SShape Factor", "A shape factor.  See Rae, Pope, and Barlow (2nd ed) figure 6.12."),
        k("k Constant", "A factor from Rae, Pope, and Barlow.");
        
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
    public Double getConstant(Constants constant) {
        Double val = this.constants.get(constant);
        if (val != null) {
            return val;
        }
        return 0.0;
    }
    
    public void setConstant(Constants constant, Double value) {
        this.constants.put(constant, value);
    }
    
}
