/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import com.nfa.drs.data.DataSet.DataValues;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Nathan Templon
 */
public class StudentWindTunnelFormat implements DataFormat {

    // Enumerations
    public enum ParseStates {

        RUN_NAME,
        DATA
    }


    // Constants
    public static final String DELIMITER = ",";
    public static final int AOA_INDEX = 0;
    public static final int LIFT_INDEX = 2;
    public static final int DRAG_INDEX = 4;
    public static final int PM_INDEX = 6;
    public static final int Q_INDEX = 8;
    public static final int COMMENT_INDEX = 10;

    public static final int LINES_IN_HEADER = 8;
    
    private final Pattern numCheck = Pattern.compile("[\\-0-9\\.]+");


    // Properties
    @Override
    public String getName() {
        return "WSU 3x4 Wind Tunnel (csv)";
    }


    // Public Methods
    @Override
    public Test fromDirectory(Path dir) {
        if (Files.isDirectory(dir)) {
            List<Run> runs = new ArrayList<>(20);
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
                for (Path path : dirStream) {
                    try {
                        if (path.toString().toLowerCase().endsWith(".csv")) {
                            runs.addAll(this.fromBlock(Files.readAllLines(path)));
                        }
                    }
                    catch (IOException ex) {
                        
                    }
                }
            }
            catch (IOException ex) {

            }
            return new Test(runs);
        }
        return null;
    }

    @Override
    public List<Run> fromBlock(List<String> text) {
        List<Run> runs = new ArrayList<>(20);

        String name = "";
        List<Datapoint> points = new ArrayList<>(30);
        int pointNumber = 1;
        ParseStates state = ParseStates.RUN_NAME;
        for (int i = LINES_IN_HEADER; i < text.size(); i++) {
            String line = text.get(i);
            line = line.trim();
            if (line.replace(",", "").isEmpty()) {
                if (state.equals(ParseStates.DATA)) {
                    runs.add(new Run(name, points));
                    points.clear();
                    name = "";
                    state = ParseStates.RUN_NAME;
                    pointNumber = 1;
                }
            }
            else {
                String[] parts = line.split(",");
                if (state.equals(ParseStates.RUN_NAME)) {
                    if (parts.length == 1) {
                        name = parts[0];
                        state = ParseStates.DATA;
                    }
                }
                else if (state.equals(ParseStates.DATA)) {
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

    @Override
    public Datapoint fromLine(String line) {
        String[] parts = line.split(DELIMITER);

        Map<DataValues, Double> data = new HashMap<>();
        if (parts.length > AOA_INDEX) {
            data.put(DataValues.AngleOfAttack, this.safeParse(parts[AOA_INDEX]));
        }
        if (parts.length > LIFT_INDEX) {
            data.put(DataValues.Lift, this.safeParse(parts[LIFT_INDEX]));
        }
        if (parts.length > DRAG_INDEX) {
            data.put(DataValues.Drag, this.safeParse(parts[DRAG_INDEX]));
        }
        if (parts.length > PM_INDEX) {
            data.put(DataValues.PitchMoment, this.safeParse(parts[PM_INDEX]));
        }
        if (parts.length > Q_INDEX) {
            data.put(DataValues.DynamicPressure, this.safeParse(parts[Q_INDEX]));
        }

        DataSet ds = new DataSet(data);

        String comment = "";
        if (parts.length > COMMENT_INDEX) {
            comment = parts[COMMENT_INDEX];
        }

        return new Datapoint(0, ds, comment);
    }


    // Private Methods
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
