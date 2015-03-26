/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs;

/**
 *
 * @author Nathan Templon
 */
public class Defaults {
    
    // Fields
    private String importDirectory = null;
    private String importFormat = null;

    
    // Properties
    /**
     * @return the importDirectory
     */
    public String getImportDirectory() {
        return importDirectory;
    }

    /**
     * @param importDirectory the importDirectory to set
     */
    public void setImportDirectory(String importDirectory) {
        this.importDirectory = importDirectory;
    }

    /**
     * @return the importFormat
     */
    public String getImportFormat() {
        return importFormat;
    }

    /**
     * @param importFormat the importFormat to set
     */
    public void setImportFormat(String importFormat) {
        this.importFormat = importFormat;
    }
    
}
