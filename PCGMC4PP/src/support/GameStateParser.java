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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import game.BoardState;
import game.component.Component;
import game.component.ComponentConditional;
import game.component.ComponentDelivery;
import game.component.ComponentExchange;
import game.component.ComponentIntersection;
import game.component.ComponentLine;
import game.component.ComponentPainter;
import game.component.ComponentPickup;
import game.component.ComponentSemaphore;
import game.component.ComponentSignal;
import game.ComponentState;
import game.component.ComponentUnit;
import game.GameState;
import game.GoalCondition;
import game.Tile;
import game.UnitState;
import game.component.ComponentArrow;
import game.component.ComponentComment;
import game.component.ComponentCustoms;
import game.component.ComponentDiverter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class GameStateParser {

    public static int errors = 0;
    public static char color_base = ' ';
    public static boolean ignore_tile_colors = false;
    private static String tileDirectionsCache = null;

    public static Class<? extends Component>[] componentList = new Class[]{
        ComponentUnit.class,
        ComponentSignal.class,
        ComponentConditional.class,
        ComponentDelivery.class,
        ComponentExchange.class,
        ComponentIntersection.class,
        ComponentLine.class,
        ComponentPainter.class,
        ComponentPickup.class,
        ComponentSemaphore.class,
        ComponentDiverter.class,
        ComponentCustoms.class,
        ComponentArrow.class,
        ComponentComment.class,};
    public static Set<String> componentNames = new HashSet();

    public static void prepareParser() {
        if (componentNames.size() == 0) {
            for (Class componentClass : componentList) {
                try {
                    componentNames.add((String) componentClass.getField("representation").get(componentClass));
                    tileDirectionsCache = new String(Tile.DIRECTIONS);
                } catch (Exception ex) {

                }
            }
        }
    }

    public static GameState parseFile(String filename, boolean verbose) throws FileNotFoundException, IOException, ParseException, Exception {
        return parseFile(filename, verbose, false);
    }
    public static GameState parseFile(String filename, boolean verbose, boolean remove_solution) throws FileNotFoundException, IOException, ParseException, Exception {
        GameStateParser.prepareParser();

        if (verbose) {
            System.err.println("Loading: " + filename);
        }

        BoardState board = null;
        ComponentState components = null;
        UnitState units = null;
        List<String> data = new ArrayList();

        FileInputStream fis = new FileInputStream(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        try {
            int line_number = 0;
            String line = null;
            String current_section = null;
            while ((line = br.readLine()) != null) {
                line_number++;
                if (verbose) {
                    System.out.println(line_number + ": " + line);
                }
                if (line.length() > 0 && current_section == null) {
                    current_section = line.trim();
                } else if (line.length() > 0 && current_section != null && current_section != "PLAYER") {
                    data.add(line);
                } else if (line.length() > 0 && current_section != null && current_section == "PLAYER") {
                    // TODO PlayerModel.addEventFromLine(line)
                } else if (line.length() == 0 && current_section == null) {
                    if (verbose) {
                        System.err.println("Empty line " + line_number + " ignored when parsing file ");
                    }
                } else if (line.length() == 0 && current_section != null) {
                    if ("METADATA".equals(current_section)) {
                        board = parseFileMetadata(data, line_number);
                    } else if ("LAYOUT".equals(current_section)) {
                        if (board == null) {
                            throw new ParseException("LAYOUT Section before METADATA Section", line_number);
                        } else {
                            parseFileLayout(data, line_number - data.size(), board);
                        }
                    } else if ("COLORS".equals(current_section)) {
                        if (board == null) {
                            throw new ParseException("COLORS Section before METADATA Section", line_number);
                        } else if (!GameStateParser.ignore_tile_colors) {
                            parseFileColors(data, line_number - data.size(), board);
                        }
                    } else if ("DIRECTIONS".equals(current_section)) {
                        if (board == null) {
                            throw new ParseException("DIRECTIONS Section before METADATA Section", line_number);
                        } else if (!GameStateParser.ignore_tile_colors) {
                            parseFileDirections(data, line_number - data.size(), board);
                        }
                    } else if ("COMPONENTS".equals(current_section)) {
                        if (board == null) {
                            throw new ParseException("COMPONENTS Section before METADATA Section", line_number);
                        } else {
                            components = parseFileComponents(data, line_number - data.size(), board);
                            units = extractUnitsFromComponents(components);
                        }
                    } else if ("EXECUTION".equals(current_section)) {
                        if (verbose) {
                            System.err.println("EXECUTION Section ignored when parsing file.");
                        }
                        break;
                    } else if (verbose) {
                        System.err.println("Unknown section ignored when parsing file.");
                    }
                    current_section = null;
                    data.clear();
                }
            }
        } finally {
            br.close();
        }
        if (board == null) {
            throw new ParseException("Board is null", 0);
        }
        if (components == null) {
            throw new ParseException("Components is null", 0);
        }
        if (units == null) {
            throw new ParseException("Units is null", 0);
        }

        GameState gs = new GameState(board, components, units);
        gs.init();
        return gs;
    }

    public static BoardState parseFileMetadata(List<String> data, int offset) {
        BoardState board = new BoardState();
        int board_width = 0;
        int board_height = 0;
        int line_number = -1;
        for (String line : data) {
            line_number++;
            String[] properties = line.split("\t");
            if (properties.length != 2) {
                System.err.println("Ignoring line " + (offset - data.size() + line_number));
                errors++;
                continue;
            }
            if ("level_id".equals(properties[0])) {
                board.level_id = Integer.parseInt(properties[1]);
            } else if ("level_title".equals(properties[0])) {
                board.level_title = properties[1];
            } else if ("goal_string".equals(properties[0])) {
                board.goal_string = properties[1];
            } else if ("goal_struct".equals(properties[0])) {
                board.goal_struct = GameStateParser.parseWinningConditions(properties[1], (offset - data.size() + line_number));
            } else if ("player_palette".equals(properties[0])) {
                board.player_palette = GameStateParser.parsePalette(properties[1], (offset - data.size() + line_number));
                if (board.player_palette.containsKey("colors")) {
                    board.max_colors = board.player_palette.get("colors");
                }
            } else if ("time_pickup".equals(properties[0])) {
                board.time_pickup_min = Integer.parseInt(properties[1]);
                board.time_pickup_max = Integer.parseInt(properties[1]);
            } else if ("time_delivery".equals(properties[0])) {
                board.time_delivery_min = Integer.parseInt(properties[1]);
                board.time_delivery_max = Integer.parseInt(properties[1]);
            } else if ("time_exchange".equals(properties[0])) {
                board.time_exchange_min = Integer.parseInt(properties[1]);
                board.time_exchange_max = Integer.parseInt(properties[1]);
            } else if ("time_pickup_min".equals(properties[0])) {
                board.time_pickup_min = Integer.parseInt(properties[1]);
            } else if ("time_delivery_min".equals(properties[0])) {
                board.time_delivery_min = Integer.parseInt(properties[1]);
            } else if ("time_exchange_min".equals(properties[0])) {
                board.time_exchange_min = Integer.parseInt(properties[1]);
            } else if ("time_pickup_max".equals(properties[0])) {
                board.time_pickup_max = Integer.parseInt(properties[1]);
            } else if ("time_delivery_max".equals(properties[0])) {
                board.time_delivery_max = Integer.parseInt(properties[1]);

            } else if ("time_exchange_max".equals(properties[0])) {
                board.time_exchange_max = Integer.parseInt(properties[1]);
            } else if ("board_width".equals(properties[0])) {
                board_width = Integer.parseInt(properties[1]);
            } else if ("board_height".equals(properties[0])) {
                board_height = Integer.parseInt(properties[1]);

            }
        }
        if (board_width > 0 && board_height > 0) {
            board.setSize(board_width, board_height);
        } else {
            System.err.println("Wrong board size " + offset);
            errors++;
        }
        return board;
    }

    public static void parseFileLayout(List<String> data, int offset, BoardState board) throws ParseException {
        if (data.size() != board.getHeight()) {
            System.err.println("LAYOUT height and METADATA height mismatch.");
            errors++;
        } else {
            {
                int y = 0;
                for (String line : data) {
                    if (line.length() != board.getWidth()) {
                        System.err.println("LAYOUT width and METADATA width mismatch on line " + (offset + y) + ".");
                        errors++;
                    }
                    int x = 0;
                    for (char tile : line.toCharArray()) {
                        board.getTile(x, y).type = tile == '-' ? Tile.TILE_EMPTY : Tile.TILE_TRACK;
                        x++;
                    }
                    y++;
                }
            }
            // Sanity check for tile types, used for exporting
            board.initTileNeighbors();
            {
                int y = 0;
                for (String line : data) {
                    int x = 0;
                    for (char tile : line.toCharArray()) {
                        if (tile == '-') {
                            tile = '@';
                        }
                        int bitmask = tile - (int) '@';
                        if (board.getTile(x, y).getTileBitmask() != bitmask) {
                            char expected = (char) (board.getTile(x, y).getTileBitmask() + (int) '@');
                            if(expected=='A' || expected=='B' || expected=='D' || expected =='H'){
                                // this is the case for the vertical end that Justin doesn't support just yet
                            } else {
                                System.err.println("Tile bitmask mismatch on line " + (offset + y) + ", column " + x + " expected " + expected + " found " + tile + ".");
                                errors++;
                            }
                            board.getTile(x, y).setTileBitmask(bitmask);
                        }
                        x++;
                    }
                    y++;
                }
            }
        }
    }

    public static void parseFileDirections(List<String> data, int offset, BoardState board) throws ParseException {
        if (data.size() != board.getHeight()) {
            System.err.println("DIRECTIONS height and METADATA height mismatch.");
            errors++;
        } else {
            int y = 0;
            for (String line : data) {
                if (line.length() != board.getWidth()) {
                    System.err.println("DIRECTIONS width and METADATA width mismatch on line " + (offset + y) + ".");
                    errors++;
                }
                int x = 0;
                for (char tile : line.toCharArray()) {
                    board.getTile(x, y).direction = tileDirectionsCache.indexOf(tile);
                    if (board.getTile(x, y).direction == -1){
                        System.err.println("DIRECTIONS has a bad direction "+tile +" in "+ (offset + y) + ".");
                        errors++;                        
                    }
                    switch (tile) {
                        case ' ':
                            break;
                        case 'A':
                            board.getTile(x, y).traveled_to.add(board.getTile(x, y - 1));
                            break;
                        case 'V':
                            board.getTile(x, y).traveled_to.add(board.getTile(x, y + 1));
                            break;
                        case '<':
                            board.getTile(x, y).traveled_to.add(board.getTile(x - 1, y));
                            break;
                        case '>':
                            board.getTile(x, y).traveled_to.add(board.getTile(x + 1, y));
                            break;
                        case 'X':
                            board.getTile(x, y).traveled_to.addAll(board.getTile(x, y).neighbors);
                            board.getTile(x, y).overpass = true;
                            break;
                        default:
                            System.err.println("DIRECTIONS has an invalid character on line " + (offset + y) + ".");
                            break;
                    }
                    x++;
                }
                y++;
            }
        }
    }

    public static void parseFileColors(List<String> data, int offset, BoardState board) throws ParseException {
        if (data.size() != board.getHeight()) {
            System.err.println("COLORS height and METADATA height mismatch.");
            errors++;
        } else {
            int y = 0;
            for (String line : data) {
                if (line.length() != board.getWidth()) {
                    System.err.println("COLORS width and METADATA width mismatch on line " + (offset + y) + ".");
                    errors++;
                }
                int x = 0;
                for (char tile_color : line.toCharArray()) {
                    int bitmask = tile_color - (int) color_base;
                    Tile tile = board.getTile(x, y);
                    if (tile != null) {
                        tile.colors = bitmask;
                    }
                    x++;
                }
                y++;
            }
        }

    }

    public static ComponentState parseFileComponents(List<String> data, int offset, BoardState board) throws Exception {

        ComponentState components = new ComponentState();
        int line_number = -1;
        for (String line : data) {
            line_number++;
            String[] properties = line.split("\t");
            if (properties.length != 7) {
                System.err.println("Wrong number of columns at line " + (offset + line_number));
                errors++;
                continue;
            }
            int id = Integer.parseInt(properties[0]);
            int x = Integer.parseInt(properties[2]);
            int y = Integer.parseInt(properties[3]);
            char owner = properties[4].charAt(0);
            boolean locked = properties[5].charAt(0) == 'L';
            if (owner != Component.OWNER_PLAYER && owner != Component.OWNER_SYSTEM) {
                System.err.println("Wrong owner for component in line " + (offset + line_number));
                errors++;
            }
            Tile tile = board.getTile(x, y);
            if (tile == null || !tile.isPassable()) {
                System.err.println("Component in line " + (offset + line_number) + " placed on an empty tile");
                errors++;
            }
            try {
                for (Class<? extends Component> componentClass : componentList) {
                    String representation = (String) componentClass.getField("representation").get(componentClass);
                    if (representation.equals(properties[1])) {
                        Constructor<? extends Component> ctor = componentClass.getConstructor(int.class, int.class, int.class, int.class, char.class, boolean.class);
                        Component component = ctor.newInstance(new Object[]{x, y, id, 0, owner, locked});
                        parseComponentJson(component, properties[6], offset + line_number);
                        components.addComponent(component);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Cannot create component on line " + (offset + line_number) + " error: " + ex);
                errors++;
            }
        }
        return components;
    }

    public static final String[] properties_valid = new String[]{
        "color", // common
        "picked", "delivered", "missed", "passed", "exchanged", "painted", // for checking goals, default to 0
        "delay", "number", "frequency", // intended for spawners, unused
        "initial_direction", // for units
        "time_pickup_min", "time_delivery_min", "time_exchange_min", "time_pickup_max", "time_delivery_max", "time_exchange_max",
        "type", "directions", "capacity", "link", "value", "stopped", "consumer", "strict", "stop", "current", // for other components
        "direction_condition", "direction_default", // for diverter
        "direction", // for arrow
        "accepted_types", "accepted_colors", // for delivery
        "directions_types", "directions_colors", // for diverters
        "denominator", // for diverter UI
    };

    private static Set<String> properties_valid_set = null; // updated in parseComponentJson with properties_valid

    private static List<GoalCondition> parseConditions(JsonArray jsonArray, int goal_type) {
        List<GoalCondition> goals = new ArrayList();
        for (JsonElement entry : jsonArray) {
            int thread_id = 0;
            if (entry.getAsJsonObject().has("thread_id")) {
                thread_id = entry.getAsJsonObject().get("thread_id").getAsInt();
            }

            goals.add(new GoalCondition(
                    entry.getAsJsonObject().get("id").getAsInt(),
                    entry.getAsJsonObject().get("type").getAsString(),
                    entry.getAsJsonObject().get("property").getAsString(),
                    entry.getAsJsonObject().get("value").getAsInt(),
                    entry.getAsJsonObject().get("condition").getAsString(),
                    goal_type,
                    thread_id
            ));
        }
        return goals;
    }

    public static List<GoalCondition> parseWinningConditions(String json, int line_number) {
        List<GoalCondition> goals = new ArrayList();
        try {
            JsonElement root = new JsonParser().parse(json);
            goals.addAll(GameStateParser.parseConditions(root.getAsJsonObject().get("required").getAsJsonArray(), GoalCondition.GOAL_REQUIRED));
            goals.addAll(GameStateParser.parseConditions(root.getAsJsonObject().get("desired").getAsJsonArray(), GoalCondition.GOAL_DESIRED));
        } catch (Exception ex) {
            System.err.println("Malformed JSON for goals on line " + line_number);
            errors++;
        }
        return goals;

    }

    public static Map<String, Integer> parsePalette(String json, int line_number) {
        Map<String, Integer> palette = new HashMap();
        try {
            JsonElement root = new JsonParser().parse(json);
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                if (componentNames.contains(entry.getKey()) || "colors".equals(entry.getKey())) {
                    palette.put(entry.getKey(), entry.getValue().getAsJsonObject().get("count").getAsInt());
                } else {
                    System.err.println("Unknown component " + entry.getKey() + " for palette on line " + line_number);
                    errors++;
                }
            }
        } catch (Exception ex) {
            System.err.println("Malformed JSON for palette on line " + line_number);
            errors++;
        }

        return palette;
    }

    public static void parseComponentJson(Component c, String json, int line_number) {
        // Lazy initialization
        if (properties_valid_set == null) {
            properties_valid_set = new HashSet();
            properties_valid_set.addAll(Arrays.asList(properties_valid));
        }
        JsonElement root = new JsonParser().parse(json);
        // Iterate properties from the JSON part
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
            if (properties_valid_set.contains(entry.getKey())) {
                // Handle special properties
                if ("type".equals(entry.getKey())) {
                    if (!ComponentPickup.class.isInstance(c)) {
                        System.err.println("Cannot set property type for " + c.getClass().getName() + " in line " + line_number);
                    } else {
                        ComponentPickup c_ = (ComponentPickup) c;
                        try {
                            c_.type = Arrays.asList(ComponentPickup.PICKUP_TYPES).indexOf(entry.getValue().getAsString());
                            if (c_.type < 0) {
                                System.err.println("Invalid type for pickup component property " + entry.getValue().getAsString() + " in line " + line_number);
                                errors++;
                            }
                        } catch (Exception ex) {
                            System.err.println("Invalid type for pickup component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("initial_direction".equals(entry.getKey())) {
                    if (!ComponentUnit.class.isInstance(c)) {
                        System.err.println("Cannot set property initial_direction for " + c.getClass().getName() + " in line " + line_number);
                    } else {
                        ComponentUnit c_ = (ComponentUnit) c;
                        try {
                            int initial_direction = Arrays.asList(Component.DIRECTIONS).indexOf(entry.getValue().getAsString());
                            if (initial_direction < 0) {
                                System.err.println("Invalid initial_direction component property " + entry.getValue().getAsString() + " in line " + line_number);
                                errors++;
                            }
                            c_.initial_direction = initial_direction;
                            c_.direction = initial_direction;
                        } catch (Exception ex) {
                            System.err.println("Invalid initial_direction component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("direction_condition".equals(entry.getKey())) {
                    if (!ComponentDiverter.class.isInstance(c)) {
                        System.err.println("Cannot set property direction_condition for " + c.getClass().getName() + " in line " + line_number);
                    } else {
                        ComponentDiverter c_ = (ComponentDiverter) c;
                        try {
                            int direction_condition = Arrays.asList(Component.DIRECTIONS).indexOf(entry.getValue().getAsString());
                            if (direction_condition < 0) {
                                System.err.println("Invalid direction_condition component property " + entry.getValue().getAsString() + " in line " + line_number);
                                errors++;
                            }
                            c_.direction_condition = direction_condition;
                        } catch (Exception ex) {
                            System.err.println("Invalid direction_condition component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("direction_default".equals(entry.getKey())) {
                    if (!ComponentDiverter.class.isInstance(c)) {
                        System.err.println("Cannot set property direction_default for " + c.getClass().getName() + " in line " + line_number);
                    } else {
                        ComponentDiverter c_ = (ComponentDiverter) c;
                        try {
                            int direction_default = Arrays.asList(Component.DIRECTIONS).indexOf(entry.getValue().getAsString());
                            if (direction_default < 0) {
                                System.err.println("Invalid direction_default component property " + entry.getValue().getAsString() + " in line " + line_number);
                                errors++;
                            }
                            c_.direction_default = direction_default;
                        } catch (Exception ex) {
                            System.err.println("Invalid direction_default component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("direction".equals(entry.getKey())) {
                    if (!ComponentArrow.class.isInstance(c)) {
                        System.err.println("Cannot set property direction for " + c.getClass().getName() + " in line " + line_number);
                    } else {
                        ComponentArrow c_ = (ComponentArrow) c;
                        try {
                            int direction_default = Arrays.asList(Component.DIRECTIONS).indexOf(entry.getValue().getAsString());
                            if (direction_default < 0) {
                                System.err.println("Invalid direction component property " + entry.getValue().getAsString() + " in line " + line_number);
                                errors++;
                            }
                            c_.direction = direction_default;
                            c_.setDirection(direction_default); // This makes it compatible with intersections
                        } catch (Exception ex) {
                            System.err.println("Invalid direction component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("directions".equals(entry.getKey())) {
                    List<String> directions_map = Arrays.asList(Component.DIRECTIONS);
                    JsonArray directions_old = entry.getValue().getAsJsonArray();
                    int[] directions = new int[directions_old.size()];
                    try {
                        for (int i = 0; i < directions_old.size(); i++) {
                            directions[i] = directions_map.indexOf(directions_old.get(i).getAsString());
                            if (directions[i] < 0) {
                                directions[i] = 0;
                                System.err.println("Wrong direction " + directions[i] + " in line " + line_number);
                                errors++;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Invalid directions in line " + line_number);
                        errors++;
                    }
                    if (ComponentConditional.class.isInstance(c)) {
                        ComponentConditional c_ = (ComponentConditional) c;
                        try {
                            c_.directions = directions;
                        } catch (Exception ex) {
                            System.err.println("Invalid initial_direction component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }

                    } else if (ComponentIntersection.class.isInstance(c)) {
                        ComponentIntersection c_ = (ComponentIntersection) c;
                        try {
                            c_.directions = directions;
                        } catch (Exception ex) {
                            System.err.println("Invalid initial_direction component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }

                    } else {
                        System.err.println("Cannot set property directions for " + c.getClass().getName() + " in line " + line_number);
                        errors++;
                    }
                } else if ("painted".equals(entry.getKey())) {
                    if (!ComponentPainter.class.isInstance(c)) {
                        System.err.println("Cannot set property painted for " + c.getClass().getName() + " in line " + line_number);
                        errors++;
                    } else {
                        ComponentPainter c_ = (ComponentPainter) c;
                        JsonArray painted_old = entry.getValue().getAsJsonArray();
                        int[] painted = new int[painted_old.size()];
                        for (int i = 0; i < painted_old.size(); i++) {
                            painted[i] = painted_old.get(i).getAsInt();
                        }
                        try {
                            c_.painted = painted;
                        } catch (Exception ex) {
                            System.err.println("Invalid painted for painter component property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("accepted_types".equals(entry.getKey())) {
                    if (!ComponentDelivery.class.isInstance(c)) {
                        System.err.println("Cannot set property accepted_types for " + c.getClass().getName() + " in line " + line_number);
                        errors++;
                    } else {
                        ComponentDelivery c_ = (ComponentDelivery) c;
                        JsonArray accepted_old = entry.getValue().getAsJsonArray();
                        int[] accepted = new int[accepted_old.size()];
                        for (int i = 0; i < accepted_old.size(); i++) {
                            accepted[i] = Arrays.asList(ComponentPickup.PICKUP_TYPES).indexOf(accepted_old.get(i).getAsString());
                        }
                        try {
                            c_.accepted_types = accepted;
                        } catch (Exception ex) {
                            System.err.println("Invalid accepted_colors for property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("accepted_colors".equals(entry.getKey())) {
                    if (!ComponentDelivery.class.isInstance(c)) {
                        System.err.println("Cannot set property accepted_colors for " + c.getClass().getName() + " in line " + line_number);
                        errors++;
                    } else {
                        ComponentDelivery c_ = (ComponentDelivery) c;
                        JsonArray accepted_old = entry.getValue().getAsJsonArray();
                        int[] accepted = new int[accepted_old.size()];
                        for (int i = 0; i < accepted_old.size(); i++) {
                            accepted[i] = accepted_old.get(i).getAsInt();
                        }
                        try {
                            c_.accepted_colors = accepted;
                        } catch (Exception ex) {
                            System.err.println("Invalid accepted_colors for property " + entry.getValue().getAsString() + " in line " + line_number);
                            errors++;
                        }
                    }
                } else if ("directions_types".equals(entry.getKey())) {
                    if (!ComponentDiverter.class.isInstance(c)) {
                        System.err.println("Cannot set property directions_types for " + c.getClass().getName() + " in line " + line_number);
                        errors++;
                    } else {
                        ComponentDiverter c_ = (ComponentDiverter) c;
                        JsonArray directionValues = entry.getValue().getAsJsonArray();
                        if (directionValues.size() != 4) {
                            //System.err.println("directions_types property requires 4 directions in line " + line_number);
                            errors++;
                        } else {
                            for (int i = 0; i < 4; i++) {
                                JsonArray thisDirectionValues = directionValues.get(i).getAsJsonArray();
                                int[] accepted = new int[thisDirectionValues.size()];
                                for (int j = 0; j < thisDirectionValues.size(); j++) {
                                    accepted[j] = Arrays.asList(ComponentPickup.PICKUP_TYPES).indexOf(thisDirectionValues.get(j).getAsString());
                                }
                                c_.directions_types[i] = accepted;
                            }
                        }
                    }
                } else if ("directions_colors".equals(entry.getKey())) {
                    if (!ComponentDiverter.class.isInstance(c)) {
                        System.err.println("Cannot set property directions_colors for " + c.getClass().getName() + " in line " + line_number);
                        errors++;
                    } else {
                        ComponentDiverter c_ = (ComponentDiverter) c;
                        JsonArray directionValues = entry.getValue().getAsJsonArray();
                        if (directionValues.size() != 4) {
                            //System.err.println("directions_colors property requires 4 directions in line " + line_number);
                            errors++;
                        } else {
                            for (int i = 0; i < 4; i++) {
                                JsonArray thisDirectionValues = directionValues.get(i).getAsJsonArray();
                                int[] accepted = new int[thisDirectionValues.size()];
                                for (int j = 0; j < thisDirectionValues.size(); j++) {
                                    accepted[j] = thisDirectionValues.get(j).getAsInt();
                                }
                                c_.directions_colors[i] = accepted;
                            }
                        }
                    }

                } else {
                    // Set the rest as int
                    try {
                        c.getClass().getField(entry.getKey()).set(c, entry.getValue().getAsInt());
                    } catch (Exception ex) {
                        System.err.println("Cannot set property " + entry.getKey() + " for component " + c.getClass().getName() + " error: " + ex.getMessage());
                        errors++;
                    }
                }
            } else {
                System.err.println("Invalid component property " + entry.getKey() + " in line " + line_number);
                errors++;
            }
        }
        if (ComponentExchange.class.isInstance(c)) {
            ComponentExchange ce = (ComponentExchange) c;
            if (ce.link == -1) {
                System.err.println("No link for exchange component");
            }

        }
    }

    public static UnitState extractUnitsFromComponents(ComponentState components) {
        UnitState units = new UnitState();
        if(components!=null){
            for (Component c : components.getComponents()) {
                if (ComponentUnit.class.isInstance(c)) {
                    units.addUnit((ComponentUnit) c.clone());
                }
            }
        }
        return units;
    }
}
