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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class BoardState {

    public int width;
    public int height;
    public Tile[] tiles;

    public int level_id = 1;
    public String level_title = "Parallel Programming";
    public String goal_string = "Deliver all packages";
    //public Map<String, List<GoalCondition>> goal_struct;
    public List<GoalCondition> goal_struct;
    public Map<String, Integer> player_palette;
    public int time_pickup_min = 0;
    public int time_pickup_max = 0;
    public int time_delivery_min = 3;
    public int time_delivery_max = 3;
    public int time_exchange_min = 1;
    public int time_exchange_max = 1;
    public int max_colors = 0; // 0 means only grey
    public float time_efficiency = 0.0f;

    public BoardState(int width, int height, Tile[] tiles) {
        this();
        this.width = width;
        this.height = height;
        this.tiles = tiles;
        
    }

    public BoardState(int width, int height) {
        this();
        this.setSize(width, height);
        
    }

    public BoardState() {
        this.player_palette = new LinkedHashMap();
        this.goal_struct = new ArrayList();
    }

    public BoardState setSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[x + y * width] = new Tile(x, y, x + y * width);
            }
        }
        return this;
    }

    public int getWidth() {
        return this.width;

    }

    public int getHeight() {
        return this.height;

    }

    public Tile getTile(int x, int y) {
        return (x + y * width >= 0 && x + y * width<tiles.length) ? tiles[x + y * width] : null;
    }

    public Tile getTile(int id) {
        return tiles[id];
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public void initTileNeighbors() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // North, South, West, East
                Tile tile = getTile(x, y);
                tile.neighbors.clear(); // Sanity check when parsing is invoking this twice
                if (tile.isPassable()) {
                    if (y > 0 && getTile(x, y - 1).isPassable()) {
                        tile.tile_bitmask |= 1 << 3;
                        tile.neighbors.add(getTile(x, y - 1));
                    }
                    if (y < height - 1 && getTile(x, y + 1).isPassable()) {
                        tile.tile_bitmask |= 1 << 1;
                        tile.neighbors.add(getTile(x, y + 1));
                    }
                    if (x > 0 && getTile(x - 1, y).isPassable()) {
                        tile.tile_bitmask |= 1 << 0;
                        tile.neighbors.add(getTile(x - 1, y));
                    }
                    if (x < width - 1 && getTile(x + 1, y).isPassable()) {
                        tile.tile_bitmask |= 1 << 2;
                        tile.neighbors.add(getTile(x + 1, y));
                    }
                }
            }
        }
    }

    public BoardState clone() {
        return new BoardState(width, height, tiles);
    }

    private int[] expandMergeTileColors(int[] colors1, int[] colors2) {
        if (colors1.length == 1 && colors1[0] == 0 || colors2.length == 1 && colors2[0] == 0) {
            return new int[]{0};
        } else {
            return colors1;
        }
    }

    public BoardState expand() {
        return expand(3, 3);
    }

    public BoardState expand(int ratio_x, int ratio_y) {
        BoardState board = new BoardState(this.width * ratio_x, this.height * ratio_y);
        int offset_x = ratio_x / 2 + 1 - 1;
        int offset_y = ratio_y / 2 + 1 - 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile current = board.getTile(offset_x + x * ratio_x, offset_y + y * ratio_y);
                current.copyPropertiesFrom(this.getTile(x, y));
                int colors = current.colors;
                if ((current.tile_bitmask & (1 << 3)) != 0) {
                    for (int i = 1; i < ratio_y / 2 + 1; i++) {
                        Tile interm = board.getTile(current.x, current.y - i);
                        interm.type = Tile.TILE_TRACK;
                        interm.colors |= colors;
                    }
                }
                if ((current.tile_bitmask & (1 << 1)) != 0) {
                    for (int i = 1; i < ratio_y / 2 + 1; i++) {
                        Tile interm = board.getTile(current.x, current.y + i);
                        interm.type = Tile.TILE_TRACK;
                        interm.colors |= colors;
                    }
                }
                if ((current.tile_bitmask & (1 << 0)) != 0) {
                    for (int i = 1; i < ratio_x / 2 + 1; i++) {
                        Tile interm = board.getTile(current.x - i, current.y);
                        interm.type = Tile.TILE_TRACK;
                        interm.colors |= colors;
                    }
                }
                if ((current.tile_bitmask & (1 << 2)) != 0) {
                    for (int i = 1; i < ratio_x / 2 + 1; i++) {
                        Tile interm = board.getTile(current.x + i, current.y);
                        interm.type = Tile.TILE_TRACK;
                        interm.colors |= colors;
                    }
                }
            }
        }
        board.initTileNeighbors();
        board.copyPropertiesFrom(this);
        return board;
    }
    public void copyPropertiesFrom(BoardState from){
        BoardState.copyProperties(this, from);
    }
    public static void copyProperties(BoardState to, BoardState from) {
        to.level_id = from.level_id;
        to.level_title = from.level_title;
        to.goal_string = from.goal_string;
        to.goal_struct = from.goal_struct;
        to.player_palette = from.player_palette;
        to.time_pickup_min = from.time_pickup_min;
        to.time_delivery_min = from.time_delivery_min;
        to.time_exchange_min = from.time_exchange_min;
        to.time_pickup_max = from.time_pickup_max;
        to.time_delivery_max = from.time_delivery_max;
        to.time_exchange_max = from.time_exchange_max;
        to.max_colors = from.max_colors;
    }

}
