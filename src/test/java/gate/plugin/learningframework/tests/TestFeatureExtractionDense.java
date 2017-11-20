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
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.data.InstanceRepresentation;
import gate.plugin.learningframework.data.InstanceRepresentationDenseVolatile;
import gate.plugin.learningframework.features.FeatureExtractionDense;
import static gate.plugin.learningframework.features.FeatureExtractionBase.*;
import gate.plugin.learningframework.features.FeatureSpecAttribute;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import static gate.plugin.learningframework.tests.Utils.*;
import gate.util.GateException;
import java.util.List;
import static org.hamcrest.CoreMatchers.instanceOf;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;


/**
 * Tests extracting features to a dense Instance representation
 * 
 * @author Johann Petrak
 */
public class TestFeatureExtractionDense {
  
  private static final String specAttrNoFeature = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
  private static final String specAttrNominalAsNum = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><CODEAS>number</CODEAS></ATTRIBUTE>"+
            "</ROOT>";
  private static final String specAttrNumeric = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
  
  
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
  public void basicInstanceRepresentationDenseVolatile1() {
    InstanceRepresentation inst = new InstanceRepresentationDenseVolatile();
    System.err.println("TestFeatureExtractionDense DEBUG: empty inst="+inst);
    assertFalse(inst.hasFeature("someFeature"));
    assertNull(inst.getFeature("someFeature"));
    inst.setFeature("someFeature",22);
    System.err.println("TestFeatureExtractionDense DEBUG: one feature="+inst);
    assertTrue(inst.hasFeature("someFeature"));
    assertEquals(22, inst.getFeature("someFeature"));
    assertNull(inst.getTargetValue());
    assertFalse(inst.hasTarget());
    inst.setTargetValue("asd");
    System.err.println("TestFeatureExtractionDense DEBUG: target added="+inst);
    assertNotNull(inst.getTargetValue());
    assertTrue(inst.hasTarget());
  }
  
  
  @Test
  public void extractSimple1() {    
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature2</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>numfeature1</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>numfeature2</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>boolfeature1</FEATURE><DATATYPE>boolean</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>boolfeature2</FEATURE><DATATYPE>bool</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE></ATTRIBUTE>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<FeatureSpecAttribute> as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(7,as.size());

    InstanceRepresentation inst = new InstanceRepresentationDenseVolatile();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 0, "theType", gate.Utils.featureMap());
    instAnn.getFeatures().put("theFeature", "value1");
    instAnn.getFeatures().put("feature2", "valOfFeature2");
    instAnn.getFeatures().put("numfeature1", 1.1);
    instAnn.getFeatures().put("numfeature2", "2.2");
    instAnn.getFeatures().put("boolfeature1", true);
    instAnn.getFeatures().put("boolfeature2", 3.3);
    
    // 1) the following all specify the same instance annotation type as is specified in the 
    // attribute so the instance annotation should directly get used.
    
