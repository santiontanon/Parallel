/*
 * The MIT License
 *
 * Copyright 2016 Josep Valls-Vargas <josep@valls.name>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package support;

import orthographicembedding.DisconnectedGraphs;
import game.GameState;
import game.pcg.GraphManager;
import game.pcg.PuzzleEmbeddingComparator;
import game.pcg.PuzzleEmbeddingEvaluator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.swing.JFrame;
import lgraphs.LGraph;
import lgraphs.LGraphNode;
import lgraphs.ontology.Ontology;
import lgraphs.ontology.Sort;
import lgraphs.sampler.LGraphGrammarSampler;
import lgraphs.sampler.LGraphRewritingGrammar;
import lgraphs.sampler.LGraphRewritingRule;
import lgraphs.visualization.LGraphVisualizer;
import optimization.EmbeddingComparator;
import optimization.OrthographicEmbeddingBoardSizeOptimizer;
import optimization.OrthographicEmbeddingOptimizer;
import optimization.OrthographicEmbeddingPathOptimizer;
import orthographicembedding.OrthographicEmbedding;
import orthographicembedding.OrthographicEmbeddingResult;
import util.Sampler;
import valls.util.IsNumber;
import valls.util.ListToArrayUtility;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class PCG {

    public static boolean simplify = true;
    public static boolean correct = true;
    public static void main(String args[]) throws FileNotFoundException, IOException, Exception {
        if (args.length < 2) {
            System.out.println("Usage: support.PCG parameter_file|debug random_seed size [keep_solution]");
            System.exit(4);
        }
        boolean keep_solution = false;
        if (args.length >= 4) keep_solution = true;

        String filename = args[0];
        // TODO Get parameters from filename and sample GG with parameters from filename
        boolean debug = false;
        long randomSeed = Long.parseLong(args[1]);
        String sizeStr = args[2];
        LinkedHashMap<String, Double> playerModel = null;
        if("debug".equals(filename)) {
            randomSeed = 0;
            debug = true;
        } else {
            playerModel = loadPlayerModel(filename);
        }
        GameState gs = generateGameState(randomSeed, sizeStr, keep_solution, playerModel, debug);
        if(!("debug".equals(filename))){
            // Export
            export(gs, getNewFileFromFilename(filename, false));
        } else {
            System.out.println(GameStateExporter.export(gs));
        }
        System.exit(0);
    }
    
    public static File getNewFileFromFilename(String filename, boolean overwrite) throws IOException{
        File file = new File(filename);
        String path = file.getAbsolutePath();
        File out_file;
        if (overwrite) {
            out_file = new File(file.getParentFile(), "pcg_out.txt");
        } else {
            out_file = File.createTempFile("pcg_out_", ".txt", file.getParentFile());
        }
        return out_file;
    }

    public static GameState generateGameState(long randomSeedGraph, long randomSeedEmbedding, int size, boolean keep_solution, LinkedHashMap<String, Double> playerModel, boolean debug) throws Exception {
        LGraph graph = generateGraph(randomSeedGraph, size, keep_solution, playerModel, false);
        return embeddGraph(graph, randomSeedEmbedding, debug);
    }
    
    public static GameState generateGameState(long randomSeed, String sizeStr, boolean keep_solution, LinkedHashMap<String, Double> playerModel, boolean debug) throws Exception {
        int size = -1;
        if (IsNumber.isNumber(sizeStr)) {
            size = (int)Double.parseDouble(sizeStr);
        }
        return generateGameState(randomSeed,randomSeed, size, keep_solution, playerModel, debug);
    }

    public static LGraph applyGrammar(Ontology ontology, LGraph graph, String filename, Random r, boolean debug) throws Exception {
        return applyGrammar(ontology, graph, filename, r, debug, null, null);
    }
    public static LGraph applyGrammar(Ontology ontology, LGraph graph, String filename, Random r, boolean debug, Map<String,Integer> rule_applications, Map<String,Integer> size_application_limits) throws Exception {
        if (debug) {
            System.out.println("Applying " + filename);
        }
        LGraphRewritingGrammar grammar = LGraphRewritingGrammar.load(filename);
        if(rule_applications!=null){
            for(LGraphRewritingRule entry:grammar.getRules()){
                if(!rule_applications.containsKey(entry.getName())){
                    rule_applications.put(entry.getName(), 0);
                }
            }
        }
        
        LGraph lastGraph = graph;
        LGraphGrammarSampler generator = new LGraphGrammarSampler(graph, grammar, true, r);
        if(size_application_limits!=null){
            for(Entry<String,Integer> entry:size_application_limits.entrySet()){
                generator.addApplicationLimit(entry.getKey(), entry.getValue());
            }
        }
        // Use the grammar to rewrite the graph:
        do {
            if (debug) {
                System.out.println("Current graph:");
                System.out.println("  " + graph);
            }
            graph = generator.applyRuleStochastically();
            if (graph != null) {
                lastGraph = graph;
            }
        } while (graph != null);
//        if (debug) {
            generator.printRuleApplicationCounts();
            System.out.println("Current graph (after):");
            System.out.println("  " + lastGraph);
//        }
        if(rule_applications!=null){
            for(Entry<String,Integer> entry:generator.getRuleApplicationCounts().entrySet()){
                rule_applications.put(entry.getKey(), rule_applications.get(entry.getKey())+entry.getValue());
            }
        }
        return lastGraph;
    }

    public static LGraph generateGraph(long randomSeed, int size, boolean keep_solution, LinkedHashMap<String, Double> playerModel, boolean debug) throws Exception {
        return generateGraph(randomSeed, size, keep_solution, playerModel, debug, null);
    }
    public static LGraph generateGraph(long randomSeed, int size, boolean keep_solution, LinkedHashMap<String, Double> playerModel, boolean debug, Map<String,Integer> rule_applications) throws Exception {
        Random r = new Random(randomSeed);
        if (size == -1) size = r.nextInt(5)+1;
        
        System.out.println("PlayerModel:");
        if (playerModel != null) {
            for(String skill:playerModel.keySet()) {
                System.out.println(skill + ": " + playerModel.get(skill));
            }
        }

        List<Integer> sizes = new Sampler(randomSeed).createDistribution(size, 2);
        Map<String,Integer> size_application_limits = new LinkedHashMap();
        System.out.println("Sizes: "+sizes.get(0)+" "+sizes.get(1));
        size_application_limits.put("ADD_MORE_PROBLEMS", 0);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_SERIAL_TASKS", sizes.get(0));
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_PARALLEL_TASKS", sizes.get(1));
        /*
        List<Integer> sizes = new Sampler(randomSeed).createDistribution(size, 3);
        Map<String,Integer> size_application_limits = new LinkedHashMap();
        System.out.println("Sizes: "+sizes.get(0)+" "+sizes.get(1)+" "+sizes.get(2));
        size_application_limits.put("ADD_MORE_PROBLEMS", sizes.get(0));
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_SERIAL_TASKS", sizes.get(1));
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_PARALLEL_TASKS", sizes.get(2));
        */
        /*
        size_application_limits.put("ADD_MORE_PROBLEMS", 0);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_SERIAL_TASKS", 1);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_PARALLEL_TASKS", 1);
        */
        /*
        size_application_limits.put("ADD_MORE_PROBLEMS", 0);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_SERIAL_TASKS", 0);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_PARALLEL_TASKS", 0);
        */
        /*
        size_application_limits.put("ADD_MORE_PROBLEMS", 2);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_SERIAL_TASKS", 0);
        size_application_limits.put("MAKE_SUBPROBLEM_ABST_PARALLEL_TASKS", 0);
        */
        
        Sort.clearSorts();
        Ontology ontology = new Ontology("data/ppppOntology4.xml");
        LGraph graph = LGraph.fromString("N0:problem()");
        // Create structure for problems and subproblems        
        graph = applyGrammar(ontology, graph, "data/ppppGrammar4a.txt", r, debug, rule_applications, size_application_limits);
