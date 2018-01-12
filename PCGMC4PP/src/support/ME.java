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
package support;

import game.GameState;
import game.GameStateSearch;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.ParseException;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ME {

    public static void main(String args[]) throws Exception {
        // ExitCode = 0: OK
        // ExitCode = 1: System Errors
        // ExitCode = 2: Errors parsing input, use support.validate to check input
        // ExitCode = 3: Search budget depleted, no results, try a bigger budget
        // ExitCode = 4: No results found, search space exhausted (other search errors)
        // ExitCode = 5: Wrong arguments
        // ExitCode = 6: Other exception within the ME Java code
        if (args.length >= 2) {
            try {
                String filename = args[0];
                int budget = Integer.parseInt(args[1]);
                
                GameState gs = GameStateParser.parseFile(filename, false);
                if (GameStateParser.errors > 0) {
                    System.out.println("Errors parsing input file: " + GameStateParser.errors);
                    System.exit(2);
                }

                GameStateSearch gss = new GameStateSearch(gs);
                gss.setSearchBudget(budget);
                if (args.length >= 3) {
                    gss.setSearchTime(Integer.parseInt(args[2]));
                }
                gss.setSearchOptions(false, true, true, true);
                gss.search();

                /*
                if (gss.getAllResults().size()==0 && gss.isSearchOverBudget()) {
                    System.out.println("Search budget depleted without solutions");
                    System.exit(2);
                } else if (gss.getAllResults().size()==0 && gss.isSearchSpaceExhausted()){
                    System.out.println("Search space exhausted without solutions");
                    System.exit(3);                    
                } else {*/
                GameState result = gss.getWorstResult();
                if(result==null){
                    System.out.println("Search space exhausted without solutions");
                    System.exit(4);                    
                } else {
                    String out = GameStateExporter.export(gss,null);
                    File file = new File(filename);
                    String path = file.getAbsolutePath();
                    File out_file = File.createTempFile("me_out_",".txt",file.getParentFile());
                    PrintWriter writer = new PrintWriter(out_file);
                    writer.print(out);
                    writer.close();
                    System.out.println(out_file.getAbsolutePath());
                    System.exit(0);
                }
            } catch (FileNotFoundException ex) {
                System.out.println("Exception: " + ex.getMessage());
                System.exit(6);
            }
        } else {
            System.out.println("Usage: support.ME filename budget [milliseconds]");
            System.exit(4);
        }
    }
}
