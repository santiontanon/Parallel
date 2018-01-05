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
package tests;

import game.GameState;
import game.GameStateSearch;
import java.util.Arrays;
import java.util.Collections;
import support.GameStateExporter;
import support.GameStateParser;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class TestSearch {
    // TODO goal condition delivery on threads doesn't work for level-1 because it's not considered for symmetries, works for delivery points
    
    public static void main(String args[]) throws Exception {
        String level_path;
        level_path = "/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/";
        level_path = "/Users/josepvalls/paraprog/pcg_pppp_samples/";
        level_path = "/Users/josepvalls/Dropbox/projects/unity/ParallelProg/persistent/";
        level_path = "/Users/josepvalls/Desktop/";
        
        String[] levels;
        levels = new String[]{
            "level-1-prototype.txt", //ok=10
            //"level-2-prototype.txt", //fails, infinite loop
            //"level-2-prototype-solved.txt", //there are good/bad solutions
            //"level-2-prototype-solved-more-complex.txt", //there are problematic solutions missed=20
            //"level-2-prototype-solved-better.txt", //there are good solutions, currently in more time because it goes deeper
            //"level-3-prototype.txt", //missed package = 20
            "level-4-prototype.txt", //this is dumb, it requieres nothing if the exchange points stop
            //"level-complex-mini.txt", //good and deadlock; doesn't work now, it relies on colors
            //"level-colors-expanded.txt", // only finds solution with budget of ~1000000 and 30 seconds to run
            "level-5-prototype.txt", //misses packages=20
            "level-5-prototype-solved.txt", //only good solutions
            "level-6-prototype.txt", // problematic missing = 20
            "level-6-prototype-solved.txt", // ok
            "level-6-prototype-solved-better.txt", // ok, same as before
            // TODO Q as a metric, maybe count number of packages delivered?
            "level-7-prototype.txt", // problematic, starvation = 36
            "level-7-prototype-solved.txt", // ok
            //"level-nocolors.txt", // only finds solution with budget of ~1000000 and 30 seconds to run
        };
        if(true){
            levels = new String[]{
                //"level-2-prototype-solved-better.txt", //there are good/bad solutions
                //"level-3-prototype-solved.txt", //there are good/bad solutions
                "level-8-prototype-solved-better.txt",
                //"level-colors-expanded.txt",
                //"level-nocolors.txt"
                //"level-3-prototype.txt", //there are good/bad solutions
                    
            };
        }
        if(true){
            levels = new String[]{
                //"level-2-prototype-solved-better.txt", //there are good/bad solutions
                //"level-3-prototype-solved.txt", //there are good/bad solutions
                //"pcg_5_1.txt",
                //"level-colors-expanded.txt",
                //"level-nocolors.txt"
                //"level-3-prototype.txt", //there are good/bad solutions
                    
            };
        }
        if(true){
            levels = new String[]{
                //"me_out_3955233849799548641.txt" // this is the 1 pickup 2 deliveries 2 loops problem 
                //"MEInput_ME_20170913132159.txt" // the big level jess is having trouble with, when reduced to 2 threads finishes fine
                //"MEInput_ME_20170915155111.txt"  
                //"me_out_8020943236649410885.txt"
                //"MEInput_PLAY_20170921162026.txt"
                //"MEInput_ME_20170922144142.txt"
                //"me_out_535435366883318269.txt"
                //"me_out_7113927654307391890.txt"
                "MEInput_PLAY_20171003172159.txt"
                //"level-3-prototype-solved.txt", //there are good/bad solutions
                //"pcg_5_1.txt",
                //"level-colors-expanded.txt",
                //"level-nocolors.txt"
                //"level-3-prototype.txt", //there are good/bad solutions
                    
            };
                    
                    
        }
        GameState gs;
        for (String level : levels) {
            System.out.println("Loading "+level);
            gs = GameStateParser.parseFile(level_path+level, levels.length==1);

            GameStateSearch gss = new GameStateSearch(gs);
            //gss.setSearchOptions(false, true, true, false);
            gss.setSearchOptions(false, true, true, true); // these are the options set in the ME used for the game
            gss.verbose = true;
            //gss.setSearchTime(30000);
            //gss.setSearchTime(1000);
            gss.setSearchBudget(30000);
            gss.setSearchBudget(60000);  // this is the budget currently set in the ME used for the game
            gss.setSearchBudget(60000);
            gss.search();

            if (gss.isSearchOverBudget()) {
                System.out.println("Search budget depleted");
            }
            if (gss.isSearchSpaceExhausted()) {
                System.out.println("Search space exhausted");
            }
            System.out.println("DETAILED STATS");
            gss.printStats();
            GameState result = gss.getWorstResult();
            System.out.println("END of DETAILED STATS");
            System.out.println("Result of type "+result.result_type+" in time\t"+result.getTime()+" steps\t"+result.getSteps()+"\t"+result);
            if (levels.length==1 && gss.getAllResults().size() > 0) {
                //System.out.println("FIRST RESULTS");
                //System.out.println(GameStateExporter.export(gss.getStart(), GameStateSearch.toExecutionPlan(gss.getAllResults().get(0)), null));
            }
            System.out.println("WORST RESULTS");
            //System.out.println(GameStateExporter.export(gss.getStart(), GameStateSearch.toExecutionPlan(gss.getWorstResult()), null));
            System.out.println(GameStateExporter.exportFinalCondition(gss.getWorstResult()));// export(gss.getStart(), GameStateSearch.toExecutionPlan(gss.getWorstResult()), null));
        }
    }
}
