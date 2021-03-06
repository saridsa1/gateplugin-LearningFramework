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

package gate.plugin.learningframework.tests;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.data.CorpusRepresentationVolatileDense2JsonStream;
import gate.plugin.learningframework.data.InstanceRepresentation;
import static gate.plugin.learningframework.features.FeatureExtractionBase.featureName;
import static gate.plugin.learningframework.features.FeatureExtractionBase.featureSpecAttributes2FeatureNames;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecAttribute;
import gate.plugin.learningframework.features.FeatureSpecification;
import gate.plugin.learningframework.features.TargetType;
import static gate.plugin.learningframework.tests.Utils.*;
import gate.util.GateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import static org.hamcrest.CoreMatchers.instanceOf;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import gate.test.GATEPluginTests;


/**
 * Tests extracting features to a dense Instance representation
 * 
 * @author Johann Petrak
 */
public class TestCorpusRepresentationVD2JS extends GATEPluginTests {
  
  
  @BeforeClass
  public static void setup() throws ResourceInstantiationException, GateException {
    Gate.init();
  }
  
  private Document doc;
  
  @Before
  public void before() throws ResourceInstantiationException {
    doc = newDocument();
  }
  
  @After
  public void after() {
    if(doc != null)
      Factory.deleteResource(doc);
  }
  
  @Test  
  public void json4metadata1() throws IOException {
    // NOTE: so far this is mainly here so we can print out the JSON if needed,
    // not any useful tests really
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTELIST><FROM>-3</FROM><TO>2</TO><TYPE>theType</TYPE><FEATURE>feature2</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTELIST>"+
            "<NGRAM><NUMBER>2</NUMBER><TYPE>theType</TYPE><FEATURE>ngramfeature1</FEATURE></NGRAM>"+
            "<ATTRIBUTELIST><FROM>-1</FROM><TO>1</TO><FEATURE>feature3</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTELIST>"+
            "<ATTRIBUTE><TYPE>someOtherType</TYPE></ATTRIBUTE>"+
            "</ROOT>";    
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    // NOTE: just creating the instance will overwrite any other meta and data files created in the same directory
    CorpusRepresentationVolatileDense2JsonStream cr = new CorpusRepresentationVolatileDense2JsonStream(TESTS_DIR, fi);
    String json = "";
    try 
      (StringWriter sw = new StringWriter()) {
       cr.json4metadata(sw);
       json = sw.toString();
    } 
    //System.err.println("TestCorpusRepresentation/json4metadata1 Debug: json="+json);
    assertTrue(json.contains("someOtherType┆╬A"));
  }  

  @Test
  @SuppressWarnings("unchecked")
  public void json4stuff1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTELIST><FROM>-3</FROM><TO>2</TO><FEATURE>feature2</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTELIST>"+
            "<NGRAM><NUMBER>2</NUMBER><TYPE>theType</TYPE><FEATURE>ngramstring</FEATURE></NGRAM>"+
            "<ATTRIBUTELIST><FROM>-1</FROM><TO>1</TO><FEATURE>feature3</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTELIST>"+
            "<ATTRIBUTE><TYPE>someNonExistingType</TYPE></ATTRIBUTE>"+
            "<NGRAM><NUMBER>1</NUMBER><FEATURE>ngramstring</FEATURE><TYPE>theType</TYPE></NGRAM>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<FeatureSpecAttribute> as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(6,as.size());
    // get the actual feature name list generated by that specification
    List<FeatureSpecAttribute> attrs = fi.getAttributes();
    List<String> fnames = featureSpecAttributes2FeatureNames(attrs);
    assertNotNull(fnames);
    assertEquals(13, fnames.size());
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: fnames size="+fnames.size());    
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: fnames="+fnames);    
    
