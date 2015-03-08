/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction;

import com.nfa.drs.constants.ModelConstants;
import com.nfa.drs.data.DataSet;

/**
 *
 * @author Nathan Templon
 */
public interface DataCorrection {
    
    DataSet getCorrection(DataSet data, ModelConstants constants);
    String getName();
    
}
