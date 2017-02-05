package org.meetup.jnlp;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Before;

import java.io.IOException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescriptionFromPath;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class PipelineBase {
    public AnalysisEngineDescription simpleSegmentAnnotator;
    public AnalysisEngineDescription sentenceDetectorAnnotator;
    public AnalysisEngineDescription tokenizerAnnotator;
    public AnalysisEngineDescription contextDependentTokenizerAnnotator;
    public AnalysisEngineDescription posTagger;
    public AnalysisEngineDescription chunker;
    public AnalysisEngineDescription adjustNounPhraseToIncludeFollowingNP;
    public AnalysisEngineDescription adjustNounPhraseToIncludeFollowingPPNP;
    public AnalysisEngineDescription lookupWindowAnnotator;
    public AnalysisEngineDescription dictionaryLookupAnnotatorDB;
    public AnalysisEngineDescription negeationAnnotator;

    /**
     *  Total pipeline in order
     *  Requires UMLS credentials in environment
     *      ctakes.umlsuser = ...
     *      ctakes.umlspw = ...
     */
    public AnalysisEngineDescription totalPipeline;

    @Before
    public void init() throws IOException, InvalidXMLException, ResourceInitializationException, UIMAException {
        simpleSegmentAnnotator = createEngineDescriptionFromPath("../ctakes-clinical-pipeline/desc/analysis_engine/SimpleSegmentAnnotator.xml");
        sentenceDetectorAnnotator = createEngineDescriptionFromPath("../ctakes-core/desc/analysis_engine/SentenceDetectorAnnotator.xml");
        tokenizerAnnotator = createEngineDescriptionFromPath("../ctakes-core/desc/analysis_engine/TokenizerAnnotator.xml");
        contextDependentTokenizerAnnotator = createEngineDescriptionFromPath("../ctakes-context-tokenizer/desc/analysis_engine/ContextDependentTokenizerAnnotator.xml");
        posTagger = createEngineDescriptionFromPath("../ctakes-pos-tagger/desc/POSTagger.xml");
        chunker = createEngineDescriptionFromPath("../ctakes-chunker/desc/Chunker.xml");
        adjustNounPhraseToIncludeFollowingNP = createEngineDescriptionFromPath("../ctakes-chunker/desc/AdjustNounPhraseToIncludeFollowingNP.xml");
        adjustNounPhraseToIncludeFollowingPPNP = createEngineDescriptionFromPath("../ctakes-chunker/desc/AdjustNounPhraseToIncludeFollowingPPNP.xml");
        lookupWindowAnnotator = createEngineDescriptionFromPath("../ctakes-clinical-pipeline/desc/analysis_engine/LookupWindowAnnotator.xml");
        dictionaryLookupAnnotatorDB = createEngineDescriptionFromPath("../ctakes-dictionary-lookup/desc/analysis_engine/DictionaryLookupAnnotatorUMLS.xml");
        negeationAnnotator = createEngineDescriptionFromPath("../ctakes-ytex-uima/desc/analysis_engine/NegexAnnotator.xml");

        totalPipeline = createEngineDescription(
            simpleSegmentAnnotator,
            sentenceDetectorAnnotator,
            tokenizerAnnotator,
            contextDependentTokenizerAnnotator,
            posTagger,
            chunker,
            adjustNounPhraseToIncludeFollowingNP,
            adjustNounPhraseToIncludeFollowingPPNP,
            lookupWindowAnnotator,
            dictionaryLookupAnnotatorDB,
            negeationAnnotator
        );
    }
}
