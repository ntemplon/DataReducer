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
public class AngleOfAttackCorrection implements DataCorrection {

    @Override
    public DataSet getCorrection(DataSet data, ModelConstants constants) {
        final double delta = constants.get(Constants.Delta);
        final double csArea = constants.get(Constants.JetHeight) * constants.get(Constants.JetWidth);
        final double wingArea = constants.get(Constants.WingArea);
        data.coefficientsFromLoads(constants);
        final double cl = data.get(DataValues.CL);
        
        final double deltaAlpha = Math.toDegrees(((delta * wingArea) / csArea) * cl);
        
        final Map<DataValues, Double> correction = new HashMap<>();
        correction.put(DataValues.AngleOfAttack, deltaAlpha);
        
        return new DataSet(correction);
    }

    @Override
    public String getName() {
        return "Angle of Attack Correction";
    }
    
}
