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
public class ComponentPickup extends Component {
    public static final String representation = "pickup";
    
    public static final int UNCONDITIONAL = 0;
    public static final int CONDITIONAL = 1;
    public static final int LIMITED = 2;    
    public static final int EMPTY = 3;
    public static final String[] PICKUP_TYPES = new String[]{"Unconditional","Conditional","Limited", "Empty"};
    
    public int type = 0;
    public int available = 1;
    public int picked = 0;
    
    public ComponentPickup clone(){
        ComponentPickup clone = new ComponentPickup(this.x,this.y,this.id,this.color,this.owner,this.locked);
        Component.copyProperties(clone, this);
        clone.type = this.type;
        clone.available = this.available;
        clone.picked = this.picked;
        return clone;
    }

    
    public ComponentPickup(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }
    
    private boolean canPickup(ComponentUnit unit, GameState gs){        
        return (unit.payload.length<unit.capacity || unit.capacity == -1);
    }
    private void pickup(ComponentUnit unit, GameState gs){                
        this.picked++;
        //unit.payload=new int[]{this.id};
        int[] new_payload = new int[unit.payload.length+1];
        for(int i=0;i<unit.payload.length;i++){
            new_payload[i]=unit.payload[i];
        }
        new_payload[unit.payload.length]=this.id;
        unit.payload = new_payload;
    }
    public boolean updateUnitEnter(ComponentUnit unit,GameState gs){
        super.updateUnitEnter(unit, gs);
        return !this.canPickup(unit, gs);
    }
    public void updateDelivery(ComponentDelivery cd, ComponentUnit cu,GameState gs){
        switch (this.type) {
            case ComponentPickup.UNCONDITIONAL:
                break;
            case ComponentPickup.CONDITIONAL:
                this.available++;
                break;
            case ComponentPickup.LIMITED:
                break;
        }
    }
    
  
    public boolean updateUnitLeave(ComponentUnit unit,GameState gs){
        // This doesn't depend on anything else, same as intersection
        super.updateUnitLeave(unit, gs);
        boolean has_picked = false;
        // TODO Q when to pickup a package? A as soon as enters
        switch (this.type) {
            case ComponentPickup.UNCONDITIONAL:
                if(this.canPickup(unit, gs)){
                    this.pickup(unit, gs);
                    has_picked = true;
                }
                break;
            case ComponentPickup.CONDITIONAL:
            case ComponentPickup.LIMITED:
                if (this.available > 0) {
                    if(this.canPickup(unit, gs)){
                        this.pickup(unit, gs);
                        this.available--;
                        has_picked = true;
                    }
                } else {
                    // Santi: since the following condition is not 100% sure, 
                    // I am commenting it out for now. The other arrow might just be there by chance...
                    // if (!gs.getUnitState().getUnitsByPosition(this.x, this.y, unit).isEmpty()) {
                        // Here, an arrow is going through a pickup point, unable to pick up a package, and
                        // there is another arrow in the same spot. So, it's likely that the other arrow is
                        // the one that took the package, and thus it's a race condition.
                        // gs.race_condition_detected = true;
                    // }                    
                }
                break;
        }
        if(GameState.enable_delays){
            unit.delay = gs.getBoardState().time_pickup_min;
            // TODO select from the min-max range?
        }
        return (!has_picked && !this.canPickup(unit, gs));
    }   
}
