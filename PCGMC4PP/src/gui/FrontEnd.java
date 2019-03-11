/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author santi
 */
public class FrontEnd extends JPanel {
    
    public FrontEnd() {
        super(new GridLayout(1, 1));         
        JComponent panel1 = new FEStatePane();
        add(panel1);
    }
        
    protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }    
    
    public static void main(String args[]) {
        JFrame frame = new JFrame("PCGMC4PP Front End");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);         
        frame.add(new FrontEnd(), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }    
}
