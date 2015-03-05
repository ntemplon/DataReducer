/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import static com.nfa.drs.data.DataSet.DataValues.TestPoint;
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
        TestPoint("Test Point", Integer.class),
        AngleOfAttack("Angle of Attack (deg)", Double.class),
        Lift("Lift (lbf)", Double.class),
        Drag("Drag (lbf)", Double.class),
        PitchMoment("Pitching Moment (ft-lbf)", Double.class),
        DynamicPressure("Q (psf)", Double.class);
        
        // Static Fields
        private static final Map<String, DataValues> byDisplayName = new HashMap<>();
        private static final Lock bdnLock = new ReentrantLock();

        
        // Static Methods
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

        
        // Fields
        private final String displayName;
        private final Class<?> valueClass;
        
        
        // Properties
        public final String getDisplayName() {
            return this.displayName;
        }
        
        public final Class<?> getValueClass() {
            return this.valueClass;
        }
        
        
        // Initialization
        DataValues(String displayName, Class<?> valueClass) {
            this.displayName = displayName;
            this.valueClass = valueClass;
        }
    }


    // Fields
    private final Map<DataValues, Double> data;
    
    
    // Properties
    public final void setPointNumber(int pointNumber) {
        this.data.put(TestPoint, new Double(pointNumber));
    }
    
    
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
