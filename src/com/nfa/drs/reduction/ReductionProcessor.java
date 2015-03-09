/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction;

import com.jupiter.ganymede.math.function.LinearFunction;
import com.jupiter.ganymede.math.regression.LinearRegressor;
import com.jupiter.ganymede.math.regression.Regressor;
import com.jupiter.ganymede.math.regression.Regressor.Point;
import com.nfa.drs.constants.ModelConstants;
import com.nfa.drs.constants.ModelConstants.Constants;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Set<String> staticTares = new HashSet<>();
    private final Set<String> dynamicTares = new HashSet<>();


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

                    // Add Static and Dynamic Tares to List
                    TareSettingsEntry tse = this.tareSettings.getSettings(run.getName());
                    String staticTare = tse.getStaticTare();
                    if (staticTare != null && !staticTare.isEmpty()) {
                        this.staticTares.add(staticTare);
                    }
                    String dynamicTare = tse.getDynamicTare();
                    if (dynamicTare != null && !dynamicTare.isEmpty()) {
                        this.dynamicTares.add(dynamicTare);
                    }
                });
    }


    // Public Methods
    public ReductionResults reduce() {
        ReductionResults results = new ReductionResults();

        this.removeThermalBias();
        this.removeStaticTares();
        this.removeDynamicTares();
        this.applyFlowCorrection(new BuoyancyCorrection());
        this.performAxisTransfer();
        this.applyFlowCorrection(new BlockageCorrection(this.getCd0()));
        this.applyFlowCorrection(new AngleOfAttackCorrection());
        this.applyFlowCorrection(new WakeDragCorrection());
        this.applyFlowCorrection(new StreamwiseCurvatureCorrection(this.getClAlpha()));

        this.currentData.keySet().stream()
                .forEach((String runName) ->
                        this.currentData.get(runName).keySet().stream()
                        .forEach((Integer testPoint) -> this.currentData.get(runName).get(testPoint).getData().coefficientsFromLoads(this.constants))
                );

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
                                        return (Integer testPoint) -> -1.0 * (first.getData().get(value) + slopes.get(value) * (testPoint - 1));
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
                                        return (Integer testPoint) -> -1.0 * (first.getData().get(value) + slopes.get(value)
                                        * (settings.getTime(testPoint) - firstTime));
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
                    this.removeStaticTare(run.getName(), this.tareSettings.getSettings(run.getName()).getStaticTare());
                });
    }

    private void removeStaticTare(String dataRun, String tareRun) {
        Map<Integer, DataContainer> tareData = this.currentData.get(tareRun);
        List<DataSet> tareSets = tareData.keySet().stream()
                .map((Integer testPoint) -> tareData.get(testPoint).getData())
                .collect(Collectors.toList());
        tareSets.sort((DataSet first, DataSet second) -> Double.compare(first.get(DataValues.AngleOfAttack), second.get(DataValues.AngleOfAttack)));
        Map<Integer, DataContainer> runData = this.currentData.get(dataRun);

        Regressor regress = new LinearRegressor();
        Map<DataValues, Function<Double, Double>> tareFunctions = Arrays.asList(DataValues.values()).stream()
                .collect(Collectors.toMap(
                                (DataValues values) -> values,
                                (DataValues values) -> {
                                    if (values.isLoad()) {
                                        final Function<Double, Double> func = regress.bestFit(
                                                tareSets.stream()
                                                .map((DataSet set) -> new Regressor.Point<Double, Double>(set.get(DataValues.AngleOfAttack), set.get(values)))
                                                .collect(Collectors.toSet()));
                                        return func;
                                    }
                                    else {
                                        return (Double value) -> 0.0;
                                    }
                                }
                        )
                );

        runData.keySet().stream()
                .forEach((Integer testPoint) -> {
                    DataSet pointData = runData.get(testPoint).getData();
                    DataSet correction = new DataSet(Arrays.asList(DataValues.values()).stream()
                            .collect(Collectors.toMap(
                                            (DataValues value) -> value,
                                            (DataValues value) -> {
                                                return -1.0 * tareFunctions.get(value).apply(pointData.get(DataValues.AngleOfAttack));
                                            }
                                    ))
                    );
                    this.applyCorrection(dataRun, testPoint, "Static Tare", correction);
                });
    }

    private void removeDynamicTares() {
        this.rawData.getRuns().stream()
                .filter((Run run) -> {
                    if (this.tareSettings == null) {
                        return false;
                    }

                    TareSettingsEntry tse = this.tareSettings.getSettings(run.getName());
                    if (tse == null) {
                        return false;
                    }

                    String dynamicTareName = tse.getDynamicTare();
                    return dynamicTareName != null && !dynamicTareName.isEmpty();
                })
                .forEach((Run run) -> {
                    this.removeDynamicTare(run.getName(), this.tareSettings.getSettings(run.getName()).getDynamicTare());
                });
    }

    private void removeDynamicTare(String dataRun, String tareRun) {
        Map<Integer, DataContainer> tareData = this.currentData.get(tareRun);
        Map<Integer, DataContainer> runData = this.currentData.get(dataRun);

        Regressor regress = new LinearRegressor();
        Map<DataValues, Function<Double, Double>> tareFunctions = Arrays.asList(DataValues.values()).stream()
                .collect(Collectors.toMap(
                                (DataValues values) -> values,
                                (DataValues values) -> {
                                    if (values.isLoad()) {
                                        return regress.bestFit(
                                                tareData.keySet().stream()
                                                .filter((Integer testPoint) -> this.isValidPoint(dataRun, testPoint))
                                                .map((Integer testPoint) -> tareData.get(testPoint).getData())
                                                .map((DataSet set) -> new Regressor.Point<Double, Double>(set.get(DataValues.AngleOfAttack), set.get(values)
                                                                / set.get(DataValues.DynamicPressure)))
                                                .collect(Collectors.toSet()));
                                    }
                                    else {
                                        return (Double value) -> 0.0;
                                    }
                                }));

        runData.keySet().stream()
                .forEach((Integer testPoint) -> {
                    DataSet pointData = runData.get(testPoint).getData();
                    DataSet correction = new DataSet(Arrays.asList(DataValues.values()).stream()
                            .collect(Collectors.toMap(
                                            (DataValues value) -> value,
                                            (DataValues value) -> {
                                                final double corr = -1.0 * tareFunctions.get(value).apply(pointData.get(DataValues.AngleOfAttack))
                                                * pointData.get(DataValues.DynamicPressure);

                                                return corr;
                                            }
                                    ))
                    );
                    this.applyCorrection(dataRun, testPoint, "Dynamic Tare", correction);
                });
    }

    private void performAxisTransfer() {
        final double xOff = this.constants.get(Constants.Xmrc);
        final double zOff = this.constants.get(Constants.Zmrc);

        this.rawData.getRuns().stream()
                .map((Run run) -> run.getName())
                .filter((String runName) -> this.isDataRun(runName))
                .forEach((String runName) -> {
                    Map<Integer, DataContainer> runData = new LinkedHashMap<>(this.currentData.get(runName));
                    runData.keySet().stream()
                    .forEach((Integer testPoint) -> {
                        final DataSet pointData = runData.get(testPoint).getData();
                        final double alpha = Math.toRadians(pointData.get(DataValues.AngleOfAttack));

                        final double xAcRc = zOff * Math.sin(alpha) + xOff * Math.cos(alpha);
                        final double zAcRc = zOff * Math.cos(alpha) - xOff * Math.sin(alpha);

                        final double lift = pointData.get(DataValues.Lift);
                        final double drag = pointData.get(DataValues.Drag);
                        final double pmChange = (drag * zAcRc) - (lift * xAcRc);

                        final Map<DataValues, Double> correction = new HashMap<>();
                        correction.put(DataValues.PitchMoment, pmChange);

                        this.applyCorrection(runName, testPoint, "Moment Transfer", new DataSet(correction));
                    });
                });
    }

    private void applyCorrection(String runName, int testPoint, String correctionName, DataSet correction) {
        this.reductionSteps.get(runName).get(testPoint).add(new DataWrapper(correctionName, new SimpleDataContainer(correction, "")));

        Map<Integer, DataContainer> currentRunData = this.currentData.get(runName);
        currentRunData.put(testPoint, new SimpleDataContainer(currentRunData.get(testPoint).getData().plus(correction), ""));
    }

    private void applyFlowCorrection(DataCorrection correction) {
        this.rawData.getRuns().stream()
                .map((Run run) -> run.getName())
                .filter((String runName) -> this.isDataRun(runName))
                .forEach((String runName) -> {
                    Map<Integer, DataContainer> runData = new LinkedHashMap<>(this.currentData.get(runName));
                    runData.keySet().stream()
                    .forEach((Integer testPoint) -> {
                        this.applyCorrection(runName, testPoint, correction.getName(), correction.getCorrection(this.currentData.get(runName).get(testPoint)
                                        .getData(), this.constants));
                    });
                });
    }

    private double getCd0() {
        return this.currentData.keySet().stream()
                .filter(this::isDataRun)
                .mapToDouble(this::getCd0)
                .average().getAsDouble();
    }

    private double getCd0(String runName) {
//        return this.currentData.get(runName).keySet().stream()
//                .filter((Integer testPoint) -> this.isValidPoint(runName, testPoint))
//                .mapToDouble((Integer testPoint) -> this.getCd0(runName, testPoint))
//                .average().getAsDouble();
        Integer lowLiftPoint = null;
        double lowestLiftMag = 0.0;
        final Map<Integer, DataContainer> runData = this.currentData.get(runName);
        for (Integer testPoint : runData.keySet()) {
            if (this.isValidPoint(runName, testPoint)) {
                final double liftMag = Math.abs(runData.get(testPoint).getData().get(DataValues.Lift));
                if (lowLiftPoint == null || liftMag < lowestLiftMag) {
                    lowestLiftMag = liftMag;
                    lowLiftPoint = testPoint;
                }
            }
        }
        return this.getCd0(runName, lowLiftPoint);
    }

    private double getCd0(String runName, Integer testPoint) {
        final DataSet data = this.currentData.get(runName).get(testPoint).getData();

        data.coefficientsFromLoads(this.constants);

        final double cd = data.get(DataValues.CD);
        final double cl = data.get(DataValues.CL);
        final double e = this.constants.get(Constants.OswaldEfficiency);
        final double ar = this.constants.get(Constants.AspectRatio);

        final double cdi = (cl * cl) / (Math.PI * e * ar);
        final double cd0 = cd - cdi;

        return cd0;
    }

    private double getClAlpha() {
        return this.currentData.keySet().stream()
                .filter(this::isDataRun)
                .mapToDouble(this::getClAlpha)
                .average().getAsDouble();
    }

    private double getClAlpha(String runName) {
        LinearRegressor regress = new LinearRegressor();

        final Map<Integer, DataContainer> runData = this.currentData.get(runName);
        Function<Double, Double> clFunction = regress.bestFit(
                runData.keySet().stream()
                .filter((Integer testPoint) -> this.isValidPoint(runName, testPoint))
                .map((Integer testPoint) -> {
                    final DataSet pointData = runData.get(testPoint).getData();
                    pointData.coefficientsFromLoads(this.constants);
                    return new Point<Double, Double>(
                            pointData.get(DataValues.AngleOfAttack),
                            pointData.get(DataValues.CL));
                })
                .collect(Collectors.toSet()));

        if (clFunction instanceof LinearFunction) {
            LinearFunction lin = (LinearFunction) clFunction;
            System.out.println(lin.getSlope());
            return lin.getSlope();
        }
        return (2.0 * Math.PI) / (180.0 / Math.PI);
    }

    private boolean isDataRun(String runName) {
        return !(this.staticTares.contains(runName) || this.dynamicTares.contains(runName));
    }

    private boolean isValidPoint(String runName, int testPoint) {
        ThermalBiasSettings tbs = this.thermalBiasSettings.get(runName);
        if (tbs != null && tbs.getComputeThermalBias()) {
            return testPoint > 1 && testPoint < this.rawData.getRun(runName).getPointCount();
        }
        return true;
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
