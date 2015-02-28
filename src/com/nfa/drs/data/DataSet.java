/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import com.nfa.drs.constants.ModelConstants;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class DataSet {

    // Enumerations
    public enum DataValues {
        TestPoint("Test Point"),
        AngleOfAttack("Angle of Attack (deg)"),
        Lift("Lift (lbf)"),
        Drag("Drag (lbf)"),
        PitchMoment("Pitching Moment (ft-lbf)"),
        DynamicPressure("Q (psf)");
        
        private static final Map<String, DataValues> byDisplayName = new HashMap<>();
        private static final Lock bdnLock = new ReentrantLock();

        public static DataValues getByDisplayName(String displayName) {
            if (byDisplayName.isEmpty()) {
                bdnLock.lock();

                try {
                    if (byDisplayName.isEmpty()) {
                        Arrays.asList(DataValues.values()).stream()
                                .forEach((DataValues dataVAlue) -> {
                                    byDisplayName.put(dataVAlue.getDisplayName(), dataVAlue);
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
        
        public final String getDisplayName() {
            return this.displayName;
        }
        
        DataValues(String displayName) {
            this.displayName = displayName;
        }
    }


    // Fields
    private final Map<DataValues, Double> data;
    
    
    // Public Methods
    public final double get(DataValues item) {
        Double val = this.data.get(item);
        if (val != null) {
            return val;
        }
        return 0.0;
    }


    // Initialization
    public DataSet(Map<DataValues, Double> data) {
        this.data = data.keySet().stream()
                .collect(Collectors.toMap(
                        (DataValues dataVal) -> dataVal,
                        (DataValues dataVal) -> data.get(dataVal)
                ));
    }

}
