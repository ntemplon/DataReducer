/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import com.nfa.drs.data.DataSet.DataValues;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author Nathan Templon
 */
public class DataContainerViewer extends JTable {
    
    // Constants
    public static final String DESCRIPTION_COLUMN_NAME = "Description";
    
    private static String[] getHeaderStrings() {
        List<String> names = new ArrayList<>();
        
        names.add(DESCRIPTION_COLUMN_NAME);
        for(DataValues value : DataValues.values()) {
            names.add(value.getDisplayName());
        }
        
        return names.toArray(new String[names.size()]);
    }
    
    
    // Fields
    private final List<DataContainer> dataContainers = new ArrayList<>();
    private final List<DataContainer> containerAccess = Collections.unmodifiableList(this.dataContainers);
    private final Map<String, DataContainer> byName = new HashMap<>();
    
    
    // Initialization
    public DataContainerViewer() {
        super(new Object[0][0], getHeaderStrings());
        this.initComponent();
    }
    
    
    // Private Methods
    private void initComponent() {
        
    }
    
}
