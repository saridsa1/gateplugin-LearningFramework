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

/**
 * A class that represents the result of a classification evaluation.
 * 
 * @author Johann Petrak
 */
public abstract class EvaluationResultClassification extends EvaluationResult {
  public double accuracyEstimate;
  public int nrCorrect;    // number of correct over all folds and all repeats
  public int nrIncorrect;  // number of incorrect over all folds and all repeats
  
  // TODO: correct implementation of equals and hashCode!?!

  
}
