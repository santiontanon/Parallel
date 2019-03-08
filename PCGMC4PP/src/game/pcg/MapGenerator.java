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
import game.Tile;
import game.UnitState;
import game.component.ComponentUnit;
import game.component.Component;
import game.component.ComponentIntersection;
import java.util.ArrayList;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class MapGenerator {

    
    public static GameState SmallestTest() {
        BoardState board = new BoardState(3,3);
        for(Tile tile:board.getTiles()){
            tile.type=Tile.TILE_TRACK;
        }
        board.initTileNeighbors();
        ComponentState components = new ComponentState();
        UnitState units = new UnitState();
        return new GameState(board,components,units, new ArrayList<String>());
    }

    public static GameState SmallerTest() {
        int size_x = 4;
        int size_y = 4;
        BoardState board = new BoardState(size_x,size_y);
        for(int x =0;x<size_x;x++){
            board.getTile(x,0).type = Tile.TILE_TRACK;
            board.getTile(x,size_y-1).type = Tile.TILE_TRACK;
            if(x<size_x-1){
                board.getTile(x,0).neighbors.add(board.getTile(x+1,0));
                board.getTile(size_x-x-1,size_y-1).neighbors.add(board.getTile(size_x-x-2,size_y-1));
            }
        }
        for(int y =0;y<size_y;y++){
            board.getTile(0,y).type = Tile.TILE_TRACK;
            board.getTile(size_x-1,y).type = Tile.TILE_TRACK;
            if(y<size_y-1){
                board.getTile(size_x-1,y).neighbors.add(board.getTile(size_x-1,y+1));
                board.getTile(0,size_y-y-1).neighbors.add(board.getTile(0,size_y-y-2));
            }

        }
        
        board.initTileNeighbors();
        ComponentState components = new ComponentState();
        UnitState units = new UnitState();
        units.getUnits().add(new ComponentUnit(0,0,0,0));
        return new GameState(board,components,units, new ArrayList<String>());
    }

    
    
    public static GameState SmallTest() {
        boolean expand = false;
        BoardState board = CreateSmallCrossWorld(11,11);
        if(expand) board = board.expand();
        ComponentState components = new ComponentState();
        UnitState units = CreateAlternatingUnits(expand ? 1 : 0,expand ? 1 : 0,6,2);
        return new GameState(board,components,units, new ArrayList<String>());
    }

    public static BoardState CreateSmallCrossWorld(int size_x, int size_y) {
        BoardState board = new BoardState(size_x,size_y);
        for (int y = 0; y < size_y; y++) {
            for (int x = 0; x < size_x; x++) {
                if (x == 0 || y == 0 || x == size_x - 1 || y == size_y - 1 || x == size_x / 2 || y == size_y / 2) {
                    board.getTile(x, y).type = Tile.TILE_TRACK;
                    if(x==0||y==0||x==size_x-1||y==size_y-1){
                        board.getTile(x, y).colors=5; //GB
                    } else {
                        board.getTile(x, y).colors=4; //B
                    }
                } else {
                    board.getTile(x, y).type = Tile.TILE_EMPTY;
                }
                if(x == size_x / 2 && y <= size_y / 2 || x <= size_x / 2 && y == size_y / 2){
                    board.getTile(x, y).colors=2; // G
                }
                if(x < size_x / 2 && y < size_y / 2 || x <= size_x / 2 && y ==0 / 2 || x ==0 && y <= size_y / 2){
                    board.getTile(x, y).colors=3; // RG
                }
                if(x == size_x / 2 && y == size_y / 2){
                    board.getTile(x, y).colors=7; // RGB
                }

            }
        }
        for (int i = 0; i < size_x-1; i++) {
            board.getTile(i, 0).neighbors.add(board.getTile(i+1, 0));
            board.getTile(i+1, size_y-1).neighbors.add(board.getTile(i, size_y-1));
        }
        for (int i = 0; i < size_y-1; i++) {
            board.getTile(size_x-1, i).neighbors.add(board.getTile(size_x-1, i+1));
            board.getTile(0, i+1).neighbors.add(board.getTile(0, i));
        }
        for (int i = 0; i < size_x/2; i++) {
            board.getTile(i+1, size_y/2).neighbors.add(board.getTile(i, size_y/2));
        }
        for (int i = 0; i < size_y/2; i++) {
            board.getTile(size_x/2, i).neighbors.add(board.getTile(size_y/2,i+1));
        }
        board.initTileNeighbors();
        return board;
    }
    public static UnitState CreateAlternatingUnits(int start_x, int start_y, int num,int colors){
        UnitState units = new UnitState();
        for(int i =0;i<num;i++){
            units.getUnits().add(new ComponentUnit(start_x+i,start_y,i,1+i%colors));
        }
        return units;
    }
    public static ComponentState CreateSingleDirection(){
        ComponentState components = new ComponentState();
        ComponentIntersection ci = new ComponentIntersection(0,0,0,0,Component.OWNER_SYSTEM,true);
        ci.setDirection(ComponentIntersection.EAST);
        components.getComponents().add(ci);
        
        return components;
    }
}
