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
public class Datapoint implements DataContainer { 
    
    // Fields
    private final DataSet data;
    private final String comment;
    
    
    // Properties
    @Override
    public DataSet getData() {
        return this.data;
    }
    
    public String getComment() {
        return this.comment;
    }
    
    public void setPointNumber(int pointNumber) {
        this.data.setPointNumber(pointNumber);
    }

    
    
    // Initialization
    public Datapoint(DataSet data, String comment) {
        this.data = data;
        this.comment = comment;
    }
    
    public Datapoint(DataSet data) {
        this(data, "");
    }
    
}
