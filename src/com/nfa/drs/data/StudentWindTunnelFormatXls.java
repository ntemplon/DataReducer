/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import static com.nfa.drs.data.StudentWindTunnelFormatCsv.DELIMITER;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author Nathan Templon
 */
public class StudentWindTunnelFormatXls implements DataFormat {

    // Enumerations
    public enum ParseStates {

        RUN_NAME,
        DATA
    }


    // Constants
    public static final String DATA_FILE_EXTENSION = ".xls";
    public static final int AOA_INDEX = 0;
    public static final int LIFT_INDEX = 2;
    public static final int DRAG_INDEX = 4;
    public static final int PM_INDEX = 6;
    public static final int Q_INDEX = 8;
    public static final int COMMENT_INDEX = 10;

    public static final int LINES_IN_HEADER = 9;

    private final Pattern numCheck = Pattern.compile("[\\-0-9\\.]+");


    // Fields
    private int nextRunNumber = 1;
    private int nextStaticTareNumber = 1001;
    private int nextDynamicTareNumber = 3001;


    // Properties
    @Override
    public String getName() {
        return "WSU 3x4 Wind Tunnel (xls)";
    }


    // Public Methods
    @Override
    public Test fromDirectory(Path directory) {
        if (Files.isDirectory(directory)) {
            List<Run> runs = new ArrayList<>();
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
                for (Path path : dirStream) {
                    if (path.toString().toLowerCase().endsWith(DATA_FILE_EXTENSION)) {
                        runs.addAll(this.fromBlock(this.readXlsLines(path)));
//                        String output = this.readXlsLines(path).stream()
//                                .reduce((String first, String second) -> first + "\n" + second).get();
//                        JOptionPane.showMessageDialog(null, output);
                    }
                }
            }
            catch (IOException ex) {

            }
            return new Test(runs);
        }
        return null;
    }

    public List<Run> fromBlock(List<String> text) {
        List<Run> runs = new ArrayList<>(20);

        String name = "";
        List<Datapoint> points = new ArrayList<>(30);
        int pointNumber = 1;
        StudentWindTunnelFormatCsv.ParseStates state = StudentWindTunnelFormatCsv.ParseStates.RUN_NAME;
        for (int i = LINES_IN_HEADER; i < text.size(); i++) {
            String line = text.get(i);
            line = line.trim();
            if (line.replace(",", "").isEmpty()) {
                if (state.equals(StudentWindTunnelFormatCsv.ParseStates.DATA)) {
                    runs.add(new Run(name, points));
                    points.clear();
                    name = "";
                    state = StudentWindTunnelFormatCsv.ParseStates.RUN_NAME;
                    pointNumber = 1;
                }
            }
            else {
                String[] parts = line.split(",");
                if (state.equals(StudentWindTunnelFormatCsv.ParseStates.RUN_NAME)) {
                    if (parts.length == 1) {
                        name = parts[0];
                        state = StudentWindTunnelFormatCsv.ParseStates.DATA;
                        switch (name) {
                            case "RUN":
                                name = nextRunNumber + "";
                                nextRunNumber++;
                                break;
                            case "STATIC TARE":
                                name = nextStaticTareNumber + "";
                                nextStaticTareNumber++;
                                break;
                            case "DYNAMIC TARE":
                                name = nextDynamicTareNumber + "";
                                nextDynamicTareNumber++;
                                break;
                        }
                    }
                }
                else if (state.equals(StudentWindTunnelFormatCsv.ParseStates.DATA)) {
                    if (isNumber(parts[0])) {
                        Datapoint point = this.fromLine(line);
                        point.setPointNumber(pointNumber);
                        points.add(point);
                        pointNumber++;
                    }
                }
            }
        }

        if (!points.isEmpty()) {
            runs.add(new Run(name, points));
        }

        return runs;
    }

    public Datapoint fromLine(String line) {
        String[] parts = line.split(DELIMITER);

        Map<DataSet.DataValues, Double> data = new HashMap<>();
        if (parts.length > AOA_INDEX) {
            data.put(DataSet.DataValues.AngleOfAttack, this.safeParse(parts[AOA_INDEX]));
        }
        if (parts.length > LIFT_INDEX) {
            data.put(DataSet.DataValues.Lift, this.safeParse(parts[LIFT_INDEX]));
        }
        if (parts.length > DRAG_INDEX) {
            data.put(DataSet.DataValues.Drag, this.safeParse(parts[DRAG_INDEX]));
        }
        if (parts.length > PM_INDEX) {
            data.put(DataSet.DataValues.PitchMoment, this.safeParse(parts[PM_INDEX]));
        }
        if (parts.length > Q_INDEX) {
            data.put(DataSet.DataValues.DynamicPressure, this.safeParse(parts[Q_INDEX]));
        }

        DataSet ds = new DataSet(data);

        String comment = "";
        if (parts.length > COMMENT_INDEX) {
            comment = parts[COMMENT_INDEX];
        }

        return new Datapoint(0, ds, comment);
    }


    // Private Methods
    private List<String> readXlsLines(Path file) {
        List<String> lines = new ArrayList<>();

        try {
            FileInputStream stream = new FileInputStream(file.toFile());
            HSSFWorkbook book = new HSSFWorkbook(stream);
            HSSFSheet sheet = book.getSheetAt(0);

            for (Row row : sheet) {
                int rowIndex = row.getRowNum();
                while (rowIndex > lines.size() - 1) {
                    lines.add("");
                }

                StringBuilder line = new StringBuilder();
                for (Cell cell : row) {
                    if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                        line.append(cell.getStringCellValue());
                    }
                    else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        line.append(cell.getNumericCellValue());
                    }
                    line.append(",");
                }
                lines.add(line.toString());
            }
        }
        catch (IOException ex) {

        }

        return lines;
    }

    private double safeParse(String value) {
        try {
            return Double.parseDouble(value);
        }
        catch (Exception ex) {
            return 0.0;
        }
    }

    private boolean isNumber(String test) {
        Matcher match = numCheck.matcher(test);
        if (!match.matches()) {
            return false;
        }
        return match.start() == 0 && match.end() == test.length();
    }

}
