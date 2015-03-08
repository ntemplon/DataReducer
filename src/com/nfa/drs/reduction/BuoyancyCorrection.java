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
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class BuoyancyCorrection implements DataCorrection {

    @Override
    public String getName() {
        return "Buoyancy";
    }
    
    @Override
    public DataSet getCorrection(DataSet data, ModelConstants constants) {
        final double density = constants.getConstant(Constants.Density);
        final double velocity = Math.sqrt((2 * data.get(DataValues.DynamicPressure)) / density);
        final double gradient = -1.0 * constants.getConstant(Constants.k) * ((density * velocity * velocity) / (2 * constants.getConstant(Constants.JetWidth)));

        final double buoyancyCorrection = gradient * constants.getConstant(Constants.Volume);

        return new DataSet(Arrays.asList(DataValues.values()).stream()
                .collect(Collectors.toMap(
                                (DataValues values) -> values,
                                (DataValues values) -> {
                                    if (values.equals(DataValues.Drag)) {
                                        return buoyancyCorrection;
                                    }
                                    else {
                                        return 0.0;
                                    }
                                }
                        )));
    }

}
