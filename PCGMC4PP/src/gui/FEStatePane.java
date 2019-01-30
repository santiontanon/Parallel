/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import support.GameStateExporter;
import support.GameStateParser;
import gui.BoardGameStatePanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.JList;
import javax.swing.Box;

import game.GameState;
import game.pcg.MapGenerator;
import game.BoardState;
import game.ComponentState;
import game.GameStateSearch;
import game.execution.ExecutionDeterministic;
import game.execution.ExecutionFair;
import game.execution.ExecutionNonDeterministic;
import game.execution.ExecutionPlan;
import game.UnitState;
import game.execution.ExecutionDeterministic1;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lgraphs.ontology.Sort;
import support.PCG;
import valls.util.Pair;

/**
 *
 * @author santi
 */
public class FEStatePane extends JPanel {

    GameState currentGameState = null;
    GameState initialGameState = null;
    BoardGameStatePanel statePanel = null;
    JTextArea textArea = null;
    JFileChooser fileChooser = new JFileChooser();
    JFormattedTextField cpuTimeField = null;
    JFormattedTextField pcgRandomSeedGraph = null;
    JFormattedTextField pcgRandomSeedEmbedding = null;
    JFormattedTextField pcgSize = null;

    JFormattedTextField maxCyclesField = null;
    JCheckBox saveTraceBox = null;
    JCheckBox slowDownBox = null;
    DefaultListModel model = null;
    List<ExecutionPlan> solutions = null;
    List<ExecutionPlan> solutions_base = null;
    ExecutionPlan current_plan = null;
    JList solutionsJList = null;
    boolean forceStop = false;

    FEStateMouseListener mouseListener = null;

    public FEStatePane() {
        //initialGameState = MapGenerator.SmallTest();
        //initialGameState = MapGenerator.SmallerTest();
        try {
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-3-mini.txt", true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-3-mini-solved.txt", true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-complex-mini.txt", true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-2-prototype-solved.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-2-prototype-solved-better.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-colors.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-3-prototype.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-colors-expanded.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-nocolors.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-6.txt",true);
            //initialGameState = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-8-prototype.txt",true);
            initialGameState = GameStateParser.parseFile("/Users/josepvalls/Desktop/level01.txt",true);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(e.getStackTrace());
            initialGameState = MapGenerator.SmallTest();
        }
        currentGameState = initialGameState.clone();
        setLayout(new BorderLayout());

