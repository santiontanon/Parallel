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
public class ComponentExchange extends Component {
    public static final String representation = "exchange";
    
    public static final int VALUE_IDLE = 0;
    public static final int VALUE_BUSY = 1;
    
    public int link = -1;
    public int strict;
    public int stop;
    public int exchanged;
    public int delay = 0;
    public int value = VALUE_IDLE;
    
    public ComponentExchange clone(){
        ComponentExchange clone = new ComponentExchange(this.x,this.y,this.id,this.color,this.owner,this.locked);
        Component.copyProperties(clone, this);
        clone.link = this.link;
        clone.strict = this.strict;
        clone.stop = this.stop;
        clone.exchanged = this.exchanged;
        clone.delay = this.delay;
        clone.value = this.value;
        return clone;
    }
    
    
    public ComponentExchange(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }
    
    public boolean canUpdateClockTick(GameState gs){
        return (this.delay>0);
    }
    
    public void updateClockTick(GameState gs){
        if(this.delay>0) this.delay--;
    }

    private boolean tryExchange(ComponentUnit unit, GameState gs) {
        ComponentExchange ce_link = (ComponentExchange) gs.getComponentState().getComponentById(this.link);
        if(ce_link==null){
            return false;
        }
        if (ce_link.value == ComponentExchange.VALUE_BUSY) {
            // There is already a unit waiting there
            ComponentUnit unit_link = gs.getUnitState().getUnitByPosition(ce_link.x, ce_link.y);
            int[] temp = unit_link.payload;
            unit_link.payload = unit.payload;
            unit.payload = temp;
            // Exchange completed
            if (GameState.enable_delays) {
                unit.delay = gs.getBoardState().time_exchange_min;
                // TODO select from the min-max range?
            }
            this.exchanged++;
            ce_link.exchanged++;
            this.value = ComponentExchange.VALUE_IDLE;
            ce_link.value = ComponentExchange.VALUE_IDLE;
            return true;

        } else {
            // Nothing, just stay in busy...
            this.value = ComponentExchange.VALUE_BUSY;
            return false;
        }

    }

    public boolean updateUnitEnter(ComponentUnit unit, GameState gs) {
        super.updateUnitEnter(unit, gs);
        this.tryExchange(unit, gs);
        return false;
    }

    public boolean canUnitLeave(ComponentUnit unit, GameState gs) {
        return (this.value == ComponentExchange.VALUE_IDLE);
    }

    public boolean canUnitEnter(ComponentUnit unit, GameState gs) {
        // TODO Q prevent multiple units in an exchange? A yes for now, tryExchange is retrieving units by position
        return (this.value == ComponentExchange.VALUE_IDLE && gs.getUnitState().getUnitsByPosition(x, y).size()==0);

    }

    
}
