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
public class PhysicalGameStateJFrame extends JFrame {
    BoardGameStatePanel panel = null;
    
    public PhysicalGameStateJFrame(String title, int dx, int dy, BoardGameStatePanel a_panel) {
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
