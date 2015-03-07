/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction.tare;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nfa.drs.DataReducer;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Nathan Templon
 */
public class TareSettings {
    
    // Constants
    public static final String DEFAULT_TARE_SETTINGS_FILE = "tares.json";
    
    
    // Fields
    private final Map<String, TareSettingsEntry> settings = new HashMap<>();
    private final transient Map<String, TareSettingsEntry> settingsAcces = Collections.unmodifiableMap(this.settings);
    
    
    // Properties
    public Map<String, TareSettingsEntry> getAllSettings() {
        return this.settingsAcces;
    }
    
    public void setAllSettings(Map<String, TareSettingsEntry> settings) {
        this.settings.clear();
        this.settings.putAll(settings);
    }
    
    public TareSettingsEntry getSettings(String runName) {
        return this.settings.get(runName);
    }
    
    public void setSettings(String runName, TareSettingsEntry entry) {
        this.settings.put(runName, entry);
    }
    
    
    // Initialization
    public TareSettings() {
        
    }
    
    
    // Nested Classes
    public static class TareSettingsEntry {
        
        // Fields
        private String staticTare;
        private String dynamicTare;
        
        
        // Properties
        public String getStaticTare() {
            return this.staticTare;
        }
        
        public void setStaticTare(String staticTare) {
            this.staticTare = staticTare;
        }
        
        public String getDynamicTare() {
            return this.dynamicTare;
        }
        
        public void setDynamicTare(String dynamicTare) {
            this.dynamicTare = dynamicTare;
        }
        
        
        // Initialization
        public TareSettingsEntry(String staticTare, String dynamicTare) {
            this.staticTare = staticTare;
            this.dynamicTare = dynamicTare;
        }
        
        public TareSettingsEntry() {
            this("", "");
        }
        
    }
    
    
    public static class TareSettingsSerializer implements JsonSerializer<TareSettings> {

        @Override
        public JsonElement serialize(TareSettings src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject root = new JsonObject();
            
            src.getAllSettings().keySet().stream()
                    .forEach((String runName) -> root.add(runName, DataReducer.GSON.toJsonTree(src.getSettings(runName))));
            
            return root;
        }
        
    }
    
    
    public static class TareSettingsDeserializer implements JsonDeserializer<TareSettings> {

        @Override
        public TareSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject root = json.getAsJsonObject();
            
            TareSettings settings = new TareSettings();
            root.entrySet().stream()
                    .forEach((Entry<String, JsonElement> entry) -> {
                        String name = entry.getKey();
                        TareSettingsEntry tse = DataReducer.GSON.fromJson(root.get(name), TareSettingsEntry.class);
                        settings.setSettings(name, tse);
                    });
            
            return settings;
        }
    }
    
}
