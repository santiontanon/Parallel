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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import game.BoardState;
import game.component.Component;
import game.component.ComponentPickup;
import game.ComponentState;
import game.component.ComponentUnit;
import game.execution.ExecutionPlan;
import game.GameState;
import game.GameStateSearch;
import game.GoalCondition;
import game.IntermediateUnitPosition;
import game.Tile;
import game.component.ComponentArrow;
import game.component.ComponentComment;
import game.component.ComponentConditional;
import game.component.ComponentDelivery;
import game.component.ComponentExchange;
import game.playermodel.PlayerData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static valls.util.ListToArrayUtility.toIntegerList;
import static valls.util.ListToArrayUtility.compareArrays;
import valls.util.Pair;
import valls.util.SortablePair;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class GameStateExporter {

    public static char color_base = ' ';

    public static String export(GameStateSearch gss, PlayerData pd, String desiredID) throws Exception {
        return GameStateExporter.export(gss, gss.getStart(), GameStateSearch.toExecutionPlan(gss.getWorstResult()), null, desiredID, false);
    }

    public static String export(GameState gs, ExecutionPlan ep, PlayerData pd, String desiredID) {
        return GameStateExporter.export(null, gs, ep, pd, desiredID, false);
    }

    public static String export(GameStateSearch gss, GameState gs, ExecutionPlan ep, PlayerData pd, String desiredID, boolean verbose) {
        StringBuilder sb = new StringBuilder();
        BoardState board = gs.getBoardState();
        sb.append("METADATA\n");
        sb.append("level_id\t");
        sb.append(board.level_id);
        sb.append('\n');
        if (desiredID != null) {
            sb.append("pcg_id\t");
            sb.append(desiredID);
            sb.append('\n');
        }
        sb.append("level_title\t");
        sb.append(board.level_title);
        sb.append('\n');
        sb.append("goal_string\t");
        sb.append(board.goal_string);
        sb.append('\n');
        sb.append("goal_struct\t");
        sb.append(exportGoalsToJson(board.goal_struct));
        sb.append('\n');
        sb.append("player_palette\t");
        board.player_palette.put("colors", board.max_colors);
        sb.append(exportPaletteToJson(board.player_palette));
        sb.append('\n');
        sb.append("time_pickup_min\t");
        sb.append(board.time_pickup_min);
        sb.append('\n');
        sb.append("time_delivery_min\t");
        sb.append(board.time_delivery_min);
        sb.append('\n');
        sb.append("time_exchange_min\t");
        sb.append(board.time_exchange_min);
        sb.append('\n');
        sb.append("time_pickup_max\t");
        sb.append(board.time_pickup_max);
        sb.append('\n');
        sb.append("time_delivery_max\t");
        sb.append(board.time_delivery_max);
        sb.append('\n');
        sb.append("time_exchange_max\t");
        sb.append(board.time_exchange_max);
        sb.append('\n');

        sb.append("board_width\t");
        sb.append(board.getWidth());
        sb.append('\n');
        sb.append("board_height\t");
        sb.append(board.getHeight());
        sb.append('\n');
        sb.append("time_efficiency\t");
        sb.append(board.time_efficiency);
        sb.append('\n');
        sb.append("\n");
        sb.append("LAYOUT\n");
        sb.append(exportBoardToTextLayout(board));
        sb.append("\n");
        if (verbose) {
            sb.append("LAYOUT_VERBOSE\n");
            sb.append(exportBoardToTextLayoutVerbose(board));
            sb.append("\n");
        }
        sb.append("COLORS\n");
        sb.append(exportBoardToTextColors(board));
        sb.append("\n");
        sb.append("DIRECTIONS\n");
        sb.append(exportBoardToTextDirections(board));
        sb.append("\n");
        if (gs.skills!=null) {
            sb.append("SKILLS\n");
            for(String s:gs.skills) {
                sb.append(s + "\n");
            }
            sb.append("\n");
        }

        ComponentState components = gs.getComponentState();
        sb.append(exportComponentsToTextRepresentation(components));
        sb.append(exportExecutionToTextRepresentation(gss, board, ep));
        sb.append(exportPlayerDataToTextRepresentation(pd));
        return sb.toString();

    }

    public static String export(GameState gs) {
        return GameStateExporter.export(null, gs, new ExecutionPlan(), new PlayerData(), null, false);
    }

    public static String export(GameState gs, String desiredLEvelID) {
        return GameStateExporter.export(null, gs, new ExecutionPlan(), new PlayerData(), desiredLEvelID, false);
    }

    public static String export(GameState gs, boolean verbose) {
        return GameStateExporter.export(null, gs, new ExecutionPlan(), new PlayerData(), null, verbose);
    }

    private static String exportBoardTtoTextHelper(BoardState board, boolean colors, boolean verbose) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                if (verbose) {
                    if (board.getTile(x, y).isPassable()) {
                        sb.append('@');
                    } else {
                        sb.append('-');
                    }

                } else {
                    int bitmask = board.getTile(x, y).getTileBitmask();
                    if (bitmask == 0) {
                        sb.append('-');
                    } else if (colors) {
                        bitmask = board.getTile(x, y).getColorsBitmask();
                        sb.append((char) (color_base + bitmask));
                    } else {
                        sb.append((char) ('@' + bitmask));
                    }
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static String exportBoardToTextLayoutVerbose(BoardState board) {
        return exportBoardTtoTextHelper(board, false, true);
    }

    public static String exportBoardToTextLayout(BoardState board) {
        return exportBoardTtoTextHelper(board, false, false);
    }

    public static String exportBoardToTextColors(BoardState board) {
        return exportBoardTtoTextHelper(board, true, false);
    }

    public static String exportBoardToTextDirections(BoardState board) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Tile tile = board.getTile(x, y);
                if(tile.direction>-1){
                    sb.append(Tile.DIRECTIONS[tile.direction]);
                } else {
                    if (tile.traveled_to.size() == 0) {
                        sb.append(' ');
                    } else if (tile.traveled_to.size() == 1) {
                        Tile traveled_to = tile.traveled_to.iterator().next();
                        if(traveled_to!=null){
                            if (traveled_to.x > tile.x) {
                                sb.append('>');
                            } else if (traveled_to.x < tile.x) {
                                sb.append('<');
                            } else if (traveled_to.y < tile.y) {
                                sb.append('A');
                            } else if (traveled_to.y > tile.y) {
                                sb.append('V');
                            } else {
                                sb.append(' ');
                            }
                        }
                    } else {
                        sb.append('X');
                    }
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static String exportComponentsToTextRepresentation(ComponentState components) {
        if(components==null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("COMPONENTS\n");
        for (Component c : components.getComponents()) {
            if(ComponentArrow.class.isInstance(c)){
                continue;
            }
            sb.append(c.id);
            sb.append('\t');
            String representation = "?";
            try {
                representation = (String) c.getClass().getField("representation").get(c);
            } catch (Exception ex) {
                // Shouldn't happen
            }
            sb.append(representation);
            sb.append('\t');
            sb.append(c.x);
            sb.append('\t');
            sb.append(c.y);
            sb.append('\t');
            sb.append(c.owner);
            sb.append('\t');
            sb.append(c.locked ? 'L' : 'E');
            sb.append('\t');
            sb.append(exportComponentPropertiesToJson(c));
            sb.append('\n');

        }
        sb.append('\n');
        return sb.toString();
    }

    public static String exportComponentPropertiesToJson(Component c) {
        Map<String, Object> export_map = new LinkedHashMap();
        for (String property : GameStateParser.properties_valid) {
            try {
                export_map.put(property, c.getClass().getField(property).get(c));
            } catch (NoSuchFieldException ex) {
                // It's fine, some fields won't be available for every components
            } catch (Exception ex) {
                System.err.println("Error exporting component properties " + ex);
            }
        }
        // Handle special properties that use ENUMs instead of ints
        final String pPT = "type";
        if (export_map.containsKey(pPT)) {
            export_map.put(pPT, ComponentPickup.PICKUP_TYPES[(Integer) export_map.get(pPT)]);
        }
        final String pID = "initial_direction";
        if (export_map.containsKey(pID)) {
            export_map.put(pID, Component.DIRECTIONS[(Integer) export_map.get(pID)]);
        }
        final String pDC = "direction_condition";
        if (export_map.containsKey(pDC)) {
            export_map.put(pDC, Component.DIRECTIONS[(Integer) export_map.get(pDC)]);
        }
        final String pDD = "direction_default";
        if (export_map.containsKey(pDD)) {
            export_map.put(pDD, Component.DIRECTIONS[(Integer) export_map.get(pDD)]);
        }
        final String pAD = "direction";
        if (export_map.containsKey(pAD)) {
            export_map.put(pAD, Component.DIRECTIONS[(Integer) export_map.get(pAD)]);
        }
        final String pDS = "directions";
        if (export_map.containsKey(pDS)) {
            int[] directions_old = ((int[]) export_map.get(pDS));
            String[] directions = new String[directions_old.length];
            for (int i = 0; i < directions_old.length; i++) {
                directions[i] = Component.DIRECTIONS[directions_old[i]];
            }
            export_map.put(pDS, directions);
        }
        if (ComponentConditional.class.isInstance(c)) {
            ComponentConditional cd = (ComponentConditional) c;
            export_map.put("current", cd.current_default);
        }
        // ComponentArrow doesn't need the directions array, remove it
        if (ComponentArrow.class.isInstance(c)) {
            export_map.remove(pDS);
        }
        // Comments
        if (ComponentComment.class.isInstance(c)) {
            ComponentComment cc = (ComponentComment) c;
            export_map.put("comment", cc.comment);
        }
        if (export_map.containsKey("directions_types")) {
            // TODO translate directions_types, remove meanwhile
            int[][] directions_types = (int[][]) export_map.get("directions_types");
            String[][] directions_types_ = {{}, {}, {}, {}};
            for (int i = 0; i < directions_types.length; i++) {
                String[] directions_types__ = new String[directions_types[i].length];
                for (int j = 0; j < directions_types[i].length; j++) {
                    directions_types__[j] = ComponentPickup.PICKUP_TYPES[directions_types[i][j]];
                }
                directions_types_[i] = directions_types__;
            }
            export_map.put("directions_types", directions_types_);
        }
        if (export_map.containsKey("accepted_types")) {
            int[] accepted_types = (int[]) export_map.get("accepted_types");
            String[] accepted_types_ = new String[accepted_types.length];
            for (int i = 0; i < accepted_types.length; i++) {
                accepted_types_[i] = ComponentPickup.PICKUP_TYPES[accepted_types[i]];
            }
            export_map.put("accepted_types", accepted_types_);
        }
        // Generate the JSON for the component
        Gson gson = new GsonBuilder().create();
        return gson.toJson(export_map);
    }

    public static String exportPaletteToJson(Map<String, Integer> p) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (Entry<String, Integer> entry : p.entrySet()) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append(String.format("\"%s\":{\"count\":%d}", entry.getKey(), entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    public static String exportGoalsToJson(List<GoalCondition> goal_struct) {
        List<Map<String, Object>> goals_required = new ArrayList();
        List<Map<String, Object>> goals_desired = new ArrayList();
        Map<String, List<Map<String, Object>>> goal_struct_export = new LinkedHashMap();
        for (GoalCondition goal : goal_struct) {
            Map<String, Object> export_map = new LinkedHashMap();
            export_map.put("id", goal.component_id);
            export_map.put("type", goal.component_class);
            export_map.put("property", goal.component_property);
            export_map.put("value", goal.component_value);
            export_map.put("condition", goal.condition);
            export_map.put("thread_id", goal.thread_id);
            if (goal.goal_type == GoalCondition.GOAL_REQUIRED) {
                goals_required.add(export_map);
            }
            if (goal.goal_type == GoalCondition.GOAL_DESIRED) {
                goals_desired.add(export_map);
            }
        }
        goal_struct_export.put("required", goals_required);
        goal_struct_export.put("desired", goals_desired);
        Gson gson = new GsonBuilder().create();
        return gson.toJson(goal_struct_export);
        // Note, the default toString() exports maps in a JSON compatible format but the keys are not quoted and uses = instead of :
    }

    public static SortablePair<Integer,String> exportExecutionToTextRepresentationIntermediate(StringBuilder sb, int unit_id, int time, int x, int y) {
        Gson gson = new GsonBuilder().create();
        return new SortablePair(time,"M\t"+time+"\t"+unit_id+"\t"+x+"\t"+y+"\t\n");
        /*sb.append("M\t");
        sb.append(time);
        sb.append("\t");
        sb.append(unit_id);
        sb.append("\t");
        sb.append(x);
        sb.append("\t");
        sb.append(y);
        sb.append("\t");
        sb.append("\n");*/
    }

    public static void exportExecutionToTextRepresentationUnit(StringBuilder sb, ComponentUnit unit, int time, float speed) {
        Gson gson = new GsonBuilder().create();
        sb.append("S\t");
        sb.append(time);
        sb.append("\t");
        sb.append(unit.id);
        sb.append("\t");
        sb.append(unit.x);
        sb.append("\t");
        sb.append(unit.y);
        sb.append("\t");
        Map<String, Object> export_map = new LinkedHashMap();
        export_map.put("speed", speed);
        sb.append(gson.toJson(export_map));
        sb.append("\n");
    }

    public static void exportExecutionToTextRepresentationEvent(StringBuilder sb, int time, Map<String, Object> export_map) {
        Gson gson = new GsonBuilder().create();
        sb.append("E\t");
        sb.append(time);
        sb.append("\t");
        sb.append(0);
        sb.append("\t");
        sb.append(0);
        sb.append("\t");
        sb.append(0);
        sb.append("\t");
        sb.append(gson.toJson(export_map));
        sb.append("\n");
    }

    public static void exportExecutionToTextRepresentationEvent(StringBuilder sb, char type, Component c, int time, Map<String, Object> export_map) {
        Gson gson = new GsonBuilder().create();
        sb.append(type);
        sb.append("\t");
        sb.append(time);
        sb.append("\t");
        sb.append(c.id);
        sb.append("\t");
        sb.append(c.x);
        sb.append("\t");
        sb.append(c.y);
        sb.append("\t");
        if(export_map!=null)
            sb.append(gson.toJson(export_map));
        sb.append("\n");
    }

    public static String exportExecutionToTextRepresentation(GameStateSearch gss, BoardState board, ExecutionPlan ep) {
        StringBuilder sb = new StringBuilder();

        sb.append("EXECUTION\n");

        if (ep == null || ep.getStates() == null || ep.getStates().size() == 0) {
            sb.append('\n');
            return sb.toString();
        }

        List<IntermediateUnitPosition> final_unit_positions = new ArrayList(); // TODO this is only used for units that will not move at all, remove?        

        // Extract unit movement and compute speeds
        Map<Integer, List<Pair<Integer, Integer>>> um = new LinkedHashMap();
        // This is a set of unit_id and a list of x,y pairs

        // Export speed change events in a per-unit basis
        GameState gs_prev = null;
        for (ComponentUnit unit_iter : ep.getStates().get(0).getUnitState().getUnits()) {
            // iterate and export each unit separately
            int unit_id = unit_iter.id;
            float last_reported_speed = 0.0f;
            boolean reported = false;
            gs_prev = null;
            for (GameState gs : ep.getStates()) {
                if (gs_prev == null) {
                    // ignore first step
                } else {
                    ComponentUnit unit = gs.getUnitState().getUnitById(unit_id);
                    ComponentUnit unit_prev = gs_prev.getUnitState().getUnitById(unit_id);
                    float unit_speed = gs.getUnitMovementSpeed(unit_id);

                    if (unit_speed != last_reported_speed) {
                        //exportExecutionToTextRepresentationUnit(sb, unit, gs_prev.getTime(), unit_speed);
                        last_reported_speed = unit_speed;
                        reported = true;
                        //exportExecutionToTextRepresentationIntermediate(sb, unit.id, gs_prev.getTime(), unit_prev.x, unit_prev.y);
                    }
                }
                gs_prev = gs;
            }
            ComponentUnit unit = gs_prev.getUnitState().getUnitById(unit_id);

            // wrap up movement at the end
            if (last_reported_speed > 0.0f) {
                exportExecutionToTextRepresentationUnit(sb, unit, gs_prev.getTime(), 0.0f);
            }
            // edge case for executions when an unit never moved are not reporting anything for the unit, make it explicit that it didn't move    
            if (!reported) {
                exportExecutionToTextRepresentationUnit(sb, unit, 0, 0.0f);
                // collect last unit position for wrap up later
                final_unit_positions.add(new IntermediateUnitPosition(unit.id, unit.tile_current.id, 0));
            }
        }
        // Export all the unit positions without state compression        
        int gs_prev_time = 0;
        List<SortablePair<Integer,String>> positions = new ArrayList();
        for (ComponentUnit unit : ep.getStates().get(0).getUnitState().getUnits()) {
            positions.add(exportExecutionToTextRepresentationIntermediate(sb, unit.id, gs_prev_time, unit.x, unit.y));
        }
        for (GameState gs : ep.getStates()) {
            // TODO if we disable state compression we need to detect waypoints from the list of all states
            for (IntermediateUnitPosition iup : gs.intermediate_unit_positions) {
                Tile tile = board.getTile(iup.tile_id);
                int x = tile.x;
                int y = tile.y;
                positions.add(exportExecutionToTextRepresentationIntermediate(sb, iup.unit_id, gs_prev_time + iup.time, x, y));
            }
            gs_prev_time = gs.getTime();
        }
        for (IntermediateUnitPosition iup : final_unit_positions) {
            Tile tile = board.getTile(iup.tile_id);
            int x = tile.x;
            int y = tile.y;
            positions.add(exportExecutionToTextRepresentationIntermediate(sb, iup.unit_id, iup.time, x, y));

        }
        // Export the positions properly sorted
        Collections.sort(positions);
        for(SortablePair p:positions){
            sb.append(p.m_b);
        }
        
        // Export events from changed properties
        gs_prev = null;
        Set<String> export_props = new LinkedHashSet();
        for (String i : GameStateParser.properties_valid) {
            if(!"directions".equals(i)){
                // TODO figure out why this happened. See the file michael sent me by e-mail, couldn't reproduce.
                export_props.add(i);
            }
        }
        export_props.add("payload");
        export_props.add("passed");
        export_props.add("available");
        int previous_goals = 0;
        for (GameState gs : ep.getStates()) {
            if (gs_prev != null) {
                for (Component c : gs.getComponentState().getComponents()) {
                    Component old = gs_prev.getComponentState().getComponentById(c.id);
                    Field[] fields = c.getClass().getFields();
                    for (Field field : fields) {
                        try {
                            Object vn = field.get(c);
                            Object vo = field.get(old);
                            if (vn != null && !vn.equals(vo) && export_props.contains(field.getName())) {
                                Map<String, Object> export_map = new LinkedHashMap();
                                export_map.put(field.getName(), vn);
                                exportExecutionToTextRepresentationEvent(sb, 'E', old, gs.getTime(), export_map);
                            }
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(GameStateExporter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(GameStateExporter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    if(ComponentDelivery.class.isInstance(c)){
                        ComponentDelivery cd = (ComponentDelivery) c;
                        ComponentDelivery cdo = (ComponentDelivery) old;
                        if (cd.passed>cdo.passed && !(cd.delivered>cdo.delivered)){
                            // No Delivery happened, added event to have some sort of feedback
                            exportExecutionToTextRepresentationEvent(sb, 'F', cd, gs.getTime(), null);
                        }
                    }
                }
                Set<Integer> exchangedIds = new LinkedHashSet();
                for (Component c : gs.getUnitState().getUnits()) {
                    Component old = gs_prev.getUnitState().getUnitById(c.id);
                    Field[] fields = c.getClass().getFields();
                    for (Field field : fields) {
                        try {
                            Object vn = field.get(c);
                            Object vo = field.get(old);
                            if (vn != null && !vn.equals(vo) && export_props.contains(field.getName())) {
                                Map<String, Object> export_map = new LinkedHashMap();
                                export_map.put(field.getName(), vn);
                                exportExecutionToTextRepresentationEvent(sb, 'E', old, gs.getTime(), export_map);
                            }
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(GameStateExporter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(GameStateExporter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    ComponentUnit ou = (ComponentUnit) old;
                    ComponentUnit nu = (ComponentUnit) c;
                    Component component_to = gs.getComponentState().getComponentByPosition(ou.x, ou.y);
                    if (ou.payload.length > 0 && ou.payload.length > nu.payload.length && ComponentDelivery.class.isInstance(component_to)) {
                        // Delivery of packages to delivery points
                        Map<String, Object> export_map = new LinkedHashMap();
                        List<Integer> delivered = toIntegerList(ou.payload);
                        List<Integer> missed = new ArrayList();
                        export_map.put("delivered_items", delivered);
                        export_map.put("missed_items", missed);
                        for (int i = 0; i < nu.payload.length; i++) {
                            delivered.remove(new Integer(nu.payload[i]));
                        }
                        for (int i = 0; i < (nu.missed - ou.missed); i++) {
                            Integer item = delivered.remove(0);
                            missed.add(item);
                        }
                        export_map.put("delivered_to", component_to.id);
                        exportExecutionToTextRepresentationEvent(sb, 'D', old, gs.getTime(), export_map);
                    }
                    if (!compareArrays(ou.payload, nu.payload) && ComponentExchange.class.isInstance(component_to) && !exchangedIds.contains(nu.id)) {
                        ComponentExchange ce = (ComponentExchange) component_to;
                        Component ce2 = gs.getComponentState().getComponentById(ce.link);
                        ComponentUnit ce2u = gs.getUnitState().getUnitByPosition(ce2.x, ce2.y);
                        exchangedIds.add(nu.id);
                        exchangedIds.add(ce2u.id);
                        Map<String, Object> export_map = new LinkedHashMap();
                        export_map.put("exchange_between_a", nu.id);
                        export_map.put("exchange_between_b", ce2u.id);
                        exportExecutionToTextRepresentationEvent(sb, 'D', old, gs.getTime(), export_map);
                    }
                }
                if (gs.evaluateState() > previous_goals) {
                    previous_goals = gs.evaluateState();
                    Map<String, Object> export_map = new LinkedHashMap();
                    export_map.put("goals_completed", previous_goals);
                    exportExecutionToTextRepresentationEvent(sb, gs.getTime(), export_map);
                }

            }
            gs_prev = gs;

        }

        // Export final condition
        if (gs_prev != null) {
            if(gss==null){
                exportExecutionToTextRepresentationEvent(sb, gs_prev.getTime(), exportFinalCondition(gs_prev));
            } else {
                exportExecutionToTextRepresentationEvent(sb, gs_prev.getTime(), exportFinalConditionAndStats(gss, gs_prev));
                
            }
        }

        // Wrap up section
        sb.append("\n");
        return sb.toString();
    }
    
    public static Map<String, Object> exportFinalCondition(GameState gs){
        List<String> reasons = new ArrayList();
        for (int i = 0; i < gs.getBoardState().goal_struct.size(); i++) {
            GoalCondition goal = gs.getBoardState().goal_struct.get(i);
            //if(goal.goal_type==GoalCondition.GOAL_REQUIRED && !gs.testGoal(goal)){
            reasons.add(gs.describeGoal(goal));
        }
        
        Map<String, Object> export_map = new LinkedHashMap();
        export_map.put("final_condition", gs.updateAndGetResultType());
        export_map.put("goal_state", gs.updateAndGetAchievedGoals());
        export_map.put("goal_descriptions", reasons);
        
        return export_map;
    }
    public static Map<String, Object> exportFinalConditionAndStats(GameStateSearch gss, GameState gs){
        Map<String, Object> export_map = exportFinalCondition(gs);
        export_map.put("time_efficiency", gss.getTimeEfficiencyAverageDifference());
        return export_map;
    }

    public static String exportPlayerDataToTextRepresentation(PlayerData pd) {
        return "PLAYER\n\n";
    }

}
