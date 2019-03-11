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
package game.execution;

import game.GameState;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ExecutionPlan {

    String description = "No execution plan yet...";
    List<GameState> states;
    int current = 0;

    public String getDescription() {
        return description;
    }

    public List<GameState> getStates() {
        return states;
    }

    public int getCurrent() {
        return current;
    }

    public void reset() {
        this.current = 0;
    }

    public String toString() {
        return this.description+" (S: "+this.getStates().size()+ ", T: "+(this.getStates().size()==0?0:this.getStates().get(this.getStates().size()-1).getTime())+")";

    }

    public ExecutionPlan() {
        this.states = new ArrayList();
    }
    public ExecutionPlan(List<GameState> states) {
        this.states = states;
        if(states.size()>0){
            GameState ending = states.get(states.size()-1);
            if ((ending.result_type & GameState.RESULT_DEADEND) == GameState.RESULT_DEADEND){
                this.description = "Deadend";
            } else if ((ending.result_type & GameState.RESULT_PARTIAL) == GameState.RESULT_PARTIAL){
                this.description = "Partial";
            } else if ((ending.result_type & GameState.RESULT_PROBLEMATIC) == GameState.RESULT_PROBLEMATIC){
                this.description = "Problematic";
            } else if ((ending.result_type & GameState.RESULT_SUCCESSFUL) == GameState.RESULT_SUCCESSFUL){
                this.description = "Successful";
            } else if ((ending.result_type & GameState.RESULT_INCOMPLETE) == GameState.RESULT_INCOMPLETE){
                this.description = "Incomplete";
            } else {
                this.description = "Unknown";
            }
    } else {
            this.description = "Empty";
        }
    }
    public void addDescriptionComment(String str){
        this.description = this.description + " (" + str + ")";
    }

    
    public GameState next(GameState current) {
        if (this.current < this.states.size()) {
            return this.states.get(this.current++);
        }
        return current;
    }

        
}
