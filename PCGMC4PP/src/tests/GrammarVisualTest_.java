/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import lgraphs.LGraph;
import lgraphs.LGraphNode;
import lgraphs.ontology.Sort;
import lgraphs.visualization.LGraphVisualizer;

/**
 *
 * @author santi
 */
public class GrammarVisualTest_ {
    public static void main(String args[]) throws Exception {
        LGraph graph = GrammarTest_.generateGraph(0);
        JFrame v = LGraphVisualizer.newWindow("Resulting graph", 1200, 600, graph);
        List<Sort> noi = new LinkedList<Sort>();
        List<Sort> eoi = new LinkedList<Sort>();
        noi.add(Sort.getSort("track"));
        eoi.add(Sort.getSort("to"));
        LGraph layoutGraph = graph.cloneSubGraph(noi,eoi, new LinkedHashMap<LGraphNode, LGraphNode>());
        JFrame v2 = LGraphVisualizer.newWindow("Track graph", 1200, 600, layoutGraph);
    }
}
