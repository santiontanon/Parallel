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

import game.GameState;
import support.GameStateExporter;
import support.GameStateParser;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class TestImport {
    public static void main(String args[]) throws Exception {       
        GameState gs;
        System.out.println("################ PROBLEMATIC FILE ################");
        //gs = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-3-prototype-errors.txt",true);
        System.out.println("################ SOLUTION FILE ################");
        //gs = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-2-prototype-solved.txt",true);
        System.out.println("################ GOOD FILE ################");
        //gs = GameStateParser.parseFile("/Users/josepvalls/Dropbox/projects/unity/ParallelProg/levels/level-3-prototype.txt",true);
        gs = GameStateParser.parseFile("/Users/josepvalls/Desktop/MEInput_PLAY_20171003172159.txt",true);
        System.out.println("################ FILE EXPORT ################");
        System.out.println(GameStateExporter.export(gs));
        
        
    } 
}
