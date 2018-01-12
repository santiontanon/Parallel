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
package game;

import game.execution.ExecutionPlan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import valls.util.MathUtils;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class GameStateSearch {

    GameState start;

    public GameState getStart() {
        return start;
    }
    private LinkedList<GameState> open;
    private Set<GameStateDescriptionWrapper> closed_hashes;
    private Set<GameStateDescriptionWrapper> open_hashes;

    public List<GameState> results_successful;
    public List<GameState> results_problematic;
    public List<GameState> results_deadend;
    private GameState result_last_current = null;
    private GameState result_first_long = null;
    private GameState result_first_longer = null;

    public int nodes_ignored_symmetry = 0;
    public int nodes_ignored_unfair_scheduler = 0;
    public int result_successful_depth = Integer.MAX_VALUE;
    public int nodes_ignored_result_successful_depth = 0;

    private int search_budget = 1000;
    //private int search_millis = Integer.MAX_VALUE;
    private int search_millis = 60000;
    private long search_millis_end = Long.MAX_VALUE;
    private int search_cost = 0;
    private boolean search_space_exhausted = false;
    private int unfair_scheduler_threshold = Integer.MAX_VALUE;
    
    public boolean verbose = false;

    private boolean break_on_first_successful = true;
    private boolean break_on_first_deadlock = true;
    private boolean break_on_first_problematic = false;
    private boolean drop_symmetries_successors = true;
    
    public static int MINIMUM_STEPS_REQUIRED_IN_SOLUTION_IN_ORDER_TO_SHOW_SOMETHIN_MOVING = 25;

    public void setSearchOptions(boolean break_on_first_successful, boolean break_on_first_deadlock, boolean break_on_first_problematic, boolean drop_symmetries_successors) {
        this.break_on_first_successful = break_on_first_successful;
        this.break_on_first_deadlock = break_on_first_deadlock;
        this.break_on_first_problematic = break_on_first_problematic;
        this.drop_symmetries_successors = drop_symmetries_successors;

    }
    
    public float getTimeEfficiencyAverageDifference(){
        // =< 0 it's a good solution
        return this.getAverageTime() - this.getStart().getBoardState().time_efficiency;
    }

    public List<GameState> getAllResults() {
        List<GameState> results = new ArrayList();
        results.addAll(results_deadend);
        results.addAll(results_problematic);
        results.addAll(results_successful);
        return results;
    }
    public float getAverageTime(){
        float total = 0.0f;
        int count = 0;
        for(GameState gs : this.getAllResults()){
            count +=1;
            total += gs.getTime();
        }
        if(count>0){
            total/=count;
            return total;
        } else {
            return 1.0f;
        }
        
    }
    public List<GameState> getSomeResults() {
        List<GameState> results = new ArrayList();
        if (results_deadend.size() > 0) {
            results.add(results_deadend.get(0));
        }
        if (results_problematic.size() > 0) {
            results.add(results_problematic.get(0));
        }
        if (results_successful.size() > 0) {
            results.add(results_successful.get(0));
        }
         return results;
    }
    public static class GameStateComparatorByTime implements Comparator<GameState> {
        @Override
        public int compare(GameState o1, GameState o2) {
            return o2.getTime() - o1.getTime();
            // get results descending from largest to shortest
        }
    }
    public static class GameStateComparatorBySteps implements Comparator<GameState> {
        @Override
        public int compare(GameState o1, GameState o2) {
            return o2.getSteps() - o1.getSteps();
            // get results descending from largest to shortest
        }
    }

    public GameState getWorstResult() throws Exception{
        if(results_deadend.size()>0){
            return results_deadend.get(0);
        } else if (results_problematic.size()>0){
            List<GameState> results = new ArrayList();
            results.addAll(results_problematic);
            Collections.sort(results, new GameStateComparatorByTime());
            for(GameState result:results){
                if(result.getSteps()>MINIMUM_STEPS_REQUIRED_IN_SOLUTION_IN_ORDER_TO_SHOW_SOMETHIN_MOVING){
                    return result;
                }
            }
            return results.get(0);
        } else if (results_successful.size()>0){
            return results_successful.get(results_successful.size()-1);
        } else if (this.result_first_long!=null){
            return this.result_first_long;
        } else if (this.result_first_longer!=null){
            return this.result_first_longer;
        } else if (this.result_last_current!=null){
            return this.result_last_current;
        } else { 
            throw new Exception("There is no result to return!");
        }
    }

    public static ExecutionPlan toExecutionPlan(GameState gs) {
        List<GameState> states = new ArrayList();
        while (gs != null) {
            states.add(gs);
            gs = gs.getParent();
        }
        Collections.reverse(states);
        return new ExecutionPlan(states);
    }

    public GameStateSearch(GameState gs) {
        results_successful = new ArrayList<GameState>();
        results_problematic = new ArrayList<GameState>();
        results_deadend = new ArrayList<GameState>();
        this.start = gs;
        this.start.updateGameStateDescriptionLength();
        this.open = new LinkedList<GameState>();
        this.open.add(gs);
        this.closed_hashes = new HashSet();
        this.open_hashes = new HashSet();
    }

    public void search() {
        // TODO change search from BFS to ID-DFS?
        this.search_millis_end = System.currentTimeMillis()+this.search_millis;
        // TODO Q what is the right threshold for unfair schedulers?
        this.unfair_scheduler_threshold = this.start.getComponentState().getComponents().size();
        
        if(!this.verbose) GameState.verbose = false;
        
        GameState current=null;
        while (!this.open.isEmpty() && this.search_budget > this.search_cost) {
            search_cost++;
            if(search_cost%1000==0 && this.search_millis_end < System.currentTimeMillis()){
                if(this.verbose) System.out.println("Exhausted time");
                break;
            }
            if(search_cost%1000==0){
                if(this.verbose) System.out.println("open:"+this.open.size()+" cost:"+search_cost);
            }
            //current = this.open.pop();
            current = this.open.removeLast();
            if(this.verbose) System.out.println("Result of type "+current.result_type+" in time\t"+current.getTime()+" steps\t"+current.getSteps()+"\t"+current);
            closed_hashes.add(new GameStateDescriptionWrapper(current));

            // Evaluate the current GameState
            if (current.isStateComplete()) {
                current.result_type |= GameState.RESULT_SUCCESSFUL;
                if(current.getTime()<this.result_successful_depth){
                    this.results_successful.add(current);
                    this.result_successful_depth = current.getTime();
                } else {
                    this.nodes_ignored_result_successful_depth++;
                }
                if (this.break_on_first_successful) {
                    if(this.verbose) System.out.println("Break on first successful hit");
                    break;
                }
                // don't break here unless the goal conditions always encode the problem completely
            } 
            if (current.isStateDeadEnd()) {
                current.result_type |= GameState.RESULT_DEADEND;
                this.results_deadend.add(current);
                if (this.break_on_first_deadlock) {
                    if(this.verbose) System.out.println("Break on first deadlock hit");
                    break; // should be safe to always break here in production
                }
            }
            int problem_code = 0;
            if ((problem_code = current.isStateProblematic())!=0) {
                current.result_type |= GameState.RESULT_PROBLEMATIC;
                current.result_type |= problem_code;
                this.results_problematic.add(current);
                if (this.break_on_first_problematic) {
                    if(this.verbose) System.out.println("Break on first problematic hit");
                    break; // TODO Q what search breaks stay in production?
                }

            }
            if (current.isStatePartial()) {
                current.result_type |= GameState.RESULT_PARTIAL;
                // do NOT break here, it may lead to a successful later on
            }
            // Store a valid execution to return if there is no solution and no other problem
            if(this.result_first_long==null && current.getSteps()>MINIMUM_STEPS_REQUIRED_IN_SOLUTION_IN_ORDER_TO_SHOW_SOMETHIN_MOVING){
                this.result_first_long = current;
            }
            if(this.result_first_longer==null || this.result_first_longer.getSteps()<current.getSteps()){
                this.result_first_longer = current;
            }
            // Compute successors and filter successors
            for (GameState successor : current.getSuccessors()) {
                // Check for unfair schedules
                boolean is_unfair = false;
                if(successor.getMostUnfairScheduledUnit().consecutive_unscheduled>this.unfair_scheduler_threshold){
                    this.nodes_ignored_unfair_scheduler++;
                    is_unfair = true;
                }

                // Check for symmetries
                boolean symmetry_found = false;
                GameStateDescriptionWrapper successor_hash = new GameStateDescriptionWrapper(successor);
                if (closed_hashes.contains(successor_hash) || open_hashes.contains(successor_hash)) {
                    this.nodes_ignored_symmetry++;
                    symmetry_found = true;
                }
                // TODO not all symmetries should be discared AND/OR need more precise description...

                if ((!is_unfair)
                        && (!symmetry_found || !drop_symmetries_successors)
                        && !current.isStateComplete()) {
                    this.open.add(successor);
                    open_hashes.add(successor_hash);
                }
            }
        }
        this.result_last_current=current;
        this.search_space_exhausted = this.open.isEmpty();
    }

    public LinkedList<GameState> getOpen() {
        return open;
    }

    public Set<GameStateDescriptionWrapper> getClosed() {
        return closed_hashes;
    }

    public List<GameState> getResultsSuccessful() {
        return results_successful;
    }

    public List<GameState> getResultsDeadend() {
        return results_deadend;
    }
    
    public List<GameState> getResultsProblematic() {
        return results_problematic;
    }

    public void setSearchBudget(int budget) {
        search_budget = budget;
    }
    public void setSearchTime(int milliseconds){
        this.search_millis = milliseconds;
    }

    public int getSearchBudget() {
        return search_budget;
    }

    public boolean isSearchOverBudget() {
        return search_cost >= search_budget;
    }

    public boolean isSearchSpaceExhausted() {
        return search_space_exhausted;
    }
    
    public void getStatsIntoMap(Map<String,Double> m){
        List<Integer> lst;
        lst = new ArrayList();
        for(GameState gs:this.results_successful){
            lst.add(gs.getSteps());
        }
        double n_successful_avg = MathUtils.average(lst);
        lst = new ArrayList();
        for(GameState gs:this.results_problematic){
            lst.add(gs.getSteps());
        }
        double n_problematic_avg = MathUtils.average(lst);
        lst = new ArrayList();
        for(GameState gs:this.results_deadend){
            lst.add(gs.getSteps());
        }
        double n_deadend_avg = MathUtils.average(lst);
        Collections.sort(this.results_successful, new GameStateSearch.GameStateComparatorBySteps());
        Collections.sort(this.results_problematic, new GameStateSearch.GameStateComparatorBySteps());
        Collections.sort(this.results_deadend, new GameStateSearch.GameStateComparatorBySteps());
        
        int n_successful_f = this.results_successful.size() > 0 ? this.results_successful.get(this.results_successful.size()-1).getSteps() : 0;
        int n_problematic_f = this.results_problematic.size() > 0 ? this.results_problematic.get(this.results_problematic.size()-1).getSteps() : 0;
        int n_deadend_f = this.results_deadend.size() > 0 ? this.results_deadend.get(this.results_deadend.size()-1).getSteps() : 0;

        m.put("m_results_successful.size", (double)this.results_successful.size());
        m.put("m_results_problematic.size", (double)this.results_problematic.size());
        m.put("m_results_deadend.size", (double)this.results_deadend.size());
        m.put("m_n_successful_avg", n_successful_avg);
        m.put("m_n_problematic_avg", n_problematic_avg);
        m.put("m_n_deadend_avg", n_deadend_avg);
        m.put("m_n_successful_f", (double)n_successful_f);
        m.put("m_n_problematic_f", (double)n_problematic_f);
        m.put("m_n_deadend_f", (double)n_deadend_f);
        m.put("m_getClosed", (double)this.getClosed().size());
        m.put("m_getOpen", (double)this.getOpen().size());
        m.put("m_nodes_ignored_symmetry", (double)this.nodes_ignored_symmetry);
        m.put("m_nodes_ignored_unfair_scheduler", (double)this.nodes_ignored_unfair_scheduler);
        m.put("time_efficiency", (double)this.getAverageTime());
        m.put("time_efficiency_difference", (double)this.getTimeEfficiencyAverageDifference());
    }
    
    public void printStats(){
            Collections.sort(this.results_successful, new GameStateSearch.GameStateComparatorBySteps());
            Collections.sort(this.results_problematic, new GameStateSearch.GameStateComparatorBySteps());
            Collections.sort(this.results_deadend, new GameStateSearch.GameStateComparatorBySteps());
            
            int n_successful_f = this.results_successful.size() > 0 ? this.results_successful.get(this.results_successful.size()-1).getSteps() : 0;
            int n_problematic_f = this.results_problematic.size() > 0 ? this.results_problematic.get(this.results_problematic.size()-1).getSteps() : 0;
            int n_deadend_f = this.results_deadend.size() > 0 ? this.results_deadend.get(this.results_deadend.size()-1).getSteps() : 0;
            
            System.out.println("Search results with budget of:"+this.getSearchBudget());
            System.out.println("good solutions:"+this.results_successful.size());
            System.out.println("good solutions not considered:"+this.nodes_ignored_result_successful_depth);
            System.out.println("problematic solutions:"+this.results_problematic.size());
            System.out.println("dead end solutions:"+this.results_deadend.size());
            System.out.println("good solutions first steps:"+n_successful_f);
            System.out.println("problematic solutions first steps:"+n_problematic_f);
            System.out.println("dead end  solutions first steps:"+n_deadend_f);
            System.out.println("closed size:"+this.getClosed().size());
            System.out.println("open size:"+this.getOpen().size());
            System.out.println("ignored by symetries:"+this.nodes_ignored_symmetry);
            System.out.println("ignored unfair:"+this.nodes_ignored_unfair_scheduler);
    }

}
