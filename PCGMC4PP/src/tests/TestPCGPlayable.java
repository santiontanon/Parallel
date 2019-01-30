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
import game.GoalCondition;
import game.execution.ExecutionPlan;
import gui.BoardGameStateJFrame;
import java.io.File;
import java.util.LinkedHashMap;
import support.PCG;
import support.PCGPlayerModelUtils;
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
    
    public static int WINDOW_WIDTH = 800;
    public static int WINDOW_HEIGHT = 400;

    public static void main(String args[]) throws Exception {
        
        
//        LinkedHashMap<String, Double> playerModel = PCG.loadPlayerModel("currentParameters.txt");
        LinkedHashMap<String, Double> playerModel = null;
        if (playerModel != null) System.out.println("Player model recommended level size: " + PCGPlayerModelUtils.determineLevelSize(playerModel));
        
        int accumWidth = 0;
        boolean debug = false;
        for(int size=1;size<=1;size++){        
//            for(int randomSeed=0;randomSeed<20000;randomSeed+=500){
            for(int randomSeed=1140;randomSeed<1300;randomSeed+=10){
//            int randomSeed = 1140; {
                System.out.println("randomSeed: " + randomSeed);
//                LGraphGrammarSampler.DEBUG = 1;
                //OrthographicEmbeddingBoardSizeOptimizer.DEBUG = 1;
                //GameState gs = PCG.generateGameState(-randomSeed,-randomSeed, size, true, false, playerModel, debug);
                GameState gs = PCG.generateGameState(-randomSeed,-randomSeed, size, false, false, playerModel, debug);
                System.out.println(gs.skills);
//                GameState gs = PCG.generateGameState(randomSeed,randomSeed, size, false, true);
//                new BoardGameStateJFrame("level " + randomSeed, WINDOW_WIDTH, WINDOW_HEIGHT, gs);                
                //PCG.export(gs, new File("PCG-test-level.txt"), null);
                if (!solvable(gs)) {
                    BoardGameStateJFrame f = new BoardGameStateJFrame("level", WINDOW_WIDTH, WINDOW_HEIGHT, gs);                
                    System.err.println("Level is not solvable! randomSeed: " + randomSeed);
                    //break;
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
        System.out.println("goal_struct.size() = " + gs2.getBoardState().goal_struct.size());
        if (gs2.getBoardState().goal_struct.size() == 0) {
            BoardGameStateJFrame f = new BoardGameStateJFrame("level (after simulation)", WINDOW_WIDTH, WINDOW_HEIGHT, gs2);                
            System.err.println("There are no goals!");
            return false;
        }
        for (int i = 0; i < gs2.getBoardState().goal_struct.size(); i++) {
            GoalCondition goal = gs2.getBoardState().goal_struct.get(i);
            if(goal.goal_type==GoalCondition.GOAL_REQUIRED && !gs2.testGoal(goal)){
                BoardGameStateJFrame f = new BoardGameStateJFrame("level (after simulation)", WINDOW_WIDTH, WINDOW_HEIGHT, gs2);                
                System.err.println("Goal not achieved: " + goal.toString());
                return false;
            } else {
                System.out.println("Goal achieved: " + goal.toString());
            }
        }
        return true;
    }
}
