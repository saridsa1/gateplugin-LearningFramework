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

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.SparseVector;
import gate.AnnotationSet;
import gate.plugin.learningframework.features.SeqEncoder;
import gate.plugin.learningframework.features.TargetType;
import gate.util.GateRuntimeException;
import java.util.List;
import libsvm.svm_node;
import libsvm.svm_problem;

/**
 *
 * @author Johann Petrak
 */
public class CorpusRepresentationLibSVM extends CorpusRepresentationMalletRelated {

  protected svm_problem data;
  protected CorpusRepresentationMallet crm;

  public CorpusRepresentationLibSVM(CorpusRepresentationMallet other) {
    data = getFromMallet(other);
    crm = other;    
  }

  /**
   * Return the libsvm internal representation of the corpus.
   * 
   * @return libsvm representation
   */
  public svm_problem getRepresentationLibSVM() {
    return data;
  }

  @Override
  public Object getRepresentation() {
    return data;
  }

  public static svm_node[] libSVMInstanceIndepFromMalletInstance(
          cc.mallet.types.Instance malletInstance) {

    // TODO: maybe check that data is really a sparse vector? Should be in all cases
    // except if we have an instance from MalletSeq
    SparseVector data = (SparseVector) malletInstance.getData();
    int[] indices = data.getIndices();
    double[] values = data.getValues();
    svm_node[] nodearray = new svm_node[indices.length];
    int index = 0;
    for (int j = 0; j < indices.length; j++) {
      svm_node node = new svm_node();
      node.index = indices[j]+1;   // NOTE: LibSVM locations have to start with 1
      node.value = values[j];
      nodearray[index] = node;
      index++;
    }
    return nodearray;
  }

  /**
   * Create libsvm representation from Mallet.
   *
   * @param crm mallet representation
   * @return libsvm representation
   */
  public static svm_problem getFromMallet(CorpusRepresentationMallet crm) {
    InstanceList instances = crm.getRepresentationMallet();
    svm_problem prob = new svm_problem();
    int numTrainingInstances = instances.size();
    prob.l = numTrainingInstances;
    prob.y = new double[prob.l];
    prob.x = new svm_node[prob.l][];

    for (int i = 0; i < numTrainingInstances; i++) {
      Instance instance = instances.get(i);

      //Labels
      // convert the target: if we get a label, convert to index,
      // if we get a double, use it directly
      Object tobj = instance.getTarget();
      if (tobj instanceof Label) {
        prob.y[i] = ((Label) instance.getTarget()).getIndex();
      } else if (tobj instanceof Double) {
        prob.y[i] = (double) tobj;
      } else {
        throw new GateRuntimeException("Odd target in mallet instance, cannot convert to LIBSVM: " + tobj);
      }

      //Features
      SparseVector data = (SparseVector) instance.getData();
      int[] indices = data.getIndices();
      double[] values = data.getValues();
      prob.x[i] = new svm_node[indices.length];
      for (int j = 0; j < indices.length; j++) {
        svm_node node = new svm_node();
        node.index = indices[j]+1; // NOTE: LibSVM location indices have to start with 1
        node.value = values[j];
        prob.x[i][j] = node;
      }
    }
    return prob;
  }

  @Override
  public InstanceList getRepresentationMallet() {
   return crm.instances;
  }

  @Override
  public void add(AnnotationSet instancesAS, AnnotationSet sequenceAS, AnnotationSet inputAS, AnnotationSet classAS, String targetFeatureName, TargetType targetType, String instanceWeightFeature, String nameFeatureName, SeqEncoder seqEncoder) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void finishAdding() {
    // Do nothing
  }

  @Override
  public int nrInstances() {
    return crm.nrInstances();
  }
  
  @Override
  public TargetType getTargetType() {
    return crm.getTargetType();
  }
  
  @Override
  public void setTargetType(TargetType val) {
    crm.setTargetType(val);
  }

  @Override
  public int nrDimensions() {
    return crm.getPipe().getAlphabet().size();
  }

  @Override
  public List<String> getLabelList() {
    return crm.getLabelList();
  }
  
  
}
