package org.meetup.jnlp;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Ignore;
import org.junit.Test;
import org.meetup.jnlp.utils.TestUtils;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.util.JCasUtil.select;


public class PipelineTest extends PipelineBase {
    @Test @Ignore
    public void HelloWorld() {
        System.out.println("Hello World");
    }

    @Test @Ignore
    public void visualizerTest() throws Exception {
        // Combine the other AnalysisEngineDescriptions
        AnalysisEngineDescription pipeline =  createEngineDescription(
            simpleSegmentAnnotator,
            sentenceDetectorAnnotator
        );

        // Create AnalysisEngine
        AnalysisEngine ae = createEngine(pipeline);

        // Create Java common analysis system
        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText("John has cancer. Jane has diabetes.");

        // Process
        long start = System.currentTimeMillis();
        ae.process(jCas);
        long end = System.currentTimeMillis();

        System.out.println("Processing time: " + (end-start) + " milliseconds");

        // Visualize results (buggy)
        TestUtils.cvd(jCas);
    }

    @Test @Ignore
    public void SegmentingTest1() throws Exception {
        AnalysisEngineDescription pipeline =  createEngineDescription(
                simpleSegmentAnnotator
        );

        AnalysisEngine ae = createEngine(pipeline);

        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText("John has cancer. Jane has diabetes.");

        ae.process(jCas);

        for(Segment sentence : select(jCas, Segment.class)) {
            System.out.println("Segments: " + sentence.getCoveredText());
        }
    }

    @Test @Ignore
    public void SentenceTest1() throws Exception {
        AnalysisEngineDescription pipeline =  createEngineDescription(
                simpleSegmentAnnotator
                // TODO: add the sentenceAnnotator
        );

        AnalysisEngine ae = createEngine(pipeline);

        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText("John has cancer. Jane has diabetes.");

        ae.process(jCas);

        // Write code here to print out sentences
    }
}
