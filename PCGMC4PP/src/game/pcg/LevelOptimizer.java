/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game.pcg;

import game.BoardState;
import game.GameState;
import game.Tile;
import game.component.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import valls.util.Pair;

/**
 *
 * @author santi
 */
public class LevelOptimizer {
    public static final int MIN_SEPARATION = 4;
    
    static int dx[] = {-1, 0,1,0};
    static int dy[] = { 0,-1,0,1};
    
    public static GameState optimize(GameState gs) {
        gs = optimize(gs, true, true, true);  // try moving elements to the left and shorten paths
        gs = optimize(gs, false, false, true);    // now try moving them to the right
        gs = clearEmptyBorderSpaces(gs);
        gs.init();
        return gs;
    }
    
    public static GameState optimize(GameState gs, boolean moveToLeft, boolean shortenPaths, boolean tryToRemoveColumns) {
        // step 1: try to align elements in the same rows/columns (this will enable further optimizations)
        int w = gs.bs.getWidth();
        int h = gs.bs.getHeight();
        
        List<Component> alreadyHandled = new ArrayList<>();
        List<Component> allComponents = new ArrayList<>();
        allComponents.addAll(gs.getComponentState().getComponents());
        allComponents.addAll(gs.getUnitState().getUnits());
        
        for(Component u:allComponents) {
            if (alreadyHandled.contains(u)) continue;
            
            // identify the path in which this component is, an get all the components in it, in order:
            List<Tile> path = new ArrayList<>();
            
            List<Tile> open = new ArrayList<>();
            open.add(gs.getBoardState().getTile(u.x, u.y));
            while(!open.isEmpty()) {
                Tile current = open.remove(0);
                path.add(current);
                int x = current.x;
                int y = current.y;
                List<Tile> neighbors = new ArrayList<>();
                for(int i = 0;i<4;i++) {
                    if (x+dx[i]<0 || x+dx[i]>=w || 
                        y+dy[i]<0 || y+dy[i]>=h) continue;
                    Tile t = gs.getBoardState().getTile(x+dx[i], y+dy[i]);
                    if (t!=null && t.type == Tile.TILE_TRACK) neighbors.add(t);
                }
                if (neighbors.size() <= 2) {
                    // we are still on a path:
                    for(Tile t:neighbors) {
                        if (!open.contains(t) && !path.contains(t)) open.add(t);
                    }
                }
            }
            
            if (path.size()<3) continue; 
            
            // sort the path: 
            List<Tile> sortedPath = new ArrayList<>();
            sortedPath.add(path.get(0));
            path.remove(0);
            
            // first pass to the right:
            Tile current = sortedPath.get(0);
            Tile next = null;
            do {
                next = null;
                for(Tile t:path) {
                    if (Math.abs(t.x-current.x)+Math.abs(t.y-current.y) == 1) {
                        next = t;
                        break;
                    }
                }
                if (next != null) {
                    sortedPath.add(next);
                    path.remove(next);  
                    current = next;
                }
            }while(next!=null);

            // second pass to the left:
            current = sortedPath.get(0);
            do {
                next = null;
                for(Tile t:path) {
                    if (Math.abs(t.x-current.x)+Math.abs(t.y-current.y) == 1) {
                        next = t;
                        break;
                    }
                }
                if (next != null) {
                    sortedPath.add(0, next);
                    path.remove(next);                    
                    current = next;
                }
            }while(next!=null);
            if (!path.isEmpty()) System.err.println("some tiles leftover after path sorting!: " + path.size());
            path = sortedPath;
                        
            if (path.get(0).x > path.get(path.size()-1).x) {
                // reverse path:
                List<Tile> path2 = new ArrayList<>();
                for(Tile t:path) {
                    path2.add(0, t);
                }
                path = path2;
            }
            
            // remove the two extremes, as they are not useable (but remember them):
            Tile leftExtreme = path.remove(0);
            Tile rightExtreme = path.remove(path.size()-1);

            // find all the components along it:
            List<Component> pathComponents = new ArrayList<>();
            List<Integer> pathComponentsPosition = new ArrayList<>();
            int previousPosition = -1;
            int leeway = 0;
            for(Tile t:path) {
                Component c = gs.cs.getComponentByPosition(t.x, t.y);
                if (c != null) pathComponents.add(c);
                pathComponents.addAll(gs.us.getUnitsByPosition(t.x, t.y));
                while(pathComponentsPosition.size() < pathComponents.size()) {
                    int position = path.indexOf(t);
                    pathComponentsPosition.add(position);
                    int step = position - previousPosition;
                    if (step > MIN_SEPARATION) leeway += step - MIN_SEPARATION;
                    previousPosition = position;
                }
            }
            int step = path.size() - previousPosition;
            if (step > MIN_SEPARATION) leeway += step - MIN_SEPARATION;
            int minimumPathSize = path.size() - leeway;
            System.out.println("This path is length: " + path.size() + ", but could be compressed to " + minimumPathSize);
            
            /*
            // show the path (with components):
            System.out.println("path:");
            for(Tile t:path) {
                Component found = null;
                for(Component c:pathComponents) {
                    if (c.x == t.x && c.y == t.y) found = c;
                }
                if (found == null) {
                    System.out.println(t.x + ", " + t.y);
                } else {
                    System.out.println(t.x + ", " + t.y + " - " + found);
                }   
            }
            */

            // mark components as handled:
            alreadyHandled.addAll(pathComponents);
            
            // optimization step 1: replace path with shortest path (with minimum number of bends)
            if (shortenPaths) {
                List<Tile> shorterPath = findShorterPath(gs.bs, path, pathComponents, pathComponentsPosition);
                // If components still fit, place them in the new path, and replace the old one!
                if (shorterPath.size() < path.size() && shorterPath.size() >= minimumPathSize) {
                    System.out.println("Found a better acceptable path, replacing!!");

                    // remove previous path:
                    for(Tile t:path) {
                        gs.bs.tiles[t.x + t.y*w] = new Tile(t.x, t.y, t.id);
                    }

                    // add new path:
                    for(int i = 0;i<shorterPath.size();i++) {
                        // tiles:
                        Tile shorterT = shorterPath.get(i);
                        Tile oldT = path.get(i);
                        gs.bs.tiles[shorterT.x + shorterT.y*w] = oldT;
                        oldT.x = shorterT.x;
                        oldT.y = shorterT.y;

                        if (i == shorterPath.size()-1) {
                            if (oldT.traveled_to.contains(path.get(i+1))) {
                                oldT.traveled_to.remove(path.get(i+1));
                                oldT.traveled_to.add(rightExtreme);
                            }
                        }
                    }

                    gs.bs.initTileNeighbors();

                    path = shorterPath;
                }
            }

            // optimization step 2:
            // move all the components to the left/right as much as possible, leaving their gaps, if they are smaller than MIN_SPARATION steps:
            if (moveToLeft) {
                previousPosition = -1;
                for(int i = 0;i<pathComponents.size();i++) {
                    int gap = pathComponentsPosition.get(i) - previousPosition;
                    if (gap > MIN_SEPARATION) {
                        // move it closer to the left!
                        int newPosition = previousPosition + MIN_SEPARATION;
                        pathComponentsPosition.set(i, newPosition);
                        pathComponents.get(i).x = path.get(newPosition).x;
                        pathComponents.get(i).y = path.get(newPosition).y;
                    } else {
                        // leave it where it is:
                        int newPosition = previousPosition + gap;
                        pathComponentsPosition.set(i, newPosition);
                        pathComponents.get(i).x = path.get(newPosition).x;
                        pathComponents.get(i).y = path.get(newPosition).y;
                    }
                    previousPosition = pathComponentsPosition.get(i);
                }
            } else {
                previousPosition = path.size();
                for(int i = pathComponents.size()-1;i>=0;i--) {
                    int gap = previousPosition - pathComponentsPosition.get(i);
                    if (gap > MIN_SEPARATION) {
                        // move it closer to the left!
                        int newPosition = previousPosition - MIN_SEPARATION;
                        pathComponentsPosition.set(i, newPosition);
                        pathComponents.get(i).x = path.get(newPosition).x;
                        pathComponents.get(i).y = path.get(newPosition).y;
                    } else {
                        // leave it where it is:
                        int newPosition = previousPosition - gap;
                        pathComponentsPosition.set(i, newPosition);
                        pathComponents.get(i).x = path.get(newPosition).x;
                        pathComponents.get(i).y = path.get(newPosition).y;
                    }
                    previousPosition = pathComponentsPosition.get(i);
                }
            }                
        }
        
        // optimization step 3: remove rows/columns that are empty
        if (tryToRemoveColumns) {
            boolean doneRemovingColumns;
            do{
                doneRemovingColumns = true;
                List<Integer> emptyColumns = new ArrayList<>();
                for(int x = 0;x<w;x++) {
                    boolean isEmpty = true;
                    for(Component c:allComponents) {
                        if (c.x == x) {
                            isEmpty = false;
                            break;
                        }
                    }

                    // detect vertical paths:
                    for(int y = 0;y<h-1;y++) {
                        Tile t1 = gs.bs.getTile(x, y);
                        Tile t2 = gs.bs.getTile(x, y+1);
                        if (t1.type == Tile.TILE_TRACK && t2.type == Tile.TILE_TRACK) {
                            isEmpty = false;
                            break;
                        }
                    }

                    if (isEmpty) emptyColumns.add(x);
                }

                // detect gaps larger than MIN_SEPARATION-1:
                int previous = -1;
                int consecutive = 0;
                for(int column:emptyColumns) {
                    if (column != previous+1) {
                        if (consecutive >= MIN_SEPARATION) {
                            System.out.println("Remove consecutive columns " + ((consecutive-MIN_SEPARATION)+1) + " at " + previous);

                            int nColumnsToRemove = (consecutive-MIN_SEPARATION)+1;
                            int startIndex = previous - nColumnsToRemove;

                            // move components:
                            for(Component c:allComponents) {
                                if (c.x>=startIndex) c.x -= nColumnsToRemove;
                            }

                            // move tiles:
                            int new_w = w-nColumnsToRemove;
                            BoardState bs = new BoardState(new_w, h);
                            for(int x = 0;x<new_w;x++) {
                                for(int y = 0;y<h;y++) {
                                    if (x<startIndex) {
                                        bs.tiles[x+y*new_w] = gs.getBoardState().tiles[x+y*w];
                                    } else {
                                        bs.tiles[x+y*new_w] = gs.getBoardState().tiles[x+nColumnsToRemove+y*w];
                                    }
                                }
                            }
                            bs.initTileNeighbors(); // fix neighbors
                            for(int x = 0;x<new_w;x++) {
                                for(int y = 0;y<h;y++) {
                                    Tile t = bs.tiles[x+y*new_w];
                                    Set<Tile> new_traveled_to = new HashSet<>();
                                    for(Tile t2:t.traveled_to) {
                                        if (t2.x<startIndex) {
                                            new_traveled_to.add(gs.getBoardState().tiles[t2.x+t2.y*w]);
                                        } else {
                                            new_traveled_to.add(gs.getBoardState().tiles[t2.x+nColumnsToRemove+t2.y*w]);
                                        }                                    
                                    }
                                    t.traveled_to = new_traveled_to;
                                }
                            }
                            for(int x = 0;x<new_w;x++) {
                                for(int y = 0;y<h;y++) {
                                    bs.tiles[x+y*new_w].x = x;
                                    bs.tiles[x+y*new_w].y = y;
                                    bs.tiles[x+y*new_w].id = x+y*new_w;
                                }
                            }
                            gs.bs = bs;
                            w = new_w;

                            doneRemovingColumns = false;
                            break;
                        }
                        consecutive = 0;
                    }
                    previous = column;
                    consecutive++;
                }
            }while(!doneRemovingColumns);
        }

        return gs;
    }
    
    
    public static GameState clearEmptyBorderSpaces(GameState gs)
    {
        int empty_columns_left = 0;
        int empty_columns_right = 0;
        int empty_rows_up = 0;
        int empty_rows_down = 0;
        
        // 1) find empty columns on the left:
        for(int x = 0;x<gs.bs.getWidth();x++) {
            boolean empty = true;
            for(int y = 0;y<gs.bs.getHeight();y++) {
                if (gs.bs.getTile(x, y).type == Tile.TILE_TRACK) {
                    empty = false;
                    break;
                }
            }
            if (!empty) {
                empty_columns_left = x;
                break;
            }
        }
        // 1) find empty columns on the right:
        for(int x = gs.bs.getWidth()-1;x>=0;x--) {
            boolean empty = true;
            for(int y = 0;y<gs.bs.getHeight();y++) {
                if (gs.bs.getTile(x, y).type == Tile.TILE_TRACK) {
                    empty = false;
                    break;
                }
            }
            if (!empty) {
                empty_columns_right = (gs.bs.getWidth()-1)-x;
                break;
            }
        }
        
        // 1) find empty columns on the left:
        for(int y = 0;y<gs.bs.getHeight();y++) {
            boolean empty = true;
            for(int x = 0;x<gs.bs.getWidth();x++) {
                if (gs.bs.getTile(x, y).type == Tile.TILE_TRACK) {
                    empty = false;
                    break;
                }
            }
            if (!empty) {
                empty_rows_up = y;
                break;
            }
        }
        // 1) find empty columns on the right:
        for(int y = gs.bs.getHeight()-1;y>=0;y--) {
            boolean empty = true;
            for(int x = 0;x<gs.bs.getWidth();x++) {
                if (gs.bs.getTile(x, y).type == Tile.TILE_TRACK) {
                    empty = false;
                    break;
                }
            }
            if (!empty) {
                empty_rows_down = (gs.bs.getHeight()-1)-y;
                break;
            }
        }
             
        System.out.println("empty_columns: " + empty_columns_left + ", " + empty_columns_right);
        System.out.println("empty_rows: " + empty_rows_up + ", " + empty_rows_down);
        
        // shift everything up/left:
        for(Component c:gs.cs.getComponents()) {
            c.x -= empty_columns_left;
            c.y -= empty_rows_up;
        }
        for(Component c:gs.us.getUnits()) {
            c.x -= empty_columns_left;
            c.y -= empty_rows_up;
        }
        for(int x = 0;x<gs.bs.getWidth();x++) {
            int x2 = x+empty_columns_left;
            for(int y = 0;y<gs.bs.getHeight();y++) {
                int y2 = y+empty_rows_up;
                if (x2<gs.bs.getWidth() && y2<=gs.bs.getHeight()) {
                    gs.bs.tiles[x+y*gs.bs.getWidth()] = gs.bs.tiles[x2+y2*gs.bs.getWidth()];
                    gs.bs.tiles[x+y*gs.bs.getWidth()].x = x;
                    gs.bs.tiles[x+y*gs.bs.getWidth()].y = y;
                    gs.bs.tiles[x+y*gs.bs.getWidth()].id = x+y*gs.bs.getWidth();
                } else {
                    gs.bs.tiles[x+y*gs.bs.getWidth()] = new Tile(x, y, x+y*gs.bs.getWidth());
                }
            }
        }
        
        // remove extra tiles:
        int nw = gs.bs.getWidth() - (empty_columns_left + empty_columns_right);
        int nh = gs.bs.getHeight() - (empty_rows_up + empty_rows_down);
        Tile nt[] = new Tile[nw*nh];
        for(int x = 0;x<nw;x++) {
            for(int y = 0;y<nh;y++) {       
                nt[x+y*nw] = gs.bs.tiles[x+y*gs.bs.getWidth()];
                nt[x+y*nw].id = x+y*nw;
            }
        }
        gs.bs.width = nw;
        gs.bs.height = nh;
        gs.bs.tiles = nt;
        gs.bs.initTileNeighbors();
        return gs;
    }
    

