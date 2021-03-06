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

import cc.mallet.classify.Classifier;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.topics.TopicModelDiagnostics;
import cc.mallet.types.Instance;
import gate.Annotation;
import gate.AnnotationSet;
import gate.plugin.learningframework.EvaluationMethod;
import gate.plugin.learningframework.ModelApplication;
import static gate.plugin.learningframework.engines.Engine.FILENAME_MODEL;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static gate.plugin.learningframework.LFUtils.newURL;
import gate.plugin.learningframework.data.CorpusRepresentationMalletLDA;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johann Petrak
 */
public class EngineMBTopicsLDA extends EngineMBMallet {

  // not used for now
  // private static final Logger LOGGER = Logger.getLogger(EngineMBTopicsLDA.class);

  protected TopicModelDiagnostics tmd;
  protected ParallelTopicModel tm;
  
  public ParallelTopicModel getTopicModel() {
    return tm;
  }
  public TopicModelDiagnostics getTopicModelDiagnostics() {
    return tmd;
  }
  
  public EngineMBTopicsLDA() { }

  public List<Map<String,Double>>  getTopicWordScores(ParallelTopicModel tm) {
    List<Map<String,Double>> perTopicWord2Score = new ArrayList<>(tm.numTopics);
    for(int i=0; i<tm.numTopics; i++) {
      perTopicWord2Score.add(new HashMap<>());
    }
    // modified from Mallet ParallelTopicModel code
    for(int topicnr=0; topicnr<tm.numTopics; topicnr++) {
      for(int type=0; type<tm.numTypes; type++) {
        int[] topicCounts=tm.typeTopicCounts[type];
        double weight = tm.beta;
        int index = 0;
        while (index < topicCounts.length && topicCounts[index] > 0) {
          // Mallet really stores both the count and the topic number in the 
          // topicCounts variable: the actual counts are in the highest bits 
          // while the topic number is in as many lowest bits as necessary.
          // The topic mask is ones for those lowest bits.
          int currentTopic = topicCounts[index] & tm.topicMask;
          if(currentTopic == topicnr) {
            weight += topicCounts[index] >> tm.topicBits;  // get the actual count
            break;
          }
          index++;
        }
        perTopicWord2Score.get(topicnr).put((String)tm.alphabet.lookupObject(type), weight);
      }
    }
    
    for(int i=0; i<perTopicWord2Score.size(); i++) {
      Map<String,Double> unsortedMap = perTopicWord2Score.get(i);
      // calculate sum
      double sum = unsortedMap.entrySet().stream().mapToDouble(x -> x.getValue()).sum();
      // remap values to 0..1, NOTE: sum should always be > 0!
      unsortedMap.entrySet().stream().forEach(x -> unsortedMap.put(x.getKey(), x.getValue()/sum));
      // sort and store result in a linked hash map
      Map<String,Double> sortedMap = new LinkedHashMap<>();
      unsortedMap.entrySet().stream().
              sorted(Map.Entry.<String, Double>comparingByValue().reversed()).
              forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
      // store the sorted map instead of the original one int th result
      perTopicWord2Score.set(i, sortedMap);
    }
    return perTopicWord2Score;
  }
  
  public void printTopTopicWords(ParallelTopicModel tm, PrintStream pw, int numTopWords) {
    List<Map<String,Double>> perTopicWord2Score = getTopicWordScores(tm);
    for(int topicnr=0; topicnr<tm.numTopics; topicnr++) {
      Map<String,Double> sortedWordScores = perTopicWord2Score.get(topicnr);
      pw.print(topicnr);
      pw.print("(");
      pw.print(String.format(java.util.Locale.US,"%.4f", tm.alpha[topicnr]));
      pw.print(")");
      pw.print(":");
      Iterator<Map.Entry<String,Double>> it = sortedWordScores.entrySet().iterator();
      for(int i=0; i<numTopWords; i++) {
        if(it.hasNext()) {
          Entry<String,Double> entry = it.next();
          pw.print(" ");
          pw.print(entry.getKey());
          pw.print(":");
          pw.print(String.format(java.util.Locale.US,"%.4f", entry.getValue()));
        } else {
          break;
        }
      }
      pw.println();
    }
  }
  
  
  
