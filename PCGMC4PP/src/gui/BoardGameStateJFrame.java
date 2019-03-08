/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import javax.swing.JFrame;
import game.GameState;

/**
 *
 * @author santi
 */
public class BoardGameStateJFrame extends JFrame {
    BoardGameStatePanel panel = null;
    
    public BoardGameStateJFrame(String title, int dx, int dy, GameState gs) {
        this(title, dx, dy, new BoardGameStatePanel(gs));
    }
    
    
    public BoardGameStateJFrame(String title, int dx, int dy, BoardGameStatePanel a_panel) {
        super(title);
        panel = a_panel;

        getContentPane().add(panel);
        pack();
        setResizable(false);
        setSize(dx,dy);
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    public BoardGameStatePanel getPanel() {
        return panel;
    }
    
    public void setState(GameState gs) {
        panel.setState(gs);
    }
            
}
