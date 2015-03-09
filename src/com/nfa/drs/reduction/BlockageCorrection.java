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
public class BlockageCorrection implements DataCorrection {

    // Fields
    private final double cd0;
    
    
    // Initialization
    public BlockageCorrection(double cd0) {
        this.cd0 = cd0;
    }
    
    
    // Public Methods
    @Override
    public DataSet getCorrection(DataSet data, ModelConstants constants) {
        final double wingArea = constants.get(Constants.WingArea);
        final double csArea = constants.get(Constants.JetWidth) * constants.get(Constants.JetHeight);
        final double ktau = constants.get(Constants.KTau);
        final double volume = constants.get(Constants.Volume);
        final double q0 = data.get(DataValues.DynamicPressure);
        
        final double esb = (ktau * volume) / Math.pow(csArea, 1.5);
        final double ewb = (wingArea * cd0) / (4.0 * csArea);
        final double et = esb + ewb;
        
        final double qc = (1 + et) * (1 + et) * q0;
        final double deltaQ = qc - q0;
        
        final Map<DataValues, Double> correction = new HashMap<>();
        correction.put(DataValues.DynamicPressure, deltaQ);
        
        return new DataSet(correction);
    }

    @Override
    public String getName() {
        return "Blockage Correction";
    }
    
}
