package org.meetup.jnlp.utils;

import org.apache.uima.jcas.JCas;
import org.apache.uima.tools.cvd.CVD;
import org.apache.uima.tools.cvd.MainFrame;

public class TestUtils {
    public static void cvd(JCas jCas) throws Exception {
        MainFrame frame = CVD.createMainFrame(null);
        frame.setTextNoTitle(jCas.getDocumentText());
        frame.setExitOnClose(false);
        frame.setCas(jCas.getCas());
        frame.updateIndexTree(true);
        frame.setAllAnnotationViewerItemEnable(true);
        frame.setDirty(false);
        while(true) { Thread.sleep(5000); }
    }
}