    inst = FeatureExtractionDense.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(0)+" listsep="+as.get(0).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 0="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","theFeature")));
    assertThat(inst.getFeature(featureName4Attribute("","theFeature")), instanceOf(String.class));
    assertEquals("value1",inst.getFeature(featureName4Attribute("","theFeature")));

    inst = FeatureExtractionDense.extractFeature(inst, as.get(1), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(1)+" listsep="+as.get(1).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 1="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","feature2")));
    assertThat(inst.getFeature(featureName4Attribute("","feature2")), instanceOf(String.class));
    assertEquals("valOfFeature2",inst.getFeature(featureName4Attribute("","feature2")));

    inst = FeatureExtractionDense.extractFeature(inst, as.get(2), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(2)+" listsep="+as.get(2).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 2="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","numfeature1")));
    assertThat(inst.getFeature(featureName4Attribute("","numfeature1")), instanceOf(Double.class));
    assertEquals(1.1,inst.getFeature(featureName4Attribute("","numfeature1")));

    inst = FeatureExtractionDense.extractFeature(inst, as.get(3), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(3)+" listsep="+as.get(3).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 3="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","numfeature2")));
    assertThat(inst.getFeature(featureName4Attribute("","numfeature2")), instanceOf(Double.class));
    assertEquals(2.2,inst.getFeature(featureName4Attribute("","numfeature2")));

    inst = FeatureExtractionDense.extractFeature(inst, as.get(4), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(4)+" listsep="+as.get(4).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 4="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","boolfeature1")));
    assertThat(inst.getFeature(featureName4Attribute("","boolfeature1")), instanceOf(Boolean.class));
    assertEquals(true,inst.getFeature(featureName4Attribute("","boolfeature1")));

    inst = FeatureExtractionDense.extractFeature(inst, as.get(5), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(5)+" listsep="+as.get(5).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 5="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","boolfeature2")));
    assertThat(inst.getFeature(featureName4Attribute("","boolfeature2")), instanceOf(Boolean.class));
    assertEquals(true,inst.getFeature(featureName4Attribute("","boolfeature2")));

    inst = FeatureExtractionDense.extractFeature(inst, as.get(6), doc.getAnnotations(), instAnn);
    System.err.println("TestFeatyreExtractionDense Debug: adding attr="+as.get(6)+" listsep="+as.get(6).listsep);
    System.err.println("TestFeatureExtractionDense DEBUG: extracted 6="+inst);
    assertNotNull(inst.getFeature(featureName4Attribute("","")));
    assertThat(inst.getFeature(featureName4Attribute("","")), instanceOf(Boolean.class));
    assertEquals(true,inst.getFeature(featureName4Attribute("","")));

  }  

  /*
  // @Test(expected = GateRuntimeException.class)
  public void extractSimple2() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature1</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    List<FeatureSpecAttribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 10, "instanceType", gate.Utils.featureMap());
    Annotation tok1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","f1v1"));
    tok1.getFeatures().put("feature2", "valOfFeature2");
    Annotation tok2 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","f1v2"));
    tok2.getFeatures().put("feature2", "valOfFeature2B");
    
    // We do not allow more than one overlapping annotation of the given type for ATTRIBUTE
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
  }  

  @Test
  public void extractSimple3() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature1</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    List<FeatureSpecAttribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 10, "instanceType", gate.Utils.featureMap());
    Annotation tok1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","f1v1"));
    tok1.getFeatures().put("feature2", "valOfFeature2");
    
    // We do not allow more than one overlapping annotation of the given type for ATTRIBUTE
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (overlapping) FV="+inst.getData());
  }  


  
  @Test
  public void extractNgram1() {
    String spec = "<ROOT>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>1</NUMBER></NGRAM>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>2</NUMBER></NGRAM>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>3</NUMBER></NGRAM>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<FeatureSpecAttribute> as = fi.getAttributes();

    Alphabet a = new Alphabet();
    AugmentableFeatureVector afv = new AugmentableFeatureVector(a);
    Instance inst = new Instance(afv,null,null,null);
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 20, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (one-grams) FV="+inst.getData());
    assertEquals(5,inst.getAlphabet().size());
    System.err.println("Alphabet N1="+inst.getAlphabet());
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N1═tok1"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N1═tok2"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N1═tok3"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N1═tok4"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N1═tok5"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N1═tok1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N1═tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N1═tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N1═tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N1═tok5"),EPS);
    
    // now the bigrams
    inst = newInstance();
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(1), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(1)+" (bi-grams) FV="+inst.getData());
    System.err.println("Alphabet N2="+inst.getAlphabet());
    assertEquals(4,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N2═tok1┋tok2"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N2═tok2┋tok3"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N2═tok3┋tok4"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N2═tok4┋tok5"));
    assertEquals(4,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N2═tok1┋tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N2═tok2┋tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N2═tok3┋tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N2═tok4┋tok5"),EPS);

    // and the 3-grams
    inst = newInstance();
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(2), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(2)+" (tri-grams) FV="+inst.getData());
    System.err.println("Alphabet N3="+inst.getAlphabet());
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N3═tok1┋tok2┋tok3"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N3═tok2┋tok3┋tok4"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬N3═tok3┋tok4┋tok5"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N3═tok1┋tok2┋tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N3═tok2┋tok3┋tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬N3═tok3┋tok4┋tok5"),EPS);
  }

  @Test
  public void extractNgram2() {
    // essentially the same as extractNgram1 but explicitly specifies the name to use as internal
    // feature name
    String spec = "<ROOT>"+
            "<NGRAM><NAME>ng1</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>1</NUMBER></NGRAM>"+
            "<NGRAM><NAME>ngram2</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>2</NUMBER></NGRAM>"+
            "<NGRAM><NAME>someName</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>3</NUMBER></NGRAM>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<FeatureSpecAttribute> as = fi.getAttributes();
    System.err.println("NGRAMS with explicitly specified name!!");
    Alphabet a = new Alphabet();
    AugmentableFeatureVector afv = new AugmentableFeatureVector(a);
    Instance inst = new Instance(afv,null,null,null);
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 20, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (one-grams) FV="+inst.getData());
    assertEquals(5,inst.getAlphabet().size());
    System.err.println("Alphabet N3="+inst.getAlphabet());
    assertTrue(inst.getAlphabet().contains("ng1╬N1═tok1"));
    assertTrue(inst.getAlphabet().contains("ng1╬N1═tok2"));
    assertTrue(inst.getAlphabet().contains("ng1╬N1═tok3"));
    assertTrue(inst.getAlphabet().contains("ng1╬N1═tok4"));
    assertTrue(inst.getAlphabet().contains("ng1╬N1═tok5"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1╬N1═tok1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1╬N1═tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1╬N1═tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1╬N1═tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1╬N1═tok5"),EPS);
    
    // now the bigrams
    inst = newInstance();
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(1), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(1)+" (bi-grams) FV="+inst.getData());
    System.err.println("Alphabet N4="+inst.getAlphabet());
    assertEquals(4,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("ngram2╬N2═tok1┋tok2"));
    assertTrue(inst.getAlphabet().contains("ngram2╬N2═tok2┋tok3"));
    assertTrue(inst.getAlphabet().contains("ngram2╬N2═tok3┋tok4"));
    assertTrue(inst.getAlphabet().contains("ngram2╬N2═tok4┋tok5"));
    assertEquals(4,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2╬N2═tok1┋tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2╬N2═tok2┋tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2╬N2═tok3┋tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2╬N2═tok4┋tok5"),EPS);

    // and the 3-grams
    inst = newInstance();
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(2), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(2)+" (bi-grams) FV="+inst.getData());
    System.err.println("Alphabet N4="+inst.getAlphabet());
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("someName╬N3═tok1┋tok2┋tok3"));
    assertTrue(inst.getAlphabet().contains("someName╬N3═tok2┋tok3┋tok4"));
    assertTrue(inst.getAlphabet().contains("someName╬N3═tok3┋tok4┋tok5"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("someName╬N3═tok1┋tok2┋tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("someName╬N3═tok2┋tok3┋tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("someName╬N3═tok3┋tok4┋tok5"),EPS);
  }

  
  @Test
  public void extractList1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTELIST><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-1</FROM><TO>1</TO></ATTRIBUTELIST>"+
            "</ROOT>";
    List<FeatureSpecAttribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 10, 11, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    addAnn(doc,"",10,12,"theType",gate.Utils.featureMap("theFeature","tok6"));
    addAnn(doc,"",12,14,"theType",gate.Utils.featureMap("theFeature","tok7"));
    addAnn(doc,"",14,16,"theType",gate.Utils.featureMap("theFeature","tok8"));
    addAnn(doc,"",16,18,"theType",gate.Utils.featureMap("theFeature","tok9"));
    addAnn(doc,"",18,20,"theType",gate.Utils.featureMap("theFeature","tok10"));
    
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (list -1to1) FV="+inst.getData());
    System.err.println("Alphabet L1="+inst.getAlphabet());
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬L-1═tok5"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬L0═tok6"));
    assertTrue(inst.getAlphabet().contains("theType┆theFeature╬L1═tok7"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬L-1═tok5"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬L0═tok6"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆theFeature╬L1═tok7"),EPS);
  }
  
  @Test
  public void extractList2() {
    // same as extractList2, but with explicitly specified name
    String spec = "<ROOT>"+
            "<ATTRIBUTELIST><NAME>myAttList</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-2</FROM><TO>2</TO></ATTRIBUTELIST>"+
            "</ROOT>";
    List<FeatureSpecAttribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 10, 12, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    addAnn(doc,"",10,12,"theType",gate.Utils.featureMap("theFeature","tok6"));
    addAnn(doc,"",12,14,"theType",gate.Utils.featureMap("theFeature","tok7"));
    addAnn(doc,"",14,16,"theType",gate.Utils.featureMap("theFeature","tok8"));
    addAnn(doc,"",16,18,"theType",gate.Utils.featureMap("theFeature","tok9"));
    addAnn(doc,"",18,20,"theType",gate.Utils.featureMap("theFeature","tok10"));
    Annotation withinAnn = addAnn(doc,"",8,14,"within",gate.Utils.featureMap());
    
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (list -1to1) FV="+inst.getData());
    System.err.println("Alphabet L2="+inst.getAlphabet());
    assertEquals(5,inst.getAlphabet().size());
    System.err.println("Alphabet is "+inst.getAlphabet());
    FeatureVector fv = (FeatureVector)inst.getData();
    System.err.println("extractList2-all: "+fv.toString(true));
    assertTrue(inst.getAlphabet().contains("myAttList╬L-2═tok4"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L-1═tok5"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L0═tok6"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L1═tok7"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L2═tok8"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L-2═tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L-1═tok5"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L0═tok6"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L1═tok7"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L2═tok8"),EPS);
    
    // Do the test again, but this time with a declaration that limits it to within the within annotation
    spec = "<ROOT>"+
            "<ATTRIBUTELIST><NAME>myAttList</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-1</FROM><TO>1</TO><WITHIN>within</WITHIN></ATTRIBUTELIST>"+
            "</ROOT>";
    as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    inst = newInstance();
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    fv = (FeatureVector)inst.getData();
    System.err.println("extractList2-within: "+fv.toString(true));
    assertEquals(5,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("myAttList╬L-1═tok5"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L0═tok6"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L1═tok7"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L-1═╔START╗"));
    assertTrue(inst.getAlphabet().contains("myAttList╬L1═╔STOP╗"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L-1═tok5"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L0═tok6"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L1═tok7"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList╬L-1═╔START╗"),EPS);
  }
  
  // Test extracting a nominal attribute where the annotation feature is a collection
  @Test
  public void extractSimpleList1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature1</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    List<FeatureSpecAttribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 10, "instanceType", gate.Utils.featureMap());
    HashSet<String> v1 = new HashSet<String>();
    v1.add("setval1");
    v1.add("setval2");
    v1.add("setval3");
    Annotation tok1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1",v1));

    
    Annotation instAnn2 = addAnn(doc, "", 11, 20, "instanceType", gate.Utils.featureMap());
    HashSet<String> v2 = new HashSet<String>();
    v2.add("setval1");
    v2.add("setval4");
    v2.add("setval5");
    Annotation tok2 = addAnn(doc, "", 12, 15, "theType", gate.Utils.featureMap("feature1",v2));
    
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    FeatureVector fv = (FeatureVector)inst.getData();
    System.err.println("FeatureExtraction SimpleList1a: "+fv.toString(true));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═setval1"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═setval2"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═setval3"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═setval1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═setval2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═setval3"),EPS);
    
    inst = newInstance(inst.getAlphabet());
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn2);
    fv = (FeatureVector)inst.getData();
    System.err.println("FeatureExtraction SimpleList1b: "+fv.toString(true));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═setval1"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═setval4"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═setval5"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═setval1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═setval4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═setval5"),EPS);
    
  }  

  // Test extracting a nominal attribute where the annotation feature is a collection
  @Test
  public void extractSimpleList2() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature1</FEATURE><DATATYPE>nominal</DATATYPE><LISTSEP>:</LISTSEP></ATTRIBUTE>"+
            "</ROOT>";
    List<FeatureSpecAttribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 10, "instanceType", gate.Utils.featureMap());
    Annotation tok1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","lval1:lval2:lval3"));

    
    Annotation instAnn2 = addAnn(doc, "", 11, 20, "instanceType", gate.Utils.featureMap());
    Annotation tok2 = addAnn(doc, "", 12, 15, "theType", gate.Utils.featureMap("feature1","lval1:lval4:lval5"));
    
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    FeatureVector fv = (FeatureVector)inst.getData();
    System.err.println("FeatureExtraction SimpleList2a: "+fv.toString(true));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═lval1"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═lval2"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═lval3"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═lval1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═lval2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═lval3"),EPS);
    
    inst = newInstance(inst.getAlphabet());
    FeatureExtractionMalletSparse.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn2);
    fv = (FeatureVector)inst.getData();
    System.err.println("FeatureExtraction SimpleList2b: "+fv.toString(true));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═lval1"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═lval4"));
    assertTrue(inst.getAlphabet().contains("theType┆feature1╬A═lval5"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═lval1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═lval4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType┆feature1╬A═lval5"),EPS);
    
  }  

  */
 
}
