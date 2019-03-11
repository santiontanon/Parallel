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
package game.pcg;

import game.BoardState;
import game.ComponentState;
import game.GameState;
import game.GoalCondition;
import game.Tile;
import game.UnitState;
import game.component.Component;
import game.component.ComponentArrow;
import game.component.ComponentConditional;
import game.component.ComponentDelivery;
import game.component.ComponentDiverter;
import game.component.ComponentExchange;
import game.component.ComponentPickup;
import game.component.ComponentSemaphore;
import game.component.ComponentSignal;
import game.component.ComponentUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lgraphs.LGraph;
import lgraphs.LGraphEdge;
import lgraphs.LGraphNode;
import lgraphs.ontology.Sort;
import orthographicembedding.OrthographicEmbeddingResult;
import support.GameStateParser;
import valls.util.Pair;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class GraphManager {

    OrthographicEmbeddingResult oe;
    LGraph graph;
    LGraph layoutGraph;
    Map<LGraphNode, LGraphNode> map;
    BoardState board;
    List<LGraphNode> node_list;
    Map<Tile, LGraphNode> tile_to_node;
    Map<LGraphNode, Tile> node_to_tile;
    Map<Pair<LGraphNode, LGraphNode>, Tile> node_to_node_by_tile;
    ComponentState components;

    double minx = -1;
    double maxx = -1;
    double miny = -1;
    double maxy = -1;

    static Sort sortHas = null;
    static Sort sortPartOf = null;
    static Sort sortChallenge = null;
    static Sort sortThread = null;
    static Sort sortPickup = null;
    static Sort sortPickupConditional = null;
    static Sort sortPickupUnconditional = null;
    static Sort sortPickupLimited = null;
    static Sort sortPreventor = null;
    static Sort sortDelivery = null;
    static Sort sortButton = null;
    static Sort sortSemaphore = null;
    static Sort sortSemaphoreRed = null;
    static Sort sortConditional = null;
    static Sort sortSubproblem = null;
    static Sort sortLoop = null;
    static Sort sortGoal = null;
    static Sort sortMerge = null;
    static Sort sortTo = null;
    static Sort sortToStarting = null;
    static Sort sortToWithPackage = null;
    static Sort sortToWithoutPackage = null;
    static Sort sortIs = null;
    static Sort sortTrack = null;
    static Sort sortTrash = null;
//    static Sort sortFirst = null;
    static Sort sortDiverter = null;
    static Sort sortExchange = null;
    static Sort sortLink = null;
    static Sort sortDeliverTo = null;

    public GraphManager(OrthographicEmbeddingResult oe, LGraph graph, LGraph layoutGraph, Map<LGraphNode, LGraphNode> map) {
        this.oe = oe;
        this.graph = graph;
        this.layoutGraph = layoutGraph;
        this.map = map;
    }

    public GameState graphToGameStateForVisualEval() {
        // Slightly faster, incomplete, to be used for OGE optimization
        initSorts();
        this.initBoard(false);
        this.addComponentsForVisualEval(); // a smaller version of addDirections() is folded inside
        GameState gs = new GameState(board, components, new UnitState(), new ArrayList<String>());
        gs.initComponents();
        return gs;
    }

    public GameState graphToGameState(List<String> skills) {
        initSorts();
        this.initBoard(true);
        this.addDirections();
        this.addGoalMetadata();
        this.addComponents();
        UnitState units = GameStateParser.extractUnitsFromComponents(components);
        return new GameState(board, components, units, skills);
    }

    public void initBoard(boolean allow_transpose_when_necessary) {
        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            if (i == 0) {
                minx = maxx = oe.x[i];
                miny = maxy = oe.y[i];
            } else {
                if (oe.x[i] < minx) {
                    minx = oe.x[i];
                }
                if (oe.x[i] > maxx) {
                    maxx = oe.x[i];
                }
                if (oe.y[i] < miny) {
                    miny = oe.y[i];
                }
                if (oe.y[i] > maxy) {
                    maxy = oe.y[i];
                }
            }
        }
        int width_in_cells = (int) ((maxx - minx) + 1);
        int height_in_cells = (int) ((maxy - miny) + 1);
        if (allow_transpose_when_necessary && height_in_cells > width_in_cells) {
            // Transpose
            double[] temp_ = oe.x;
            oe.x = oe.y;
            oe.y = temp_;
            int temp__ = width_in_cells;
            width_in_cells = height_in_cells;
            height_in_cells = temp__;
            double temp = minx;
            minx = miny;
            miny = temp;
        }

        board = new BoardState(width_in_cells * 2 - 1, height_in_cells * 2 - 1);
        //board = new BoardState(width_in_cells * 2 +1 , height_in_cells * 2 + 1);
        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            for (int j = 0; j < oe.nodeIndexes.length; j++) {
                if (oe.edges[i][j] || oe.edges[j][i]) {
                    //Track
                    double x0 = Math.min(oe.x[i], oe.x[j]) - minx;
                    double y0 = Math.min(oe.y[i], oe.y[j]) - miny;
                    double x1 = Math.max(oe.x[i], oe.x[j]) - minx;
                    double y1 = Math.max(oe.y[i], oe.y[j]) - miny;
                    for (int y = (int) y0 * 2; y <= y1 * 2; y++) {
                        for (int x = (int) x0 * 2; x <= x1 * 2; x++) {
                            board.getTile(x, y).type = Tile.TILE_TRACK;
                        }
                    }
                } else {
                    // No track
                }
            }
        }
        board.initTileNeighbors();

        node_list = new ArrayList();
        for (LGraphNode node : graph.getNodes()) {
            node_list.add(node);
        }
    }

    static private void initSorts() {
        try {
            sortTo = Sort.getSort("to");
            sortToStarting = Sort.getSort("toStarting");
            sortToWithPackage = Sort.getSort("toWithPackage");
            sortToWithoutPackage = Sort.getSort("toWithoutPackage");
            sortIs = Sort.getSort("is");
            sortTrack = Sort.getSort("track");
            sortHas = Sort.getSort("has");
            sortPartOf = Sort.getSort("partOf");
            sortChallenge = Sort.getSort("challenge"); // currently unused
            sortThread = Sort.getSort("thread");
            sortPickup = Sort.getSort("pickup");
            sortPickupConditional = Sort.getSort("pickup_conditional");
            sortPickupUnconditional = Sort.getSort("pickup_unconditional");
            sortPickupLimited = Sort.getSort("pickup_limited");
            sortDelivery = Sort.getSort("delivery");
            sortButton = Sort.getSort("button");
            sortSemaphore = Sort.getSort("semaphore");
            sortSemaphoreRed = Sort.getSort("semaphore_red");
            sortConditional = Sort.getSort("conditional");
            sortDiverter = Sort.getSort("diverter");
            sortSubproblem = Sort.getSort("subproblem");
            sortLoop = Sort.getSort("loop");
            sortGoal = Sort.getSort("goal"); // currently unused
            sortMerge = Sort.getSort("merge");
            sortTrash = Sort.getSort("trash");
//            sortFirst = Sort.getSort("first");
            sortExchange = Sort.getSort("exchange");
            sortPreventor = Sort.getSort("preventor");
            sortLink = Sort.getSort("link");
            sortDeliverTo = Sort.getSort("deliverTo");
        } catch (Exception ex) {
            Logger.getLogger(GraphManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addDirections() {
        tile_to_node = new LinkedHashMap();
        node_to_tile = new LinkedHashMap();
        node_to_node_by_tile = new LinkedHashMap();

        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            // Node or Shoulder
            int x = (int) (oe.x[i] - minx) * 2;
            int y = (int) (oe.y[i] - miny) * 2;
            // TODO confirm redundant
            board.getTile(x, y).type = Tile.TILE_TRACK;
            if (oe.nodeIndexes[i] >= 0) {
                tile_to_node.put(board.getTile(x, y), map.get(layoutGraph.getNode(oe.nodeIndexes[i])));
                node_to_tile.put(map.get(layoutGraph.getNode(oe.nodeIndexes[i])), board.getTile(x, y));
                // System.out.println(i + " --> " + x + ", " + y);
            } else {
                // Shoulders get a -1
            }
        }
        for (LGraphNode node : graph.getNodes()) {
            for (LGraphNode child : node.getChildNodes(sortTo, sortTrack)) {
                // let's find a path from node to destination
                Tile start = node_to_tile.get(node);
                Tile dest = node_to_tile.get(child);
                //System.out.println("planning route from "+start+" to "+dest);
                Tile found_first = addDirectionsFindStartPathFromnodeToNode(tile_to_node, start, dest);
                if (found_first != null) {
                    //System.out.println(" route found starting at "+found_first);
                    node_to_node_by_tile.put(new Pair(node, child), found_first);
                    addDirectionsMarkTilePathFromNodeToNode(start, found_first, dest);
                }
            }
        }
    }

    private static Tile addDirectionsFindStartPathFromnodeToNode(Map<Tile, LGraphNode> tile_to_node, Tile start, Tile dest) {
        boolean found = false;
        Tile found_route = null;
        for (Tile neighbor : start.neighbors) {
            found_route = neighbor;
            Tile current = neighbor;
            Tile last = start;
            while (true) {
                // check if we are at the desired destination node
                if (current.equals(dest)) {
                    found = true;
                    break;
                }
                // check if we are at another node and break
                if (tile_to_node.get(current) != null) {
                    break;
                }
                // continue exploring this path
                boolean valid = false;
                for (Tile next : current.neighbors) {
                    if (next.equals(last)) {
                        continue;
                    } else {
                        valid = true;
                        last = current;
                        current = next;
                        break;
                    }
                }
                if (!valid) {
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        return found ? found_route : null;
    }

    private static void addDirectionsMarkTilePathFromNodeToNode(Tile start, Tile first, Tile dest) {
        Tile current = first;
        Tile last = start;
//        System.out.println("addDirectionsMarkTilePathFromNodeToNode start " + start.x + "," + start.y + "  first: " + first.x + "," + first.y + "  dest: " + dest.x + "," + dest.y);
        while (true) {
//            System.out.println("     last " + last.x + "," + last.y + "  current: " + current.x + "," + current.y);
            last.traveled_to.add(current);
            if (current.equals(dest)) {
                break;
            }
            for (Tile next : current.neighbors) {
                if (next.equals(last)) {
                    continue;
                } else {
                    last = current;
                    current = next;
                    break;
                }
            }
        }

    }

    private void addComponents() {
        components = new ComponentState();
        
        // Search all pickups to see if we need specific packages:
        int nextPackageColor = 2;
        HashMap<LGraphNode, Integer> packageColors = new HashMap<>();
        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            if (oe.nodeIndexes[i] >= 0) {
                LGraphNode node = map.get(layoutGraph.getNode(oe.nodeIndexes[i]));
                for(LGraphEdge hasComponent : node.getOutgoingEdges(sortHas)) {
                    if (hasComponent.end.subsumedBy(sortPickup)) {
                        for(LGraphEdge deliverTo:node.getOutgoingEdges(sortDeliverTo)) {
                            for(LGraphEdge hasComponent2 : deliverTo.end.getOutgoingEdges(sortHas)) {
                                if (hasComponent2.end.subsumedBy(sortDelivery)) {
                                    // System.out.println("deliverTo: " + hasComponent2);
                                    if (packageColors.containsKey(hasComponent.end)) {
                                        packageColors.put(hasComponent2.end, packageColors.get(hasComponent.end));
                                    } else if (packageColors.containsKey(hasComponent2.end)) {
                                        packageColors.put(hasComponent.end, packageColors.get(hasComponent2.end));
                                    } else {
                                        packageColors.put(hasComponent.end, nextPackageColor);
                                        packageColors.put(hasComponent2.end, nextPackageColor);
                                        nextPackageColor++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        int idx = 1;
        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            // Node or Shoulder
            int x = (int) oe.x[i] * 2;
            int y = (int) oe.y[i] * 2;
            Tile tile = board.getTile(x, y);
            tile.type = Tile.TILE_TRACK;
            if (oe.nodeIndexes[i] >= 0) {
                // System.out.println("adding component: " + map.get(layoutGraph.getNode(oe.nodeIndexes[i])));
                addComponent(tile, x, y, idx++, map.get(layoutGraph.getNode(oe.nodeIndexes[i])), packageColors);
            } else {
                // Shoulders get a -1
            }
        }
    }
    static final int DENOMINATOR_PER_LOOP = 3;

    private void addComponent(Tile tile, int x, int y, int idx, LGraphNode node, HashMap<LGraphNode, Integer> packageColors) {
        int n_components = 0;
        int color = 1;
        /*
        if (false && node.subsumedBy(sortMerge)) {
            // add arrow in merges
            ComponentArrow ca = new ComponentArrow(x, y, idx, color, Component.OWNER_SYSTEM, true);
            LGraphNode next_node = null;
            for (LGraphNode next_child : node.getChildNodes(sortTo, sortTrack)) {
                if (next_node != null) {
                    System.out.println("MERGE cannot have more than one out TRACKS");
                }
                next_node = next_child;
            }
            if (next_node == null) {
                System.out.println("MERGE doesnt have out TRACKS");
            } else {
                Tile t_node_merge = node_to_tile.get(node);
                Tile t_node_next = node_to_node_by_tile.get(new Pair(node, next_node));
                ca.direction = t_node_merge.getDirectionTo(t_node_next);
            }
            components.addComponent(ca);
            n_components++;
        }
        */
        for (LGraphEdge hasComponent : node.getOutgoingEdges(sortHas)) {
            /*
            boolean skipTileComponents = false;
            for (LGraphNode e_ : hasComponent.end.getChildNodes(sortPartOf, sortChallenge)) {
                skipTileComponents = false;
                break;
            }
            if (skipTileComponents) {
                break;
            }
             */
            if (hasComponent.end.subsumedBy(sortPreventor)) {
                // Preventor prevents a component from being instantiated
                continue;
            }
            if (hasComponent.end.subsumedBy(sortThread)) {
//            if (hasComponent.end.subsumes(sortThread)) {
                ComponentUnit cu = new ComponentUnit(x, y, idx, color, Component.OWNER_SYSTEM, true);
                for (Tile neighbor : tile.traveled_to) {
                    cu.initial_direction = tile.getDirectionTo(neighbor);
                }
                components.addComponent(cu);
                n_components++;
                //System.out.println("added thread at " + x + ", " + y + " (" + idx + ")");
            } else if (hasComponent.end.subsumedBy(sortPickup)) {
                idx = 2000 + node_list.indexOf(hasComponent.end);
                int packageColor = color;
                if (packageColors.containsKey(hasComponent.end)) packageColor = packageColors.get(hasComponent.end);
                ComponentPickup cp = new ComponentPickup(x, y, idx, packageColor, Component.OWNER_SYSTEM, true);
                if (hasComponent.end.subsumedBy(sortPickupConditional)) {
                    cp.type = ComponentPickup.CONDITIONAL;
                } else if (hasComponent.end.subsumedBy(sortPickupUnconditional)) {
                    cp.type = ComponentPickup.UNCONDITIONAL;
                } else if (hasComponent.end.subsumedBy(sortPickupLimited)) {
                    cp.type = ComponentPickup.LIMITED;
                }
                components.addComponent(cp);
                n_components++;
            } else if (hasComponent.end.subsumedBy(sortDelivery)) {
//            } else if (hasComponent.end.subsumes(sortDelivery)) {
                idx = 2000 + node_list.indexOf(hasComponent.end);
                int packageColor = color;
                if (packageColors.containsKey(hasComponent.end)) packageColor = packageColors.get(hasComponent.end);
                ComponentDelivery cd = new ComponentDelivery(x, y, idx, packageColor, Component.OWNER_SYSTEM, true);                
                cd.denominator = 1;
                cd.accepted_colors = new int[]{packageColor};
                for (LGraphNode partOf : node.getChildNodes(sortPartOf, sortSubproblem)) {
                    if (partOf.subsumedBy(sortLoop)) {
                        cd.denominator = DENOMINATOR_PER_LOOP;
                    }
                }
                components.addComponent(cd);
                n_components++;
//            } else if (hasComponent.end.subsumes(sortButton)) {
            } else if (hasComponent.end.subsumedBy(sortButton)) {
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentSignal cb = new ComponentSignal(x, y, idx, color, Component.OWNER_SYSTEM, true);
                LGraphNode cs = null;
                try {
//                    cs = hasComponent.end.getFirstChildNode(sortHas);
//                    if (cs == null) cs = hasComponent.end.getFirstChildNode(sortPartOf);
                    cs = hasComponent.end.getFirstChildNode(sortLink);
                } catch (Exception ex) {
                    Logger.getLogger(GraphManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (cs != null) {
                    cb.link = 1000 + node_list.indexOf(cs);
                }
                components.addComponent(cb);
                n_components++;
            } else if (hasComponent.end.subsumedBy(sortSemaphore)) {
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentSemaphore cs = new ComponentSemaphore(x, y, idx, color, Component.OWNER_SYSTEM, true);
                cs.value = ComponentSemaphore.GREEN;
                if (hasComponent.end.subsumedBy(sortSemaphoreRed)) {
                    cs.value = ComponentSemaphore.RED;
                }
                components.addComponent(cs);
                n_components++;
//            } else if (hasComponent.end.subsumes(sortConditional)) {
            } else if (hasComponent.end.subsumedBy(sortConditional)) {
                List<Integer> directions = new ArrayList<Integer>();
                Tile t = node_to_tile.get(node);
                int current = 0;
                for (LGraphNode child : node.getChildNodes(sortTo, sortTrack)) {
                    Tile t_ = node_to_node_by_tile.get(new Pair(node, child));
                    LGraphEdge edge = node.getEdge(child);
//                    System.out.println("conditional out edge: " + edge.labelSet);
                    if (edge.labelSet.subsumedBy(sortToStarting)) {
                        // we don't add it yet, we will add it later (also, we assume there is only one)
                        directions.add(t.getDirectionTo(t_));
                        current = directions.size() - 1;
                    } else {
                        directions.add(t.getDirectionTo(t_));
                    }
//                    if (child.getFirstChildNode(sortIs, sortFirst) != null) {
//                        current = directions.size() - 1;
//                    }
                }
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentConditional cc = new ComponentConditional(x, y, idx, color, Component.OWNER_SYSTEM, true);
                cc.directions = valls.util.ListToArrayUtility.toIntArray(directions);
                cc.current = current;
                components.addComponent(cc);
                n_components++;
//            } else if (hasComponent.end.subsumes(sortDiverter)) {
            } else if (hasComponent.end.subsumedBy(sortDiverter)) {
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentDiverter cc = new ComponentDiverter(x, y, idx, color, Component.OWNER_SYSTEM, true);
                Tile t = node_to_tile.get(node);
                for (LGraphEdge e : node.getOutgoingEdges(sortTo)) {
                    if (e.end.subsumedBy(sortTrack)) {
                        int direction = t.getDirectionTo(node_to_tile.get(e.end));
                        //System.out.println("e.end position: " + node_to_tile.get(e.end).x + ", " + node_to_tile.get(e.end).y);
                        if (e.labelSet.subsumedBy(sortToWithPackage)) {
                            cc.directions_colors[direction] = new int[]{1, 2, 3, 4, 5, 6};
                            cc.directions_types[direction] = new int[]{ComponentPickup.CONDITIONAL, ComponentPickup.UNCONDITIONAL, ComponentPickup.LIMITED};
                            //System.out.println("Diverter -> toWithPackage -> " + direction);
                        } else if (e.labelSet.subsumedBy(sortToWithoutPackage)) {
                            cc.directions_colors[direction] = new int[]{-1};
                            cc.directions_types[direction] = new int[]{ComponentPickup.EMPTY};
                            //System.out.println("Diverter -> sortToWithoutPackage -> " + direction);
                        } else if (e.end.getFirstChildNode(sortIs, sortTrash) != null) {
                            cc.directions_colors[direction] = new int[]{-1};
                            cc.directions_types[direction] = new int[]{ComponentPickup.EMPTY};
                            //System.out.println("Diverter -> trash -> " + direction);
                        } else {
                            cc.directions_colors[direction] = new int[]{1, 2, 3, 4, 5, 6};
                            cc.directions_types[direction] = new int[]{ComponentPickup.CONDITIONAL, ComponentPickup.UNCONDITIONAL, ComponentPickup.LIMITED};
                            //System.out.println("Diverter -> - -> " + direction);
                        }
                    }
                }
                /*
                int direction_with_trash = -1;
                int direction_without_trash = -1;
                for(LGraphNode child:node.getChildNodes(sortTo,sortTrack)){
                    if(child.getFirstChildNode(sortIs, sortTrash)!=null){
                        direction_with_trash = t.getDirectionTo(node_to_tile.get(child));
                    } else {
                        direction_without_trash = t.getDirectionTo(node_to_tile.get(child));
                    }
                }
                // get bad direction, set
                if(direction_with_trash>-1){
                    cc.directions_colors[direction_with_trash] = new int[]{-1};
                    cc.directions_types[direction_with_trash] = new int[]{ComponentPickup.EMPTY};
                } else {
                    System.err.println("The diverter doesn't have a TRASH (1)");
                }
                if(direction_without_trash>-1){
                    cc.directions_colors[direction_without_trash] = new int[]{1,2,3,4,5,6};
                    cc.directions_types[direction_without_trash] = new int[]{ComponentPickup.CONDITIONAL,ComponentPickup.UNCONDITIONAL,ComponentPickup.LIMITED};
                }
                 */
                components.addComponent(cc);
                n_components++;

//            } else if (hasComponent.end.subsumes(sortExchange)) {
            } else if (hasComponent.end.subsumedBy(sortExchange)) {
                idx = 4000 + node_list.indexOf(hasComponent.end);
                ComponentExchange cu = new ComponentExchange(x, y, idx, color, Component.OWNER_SYSTEM, true);
                LGraphNode other = hasComponent.end.getFirstChildNode(sortPartOf, sortExchange);
                if (other != null) {
                    cu.link = 4000 + node_list.indexOf(other);
                }
                components.addComponent(cu);
                n_components++;
            } else {
                System.out.println("Node has wrong component");
            }

        }
        if (n_components > 1) {
            System.out.println("Node " + node.getLabelSet().toString() + " has more than one component");
        }
    }

    private void addGoalMetadata() {
        board.level_title = "PCG Level";
        board.goal_string = "Deliver all packages indicated in each delivery point.";
        board.goal_struct = new ArrayList<GoalCondition>();
        //public Map<String, Integer> player_palette;
        board.max_colors = 1;
        for (LGraphNode node : graph.getNodes()) {
            for (LGraphEdge hasComponent : node.getOutgoingEdges(sortHas)) {
                boolean isGoal = true;
                /*
                for (LGraphEdge e_ : hasComponent.end.getOutgoingEdges(sortPartOf, sortGoal)) {
                    isGoal = true;
                }
                 */
                if (isGoal) {
                    if (hasComponent.end.subsumedBy(sortPreventor)) {
                        // Preventor prevented the component from being instantiated therefore should not contribute to the goals
                        continue;
                    }
                    if (hasComponent.end.subsumedBy(sortThread)) {
                    } else if (hasComponent.end.subsumedBy(sortPickup)) {
                    } else if (hasComponent.end.subsumedBy(sortDelivery)) {
                        int idx = 2000 + node_list.indexOf(hasComponent.end);
                        int thread_id = 0;
                        int denominator = 0;
                        for (LGraphNode partOf : node.getChildNodes(sortPartOf, sortSubproblem)) {
                            if (partOf.subsumedBy(sortLoop)) {
                                denominator = DENOMINATOR_PER_LOOP - 1;
                            }
                        }

                        //(int component_id, String component_class, String property, int value, String condition, int goal_type, int thread_id) {
                        board.goal_struct.add(new GoalCondition(idx, "delivery", "delivered", denominator, "gt", GoalCondition.GOAL_REQUIRED, thread_id));
                    } else if (hasComponent.end.subsumedBy(sortButton)) {
                    } else if (hasComponent.end.subsumedBy(sortSemaphore)) {
                    } else {
                        //boolean isGoal = true;
                        //System.out.println("Challenge has wrong component");
                    }
                }
            }

        }
    }

    private void addComponentsForVisualEval() {

        tile_to_node = new LinkedHashMap();
        node_to_tile = new LinkedHashMap();

        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            // Node or Shoulder
            int x = (int) (oe.x[i] - minx) * 2;
            int y = (int) (oe.y[i] - miny) * 2;
            // TODO confirm redundant
            board.getTile(x, y).type = Tile.TILE_TRACK;

            if (oe.nodeIndexes[i] >= 0) {
                tile_to_node.put(board.getTile(x, y), map.get(layoutGraph.getNode(oe.nodeIndexes[i])));
                node_to_tile.put(map.get(layoutGraph.getNode(oe.nodeIndexes[i])), board.getTile(x, y));
            } else {
                // Shoulders get a -1
            }
        }

        components = new ComponentState();
        int idx = 0;
        for (int i = 0; i < oe.nodeIndexes.length; i++) {
            // Node or Shoulder
            int x = (int) (oe.x[i] - minx) * 2;
            int y = (int) (oe.y[i] - miny) * 2;
            Tile tile = board.getTile(x, y);
            tile.type = Tile.TILE_TRACK;
            if (oe.nodeIndexes[i] >= 0) {
                LGraphNode embedded_graph_node = layoutGraph.getNode(oe.nodeIndexes[i]);
                LGraphNode original_graph_node = map.get(embedded_graph_node);
                if (original_graph_node != null) {
                    addComponentForVisualEval(tile, x, y, idx++, original_graph_node);
                }
            } else {
                // Shoulders get a -1
            }
        }
    }

    private void addComponentForVisualEval(Tile tile, int x, int y, int idx, LGraphNode node) {
        int n_components = 0;
        int color = 1;
        for (LGraphEdge hasComponent : node.getOutgoingEdges(sortHas)) {
            if (hasComponent.end.subsumedBy(sortPreventor)) {
                // Preventor prevents a component from being instantiated
                continue;
            }
            if (hasComponent.end.subsumes(sortThread)) {
                ComponentUnit cu = new ComponentUnit(x, y, idx, color, Component.OWNER_SYSTEM, true);
                // System.out.println("added thread (vor visual eval) at " + x + ", " + y + " (" + idx + ")");
                for (Tile neighbor : tile.traveled_to) {
                    cu.initial_direction = tile.getDirectionTo(neighbor);
                }
                components.addComponent(cu);
                n_components++;
            } else if (hasComponent.end.subsumedBy(sortPickup)) {
                idx = 2000 + node_list.indexOf(hasComponent.end);
                ComponentPickup cp = new ComponentPickup(x, y, idx, color, Component.OWNER_SYSTEM, true);
                if (hasComponent.end.subsumedBy(sortPickupConditional)) {
                    cp.type = ComponentPickup.CONDITIONAL;
                } else if (hasComponent.end.subsumedBy(sortPickupUnconditional)) {
                    cp.type = ComponentPickup.UNCONDITIONAL;
                } else if (hasComponent.end.subsumedBy(sortPickupLimited)) {
                    cp.type = ComponentPickup.LIMITED;
                }
                components.addComponent(cp);
                n_components++;
            } else if (hasComponent.end.subsumes(sortDelivery)) {
                idx = 2000 + node_list.indexOf(hasComponent.end);
                ComponentDelivery cd = new ComponentDelivery(x, y, idx, color, Component.OWNER_SYSTEM, true);
                cd.denominator = 1;
                for (LGraphNode partOf : node.getChildNodes(sortPartOf, sortSubproblem)) {
                    if (partOf.subsumedBy(sortLoop)) {
                        cd.denominator = DENOMINATOR_PER_LOOP;
                    }
                }
                components.addComponent(cd);
                n_components++;
            } else if (hasComponent.end.subsumes(sortButton)) {
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentSignal cb = new ComponentSignal(x, y, idx, color, Component.OWNER_SYSTEM, true);
                LGraphNode cs = null;
                try {
//                    cs = hasComponent.end.getFirstChildNode(sortHas);
                    cs = hasComponent.end.getFirstChildNode(sortLink);
                } catch (Exception ex) {
                    Logger.getLogger(GraphManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (cs != null) {
                    cb.link = 1000 + node_list.indexOf(cs);
                }
                components.addComponent(cb);
                n_components++;
            } else if (hasComponent.end.subsumedBy(sortSemaphore)) {
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentSemaphore cs = new ComponentSemaphore(x, y, idx, color, Component.OWNER_SYSTEM, true);
                cs.value = ComponentSemaphore.GREEN;
                if (hasComponent.end.subsumedBy(sortSemaphoreRed)) {
                    cs.value = ComponentSemaphore.RED;
                }
                components.addComponent(cs);
                n_components++;
            } else if (hasComponent.end.subsumes(sortConditional)) {
                List<Integer> directions = new ArrayList<Integer>();
                int current = 0;
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentConditional cc = new ComponentConditional(x, y, idx, color, Component.OWNER_SYSTEM, true);
                cc.directions = valls.util.ListToArrayUtility.toIntArray(directions);
                cc.current = current;
                components.addComponent(cc);
                n_components++;
            } else if (hasComponent.end.subsumes(sortDiverter)) {
                idx = 1000 + node_list.indexOf(hasComponent.end);
                ComponentDiverter cc = new ComponentDiverter(x, y, idx, color, Component.OWNER_SYSTEM, true);
                Tile t = node_to_tile.get(node);
                int direction_bad = -1;
                int direction_good = -1;
                for (LGraphNode child : node.getChildNodes(sortTo, sortTrack)) {
                    if (node_to_tile.get(child) != null) {
                        // TODO review why I get a nullpointer exception here, happened on game.pcg.GraphManager.addComponentForVisualEval(GraphManager.java:651)
                        if (child.getFirstChildNode(sortIs, sortTrash) != null) {
                            direction_bad = t.getDirectionTo(node_to_tile.get(child));
                        } else {
                            direction_good = t.getDirectionTo(node_to_tile.get(child));
                        }
                    }
                }
                // get bad direction, set
                if (direction_bad > -1) {
                    cc.directions_colors[direction_bad] = new int[]{-1};
                    cc.directions_types[direction_bad] = new int[]{ComponentPickup.EMPTY};

                } else {
//                    System.err.println("The diverter doesn't have a TRASH (2)");
                }
                if (direction_good > -1) {
                    cc.directions_colors[direction_good] = new int[]{1, 2, 3, 4, 5, 6};
                    cc.directions_types[direction_good] = new int[]{ComponentPickup.CONDITIONAL, ComponentPickup.UNCONDITIONAL, ComponentPickup.LIMITED};
                }
                components.addComponent(cc);
                n_components++;

            } else if (hasComponent.end.subsumes(sortExchange)) {
                idx = 4000 + node_list.indexOf(hasComponent.end);
                ComponentExchange cu = new ComponentExchange(x, y, idx, color, Component.OWNER_SYSTEM, true);
                LGraphNode other = hasComponent.end.getFirstChildNode(sortPartOf, sortExchange);
                if (other != null) {
                    cu.link = 4000 + node_list.indexOf(other);
                }
                components.addComponent(cu);
                n_components++;
            } else {
                //System.out.println("Node has wrong component");
            }

        }
        if (n_components > 1) {
            //System.out.println("Node " + node.getLabelSet().toString() + " has more than one component");
        }
    }

}
