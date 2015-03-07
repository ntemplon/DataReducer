/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction.thermal;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class ThermalBiasSettings {

    // Constants
    public static final String DEFAULT_FILE_NAME = "thermalbias.json";
    private static final String TIMES_KEY = "times";
    private static final String COMPUTE_THERMAL_BIAS_KEY = "compute-thermal-bias";
    private static final String THERMAL_BIAS_LINEARITY_KEY = "linearity";


    // Enumerations
    public enum ThermalBiasLinearity {

        POINT("Test Point"),
        TIME("Time");

        private final String displayName;

        ThermalBiasLinearity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
        
        public String getValueString() {
            return super.toString();
        }

        @Override
        public String toString() {
            return this.getDisplayName();
        }
    }


    // Fields
    private final Map<Integer, Double> times = new HashMap<>();
    private final transient Map<Integer, Double> timesAccess = Collections.unmodifiableMap(this.times);
    private final int pointCount;

    private boolean computeThermalBias;
    private ThermalBiasLinearity linearity;


    // Properties
    /**
     * @return the computeThermalBias
     */
    public boolean getComputeThermalBias() {
        return computeThermalBias;
    }

    /**
     * @param computeThermalBias the computeThermalBias to set
     */
    public void setComputeThermalBias(boolean computeThermalBias) {
        this.computeThermalBias = computeThermalBias;
    }

    /**
     * @return the linearity
     */
    public ThermalBiasLinearity getLinearity() {
        return linearity;
    }

    /**
     * @param linearity the linearity to set
     */
    public void setLinearity(ThermalBiasLinearity linearity) {
        this.linearity = linearity;
    }

    public double getTime(int point) {
        Double value = this.times.get(point);
        if (value != null) {
            return value;
        }
        return 0.0;
    }

    public void setTime(int point, double time) {
        this.times.put(point, time);
    }

    public Map<Integer, Double> getTimes() {
        return this.timesAccess;
    }

    public void setTimes(Map<Integer, Double> times) {
        this.times.clear();
        this.times.putAll(times);
    }
    
    public int getPointCount() {
        return this.pointCount;
    }


    // Initialization
    public ThermalBiasSettings(int pointCount) {
        this.computeThermalBias = true;
        this.pointCount = pointCount;
        this.linearity = ThermalBiasLinearity.POINT;

        for (int i = 1; i <= pointCount; i++) {
            this.times.put(i, 0.0);
        }
    }


    // Serialization Helpers
    public static class ThermalBiasSettingsSerializer implements JsonSerializer<ThermalBiasSettings> {

        @Override
        public JsonElement serialize(ThermalBiasSettings src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();

            // Times
            JsonObject times = new JsonObject();
            src.getTimes().keySet().stream()
                    .forEach((Integer key) -> {
                        times.add(key.toString(), new JsonPrimitive(src.getTime(key)));
                    });
            obj.add(TIMES_KEY, times);

            // Primitives
            obj.add(COMPUTE_THERMAL_BIAS_KEY, new JsonPrimitive(src.getComputeThermalBias()));
            obj.add(THERMAL_BIAS_LINEARITY_KEY, new JsonPrimitive(src.getLinearity().getValueString()));

            return obj;
        }

    }

    public static class ThermalBiasSettingsDeserializer implements JsonDeserializer<ThermalBiasSettings> {

        @Override
        public ThermalBiasSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject src = json.getAsJsonObject();

            JsonObject times = src.getAsJsonObject(TIMES_KEY);
            Map<Integer, Double> timeMap = times.entrySet().stream()
                    .collect(Collectors.toMap(
                                    (Entry<String, JsonElement> entry) -> new Integer(entry.getKey()),
                                    (Entry<String, JsonElement> entry) -> entry.getValue().getAsDouble()
                            ));
            
            ThermalBiasSettings tbs = new ThermalBiasSettings(timeMap.size());
            
            tbs.setTimes(timeMap);
            tbs.setComputeThermalBias(src.get(COMPUTE_THERMAL_BIAS_KEY).getAsBoolean());
            tbs.setLinearity(ThermalBiasLinearity.valueOf(src.get(THERMAL_BIAS_LINEARITY_KEY).getAsString()));
            
            return tbs;
        }

    }

}