  @Override
  public void trainModel(File dataDirectory, String instanceType, String parmString) {
    
    // TODO: at this point we could maybe remove low-frequency words.
    // This would be possible by adding a vocab/stats object to count each 
    // entry in the alphabet and then going through the instances, using
    // the FeatureSequence.prune method
    
    
    System.err.println("EngineMalletClass.trainModel: trainer="+trainer);
    System.err.println("EngineMalletClass.trainModel: CR="+corpusRepresentation);
    

    // default values for those which can be set 
    int nrTopics = 10;
    double alpha = 1.0;
    double beta = 0.01;
    int showNrTopWords = 20;
    int showNrDocs = 5;
    int showNrTopics = 10;
    int optimizeInterval = 50;
    boolean doDiagnostics = false;
    int numThreads = Runtime.getRuntime().availableProcessors();
    if(numThreads<1) {
      numThreads = 1;
    }
    int seed = 0;
    int maxCondModesIts = 0;
    int displayInterval = 200;
    int numIterations = 1000;
    int numBurnin = 200;

    Parms parmdef = new Parms(parmString,
                "t:topics:i",
                "T:stopics:i",
                "p:procs:i",
                "w:words:i",
                "d:docs:i",
                "s:seed:i",
                "a:alpha:d",
                "M:mcmi:i",
                "b:beta:d",
                "o:opti:i",
                "D:diags:b",
                "i:iters:i",
                "B:burnin:i",
                "S:show:i"
    );
    nrTopics = (int) parmdef.getValueOrElse("topics", nrTopics);
    alpha = (double) parmdef.getValueOrElse("alpha", alpha);
    beta = (double) parmdef.getValueOrElse("beta", beta);
    showNrTopWords = (int) parmdef.getValueOrElse("words", showNrTopWords);
    showNrDocs = (int) parmdef.getValueOrElse("docs", showNrDocs);
    numThreads = (int) parmdef.getValueOrElse("procs", numThreads);
    seed = (int) parmdef.getValueOrElse("seed", seed);
    maxCondModesIts = (int)parmdef.getValueOrElse("mcmi", maxCondModesIts);
    optimizeInterval = (int)parmdef.getValueOrElse("opti", optimizeInterval);
    doDiagnostics = (boolean)parmdef.getValueOrElse("diags", doDiagnostics);
    numIterations = (int)parmdef.getValueOrElse("iters", numIterations);
    numBurnin = (int)parmdef.getValueOrElse("burn", numBurnin);
    displayInterval = (int)parmdef.getValueOrElse("show", displayInterval);
    
    System.out.println("INFO: running Mallet LDA with parameters: topics="+nrTopics+
            ",alpha="+alpha+",beta="+beta+",words="+showNrTopWords+",procs="+numThreads+
            ",docs="+showNrDocs+",seed="+seed+",maxCondModesIts="+maxCondModesIts+
            ",optimizeInterval="+optimizeInterval+",doDiagnostics"+doDiagnostics+
            ",iters="+numIterations+",burnin="+numBurnin
            );
    
    tm = new ParallelTopicModel(nrTopics, alpha, beta);
    // NOTE: this cauases the model to get saved by the standard mallet serialization process
    model= tm;    
    tm.setTopicDisplay(displayInterval, showNrTopWords);
    tm.setNumThreads(numThreads);
    tm.setNumIterations(numIterations);
    tm.setOptimizeInterval(optimizeInterval);
    tm.setNumIterations(numIterations);
    tm.setBurninPeriod(numBurnin);    
    tm.setRandomSeed(seed);
    // For showing top documents see implementation of 
    // tm.printTopicDocuments(printwriter, showNrDocs);
    tm.addInstances(corpusRepresentation.getRepresentationMallet());
    try {
      System.out.println("INFO: estimating the model...");
      tm.estimate();
      System.out.println("INFO: topics estimation finished");
      if(maxCondModesIts > 0) {
        System.out.println("INFO: maximizing using iterated conditional modes ...");
        tm.maximize(maxCondModesIts);
        System.out.println("INFO: maximizing finished");
      }
    } catch (IOException ex) {
      throw new GateRuntimeException("Exception during training of model", ex);
    }    
    System.out.println();
    System.out.println("Top topic words and their scores:");    
    printTopTopicWords(tm, System.out, showNrTopWords);
    System.out.println();
    
    File topWordsPerTopicFile = new File(dataDirectory, "topWordsPerTopic.txt");
    try  (FileOutputStream fos = new FileOutputStream(topWordsPerTopicFile);
          PrintStream out = new PrintStream(fos) )
    {
      System.out.println("INFO: writing top words per topic to "+topWordsPerTopicFile);
      printTopTopicWords(tm, out, showNrTopWords);
    } catch (IOException ex) {
      throw new GateRuntimeException("Exception during writing of words per topic file", ex);
    }    
    /* replaced by our own method above 
    try {
      // Save the topicKeysFile
      System.out.println("INFO: writing top words per topics to file "+topWordsPerTopicFile);
      tm.printTopWords(topWordsPerTopicFile, showNrTopWords, false);
    } catch (IOException ex) {
      throw new GateRuntimeException("Exception during writing of top words per topic", ex);
    }
    */
    
    File topDocsPerTopicFile = new File(dataDirectory, "topDocsPerTopic.txt");
    try  (PrintWriter out = new PrintWriter(topDocsPerTopicFile) )
    {
      System.out.println("INFO: writing top documents per topic file "+topDocsPerTopicFile);
      tm.printTopicDocuments(out, showNrDocs);
    } catch (FileNotFoundException ex) {
      throw new GateRuntimeException("Exception during writing of docs per topic file", ex);
    }
    
    
    File topTopicPerDocFile = new File(dataDirectory, "topTopicPerDoc.txt");
    try  (PrintWriter out = new PrintWriter(topTopicPerDocFile) )
    {
      System.out.println("INFO: writing top topics per document file "+topTopicPerDocFile);
      tm.printDocumentTopics(out, 0.0, showNrTopics);
    } catch (FileNotFoundException ex) {
      throw new GateRuntimeException("Exception during writing of topics per doc file", ex);
    }
    
    File topicWordWeightsFile = new File(dataDirectory, "topicWordWeights.txt");
    System.out.println("INFO: writing topic word weights to "+topicWordWeightsFile);
    try {
      tm.printTopicWordWeights(topicWordWeightsFile);
    } catch (IOException ex) {
      throw new GateRuntimeException("Exception during writing of topic word weights file", ex);
    }
  
    tmd = null; // if we do not run it, null indicates that we do not need to save it!
    if(doDiagnostics) {
      System.out.println("INFO: calculating topic model diagnostics...");
      tmd = new TopicModelDiagnostics(tm, corpusRepresentation.getRepresentationMallet().getAlphabet().size());
      System.out.println("Topic Model Coherence: "+Arrays.toString(tmd.getCoherence().scores));
      updateInfo();
    }
  }

