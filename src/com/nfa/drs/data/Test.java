/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class Test {
    
    // Fields
    private final List<Run> runs;
    private final List<Run> runsAccessor;
    private final Map<String, Run> byName;
    
    
    // Properties
    public final List<Run> getRuns() {
        return this.runsAccessor;
    }
    
    public final Run getRun(String name) {
        return this.byName.get(name);
    }
    
    
    // Initialization
    public Test(List<Run> runs) {
        this.runs = runs.stream()
                .collect(Collectors.toList());
        this.runsAccessor = Collections.unmodifiableList(this.runs);
        
        this.byName = this.runs.stream()
                .collect(Collectors.toMap(
                        (Run run) -> run.getName(),
                        (Run run) -> run)
                );
    }
    
}
