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
import game.Tile;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentConditional extends Component {

    public static final String representation = "conditional";

    public int directions[] = {0, 0};
    public int current = 0;
    public int current_default = 0;

    public ComponentConditional clone() {
        ComponentConditional clone = new ComponentConditional(this.x, this.y, this.id, this.color, this.owner, this.locked);
        Component.copyProperties(clone, this);
        clone.directions = this.directions;
        clone.current = this.current;
        return clone;
    }
    

    
    public ComponentConditional(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }

    public void updateExternalSignal(ComponentSignal signal, GameState gs) {
        // Can only be triggered by one component according to the GDD 20160429
        this.current++;
        this.current = this.current % this.directions.length;
        // Update the next_tile for units that may be in this component
        for(ComponentUnit unit:gs.getUnitState().getUnitsByPosition(x, y)){
            int forced_direction = this.directions[this.current];
            Tile tile_next = gs.getBoardState().getTile(unit.tile_current.x+Component.DIRECTION_OFFSET_DICT_X[forced_direction], unit.tile_current.y+Component.DIRECTION_OFFSET_DICT_Y[forced_direction]);
            unit.tile_next = tile_next;
        }
    }
    
    public int forcesDirection(ComponentUnit unit,GameState gs){
        return this.directions[this.current];
    }

}