    // prepare the document and 3 successive annotations 
    Annotation instAnn1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap());
    instAnn1.getFeatures().put("theFeature", "ann1feature1");
    instAnn1.getFeatures().put("feature2", "ann1feature2");
    instAnn1.getFeatures().put("ngramstring", "ann1feature3");
    instAnn1.getFeatures().put("feature3", "1.3");  // 1.3 == ann1feature3
    instAnn1.getFeatures().put("target", "ann1target");

    Annotation instAnn2 = addAnn(doc, "", 10, 15, "theType", gate.Utils.featureMap());
    instAnn2.getFeatures().put("theFeature", "ann2feature1");
    instAnn2.getFeatures().put("feature2", "ann2feature2");
    instAnn2.getFeatures().put("ngramstring", "ann2feature3");
    instAnn2.getFeatures().put("feature3", "2.3");  // 2.3 == ann2feature3
    instAnn2.getFeatures().put("target", "ann2target");

    Annotation instAnn3 = addAnn(doc, "", 20, 25, "theType", gate.Utils.featureMap());
    instAnn3.getFeatures().put("theFeature", "ann3feature1");
    instAnn3.getFeatures().put("feature2", "ann3feature2");
    instAnn3.getFeatures().put("ngramstring", "ann3eature3");
    instAnn3.getFeatures().put("feature3", "3.3");  // 3.3 == ann3feature3
    instAnn3.getFeatures().put("target", "ann3target");

    // NOTE: just creating the instance will overwrite any other meta and data files created in the same directory
    CorpusRepresentationVolatileDense2JsonStream cr = new CorpusRepresentationVolatileDense2JsonStream(TESTS_DIR, fi);
    AnnotationSet inputAS = doc.getAnnotations();
    AnnotationSet instancesAS = inputAS.get("theType");
    AnnotationSet sequenceAS = null;
    AnnotationSet classAS = null;
    String targetFeatureName = "target";
    String weightFeature = null;
    String nameFeature = null;
    /*
    //instead of testing the full add method, test the individual steps 
    // below!!
    cr.add(
            instancesAS, sequenceAS, inputAS, classAS, 
            targetFeatureName, TargetType.NOMINAL, 
            weightFeature, nameFeature, null);
    */
    InstanceRepresentation inst1 = cr.labeledAnnotation2Instance(instAnn1,inputAS, classAS,
          targetFeatureName, TargetType.NOMINAL,
          weightFeature, null /* seqEncoder */);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst1 size="+inst1.numFeatures());    
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst1="+inst1);    
    assertEquals(fnames.size(), inst1.numFeatures());
    InstanceRepresentation inst2 = cr.labeledAnnotation2Instance(instAnn2,inputAS, classAS,
          targetFeatureName, TargetType.NOMINAL,
          weightFeature, null /* seqEncoder */);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst2 size="+inst2.numFeatures());    
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst2="+inst2);    
    InstanceRepresentation inst3 = cr.labeledAnnotation2Instance(instAnn3,inputAS, classAS,
          targetFeatureName, TargetType.NOMINAL,
          weightFeature, null /* seqEncoder */);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst3 size="+inst3.numFeatures());    
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst3="+inst3);    
    
    // Now convert the instances to JSON and see if that works
    String inst1_json = cr.internal2Json(inst1,false);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: inst1_json="+inst1_json);   
    
    
    // Also try longer ngrams and multiple instance annotations 
    Annotation longAnn = addAnn(doc, "", 0, 25, "longType", gate.Utils.featureMap());
    longAnn.getFeatures().put("target", "ann1target");
    longAnn.getFeatures().put("theFeature", "ann1feature1");
    longAnn.getFeatures().put("feature2", "ann1feature2");
    longAnn.getFeatures().put("ngramstring", "ann1feature3");
    longAnn.getFeatures().put("feature3", "1.3");  // 1.3 == ann1feature3
    
    InstanceRepresentation instLong1 = cr.labeledAnnotation2Instance(longAnn,inputAS, classAS,
          targetFeatureName, TargetType.NOMINAL,
          weightFeature, null /* seqEncoder */);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: instLong1="+instLong1);   
    String instLong1_json = cr.internal2Json(instLong1,false);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: instLong1_json="+instLong1_json);   
    assertEquals(fnames.size(),instLong1.numFeatures());
    for(int i=0;i<fnames.size();i++) {
      assertTrue(instLong1.hasFeature(fnames.get(i)));
    }
    assertEquals("ann1feature1",instLong1.getFeature(featureName(attrs.get(0),0)));
    assertThat(instLong1.getFeature(featureName(attrs.get(2),0)),instanceOf(List.class));
    List<String> lfeature;
    lfeature = (List<String>) instLong1.getFeature(featureName(attrs.get(2),0)); // first ngram
    assertEquals(2,lfeature.size());
    lfeature = (List<String>) instLong1.getFeature(featureName(attrs.get(5),0)); // second ngram
    assertEquals(3,lfeature.size());

    // For actually looking at the file written, uncomment this
    /*
    cr.add(
            instancesAS, sequenceAS, inputAS, classAS, 
            targetFeatureName, TargetType.NOMINAL, 
            weightFeature, nameFeature, null);
    cr.finish();
    */
    
    // finally, use the long annotation as a sequence annotation
    sequenceAS = doc.getAnnotations().get("longType");
    assertEquals(1,sequenceAS.size());
    
    List<InstanceRepresentation> insts4seq = 
            cr.instancesForSequence(instancesAS, longAnn, inputAS, classAS, targetFeatureName, TargetType.NOMINAL, null);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: insts4seq="+insts4seq);   
    assertEquals(3, insts4seq.size());
    String json = cr.internal2Json(insts4seq,false);
    //System.err.println("TestCorpusRepresentationVD2JS/json4stuff Debug: insts4seq_json="+json);   

    /*
    cr.add(instancesAS, sequenceAS, inputAS, classAS, 
            targetFeatureName, TargetType.NOMINAL, weightFeature, nameFeature, null);
    cr.finish();
    */
  }
  
 
}
