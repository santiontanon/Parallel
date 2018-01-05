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

import game.component.Component;
import game.component.ComponentArrow;
import game.component.ComponentConditional;
import game.component.ComponentCustoms;
import game.component.ComponentDelivery;
import game.component.ComponentDiverter;
import game.component.ComponentExchange;
import game.component.ComponentIntersection;
import game.component.ComponentPickup;
import game.component.ComponentSemaphore;
import game.component.ComponentUnit;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import support.GameStateParser;
import static support.GameStateParser.extractUnitsFromComponents;
import valls.util.Pair;
import valls.util.MathUtils;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class GameState {

    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_MOVE = 1;
    public static final int STATE_EVENT = 2;

    public static final int RESULT_INCOMPLETE = 0;
    public static final int RESULT_DEADEND = 1;
    public static final int RESULT_PARTIAL = 2;
    public static final int RESULT_PROBLEMATIC = 4;
    public static final int RESULT_SUCCESSFUL = 8;
    public static final int RESULT_PROBLEMATIC_MISSED = 16;
    public static final int RESULT_PROBLEMATIC_STARVATION = 32;
    public static final int RESULT_PROBLEMATIC_INFINITE_LOOP = 64;
    public static final int RESULT_PROBLEMATIC_LOOPY_HASH = 128;
    public static final int RESULT_PROBLEMATIC_REGRESSION = 256;
    public static final int RESULT_PROBLEMATIC_WRONG_PATH = 512;
    
    private ComponentState cs;
    private UnitState us;
    private BoardState bs;
    public Map<Pair<Integer,Integer>,Integer> goals_delivery = new HashMap(); // TODO generalize this to other properties
    private int time_elapsed_total = 0;
    private int time_elapsed_this_step = 0;
    private Map<Integer, Integer> time_elapsed_per_unit_moved = new HashMap();
    public List<IntermediateUnitPosition> intermediate_unit_positions = new ArrayList();
    private int steps = 0;
    // Note, without state compression, time and steps should be the same

    private GameState parent = null;
    private boolean[] achieved_goals = null;

    public int state_type = STATE_UNKNOWN;
    public int result_type = RESULT_INCOMPLETE;
    // Note, result_type holds ancestry information so it can find a previously partial solution

    private static int description_length = 0;
    private static List<Pair<Class, Field>> description_extra_items;
    public static boolean enable_delays = false;
    public static boolean enable_state_compression = true;

    public static boolean verbose = true;

    public int getTime() {
        return time_elapsed_total;
    }

    public int getSteps() {
        return steps;
    }

    public GameState getParent() {
        return parent;
    }

    public void setTime(int time) {
        this.time_elapsed_total = time;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public boolean step(int deterministic) {
        // Note, this is currently only used by the GUI
        List<GameState> successors = this.getSuccessors();
        if (successors.size() > 0) {
            GameState successor;
            if (deterministic < 0) {
                Random rand = new Random();
                successor = successors.get(rand.nextInt(successors.size()));
            } else if (deterministic == 0) {
                successor = successors.get(0);
            } else {
                successor = successors.get(deterministic % successors.size());
            }
            this.cs = successor.getComponentState();
            this.us = successor.getUnitState();
            this.time_elapsed_total = successor.getTime();
            this.steps = successor.getSteps();
            return false;
        } else {
            return true; // Stop the simulation
        }

    }

    public String toString() {
        return String.format("Game Time: %d, Steps: %d, Hash: %s, Goals: %d (%s), Problems: %d",
                this.time_elapsed_total,
                this.steps,
                this.stateDescriptionHash() + " " + Arrays.toString(this.stateDescription()),
                this.evaluateState(),
                Arrays.toString(this.getAchievedGoals()),
                this.isStateProblematic()
        );
    }

    public boolean testGoal(GoalCondition gc) {
        int value = 0;

        if (gc.thread_id > 0) {
            // special case for the pairs if component/thread
            // TODO generalize to other things besides deliveries
            Pair<Integer,Integer> pair = new Pair(gc.thread_id,gc.component_id);
            if(this.goals_delivery.containsKey(pair)){
                value = this.goals_delivery.get(pair);
            } else {
                return false;
            }

        } else {
            // get a value from the components
            Component c;
            if (gc.component_class.equals("thread")) {
                c = this.us.getUnitById(gc.component_id);
            } else {
                c = this.cs.getComponentById(gc.component_id);
            }
            if (c != null) {
                // TODO Q account for goal values being NOT integers?
                try {
                    value = (Integer) gc.getField().get(c);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        // check condition
        if ("eq".equals(gc.condition)) {
            return value == gc.component_value;
        } else if ("lt".equals(gc.condition)) {
            return value < gc.component_value;
        } else if ("gt".equals(gc.condition)) {
            return value > gc.component_value;
        } else if ("ne".equals(gc.condition)) {
            return value != gc.component_value;
        }

        return false;
    }
    public String describeGoal(GoalCondition gc){
        return this.testGoal(gc) ? this.describeGoalSuccess(gc) : this.describeGoalFailure(gc);
    }
    public String describeGoalSuccess(GoalCondition gc){
        return "Not implemented.";
    }
    public String describeGoalFailure(GoalCondition gc){
        // code adapted from testGoal, maybe can be reused better
        int value = 0;
        if (gc.thread_id > 0) {
            // special case for the pairs if component/thread
            // TODO generalize to other things besides deliveries
            Pair<Integer,Integer> pair = new Pair(gc.thread_id,gc.component_id);
            if(this.goals_delivery.containsKey(pair)){
                value = this.goals_delivery.get(pair);
            } else {
                return "e00 There was no delivery by a specific arrow to a specific delivery point.";
            }

        } else {
            // get a value from the components
            Component c;
            if (gc.component_class.equals("thread")) {
                c = this.us.getUnitById(gc.component_id);
            } else {
                c = this.cs.getComponentById(gc.component_id);
            }
            if (c != null) {
                // TODO Q account for goal values being NOT integers?
                try {
                    value = (Integer) gc.getField().get(c);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (gc.component_class.equals("thread")) {
            if ("eq".equals(gc.condition) && !(value == gc.component_value)) {
                return "e12 An arrow didn't deliver the exact number of packages.";
            } else if ("lt".equals(gc.condition) && !(value < gc.component_value)) {
                return "e13 An arrow delivered more packages than it was supposed.";
            } else if ("gt".equals(gc.condition)&& !(value > gc.component_value)) {
                return "e14 An arrow did not deliver all the packages it was supposed.";
            } else if ("ne".equals(gc.condition)&& !(value != gc.component_value)) {
                return "e15 An arrow delivered the wrong number of packages.";
            }
        } else if (gc.component_class.equals("delivery")) {
            if ("eq".equals(gc.condition) && !(value == gc.component_value)) {
                return "e22 A delivery point didn't get the exact number of packages.";
            } else if ("lt".equals(gc.condition) && !(value < gc.component_value)) {
                return "e23 A delivery point got more packages than it was supposed.";
            } else if ("gt".equals(gc.condition)&& !(value > gc.component_value)) {
                return "e24 A delivery point did not get all the packages it was supposed.";
            } else if ("ne".equals(gc.condition)&& !(value != gc.component_value)) {
                return "e25 A delivery point got the wrong number of packages.";
            }
            
        } else {
            if(gc.goal_type==GoalCondition.GOAL_REQUIRED)
                return "e36 Another required goal was not met.";
            else
                return "e37 Another desired goal was not met.";
        }
        return "e48 Couldn't evaluate the goal.";
    }


    public int evaluateState() {
        // Note, this is called lazily, can compute other features of interest here
        int completed = 0;
        this.achieved_goals = new boolean[this.bs.goal_struct.size()];
        for (int i = 0; i < this.bs.goal_struct.size(); i++) {
            this.achieved_goals[i] = this.testGoal(this.bs.goal_struct.get(i));
            if (this.achieved_goals[i]) {
                completed++;
            }
        }
        return completed;
    }
    public boolean[] getAchievedGoals(){
        if (this.achieved_goals == null) {
            this.evaluateState();
        }
        return this.achieved_goals;
    }
    public int getAchievedGoalCount(){
        int c = 0;
        for(boolean i:this.getAchievedGoals()){
            if(i) c++;
        }
        return c;
    }

    public boolean isStatePartial() {
        // TODO Return true only if no ancestor was not already considered
        // add bitmask with reasons that have been found in ancestry and propagate to successors?
        if (this.achieved_goals == null) {
            this.evaluateState();
        }

        for (int i = 0; i < this.achieved_goals.length; i++) {
            if (this.bs.goal_struct.get(i).goal_type == GoalCondition.GOAL_REQUIRED && this.achieved_goals[i] == false) {
                return false;
            }
        }
        return true;

    }

    public boolean isStateComplete() {
        // Return true only if everything is accomplished (required and desired goals) as this prevents recursion
        if (this.achieved_goals == null) {
            this.evaluateState();
        }
        for (int i = 0; i < this.achieved_goals.length; i++) {
            if (this.achieved_goals[i] == false) {
                return false;
            }
        }
        return true;
    }

    public int isStateProblematic() {
        // Note, this is mostly undefined for now, can be useful if there is an additional list of assertions that should never happen or similars properties are detected.        
        for (Component c : this.getComponentState().getComponentsByType(ComponentDelivery.class)) {
            ComponentDelivery cd = (ComponentDelivery) c;
            if (cd.missed > 0) {
                return GameState.RESULT_PROBLEMATIC_MISSED;
            }
        }
        int starvation_threshold = this.getComponentState().getComponents().size() * this.getUnitState().getUnits().size();
        // TODO Q what is the right threshold for starvation?
        for (ComponentUnit cu : this.getUnitState().getUnits()) {
            //System.out.println("Starve "+cu.id+" "+cu.consecutive_blocked + " "+ cu.consecutive_unscheduled);
            if (cu.consecutive_blocked > starvation_threshold) {
                return GameState.RESULT_PROBLEMATIC_STARVATION;
            }
        }
        if((this.result_type & GameState.RESULT_PROBLEMATIC_INFINITE_LOOP) !=0){
            return GameState.RESULT_PROBLEMATIC_INFINITE_LOOP;
        }
        if((this.result_type & GameState.RESULT_PROBLEMATIC_LOOPY_HASH) !=0){
            return GameState.RESULT_PROBLEMATIC_LOOPY_HASH;
        }
        // Check for RESULT_PROBLEMATIC_REGRESSION
        if(this.parent!=null){
            for(int i=0;i<this.getAchievedGoals().length;i++){
                if(this.parent.getAchievedGoals()[i] && !this.getAchievedGoals()[i]){
                    return GameState.RESULT_PROBLEMATIC_REGRESSION;
                }
            }
        }

        // When there are no problems, return 0 as problem code
        return 0;
    }

    public boolean isStateDeadEnd() {
        // Return true only if everything is locked as this prevents recursion
        //if(this.achieved_goals==null) this.evaluateState();
        for (int i = 0; i < this.us.getUnits().size(); i++) {
            if (this.canMoveUnit(us.getUnit(i))) {
                return false;
            }
            // Check if the unit is delivering
            if (this.canUpdateComponent(us.getUnit(i))) {
                return false;
            }
        }
        return true;
    }

    private GameState newSuccessor(int successor_type) {
        Map<Pair<Integer, Integer>, Integer> new_goal_delivery = new HashMap();
        for(Map.Entry<Pair<Integer, Integer>, Integer> entry : this.goals_delivery.entrySet()){
            new_goal_delivery.put(entry.getKey(), entry.getValue());
        }
        return new GameState(this, this.bs, this.cs.clone(), this.us.clone(), this.steps + 1, this.time_elapsed_total, successor_type, this.result_type, new_goal_delivery);
    }

    public ComponentUnit getMostUnfairScheduledUnit() {
        int worst_count = Integer.MIN_VALUE;
        ComponentUnit worst_unit = null;
        for (ComponentUnit cu : this.us.getUnits()) {
            if (cu.consecutive_unscheduled > worst_count) {
                worst_unit = cu;
                worst_count = cu.consecutive_unscheduled;
            }
        }
        return worst_unit;
    }

    public List<GameState> getSuccessors() {
        // TODO consider successors where combinations of multiple threads advance
        // TODO consider scheduling policy where a single "malignant" thread is shifted around
        List<GameState> successors = new ArrayList<GameState>();
        GameState successor = null;
        for (ComponentUnit u : us.getUnits()) {
            u.consecutive_unscheduled++;
        }
        // TODO for the DFS, would it be better for this to be at the bottom? I'll leave code here for debugging
        /*
        // Move all units at once
        if (this.us.getUnits().size() > 1) {
            successor = this.newSuccessor(GameState.STATE_MOVE);
            successors.add(successor);
            for (int i = 0; i < this.us.getUnits().size(); i++) {
                ComponentUnit cu = us.getUnit(i);
                successor.getUnitState().getUnit(i).consecutive_unscheduled = 0;
                if (this.canMoveUnit(cu)) {
                    successor.getUnitState().getUnit(i).consecutive_blocked = 0;
                    successor.moveUnit(i);
                } else if (this.canUpdateComponent(cu)) {
                    successor.getUnitState().getUnit(i).consecutive_blocked = 0;
                    successor.updateComponentUnit(i);
                } else {
                    successor.getUnitState().getUnit(i).consecutive_blocked++;
                    // TODO this may be sufficient to identify starvation but may need to be updated also in all successors when moving a single unit for all "other" units
                }
            }
        } // END Move all units at once
        return successors;
        */
        // Move or update a single unit
        for (int i = 0; i < this.us.getUnits().size(); i++) {
            ComponentUnit cu = us.getUnit(i);
            if (this.canMoveUnit(cu)) {
                successor = this.newSuccessor(GameState.STATE_MOVE);
                successor.getUnitState().getUnit(i).consecutive_blocked = 0;
                successor.getUnitState().getUnit(i).consecutive_unscheduled = 0;
                successor.moveUnit(i);
                for (int j = 0; j < this.us.getUnits().size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    successor.getUnitState().getUnit(j).consecutive_unscheduled++;
                }
                successors.add(successor);
            } else if (this.canUpdateComponent(cu)) {
                successor = this.newSuccessor(GameState.STATE_EVENT);
                successor.getUnitState().getUnit(i).consecutive_blocked = 0;
                successor.getUnitState().getUnit(i).consecutive_unscheduled = 0;
                successor.updateComponentUnit(i);
                for (int j = 0; j < this.us.getUnits().size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    successor.getUnitState().getUnit(j).consecutive_unscheduled++;
                }
                successors.add(successor);
            }
        }
        // Update all components
        successor = this.newSuccessor(GameState.STATE_EVENT);
        successors.add(successor);
        for (int i = 0; i < this.cs.getComponents().size(); i++) {
            Component c = cs.getComponent(i);
            if (this.canUpdateComponent(c)) {
                successor.updateComponent(i);
            }
        }
        // See comment above for this block of code.
        // Move all units at once
        if (this.us.getUnits().size() > 1) {
            successor = this.newSuccessor(GameState.STATE_MOVE);
            successors.add(successor);
            for (int i = 0; i < this.us.getUnits().size(); i++) {
                ComponentUnit cu = us.getUnit(i);
                successor.getUnitState().getUnit(i).consecutive_unscheduled = 0;
                if (this.canMoveUnit(cu)) {
                    successor.getUnitState().getUnit(i).consecutive_blocked = 0;
                    successor.moveUnit(i);
                } else if (this.canUpdateComponent(cu)) {
                    successor.getUnitState().getUnit(i).consecutive_blocked = 0;
                    successor.updateComponentUnit(i);
                } else {
                    successor.getUnitState().getUnit(i).consecutive_blocked++;
                    // TODO this may be sufficient to identify starvation but may need to be updated also in all successors when moving a single unit for all "other" units
                }
            }
        } // END Move all units at once
        return successors;
    }

    public void updateComponentUnit(int component_index) {
        Component component = this.us.getUnit(component_index);
        this.updateComponent(component);
    }

    public void updateComponent(int component_index) {
        Component component = this.cs.getComponent(component_index);
        this.updateComponent(component);
    }

    public void updateComponent(Component component) {
        boolean allowed_to_continue = true;
        int time = 0;
        while (this.canUpdateComponent(component) && allowed_to_continue) {
            time++;
            component.updateClockTick(this);
            allowed_to_continue &= GameState.enable_state_compression;
            // Controls state compression for unit movement
        }
        this.updateTimeElapsed(time, component.id);
    }

    public void moveUnit(int unit_index) {
        ComponentUnit unit = this.us.getUnit(unit_index);
        this.moveUnit(unit);
    }

    public void moveUnit(ComponentUnit unit) {
        // TODO since the semaphores now stop before entering, units should not move before entering a semaphore in a single movement, will lose some branches
        boolean allowed_to_continue = true;
        int time = 0;
        Set<Integer> unit_hashes = new HashSet();
        while (this.canMoveUnit(unit) && allowed_to_continue) {
            time++;
            allowed_to_continue = this.moveUnitRepeat(unit);
            // Controls state compression for unit movement
            allowed_to_continue &= GameState.enable_state_compression;
            if (unit.tile_current.isWaypoint() || !allowed_to_continue) {
                // TODO instead of checking for waypoints, check for changes in direction
                // TODO move this to the export section to save on memory
                this.intermediate_unit_positions.add(new IntermediateUnitPosition(unit.id, unit.tile_current.id, time));
            }
            // TODO detect cycles here, find a better way to handle this
            Integer unit_hash = this.stateUnitDescriptionHash(unit);
            if(unit_hashes.contains(unit_hash)){
                allowed_to_continue = false;
                // This is broken in PCG levels where the ending of one path is a loop, when we properly support track endings maybe we can enable this again.
                // TODO have a check that looks for ALL the threads in a LOOPY state, then break
                // this.result_type |= GameState.RESULT_PROBLEMATIC_LOOPY_HASH;                
            }
            unit_hashes.add(unit_hash);
            if(time>this.bs.getWidth() * this.bs.getHeight()){
                allowed_to_continue = false;
                this.result_type |= GameState.RESULT_PROBLEMATIC_INFINITE_LOOP;
            }
        }
        this.updateTimeElapsed(time, unit.id);
    }

    private void updateTimeElapsed(int time_elapsed, int unit_id) {
        if (time_elapsed > this.time_elapsed_this_step) {
            this.time_elapsed_total -= this.time_elapsed_this_step;
            this.time_elapsed_this_step = time_elapsed;
            this.time_elapsed_total += this.time_elapsed_this_step;
        }
        this.time_elapsed_per_unit_moved.put(unit_id, time_elapsed_this_step);
    }

    public float getUnitMovementSpeed(int unit_id) {
        if (this.time_elapsed_per_unit_moved.containsKey(unit_id)) {
            return (float) this.time_elapsed_per_unit_moved.get(unit_id) / (float) this.time_elapsed_this_step;
        } else {
            return 0.0f;
        }
    }

    public boolean moveUnitRepeat(ComponentUnit unit) {
        boolean allowed_to_continue = true;
        // Leave a tile
        for (int component_index : unit.tile_current.component_index) {
            allowed_to_continue &= this.cs.getComponent(component_index).updateUnitLeave(unit, this);
        }
        // Check where to go next
        Tile tile_next = null;
        int forced_direction = -1;
        for (int component_index : unit.tile_next.component_index) {
            Component c = this.cs.getComponent(component_index);
            forced_direction = c.forcesDirection(unit, this);
        }
        if (forced_direction >= 0) {
            tile_next = this.bs.getTile(unit.tile_next.x + Component.DIRECTION_OFFSET_DICT_X[forced_direction], unit.tile_next.y + Component.DIRECTION_OFFSET_DICT_Y[forced_direction]);
        } else {
            int dx = unit.tile_next.x - unit.tile_current.x;
            int dy = unit.tile_next.y - unit.tile_current.y;
            Tile tile_alternate = null;
            for (Tile tile : unit.tile_next.neighbors) {
                if (tile == unit.tile_current) {
                    // Avoid going backwards
                } else if (tile.isPassable(unit.color)) {
                    if (unit.tile_next.traveled_to.contains(tile)){
                        tile_next = tile;
                    } else {
                        tile_alternate = tile;
                    }
                }
            }
            Tile tile_preferred = this.bs.getTile(unit.tile_next.x + dx, unit.tile_next.y + dy);
            if(tile_preferred!=null && (tile_next == null || unit.tile_next.overpass)){
                // The overpass case is necessary for level 16 (the smokers problem?)               
                if(tile_preferred.isPassable(unit.color)){
                    tile_next=tile_preferred;
                } else if (tile_alternate!=null){
                    tile_next = tile_alternate;
                }
            }
        }
        if (tile_next == null) {
            this.result_type |= GameState.RESULT_PROBLEMATIC_WRONG_PATH;
            if(GameState.verbose) System.err.println("The unit doesn't have any tile to go");
        } else if (tile_next != null && !tile_next.isPassable(unit.color)) {
            this.result_type |= GameState.RESULT_PROBLEMATIC_WRONG_PATH;
            if(GameState.verbose) System.err.println("The unit is going to an unpassable tile");
        }
        if (unit.tile_current.id == 256) {
            int x = 1;
        }
        // unit.tile_current.traveled_to.add(unit.tile_next);
        unit.tile_current = unit.tile_next;
        unit.tile_next = tile_next;
        // TODO save memory and lose all x,y references, use tile_id instead until the export
        unit.x = unit.tile_current.x;
        unit.y = unit.tile_current.y;
        // Enter next tile
        for (int component_index : unit.tile_current.component_index) {
            allowed_to_continue &= this.cs.getComponent(component_index).updateUnitEnter(unit, this);
        }

        
        return allowed_to_continue && (tile_next != null);
    }

    public boolean canUpdateComponent(Component component) {
        return component.canUpdateClockTick(this);
    }

    public boolean canMoveUnit(ComponentUnit unit) {
        // TODO Q can other units enter an exchange point that is already busy? what happens with packages? are packages missed?
        return unit.delay == 0 && this.canUnitLeave(unit) && this.canUnitEnter(unit);
    }

    public boolean canUnitLeave(ComponentUnit unit) {
        assert unit.tile_current != null;
        assert unit.tile_current.component_index != null;
        if (unit.tile_current.component_index.size() == 0) {
            return true;
        } else {
            boolean can_leave = true;
            for (int component_index : unit.tile_current.component_index) {
                Component c = this.cs.getComponent(component_index);
                can_leave &= c.canUnitLeave(unit, this);
            }
            return can_leave;
        }
    }

    public boolean canUnitEnter(ComponentUnit unit) {
        // Units stop in the tile before a semaphore according to the GDD 20160429        
        if (unit.tile_next == null) {
            return false;
        }
        if (unit.tile_next.component_index.size() == 0) {
            return true;
        } else {
            boolean can_enter = true;
            for (int component_index : unit.tile_next.component_index) {
                Component c = this.cs.getComponent(component_index);
                can_enter &= c.canUnitEnter(unit, this);
            }
            return can_enter;
        }
    }

    public void updateGameStateDescriptionLength() {
        // Needs to be updated between calls of different levels in the GameStateSearch, otherwise could be static
        GameState.description_length = this.us.getUnits().size()
                + this.cs.getComponentsByType(ComponentPickup.class).size() // what is available to pickup
                + this.cs.getComponentsByType(ComponentSemaphore.class).size()
                + this.cs.getComponentsByType(ComponentConditional.class).size();

        GameState.description_extra_items = new ArrayList();
        for (GoalCondition gc : this.bs.goal_struct) {
            Pair gcp = new Pair(gc.getComponentClass(), gc.getField());
            if (!GameState.description_extra_items.contains(gcp)) {
                GameState.description_length += this.cs.getComponentsByType(gc.getComponentClass()).size();
                GameState.description_extra_items.add(gcp);
            }
        }
    }

    private int getGameStateDescriptionLength() {
        if (GameState.description_length == 0) {
            this.updateGameStateDescriptionLength();
        }
        return GameState.description_length;
    }

    private int stateUnitPayloadDescriptionHash(ComponentUnit c) {
        if (c.payload.length == 0) {
            return 0;
        } else {
            int[] payload_description = new int[c.payload.length];
            for (int i = 0; i < c.payload.length; i++) {
                int unit_package_id = c.payload[i];
                ComponentPickup cp = (ComponentPickup) this.getComponentState().getComponentById(unit_package_id);
                payload_description[i] = cp.color;
            }
            Arrays.sort(payload_description);
            return this.stateDescriptionHash(payload_description);
        }
    }

    private int stateUnitDescriptionHash(ComponentUnit c) {
        int[] description = new int[5];
        int offset = 0;
        description[offset++] = c.x;
        description[offset++] = c.y;
        description[offset++] = c.color;
        description[offset++] = c.delay;
        description[offset++] = this.stateUnitPayloadDescriptionHash(c);
        return this.stateDescriptionHash(description);

    }

    public int[] stateDescription() {
        int length = this.getGameStateDescriptionLength();
        int[] description = new int[length];
        int offset = 0;
        int[] unit_hashes = new int[this.us.getUnits().size()];
        for (int i = 0; i < this.us.getUnits().size(); i++) {
            unit_hashes[i] = this.stateUnitDescriptionHash(this.us.getUnit(i));
            //System.out.println("unit "+this.stateUnitDescriptionHash(this.us.getUnit(i)));
        }
        java.util.Arrays.sort(unit_hashes);
        for (int i = 0; i < this.us.getUnits().size(); i++) {
            description[offset++] = unit_hashes[i];
        }
        for (Component c : this.cs.getComponentsByType(ComponentPickup.class)) {
            description[offset++] = ((ComponentPickup) c).available;
        }

        for (Component c : this.cs.getComponentsByType(ComponentSemaphore.class)) {
            description[offset++] = ((ComponentSemaphore) c).value;
        }
        for (Component c : this.cs.getComponentsByType(ComponentConditional.class)) {
            description[offset++] = ((ComponentConditional) c).current;
        }
        /*for(Component c: this.cs.getComponentsByType(ComponentDelivery.class)){
         description[offset++]=((ComponentDelivery)c).delivered;
         }*/
        for (Pair<Class, Field> p : GameState.description_extra_items) {
            for (Component c : this.cs.getComponentsByType(p.m_a)) {
                // TODO restrict the items in the description to ints
                try {
                    int value = p.m_b.getInt(c);
                    description[offset++] = value>5?5:value;
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        return description;
    }

    public int stateDescriptionHash() {
        int[] description = this.stateDescription();
        return this.stateDescriptionHash(description);
    }

    public int stateDescriptionHash(int[] description) {
        int hash = 0;
        for (int i = 0; i < description.length; i++) {
            hash += description[i] * MathUtils.ipow(31, description.length - 1 - i);
        }
        return hash;
    }

    public boolean stateDescriptionEquivalentTo(GameState other) {
        int[] description = this.stateDescription();
        int[] description_other = other.stateDescription();
        for (int i = 0; i < description.length; i++) {
            if (description[i] != description_other[i]) {
                return false;
            }
        }
        return true;
    }

    public GameState(BoardState board, ComponentState components, UnitState units) {
        this.bs = board;
        this.cs = components;
        this.us = units;
    }

    public GameState(GameState parent, BoardState board, ComponentState components, UnitState units, int steps, int time, int state_type, int result_type, Map<Pair<Integer,Integer>,Integer> goals_delivery) {
        this.bs = board;
        this.cs = components;
        this.us = units;
        this.parent = parent;
        this.steps = steps;
        this.time_elapsed_total = time;
        this.state_type = state_type;
        this.result_type = result_type;
        this.goals_delivery = goals_delivery;
    }

    public void expand() {
        this.expand(3, 3);
    }

    public void expand(int ratio_x, int ratio_y) {
        this.bs = this.bs.expand(ratio_x, ratio_y);
        for (Component c : this.cs.getComponents()) {
            c.x = c.x * ratio_x + ((int) ratio_x / 2);
            c.y = c.y * ratio_y + ((int) ratio_x / 2);
        }
        this.us = GameStateParser.extractUnitsFromComponents(this.cs);
        this.init();
    }

    public void init() {
        this.initComponents();
        this.bs.initTileNeighbors();
        this.initIntersections();
        this.initUnitsNextTile();
        this.updateGameStateDescriptionLength();
    }

    public void initComponents() {
        for (int i = 0; i < this.getComponentState().getComponents().size(); i++) {
            Component component = this.getComponentState().getComponents().get(i);
            // Components to tiles
            Tile tile = this.bs.getTile(component.x, component.y);
            if (tile == null || !tile.isPassable()) {
                System.err.println("A component can't be placed on an unpassable tile: " + component.toString());
            } else {
                tile.component_index.add(i);
            }
            // Conditionals defaults for exporting
            if (ComponentConditional.class.isInstance(component)) {
                ComponentConditional cd = (ComponentConditional) component;
                cd.current_default = cd.current;
            }
        }
    }

    private void initIntersections() {
        int generated_components = 0;
        for (Tile tile : this.bs.getTiles()) {
            if (tile.traveled_to.size() == 3) {
                boolean something_disambiguates_direction = false;
                if (tile.component_index.size() > 0) {
                    for (int idx : tile.component_index) {
                        Component c = this.cs.getComponent(idx);
                        if (ComponentArrow.class.isInstance(c)
                                || ComponentConditional.class.isInstance(c)
                                || ComponentDiverter.class.isInstance(c)
                                || ComponentIntersection.class.isInstance(c)) {
                            // There is a component that diverts, that's good
                            something_disambiguates_direction = true;
                            break;
                        }
                    }
                    if (!something_disambiguates_direction && (
                            tile.traveled_to.size() == 3 || // this uses the older "flood-fill" approach for directions
                            tile.direction == 0             // this overrides the older approach with the directions section
                            ) ) {
                        System.out.println("There is an ambiguous T intersection at " + tile.x + "," + tile.y + " with components that don't disambiguate direction.");
                    }
                }
                int ca_direction = -1;
                if (!something_disambiguates_direction) {
                    int outbound_colors = -1;
                    for (Tile neighbor : tile.traveled_to) {
                        if (!neighbor.traveled_to.contains(tile)) {
                            // This neighbor is outbound
                            int direction = -1;
                            //{"West","South","East","North"}
                            if (neighbor.x > tile.x) {
                                direction = Component.EAST;
                            } else if (neighbor.y > tile.y) {
                                direction = Component.SOUTH;
                            } else if (neighbor.x < tile.x) {
                                direction = Component.WEST;
                            } else if (neighbor.y < tile.y) {
                                direction = Component.NORTH;
                            }
                            if (direction != -1 && ca_direction == -1) {
                                ca_direction = direction;
                            } else {
                                ca_direction = -1;
                            }
                            // Check for colors and assume that if there are colors, these will disambiguate
                            if (outbound_colors == -1) {
                                outbound_colors = neighbor.getColorsBitmask();
                            } else if (outbound_colors != neighbor.getColorsBitmask()) {
                                something_disambiguates_direction = true;
                            }
                        }
                    }
                }
                if (!something_disambiguates_direction) {
                    generated_components++;
                    ComponentArrow ca = new ComponentArrow(tile.x, tile.y, generated_components, 0, 'S', true);
                    ca.direction = ca_direction;
                    if (ca.direction == -1) {
                        System.out.println("Unable to compute direction for ambiguous T intersection at " + tile.x + "," + tile.y + ".");
                    } else {
                        int idx = this.cs.addComponent(ca);
                        tile.component_index.add(idx);
                    }
                }
            }
        }
    }

    private void initUnitsNextTile() {
        for (ComponentUnit unit : this.us.getUnits()) {
            Tile tile = this.bs.getTile(unit.x, unit.y);
            unit.tile_current = tile;
            Tile tile_next = this.bs.getTile(unit.x + Component.DIRECTION_OFFSET_DICT_X[unit.initial_direction], unit.y + Component.DIRECTION_OFFSET_DICT_Y[unit.initial_direction]);
            if (tile == null | tile_next == null) {
                System.out.println("The unit's initial tile or direction doesn't point to a valid tile: " + unit.toString());
            } else if (!tile.neighbors.contains(tile_next)) {
                System.out.println("The unit's initial direction doesn't point to a valid tile: " + unit.toString());
            } else {
                unit.tile_next = tile_next;
            }
        }
    }

    public void setBoardState(BoardState board) {
        this.bs = board;
    }

    public void setComponentState(ComponentState components) {
        this.cs = components;
    }

    public void setUnitState(UnitState units) {
        this.us = units;
    }

    public BoardState getBoardState() {
        return bs;
    }

    public ComponentState getComponentState() {
        return cs;
    }
    public List<Component> getComponentsAt(Tile tile){
        List<Component> components = new ArrayList();
        for(int i:tile.component_index){
            components.add(this.cs.getComponent(i));
        }
        return components;
    }
    public List<Component> getComponentsAt(int x, int y){
        return this.getComponentsAt(this.bs.getTile(x, y));
    }

    public UnitState getUnitState() {
        return us;
    }

    public GameState clone() {
        BoardState bs_ = this.bs.clone();
        bs_.copyPropertiesFrom(this.bs);
        GameState gs = new GameState(
                bs_,
                cs.clone(),
                us.clone());
        return gs;
    }

    public int updateResultType() {
        // TODO consolidate the result type computation outside of the GSS

        if (this.isStateComplete()) {
            this.result_type |= GameState.RESULT_SUCCESSFUL;
        }
        if (this.isStateDeadEnd()) {
            this.result_type |= GameState.RESULT_DEADEND;
        }
        int problem_code = 0;
        if ((problem_code = this.isStateProblematic()) != 0) {
            this.result_type |= GameState.RESULT_PROBLEMATIC;
            this.result_type |= problem_code;
        }
        if (this.isStatePartial()) {
            this.result_type |= GameState.RESULT_PARTIAL;
        }
        return this.result_type;
    }
}
