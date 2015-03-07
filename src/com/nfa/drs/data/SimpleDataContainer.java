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
public class SimpleDataContainer implements DataContainer {

    // Fields
    private final DataSet data;
    private final String comment;
    
    
    // Initialization
    public SimpleDataContainer(DataSet data, String comment) {
        this.data = data;
        this.comment = comment;
    }
    
    
    // Public Methods
    @Override
    public DataSet getData() {
        return this.data;
    }

    @Override
    public String getComment() {
        return this.comment;
    }
    
}
