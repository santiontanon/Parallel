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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class Tile {
    public static int TILE_TRACK = 1; // UNITY?????????
    public static int TILE_EMPTY = 2; // UNITY?????????
    
    public int id;
    public int x;
    public int y;
    public int type;
    public boolean overpass = false;
    public int colors;
    public List<Tile> neighbors;
    public Set<Tile> traveled_to;
    // traveled_to was initially used for generating directions before there was a directions section
    // currently the directions section information is stored in this.direction using the DIRECTIONS chars
    // when present, these will override the traveled_to for exports
    public boolean busy = false;
    public int tile_bitmask;
    public int direction = -1;
    public static final char[] DIRECTIONS = new char[]{'X','<', 'V', '>', 'A', ' '};
    
    public List<Integer> component_index = new ArrayList(); // Faster lookup for components

    public Tile(int _x, int _y, int _type, int _id, int _colors, int _direction) {
        this.x = _x;
        this.y = _y;
        this.type = _type;
        this.id = _id;
        this.neighbors = new ArrayList<Tile>();  // UNITY List<>
        this.traveled_to = new HashSet<Tile>();  // UNITY List<>
        this.colors = _colors;
        this.tile_bitmask = 0;
        this.direction = _direction;
    }
    public Tile(int _x, int _y, int _id) {
        this.x = _x;
        this.y = _y;
        this.type = Tile.TILE_EMPTY;
        this.id = _id;
        this.neighbors = new ArrayList<Tile>();  // UNITY List<>
        this.traveled_to = new HashSet<Tile>();  // UNITY List<>
        this.colors = 0;
        this.tile_bitmask = 0;
    }
    
    public void copyPropertiesFrom(Tile tile){
        this.type = tile.type;
        this.tile_bitmask = tile.tile_bitmask;
        this.colors = tile.colors;
    }
    
    public boolean isPassable(int color_query){
        if(this.type==Tile.TILE_TRACK){
            if(this.colors==0) return true;
            if((this.colors&(1<<(color_query-1)))!=0) return true;
        }
        return false;
    }
    public boolean isPassable(){
        return this.type==Tile.TILE_TRACK;
    }
    
    public boolean isWaypoint(){
        if(this.tile_bitmask==5 // horizontal
                || this.tile_bitmask==10){ // vertical
            return false;
        }
        return true;
    }
    
    public int getTileBitmask(){
        return this.tile_bitmask;
    }
    public void setTileBitmask(int bitmask){
        this.tile_bitmask = bitmask;
    }
    
    public int getNeighborsBitmask(){
        int mask = 0;
        // North, South, West, East
        for(Tile neighbor : this.neighbors){
            if (neighbor.y<this.y) mask |= 1<<3;
            else if (neighbor.y>this.y) mask |= 1<<1;
            else if (neighbor.x<this.x) mask |= 1<<0;
            else if (neighbor.x>this.x) mask |= 1<<2;
        }
        return mask;
    }
    public int getColorsBitmask(){
        return this.colors;
    }
    public int getDirectionTo(Tile tile){
        //public static final String[] DIRECTIONS = new String[]{"West", "South", "East", "North"};
        if(tile.x<this.x){
            return 0;
        } else if (tile.x>this.x){
            return 2;
        } else if (tile.y<this.y){
            return 3;
        } else if (tile.y>this.y){
            return 1;
        } else {
            return -1;
        }
    }
    public String toString(){
        return "Tile("+this.x+','+this.y+")";
    }

}
