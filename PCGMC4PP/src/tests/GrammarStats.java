/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import game.BoardState;
import game.GameState;
import game.GameStateSearch;
import game.Tile;
import game.component.Component;
import game.component.ComponentDelivery;
import game.component.ComponentDiverter;
import game.component.ComponentExchange;
import game.component.ComponentPickup;
import game.component.ComponentSemaphore;
import game.component.ComponentSignal;
import game.component.ComponentUnit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import lgraphs.LGraph;
import lgraphs.ontology.Ontology;
import lgraphs.ontology.Sort;
import lgraphs.sampler.LGraphGrammarSampler;
import lgraphs.sampler.LGraphRewritingGrammar;
import lgraphs.sampler.LGraphRewritingRule;
import support.GameStateExporter;
import support.GameStateParser;
import support.PCG;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import util.Sampler;
import static support.PCG.applyGrammar;
import valls.util.MathUtils;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;
import static support.PCG.applyGrammar;
import static support.PCG.embeddGraph;
import static support.PCG.applyGrammar;

/**
 *
 * @author santi
 */
public class GrammarStats {

    public static final boolean WRITE_ARFF = true;

    public Map<String, Double> processFiles(int seed, int size) throws Exception {
        File out_file_ns = new File("/Users/josepvalls/temp/pppp_samples/", "pcg_rx_" + size + "_" + seed + "_ns.txt");
        File out_file_ws = new File("/Users/josepvalls/temp/pppp_samples/", "pcg_rx_" + size + "_" + seed + "_ws.txt");
        if (!out_file_ws.exists()) {
            generateFiles(seed, size, out_file_ns, out_file_ws);
        }

        Map<String, Double> export_map = processFile(out_file_ns);
        //Map<String, Double> export_map = new HashMap();
        //double [] vector = new double[vector_1.length*2];
        //System.arraycopy( vector_1, 0, vector, 0, vector_1.length);

        /*Map<String, Double> export_map_ = processFile(out_file_ws);
        //System.arraycopy( vector_1, 0, vector, vector_1.length, vector_1.length );
        for (Entry<String, Double> entry : export_map_.entrySet()) {
            export_map.put("ws" + entry.getKey(), entry.getValue());
        }*/
        return export_map;
    }

    public Map<String, Double> processUnsolvedFiles(int seed, int size) throws Exception {
        return processFile(new File("/Users/josepvalls/temp/pppp_samples/", "pcg_" + size + "_" + seed + "_ns.txt"));
    }

    public Map<String, Double> processFile(File file) throws Exception {
        GameState gs;
        System.out.println("Processing: " + file.getAbsolutePath());
        gs = GameStateParser.parseFile(file.getAbsolutePath(), false);
        return getStatsOnGameState(gs);
    }

