/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import game.BoardState;
import game.component.Component;
import game.component.ComponentDelivery;
import game.component.ComponentPickup;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JPanel;
import game.GameState;
import game.Tile;
import game.component.ComponentConditional;
import game.component.ComponentDiverter;
import game.component.ComponentExchange;
import game.component.ComponentIntersection;
import game.component.ComponentSemaphore;
import game.component.ComponentSignal;
import game.component.ComponentUnit;
import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.Random;
import valls.util.Pair;

/**
 *
 * @author santi
 */
public class BoardGameStatePanel extends JPanel {

    public static int DISPLAY_TILES = 1;
    public static int DISPLAY_COMPONENTS = 2;
    public static int DISPLAY_REDUCED_STATE = 3;
    public static int DISPLAY_LINKS = 4;
    public static int DISPLAY_TILES_AND_LINKS = 5;
    public static Color[] colors = {Color.LIGHT_GRAY, new Color(255, 128, 128), new Color(128, 255, 128), new Color(128, 128, 255), Color.YELLOW, Color.ORANGE, Color.PINK, Color.MAGENTA, Color.CYAN, Color.CYAN};
    public static Color[] colors_bold = {Color.GRAY, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE, Color.PINK, Color.MAGENTA, Color.CYAN, Color.CYAN};

    GameState gs = null;
    BoardState board = null;

    // Units to be highlighted (this is used, for example, by the MouseController, 
    // to give feedback to the human, on which units are selectable.
    List<ComponentUnit> toHighLight = new ArrayList<ComponentUnit>();

    // Coordinates where things were drawn the last time this was redrawn:
    int last_start_x = 0;
    int last_start_y = 0;
    int last_grid = 0;

    int displayLayer = DISPLAY_TILES_AND_LINKS;

    public BoardGameStatePanel(GameState a_gs) {
        gs = a_gs;
        board = gs.getBoardState();
        if (displayLayer == DISPLAY_TILES ||
            displayLayer == DISPLAY_TILES_AND_LINKS) {
            setBackground(Color.BLACK);
        } else {
            setBackground(Color.WHITE);
        }
    }

    public static BoardGameStateJFrame newVisualizer(GameState a_gs, int dx, int dy) {
        BoardGameStatePanel ad = new BoardGameStatePanel(a_gs);
        BoardGameStateJFrame frame = null;
        frame = new BoardGameStateJFrame("Game State Visualizer", dx, dy, ad);
        return frame;
    }

    public void setDisplayLayer(int cs) {
        displayLayer = cs;
        if (displayLayer == DISPLAY_TILES ||
            displayLayer == DISPLAY_TILES_AND_LINKS) {
            setBackground(Color.BLACK);
        } else {
            setBackground(Color.WHITE);
        }
    }

    public int getDisplayLayer() {
        return displayLayer;
    }

    public void setState(GameState a_gs) {
        gs = a_gs;
        board = gs.getBoardState();
    }

    public GameState getState() {
        return gs;
    }

    public void clearHighlights() {
        toHighLight.clear();
    }

    public void highlight(ComponentUnit u) {
        toHighLight.add(u);
    }