  @Override
  protected void saveModel(File directory) {
    System.out.println("INFO: saving model");
    super.saveModel(directory);
    // in addition to saving the model, we also write out some additional files
    // generated from the diagnostics
    if(tmd != null) {
      try (
              PrintWriter pw = new PrintWriter(new File(directory, "diagnostics.xml"), "UTF-8");
          )
      {
        System.out.println("INFO: saving diagnostics.xml file");
        pw.print(tmd.toXML());
      } catch (FileNotFoundException | UnsupportedEncodingException ex)  {
        throw new GateRuntimeException("Exception when writing diagnostics.xml",ex);
      }
    }
  }  
  
  
  @Override
  protected void loadAndSetCorpusRepresentation(URL directory) {
    if(corpusRepresentation==null) {
      corpusRepresentation = CorpusRepresentationMalletLDA.load(directory);
    }
  }
  

  @Override
  protected void loadModel(URL directory, String parms) {
    URL modelFile = newURL(directory, FILENAME_MODEL);
    Classifier classifier;
    try (InputStream is = modelFile.openStream();
         ObjectInputStream ois = new ObjectInputStream(is)) {
      ParallelTopicModel ptm = (ParallelTopicModel) ois.readObject();
      model=ptm;
    } catch (IOException | ClassNotFoundException ex) {
      throw new GateRuntimeException("Could not load Mallet model", ex);
    }
  }
  
  
  
  
  @Override
  public List<ModelApplication> applyModel(
          AnnotationSet instanceAS, AnnotationSet inputAS, AnnotationSet sequenceAS, String parms) {
    // NOTE: this generic method cannot be used for LDA since we need to know
    // the token feature. Instead we have an engine specific method (see below)
    // Also, we directly store the results in the instance annotations, instead of
    // returning a model application instance (which only works for classification/regression)
    throw new GateRuntimeException("Method applyModel cannot be used with EngineMBTopicsLDA, use applyTopicModel");
  }
  
