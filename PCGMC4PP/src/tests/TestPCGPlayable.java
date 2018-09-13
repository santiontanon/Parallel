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

import java.io.File;
import game.GameState;
import game.GoalCondition;
import game.execution.ExecutionPlan;
import gui.BoardGameStateJFrame;
import lgraphs.sampler.LGraphGrammarSampler;
import optimization.OrthographicEmbeddingBoardSizeOptimizer;
import support.PCG;
import support.Play;

/*

to do:
    OK - in the JESS1 pattern, have a way to divert the arrow with the package to the proper path
    - re-add the other rules, and see how they interact
    - try to beat a few levels
    - push to the repo
*/


/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class TestPCGPlayable {

    public static void main(String args[]) throws Exception {
        int bitmask = 0;
        for (int i = 0; i < 8; i++) {
            System.out.println((char) ('@' + bitmask));
            System.out.println(1 << i + '@');
            //System.out.println(1<<i);
            bitmask = 1 << i;
        }

        /*GameState gs;
        gs = MapGenerator.SmallestTest();
        System.out.println(GameStateExporter.export(gs));
        gs = MapGenerator.SmallerTest();
        System.out.println(GameStateExporter.export(gs));
        gs = MapGenerator.SmallTest();
        System.out.println(GameStateExporter.export(gs));
        gs.setBoardState(gs.getBoardState().expand(7, 3));
        System.out.println(GameStateExporter.export(gs));
        gs = MapGenerator.SmallestTest();
        gs.setBoardState(gs.getBoardState().expand(7, 3));
        System.out.println(GameStateExporter.export(gs));*/
        
        String batchId;
        //batchId = "week2"; // Doesn't have SYNCRO nor DEADLOCK
        //batchId = "week45"; // Doesn't have SYNCRO
        //batchId = "week79"; 
        //batchId = "extra4";
        batchId = "santiTest";
        int accumWidth = 0;
        for(int size=1;size<=1;size++){        
//            for(int randomSeed=0;randomSeed<10;randomSeed++){
            int randomSeed = 10;
            {
                LGraphGrammarSampler.DEBUG = 1;
                //OrthographicEmbeddingBoardSizeOptimizer.DEBUG = 1;
                GameState gs = PCG.generateGameState(randomSeed,randomSeed, size, true, true);
                BoardGameStateJFrame f = new BoardGameStateJFrame("level", 1280, 640, gs);                
//                GameState gs = PCG.generateGameState(randomSeed,randomSeed, size, false, true);
                if (!solvable(gs)) {
                    System.err.println("Level is not solvable!");
                } else {
                    System.out.println("Level worked!");
                }                
                accumWidth+=gs.bs.getWidth();
            }
        }
        System.out.println("accumWidth = " + accumWidth);
    }
    
    
    public static boolean solvable(GameState gs)
    {
        int executionRandomSeed = 1;
        int how_many = 1000;
        int number_of_loops_before_breaking = 5;
        boolean break_when_complete = true;
        gs.init();
        ExecutionPlan ep = Play.PlayLevel(gs, executionRandomSeed, how_many, number_of_loops_before_breaking, break_when_complete);
        System.out.println("Simulation generated " + ep.getStates().size() + " states");
//        for(GameState gs2:ep.getStates()) {
//            System.out.println("  " + gs2.getTime());
//        }
        GameState gs2 = ep.getStates().get(ep.getStates().size()-1);
        BoardGameStateJFrame f = new BoardGameStateJFrame("level (after simulation)", 1280, 640, gs2);                
        for (int i = 0; i < gs2.getBoardState().goal_struct.size(); i++) {
            GoalCondition goal = gs2.getBoardState().goal_struct.get(i);
            if(goal.goal_type==GoalCondition.GOAL_REQUIRED && !gs2.testGoal(goal)){
                System.err.println("Goal not achieved: " + goal.toString());
                return false;
            } else {
                System.out.println("Goal achieved: " + goal.toString());
            }
        }
        return true;
    }
}
