/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction;

import com.nfa.drs.constants.ModelConstants;
import com.nfa.drs.data.DataContainer;
import com.nfa.drs.data.DataSet;
import com.nfa.drs.data.DataSet.DataValues;
import com.nfa.drs.data.DataWrapper;
import com.nfa.drs.data.Datapoint;
import com.nfa.drs.data.Run;
import com.nfa.drs.data.SimpleDataContainer;
import com.nfa.drs.data.Test;
import com.nfa.drs.reduction.tare.TareSettings;
import com.nfa.drs.reduction.tare.TareSettings.TareSettingsEntry;
import com.nfa.drs.reduction.thermal.ThermalBiasSettings;
import com.nfa.drs.reduction.thermal.ThermalBiasSettings.ThermalBiasLinearity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class ReductionProcessor {

    // Fields
    private final Test rawData;
    private final ModelConstants constants;
    private final TareSettings tareSettings;
    private final Map<String, ThermalBiasSettings> thermalBiasSettings;
    private final Map<String, Map<Integer, List<DataWrapper>>> reductionSteps = new LinkedHashMap<>();
    private final Map<String, Map<Integer, DataContainer>> currentData = new LinkedHashMap<>();


    // Initialization
    public ReductionProcessor(Test rawData, ModelConstants constants, TareSettings tareSettings, Map<String, ThermalBiasSettings> thermalBiasSettings) {
        this.rawData = rawData;
        this.constants = constants;
        this.tareSettings = tareSettings;
        this.thermalBiasSettings = thermalBiasSettings;

        this.rawData.getRuns().stream()
                .forEach((Run run) -> {
                    // Reduction Steps Container
                    Map<Integer, List<DataWrapper>> steps = run.getDatapoints().stream()
                    .collect(Collectors.toMap(
                                    (Datapoint point) -> point.getPointNumber(),
                                    (Datapoint point) -> new ArrayList<>()));
                    run.getDatapoints().stream()
                    .forEach((Datapoint point) -> steps.get(point.getPointNumber()).add(new DataWrapper("Raw Data", point)));
                    this.reductionSteps.put(run.getName(), steps);

                    // Current Data = Raw Data
                    Map<Integer, DataContainer> data = run.getDatapoints().stream()
                    .collect(Collectors.toMap(
                                    (Datapoint point) -> point.getPointNumber(),
                                    (Datapoint point) -> point));
                    this.currentData.put(run.getName(), data);
                });
    }


    // Public Methods
    public ReductionResults reduce() {
        ReductionResults results = new ReductionResults();

        this.removeThermalBias();
        this.removeStaticTares();
        // Dynamic Tare
        // Flow Field Corrections
        // Assemble Corrected Data

        Test corrected = new Test(this.currentData.keySet().stream()
                .map((String key) -> new Run(key,
                                this.currentData.get(key).keySet().stream()
                                .map((Integer testPoint) -> new Datapoint(testPoint, this.currentData.get(key).get(testPoint).getData()))
                                .collect(Collectors.toList())
                        ))
                .collect(Collectors.toList())
        );
        results.setReducedData(corrected);

        // Assemble Reduction Steps
        this.currentData.keySet().stream()
                .forEach((String runName) -> {
                    Map<Integer, DataContainer> pointMap = this.currentData.get(runName);
                    pointMap.keySet().stream()
                    .forEach((Integer testPoint) -> {
                        this.reductionSteps.get(runName).get(testPoint).add(new DataWrapper("Final Data", pointMap.get(testPoint)));
                    });
                });
        results.setAllReductionSteps(this.reductionSteps);

        return results;
    }


    // Private Methods
    private void removeThermalBias() {
        this.rawData.getRuns().stream()
                .filter((Run run) -> this.thermalBiasSettings.get(run.getName()) != null)
                .filter((Run run) -> this.thermalBiasSettings.get(run.getName()).getComputeThermalBias())
                .forEach((Run run) -> {
                    ThermalBiasLinearity linearity = this.thermalBiasSettings.get(run.getName()).getLinearity();

                    if (linearity == ThermalBiasSettings.ThermalBiasLinearity.POINT) {
                        this.removeThermalBiasPointLinear(run);
                    }
                    else if (linearity == ThermalBiasSettings.ThermalBiasLinearity.TIME) {
                        this.removeThermalBiasTimeLinear(run);
                    }
                });
    }

    private void removeThermalBiasPointLinear(Run run) {
        if (run == null) {
            return;
        }

        Map<Integer, DataContainer> points = this.currentData.get(run.getName());
        if (points == null) {
            return;
        }

        DataContainer first = points.get(1);
        DataContainer last = points.get(run.getPointCount());

        Map<DataValues, Double> slopes = Arrays.asList(DataValues.values()).stream()
                .collect(Collectors.toMap(
                                (DataValues value) -> value,
                                (DataValues value) -> (last.getData().get(value) - first.getData().get(value)) / (run.getPointCount() - 1.0)
                        ));

        Map<DataValues, Function<Integer, Double>> formulae = slopes.keySet().stream()
                .collect(Collectors.toMap(
                                (DataValues value) -> value,
                                (DataValues value) -> {
                                    if (value.isLoad()) {
                                        return (Integer testPoint) -> -1.0 * (slopes.get(value) * (testPoint - 1));
                                    }
                                    else {
                                        return (Integer testPoint) -> 0.0;
                                    }
                                }
                        ));

        Map<Integer, DataSet> corrections = new HashMap<>();
        for (int testPoint = 1; testPoint <= run.getPointCount(); testPoint++) {
            Map<DataValues, Double> pointChanges = new HashMap<>();
            for (DataValues value : DataValues.values()) {
                pointChanges.put(value, formulae.get(value).apply(testPoint));
            }
            corrections.put(testPoint, new DataSet(pointChanges));
        }

        // Put them into the list of reduction steps
        corrections.keySet().stream()
                .forEach((Integer testPoint) -> {
                    this.applyCorrection(run.getName(), testPoint, "Thermal Bias", corrections.get(testPoint));
                });

        // Update Current Data
        // COPY TO APPLY CORRECTION
        this.currentData.put(run.getName(), corrections.keySet().stream()
                .collect(Collectors.toMap(
                                (Integer testPoint) -> testPoint,
                                (Integer testPoint) -> new SimpleDataContainer(points.get(testPoint).getData().plus(corrections.get(testPoint)), "")))
        );
    }

    private void removeThermalBiasTimeLinear(Run run) {
        if (run == null) {
            return;
        }

        Map<Integer, DataContainer> points = this.currentData.get(run.getName());
        if (points == null) {
            return;
        }

        DataContainer first = points.get(1);
        DataContainer last = points.get(run.getPointCount());

        ThermalBiasSettings settings = this.thermalBiasSettings.get(run.getName());
        if (settings == null) {
            return;
        }

        final double firstTime = settings.getTime(1);
        final double lastTime = settings.getTime(run.getPointCount());

        Map<DataValues, Double> slopes = Arrays.asList(DataValues.values()).stream()
                .collect(Collectors.toMap(
                                (DataValues value) -> value,
                                (DataValues value) -> (last.getData().get(value) - first.getData().get(value)) / (lastTime - firstTime)
                        ));

        Map<DataValues, Function<Integer, Double>> formulae = slopes.keySet().stream()
                .collect(Collectors.toMap(
                                (DataValues value) -> value,
                                (DataValues value) -> {
                                    if (value.isLoad()) {
                                        return (Integer testPoint) -> -1.0 * (slopes.get(value) * (settings.getTime(testPoint) - firstTime));
                                    }
                                    else {
                                        return (Integer testPoint) -> 0.0;
                                    }
                                }
                        ));

        Map<Integer, DataSet> corrections = new HashMap<>();
        for (int testPoint = 1; testPoint <= run.getPointCount(); testPoint++) {
            Map<DataValues, Double> pointChanges = new HashMap<>();
            for (DataValues value : DataValues.values()) {
                pointChanges.put(value, formulae.get(value).apply(testPoint));
            }
            corrections.put(testPoint, new DataSet(pointChanges));
        }

        // Put them into the list of reduction steps
        corrections.keySet().stream()
                .forEach((Integer testPoint) -> {
                    this.reductionSteps.get(run.getName()).get(testPoint).add(
                            new DataWrapper("Thermal Bias", new SimpleDataContainer(corrections.get(testPoint), "")));
                });

        // Update Current Data
        this.currentData.put(run.getName(), corrections.keySet().stream()
                .collect(Collectors.toMap(
                                (Integer testPoint) -> testPoint,
                                (Integer testPoint) -> new SimpleDataContainer(points.get(testPoint).getData().plus(corrections.get(testPoint)), "")))
        );
    }

    private void removeStaticTares() {
        this.rawData.getRuns().stream()
                .filter((Run run) -> {
                    if (this.tareSettings == null) {
                        return false;
                    }

                    TareSettingsEntry tse = this.tareSettings.getSettings(run.getName());
                    if (tse == null) {
                        return false;
                    }

                    String staticTareName = tse.getStaticTare();
                    return staticTareName != null && !staticTareName.isEmpty();
                })
                .forEach((Run run) -> {
                    this.removeTare(run.getName(), this.tareSettings.getSettings(run.getName()).getStaticTare(), "Static Tare");
                });
    }

    private void removeTare(String dataRun, String tareRun, String stageName) {
        Map<Integer, DataContainer> tareData = this.currentData.get(tareRun);
        List<DataSet> tareSets = tareData.keySet().stream()
                .map((Integer testPoint) -> tareData.get(testPoint).getData())
                .collect(Collectors.toList());
        tareSets.sort((DataSet first, DataSet second) -> Double.compare(first.get(DataValues.AngleOfAttack), second.get(DataValues.AngleOfAttack)));

        Map<Integer, DataContainer> runData = this.currentData.get(dataRun);
        runData.keySet().stream()
                .forEach((Integer testPoint) -> {
                    DataSet pointSets = runData.get(testPoint).getData();
                    double alpha = pointSets.get(DataValues.AngleOfAttack);
                    
                    int lowestIndex = this.findLowerAngleOfAtack(tareSets, alpha);
                    int interpLower = 0;
                    int interpUpper = 1;
                    if (lowestIndex >= runData.size()) {
                        interpLower = runData.size() - 2;
                        interpUpper = runData.size() - 1;
                    }
                    else if (lowestIndex >= 0) {
                        interpLower = lowestIndex;
                        interpUpper = lowestIndex + 1;
                    }
                    
                    DataSet lowerData = tareSets.get(interpLower);
                    DataSet upperData = tareSets.get(interpUpper);
                    double lowerAlpha = lowerData.get(DataValues.AngleOfAttack);
                    double upperAlpha = upperData.get(DataValues.AngleOfAttack);
                    final double interpFraction = alpha / (upperAlpha - lowerAlpha);
                    
                    DataSet correction = new DataSet(Arrays.asList(DataValues.values()).stream()
                            .collect(Collectors.toMap(
                                    (DataValues value) -> value,
                                    (DataValues value) -> {
                                        if (value.isLoad()) {
                                            double upper = upperData.get(value);
                                            double lower = lowerData.get(value);
                                            double delta = (upper - lower) * interpFraction;
                                            return -1.0 * (lower + delta);
                                        }
                                        else {
                                            return 0.0;
                                        }
                                    }
                            ))
                    );
                    this.applyCorrection(dataRun, testPoint, stageName, correction);
                });
    }
    
    private void applyCorrection(String runName, int testPoint, String correctionName, DataSet correction) {
        this.reductionSteps.get(runName).get(testPoint).add(new DataWrapper(correctionName, new SimpleDataContainer(correction, "")));
    }
    
    private int findLowerAngleOfAtack(List<DataSet> searchList, double alpha) {
        for (int i = 0; i < searchList.size(); i++) {
            if (searchList.get(i).get(DataValues.AngleOfAttack) < alpha) {
                return i;
            }
        }
        return -1;
    }


    // Nested Classes
    public static class ReductionResults {

        // Fields
        private Test reducedData;
        private final Map<String, Map<Integer, List<DataWrapper>>> reductionSteps = new LinkedHashMap<>();
        private final Map<String, Map<Integer, List<DataWrapper>>> reductionStepsAccess = Collections.unmodifiableMap(this.reductionSteps);


        // Properties
        public final Test getReducedData() {
            return this.reducedData;
        }

        private void setReducedData(Test reducedData) {
            this.reducedData = reducedData;
        }

        public final Map<String, Map<Integer, List<DataWrapper>>> getReductionSteps() {
            return this.reductionStepsAccess;
        }

        private void setReductionSteps(String runName, Integer point, List<DataWrapper> steps) {
            if (runName != null && point != null && steps != null) {
                Map<Integer, List<DataWrapper>> runMap = this.reductionSteps.get(runName);
                if (runMap == null) {
                    runMap = new HashMap<>();
                    this.reductionSteps.put(runName, runMap);
                }
                runMap.put(point, steps);
            }
        }

        private void setAllReductionSteps(Map<String, Map<Integer, List<DataWrapper>>> steps) {
            this.reductionSteps.clear();
            this.reductionSteps.putAll(steps);
        }


        // Initialization
        public ReductionResults() {

        }

    }

}
