/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import com.nfa.drs.constants.ModelConstants;
import com.nfa.drs.constants.ModelConstants.Constants;
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

        AngleOfAttack("Alpha (deg)", Double.class, false),
        Lift("Lift (lbf)", Double.class, true),
        Drag("Drag (lbf)", Double.class, true),
        PitchMoment("PM (ft-lbf)", Double.class, true),
        CL("CL", Double.class, false),
        CD("CD", Double.class, false),
        CPM("CPM", Double.class, false),
        DynamicPressure("Q (psf)", Double.class, false);

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
        private final boolean isLoad;


        // Properties
        public final String getDisplayName() {
            return this.displayName;
        }

        public final Class<?> getValueClass() {
            return this.valueClass;
        }

        public final boolean isLoad() {
            return this.isLoad;
        }


        // Initialization
        DataValues(String displayName, Class<?> valueClass, boolean isLoad) {
            this.displayName = displayName;
            this.valueClass = valueClass;
            this.isLoad = isLoad;
        }

    }


    // Fields
    private final Map<DataValues, Double> data;


    // Properties
    public final double get(DataValues item) {
        Double val = this.data.get(item);
        if (val != null) {
            return val;
        }
        return 0.0;
    }


    // Initialization
    public DataSet(Map<DataValues, Double> data) {
        this.data = new HashMap<>();
        this.data.putAll(data);
    }

    public DataSet() {
        this(Arrays.asList(DataValues.values()).stream()
                .collect(Collectors.toMap(
                                (DataValues values) -> values,
                                (DataValues values) -> 0.0
                        )
                )
        );
    }


    // Public Methods
    public DataSet plus(DataSet other) {
        return new DataSet(Arrays.asList(DataValues.values()).parallelStream()
                .collect(Collectors.toMap(
                                (DataValues value) -> value,
                                (DataValues value) -> this.get(value) + other.get(value)
                        ))
        );
    }

    public DataSet minus(DataSet other) {
        return new DataSet(Arrays.asList(DataValues.values()).parallelStream()
                .collect(Collectors.toMap(
                                (DataValues value) -> value,
                                (DataValues value) -> this.get(value) - other.get(value)
                        ))
        );
    }

    public void coefficientsFromLoads(ModelConstants constants) {
        if (this.data.get(DataValues.DynamicPressure) < 0.1) {
            this.data.put(DataValues.CL, 0.0);
            this.data.put(DataValues.CD, 0.0);
            this.data.put(DataValues.CPM, 0.0);
        }
        else {
            this.data.put(DataValues.CL, this.data.get(DataValues.Lift) / (this.data.get(DataValues.DynamicPressure) * constants.get(Constants.WingArea)));
            this.data.put(DataValues.CD, this.data.get(DataValues.Drag) / (this.data.get(DataValues.DynamicPressure) * constants.get(Constants.WingArea)));
            this.data.put(DataValues.CPM, this.data.get(DataValues.PitchMoment) / (this.data.get(DataValues.DynamicPressure)
                    * constants.get(Constants.WingArea) * constants.get(Constants.Chord)));
        }
    }

    public void loadsFromCoefficients(ModelConstants constants) {
        this.data.put(DataValues.Lift, this.data.get(DataValues.CL) * this.data.get(DataValues.DynamicPressure) * constants.get(Constants.WingArea));
        this.data.put(DataValues.Drag, this.data.get(DataValues.CD) * this.data.get(DataValues.DynamicPressure) * constants.get(Constants.WingArea));
        this.data.put(DataValues.PitchMoment, this.data.get(DataValues.CPM) * this.data.get(DataValues.DynamicPressure)
                * constants.get(Constants.WingArea) * constants.get(Constants.Chord));
    }

}
