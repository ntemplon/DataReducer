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
    
    private int pointNumber; 
    
    
    // Properties
    @Override
    public DataSet getData() {
        return this.data;
    }
    
    @Override
    public String getComment() {
        return this.comment;
    }
    
    public int getPointNumber() {
        return this.pointNumber;
    }
    
    public void setPointNumber(int pointNumber) {
        this.pointNumber = pointNumber;
    }

    
    
    // Initialization
    public Datapoint(int pointNumber, DataSet data, String comment) {
        this.pointNumber = pointNumber;
        this.data = data;
        this.comment = comment;
    }
    
    public Datapoint(int pointNumber, DataSet data) {
        this(pointNumber, data, "");
    }
    
}
