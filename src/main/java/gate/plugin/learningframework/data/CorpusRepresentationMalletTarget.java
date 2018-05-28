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

import gate.Annotation;
import gate.AnnotationSet;
import java.util.List;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.LFUtils;
import gate.plugin.learningframework.features.FeatureSpecAttribute;
import gate.plugin.learningframework.features.FeatureExtractionMalletSparse;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.SeqEncoder;
import gate.plugin.learningframework.features.TargetType;
import gate.plugin.learningframework.mallet.LFPipe;
import gate.plugin.learningframework.mallet.PipeScaleMeanVarAll;
import gate.plugin.learningframework.mallet.PipeScaleMinMaxAll;
import gate.plugin.learningframework.mbstats.FVStatsMeanVarAll;
import gate.util.GateRuntimeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import static gate.plugin.learningframework.LFUtils.newURL;

/**
 * This represents a corpus in Mallet format where we have a single feature vector and single
 * target. This corpus may be created for classification, regression or sequence tagging, but the
 * representation is always a pair (featurevector,target). In the case of sequence tagging, the
 * corpus is created for the use of normal classification algorithms that will be used to predict
 * the begin/inside/outside classifications.
 *
 * @author johann
 */
public class CorpusRepresentationMalletTarget extends CorpusRepresentationMallet {

  static final Logger logger = Logger.getLogger("CorpusRepresentationMallet");


  /**
   * Constructor for creating a new CorpusRepresentation from a FeatureInfo. 
   * @param fi TODO
   * @param sm  TODO
   * @param targetType TODO
   */
  public CorpusRepresentationMalletTarget(FeatureInfo fi, ScalingMethod sm, TargetType targetType) {
    featureInfo = fi;
    scalingMethod = sm;

    LabelAlphabet targetAlphabet = (targetType == TargetType.NOMINAL) ? new LabelAlphabet() : null;
    Pipe innerPipe = new Noop(new Alphabet(), targetAlphabet);
    List<Pipe> pipes = new ArrayList<>();
    pipes.add(innerPipe);
    pipe = new LFPipe(pipes);
    pipe.setFeatureInfo(fi);
    instances = new InstanceList(pipe);
  }
  
  /**
   * Non-public constructor for use when creating from a serialized pipe.
   * @param fi 
   */
  CorpusRepresentationMalletTarget(LFPipe pipe) {
    this.pipe = pipe;
    this.featureInfo = pipe.getFeatureInfo();
    this.scalingMethod = null;
    this.instances = new InstanceList(pipe);
  }

  /**
   * Create a new CRMT instance based on the pipe stored in directory.
   * @param directory TODO
   * @return  TODO
   */
  public static CorpusRepresentationMalletTarget load(URL directory) {
    // load the pipe
    URL inFile = newURL(directory,"pipe.pipe");
    ObjectInputStream ois = null;
    LFPipe lfpipe = null;
    try (InputStream is = inFile.openStream()) {
      ois = new ObjectInputStream (is);
      lfpipe = (LFPipe) ois.readObject();
    } catch (Exception ex) {
      throw new GateRuntimeException("Could not read pipe from "+inFile,ex);
    } finally {
      try {
        if(ois!=null) ois.close();
      } catch (IOException ex) {
        logger.error("Error closing stream after loading pipe "+inFile, ex);
      }
    }
    CorpusRepresentationMalletTarget crmc = new CorpusRepresentationMalletTarget(lfpipe);
    return crmc;
  }
  
  
  
  
  
  // NOTE: at application time we do not explicitly create a CorpusRepresentatioMallet object.
  // Instead, the pipe gets saved with the model and can get retrieved from the loaded model 
  // later. The method extractIndependentFeaturesHelper is also used at application time to 
  // extract the Instances, using the Pipe that was stored with the model.
  // For non-Mallet algorithms we store the pipe separately and load it separately when the model
  // is loaded for application. The Pipe is then again used with extractIndependentFeaturesHelper 
  // to get the instances.

  /**
   * TODO
   * @param instanceAnnotation TODO
   * @param inputAS TODO
   * @return TODO 
   */


  public Instance extractIndependentFeatures(
          Annotation instanceAnnotation,
          AnnotationSet inputAS)
  {
    LFPipe pipe = (LFPipe)instances.getPipe();
    FeatureInfo featureInfo = pipe.getFeatureInfo();
    return extractIndependentFeaturesHelper(instanceAnnotation, inputAS,
            featureInfo, pipe);
  }
  
  /**
   * Extract the independent features for a single instance annotation.
   * Extract the independent features for a single annotation according to the information
   * in the featureInfo object. The information in the featureInfo instance gets updated 
   * by this. 
   * NOTE: this method is static so that it can be used in the CorpusRepresentationMalletSeq class too.
   * @param instanceAnnotation TODO
   * @param inputAS TODO
   * @param targetFeatureName TODO
   * @param featureInfo TODO
   * @param pipe TODO
   * @param nameFeature TODO
   * @return  TODO
   */
  static Instance extractIndependentFeaturesHelper(
          Annotation instanceAnnotation,
          AnnotationSet inputAS,
          FeatureInfo featureInfo,
          Pipe pipe) {
    
    AugmentableFeatureVector afv = new AugmentableFeatureVector(pipe.getDataAlphabet());
    // Constructor parms: data, target, name, source
    Instance inst = new Instance(afv, null, null, null);
    for(FeatureSpecAttribute attr : featureInfo.getAttributes()) {
      FeatureExtractionMalletSparse.extractFeature(inst, attr, inputAS, instanceAnnotation);
    }
    // TODO: we destructively replace the AugmentableFeatureVector by a FeatureVector here,
    // but it is not clear if this is beneficial - our assumption is that yes.
    inst.setData(((AugmentableFeatureVector)inst.getData()).toFeatureVector());
    return inst;
  }

