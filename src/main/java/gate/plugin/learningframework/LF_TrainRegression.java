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
package gate.plugin.learningframework;

import gate.Annotation;
import gate.AnnotationSet;
import java.net.URL;

import org.apache.log4j.Logger;

import gate.Controller;
import gate.Document;
import gate.FeatureMap;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.learningframework.data.CorpusRepresentation;
import gate.plugin.learningframework.data.CorpusRepresentationMallet;
import gate.plugin.learningframework.engines.AlgorithmRegression;
import gate.plugin.learningframework.engines.Engine;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import gate.plugin.learningframework.features.TargetType;
import gate.util.GateRuntimeException;
import java.io.File;

@CreoleResource(
        name = "LF_TrainRegression",
        helpURL = "https://gatenlp.github.io/gateplugin-LearningFramework/LF_TrainRegression",
        comment = "Train a machine learning model for regression")
public class LF_TrainRegression extends LearningFrameworkPRBase {

  private static final long serialVersionUID = 3354214881596583124L;

  private final Logger logger = Logger.getLogger(LF_TrainRegression.class.getCanonicalName());

  protected URL dataDirectory;

  @RunTime
  @CreoleParameter(comment = "The directory where all data will be stored and read from")
  public void setDataDirectory(URL output) {
    dataDirectory = output;
  }

  public URL getDataDirectory() {
    return this.dataDirectory;
  }

  protected String instanceWeightFeature = "";
  @RunTime
  @Optional
  @CreoleParameter(comment = "The feature that constains the instance weight. If empty, no instance weights are used",
          defaultValue="")
  public void setInstanceWeightFeature(String val) {
    instanceWeightFeature = val;
  }
  public String getInstanceWeightFeature() { return instanceWeightFeature; }
  
  
  
  
  /**
   * The configuration file.
   *
   */
  private java.net.URL featureSpecURL;

  @RunTime
  @CreoleParameter(comment = "The feature specification file.")
  public void setFeatureSpecURL(URL featureSpecURL) {
    this.featureSpecURL = featureSpecURL;
  }

  public URL getFeatureSpecURL() {
    return featureSpecURL;
  }

  private AlgorithmRegression trainingAlgorithm;

  @RunTime
  @Optional
  @CreoleParameter(comment = "The algorithm to be used for training.")
  public void setTrainingAlgorithm(AlgorithmRegression algo) {
    this.trainingAlgorithm = algo;
  }

  public AlgorithmRegression getTrainingAlgorithm() {
    return this.trainingAlgorithm;
  }

  protected ScalingMethod scaleFeatures = ScalingMethod.NONE;

  @RunTime
  @CreoleParameter(defaultValue = "NONE", comment = "If and how to scale features. ")
  public void setScaleFeatures(ScalingMethod sf) {
    scaleFeatures = sf;
  }

  public ScalingMethod getScaleFeatures() {
    return scaleFeatures;
  }

  protected String targetFeature;

  @RunTime
  @Optional
  @CreoleParameter(comment = "The feature containing the target value")
  public void setTargetFeature(String classFeature) {
    this.targetFeature = classFeature;
  }

  public String getTargetFeature() {
    return this.targetFeature;
  }

  private CorpusRepresentation corpusRepresentation = null;
  private FeatureSpecification featureSpec = null;

  private Engine engine = null;

  private File dataDirFile;

  @Override
  public void process(Document doc) {
    if(isInterrupted()) {
      interrupted = false;
      throw new GateRuntimeException("Execution was requested to be interrupted");
    }
    // extract the required annotation sets,
    AnnotationSet inputAS = doc.getAnnotations(getInputASName());
    AnnotationSet instanceAS = inputAS.get(getInstanceType());
    // the classAS is always null for the regression task!
    // the sequenceAS is always null for the regression task!
    // the nameFeatureName is always null for now!
    String nameFeatureName = null;
    for(Annotation inst : instanceAS) {
      FeatureMap fm = inst.getFeatures();
      Object targetVal = fm.get(getTargetFeature());
      if(null==targetVal) {
        throw new GateRuntimeException("Target value is null in document "+document.getName()+" for instance "+inst);
      }
    }
    
    corpusRepresentation.add(instanceAS, null, inputAS, null, getTargetFeature(), TargetType.NUMERIC, instanceWeightFeature, nameFeatureName, null);
  }
  
