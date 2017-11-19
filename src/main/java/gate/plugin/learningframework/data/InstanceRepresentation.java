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
package gate.plugin.learningframework.data;

/**
 * Common interface to our own representations of learning instances.
 * 
 * Learning instances represent features and optional target information.
 * Features map from a feature name (a String) to a feature value (an Object).
 * Specific InstanceRepresentations can limit the type of the value to e.g.
 * just floats.
 * The target information also maps a target property name (a String) to a 
 * target property value (an Object). Again, specific implementations can
 * limit the available target property names and/or the type of their values.
 * 
 * @author Johann Petrak <johann.petrak@gmail.com>
 */
public interface InstanceRepresentation {
  /**
   * Set the value of a feature.
   * 
   * This maps the given name to the given value, if a mapping for that
   * name already existed, overriding it. Depending on the implementation,
   * this method may return a new immutable InstanceRepresentation object.
   * @param name
   * @param value
   * @return 
   */
  
  public static final String TARGET_VALUE = "╔TARGETVALUE╗";
  public static final String TARGET_COSTS = "╔TARGETCOSTS╗";
  public static final String INSTANCE_WEIGHT = "╔INSTANCEWEIGHT╗";
  public static final String HASMISSINGVALUE_FLAG = "╔HASMISSINGVALUE╗";
  
  public InstanceRepresentation setFeature(String name, Object value);
  public Object getFeature(String name);
  public boolean hasFeature(String name);
  public InstanceRepresentation setTargetValue(Object value);
  public boolean hasTarget();
  public InstanceRepresentation setTargetCosts(Object value);
  public InstanceRepresentation setInstanceWeight(double weight);
  public InstanceRepresentation setHasMissing(boolean flag);
  public boolean hasMissing();
}