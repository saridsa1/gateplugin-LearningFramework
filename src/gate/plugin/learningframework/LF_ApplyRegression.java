/*
 * Copyright (c) 1995-2015, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright 2015 South London and Maudsley NHS Trust and King's College London
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 */
package gate.plugin.learningframework;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import gate.AnnotationSet;
import gate.Controller;
import gate.Document;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.learningframework.engines.Engine;
import gate.util.GateRuntimeException;
import java.net.URL;

/**
 * <p>
 * Training, evaluation and application of ML in GATE.</p>
 */
@CreoleResource(name = "LF_ApplyRegression",
        helpURL = "https://github.com/GateNLP/gateplugin-LearningFramework/wiki/LF_ApplyRegression",
        comment = "Apply a trained regression model to documents")
public class LF_ApplyRegression extends LearningFrameworkPRBase {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  static final Logger logger = Logger.getLogger(LF_ApplyRegression.class.getCanonicalName());

  protected URL dataDirectory;

  @RunTime
  @CreoleParameter(comment = "The directory where all data will be stored and read from")
  public void setDataDirectory(URL output) {
    dataDirectory = output;
  }

  public URL getDataDirectory() {
    return this.dataDirectory;
  }

  
  
  protected String outputASName;

  @RunTime
  @Optional
  @CreoleParameter(comment="If identical to the input AS, existing annotations are updated", 
          defaultValue = "LearningFramework")
  public void setOutputASName(String oasn) {
    this.outputASName = oasn;
  }

  public String getOutputASName() {
    return this.outputASName;
  }


  protected String targetFeature;

  // TODO: we want to get rid of this and read this name from the info file!!
  @RunTime
  @Optional
  @CreoleParameter(comment = "Name of class feature to add to the original "
          + "instance annotations",
          defaultValue = "")
  public void setTargetFeature(String name) {
    targetFeature = name;
  }

  public String getTargetFeature() {
    return targetFeature;
  }
  
  protected String instanceWeightFeature = "";
  /*
  @RunTime
  @Optional
  @CreoleParameter(comment = "The feature that constains the instance weight. If empty, no instance weights are used",
          defaultValue="")
  public void setInstanceWeightFeature(String val) {
    instanceWeightFeature = val;
  }
  public String getInstanceWeightFeature() { return instanceWeightFeature; }
  */

////////////////////////////////////////////////////////////////////////////

  private Engine engine;

  private File savedModelDirectoryFile;

  // this is either what the user specifies as the PR parameter, or what we have stored 
  // with the saved model.
  private String targetFeatureToUse; 

  @Override
  public Document process(Document doc) {
    if(isInterrupted()) {
      interrupted = false;
      throw new GateRuntimeException("Execution was requested to be interrupted");
    }
    // extract the required annotation sets,
    AnnotationSet inputAS = doc.getAnnotations(getInputASName());
    AnnotationSet instanceAS = inputAS.get(getInstanceType());

    List<GateClassification> gcs = engine.classify(
          instanceAS, inputAS,
          null, getAlgorithmParameters());

    // If the outputSet is the same as the inputSet, we do not create new 
    // annotations
    // So if they are both null or both the same non-null value we leave the outputAS null, otherwise we 
    // set it to something (in the case of null, the default set).
    AnnotationSet outputAS = null;
    if(getOutputASName()==null && getInputASName()==null || 
       getOutputASName()!= null && getInputASName() != null && getOutputASName().equals(getInputASName())) {
    } else {
      outputAS = doc.getAnnotations(getOutputASName());
    }

    
    
    GateClassification.applyClassification(doc, gcs, targetFeatureToUse, outputAS, null);   
    return doc;
  }


  @Override
  public void afterLastDocument(Controller arg0, Throwable throwable) {
    // No need to do anything, empty implementation!
  }

  public void finishedNoDocument(Controller arg0, Throwable throwable) {
    // no need to do anything
  }

  @Override
  protected void beforeFirstDocument(Controller controller) {

    // JP: this was moved from the dataDirectory setter to avoid problems
    // but we should really make sure that the learning is reloaded only 
    // if the URL has changed since the last time (if ever) it was loaded.
    savedModelDirectoryFile = gate.util.Files.fileFromURL(dataDirectory);

    // Restore the Engine
    engine = Engine.loadEngine(savedModelDirectoryFile, getAlgorithmParameters());
    System.out.println("LF-Info: model loaded is now "+engine);

    if (engine.getModel() == null) {
      throw new GateRuntimeException("Do not have a model, something went wrong.");
    } else {
      System.out.println("LearningFramework: Applying model "
              + engine.getModel().getClass() + " ...");
    }
    
    if(getTargetFeature()==null || getTargetFeature().isEmpty()) {
      // try to get the target feature from the model instead
      String targetFeatureFromModel = engine.getInfo().targetFeature;
      if(targetFeatureFromModel == null || targetFeatureFromModel.isEmpty()) {
        throw new GateRuntimeException("Not targetFeature parameter specified and none available from the model info file either.");
      }
      targetFeatureToUse = targetFeatureFromModel;
      System.err.println("Using target feature name from model: "+targetFeatureToUse);
    } else {
      targetFeatureToUse = getTargetFeature();
      System.err.println("Using target feature name from PR parameter: "+targetFeatureToUse);
    }
  }

}
