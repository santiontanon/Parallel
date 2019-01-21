/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import game.GameState;
import game.GameStateSearch;
import java.io.File;
import java.io.PrintWriter;
import support.GameStateExporter;
import support.GameStateParser;

/**
 *
 * @author santi
 */
public class TestME {
    public static void main(String args[]) throws Exception
    {
        String filename = "data/ParallelMEInput_ME_20190118233118.txt";
        String filename_me = "data/ParallelMEOut_ME_20190118233118.txt";
        GameState gs = GameStateParser.parseFile(filename, false);

                GameStateSearch gss = new GameStateSearch(gs);
                //gss.verbose = true;
                gss.setSearchBudget(600000);
                gss.setSearchOptions(false, true, true, true);
                gss.search();
                GameState result = gss.getWorstResult();
                if(result==null){
                    System.out.println("Search space exhausted without solutions");
                    System.exit(4);                    
                } else {
                    System.out.println("result has " + result.getSteps() + " steps");
                    String out = GameStateExporter.export(gss, null, null);
                    File out_file = new File(filename_me);
                    PrintWriter writer = new PrintWriter(out_file);
                    writer.print(out);
                    writer.close();
                    System.out.println(out_file.getAbsolutePath());
                    System.exit(0);
                }
        
        
        
    }
}