        JPanel panelLeft = new JPanel();
        panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
        {
            JPanel ptmp = new JPanel();
            ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
            {
                JButton b = new JButton("Generate Level");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                            try {
                                long randomSeedGraph = Long.parseLong(pcgRandomSeedGraph.getText());
                                long randomSeedEmbedding = Long.parseLong(pcgRandomSeedEmbedding.getText());
                                int size = Integer.parseInt(pcgSize.getText());
                                pcgRandomSeedGraph.setText(Long.toString(randomSeedGraph+1));
                                //pcgRandomSeedEmbedding.setText(Long.toString(randomSeedEmbedding+1));
                                System.out.println(pcgRandomSeedGraph+"; "+pcgRandomSeedEmbedding);
                                initialGameState = PCG.generateGameState(randomSeedGraph, randomSeedEmbedding, size, true, false, null, false);
                                //initialGameState = PCG.generateGameState(randomSeed, true, false);
                                initialGameState.init();
                                currentGameState = initialGameState.clone();
                                statePanel.setState(currentGameState);
                                statePanel.repaint();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                    }
                });
                ptmp.add(b);
            }
            panelLeft.add(ptmp);
        }
        pcgRandomSeedGraph = addTextField(panelLeft, "Seed Graph:", "0", 5);
        pcgRandomSeedEmbedding = addTextField(panelLeft, "Seed Embedding:", "0", 5);
        pcgSize = addTextField(panelLeft, "Size:", "0", 5);
        panelLeft.add(new JSeparator(SwingConstants.HORIZONTAL));
        {
            JPanel ptmp = new JPanel();
            ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
            {
                JButton b = new JButton("Import Level");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = fileChooser.showOpenDialog((Component) null);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            try {
                                initialGameState = GameStateParser.parseFile(file.getAbsolutePath(), false);
                                currentGameState = initialGameState.clone();
                                statePanel.setState(currentGameState);
                                statePanel.repaint();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
                ptmp.add(b);
            }
            {
                JButton b = new JButton("Export Level");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (currentGameState != null) {
                            int returnVal = fileChooser.showSaveDialog((Component) null);
                            if (returnVal == fileChooser.APPROVE_OPTION) {
                                File file = fileChooser.getSelectedFile();
                                try {
                                    FileWriter fw = new FileWriter(file);
                                    fw.write(GameStateExporter.export(currentGameState));
                                    fw.close();
                                    //XMLWriter xml = new XMLWriter(new FileWriter(file.getAbsolutePath()));
                                    //currentGameState.getPhysicalGameState().toxml(xml);
                                    //xml.flush();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                });
                ptmp.add(b);
            }
            panelLeft.add(ptmp);
        }
        panelLeft.add(new JSeparator(SwingConstants.HORIZONTAL));
        {
            String colorSchemes[] = {"Board Overview", "Components", "State Reduction", "Links"};
            JComboBox b = new JComboBox(colorSchemes);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setAlignmentY(Component.TOP_ALIGNMENT);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JComboBox combo = (JComboBox) e.getSource();
                    if (combo.getSelectedIndex() == 0) {
                        statePanel.setDisplayLayer(BoardGameStatePanel.DISPLAY_TILES);
                    }
                    if (combo.getSelectedIndex() == 1) {
                        statePanel.setDisplayLayer(BoardGameStatePanel.DISPLAY_COMPONENTS);
                    }
                    if (combo.getSelectedIndex() == 2) {
                        statePanel.setDisplayLayer(BoardGameStatePanel.DISPLAY_REDUCED_STATE);
                    }
                    if (combo.getSelectedIndex() == 3) {
                        statePanel.setDisplayLayer(BoardGameStatePanel.DISPLAY_LINKS);
                    }
                    statePanel.repaint();
                }
            });
            b.setMaximumSize(new Dimension(300, 24));
            panelLeft.add(b);
        }
        panelLeft.add(new JSeparator(SwingConstants.HORIZONTAL));
        // Executions
        {
            JPanel ptmp = new JPanel();
            ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
            JButton b = new JButton("Compute Solutions");
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setAlignmentY(Component.TOP_ALIGNMENT);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Computing solutions...");
                    model.clear();
                    model.addElement(new ExecutionDeterministic());
                    model.addElement(new ExecutionDeterministic1());
                    model.addElement(new ExecutionNonDeterministic());
                    model.addElement(new ExecutionFair());
                    current_plan = (ExecutionPlan)model.get(0);
                    Runnable r = new Runnable() {
                        public void run() {
                            try {
                                System.out.println("Searching...");
                                GameStateSearch gss = new GameStateSearch(currentGameState);
                                gss.setSearchBudget(30000);
                                gss.verbose = true;
                                gss.search();
                                System.out.println("Done searching...");
                                if(true){
                                    if (gss.isSearchOverBudget()) {
                                        System.out.println("Search budget depleted");
                                    }
                                    if (gss.isSearchSpaceExhausted()) {
                                        System.out.println("Search space exhausted");
                                    }
                                    gss.printStats();
                                }
                                for(GameState gs:gss.getSomeResults()){
                                    model.addElement(GameStateSearch.toExecutionPlan(gs));
                                }
                                ExecutionPlan p = GameStateSearch.toExecutionPlan(gss.getWorstResult());
                                p.addDescriptionComment("Worst returned to ME");
                                model.addElement(p);
                                
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    };
                    (new Thread(r)).start();
                }
            });
            ptmp.add(b);
            panelLeft.add(ptmp);
        }
        {
            model = new DefaultListModel();
            model.addElement(new ExecutionDeterministic());
            model.addElement(new ExecutionDeterministic1());
            model.addElement(new ExecutionNonDeterministic());
            model.addElement(new ExecutionFair());
            model.addElement(new ExecutionPlan());
            JPanel ptmp = new JPanel();
            /*solutions_base = new ArrayList();
            solutions = new ArrayList();
            solutions_base.add(new ExecutionDeterministic());
            solutions_base.add(new ExecutionDeterministic1());
            solutions_base.add(new ExecutionNonDeterministic());
            solutions_base.add(new ExecutionFair());
            solutions.addAll(solutions_base);
            solutions.add(new ExecutionPlan());
            current_plan = solutions.get(0);
            solutionsJList = new JList(solutions.toArray());*/
            current_plan = (ExecutionPlan)model.get(0);
            solutionsJList = new JList(model);
            solutionsJList.setSelectedIndex(0);
            solutionsJList.setAlignmentX(Component.CENTER_ALIGNMENT);
            solutionsJList.setAlignmentY(Component.TOP_ALIGNMENT);
            
            solutionsJList.addListSelectionListener(new ListSelectionListener(){
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    //current_plan = solutionsJList.getModel().getElementAt(solutionsJList.getSelectedIndex());
                    //current_plan = solutions.get(solutionsJList.getSelectedIndex());
                    if (solutionsJList.getSelectedIndex() >= 0) {
                        current_plan = (ExecutionPlan) model.get(solutionsJList.getSelectedIndex());
                        System.out.println("Selected plan "+solutionsJList.getSelectedIndex()+ " "+current_plan.toString());
                    }
                }
            });
            
            JScrollPane js = new JScrollPane(solutionsJList);
            js.setMinimumSize(new Dimension(300,300));
            js.setMaximumSize(new Dimension(300,300));
            js.setPreferredSize(new Dimension(300,300));

            ptmp.add(js);
            ptmp.setMinimumSize(new Dimension(300,300));
            ptmp.setMaximumSize(new Dimension(300,300));
            ptmp.setPreferredSize(new Dimension(300,300));
            //Box box = Box.createRigidArea(new Dimension(300,300));
            panelLeft.add(ptmp);
        }

        {
            JPanel ptmp = new JPanel();
            ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
            {
                JButton b = new JButton("Import Execution");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                    }
                });
                ptmp.add(b);
            }
            {
                JButton b = new JButton("Export Execution");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = fileChooser.showSaveDialog((Component) null);
                        if (returnVal == fileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            try {
                                FileWriter fw = new FileWriter(file);
                                fw.write(GameStateExporter.export(initialGameState, current_plan, null));
                                fw.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                    }
                });
                ptmp.add(b);
            }
            panelLeft.add(ptmp);

        }

        panelLeft.add(new JSeparator(SwingConstants.HORIZONTAL));

        cpuTimeField = addTextField(panelLeft, "Delay (ms):", "100", 5);
        maxCyclesField = addTextField(panelLeft, "Max Cycles:", "10000", 5);

        {
            JPanel ptmp = new JPanel();
            ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
            {
                JButton b = new JButton("Run");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        forceStop = false;
                        Runnable r = new Runnable() {
                            public void run() {
                                try {
                                    int period = Integer.parseInt(cpuTimeField.getText());
                                    if (!slowDownBox.isSelected()) {
                                        period = 1;
                                    }
                                    int MAXCYCLES = Integer.parseInt(maxCyclesField.getText());
                                    boolean gameover = false;

                                    long nextTimeToUpdate = System.currentTimeMillis() + period;

                                    do {
                                        if (System.currentTimeMillis() >= nextTimeToUpdate) {
                                            /*
                                             PlayerAction pa1 = ai1.getAction(0, gs);
                                             gs.issueSafe(pa1);
                                             gameover = gs.cycle();
                                             */
                                            currentGameState = current_plan.next(currentGameState);
                                            statePanel.repaint();
                                            nextTimeToUpdate += period;

                                        } else {
                                            Thread.sleep(1);
                                        }
                                    } while (!forceStop && !gameover && currentGameState.getSteps() < MAXCYCLES);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        };
                        (new Thread(r)).start();
                    }
                });
                ptmp.add(b);
            }
            {
                JButton b = new JButton("Pause");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        forceStop = true;
                    }
                });
                ptmp.add(b);
            }
            {

                JButton b = new JButton("Step");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        currentGameState = current_plan.next(currentGameState);
                        System.out.println(currentGameState);
                        statePanel.setState(currentGameState);
                        statePanel.repaint();
                    }
                });
                ptmp.add(b);

            }
            {

                JButton b = new JButton("Reset");
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setAlignmentY(Component.TOP_ALIGNMENT);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        currentGameState = initialGameState.clone();
                        System.out.println(currentGameState);
                        current_plan.reset();
                        statePanel.setState(currentGameState);
                        statePanel.repaint();
                    }
                });
                ptmp.add(b);

            }
            panelLeft.add(ptmp);
        }

        {
            JPanel ptmp = new JPanel();
            ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
            {
                slowDownBox = new JCheckBox("Slow Down");
                slowDownBox.setAlignmentX(Component.CENTER_ALIGNMENT);
                slowDownBox.setAlignmentY(Component.TOP_ALIGNMENT);
                slowDownBox.setMaximumSize(new Dimension(120, 20));
                slowDownBox.setSelected(true);
                ptmp.add(slowDownBox);
            }
            panelLeft.add(ptmp);
        }

        JPanel panelRight = new JPanel();
        panelRight.setLayout(new BoxLayout(panelRight, BoxLayout.Y_AXIS));
        statePanel = new BoardGameStatePanel(currentGameState);
        statePanel.setPreferredSize(new Dimension(800, 800));
        panelRight.add(statePanel);
        /*textArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setEditable(false);
        scrollPane.setPreferredSize(new Dimension(512, 192));
        panelRight.add(scrollPane, BorderLayout.CENTER);*/

        add(panelLeft, BorderLayout.WEST);
        add(panelRight, BorderLayout.EAST);

        mouseListener = new FEStateMouseListener(statePanel);
        statePanel.addMouseListener(mouseListener);
    }

    public JFormattedTextField addTextField(JPanel p, String name, String defaultValue, int columns) {
        JPanel ptmp = new JPanel();
        ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
        ptmp.add(new JLabel(name));
        JFormattedTextField f = new JFormattedTextField();
        f.setValue(defaultValue);
        f.setColumns(columns);
        f.setMaximumSize(new Dimension(80, 20));
        ptmp.add(f);
        p.add(ptmp);
        return f;
    }

    public void setState(GameState gs) {
        currentGameState = gs;
        statePanel.setState(currentGameState);
        statePanel.repaint();
    }
}
