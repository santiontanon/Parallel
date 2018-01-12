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
import static valls.util.ListToArrayUtility.intInIntArray;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentDiverter extends Component {

    public static final String representation = "diverter";
    public int direction_condition = 0;
    public int direction_default = 0;
    public int[][] directions_types = {{},{},{},{}};
    public int[][] directions_colors = {{},{},{},{}};

    public ComponentDiverter clone() {
        ComponentDiverter clone = new ComponentDiverter(this.x, this.y, this.id, this.color, this.owner, this.locked);
        Component.copyProperties(clone, this);
        clone.direction_condition = this.direction_condition;
        clone.direction_default = this.direction_default;
        clone.directions_types = this.directions_types;
        clone.directions_colors = this.directions_colors;
        return clone;
    }

    public ComponentDiverter(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }
    public int forcesDirection(ComponentUnit unit, GameState gs) {
        if(unit.payload.length==0){
            for(int direction=0;direction<4;direction++){
                if(intInIntArray(this.directions_types[direction],ComponentPickup.EMPTY)){
                    return direction;
                }
                if(intInIntArray(this.directions_colors[direction],-1)){
                    return direction;
                }
            }
        } else {
            // Colors override types
            for(int i=0;i<unit.payload.length;i++){
                ComponentPickup cp = (ComponentPickup)gs.getComponentState().getComponentById(unit.payload[i]);
                for(int direction=0;direction<4;direction++){
                    if(intInIntArray(this.directions_colors[direction],cp.color)){
                        return direction;
                    }
                }
            }
            // Colors override types
            for(int i=0;i<unit.payload.length;i++){
                ComponentPickup cp = (ComponentPickup)gs.getComponentState().getComponentById(unit.payload[i]);
                for(int direction=0;direction<4;direction++){
                    if(intInIntArray(this.directions_types[direction],cp.type)){
                        return direction;
                    }
                }
            }
        }
        return -1;
    }
    
    public int forcesDirectionOldCodeByDiverterColor(ComponentUnit unit, GameState gs) {
        if (unit == null || unit.payload.length==0){
            // No package always fails
            return this.direction_default;
        } else if (this.color==0 && unit.payload.length>0){
            // Case when it's only checking for any package
            return this.direction_condition;
        } else {
            for(int i=0;i<unit.payload.length;i++){
                Component cp = gs.getComponentState().getComponentById(unit.payload[i]);
                if(this.color==cp.color){
                    // It's carrying the package somewhere
                    return this.direction_condition;
                }
            }
            // Not carrying the package
            return this.direction_default;
        }
        
    }
    
    public boolean updateUnitEnter(ComponentUnit unit,GameState gs){
        // This doesn't depend on anything else, same as intersection
        super.updateUnitEnter(unit, gs);
        return false; // this was set to true but made Evan's philosophers level go into a loop
    }
    public boolean updateUnitLeave(ComponentUnit unit,GameState gs){
        // This doesn't depend on anything else, same as intersection
        super.updateUnitLeave(unit, gs);
        return true;
    }

}
