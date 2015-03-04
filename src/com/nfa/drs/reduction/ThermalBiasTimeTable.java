/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.reduction;

import com.nfa.gui.TableColumnAdjuster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Nathan Templon
 */
public class ThermalBiasTimeTable extends JTable {

    // Fields
    private final ThermalBiasTimeTableModel model;
    private final TableColumnAdjuster tca = new TableColumnAdjuster(this);


    // Properties
    public void setTimes(Map<Integer, Double> times) {
        this.model.setTimes(times);
        this.tca.adjustColumns();
    }

    public Map<Integer, Double> getTimes() {
        return this.model.getTimes();
    }


    // Initialization
    public ThermalBiasTimeTable() {
        this(new ThermalBiasTimeTableModel());
    }

    private ThermalBiasTimeTable(ThermalBiasTimeTableModel model) {
        super(model);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.model = model;
        this.tca.adjustColumns();
    }


    // Public Methods
    // Nested Classes
    private static class ThermalBiasTimeTableModel extends DefaultTableModel {

        // Fields
        private final List<String> columnHeaders = Collections.unmodifiableList(Arrays.asList(new String[]{"Test Point", "Time Since Start"}));
        private final List<Class<?>> columnClasses = Collections.unmodifiableList(Arrays.asList(new Class<?>[]{Integer.class, Double.class}));
        private final List<Object[]> data = new ArrayList<>();


        // Properties
        public void setTimes(Map<Integer, Double> times) {
            this.data.clear();
            this.data.addAll(times.keySet().stream()
                    .map((Integer point) ->
                            new Object[]{point, times.get(point)}
                    )
                    .collect(Collectors.toList())
            );
            this.fireTableDataChanged();
        }

        public Map<Integer, Double> getTimes() {
            return Collections.unmodifiableMap(
                    this.data.stream()
                    .collect(Collectors.toMap(
                                    (Object[] row) -> (Integer) row[0],
                                    (Object[] row) -> (Double) row[1]
                            ))
            );
        }


        // Public Methods
        @Override
        public String getColumnName(int col) {
            return this.columnHeaders.get(col);
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return this.columnClasses.get(col);
        }

        @Override
        public int getRowCount() {
            if (this.data == null) {
                // Can happen during superclass constructor
                return 0;
            }
            return this.data.size();
        }

        @Override
        public int getColumnCount() {
            return this.columnHeaders.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return this.data.get(rowIndex)[columnIndex];
        }
        
        @Override
        public void setValueAt(Object item, int rowIndex, int columnIndex) {
            this.data.get(rowIndex)[columnIndex] = item;
            this.fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col > 0;
        }

        @Override
        public int findColumn(String columnName) {
            return this.columnHeaders.indexOf(columnName);
        }

    }

}
