/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class Run {
    
    // Fields
    private final List<Datapoint> points;
    private final List<Datapoint> pointAccess;
    private final String name;
    
    
    // Properties
    public final List<Datapoint> getDatapoints() {
        return this.pointAccess;
    }
    
    public final int getPointCount() {
        return this.pointAccess.size();
    }
    
    public final String getName() {
        return this.name;
    }
    
    
    // Initialization
    public Run(String name, List<Datapoint> points) {
        this.name = name;
        
        this.points = points.stream()
                .collect(Collectors.toList());
        this.pointAccess = Collections.unmodifiableList(this.points);
    }
    
}