  /**
   * Add instances. The exact way of how the target is created to the instances depends on which
   * parameters are given and which are null. The parameter sequenceAS must always be null for this
   * corpus representation since this corpus representation is not usable for sequence tagging
   * algorithms If the parameter classAS is non-null then instances for a sequence tagging task are
   * created, in that case targetFeatureName must be null. If targetFeatureName is non-null then
   * instances for a regression or classification problem are created (depending on targetType) and
   * classAS must be null. if the parameter nameFeatureName is non-null, then a Mallet instance name
   * is added from the source document and annotation.
   *
   * @param instancesAS TODO
   * @param sequenceAS TODO
   * @param inputAS TODO
   * @param classAS TODO
   * @param targetFeatureName TODO
   * @param targetType TODO
   * @param instanceWeightFeature TODO
   * @param nameFeatureName TODO
   * @param seqEncoder TODO
   */
  @Override
  public void add(AnnotationSet instancesAS, AnnotationSet sequenceAS, AnnotationSet inputAS, AnnotationSet classAS, String targetFeatureName, TargetType targetType, String instanceWeightFeature, String nameFeatureName, SeqEncoder seqEncoder) {
    if(sequenceAS != null) {
      throw new GateRuntimeException("LF invalid call to CorpusRepresentationMallet.add: sequenceAS must be null "+
              " for document "+inputAS.getDocument().getName());
    }
    List<Annotation> instanceAnnotations = instancesAS.inDocumentOrder();
    for (Annotation instanceAnnotation : instanceAnnotations) {
      Instance inst = extractIndependentFeaturesHelper(instanceAnnotation, inputAS, featureInfo, pipe);
      if (classAS != null) {
        // extract the target as required for sequence tagging
        FeatureExtractionMalletSparse.extractClassForSeqTagging(inst, pipe.getTargetAlphabet(), classAS, instanceAnnotation, seqEncoder);
      } else {
        if(targetType == TargetType.NOMINAL) {
          FeatureExtractionMalletSparse.extractClassTarget(inst, pipe.getTargetAlphabet(), targetFeatureName, instanceAnnotation, inputAS);
        } else if(targetType == TargetType.NUMERIC) {
          FeatureExtractionMalletSparse.extractNumericTarget(inst, targetFeatureName, instanceAnnotation, inputAS);
        }
      }
      // if a nameFeature is specified, add the name informatin to the instance
      if(nameFeatureName != null) {
        FeatureExtractionMalletSparse.extractName(inst, instanceAnnotation, inputAS.getDocument());
      }
      if(instanceWeightFeature != null && !instanceWeightFeature.isEmpty()) {
        // If the instanceWeightFeature is not specified we do not set any weight, but if it is 
        // specified then we either try to convert the value to double or use 1.0.
        double score = LFUtils.anyToDoubleOrElse(instanceAnnotation.getFeatures().get(instanceWeightFeature), 1.0);
        inst.setProperty("instanceWeight", score);
      }
      if(!FeatureExtractionMalletSparse.ignoreInstanceWithMV(inst)) {
        instances.add(inst);
      }
    }
  }
  
  /**
   * Finish adding instances to the CR. 
   * This will also do the rescaling and any other additional calculations, if necessary. 
   */
  @Override
  public void finishAdding() {    
    if(scalingMethod == ScalingMethod.NONE) return;
    Pipe normalizer = null;
    if(scalingMethod == ScalingMethod.MEANVARIANCE_ALL_FEATURES) {
      FVStatsMeanVarAll stats = new FVStatsMeanVarAll(instances);
      //System.err.println("DEBUG: got stats:\n"+stats);
      normalizer = new PipeScaleMeanVarAll(instances.getDataAlphabet(), stats);
    } else if(scalingMethod == ScalingMethod.MINMAX_ALL_FEATURES) {
      FVStatsMeanVarAll stats = new FVStatsMeanVarAll(instances);
      //System.err.println("DEBUG: got stats:\n"+stats);
      normalizer = new PipeScaleMinMaxAll(instances.getDataAlphabet(), stats);      
    } else {
      throw new GateRuntimeException("Internal error: unexpected scaling method");
    }
    //System.err.println("DEBUG: re-normalizing instances");
    for(Instance inst : instances) {
      inst = normalizer.pipe(inst);
    }
    ArrayList<Pipe> pipeList = pipe.pipes();
    pipeList.add(normalizer);
    //System.out.println("DEBUG normalize: added normalizer pipe " + normalizer);
    //System.out.println("DEBUG pipes after normalization: " + pipe);
  }
  
  @Override
  public int nrInstances() {
    if(instances == null) return 0;
    else return instances.size();
  }

}