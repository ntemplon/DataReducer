/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Nathan Templon
 */
public class Test {
    
    // Fields
    private final Set<Run> runs;
    private final Set<Run> runsAccessor;
    
    
    // Properties
    public final Set<Run> getRuns() {
        return this.runsAccessor;
    }
    
    
    // Initialization
    public Test(Set<Run> runs) {
        this.runs = runs.stream()
                .collect(Collectors.toSet());
        this.runsAccessor = Collections.unmodifiableSet(this.runs);
    }
    
}
