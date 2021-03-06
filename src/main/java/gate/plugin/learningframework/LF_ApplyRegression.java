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
import gate.plugin.learningframework.engines.EngineMBServer;
import gate.util.GateRuntimeException;
import java.net.URL;

/**
 * <p>
 * Training, evaluation and application of ML in GATE.</p>
 */
@CreoleResource(name = "LF_ApplyRegression",
        helpURL = "https://gatenlp.github.io/gateplugin-LearningFramework/LF_ApplyRegression",
        comment = "Apply a trained regression model to documents")
public class LF_ApplyRegression extends LearningFrameworkPRBase {

  static final Logger LOGGER = Logger.getLogger(LF_ApplyRegression.class.getCanonicalName());
  private static final long serialVersionUID = 5851732674711579672L;

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
  
  private String serverUrl;

  @RunTime
  @Optional
  @CreoleParameter(comment = "Classify from a server instead of a stored model")
  public void setServerUrl(String url) {
    serverUrl = url;
  }

  public String getServerUrl() {
    return serverUrl;
  }
  
  
////////////////////////////////////////////////////////////////////////////

  private transient Engine engine;

  private URL savedModelDirectoryURL;

  // this is either what the user specifies as the PR parameter, or what we have stored 
  // with the saved model.
  private String targetFeatureToUse; 

  @Override
  public void process(Document doc) {
    if(isInterrupted()) {
      interrupted = false;
      throw new GateRuntimeException("Execution was requested to be interrupted");
    }
    // extract the required annotation sets,
    AnnotationSet inputAS = doc.getAnnotations(getInputASName());
    AnnotationSet instanceAS = inputAS.get(getInstanceType());

    List<ModelApplication> gcs = engine.applyModel(
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

    
    
    ModelApplication.applyClassification(doc, gcs, targetFeatureToUse, outputAS, null);   
  }


  @Override
  public void controllerStarted(Controller controller) {
    if (dataDirectory == null) {
      throw new GateRuntimeException("Parameter dataDirectory not set!");
    }
    if (savedModelDirectoryURL == null || !savedModelDirectoryURL.toExternalForm().equals(dataDirectory.toExternalForm())) {
      savedModelDirectoryURL = dataDirectory;
    }
    if (serverUrl != null && !serverUrl.isEmpty()) {
      engine = new EngineMBServer(dataDirectory, serverUrl);
    } else {
      // Restore the Engine
      engine = Engine.load(savedModelDirectoryURL, getAlgorithmParameters());
      System.out.println("LF-Info: model loaded is now " + engine);

      if (engine.getModel() == null) {
        // This is really only an error if we do not have some kind of wrapped algorithm
        // where the model is handled externally.
        // For now, we just show a warning.
        // throw new GateRuntimeException("Do not have a model, something went wrong.");
        // System.err.println("WARNING: no internal model to apply, this is ok if an external model is used");
        //throw new GateRuntimeException("Do not have a model, something went wrong.");
      } else {
        System.out.println("LearningFramework: Applying model "
                + engine.getModel().getClass() + " ...");
      }

    }

    if (getTargetFeature() == null || getTargetFeature().isEmpty()) {
      // try to get the target feature from the model instead
      String targetFeatureFromModel = engine.getInfo().targetFeature;
      if (targetFeatureFromModel == null || targetFeatureFromModel.isEmpty()) {
        throw new GateRuntimeException("Not targetFeature parameter specified and none available from the model info file either.");
      }
      targetFeatureToUse = targetFeatureFromModel;
      LOGGER.warn("Using target feature name from model: " + targetFeatureToUse);
    } else {
      targetFeatureToUse = getTargetFeature();
      LOGGER.warn("Using target feature name from PR parameter: " + targetFeatureToUse);
    }
  }


}