    public static void getVisualProperties(GameState gs, Map<String, Double> m) {
        m.put("v_w", (double) gs.getBoardState().getWidth());
        m.put("v_h", (double) gs.getBoardState().getHeight());

        int track_num = 0;
        for(Tile tile:gs.getBoardState().getTiles()){
            if (tile.isPassable()){
                track_num++;
            }
        }
        m.put("v_track_num", (double) track_num);
        double density = (double) track_num / gs.getBoardState().getTiles().length;
        m.put("v_trackDensity", density);
        m.put("v_componentDensity", (double) gs.getComponentState().getComponents().size() / (double) track_num);
        List<Integer> different_components_per_row = new ArrayList();
        List<Integer> same_components_per_row = new ArrayList();
        for (int x = 0; x < gs.getBoardState().getWidth(); x++) {
            Map<String, Integer> cp = new HashMap();
            for (int y = 0; y < gs.getBoardState().getHeight(); y++) {
                for (Component c : gs.getComponentsAt(x, y)) {
                    int val = 0;
                    if(cp.containsKey(c.getRepresentation())){
                        val = cp.get(c.getRepresentation());
                    }
                    cp.put(c.getRepresentation(), val + 1);

                }
            }
            if (cp.size() > 0) {
                same_components_per_row.add(Collections.max(cp.values()));
                different_components_per_row.add(cp.size());
            }
        }
        m.put("v_rowSameComponentsAvg", MathUtils.average(same_components_per_row));
        m.put("v_rowDifferentComponentsAvg", MathUtils.average(different_components_per_row));

        different_components_per_row = new ArrayList();
        same_components_per_row = new ArrayList();
        for (int y = 0; y < gs.getBoardState().getHeight(); y++) {
            Map<String, Integer> cp = new HashMap();
            for (int x = 0; x < gs.getBoardState().getWidth(); x++) {
                for (Component c : gs.getComponentsAt(x, y)) {
                    int val = 0;
                    if(cp.containsKey(c.getRepresentation())){
                        val = cp.get(c.getRepresentation());
                    }
                    cp.put(c.getRepresentation(), val + 1);

                }
            }
            if (cp.size() > 0) {
                same_components_per_row.add(Collections.max(cp.values()));
                different_components_per_row.add(cp.size());
            }
        }
        m.put("v_colSameComponentsAvg", MathUtils.average(same_components_per_row));
        m.put("v_colDifferentComponentsAvg", MathUtils.average(different_components_per_row));

        Set anchors_w = new HashSet();
        Set anchors_h = new HashSet();
        for (Component c : gs.getComponentState().getComponents()) {
            anchors_w.add(c.x);
            anchors_h.add(c.y);
        }
        // from ngo2003modelling
        m.put("v_Simplicity", 3.0 / (double) (anchors_w.size() + anchors_h.size() + gs.getComponentState().getComponents().size()));
        {
            List<List<Tile>> bv = getTracksInVerticalHalves(gs);
            double w0v = bv.get(0).size();
            double w1v = bv.get(1).size();
            double bmv = Math.abs(w0v - w1v) / Math.max(w0v, w1v);
            List<List<Tile>> bh = getTracksInHorizontalHalves(gs);
            double w0h = bh.get(0).size();
            double w1h = bh.get(1).size();
            double bmh = Math.abs(w0h - w1h) / Math.max(w0h, w1h);
            m.put("v_balanceTracks", (double) 1 - (bmv + bmh) / 2);
        }
        {
            List<List<Tile>> bv = getTracksInVerticalHalves(gs);
            //double w0v = bv.get(0).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            //double w1v = bv.get(1).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            double w0v = 0;
            double w1v = 0;
            for(Tile i:bv.get(0)){
                w0v += (double)i.component_index.size();
            }
            for(Tile i:bv.get(1)){
                w1v += (double)i.component_index.size();
            }
            double bmv = Math.abs(w0v - w1v) / Math.max(w0v, w1v);
            List<List<Tile>> bh = getTracksInHorizontalHalves(gs);
            //double w0h = bh.get(0).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            //double w1h = bh.get(1).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            double w0h = 0;
            double w1h = 0;
            for(Tile i:bh.get(0)){
                w0h += (double)i.component_index.size();
            }
            for(Tile i:bh.get(1)){
                w1h += (double)i.component_index.size();
            }
            double bmh = Math.abs(w0h - w1h) / Math.max(w0h, w1h);
            m.put("v_balanceComponents", (double) 1 - (bmv + bmh) / 2);
        }
        {
            List<Double> q_ = new ArrayList();
            for(List<Tile> i : GrammarStats.getTracksInQuarters(gs)){
                q_.add((double)i.size());
            }
            // q_ is the list of weights, tweak to break ties
            q_.set(0, q_.get(0) + 0.4);
            q_.set(1, q_.get(1) + 0.3);
            q_.set(2, q_.get(2) + 0.2);
            q_.set(3, q_.get(3) + 0.1);
            TreeMap<Double, Integer> t = new TreeMap();
            for (int i = 0; i < 4; i++) {
                t.put(q_.get(i), i);
            }
            List<Map.Entry<Double, Integer>> t_ = new ArrayList(t.entrySet());
            // t_ holds the weights, from smaller to bigger
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                int q = 4 - i;
                int v = 4 - t_.get(i).getValue();
                sum += Math.abs(q - v);
            }
            m.put("v_sequenceTracks", (double)(sum)/8);
        }
        {
            //List<List<Tile>> q__ = this.getTracksInQuarters(gs);
            //List<Double> q_ = q__.stream().map(i -> (double) (i.stream().map(j -> j.component_index.size()).reduce(0, Integer::sum))).collect(Collectors.toList());
            List<Double> q_ = new ArrayList();
            for(List<Tile> i : GrammarStats.getTracksInQuarters(gs)){
                int sum = 0;
                for(Tile j: i){
                    sum += j.component_index.size();
                }
                q_.add((double)sum);
                
            }
            // q_ is the list of weights, tweak to break ties
            q_.set(0, q_.get(0) + 0.4);
            q_.set(1, q_.get(1) + 0.3);
            q_.set(2, q_.get(2) + 0.2);
            q_.set(3, q_.get(3) + 0.1);
            TreeMap<Double, Integer> t = new TreeMap();
            for (int i = 0; i < 4; i++) {
                t.put(q_.get(i), i);
            }
            List<Map.Entry<Double, Integer>> t_ = new ArrayList(t.entrySet());
            // t_ holds the weights, from smaller to bigger
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                int q = 4 - i;
                int v = 4 - t_.get(i).getValue();
                sum += Math.abs(q - v);
            }
            m.put("v_sequenceComponents", (double) (sum) / 8);
        }

    }

    public static double[] getVisualPropertiesVector(GameState gs){
        double[] vector = new double[15];        
        vector[0] = (double) gs.getBoardState().getWidth();
        vector[1] = (double) gs.getBoardState().getHeight();
        vector[2] = ((double) gs.getBoardState().getWidth()) / ((double) gs.getBoardState().getHeight());
//        System.out.println(vector[0] + " x " + vector[1]);
        int track_num = 0;
        for(Tile tile:gs.getBoardState().getTiles()){
            if (tile.isPassable()){
                track_num++;
            }
        }
        vector[3] = (double) track_num;
        double density = (double) track_num / gs.getBoardState().getTiles().length;
        vector[4] = density;
        vector[5] = (double) gs.getComponentState().getComponents().size() / (double) track_num;

        List<Integer> different_components_per_row = new ArrayList();
        List<Integer> same_components_per_row = new ArrayList();
        for (int x = 0; x < gs.getBoardState().getWidth(); x++) {
            Map<String, Integer> cp = new HashMap();
            for (int y = 0; y < gs.getBoardState().getHeight(); y++) {
                for (Component c : gs.getComponentsAt(x, y)) {
                    int val = 0;
                    if(cp.containsKey(c.getRepresentation())){
                        val = cp.get(c.getRepresentation());
                    }
                    cp.put(c.getRepresentation(), val + 1);
                }
            }
            if (cp.size() > 0) {
                same_components_per_row.add(Collections.max(cp.values()));
                different_components_per_row.add(cp.size());
            }
        }
        vector[6] =  MathUtils.average(same_components_per_row);
        vector[7] =  MathUtils.average(different_components_per_row);

        different_components_per_row = new ArrayList();
        same_components_per_row = new ArrayList();
        for (int y = 0; y < gs.getBoardState().getHeight(); y++) {
            Map<String, Integer> cp = new HashMap();
            for (int x = 0; x < gs.getBoardState().getWidth(); x++) {
                for (Component c : gs.getComponentsAt(x, y)) {
                    int val = 0;
                    if(cp.containsKey(c.getRepresentation())){
                        val = cp.get(c.getRepresentation());
                    }
                    cp.put(c.getRepresentation(), val + 1);
                }
            }
            if (cp.size() > 0) {
                same_components_per_row.add(Collections.max(cp.values()));
                different_components_per_row.add(cp.size());
            }
        }
        vector[8] =   MathUtils.average(same_components_per_row);
        vector[9] =   MathUtils.average(different_components_per_row);

        Set anchors_w = new HashSet();
        Set anchors_h = new HashSet();
        for (Component c : gs.getComponentState().getComponents()) {
            anchors_w.add(c.x);
            anchors_h.add(c.y);
        }
        // from ngo2003modelling
        vector[10] =   3.0 / (double) (anchors_w.size() + anchors_h.size() + gs.getComponentState().getComponents().size());
        {
            List<List<Tile>> bv = getTracksInVerticalHalves(gs);
            double w0v = bv.get(0).size();
            double w1v = bv.get(1).size();
            double bmv = Math.abs(w0v - w1v) / Math.max(w0v, w1v);
            List<List<Tile>> bh = getTracksInHorizontalHalves(gs);
            double w0h = bh.get(0).size();
            double w1h = bh.get(1).size();
            double bmh = Math.abs(w0h - w1h) / Math.max(w0h, w1h);
            vector[11] =  (double) 1 - (bmv + bmh) / 2;
        }
        {
            List<List<Tile>> bv = getTracksInVerticalHalves(gs);
            //double w0v = bv.get(0).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            //double w1v = bv.get(1).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            double w0v = 0;
            double w1v = 0;
            for(Tile i:bv.get(0)){
                w0v += (double)i.component_index.size();
            }
            for(Tile i:bv.get(1)){
                w1v += (double)i.component_index.size();
            }
            double bmv = Math.abs(w0v - w1v) / Math.max(w0v, w1v);
            List<List<Tile>> bh = getTracksInHorizontalHalves(gs);
            //double w0h = bh.get(0).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            //double w1h = bh.get(1).stream().map(i -> i.component_index.size()).reduce(0, Integer::sum);
            double w0h = 0;
            double w1h = 0;
            for(Tile i:bh.get(0)){
                w0h += (double)i.component_index.size();
            }
            for(Tile i:bh.get(1)){
                w1h += (double)i.component_index.size();
            }
            double bmh = Math.abs(w0h - w1h) / Math.max(w0h, w1h);
            vector[12]= (double) 1 - (bmv + bmh) / 2;
        }
        {
            List<Double> q_ = new ArrayList();
            for(List<Tile> i : GrammarStats.getTracksInQuarters(gs)){
                q_.add((double)i.size());
            }
            // q_ is the list of weights, tweak to break ties
            q_.set(0, q_.get(0) + 0.4);
            q_.set(1, q_.get(1) + 0.3);
            q_.set(2, q_.get(2) + 0.2);
            q_.set(3, q_.get(3) + 0.1);
            TreeMap<Double, Integer> t = new TreeMap();
            for (int i = 0; i < 4; i++) {
                t.put(q_.get(i), i);
            }
            List<Map.Entry<Double, Integer>> t_ = new ArrayList(t.entrySet());
            // t_ holds the weights, from smaller to bigger
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                int q = 4 - i;
                int v = 4 - t_.get(i).getValue();
                sum += Math.abs(q - v);
            }
            
            vector[13]= (double)(sum)/8;
        }
        {
            //List<List<Tile>> q__ = this.getTracksInQuarters(gs);
            //List<Double> q_ = q__.stream().map(i -> (double) (i.stream().map(j -> j.component_index.size()).reduce(0, Integer::sum))).collect(Collectors.toList());
            List<Double> q_ = new ArrayList();
            for(List<Tile> i : GrammarStats.getTracksInQuarters(gs)){
                int sum = 0;
                for(Tile j: i){
                    sum += j.component_index.size();
                }
                q_.add((double)sum);
                
            }
            // q_ is the list of weights, tweak to break ties
            q_.set(0, q_.get(0) + 0.4);
            q_.set(1, q_.get(1) + 0.3);
            q_.set(2, q_.get(2) + 0.2);
            q_.set(3, q_.get(3) + 0.1);
            TreeMap<Double, Integer> t = new TreeMap();
            for (int i = 0; i < 4; i++) {
                t.put(q_.get(i), i);
            }
            List<Map.Entry<Double, Integer>> t_ = new ArrayList(t.entrySet());
            // t_ holds the weights, from smaller to bigger
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                int q = 4 - i;
                int v = 4 - t_.get(i).getValue();
                sum += Math.abs(q - v);
            }
            vector[14] = (double) (sum) / 8;
        }
        for(int i=0;i<vector.length;i++){
            if(Double.isNaN(vector[i])){
                vector[i]=0;
            }
        }
        return vector;
    }

    

    private static List<List<Tile>> getTracksInVerticalHalves(GameState gs) {
        List<Tile> left = new ArrayList();
        List<Tile> right = new ArrayList();
        List<List<Tile>> quadrants = GrammarStats.getTracksInQuarters(gs);
        left.addAll(quadrants.get(0));
        left.addAll(quadrants.get(2));
        right.addAll(quadrants.get(1));
        right.addAll(quadrants.get(3));
        List<List<Tile>> out = new ArrayList();
        out.add(left);
        out.add(right);
        return out;
    }

    private static List<List<Tile>> getTracksInHorizontalHalves(GameState gs) {
        List<Tile> left = new ArrayList();
        List<Tile> right = new ArrayList();
        List<List<Tile>> quadrants = GrammarStats.getTracksInQuarters(gs);
        left.addAll(quadrants.get(0));
        left.addAll(quadrants.get(1));
        right.addAll(quadrants.get(2));
        right.addAll(quadrants.get(3));
        List<List<Tile>> out = new ArrayList();
        out.add(left);
        out.add(right);
        return out;
    }

    private static List<List<Tile>> getTracksInQuarters(GameState gs) {
        // from ngo2003modelling
        // order is as in western reading: UL, UR, LL, LR
        List<List<Tile>> quadrants = new ArrayList();
        int mid_L = gs.getBoardState().getWidth() / 2 + gs.getBoardState().getWidth() % 2;
        int mid_R = gs.getBoardState().getWidth() / 2;
        int mid_T = gs.getBoardState().getHeight() / 2 + gs.getBoardState().getHeight() % 2;
        int mid_B = gs.getBoardState().getHeight() / 2;
        quadrants.add(new ArrayList());
        quadrants.add(new ArrayList());
        quadrants.add(new ArrayList());
        quadrants.add(new ArrayList());
        for (int x = 0; x < gs.getBoardState().getWidth(); x++) {
            for (int y = 0; y < gs.getBoardState().getHeight(); y++) {
                Tile t = gs.getBoardState().getTile(x, y);
                if (!t.isPassable()) {
                    continue;
                }
                if (x < mid_L && y < mid_T) {
                    quadrants.get(0).add(t);
                }
                if (x >= mid_R && y < mid_T) {
                    quadrants.get(1).add(t);
                }
                if (x < mid_L && y >= mid_B) {
                    quadrants.get(2).add(t);
                }
                if (x >= mid_R && y >= mid_B) {
                    quadrants.get(3).add(t);
                }
            }
        }
        return quadrants;
    }

    private void getLevelProperties(GameState gs, Map<String, Double> export_map) {
        export_map.put("num_components", (double) gs.getComponentState().getComponents().size());
        export_map.put("num_semaphores", (double) gs.getComponentState().getComponentsByType(ComponentSemaphore.class).size());
        export_map.put("num_buttons", (double) gs.getComponentState().getComponentsByType(ComponentSignal.class).size());
        export_map.put("num_exchange_points", (double) gs.getComponentState().getComponentsByType(ComponentExchange.class).size());
        export_map.put("num_diverters", (double) gs.getComponentState().getComponentsByType(ComponentDiverter.class).size());
        export_map.put("num_threads", (double) gs.getComponentState().getComponentsByType(ComponentUnit.class).size());
        export_map.put("num_pickups", (double) gs.getComponentState().getComponentsByType(ComponentPickup.class).size());
        export_map.put("num_deliveries", (double) gs.getComponentState().getComponentsByType(ComponentDelivery.class).size());
    }

    public Map<String, Double> getStatsOnGameState(GameState gs) {
        // Collect level statistics
        Map<String, Double> export_map = new HashMap();
        //export_map.putAll(rule_applications);
        // Level properties
        getLevelProperties(gs, export_map);
        // Visual Properties
        getVisualProperties(gs, export_map);
        // ME Properties
        GameStateSearch gss = new GameStateSearch(gs);
        gss.setSearchOptions(false, true, true, false);
        gss.setSearchBudget(50000);
        gss.setSearchTime(1000);
        gss.search();
        gss.getStatsIntoMap(export_map);
        return export_map;

    }

    public void generateJSON(int seed, int size) throws Exception {
        // Generate
        System.out.println("USING SEED " + Integer.toString(seed));
        File out_file = new File("/Users/josepvalls/temp/pppp_samples/", "pcg_"+size+"_"+seed+".json");
        if(out_file.exists()) return;
        
        Map<String, Integer> rule_applications = new HashMap();
        LGraph graph = PCG.generateGraph(seed, size, true, true, null);
        GameState gs = embeddGraph(graph, seed, true);
        
        Gson gson = new GsonBuilder().create();
        String out = gson.toJson(rule_applications);
        PrintWriter writer = new PrintWriter(out_file);
        writer.print(out);
        writer.close();

    }
    
    public void generateFiles(int seed, int size, File out_file_ns, File out_file_ws) throws Exception {
        // Generate
        System.out.println("USING SEED " + Integer.toString(seed));
        Map<String, Integer> rule_applications = new HashMap();
        LGraph graph = PCG.generateGraph(seed, size, true, true, null);
        GameState gs = embeddGraph(graph, seed, true);
        String out = GameStateExporter.export(gs);
        PrintWriter writer = new PrintWriter(out_file_ws);
        writer.print(out);
        writer.close();
        // Remove solution
        Sort.clearSorts();
        Ontology ontology = new Ontology("data/ppppOntology4.xml");
        graph = PCG.generateGraph(seed, size, false, true, rule_applications);
        gs = embeddGraph(graph, seed, true);
        out = GameStateExporter.export(gs);
        writer = new PrintWriter(out_file_ns);
        writer.print(out);
        writer.close();
        
        Gson gson = new GsonBuilder().create();
        out = gson.toJson(rule_applications);
        File out_file = new File("/Users/josepvalls/temp/pppp_samples/", "pcg_"+size+"_"+seed+".json");
        writer = new PrintWriter(out_file);
        writer.print(out);
        writer.close();

        // Collect general statistics
        for (Entry<String, Integer> entry : rule_applications.entrySet()) {
            if (!rule_applications_general.containsKey(entry.getKey())) {
                rule_applications_general.put(entry.getKey(), 0);
            }
            rule_applications_general.put(entry.getKey(), rule_applications_general.get(entry.getKey()) + entry.getValue());
        }
    }
    Map<String, Integer> rule_applications_general;

    public void run() throws Exception {
        rule_applications_general = new HashMap();

        File out_file = new File("/Users/josepvalls/temp/pppp_samples/", WRITE_ARFF ? "dataset___.arff" : "dataset.txt");
        PrintWriter writer = new PrintWriter(out_file);
        Map<String, Double> row;

        List<String> keys = null;

        for (int size = 0; size < 8; size++) {
            for (int i = 0; i < (size+1)*10; i++) {
                //row = processFiles(i,size);
                //row = processUnsolvedFiles(i, size);
                row = processFiles(i, size);
                if (keys == null) {
                    // prepare the header
                    if(WRITE_ARFF){
                        writer.print("@RELATION pppp\n");
                    }
                    keys = new ArrayList(row.keySet());
                    java.util.Collections.sort(keys);
                    for (int j = 0; j < keys.size(); j++) {
                        if(WRITE_ARFF){
                            writer.print("@ATTRIBUTE ");
                            writer.print(keys.get(j));
                            writer.print(" NUMERIC\n");
                        } else {
                            writer.print("\t");
                        }
                    }
                    if(WRITE_ARFF){
                        writer.print("@ATTRIBUTE class {PCG,Manual}\n");
                        writer.print("@DATA\n");
                    } else {
                        writer.print("\n");
                    }
                }
                for (int j = 0; j < keys.size(); j++) {
                    writer.print(row.get(keys.get(j)));
                    if(WRITE_ARFF){
                        writer.print(',');
                    } else {
                        writer.print('\t');
                    }
                }
                writer.print("PCG");
                writer.print('\n');
                //break;
            }
            //break;
        }
        /*
        String path = "/Users/josepvalls/paraprog/Assets/Resources/Levels";
        for (int level = 1; level < 10; level++) {
            System.out.println("LEVEL" + level);
            File file = new File(path, "level0" + level + ".txt");
            row = processFile(file);
            for (int j = 0; j < keys.size(); j++) {
                writer.print(row.get(keys.get(j)));
                if(WRITE_ARFF){
                    writer.print(',');
                } else {
                    writer.print('\t');
                }
            }
            writer.print("Manual");
            writer.print('\n');
        }
        */
        writer.close();
        System.out.println("FINAL SUMMARY");
        for (Map.Entry<String, Integer> entry : rule_applications_general.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }

    }

    public static void main(String args[]) throws Exception {
        GameStateParser.prepareParser(); // we need the component names
        GrammarStats gs = new GrammarStats();
        gs.run();
    }
}
