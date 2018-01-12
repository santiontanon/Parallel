/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game.pcg;

import game.GameState;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lgraphs.LGraph;
import lgraphs.LGraphNode;
import orthographicembedding.OrthographicEmbeddingResult;
import support.GameStateExporter;
import tests.GrammarStats;

/**
 *
 * @author josepvalls
 */
public class PuzzleEmbeddingEvaluator {
    LGraph graph;
    LGraph layoutGraph;
    Map<LGraphNode, LGraphNode> map;
    public PuzzleEmbeddingEvaluator(LGraph graph, LGraph layoutGraph, Map<LGraphNode, LGraphNode> map){
        this.graph = graph;
        this.layoutGraph = layoutGraph;
        this.map = map;
    }
    //private static double[] normalization_vector = {};
    private static double[] normalization_vector = {25,27,1,248,0.53125,0.235294117647059,1.72727272727273,2.5,2.16666666666667,2.33333333333333,0.3,1,1,1,1};
    //private static double[] normalized_averages = {0.225641025641026,0.613333333333333,0.047380943877247,0.342654735272185,0.587150626286478,0.71267083147736,0.756494020382909,1.01421078921079,0.211462699557938,0.505123991235102,0.404250290473201,0.884634787169363,0.704434402504578,0.638888888888889,0.583333333333333};
    private static double[] normalized_averages = {0.225641025641026,0.613333333333333,1,0,0.587150626286478,0.71267083147736,1,0,1,0,0,1,1,1,1};    
    private static double[] vector_weights = {1,1,10,200,1,1,100,100,100,100,1,1,1,1,1};
    //private static double[] vector_weights = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
    

    
    // v_w	v_h	v_w/v_h	v_track_num	v_trackDensity	v_componentDensity	v_rowSameComponentsAvg	v_rowDifferentComponentsAvg	v_colSameComponentsAvg	v_colDifferentComponentsAvg	v_Simplicity	v_balanceTracks	v_balanceComponents	v_sequenceTracks	v_sequenceComponents
    public double evaluate(GameState gs){
        double[] vector = GrammarStats.getVisualPropertiesVector(gs);
        double ret = 0;
        for(int i=0;i<vector.length;i++){
            ret += vector_weights[i] * Math.abs((vector[i]/PuzzleEmbeddingEvaluator.normalization_vector[i])-PuzzleEmbeddingEvaluator.normalized_averages[i]);
        }
        //System.out.println(Arrays.toString(vector));
        return ret;
    }
    public double evaluate(OrthographicEmbeddingResult oe){
        // The optimization process minimizes the evaluation
        // Smaller is better
        boolean debug = false;
        //return this.evaluate(new GraphManager(oe, this.graph, this.layoutGraph, this.map).graphToAlmostGameState());
        GameState gs = new GraphManager(oe, this.graph, this.layoutGraph, this.map).graphToGameStateForVisualEval();
        double eval = this.evaluate(gs);
        if(debug){
            System.out.println("OGE "+oe);
            String out = GameStateExporter.export(gs);
            System.out.println("EVALUATION "+out);
            System.out.println("EVALUATION "+eval);
        }
        return eval;
    }
}
