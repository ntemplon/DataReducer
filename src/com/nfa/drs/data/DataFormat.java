/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nfa.drs.data;

import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author Nathan Templon
 */
public interface DataFormat {
    String getName();
    
    Test fromDirectory(Path directory);
}
