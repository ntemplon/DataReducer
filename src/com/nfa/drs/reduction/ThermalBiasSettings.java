/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nathan Templon
 */
public class ThermalBiasSettings {
    
    // Enumerations
    public enum ThermalBiasLinearity {
        POINT("Test Point"),
        TIME("Time");
        
        private final String displayName;
        
        ThermalBiasLinearity(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return this.displayName;
        }
        
        @Override
        public String toString() {
            return this.getDisplayName();
        }
    }
    
    
    // Fields
    private final Map<Integer, Double> times = new HashMap<>();
    private final Map<Integer, Double> timesAccess = Collections.unmodifiableMap(this.times);
    
    private boolean computeThermalBias;
    private int startPoint;
    private int endPoint;
    private ThermalBiasLinearity linearity;
    
    
    // Properties
    /**
     * @return the computeThermalBias
     */
    public boolean getComputeThermalBias() {
        return computeThermalBias;
    }

    /**
     * @param computeThermalBias the computeThermalBias to set
     */
    public void setComputeThermalBias(boolean computeThermalBias) {
        this.computeThermalBias = computeThermalBias;
    }

    /**
     * @return the startPoint
     */
    public int getStartPoint() {
        return startPoint;
    }

    /**
     * @param startPoint the startPoint to set
     */
    public void setStartPoint(int startPoint) {
        this.startPoint = startPoint;
    }

    /**
     * @return the endPoint
     */
    public int getEndPoint() {
        return endPoint;
    }

    /**
     * @param endPoint the endPoint to set
     */
    public void setEndPoint(int endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * @return the linearity
     */
    public ThermalBiasLinearity getLinearity() {
        return linearity;
    }

    /**
     * @param linearity the linearity to set
     */
    public void setLinearity(ThermalBiasLinearity linearity) {
        this.linearity = linearity;
    }
    
    public double getTime(int point) {
        Double value = this.times.get(point);
        if (value != null) {
            return value;
        }
        return 0.0;
    }
    
    public void setTime(int point, double time) {
        this.times.put(point, time);
    }
    
    public Map<Integer, Double> getTimes() {
        return this.timesAccess;
    }
    
    public void setTimes(Map<Integer, Double> times) {
        this.times.clear();
        this.times.putAll(times);
    }
    
    
    // Initialization
    public ThermalBiasSettings(int maxNumberOfRuns) {
        this.computeThermalBias = true;
        this.startPoint = 1;
        this.endPoint = maxNumberOfRuns;
        this.linearity = ThermalBiasLinearity.POINT;
        
        for(int i = this.startPoint; i <= this.endPoint; i++) {
            this.times.put(i, 0.0);
        }
    }
    
}
