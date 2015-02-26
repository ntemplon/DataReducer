/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.constants;

import com.nfa.drs.constants.ModelConstants.Constants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;

/**
 *
 * @author Nathan Templon
 */
public class ModelConstantsPanel extends JPanel {
    
    // Initialization
    public ModelConstantsPanel() {
        
        this.initComponents();
        
    }
    
    
    // Private Methods
    private void initComponents() {
        this.setLayout(new GridBagLayout());
        
        for(Constants mc : Constants.values()) {
            GridBagConstraints c = new GridBagConstraints();
        }
    }
    
}