  public void applyTopicModel(AnnotationSet instanceAS, AnnotationSet tokenAS,
          String tokenFeature, String featurePrefix, String parms) {
    CorpusRepresentationMalletLDA data = (CorpusRepresentationMalletLDA)corpusRepresentation;
    data.stopGrowth();

    int numIterations = 10;
    int burnIn = 10;
    int thinning = 0;
    int seed = 0;
    Parms parmdef = new Parms(parms,
                "i:iters:i",
                "B:burnin:i",
                "T:thinning:i",
                "s:seed:i"
    );
    numIterations = (int) parmdef.getValueOrElse("iters", numIterations);
    burnIn = (int) parmdef.getValueOrElse("burnin", burnIn);
    thinning = (int) parmdef.getValueOrElse("thinning", thinning);
    seed = (int) parmdef.getValueOrElse("seed", seed);


    ParallelTopicModel tm = (ParallelTopicModel)model;
    TopicInferencer ti = tm.getInferencer();
    tm.setRandomSeed(seed);
    
    for(Annotation instAnn : instanceAS.inDocumentOrder()) {
      // System.err.println("DEBUG: adding instance annotation "+instAnn);
      Instance inst = data.getInstanceFor(gate.Utils.start(instAnn), gate.Utils.end(instAnn), tokenAS, tokenFeature);
      // System.err.println("DEBUG: Instance data is "+inst.getData());
      // System.err.println("DEBUG: got inferencer "+ti);
      // NOTE: see http://mallet.cs.umass.edu/api/cc/mallet/topics/TopicInferencer.html#getSampledDistribution(cc.mallet.types.Instance,%20int,%20int,%20int)
      double[] tdist = ti.getSampledDistribution(inst, numIterations, thinning, burnIn);
      List<Double> tdistlist = new ArrayList<>(tdist.length);
      int i = 0;
      int bestTopic = -1;
      double bestProb = -999.99;
      for(double val : tdist) {
        tdistlist.add(val);
        if(val > bestProb) {
          bestTopic = i;
          bestProb = val;
        }
        i++;
      }
      if(featurePrefix == null) {
        featurePrefix = "";
      }
      instAnn.getFeatures().put(featurePrefix+"TopicDist", tdistlist);    
      // Also add a feature that gives the index and word list of the most likely topic
      instAnn.getFeatures().put(featurePrefix+"BestTopic", bestTopic);
      instAnn.getFeatures().put(featurePrefix+"BestTopicProb", bestProb);
      // TODO: to add the topic words we have to pre-calculate the top k words for each topic
      // and assign the list for topic k here!
      // instAnn.getFeatures().put("LF_MBTopicsLDA_MLTopicWords", bestProb);            
    }
  }
  

  @Override
  public void initializeAlgorithm(Algorithm algorithm, String parms) {
    // NOTE: for LDA we do not do anything in here, everything happens in the train() method
  }

  @Override
  public EvaluationResult evaluate(String algorithmParameters, EvaluationMethod evaluationMethod, int numberOfFolds, double trainingFraction, int numberOfRepeats) {
    throw new GateRuntimeException("Method evaluate not available for EngineMBTopicsLDA");
  }
  

  @Override
  protected void initWhenLoading(URL dir, String parms) {
    super.initWhenLoading(dir, parms);
    corpusRepresentation.stopGrowth();
  }
  
  
}
