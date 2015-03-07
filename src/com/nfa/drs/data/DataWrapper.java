/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

/**
 *
 * @author Nathan Templon
 */
public class DataWrapper {

    // Fields
    private final String name;
    private final DataContainer data;
    
    
    // Propertie
    public String getName() {
        return this.name;
    }
    
    public DataContainer getData() {
        return this.data;
    }
    
    
    // Initialization
    public DataWrapper(String name, DataContainer data) {
        this.name = name;
        this.data = data;
    }
    
}
