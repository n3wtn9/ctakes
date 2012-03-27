/*    Copyright 2011 Children's Hospital Boston
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
    
@author Jiaping Zheng
 * @author Tim Miller
 */
package edu.mayo.bmi.uima.coref.ae;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.chboston.cnlp.ctakes.common.type.BooleanLabeledFS;
import org.chboston.cnlp.ctakes.parser.uima.type.TreebankNode;
import org.chboston.cnlp.ctakes.parser.uima.type.TerminalTreebankNode;
import org.mipacq.annotation.type.CoreferenceRelation;
import org.mipacq.annotation.type.NonCoreferenceRelation;
import org.mipacq.annotation.type.RelationArgument;
import org.mipacq.annotation.type.UnrelationArgument;

import edu.mayo.bmi.coref.util.AbstractClassifier;
import edu.mayo.bmi.coref.util.AnaphoricityAttributeCalculator;
import edu.mayo.bmi.coref.util.AttributeCalculator;
import edu.mayo.bmi.coref.util.CorefConsts;
import edu.mayo.bmi.coref.util.FSIteratorToList;
import edu.mayo.bmi.coref.util.FeatureVector;
import edu.mayo.bmi.coref.util.HobbsTreeNavigator;
import edu.mayo.bmi.coref.util.MarkableTreeUtils;
import edu.mayo.bmi.coref.util.PairAttributeCalculator;
import edu.mayo.bmi.coref.util.ParentPtrTree;
import edu.mayo.bmi.coref.util.SyntaxAttributeCalculator;
import edu.mayo.bmi.uima.chunker.type.Chunk;
import edu.mayo.bmi.uima.core.resource.FileResource;
import edu.mayo.bmi.uima.core.type.BaseToken;
import edu.mayo.bmi.uima.core.type.NamedEntity;
import edu.mayo.bmi.uima.core.type.Sentence;
import edu.mayo.bmi.uima.core.type.WordToken;
import edu.mayo.bmi.uima.coref.type.*;

public class MipacqSvmChainCreator extends JCasAnnotator_ImplBase {

	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	// debug
	private boolean debug = false;

	// svm models
	private AbstractClassifier mod_pron, mod_dem, mod_coref;
	private svm_model mod_anaphoricity;
	// positive class label index in libsvm's prob_est array
	private int anaphoricity_idx, coref_idx;
	private edu.mayo.bmi.coref.util.SvmVectorCreator vecCreator = null;
	
	ParentPtrTree ppt;

	HashSet<String> stopwords;

	private svm_model loadModel (UimaContext uc, String m) {
		svm_model ret = null;
		try {
			String r = ((FileResource) uc.getResourceObject(m)).getFile().getAbsolutePath();
			ret = svm.svm_load_model(r);
			logger.info(m+" loaded: "+r);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error loading "+m);
		}
		return ret;
	}

