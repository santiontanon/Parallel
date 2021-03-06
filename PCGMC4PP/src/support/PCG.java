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
import lgraphs.LGraph;
import lgraphs.LGraphNode;
import lgraphs.ontology.Ontology;
import lgraphs.ontology.Sort;
import lgraphs.sampler.LGraphGrammarSampler;
import lgraphs.sampler.LGraphRewritingGrammar;
import lgraphs.sampler.LGraphRewritingRule;
import optimization.EmbeddingComparator;
import optimization.OrthographicEmbeddingBoardSizeOptimizer;
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
    public static String dataFolderPath = "data";
    
    public static void main(String args[]) throws FileNotFoundException, IOException, Exception {
        if (args.length < 2) {
            System.out.println("Usage: support.PCG parameter_file|debug random_seed size [path-to-put-the-generated-level]");
            System.exit(4);
        }
        boolean keep_solution = false;
        String parameterFile = args[0];
        long randomSeed = Long.parseLong(args[1]);
        String sizeStr = args[2];
        String outputPath = null;
        boolean debug = false;
        String desiredLevelID = null;
        if (args.length >= 4) {
            outputPath = args[3];
            dataFolderPath = args[3];
            if (args.length >= 5) {
                outputPath = args[4];
            }
        }
        for(int i = 0;i<args.length;i++) {
            if (args[i].equals("-pcgid")) {
                if (args.length > i+1) {
                    // we have the desired level ID name!
                    desiredLevelID = args[i+1];
                }
            }
        }
        LinkedHashMap<String, Double> playerModel = null;
        if("debug".equals(parameterFile)) {
            randomSeed = 0;
            debug = true;
        } else {
            playerModel = loadPlayerModel(parameterFile);
        }
        GameState gs = generateGameState(randomSeed, sizeStr, keep_solution, true, playerModel, debug);
        if("debug".equals(parameterFile)){
            System.out.println(GameStateExporter.export(gs));
        } else {
            // Export
            export(gs, getNewFileFromFilename(parameterFile, false, outputPath), desiredLevelID);
        }
        System.exit(0);
    }
    
    public static File getNewFileFromFilename(String filename, boolean overwrite, String outputPath) throws IOException{
        File file = new File(filename);
        File out_file;
        File folder = file.getParentFile();
        if (outputPath != null) folder = new File(outputPath);
        if (overwrite) {
            out_file = new File(folder, "pcg_out.txt");
        } else {
            out_file = File.createTempFile("pcg_out_", ".txt", folder);
        }
        return out_file;
    }

    public static GameState generateGameState(long randomSeedGraph, long randomSeedEmbedding, int size, boolean keep_solution, boolean ensure_solution_is_required, LinkedHashMap<String, Double> playerModel, boolean debug) throws Exception {
        List<String> skills = new ArrayList<>();    // the set of skills required by this level
        LGraph graph = generateGraph(randomSeedGraph, size, keep_solution, ensure_solution_is_required, playerModel, skills, false);
        GameState gs = embeddGraph(graph, randomSeedEmbedding, debug, skills);
        return gs;
    }
    
    public static GameState generateGameState(long randomSeed, String sizeStr, boolean keep_solution, boolean ensure_solution_is_required, LinkedHashMap<String, Double> playerModel, boolean debug) throws Exception {
        int size = -1;
        if (IsNumber.isNumber(sizeStr)) {
            size = (int)Double.parseDouble(sizeStr);
        }
        return generateGameState(randomSeed,randomSeed, size, keep_solution, ensure_solution_is_required, playerModel, debug);
    }

    public static LGraph applyGrammar(Ontology ontology, LGraph graph, String filename, Random r, boolean debug) throws Exception {
        return applyGrammar(ontology, graph, filename, r, debug, null, null, null, null, null);
    }
    public static LGraph applyGrammar(Ontology ontology, LGraph startingGraph, String filename, Random r, boolean debug, 
                                      Map<String,Integer> rule_applications, 
                                      Map<String,Integer> size_application_limits, List<String> tags_to_be_ensured,
                                      LinkedHashMap<String, Double> playerModel,
                                      List<String> skills) throws Exception {
        int attempts_left = 10;
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
        
        if (playerModel != null) PCGPlayerModelUtils.applyPlayerModel(playerModel, grammar);
        
        LGraph lastGraph = null;
        do{
            LGraph graph = startingGraph;
            lastGraph = graph;
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
//            generator.printRuleApplicationCounts();
            if (debug) {
                generator.printRuleApplicationCounts();
//                System.out.println("Current graph (after):");
//                System.out.println("  " + lastGraph);
            }
    
            boolean found = true;
            if (tags_to_be_ensured != null) {
                for(String tag:tags_to_be_ensured) {
                    if (!generator.ruleWithTagWasTriggered(tag)) {
                        found = false;
                        System.out.println("tag_to_be_ensured  not found!: " + tag);
                        break;
                    }
                }
            }
            if (found) {
                // we are good! we succeeded!
                if(rule_applications!=null){
                    for(Entry<String,Integer> entry:generator.getRuleApplicationCounts().entrySet()){
                        rule_applications.put(entry.getKey(), rule_applications.get(entry.getKey())+entry.getValue());
                    }
                }
                if (skills != null) {
                    for(LGraphRewritingRule rule:grammar.getRules()) {
                        Integer count = generator.getRuleApplicationCounts().get(rule.getName());
                        if (count != null && count > 0) {
                            for(String tag:rule.getTags()) {
                                String skill = PCGPlayerModelUtils.skillCodeToSkillName(tag);
                                if (skill != null && !skills.contains(skill)) {
                                    skills.add(skill);
                                }
                            }
                        }
                    }
                }
                break;
            }
            attempts_left--;
        }while(attempts_left>0);
        
        return lastGraph;
    }

    public static LGraph generateGraph(long randomSeed, int size, boolean keep_solution, boolean ensure_solution_is_required, LinkedHashMap<String, Double> playerModel, List<String> skills, boolean debug) throws Exception {
        return generateGraph(randomSeed, size, keep_solution, ensure_solution_is_required, playerModel, skills, debug, null);
    }
    public static LGraph generateGraph(long randomSeed, int size, boolean keep_solution, boolean ensure_solution_is_required, LinkedHashMap<String, Double> playerModel, List<String> skills, boolean debug, Map<String,Integer> rule_applications) throws Exception {
        Random r = new Random(randomSeed);
        Sampler s = new Sampler(randomSeed);
        if (size == -1) {
            if (playerModel != null) {
                size = PCGPlayerModelUtils.determineLevelSize(playerModel);
            } else {
                size = 1;
            }
            System.out.println("Player model recommended level size: " + size);
        } else {
            System.out.println("Size fixed by input parameters: " + size);
        }
        
        System.out.println("PlayerModel:");
        if (playerModel != null) {
            for(String skill:playerModel.keySet()) {
                System.out.println(skill + ": " + playerModel.get(skill));
            }
        }

        int max_attempts = 10;
        LGraph graph = null;
        do {
            List<Integer> sizes = s.createDistribution(size, 2);
            Map<String,Integer> size_application_limits = new LinkedHashMap<>();
            List<String> tags_to_be_ensured = new LinkedList<>();

            System.out.println("Sizes: "+sizes.get(0)+" "+sizes.get(1));
            size_application_limits.put("ADD_MORE_PROBLEMS", 0);
            size_application_limits.put("MAKE_SUBPROBLEM_ABST_SERIAL_TASKS", sizes.get(0));
            size_application_limits.put("MAKE_SUBPROBLEM_ABST_PARALLEL_TASKS", sizes.get(1));

            tags_to_be_ensured.add("deliver_packages");

            Sort.clearSorts();
            Ontology ontology = new Ontology(dataFolderPath + "/ppppOntology4.xml");
            graph = LGraph.fromString("N0:problem()");

            // Create structure for problems and subproblems        
            graph = applyGrammar(ontology, graph, dataFolderPath + "/ppppGrammar4a.txt", r, debug, rule_applications, size_application_limits, null, playerModel, skills);
    //	LGraphVisualizer.newWindow("after ppppGrammar4a", 800, 600, graph);

            // Instanciate situations
            graph = applyGrammar(ontology, graph, dataFolderPath + "/ppppGrammar4b.txt", r, debug, rule_applications, null, tags_to_be_ensured, playerModel, skills);
    //        graph = applyGrammar(ontology, graph, dataFolderPath + "/ppppGrammar4b-santi.txt", r, debug, rule_applications, null);
            // LGraphVisualizer.newWindow("after ppppGrammar4b", 800, 600, graph);

            // Refine components
            graph = applyGrammar(ontology, graph, dataFolderPath + "/ppppGrammar4c.txt", r, debug, rule_applications, null, null, playerModel, skills);

            // find additional skills that might not have been tagged by the grammar:
            /*
            skills.put("Understand that arrows move at unpredictable rates", "nondeterministic_arrows");
            */
            if (skills != null) {
                for(LGraphNode n:graph.getNodes()) {
                    if (n.subsumedBy(Sort.getSort("delivery"))) {
                        if (!skills.contains("Deliver packages")) skills.add("Deliver packages");
                    }
                    if (n.subsumedBy(Sort.getSort("fork"))) {
                        if (!skills.contains("Be able to link buttons to direction switches")) skills.add("Be able to link buttons to direction switches");
                    }
                    if (n.subsumedBy(Sort.getSort("semaphore"))) {
                        if (!skills.contains("Be able to link semaphores to buttons")) skills.add("Be able to link semaphores to buttons");
                        if (!skills.contains("Understand the use of semaphores")) skills.add("Understand the use of semaphores");
                    }
                    if (n.subsumedBy(Sort.getSort("diverter"))) {
                        if (!skills.contains("Use diverters")) skills.add("Use diverters");
                    }
                    if (n.subsumedBy(Sort.getSort("exchange"))) {
                        if (!skills.contains("Understand exchange points")) skills.add("Understand exchange points");
                    }
                    if (n.subsumedBy(Sort.getSort("multithread"))) {
                        if (!skills.contains("Understand that arrows move at unpredictable rates")) skills.add("Understand that arrows move at unpredictable rates");
                    }
                }
            }

            // Remove solution
            LGraph graph2 = applyGrammar(ontology, graph, dataFolderPath + "/ppppGrammar4d.txt", r, debug, rule_applications, null, null, playerModel, skills);
    //	LGraphVisualizer.newWindow("after ppppGrammar4d", 800, 600, graph);
            if (graph2 == graph) {
                System.out.println("There is no solution required!!!!");                    
                // if the graph does not need a solution, and we want to ensure one is required, we will need
                // to try again...
                if (!ensure_solution_is_required) break;
            } else {
                if(!keep_solution) graph = graph2;
                // if the graph needs a solution, then we are done!
                break;
            }
        }while(max_attempts>0);
        return graph;
    }

    public static GameState embeddGraph(LGraph graph, long randomSeed, boolean debug, List<String> skills) throws Exception {
        return embeddGraph(graph,randomSeed,debug,10, skills);
    }
    public static GameState embeddGraph(LGraph graph, long randomSeed, boolean debug, int optimizationAttempts, List<String> skills) throws Exception {
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

//                SavePNG.savePNG("PCG-g_oe0-"+attempt+".png", g_oe, 32, 32, true);
//                g_oe = OrthographicEmbeddingOptimizer.optimize(g_oe, g, pec);
//                SavePNG.savePNG("PCG-g_oe1-"+attempt+".png", g_oe, 32, 32, true);
                g_oe = OrthographicEmbeddingPathOptimizer.optimize(g_oe, g, pec);
//                SavePNG.savePNG("PCG-g_oe2-"+attempt+".png", g_oe, 32, 32, true);
                g_oe = OrthographicEmbeddingBoardSizeOptimizer.optimize(g_oe, g, pec);
//                SavePNG.savePNG("PCG-g_oe3-"+attempt+".png", g_oe, 32, 32, true);

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
//            SavePNG.savePNG("PCG-best_g_oe.png", best_g_oe, 32, 32, true);
        }
        OrthographicEmbeddingResult oe = DisconnectedGraphs.mergeDisconnectedEmbeddingsSideBySide(disconnectedEmbeddings, disconnectedGraphs, 1.0);
        
        // "Parallel"-specifiy optimizations
        GameState gs = new GraphManager(oe, graph, layoutGraph, map_inverse).graphToGameState(skills);
        //gs = LevelOptimizer.optimize(gs);

        return gs;
    }

    public static void export(GameState gs, File out_file, String desiredLevelID) throws IOException {
        String out = GameStateExporter.export(gs, desiredLevelID);
        PrintWriter writer = new PrintWriter(out_file);
        writer.print(out);
        writer.close();
        System.out.println(out_file.getAbsolutePath());
    }
    
    
    public static LinkedHashMap<String, Double> loadPlayerModel(String filename) throws Exception
    {
        LinkedHashMap<String, Double> playerModel = new LinkedHashMap<>();
//        FileWriter fw = new FileWriter(new File("/Users/santi/my-research/Parallel/PCG-debug.txt"));
//        fw.write("This is the content of the player model file:\n");
        BufferedReader br = new BufferedReader(new FileReader(filename));
        while(true) {
            String line = br.readLine();
            if (line == null) break;
//            fw.write(line + "\n");
            String tokens[] = line.split(",");
            if (tokens.length < 2) {
                System.out.println("ERROR: parameters file has the wrong format, line: " + line);
            } else {
                playerModel.put(tokens[0].trim(), Double.parseDouble(tokens[1].trim()));
            }
        }
//        fw.flush();
//        fw.close();
        
        return playerModel;
    }
}
