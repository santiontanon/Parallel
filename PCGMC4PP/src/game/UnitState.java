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
package game;

import game.component.ComponentUnit;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class UnitState {
    private List<ComponentUnit> units;

    public void addUnit(ComponentUnit unit) {
        units.add(unit);
    }

    
    public List<ComponentUnit> getUnits() {
        return units;
    }
    public ComponentUnit getUnit(int index){
        return units.get(index);
    }

    public ComponentUnit getUnitById(int id){
        for(ComponentUnit component : this.units){
            if(component.id==id){
                return component;
            }
        }
        return null;
    }
    public ComponentUnit getUnitByPosition(int x, int y){
        // Only one component can be in each tile but multiple units can
        for(ComponentUnit component : this.units){
            if(component.x==x && component.y==y){
                return component;
            }
        }
        return null;
    }
    public List<ComponentUnit> getUnitsByPosition(int x, int y){
        return this.getUnitsByPosition(x, y, null);
    }
    public List<ComponentUnit> getUnitsByPosition(int x, int y, ComponentUnit exclude){
        List<ComponentUnit> l = new ArrayList();
        for(ComponentUnit cu : this.units){
            if(cu.x==x && cu.y==y && cu!=exclude){
                l.add(cu);
            }
        }
        return l;
    }


    
    public void setUnits(List<ComponentUnit> units) {
        this.units = units;
    }
    public UnitState(){
        units = new ArrayList<ComponentUnit>();
    }
    public UnitState(List<ComponentUnit> units){
        this.units = units;
    }
    public UnitState clone(){
        List<ComponentUnit> new_units = new ArrayList<ComponentUnit>();
        for(ComponentUnit unit:units){
            new_units.add(unit.clone());
        }
        return new UnitState(new_units);
    }

}
