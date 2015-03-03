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
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Nathan Templon
 */
public class DataContainerViewer extends JTable {

    // Constants
    public static final String DESCRIPTION_COLUMN_NAME = "Run Name";
    public static final String COMMENT_COLUMN_NAME = "Comment";

    private static String[] getHeaderStrings() {
        List<String> names = new ArrayList<>();

        names.add(DESCRIPTION_COLUMN_NAME);
        for (DataValues value : DataValues.values()) {
            names.add(value.getDisplayName());
        }
        names.add(COMMENT_COLUMN_NAME);

        return names.toArray(new String[names.size()]);
    }


    // Fields
    private final DataViewerModel model;
    private final TableColumnAdjuster tca;


    // Initialization
    public DataContainerViewer() {
        this(new DataViewerModel());
    }

    private DataContainerViewer(DataViewerModel model) {
        super(model);
        this.model = model;
        this.tca = new TableColumnAdjuster(this);
        this.tca.adjustColumns();
    }


    // Public Methods
    public void addData(String name, DataContainer data) {
        this.model.addData(name, data);
        this.tca.adjustColumns();
    }

    public void clear() {
        this.model.clear();
        tca.adjustColumns();
    }


    // Private Classes
    private static class DataViewerModel extends AbstractTableModel {

        // Fields
        private final List<String> columnNames = Collections.unmodifiableList(Arrays.asList(DataContainerViewer.getHeaderStrings()));
        private final List<Object[]> data = new ArrayList<>();


        // Initializaiton
        private DataViewerModel() {
            super();
        }


        // Public Methods
        public void addData(String name, DataContainer data) {
            Object[] objs = new Object[this.columnNames.size()];

            int index = 0;
            objs[index] = name;
            index++;
            for (String header : columnNames) {
                if (!(header.equals(DESCRIPTION_COLUMN_NAME) || header.equals(COMMENT_COLUMN_NAME))) {
                    DataValues values = DataValues.getByDisplayName(header);
                    objs[index] = data.getData().get(values);
                    index++;
                }
            }
            objs[index] = data.getComment();

            this.data.add(objs);

            this.fireTableRowsInserted(this.data.size() - 1, this.data.size() - 1);
        }

        public void clear() {
            this.data.clear();
            this.fireTableDataChanged();
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
            return this.data.get(rowIndex)[columnIndex];
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
