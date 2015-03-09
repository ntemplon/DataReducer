/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction;

import com.nfa.drs.constants.ModelConstants;
import com.nfa.drs.constants.ModelConstants.Constants;
import com.nfa.drs.data.DataSet;
import com.nfa.drs.data.DataSet.DataValues;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nathan Templon
 */
public class StreamwiseCurvatureCorrection implements DataCorrection {

    // Fields
    private final double clAlpha;
    
    
    // Initialization
    public StreamwiseCurvatureCorrection(double clAlpha) {
        this.clAlpha = clAlpha;
    }
    
    
    // Public Methods
    @Override
    public DataSet getCorrection(DataSet data, ModelConstants constants) {
        final double tau2 = constants.get(Constants.Tau2);
        final double delta = constants.get(Constants.Delta);
        final double csArea = constants.get(Constants.JetWidth) * constants.get(Constants.JetHeight);
        final double wingArea = constants.get(Constants.WingArea);
        final double chord = constants.get(Constants.Chord);
        data.coefficientsFromLoads(constants);
        final double cl = data.get(DataValues.CL);
        final double q = data.get(DataValues.DynamicPressure);
        
        final double deltaAlphaSC = tau2 * delta * (wingArea / csArea) * cl;
        final double deltaClSC = -1.0 * deltaAlphaSC * this.clAlpha;
        final double deltaCpmSC = -0.25 * deltaClSC;
        
        final double deltaLift = deltaClSC * q * wingArea;
        final double deltaPitchMoment = deltaCpmSC * q * wingArea * chord;
        
        final Map<DataValues, Double> correction = new HashMap<>();
        correction.put(DataValues.Lift, deltaLift);
        correction.put(DataValues.PitchMoment, deltaPitchMoment);
        
        return new DataSet(correction);
    }

    @Override
    public String getName() {
        return "Streamwise Curvature";
    }
    
}
