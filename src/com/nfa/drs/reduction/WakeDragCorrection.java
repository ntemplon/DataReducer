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
public class WakeDragCorrection implements DataCorrection {

    @Override
    public DataSet getCorrection(DataSet data, ModelConstants constants) {
        final double delta = constants.get(Constants.Delta);
        final double wingArea = constants.get(Constants.WingArea);
        final double csArea = constants.get(Constants.JetHeight) * constants.get(Constants.JetWidth);
        data.coefficientsFromLoads(constants);
        final double cl = data.get(DataValues.CL);
        final double q = data.get(DataValues.DynamicPressure);
        
        final double cdw = delta * (wingArea / csArea) * (cl * cl);
        final double dw = q * wingArea * cdw;
        
        final Map<DataValues, Double> correction = new HashMap<>();
        correction.put(DataValues.Drag, dw);
        
        return new DataSet(correction);
    }

    @Override
    public String getName() {
        return "Wake Drag Correction";
    }
    
}
