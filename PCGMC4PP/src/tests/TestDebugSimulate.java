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
import game.GoalCondition;
import game.component.Component;
import game.component.ComponentSemaphore;
import game.component.ComponentSignal;
import game.execution.ExecutionPlan;
import gui.BoardGameStateJFrame;
import java.util.List;
import support.GameStateExporter;
import support.GameStateParser;
import support.Play;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class TestDebugSimulate {
    
    /*
    
    Notes:
        - if the random seed is 0 or higher, it is deterministic and it selects a solution based on the random seed
        - if the random seed is negative, it uses the Java Random class to randomly select a simulation
    
    */
    
    public static void main(String args[]) throws Exception {
        String filename = "Assets/Resources/Levels/level08.txt";
        GameState gs = GameStateParser.parseFile(filename, false);
        
        // add solution 1:
        /*
        ComponentSemaphore semaphore1 = new ComponentSemaphore(0, 5, 20000 ,0, Component.OWNER_PLAYER, false);
        semaphore1.value = ComponentSemaphore.RED;
        ComponentSemaphore semaphore2 = new ComponentSemaphore(8, 5, 20001 ,0, Component.OWNER_PLAYER, false);
        semaphore2.value = ComponentSemaphore.GREEN;
        ComponentSignal signal1 = new ComponentSignal(0, 3, 20002, 0, Component.OWNER_PLAYER, false);
        signal1.link = 20001;
        ComponentSignal signal2 = new ComponentSignal(8, 3, 20003, 0, Component.OWNER_PLAYER, false);
        signal2.link = 20000;
        ComponentSignal signal3 = new ComponentSignal(4, 1, 20004, 0, Component.OWNER_PLAYER, false);
        signal3.link = 5001;
        gs.cs.addComponent(semaphore1);
        gs.cs.addComponent(semaphore2);
        gs.cs.addComponent(signal1);
        gs.cs.addComponent(signal2);
        gs.cs.addComponent(signal3);
        */
        // add solution 2:
        ComponentSignal signal1 = new ComponentSignal(3, 0, 20002, 0, Component.OWNER_PLAYER, false);
        signal1.link = 5001;
        ComponentSignal signal2 = new ComponentSignal(5, 0, 20003, 0, Component.OWNER_PLAYER, false);
        signal2.link = 5001;
        gs.cs.addComponent(signal1);
        gs.cs.addComponent(signal2);
        gs.updateGameStateDescriptionLength();
        gs.init();
        BoardGameStateJFrame f = new BoardGameStateJFrame("level (before simulation)", 800, 600, gs);                

        // Get all successors:
        /*
        List<GameState> successors = gs.getSuccessors();
        for(GameState gs2:successors) {
            BoardGameStateJFrame f2 = new BoardGameStateJFrame("level (successor "+successors.indexOf(gs2)+")", 800, 600, gs2);                
            List<GameState> successors2 = gs2.getSuccessors();
            for(GameState gs3:successors2) {
                BoardGameStateJFrame f3 = new BoardGameStateJFrame("level (successor "+successors.indexOf(gs2)+"-"+successors2.indexOf(gs3)+")", 800, 600, gs3);
            }
        }
        */
        
        // Play:
        /*
        int deterministic = -1771;
        int how_many = 1000;
        int number_of_loops_before_breaking = 5;
        boolean break_when_complete = true;
        ExecutionPlan ep = Play.PlayLevel(gs, deterministic, how_many, number_of_loops_before_breaking, break_when_complete);
        String out = GameStateExporter.export(gs, ep, null);
        System.out.println(out);      
        GameState gs2 = ep.getStates().get(ep.getStates().size()-1);
        */
        
        // ME:
        GameStateSearch gss = new GameStateSearch(gs);
        gss.setSearchBudget(60000);
        gss.setSearchOptions(false, true, true, true);
//        gss.verbose = true;
        gss.search();        
        GameState gs2 = gss.getWorstResult();
//        String out = GameStateExporter.export(gss,null);
//        System.out.println(out);      
        
        BoardGameStateJFrame f2 = new BoardGameStateJFrame("level (after simulation)", 800, 600, gs2);                
        List<GameState> successors = gs2.getSuccessors();
        System.out.println("successors of final state: " + successors.size());

        System.out.println("goal_struct.size() = " + gs2.getBoardState().goal_struct.size());
        if (gs2.getBoardState().goal_struct.size() == 0) {
            System.err.println("There are no goals!");
            return;
        }
        for (int i = 0; i < gs2.getBoardState().goal_struct.size(); i++) {
            GoalCondition goal = gs2.getBoardState().goal_struct.get(i);
            if(goal.goal_type==GoalCondition.GOAL_REQUIRED && !gs2.testGoal(goal)){
                System.err.println("Goal not achieved: " + goal.toString());
                return;
            } else {
                System.out.println("Goal achieved: " + goal.toString());
            }
        }
    }
}
