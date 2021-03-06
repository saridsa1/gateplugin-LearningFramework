/*
 * Copyright (c) 2015-2016 The University Of Sheffield.
 *
 * This file is part of gateplugin-LearningFramework 
 * (see https://github.com/GateNLP/gateplugin-LearningFramework).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */


package gate.plugin.learningframework.engines;

import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.clipatched.CommandLine;
import org.apache.commons.clipatched.Options;
import org.apache.commons.clipatched.DefaultParser;
import org.apache.commons.clipatched.ParseException;
import org.apache.log4j.Logger;

/**
 * Some utilities to make it easy to extract the values of parameters as needed.
 * Parameters are specified as a string for training, application, evaluation and 
 * exporting. Which parameters can be used and what they do depends entirely on the 
 * situation and the algorithm used. Each algorithm will just look if any of the parameters
 * it knows is there and use those which are there - all other parameters are simply ignore.
 * If a parameter is there and cannot be converted to the required type, an exception is thrown
 * by the engine using the parameter.
 * 
 * The syntax of parameters is identical to how long options are specified on the command line,
 * e.g. "-maxDepth 3 -prune" sets the value of parameter "maxDepth" to "2" and the value of 
 * parameter "prune" to "true" (all Strings which will subsequently get converted to Integer and 
 * Boolean, respectively)
 * 
 * IMPORTANT NOTE 1: at the moment we use a slightly modified version of commons.cli which 
 * allows us to ignore unknown options by calling ignoreUnknownOptions(true) (this method has been
 * added)
 * Related Jira issue: https://issues.apache.org/jira/browse/CLI-257
 * 
 * IMPORTANT NOTE 2: at closer inspection it appears that commons.cli is broken or odd, for example
 * there is a mess with short options versus long options (-asd is the same as -a -s -d) and there
 * seems to be a mess with option values immediately following the option name, e.g. -opt1value1 
 * will give the value value1 if option "opt1" is defined. We should probably re-implement this 
 * using a different option processing library sometimes!
 * @author Johann Petrak
 */
public class Parms {
  private static final Logger LOGGER = Logger.getLogger(Parms.class.getName());
  private Map<String,Object> parmValues = new HashMap<>();
  /**
   * Create a Parms object that contains the parsed values from the parmString.
   * 
   * The names are strings which consist of three parts, separated by colons: a short name,
   * a long name, and one of 
   * b if the parameter is boolean and does not have a value, 
   * s if the parameter has a string  value,
   * d if the parameter has a double value or i if the parameter has an integer value.
   * B is used for a parameter with an excplicit boolean value.
   * If the value cannot be parsed to the given type, it is equivalent to the parameter missing.
   * 
   * @param names strings indicating short/long name and type of parameter to find 
   * @param parmString string with the parameters
   */
  public Parms(String parmString, String... names) {
    // just treat a parmString of null equal to the empty string: do nothing
    if(parmString == null || parmString.isEmpty()) {
      return;
    }
    List<String> longNames = new ArrayList<>();
    List<String> types = new ArrayList<>();
    Options options = new Options();
    for(String name : names) {
      String[] info = name.split(":");
      longNames.add(info[1]);
      types.add(info[2]);
      options.addOption(info[0], info[1], !info[2].equals("b"), "");
    }
    DefaultParser parser = new DefaultParser();
    parser.setIgnoreUnknownOptions(true);
    CommandLine cli = null;
    try {
      cli = parser.parse(options,parmString.split("\\s+"));
    } catch (ParseException ex) {
      //System.err.println("Parsing error");
      //ex.printStackTrace(System.err);
      LOGGER.error("Could not parse parameters: "+parmString,ex);
    }
    if(cli != null) {
      for(int i = 0; i<longNames.size(); i++) {
        String longName = longNames.get(i);
        String type = types.get(i);
        Object value =null;
        boolean haveIt = cli.hasOption(longName);
        String optVal = cli.getOptionValue(longName);
        //System.err.println("OPTION value for "+longName+" is "+optVal+" class is "+((optVal==null)? "null" : optVal.getClass())+" have it: "+haveIt);
        if(haveIt) {
        if(type.equals("b")) {
          value = haveIt;
        } else if(type.equals("s")) {
          value = optVal;
        } else if(type.equals("d")) {
          try {
            value = Double.parseDouble(optVal);
          } catch(NumberFormatException ex) {
            System.err.println("Parms: cannot parse value as double, setting to null: "+optVal);
          }
        } else if(type.equals("i")) {
          try {
            value = Integer.parseInt(optVal);
          } catch(NumberFormatException ex) {
            System.err.println("Parms: cannot parse value as int, setting to null: "+optVal);
          }
        } else if(type.equals("B")) {
          value = Boolean.parseBoolean(optVal);
        } else {
          throw new GateRuntimeException("Not a valid type indicator for Parrms: "+type);
        }
        }
        parmValues.put(longName, value);
      }
    }
  }
  
  /**
   * Get the value of the parameter.
   * @param name parameter name
   * @return value
   */
  public Object getValue(String name) {
    return parmValues.get(name);
  }
  
  /**
   * Get the value of the parameter or some default value/
   * @param name parameter name
   * @param elseValue default value
   * @return value 
   */
  public Object getValueOrElse(String name, Object elseValue) {
    Object tmp = parmValues.get(name);
    if(tmp==null) {
      return elseValue;
    } else {
      return tmp;
    }
  }
  
  /**
   * Get number of values.
   * @return number of values
   */
  public int size() { return parmValues.size(); }
  
}