    public Pair<Integer, Integer> getContentAtCoordinates(int x, int y) {
        // return the map coordiantes over which the coordinates are:
        // System.out.println(x + ", " + y + " -> last start: " + last_start_x + ", " + last_start_y);
        if (x < last_start_x) {
            return null;
        }
        if (y < last_start_y) {
            return null;
        }

        int cellx = (x - last_start_x) / last_grid;
        int celly = (y - last_start_y) / last_grid;

        if (cellx >= board.getWidth()) {
            return null;
        }
        if (celly >= board.getHeight()) {
            return null;
        }

        return new Pair<Integer, Integer>(cellx, celly);
    }

    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        synchronized (this) {
            draw(g2d, this, this.getWidth(), this.getHeight(), gs, board, displayLayer);
        }
    }
    
    
    public static void drawDirectionArrows(Tile tile, Graphics2D g2d, int grid) {
        // put this back when viewing the compressed state
        int reduction = 4;
        int center_x = tile.x;
        int center_y = tile.y;
        int required_lines = 0;
        // lines clockwise 
        //  1  /\ 2
        //  4  \/ 8
        for (Tile neighbor : tile.neighbors) {
            if (neighbor.type == Tile.TILE_EMPTY) {
                continue;
            }
            if (tile.direction >= 0) {
                if (neighbor.x < tile.x && (tile.direction == 1 || tile.direction == 0)) {
                    required_lines |= 1;
                    required_lines |= 4;
                } else if (neighbor.x > tile.x && (tile.direction == 3 || tile.direction == 0)) {
                    required_lines |= 2;
                    required_lines |= 8;
                } else if (neighbor.y < tile.y && (tile.direction == 4 || tile.direction == 0)) {
                    required_lines |= 1;
                    required_lines |= 2;
                } else if (neighbor.y > tile.y && (tile.direction == 2 || tile.direction == 0)) {
                    required_lines |= 4;
                    required_lines |= 8;
                }
            } else if (tile.traveled_to.contains(neighbor)) {
                if (neighbor.x < tile.x) {
                    required_lines |= 1;
                    required_lines |= 4;
                } else if (neighbor.x > tile.x) {
                    required_lines |= 2;
                    required_lines |= 8;
                } else if (neighbor.y < tile.y) {
                    required_lines |= 1;
                    required_lines |= 2;
                } else if (neighbor.y > tile.y) {
                    required_lines |= 4;
                    required_lines |= 8;
                }
            }
        }
        //System.out.println(tile.neighbors.size()+" "+required_lines);
        int sx, sy, nx, ny, wx, wy, ex, ey;
        nx = tile.x * grid + grid / 2;
        sx = nx;
        ny = tile.y * grid;
        sy = ny + grid;
        wx = tile.x * grid;
        ex = wx + grid;
        wy = tile.y * grid + grid / 2;
        ey = wy;
        g2d.setColor(Color.GRAY);
        if ((required_lines & 1) == 1) {
            g2d.drawLine(wx, wy, nx, ny);
        }
        if ((required_lines & 2) == 2) {
            g2d.drawLine(ex, ey, nx, ny);
        }
        if ((required_lines & 4) == 4) {
            g2d.drawLine(wx, wy, sx, sy);
        }
        if ((required_lines & 8) == 8) {
            g2d.drawLine(ex, ey, sx, sy);
        }
    }


    public static void drawArrows(Tile tile, Graphics2D g2d, int grid) {
        // put this back when viewing the compressed state
        int reduction = 4;
        int center_x = tile.x;
        int center_y = tile.y;
        int required_lines = 0;
        // lines clockwise 
        //  1  /\ 2
        //  4  \/ 8
        for (Tile neighbor : tile.neighbors) {
            if (neighbor.type == Tile.TILE_EMPTY) {
                continue;
            }
            if (neighbor.x < tile.x) {
                required_lines |= 1;
                required_lines |= 4;
            } else if (neighbor.x > tile.x) {
                required_lines |= 2;
                required_lines |= 8;
            } else if (neighbor.y < tile.y) {
                required_lines |= 1;
                required_lines |= 2;
            } else if (neighbor.y > tile.y) {
                required_lines |= 4;
                required_lines |= 8;
            }
        }
        //System.out.println(tile.neighbors.size()+" "+required_lines);
        int sx, sy, nx, ny, wx, wy, ex, ey;
        nx = tile.x * grid + grid / 2;
        sx = nx;
        ny = tile.y * grid;
        sy = ny + grid;
        wx = tile.x * grid;
        ex = wx + grid;
        wy = tile.y * grid + grid / 2;
        ey = wy;
        g2d.setColor(Color.GRAY);
        if ((required_lines & 1) == 1) {
            g2d.drawLine(wx, wy, nx, ny);
        }
        if ((required_lines & 2) == 2) {
            g2d.drawLine(ex, ey, nx, ny);
        }
        if ((required_lines & 4) == 4) {
            g2d.drawLine(wx, wy, sx, sy);
        }
        if ((required_lines & 8) == 8) {
            g2d.drawLine(ex, ey, sx, sy);
        }
    }

    public static void drawArrowsConditional(ComponentConditional ci, Graphics2D g2d, int grid) {
        int reduction = 4;
        int center_x = ci.x;
        int center_y = ci.y;
        int required_lines = 0;
        // lines clockwise 
        //  1  /\ 2
        //  4  \/ 8
        int direction = ci.directions[ci.current];
        switch (direction) {
            case Component.WEST:
                required_lines |= 1;
                required_lines |= 4;
                break;
            case Component.EAST:
                required_lines |= 2;
                required_lines |= 8;
                break;
            case Component.NORTH:
                required_lines |= 1;
                required_lines |= 2;
                break;
            case Component.SOUTH:
                required_lines |= 4;
                required_lines |= 8;
                break;
        }
        int sx, sy, nx, ny, wx, wy, ex, ey;
        nx = ci.x * grid + grid / 2;
        sx = nx;
        ny = ci.y * grid;
        sy = ny + grid;
        wx = ci.x * grid;
        ex = wx + grid;
        wy = ci.y * grid + grid / 2;
        ey = wy;
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(4));
        if ((required_lines & 1) == 1) {
            g2d.drawLine(wx, wy, nx, ny);
        }
        if ((required_lines & 2) == 2) {
            g2d.drawLine(ex, ey, nx, ny);
        }
        if ((required_lines & 4) == 4) {
            g2d.drawLine(wx, wy, sx, sy);
        }
        if ((required_lines & 8) == 8) {
            g2d.drawLine(ex, ey, sx, sy);
        }
        g2d.setStroke(new BasicStroke(1));
    }
    
    
    public static void drawArrowsDiverter(ComponentDiverter div, Graphics2D g2d, int grid, Color color) {
        int reduction = 4;
        int center_x = div.x;
        int center_y = div.y;
        int required_lines = 0;
//        int required_lines = 15;
        // lines clockwise 
        //  1  /\ 2
        //  4  \/ 8
        for(int direction = 0;direction<4;direction++) {
            if (div.directions_types[direction].length == 0 &&
                div.directions_colors[direction].length == 0) continue;
            switch (direction) {
                case Component.WEST:
                    required_lines |= 1;
                    required_lines |= 4;
                    break;
                case Component.EAST:
                    required_lines |= 2;
                    required_lines |= 8;
                    break;
                case Component.NORTH:
                    required_lines |= 1;
                    required_lines |= 2;
                    break;
                case Component.SOUTH:
                    required_lines |= 4;
                    required_lines |= 8;
                    break;
            }        
        }
        int sx, sy, nx, ny, wx, wy, ex, ey;
        nx = div.x * grid + grid / 2;
        sx = nx;
        ny = div.y * grid;
        sy = ny + grid;
        wx = div.x * grid;
        ex = wx + grid;
        wy = div.y * grid + grid / 2;
        ey = wy;
        g2d.setColor(color);
        if ((required_lines & 1) == 1) {
            g2d.drawLine(wx, wy, nx, ny);
        }
        if ((required_lines & 2) == 2) {
            g2d.drawLine(ex, ey, nx, ny);
        }
        if ((required_lines & 4) == 4) {
            g2d.drawLine(wx, wy, sx, sy);
        }
        if ((required_lines & 8) == 8) {
            g2d.drawLine(ex, ey, sx, sy);
        }
    }    

    
    public static void drawArrowsIntersection(Component tile, Graphics2D g2d, int grid, Color color) {
        int reduction = 4;
        int center_x = tile.x;
        int center_y = tile.y;
        int required_lines = 15;
        // lines clockwise 
        //  1  /\ 2
        //  4  \/ 8
        int sx, sy, nx, ny, wx, wy, ex, ey;
        nx = tile.x * grid + grid / 2;
        sx = nx;
        ny = tile.y * grid;
        sy = ny + grid;
        wx = tile.x * grid;
        ex = wx + grid;
        wy = tile.y * grid + grid / 2;
        ey = wy;
        g2d.setColor(color);
        if ((required_lines & 1) == 1) {
            g2d.drawLine(wx, wy, nx, ny);
        }
        if ((required_lines & 2) == 2) {
            g2d.drawLine(ex, ey, nx, ny);
        }
        if ((required_lines & 4) == 4) {
            g2d.drawLine(wx, wy, sx, sy);
        }
        if ((required_lines & 8) == 8) {
            g2d.drawLine(ex, ey, sx, sy);
        }
    }

    public static void draw(Graphics2D g2d,
            BoardGameStatePanel panel,
            int dx, int dy,
            GameState gs,
            BoardState board,
            int colorScheme) {
        if (board == null) {
            return;
        }
        int gridx = (dx - 64) / board.getWidth();
        int gridy = (dy - 64) / board.getHeight();
        int grid = Math.min(gridx, gridy);
        int sizex = grid * board.getWidth();
        int sizey = grid * board.getHeight();

        if (colorScheme == DISPLAY_TILES) {
            g2d.setColor(Color.WHITE);
        }
        if (colorScheme == DISPLAY_COMPONENTS) {
            g2d.setColor(Color.BLACK);
            //DrawLinks(g2d, panel,dx, dy,gs,board,colorScheme);
        }

        int unitCount = 0;
        for (ComponentUnit unit : gs.getUnitState().getUnits()) {
            unitCount++;
        }

        String info = "T: " + gs.getTime() + ", S: " + gs.getSteps() + ", P₀: " + unitCount;
        g2d.drawString(info, 10, dy - 15);

        if (panel != null) {
            panel.last_start_x = dx / 2 - sizex / 2;
            panel.last_start_y = dy / 2 - sizey / 2;
            panel.last_grid = grid;
            g2d.translate(panel.last_start_x, panel.last_start_y);
        } else {
            int last_start_x = dx / 2 - sizex / 2;
            int last_start_y = dy / 2 - sizey / 2;
            g2d.translate(last_start_x, last_start_y);
        }

        Color wallColor = new Color(0, 0.33f, 0);
        Color po0color = new Color(0, 0, 0.25f);
        Color po1color = new Color(0.25f, 0, 0);
        Color pobothcolor = new Color(0.25f, 0, 0.25f);

        // Draw tiles
        for (int j = 0; j < board.getWidth(); j++) {
            for (int i = 0; i < board.getHeight(); i++) {
                if (board.getTile(j, i).type == Tile.TILE_TRACK) {
                    //g2d.setColor(colors[pgs.getTile(j,i).colors[0]]);
                    ArrayList<Integer> colors_list = new ArrayList();
                    int[] colors_;
                    int num_colors = 0;
                    int tile_colors = board.getTile(j, i).colors;
                    if (tile_colors == 0) {
                        colors_ = new int[]{0};
                        num_colors = 1;
                    } else {
                        int current_color = 1;
                        while (tile_colors > 0) {
                            if ((tile_colors & 1) != 0) {
                                colors_list.add(current_color);
                                num_colors++;
                            }
                            current_color++;
                            tile_colors >>= 1;
                        }
                        colors_ = new int[colors_list.size()];
                        for (int idx = 0; idx < colors_.length; idx++) {
                            colors_[idx] = colors_list.get(idx);
                        }
                    }
                    //int num_colors = board.getTile(j, i).colors.length;
                    int grid_o = grid / num_colors;
                    int color_o = 0;
                    for (int k = 0; k < num_colors; k++) {
                        for (int l = 0; l < num_colors; l++) {
                            color_o = color_o % num_colors;
                            int color_index = colors_[color_o];
                            if (colorScheme == DISPLAY_TILES) {
                                g2d.setColor(colors[color_index]);
                            } else {
                                g2d.setColor(Color.LIGHT_GRAY);
                            }
                            g2d.fillRect(j * grid + l * grid_o, i * grid + k * grid_o, grid_o, grid_o);
                            color_o++;

                        }
                        color_o++;

                    }
//                    drawArrows(board.getTile(j, i), g2d, grid);
                    drawDirectionArrows(board.getTile(j, i), g2d, grid);

                } else {
                }
            }
        }

        // draw grid lines
        g2d.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i <= board.getWidth(); i++) {
            g2d.drawLine(i * grid, 0, i * grid, board.getHeight() * grid);
        }
        for (int i = 0; i <= board.getHeight(); i++) {
            g2d.drawLine(0, i * grid, board.getWidth() * grid, i * grid);
        }

        // draw the components:
        // this list copy is to prevent a concurrent modification exception
        List<Component> lc = new LinkedList<Component>();
        lc.addAll(gs.getComponentState().getComponents());
        for (Component c : lc) {
            int reduction = grid / 2 > 1 ? 1 : 0;
            int offsx = 0;
            int offsy = 0;
            String repr = "";
            g2d.setColor(Color.WHITE);
            if (c instanceof ComponentPickup) {
                ComponentPickup c2 = (ComponentPickup) c;
                repr = "P:" + c2.available;
            } else if (c instanceof ComponentDelivery) {
                ComponentDelivery c2 = (ComponentDelivery) c;
                repr = "D:" + c2.delivered + "/" + c2.missed;
            } else if (c instanceof ComponentSemaphore) {
                ComponentSemaphore c2 = (ComponentSemaphore) c;
                repr = "S:" + c2.id;
                g2d.setColor(c2.value == ComponentSemaphore.RED ? Color.RED : Color.GREEN);
            } else if (c instanceof ComponentSignal) {
                ComponentSignal c2 = (ComponentSignal) c;
                repr = "B:" + c2.link;
            } else if (c instanceof ComponentConditional) {
                ComponentConditional c2 = (ComponentConditional) c;
                drawArrowsConditional(c2, g2d, grid);
                repr = String.valueOf("Cond: " + c2.id);
            } else if (c instanceof ComponentIntersection) {
                ComponentIntersection c2 = (ComponentIntersection) c;
                drawArrowsIntersection(c2, g2d, grid, colors_bold[c2.color]);
            } else if (c instanceof ComponentDiverter) {
                ComponentDiverter c2 = (ComponentDiverter) c;
                drawArrowsDiverter(c2, g2d, grid, colors_bold[c2.color]);
                repr = "Div";
            } else if (c instanceof ComponentUnit) {
                repr = "A" + c.id;

            } else if (c instanceof ComponentExchange) {
                ComponentExchange c2 = (ComponentExchange) c;
                repr = "Ex:" + c2.exchanged;
            }
            if (repr.length() > 0 && repr.charAt(0) == ' ') {
                g2d.fillOval(c.x * grid + reduction, c.y * grid + reduction, grid - reduction * 2, grid - reduction * 2);

            }
            g2d.drawOval(c.x * grid + reduction, c.y * grid + reduction, grid - reduction * 2, grid - reduction * 2);
            g2d.setColor(colors_bold[c.color]);
            if (colorScheme == DISPLAY_TILES ||
                colorScheme == DISPLAY_TILES_AND_LINKS) {
                g2d.drawString(repr, c.x * grid + reduction * 2, c.y * grid + grid / 2);
            }
        }

        // draw the units:
        // this list copy is to prevent a concurrent modification exception
        List<ComponentUnit> lu = new LinkedList<ComponentUnit>();
        lu.addAll(gs.getUnitState().getUnits());
        for (ComponentUnit u : lu) {
            int reduction = grid / 2 > 4 ? 4 : 0;
            int offsx = Component.DIRECTION_OFFSET_DICT_X[u.direction] * (grid / 2 - reduction);
            int offsy = Component.DIRECTION_OFFSET_DICT_Y[u.direction] * (grid / 2 - reduction);
            g2d.setColor(colors[u.color]);
            g2d.fillOval(u.x * grid + reduction, u.y * grid + reduction, grid - reduction * 2, grid - reduction * 2);
            g2d.setColor(Color.BLACK);
//                if (panel!=null && panel.toHighLight.contains(u)) g2d.setColor(Color.green);
            g2d.drawOval(u.x * grid + reduction, u.y * grid + reduction, grid - reduction * 2, grid - reduction * 2);
            g2d.setColor(Color.BLACK);
            g2d.drawLine(u.x * grid + grid / 2, u.y * grid + grid / 2, u.x * grid + grid / 2 + offsx, u.y * grid + grid / 2 + offsy);
            String repr = " P:" + u.payload.length + " D:" + u.delivered + "/" + u.missed;
            if (colorScheme == DISPLAY_TILES ||
                colorScheme == DISPLAY_TILES_AND_LINKS) {
                g2d.setColor(Color.WHITE);
                g2d.drawString(repr, u.x * grid + reduction * 2, u.y * grid + grid / 2);
            }

            // print the player resources in the base:
//                String txt = "" + pgs.getPlayer(u.getPlayer()).getResources();
            //              g2d.setColor(Color.black);
            //            FontMetrics fm = g2d.getFontMetrics( g2d.getFont() );
            //          int width = fm.stringWidth(txt);
            //        g2d.drawString(txt, u.getX()*grid + grid/2 - width/2, u.getY()*grid + grid/2);
//                String txt = "" + u.getResources();
            //              g2d.setColor(Color.black);
//                FontMetrics fm = g2d.getFontMetrics( g2d.getFont() );
            //              int width = fm.stringWidth(txt);
            //            g2d.drawString(txt, u.getX()*grid + grid/2 - width/2, u.getY()*grid + grid/2);
//            if (u.getHitPoints()<u.getMaxHitPoints()) {
            //              g2d.setColor(Color.RED);
            //            g2d.fillRect(u.getX()*grid+reduction, u.getY()*grid+reduction, grid, 2);
            //          g2d.setColor(Color.GREEN);
            //        g2d.fillRect(u.getX()*grid+reduction, u.getY()*grid+reduction, (int)(grid*(((float)u.getHitPoints())/u.getMaxHitPoints())), 2);
        }
        if (colorScheme == DISPLAY_LINKS ||
            colorScheme == DISPLAY_TILES_AND_LINKS) {
            DrawLinks(g2d, panel, dx, dy, gs, board, colorScheme);
        }

    }

    private static void DrawLinks(Graphics2D g2d, BoardGameStatePanel panel,
            int dx, int dy,
            GameState gs,
            BoardState board,
            int colorScheme) {
        // draw the components:
        // this list copy is to prevent a concurrent modification exception
        List<Component> lc = new LinkedList<Component>();
        lc.addAll(gs.getComponentState().getComponents());
        int gridx = (dx - 64) / board.getWidth();
        int gridy = (dy - 64) / board.getHeight();
        float grid = (float) Math.min(gridx, gridy);

        for (Component c : lc) {
            if (c instanceof ComponentSignal) {
                ComponentSignal c2 = (ComponentSignal) c;
                if (c2.link > 0) {
                    Component c3 = null;
                    for (Component c_ : lc) {
                        if (c_.id == c2.link) {
                            c3 = c_;
                            break;
                        }
                    }
                    if (c3 != null) {
                        UpdateBezier(g2d, new Pair((float) c2.x * grid + grid * 0.5f, (float) c2.y * grid + grid * 0.5f), new Pair((float) c3.x * grid + grid * 0.5f, (float) c3.y * grid + grid * 0.5f), grid);
                    }
                }
            }
            //g2d.fillOval(c.x * grid, c.y * grid, grid, grid);

        }

    }

    private static void UpdateBezier(Graphics2D g2d, Pair<Float, Float> p0, Pair<Float, Float> p3, float grid) {
        int lineRendererSegments = 10;
        //float lineZ = 0.0f;
        Random r = new Random();
        //float extra_y = 1.0f * grid;
        //float extra_x = 0.0f * grid;
        float extra_y = (r.nextFloat() * 4.0f - 2.0f) * grid;
        float extra_x = (r.nextFloat() * 4.0f - 2.0f) * grid;
        float extra_y_ = (r.nextFloat() * 4.0f - 2.0f) * grid;
        float extra_x_ = (r.nextFloat() * 4.0f - 2.0f) * grid;

        //if (Mathf.Abs (p0.x - p3.x) + Mathf.Abs (p0.y - p3.y) < 2.0f) {
        /*if((p3-p0).magnitude > 0.1){
			if (Mathf.Abs (p0.x - p3.x) > Mathf.Abs (p0.y - p3.y)) {
				extra_y = 2.0f;
			} else {
				extra_x = 2.0f;
			}
		}*/
        //Pair<Float,Float> p1 = new Pair(p0.m_a+extra_x, (p0.m_b+p3.m_b)/2.0f+extra_y);
        //Pair<Float,Float> p2 = new Pair((p0.m_a+p3.m_a)/2.0f+extra_x_, p3.m_b+extra_y_);
        Pair<Float, Float> p1 = new Pair(p0.m_a + extra_x, p0.m_b + extra_y);
        Pair<Float, Float> p2 = new Pair(p3.m_a + extra_x_, p3.m_b + extra_y_);

        Pair<Integer, Integer> pa = null;
        for (int j = 0; j <= lineRendererSegments; j++) {
            Pair<Integer, Integer> pb = GetBezierPoint(((float) j / lineRendererSegments), p0, p1, p2, p3);
            if (pa != null) {
                g2d.setColor(Color.BLUE);
                g2d.drawLine(pa.m_a, pa.m_b, pb.m_a, pb.m_b);
            }
            pa = pb;
        }
    }

    private static Pair<Integer, Integer> GetBezierPoint(float t, Pair<Float, Float> p0, Pair<Float, Float> p1, Pair<Float, Float> p2, Pair<Float, Float> p3) {
        float cx = 3.0f * (float) (p1.m_a - p0.m_a);
        float cy = 3.0f * (float) (p1.m_b - p0.m_b);
        float bx = 3.0f * (float) (p2.m_a - p1.m_a) - cx;
        float by = 3.0f * (float) (p2.m_b - p1.m_b) - cy;
        float ax = (float) p3.m_a - (float) p0.m_a - cx - bx;
        float ay = (float) p3.m_b - (float) p0.m_b - cy - by;
        float Cube = t * t * t;
        float Square = t * t;

        float resX = (ax * Cube) + (bx * Square) + (cx * t) + p0.m_a;
        float resY = (ay * Cube) + (by * Square) + (cy * t) + p0.m_b;

        return new Pair(Math.round(resX), Math.round(resY));
    }

}