//	LGraphVisualizer.newWindow("after ppppGrammar4a", 800, 600, graph);
        
        // Instanciate situations
        graph = applyGrammar(ontology, graph, "data/ppppGrammar4b.txt", r, debug, rule_applications, null);
//        graph = applyGrammar(ontology, graph, "data/ppppGrammar4b-santi.txt", r, debug, rule_applications, null);
//	LGraphVisualizer.newWindow("after ppppGrammar4b", 800, 600, graph);

        // Refine components
        graph = applyGrammar(ontology, graph, "data/ppppGrammar4c.txt", r, debug, rule_applications, null);
        // Remove solution
        if(!keep_solution){
            graph = applyGrammar(ontology, graph, "data/ppppGrammar4d.txt", r, debug, rule_applications, null);
        }
//	LGraphVisualizer.newWindow("after ppppGrammar4d", 800, 600, graph);
        
        return graph;
    }

    public static GameState embeddGraph(LGraph graph, long randomSeed, boolean debug) throws Exception {
        return embeddGraph(graph,randomSeed,debug,10);
    }
    public static GameState embeddGraph(LGraph graph, long randomSeed, boolean debug, int optimizationAttempts) throws Exception {
        // calculate the embedding:
        List<Sort> noi = new LinkedList<Sort>();
        List<Sort> eoi = new LinkedList<Sort>();
        noi.add(Sort.getSort("track"));
        eoi.add(Sort.getSort("to"));
        Map<LGraphNode, LGraphNode> map = new LinkedHashMap<LGraphNode, LGraphNode>();
        LGraph layoutGraph = graph.cloneSubGraph(noi,eoi, map);
        Map<LGraphNode, LGraphNode> map_inverse = ListToArrayUtility.swapMapKeysValues(map);
        
        Random r;
        if(randomSeed<0){
            r = new Random(randomSeed);
        } else {
            r = new Random();
        }
        
        int[][] aa = layoutGraph.undirectedAdjacencyMatrix();
        if (debug) {
            for (int[] row : aa) {
                for (int col : row) {
                    System.out.print(col);
                    System.out.print(",");
                }
                System.out.println();
            }
        }

        List<List<Integer>> disconnectedGraphs = DisconnectedGraphs.findDisconnectedGraphs(aa);
        List<OrthographicEmbeddingResult> disconnectedEmbeddings = new ArrayList();
        PuzzleEmbeddingEvaluator pee = new PuzzleEmbeddingEvaluator(graph, layoutGraph, map_inverse);
        EmbeddingComparator pec = new PuzzleEmbeddingComparator(pee);
        //EmbeddingComparator pec = new SegmentLengthEmbeddingComparator();
        for(List<Integer> nodeSubset:disconnectedGraphs) {
            int [][]g = DisconnectedGraphs.subgraph(aa, nodeSubset);
            OrthographicEmbeddingResult best_g_oe = null;
            for(int attempt = 0;attempt<optimizationAttempts;attempt++) {
                OrthographicEmbeddingResult g_oe = OrthographicEmbedding.orthographicEmbedding(g,simplify, correct, r); 
                if (g_oe==null) continue;
                if (!g_oe.sanityCheck(false)) {
                    System.err.println("Sanity check failed!");
                    continue;
                }

                g_oe = OrthographicEmbeddingOptimizer.optimize(g_oe, g, pec);
                g_oe = OrthographicEmbeddingPathOptimizer.optimize(g_oe, g, pec);
                g_oe = OrthographicEmbeddingBoardSizeOptimizer.optimize(g_oe, g, pec);

                if (best_g_oe==null) {
                    best_g_oe = g_oe;
                    if(debug){
                        System.out.println("CURRENT EMBEDDING SCORE "+pee.evaluate(best_g_oe));
                    }
                } else {
                    if (pec.compare(g_oe, best_g_oe)<0) {
                        if(debug){
                            System.out.println("BETTER EMBEDDING FOUND "+pee.evaluate(g_oe));
                            //SavePNG.savePNG("PCG-opt3.png", g_oe, 32, 32, true);
                        }
                        best_g_oe = g_oe;
                    }
                }
            }
            if(best_g_oe == null){
                System.err.println("No orthographic projection could be found! verify the input graph is planar.");
                System.exit(10);
            }
            if (!best_g_oe.sanityCheck(false)) {
                System.err.println("The orthographic projection after optimization using custom comparator contains errors!");
                System.exit(12);
            }
                        
            disconnectedEmbeddings.add(best_g_oe);
        }
        OrthographicEmbeddingResult oe = DisconnectedGraphs.mergeDisconnectedEmbeddingsSideBySide(disconnectedEmbeddings, disconnectedGraphs, 1.0);        
        
        // "Parallel"-specifiy optimizations
        GameState gs = new GraphManager(oe, graph, layoutGraph, map_inverse).graphToGameState();
        //gs = LevelOptimizer.optimize(gs);

        return gs;
    }

    public static void export(GameState gs, File out_file) throws IOException {
        String out = GameStateExporter.export(gs);
        PrintWriter writer = new PrintWriter(out_file);
        writer.print(out);
        writer.close();
        System.out.println(out_file.getAbsolutePath());
    }
    
    public static LinkedHashMap<String, Double> loadPlayerModel(String filename) throws Exception
    {
        LinkedHashMap<String, Double> playerModel = new LinkedHashMap<>();
        
        BufferedReader br = new BufferedReader(new FileReader(filename));
        while(true) {
            String line = br.readLine();
            if (line == null) break;
            String tokens[] = line.split(",");
            if (tokens.length != 2) {
                System.out.println("ERROR: parameters file has the wrong format, line: " + line);
            } else {
                playerModel.put(tokens[0].trim(), Double.parseDouble(tokens[1].trim()));
            }
        }
        
        return playerModel;
    }
}