    private static List<Tile> findShorterPath(BoardState bs, List<Tile> path, List<Component> pathComponents, List<Integer> pathComponentsPosition) {
        List<Tile> shorterPath = new ArrayList<>();
        int w = bs.getWidth();
        int h = bs.getHeight();
        int map[][] = new int[w][h];
        for(int i = 0;i<h;i++) {
            for(int j = 0;j<w;j++) {
                map[j][i] = 0;
            }
        }
        for(int i = 0;i<h;i++) {
            for(int j = 0;j<w;j++) {
                if (bs.getTile(j, i).type == Tile.TILE_TRACK &&
                    !path.contains(bs.getTile(j, i))) {
                    map[j][i] = -1;
                    if (j>0) map[j-1][i] = -1;
                    if (i>0) map[j][i-1] = -1;
                    if (j<w-1) map[j+1][i] = -1;
                    if (i<h-1) map[j][i+1] = -1;
                }
            }
        }
        
        Pair<Integer,Integer> start = new Pair<>(path.get(0).x, path.get(0).y);
        int end_x = path.get(path.size()-1).x;
        int end_y = path.get(path.size()-1).y;
        map[end_x][end_y] = 0;
                
        // shortest path (BFS):
        List<Pair<Integer,Integer>> open = new ArrayList<>();
        open.add(start);
        map[start.m_a][start.m_b] = -2;
        while(!open.isEmpty()) {
            Pair<Integer,Integer> current = open.remove(0);

            if (current.m_a == end_x && current.m_b == end_y) {
                // we reached the goal!
                open.clear();
                break;
            }
            
            // prioritize the direction from where we come, to minimize the number of bends in the path:
            int tmp = map[current.m_a][current.m_b];
            if (tmp != -2) {
                int parentx = (tmp-1)%w;
                int parenty = (tmp-1)/w;
                int nx = current.m_a + (current.m_a - parentx);
                int ny = current.m_b + (current.m_b - parenty);
                if (nx>=0 && ny>=0 && nx<w && ny<h && map[nx][ny] == 0) {
                    open.add(new Pair<>(nx, ny));
                    map[nx][ny] = (current.m_a + current.m_b*w)+1;
                }
            }
            
            for(int i = 0;i<4;i++) {
                int nx = current.m_a + dx[i];
                int ny = current.m_b + dy[i];
                                
                if (nx>=0 && ny>=0 && nx<w && ny<h && map[nx][ny] == 0) {
                    open.add(new Pair<>(nx, ny));
                    map[nx][ny] = (current.m_a + current.m_b*w)+1;
                }
            }
        }
        
        // reconstruct the path:
        Tile current = new Tile(end_x, end_y, bs.getTile(end_x, end_y).id);
        shorterPath.add(current);
        while(map[current.x][current.y] > 0) {
            int next = map[current.x][current.y]-1;
            map[current.x][current.y] = -2;
            int next_x = next%w;
            int next_y = next/w;
            current = new Tile(next_x, next_y, bs.getTile(next_x, next_y).id);
            shorterPath.add(current);
        }
        
        for(int i = 0;i<h;i++) {
            for(int j = 0;j<w;j++) {
                if (map[j][i] == 0) {
                    if (j == start.m_a && i == start.m_b) {
                        System.out.print("S");
                    } else if (j == end_x && i == end_y) {
                        System.out.print("E");
                    } else {
                        System.out.print(".");
                    }
                } else {
                    if (j == start.m_a && i == start.m_b) {
                        System.out.print("S");
                    } else if (j == end_x && i == end_y) {
                        System.out.print("E");
                    } else if (map[j][i] == -2) {
                        System.out.print("*");
                    } else if (map[j][i] == -1) {
                        System.out.print("X");
                    } else {
                        System.out.print(".");
                    }
                }
            }
            System.out.println("");
        }
        
        System.out.println("findShorterPath: " + path.size() + " -> " + shorterPath.size());
        
        return shorterPath;
    }
    
}
