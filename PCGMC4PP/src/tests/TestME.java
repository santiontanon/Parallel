/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import game.GameState;
import support.GameStateParser;

/**
 *
 * @author santi
 */
public class TestME {
    public static void main(String args[]) throws Exception
    {
        String filename = "/Users/santi/Library/Application Support/GAIMS/Parallel/MEInput_ME_20181030231609.txt";
//        String filename = "pcg-example1.txt";
        GameState gs = GameStateParser.parseFile(filename, false);
        boolean res = TestPCGNeedsSolution.notSolvable(gs);
        System.out.println(res);
    }
}
