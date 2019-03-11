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
import game.UnitState;
import game.component.Component;
import game.component.ComponentUnit;
import game.pcg.MapGenerator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class TestSymmetry {
    public static void main(String args[]) throws Exception {       
        //GameState a = new GameState();
        //GameState b = new GameState();
        GameState a,b,c,d,e,f;
        //GameState b;
        a = MapGenerator.SmallTest();
        b = a.clone();
        c = a.clone();
        d = a.clone();
        e = a.clone();
        f = a.clone();
        
        ArrayList<ComponentUnit> x = new ArrayList<ComponentUnit>();
        x.add(new ComponentUnit(0,0,101,0,Component.OWNER_SYSTEM,true));
        x.add(new ComponentUnit(1,1,102,1,Component.OWNER_SYSTEM,true));
        c.setUnitState(new UnitState(x));
        
        ArrayList<ComponentUnit> y = new ArrayList<ComponentUnit>();
        y.add(new ComponentUnit(1,1,101,1,Component.OWNER_SYSTEM,true));
        y.add(new ComponentUnit(0,0,102,0,Component.OWNER_SYSTEM,true));
        d.setUnitState(new UnitState(y));
        
        ArrayList<ComponentUnit> z = new ArrayList<ComponentUnit>();
        z.add(new ComponentUnit(0,0,101,1,Component.OWNER_SYSTEM,true));
        z.add(new ComponentUnit(1,1,102,0,Component.OWNER_SYSTEM,true));
        e.setUnitState(new UnitState(z));
        
        ArrayList<ComponentUnit> w = new ArrayList<ComponentUnit>();
        w.add(new ComponentUnit(1,1,101,0,Component.OWNER_SYSTEM,true));
        w.add(new ComponentUnit(0,0,102,1,Component.OWNER_SYSTEM,true));
        w.add(new ComponentUnit(1,1,101,0,Component.OWNER_SYSTEM,true));
        f.setUnitState(new UnitState(w));

        System.out.println(a.stateDescriptionHash());
        System.out.println(b.stateDescriptionHash());
        System.out.println(c.stateDescriptionHash());
        System.out.println(d.stateDescriptionHash());
        System.out.println(e.stateDescriptionHash());
        System.out.println(f.stateDescriptionHash());
        //System.out.println(a.stateDescriptionEquivalentTo(b));
        
                
    }
}