	@Override
	public void initialize(UimaContext uc) throws ResourceInitializationException {
		super.initialize(uc);

		// Load svm models
		mod_anaphoricity = loadModel(uc, "svmAnaphoricityModel");
		mod_pron = new AbstractClassifier(uc, "svmPronModel", FeatureVector.getPronCorefFeatures().length + SyntaxAttributeCalculator.getNumPronFeats() - 1);
//		mod_dem = new AbstractClassifier(uc, "svmDemModel", FeatureVector.getDemCorefFeatures().length + SyntaxAttributeCalculator.getNumDemFeats() - 1);
		mod_coref = new AbstractClassifier(uc, "svmCorefModel", FeatureVector.getNECorefFeatures().length + SyntaxAttributeCalculator.getNumNEFeats() - 1);

		int[] labels = new int[2];
		svm.svm_get_labels(mod_anaphoricity, labels);
		anaphoricity_idx = labels[0]==1 ? 0 : 1;
		//		svm.svm_get_labels(mod_coref, labels);
		//		coref_idx = labels[0]==1 ? 0 : 1;

		// Load stop words list
		try {
			stopwords = new HashSet<String>();
			FileResource r = (FileResource) uc.getResourceObject("stopWords");
			BufferedReader br = new BufferedReader(new FileReader(r.getFile()));
			String l;
			while ((l = br.readLine())!=null) {
				l = l.trim();
				if (l.length()==0) continue;
				int i = l.indexOf('|');
				if (i > 0)
					stopwords.add(l.substring(0,i).trim());
				else if (i < 0)
					stopwords.add(l.trim());
			}
			vecCreator = new edu.mayo.bmi.coref.util.SvmVectorCreator(stopwords, mod_anaphoricity);
			logger.info("Stop words list loaded: " + r.getFile().getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error loading stop words list");
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// Convert the orderless FSIterator to List, sort by char offsets
		LinkedList<Annotation> lm = FSIteratorToList.convert(
				jcas.getJFSIndexRepository().getAnnotationIndex(Markable.type).iterator());

		// Create a parent pointer tree to calculate equivalence classes
		ppt = new ParentPtrTree(lm.size());

		// Make a data structure mapping markables to indexes so we don't lose the order if we re-arrange
		Map<Markable, Integer> m2q = new HashMap<Markable,Integer>();
		for(int p = 0; p < lm.size(); p++){
			m2q.put((Markable)lm.get(p), p);
		}
		
		FSIterator iter = jcas.getAnnotationIndex(MarkablePairSet.type).iterator();
		while(iter.hasNext()){
			MarkablePairSet set = (MarkablePairSet) iter.next();
			Markable anaphor = set.getAnaphor();
			FSList fs = (FSList) set.getAntecedentList();
//			double bestProb = 0.0;
			MarkableProb bestAnte = null;
			LinkedList<Markable> ll = fs2ll(fs);
			if(anaphor instanceof PronounMarkable){
				bestAnte = processPronoun(anaphor, ll, jcas);
			}else if(anaphor instanceof NEMarkable){
				bestAnte = processNE(anaphor, ll, jcas);
			}else if(anaphor instanceof DemMarkable){
				bestAnte = processDem(anaphor, ll, jcas);
			}
			if(bestAnte.prob > CorefConsts.COREF_THRESHOLD){
				CoreferenceRelation cr = new CoreferenceRelation(jcas);
				RelationArgument ra1 = new RelationArgument(jcas);
				ra1.setId(bestAnte.m.getId());
				ra1.setArgument(bestAnte.m);
				ra1.setRole("antecedent");
				RelationArgument ra2 = new RelationArgument(jcas);
				ra2.setId(anaphor.getId());
				ra2.setArgument(anaphor);
				ra2.setRole("anaphor");
				cr.setArg1(ra1);
				cr.setArg2(ra2);
				cr.setCoref_prob(bestAnte.prob);
				ra1.addToIndexes();
				ra2.addToIndexes();
				cr.addToIndexes();
				ppt.union(m2q.get(anaphor), m2q.get(bestAnte.m));		
			}
		}

		// Extract equivalence classes and save them into CAS
		int[] ec = new int[ppt.getSize()]; // class number for each Markable
		int n = ppt.equivCls(ec); // n holds the number of classes
		EmptyFSList elist = new EmptyFSList(jcas); // shared tail for all chains
		FSList[] listhds = new FSList[n]; // keep track of the heads of all chains
		CoreferenceChain[] chains = new CoreferenceChain[n];

		// Initialize n chains
		for (int i = 0; i < n; ++i) {
			chains[i] = new CoreferenceChain(jcas);
			chains[i].setId(i);
			chains[i].addToIndexes();
			listhds[i] = elist;
		}

		// Scan from the end of the Markable list
		// insert Markables to the head of their chains
		for (int i = ec.length-1; i >= 0; --i) {
			NonEmptyFSList l = new NonEmptyFSList(jcas);
			l.setHead(lm.get(i));
			l.setTail(listhds[ec[i]]);
			listhds[ec[i]] = l;
			chains[ec[i]].setMembers(l);
			chains[ec[i]].setSize(chains[ec[i]].getSize()+1);
		}
	}


	private LinkedList<Markable> fs2ll(FSList fs) {
		LinkedList<Markable> ll = new LinkedList<Markable>();
		while(fs instanceof NonEmptyFSList){
			NonEmptyFSList node = (NonEmptyFSList) fs;
			BooleanLabeledFS feat = (BooleanLabeledFS) node.getHead();
			Markable antecedent = (Markable) feat.getFeature();
			ll.add(antecedent);
			fs = node.getTail();
		}
		return ll;
	}
	
	private MarkableProb processPronoun(Markable anaphor, LinkedList<Markable> anteList, JCas jcas){
		Markable ante = null;
		double bestProb = 0.0;
//		List<Markable> resortedList = HobbsTreeNavigator.sort(anaphor, anteList, jcas);
		List<Markable> resortedList = anteList;
		for(Markable antecedent : resortedList){
			svm_node[] nodes = vecCreator.getNodeFeatures(anaphor, antecedent, jcas);
//			SyntaxAttributeCalculator sac = new SyntaxAttributeCalculator(jcas, antecedent, anaphor);
			double prob = 0.0;
			prob = mod_coref.predict(nodes);
			if(prob > bestProb){
				if(debug) indexNegativeExample(jcas, ante, anaphor, bestProb); // save former best as non-ante...
				bestProb = prob;
				ante = antecedent;
			}else{
				if(debug){
					indexNegativeExample(jcas, antecedent, anaphor, prob);
				}
			}
			if(bestProb > 0.5) break;
		}
		return new MarkableProb(ante, bestProb);
	}

	private MarkableProb processNE(Markable anaphor, List<Markable> anteList, JCas jcas){
		Markable ante = null;
		double bestProb = 0.0;
		for(Markable antecedent : anteList){
			svm_node[] nodes = vecCreator.getNodeFeatures(anaphor, antecedent, jcas, true);
//			SyntaxAttributeCalculator sac = new SyntaxAttributeCalculator(jcas, antecedent, anaphor);
			double prob = 0.0;
			prob = mod_coref.predict(nodes);
			if(prob > bestProb){
				if(debug) indexNegativeExample(jcas, ante, anaphor, bestProb);
				bestProb = prob;
				ante = antecedent;
			}else{
				if(debug) indexNegativeExample(jcas, antecedent, anaphor, prob);
			}
		}
		return new MarkableProb(ante, bestProb);
	}
	
	private MarkableProb processDem(Markable anaphor, List<Markable> anteList, JCas jcas){
		double bestProb = 0.0;
		TreebankNode n = MarkableTreeUtils.markableNode(jcas, anaphor.getBegin(), anaphor.getEnd());
		TreebankNode parent = (n != null ? n.getParent() : null);
		TreebankNode gparent = (parent != null ? parent.getParent() : null);
		Markable ante = null;
		for(Markable antecedent: anteList){
			if(n!=null && parent != null && gparent != null && n.getNodeType().equals("WHNP") && parent.getNodeType().equals("SBAR")
					&& gparent.getNodeType().equals("NP") && gparent.getChildren(1) == parent && gparent.getChildren(0).getNodeType().equals("NP")){
				TreebankNode anteNode = gparent.getChildren(0);
				Markable trueAnte = MarkableTreeUtils.nodeMarkable(jcas, anteNode.getBegin(), anteNode.getEnd());
				if(trueAnte == antecedent){
					bestProb = 1.0;
					ante = antecedent;
					break;
				}
			}
		}
		return new MarkableProb(ante, bestProb);
	}
	
	private void indexNegativeExample(JCas jcas, Markable ante, Markable ana,
			double d) {
		if(ante == null) return;
		UnrelationArgument ra1 = new UnrelationArgument(jcas);
		ra1.setId(ante.getId());
		ra1.setArgument(ante);
		ra1.setRole("non-antecedent");
		UnrelationArgument ra2 = new UnrelationArgument(jcas);
		ra2.setId(ana.getId());
		ra2.setArgument(ana);
		ra2.setRole("non-anaphor");
		NonCoreferenceRelation cr = new NonCoreferenceRelation(jcas);
		cr.setArg1(ra1);
		cr.setArg2(ra2);
		cr.setCoref_prob(d);
		ra1.addToIndexes();
		ra2.addToIndexes();
		cr.addToIndexes();

	}
}

class MarkableProb{
	public MarkableProb(Markable m, Double p){
		this.m = m;
		this.prob = p;
	}
	Markable m;
	Double prob;
}