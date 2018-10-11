/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import lgraphs.LGraph;
import lgraphs.ontology.Ontology;
import lgraphs.ontology.Sort;
import lgraphs.sampler.LGraphGrammarSampler;
import lgraphs.sampler.LGraphRewritingGrammar;
import support.GameStateExporter;
import support.PCG;

/**
 *
 * @author santi
 */
public class GrammarTest_ {

    public static LGraph generateGraph(int randomSeed) throws Exception {
        LGraph.DEBUG = 1;
        return PCG.generateGraph(randomSeed, 0, false, null, true);
        /*
        Ontology ontology = new Ontology("data/ppppOntology4.xml");
        LGraph graph = LGraph.fromString("N0:problem()");
        //graph = PCG.applyGrammar(ontology, graph, "data/minimal.txt", true);
        //graph = PCG.applyGrammar(ontology, graph, "data/error_not_deleting.txt", true);
        graph = PCG.applyGrammar(ontology, graph, "data/ppppGrammar4a.txt", randomSeed, true);
        graph = PCG.applyGrammar(ontology, graph, "data/ppppGrammar4b.txt", randomSeed, true);
        graph = PCG.applyGrammar(ontology, graph, "data/ppppGrammar4c.txt", randomSeed, true);
        //graph = PCG.applyGrammar(ontology, graph, "data/ppppGrammar4d.txt", true);
        return graph;
        */
    }

    public static void main(String args[]) throws Exception {
        if(true)
            generateGraph(6);
        else {
            for(int i=0;i<1000;i++){
//                Sort.strict_sort_creation = false;
                System.out.println("USING SEED "+Integer.toString(i));
                generateGraph(i);
            }        
        }
    }
}
