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
package tests;

import java.io.File;
import game.GameState;
import gui.BoardGameStateJFrame;
import support.PCG;

/*

to do:
    OK - in the JESS1 pattern, have a way to divert the arrow with the package to the proper path
    - re-add the other rules, and see how they interact
    - try to beat a few levels
    - push to the repo
*/


/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class TestPCGExport {

    public static void main(String args[]) throws Exception {
        int bitmask = 0;
        for (int i = 0; i < 8; i++) {
            System.out.println((char) ('@' + bitmask));
            System.out.println(1 << i + '@');
            //System.out.println(1<<i);
            bitmask = 1 << i;
        }

        /*GameState gs;
        gs = MapGenerator.SmallestTest();
        System.out.println(GameStateExporter.export(gs));
        gs = MapGenerator.SmallerTest();
        System.out.println(GameStateExporter.export(gs));
        gs = MapGenerator.SmallTest();
        System.out.println(GameStateExporter.export(gs));
        gs.setBoardState(gs.getBoardState().expand(7, 3));
        System.out.println(GameStateExporter.export(gs));
        gs = MapGenerator.SmallestTest();
        gs.setBoardState(gs.getBoardState().expand(7, 3));
        System.out.println(GameStateExporter.export(gs));*/
        
        String batchId;
        //batchId = "week2"; // Doesn't have SYNCRO nor DEADLOCK
        //batchId = "week45"; // Doesn't have SYNCRO
        //batchId = "week79"; 
        //batchId = "extra4";
        batchId = "santiTest";
        for(int size=1;size<=1;size++){        
            for(int randomSeed=0;randomSeed<10;randomSeed++){
                String filename = "level-PCG-"+batchId+"-"+size+"-"+randomSeed+".txt";
                GameState gs = PCG.generateGameState(randomSeed,randomSeed, size, false, true);
                BoardGameStateJFrame f = new BoardGameStateJFrame(filename, 1280, 640, gs);
                PCG.export(gs, new File(filename));
            }
        }
   

    }
}