  @Override
  public void controllerStarted(Controller controller) {
    if("file".equals(dataDirectory.getProtocol())) {
      dataDirFile = gate.util.Files.fileFromURL(dataDirectory);
    } else {
      throw new GateRuntimeException("Training is only possible if the dataDirectory URL is a file: URL");
    }
    if(!dataDirFile.exists()) {
      throw new GateRuntimeException("Data directory not found: "+dataDirFile.getAbsolutePath());
    }

    if (getTrainingAlgorithm() == null) {
      throw new GateRuntimeException("LearningFramework: no training algorithm specified");
    }
    // AlgorithmRegression alg = getTrainingAlgorithm();
    // System.err.println("DEBUG: Before Document.");
    //System.err.println("  Training algorithm engine class is " + alg.getEngineClass());
    //System.err.println("  Training algorithm algor class is " + alg.getTrainerClass());

    if (getDuplicateId() == 0) {
      // Read and parse the feature specification
      featureSpec = new FeatureSpecification(featureSpecURL);
      // System.err.println("DEBUG Read the feature specification: " + featureSpec);
      // Create the engine from the Algorithm parameter
      FeatureInfo fi = featureSpec.getFeatureInfo();
      fi.setGlobalScalingMethod(scaleFeatures);
      engine = Engine.create(trainingAlgorithm, getAlgorithmParameters(), fi, TargetType.NUMERIC, dataDirectory);
      corpusRepresentation = engine.getCorpusRepresentation();
      // System.err.println("DEBUG: created the engine: " + engine);
      getSharedData().put("engine", engine);
      getSharedData().put("featureSpec", featureSpec);
      getSharedData().put("corpusRepresentation", corpusRepresentation);
    } else {
      // duplicateId > 0
      engine = (Engine) getSharedData().get("engine");
      featureSpec = (FeatureSpecification) getSharedData().get("featureSpec");
      corpusRepresentation = (CorpusRepresentation) getSharedData().get("corpusRepresentation");
    }
  }
  

  @Override
  public void controllerFinished(Controller arg0, Throwable t) {
    if(t!=null) {
      System.err.println("An exception occurred during processing of documents, no training will be done");
      System.err.println("Exception was "+t.getClass()+": "+t.getMessage());
      return;
    }
    if(getSeenDocuments().get()==0) {
      throw new GateRuntimeException("No documents seen, cannot train");
    }
    if (getDuplicateId() == 0) {
      System.out.println("LearningFramework: Starting training engine " + engine);
      if (corpusRepresentation instanceof CorpusRepresentationMallet) {
        CorpusRepresentationMallet crm = (CorpusRepresentationMallet) corpusRepresentation;
        System.out.println("Training set size: " + crm.getRepresentationMallet().size());
        if (crm.getRepresentationMallet().getDataAlphabet().size() > 20) {
          System.out.println("LearningFramework: Attributes " + crm.getRepresentationMallet().getDataAlphabet().size());
        } else {
          System.out.println("LearningFramework: Attributes " + crm.getRepresentationMallet().getDataAlphabet().toString().replaceAll("\\n", " "));
        }
      }

      engine.getInfo().nrTrainingInstances = corpusRepresentation.nrInstances();

      // Store some additional information in the info datastructure which will be saved with the model
      engine.getInfo().nrTrainingDocuments = getSeenDocuments().get();
      engine.getInfo().targetFeature = getTargetFeature();
      engine.getInfo().trainingCorpusName = corpus.getName();

      engine.trainModel(gate.util.Files.fileFromURL(dataDirectory),
              getInstanceType(),
              getAlgorithmParameters());
      logger.info("LearningFramework: Training complete!");
      engine.saveEngine(dataDirFile);
    }
  }

}
