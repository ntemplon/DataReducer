/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.io;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Nathan Templon
 */
public class FileLocations {
    
    // Constants
    public static final Path USER_HOME = Paths.get(System.getProperty("user.home"));
    public static final Path SETTINGS_DIRECTORY = USER_HOME.resolve(".nfadrs");
    public static final Path DEFAULTS_FILE = SETTINGS_DIRECTORY.resolve("defaults.json");
    
    
    // Static Initialization
    static {
        Field[] fields = FileLocations.class.getDeclaredFields();
        for (Field field : fields) {
            if (Path.class.isAssignableFrom(field.getType()) && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    Path file = (Path) field.get(null);
                    if (!Files.exists(file)) {
                        Files.createDirectories(file.getParent());
                    }
                }
                catch (IllegalArgumentException | IllegalAccessException | IOException ex) {
                    
                }
                finally {

                }
            }
        }
    }
    
    
    // Initialization
    private FileLocations() {
        
    }
    
}
