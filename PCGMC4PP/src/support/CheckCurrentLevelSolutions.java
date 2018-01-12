/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package support;

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
import java.io.FileWriter;
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

/**
 *
 * @author santi
 */
public class CheckCurrentLevelSolutions {
    public Map<String, Double> getStatsOnGameState(GameState gs) {
        Map<String, Double> export_map = new HashMap();
        GameStateSearch gss = new GameStateSearch(gs);
        gss.setSearchOptions(false, true, true, true);
        gss.setSearchBudget(6000000);
        gss.setSearchTime(100000);
        gss.search();
        gss.getStatsIntoMap(export_map);
        
        System.out.println("OK:"+gss.getResultsSuccessful().size()+"\tBAD:"+(gss.getResultsDeadend().size()+gss.getResultsProblematic().size()));
        return export_map;
    }

    public void run() throws Exception {

        List<String> keys = null;

        String path_levels = "/Users/josepvalls/paraprog/Assets/Resources/Levels";
        String path_solutions = "/Users/josepvalls/paraprog/data/solutions";
        for (int level = 1; level <= 58; level++) {
            System.out.println("LEVEL " + level);
            String filename = null;
            File file;
            GameState gs;
            Map<String, Double> stats;
            // Process the solution first to get the time_efficiency average
            if(level<10){
                filename = "level0" + level + "Solution.txt";
            } else {
                filename = "level" + level + "Solution.txt";
            }
            file = new File(path_solutions, filename);       
            System.out.println("Processing: " + file.getAbsolutePath());
            try{
            gs = GameStateParser.parseFile(file.getAbsolutePath(), false);
            stats = getStatsOnGameState(gs);
            } catch (Exception ex){
                System.out.println("No solution? for "+filename);
                continue;
            }
            
            if(level<10){
                filename = "level0" + level + ".txt";
            } else {
                filename = "level" + level + ".txt";
            }
            file = new File(path_levels, filename);
            System.out.println("Processing: " + file.getAbsolutePath());
            gs = GameStateParser.parseFile(file.getAbsolutePath(), false);
            if(true){
                // Update or not?
                gs.getBoardState().time_efficiency = stats.get(new String("time_efficiency")).floatValue();
                FileWriter fw = new FileWriter(file);
                fw.write(GameStateExporter.export(gs));
                fw.close();
            }
            stats = getStatsOnGameState(gs);
        }
    }

    public static void main(String args[]) throws Exception {
        GameStateParser.prepareParser(); // we need the component names
        CheckCurrentLevelSolutions gs = new CheckCurrentLevelSolutions();
        gs.run();
    }
}
