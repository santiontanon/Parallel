/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import gui.BoardGameStatePanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import game.GameState;
import game.BoardState;
import game.Tile;
import game.component.Component;
import game.component.ComponentComment;
import game.component.ComponentUnit;
import javax.swing.JOptionPane;

/**
 *
 * @author santi
 */
public class PopUpStateEditorMenu extends JPopupMenu {

    public PopUpStateEditorMenu(final GameState gs, final int x, final int y, final BoardGameStatePanel panel) {

        final BoardState bs = gs.getBoardState();
        Component c = null;
        //c = gs.getComponentState().getComponentByPosition(x, y);

        if (c == null) {
            if (bs.getTile(x, y).type == Tile.TILE_EMPTY) {
                JMenuItem i = new JMenuItem("Set " + x + " " + y + " track");
                i.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        bs.getTile(x, y).type = Tile.TILE_TRACK;
                        bs.getTile(x, y).tile_bitmask = 1;
                        panel.repaint();
                    }
                });
                add(i);
            } else {
                {
                    JMenuItem i = new JMenuItem("Set " + x + " " + y + " empty");
                    i.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            bs.getTile(x, y).type = Tile.TILE_EMPTY;
                            bs.getTile(x, y).tile_bitmask = 0;
                            panel.repaint();
                        }
                    });
                    add(i);
                }
                {
                    JMenuItem i = new JMenuItem("Add component to " + x + " " + y);
                    i.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            String comment = (String) JOptionPane.showInputDialog("Comment for the component?");
                            gs.getComponentState().addComponent(new ComponentComment(x, y, comment));
                            panel.repaint();
                        }
                    });
                    add(i);
                }
            }
        }
    }

}
