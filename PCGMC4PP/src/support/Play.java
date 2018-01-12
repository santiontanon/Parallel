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

import game.GameState;
import game.GameStateDescriptionWrapper;
import game.GameStateSearch;
import game.execution.ExecutionDeterministic;
import game.execution.ExecutionDeterministic1;
import game.execution.ExecutionFair;
import game.execution.ExecutionNonDeterministic;
import game.execution.ExecutionPlan;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class Play {

    public static void main(String args[]) {
        boolean debug = false;
        // ExitCode = 0: OK
        // ExitCode = 1: System Errors
        // ExitCode = 2: Errors parsing input, use support.validate to check input
        // ExitCode = 3: Search budget depleted, no results, try a bigger budget
        // ExitCode = 4: No results found, search space exhausted (other search errors)
        // ExitCode = 5: Wrong arguments
        // ExitCode = 6: Other exception within the ME Java code
        if (args.length >= 2) {
            try {
                String filename = args[0];
                int deterministic = Integer.parseInt(args[1]);
                int how_many = 1000;
                int number_of_loops_before_breaking = 5;
                boolean break_when_complete = true;
                GameState gs = GameStateParser.parseFile(filename, false);
                if (GameStateParser.errors > 0) {
                    System.out.println("Errors parsing input file: " + GameStateParser.errors);
                    System.exit(2);
                }
                if (args.length >= 3) {
                    how_many = Integer.parseInt(args[2]);
                }
                Map<GameStateDescriptionWrapper,Integer> gsh = new HashMap();
                List<GameState> eps = new ArrayList();
                eps.add(gs);
                GameState gs_ = gs.clone();
                for(int i=0;i<how_many;i++){
                    List<GameState> successors = gs_.getSuccessors();
                    if (successors.size() > 0) {
                        GameState successor;
                        if (deterministic < 0) {
                            Random rand = new Random();
                            gs_ = successors.get(rand.nextInt(successors.size()));
                        } else if (deterministic == 0) {
                            gs_ = successors.get(0);
                        } else {
                            gs_ = successors.get(deterministic % successors.size());
                        }
                        GameStateDescriptionWrapper gsw = new GameStateDescriptionWrapper(gs_);
                        //System.out.println(Arrays.toString(gs_.stateDescription()));
                        if(gsh.containsKey(gsw)){
                            int num = gsh.get(gsw);
                            num++;
                            if(num>number_of_loops_before_breaking){
                                break;
                            }
                            gsh.put(gsw, num);
                        } else {
                            gsh.put(gsw, 0);
                        }
                        eps.add(gs_);
                    } else {
                        break;
                    }
                    if(break_when_complete && gs_.isStateComplete()) break;
                }
                ExecutionPlan ep = new ExecutionPlan(eps);
                String out = GameStateExporter.export(gs, ep, null);
                if(debug){
                    System.out.println(out);
                } else {
                    File file = new File(filename);
                    String path = file.getAbsolutePath();
                    File out_file = File.createTempFile("play_out_",".txt",file.getParentFile());
                    PrintWriter writer = new PrintWriter(out_file);
                    writer.print(out);
                    writer.close();
                    System.out.println(out_file.getAbsolutePath());
                }
                System.exit(0);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                System.exit(6);
            }
        } else {
            System.out.println("Usage: support.Play filename selection_strategy [how_many]");
            System.exit(4);
        }
    }
}
