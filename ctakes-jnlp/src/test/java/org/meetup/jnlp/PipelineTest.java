package org.meetup.jnlp;

import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.test.TestUtil;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.meetup.jnlp.utils.TestUtils;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.util.JCasUtil.select;


public class PipelineTest extends PipelineBase {
    @Test
    public void HelloWorld() {
        System.out.println("Hello World");
    }

    @Test
    public void segmentingTest1() throws ResourceInitializationException, UIMAException, Exception {
        AnalysisEngineDescription pipeline =  createEngineDescription(
            simpleSegmentAnnotator,
            sentenceDetectorAnnotator
        );

        AnalysisEngine ae = createEngine(pipeline);

        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText("John has cancer. Jane has diabetes.");

        ae.process(jCas);

        TestUtils.cvd(jCas);
    }

    @Test
    public void SegmentingTest2() throws ResourceInitializationException, UIMAException, Exception {
        AnalysisEngineDescription pipeline =  createEngineDescription(
                simpleSegmentAnnotator,
                sentenceDetectorAnnotator
        );

        AnalysisEngine ae = createEngine(pipeline);

        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText("John has cancer. Jane has diabetes.");

        ae.process(jCas);

        for(Sentence sentence : select(jCas, Sentence.class)) {
            System.out.println("Sentences: " + sentence.getCoveredText());
        }
    }
}
