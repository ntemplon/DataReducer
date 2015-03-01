/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import com.nfa.drs.data.DataSet.DataValues;
import com.nfa.gui.TableColumnAdjuster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

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
        for (DataValues value : DataValues.values()) {
            names.add(value.getDisplayName());
        }

        return names.toArray(new String[names.size()]);
    }


    // Fields
    private final List<DataContainer> dataContainers = new ArrayList<>();
    private final List<DataContainer> containerAccess = Collections.unmodifiableList(this.dataContainers);
    private final Map<String, DataContainer> byName = new HashMap<>();
    private final DataViewerModel model;
    
    
    // Properties
    public List<DataContainer> getDataContainers() {
        return this.containerAccess;
    }


    // Initialization
    public DataContainerViewer() {
        this(new DataViewerModel());
    }

    private DataContainerViewer(DataViewerModel model) {
        super(model);
        this.model = model;
        this.initComponent();
    }


    // Public Methods
    public void addData(String name, DataContainer data) {
        this.model.addData(name, data);
    }


    // Private Methods
    private void initComponent() {
        this.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN;
        TableColumnAdjuster tca = new TableColumnAdjuster(this);
        tca.adjustColumns();
        
//        TableColumnModel cModel = this.getColumnModel();
//        for (int col = 0; col < this.getColumnCount(); col++) {
//            TableCellRenderer render = this.getCellRenderer(0, col);
//            Component comp = this.prepareRenderer(render, 0, col);
//            cModel.getColumn(col).setPreferredWidth(comp.getPreferredSize().width);
//            System.out.println(this.model.getColumnName(col) + ": " + comp.getPreferredSize().width);
//        }
    }


    // Private Classes
    private static class DataViewerModel extends AbstractTableModel {

        // Fields
        private final List<String> rowNames = new ArrayList<>();
        private final List<DataContainer> data = new ArrayList<>();
        private final List<String> columnNames = Collections.unmodifiableList(Arrays.asList(DataContainerViewer.getHeaderStrings()));


        // Initializaiton
        private DataViewerModel() {
            super();
        }


        // Public Methods
        public void addData(String name, DataContainer data) {
            this.rowNames.add(name);
            this.data.add(data);
        }


        // AbstractTableModel Methods
        @Override
        public String getColumnName(int col) {
            return this.columnNames.get(col);
        }

        @Override
        public int getRowCount() {
            return this.data.size();
        }

        @Override
        public int getColumnCount() {
            return this.columnNames.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String header = this.getColumnName(columnIndex);
            if (header.equals(DataContainerViewer.DESCRIPTION_COLUMN_NAME)) {
                if (rowNames.isEmpty()) {
                    return "";
                }
                return rowNames.get(columnIndex);
            }
            else {
                if (data.isEmpty()) {
                    return 0.0;
                }
                DataValues dataValue = DataValues.getByDisplayName(header);
                return this.data.get(rowIndex).getData().get(dataValue);
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public int findColumn(String columnName) {
            return this.columnNames.indexOf(columnNames);
        }

    }

}
