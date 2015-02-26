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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    
    
    // Properties
    @Override
    public String getName() {
        return "WSU 3x4 Wind Tunnel (csv)";
    }


    // Public Methods
    @Override
    public Test fromDirectory(Path dir) {
        if (Files.isDirectory(dir)) {
            Set<Run> runs = new HashSet<>();
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
    public Set<Run> fromBlock(List<String> text) {
        Set<Run> runs = new HashSet<>();

        final Pattern numCheck = Pattern.compile("\b[0-9\\.]+\b");

        String name = "";
        List<Datapoint> points = new ArrayList<>(30);
        ParseStates state = ParseStates.RUN_NAME;
        for (int i = LINES_IN_HEADER; i < text.size(); i++) {
            String line = text.get(i);
            if (line.isEmpty()) {
                if (state.equals(ParseStates.DATA)) {
                    runs.add(new Run(name, points));
                    points.clear();
                    name = "";
                }
            }
            else {
                String[] parts = line.split(",");
                if (state.equals(ParseStates.RUN_NAME)) {
                    name = parts[0];
                    state = ParseStates.DATA;
                }
                else if (state.equals(ParseStates.DATA)) {
                    if (numCheck.asPredicate().test(line)) {
                        points.add(this.fromLine(line));
                    }
                }
            }
        }

        return runs;
    }

    @Override
    public Datapoint fromLine(String line) {
        String[] parts = line.split(DELIMITER);

        DataSet data = new DataSet();
        if (parts.length > AOA_INDEX) {
            data.set(DataValues.AngleOfAttack, this.safeParse(parts[AOA_INDEX]));
        }
        if (parts.length > LIFT_INDEX) {
            data.set(DataValues.Lift, this.safeParse(parts[LIFT_INDEX]));
        }
        if (parts.length > DRAG_INDEX) {
            data.set(DataValues.Drag, this.safeParse(parts[DRAG_INDEX]));
        }
        if (parts.length > PM_INDEX) {
            data.set(DataValues.PitchMoment, this.safeParse(parts[PM_INDEX]));
        }
        if (parts.length > Q_INDEX) {
            data.set(DataValues.DynamicPressure, this.safeParse(parts[Q_INDEX]));
        }

        String comment = "";
        if (parts.length > COMMENT_INDEX) {
            comment = parts[COMMENT_INDEX];
        }

        return new Datapoint(data, comment);
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

}
