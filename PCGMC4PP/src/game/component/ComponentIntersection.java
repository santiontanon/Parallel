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
package game.component;

import game.GameState;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentIntersection extends Component {
    public static final String representation = "intersection";
    public int directions[] = {0,0,0,0,0,0};

    public ComponentIntersection clone() {
        ComponentIntersection clone = new ComponentIntersection(this.x, this.y, this.id, this.color, this.owner, this.locked);
        Component.copyProperties(clone, this);
        clone.directions = this.directions;
        return clone;
    }

    
    
    public ComponentIntersection(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }

    public int getDirection(int color) {
        return directions[color];
    }

    public void setDirection(int direction) {
        for(int i=0;i<this.directions.length;i++) this.directions[i] = direction;
    }
        
    public void setDirection(int direction,int color) {
        if(this.directions.length<=color){
            int directions[] = new int[color];
            for(int i =0;i<color;i++){
                if(i<this.directions.length) 
                    directions[i]=this.directions[i];
                else
                    directions[i]=direction;
            }
        } else {
            this.directions[color] = direction;
        }
    }  
    
    public int forcesDirection(ComponentUnit unit,GameState gs){
        if(unit==null || unit.color>=this.directions.length) {
            // This currently supports 6 colors only
            return this.directions[0];
        } else {
            return this.directions[unit.color];
        }
    }
    
    public boolean updateUnitEnter(ComponentUnit unit,GameState gs){
        // This doesn't depend on anything else
        super.updateUnitEnter(unit, gs);
        return true;
    }
    public boolean updateUnitLeave(ComponentUnit unit,GameState gs){
        // This doesn't depend on anything else
        super.updateUnitLeave(unit, gs);
        return true;
    }

    
}
