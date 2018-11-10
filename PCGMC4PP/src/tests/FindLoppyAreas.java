/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import game.GameState;
import game.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author santi
 */
public class FindLoppyAreas {
    public static void findLoopyAreas(GameState gs)
    {
        int w = gs.getBoardState().getWidth();
        int h = gs.getBoardState().getHeight();
        boolean considered[][] = new boolean[w][h];
        boolean endlessloop[][] = new boolean[w][h];
        
        for(int i = 0;i<h;i++) {
            for(int j = 0;j<w;j++) {
                Tile t = gs.getBoardState().getTile(j, i);
                if (t != null && t.type == Tile.TILE_TRACK && !considered[j][i]) {
//                    System.out.println("nbs of " + j + "," + i + " ("+t.type+"): " + t.getNeighborsBitmask());
//                    System.out.println("    traveled_to: " + t.traveled_to);
                    List<Tile> pathForward = new ArrayList<>();
                    
                    // follow it forward until a fork or component:
                    // public static final char[] DIRECTIONS = new char[]{'X','<', 'V', '>', 'A', ' '};
                    Tile current = t;
                    while(true) {
                        considered[current.x][current.y] = true;
                        if (!current.component_index.isEmpty()) break;
                        if (pathForward.contains(current)) {
                            // loop!!!
                            System.out.println("Loop!!! " + pathForward);
                            for(Tile t2:pathForward) {
                                endlessloop[t2.x][t2.y] = true;
                            }
                            break;
                        }
                        pathForward.add(current);
                        if (current.traveled_to.size()==1) {
                            current = current.traveled_to.iterator().next();
                        } else {
                            break;
                        }
                    }
//                    System.out.println("    pathForward: " + pathForward.size());
                }
            }
        }
    }
}
