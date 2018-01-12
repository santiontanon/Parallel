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
import java.util.ArrayList;
import valls.util.ListToArrayUtility;
import static valls.util.ListToArrayUtility.intInIntArray;
import valls.util.Pair;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentDelivery extends Component {
    public static final String representation = "delivery";
    
    public static final int IS_STRICT = 1;
    
    public int consumer = -1;
    public int strict = 0;
    
    public int delivered = 0;
    public int missed = 0;
    public int denominator = -1;
    
    public int[] accepted_types = {};
    public int[] accepted_colors = {};
    
    // note, the type field is used by the pickup, cannot be reused unless exported too
    
    public ComponentDelivery clone(){
        ComponentDelivery clone = new ComponentDelivery(this.x,this.y,this.id,this.color,this.owner,this.locked);
        Component.copyProperties(clone, this);
        clone.consumer = this.consumer;
        clone.strict = this.strict;
        clone.delivered = this.delivered;
        clone.missed = this.missed;
        clone.accepted_types = this.accepted_types;
        clone.accepted_colors = this.accepted_colors;
        return clone;
    }
    
    public ComponentDelivery(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }
    
    private boolean canConsume(ComponentPickup cp){
        // old logic
        // return (this.color==0 || this.color==cp.color);
        return ((this.accepted_types.length==0 || intInIntArray(this.accepted_types,cp.type)) && 
                (this.accepted_colors.length==0 || intInIntArray(this.accepted_colors,cp.color)));
    }
    
    private void consume(ComponentUnit unit,GameState gs,boolean fail_to_deliver){
        if (this.consumer==0){
            // Consume nothing
        } else {            
            ArrayList<Integer> new_payload = new ArrayList();
            int consumed = 0;
            for(int i=0;i<unit.payload.length;i++){
                if(this.consumer==-1 || consumed<this.consumer){
                    ComponentPickup cp = (ComponentPickup)gs.getComponentState().getComponentById(unit.payload[i]);
                    if(canConsume(cp)){
                        cp.updateDelivery(this,unit,gs);
                        if(fail_to_deliver){
                            this.missed++;
                            unit.missed++;                        
                        } else {
                            this.delivered++;
                            unit.delivered++;
                            // TODO remove this once it's generalized
                            Pair<Integer,Integer> pair = new Pair(unit.id,this.id);
                            int value;
                            if(gs.goals_delivery.containsKey(pair)){
                                value = gs.goals_delivery.get(pair);
                            } else {
                                value = 0;
                            }
                            value++;
                            gs.goals_delivery.put(pair, value);
                        }
                        consumed++;
                    } else {
                        // cannot consume this package because of color mismatch
                        new_payload.add(unit.payload[i]);
                        
                    }
                } else {
                    // cannot consume more
                    new_payload.add(unit.payload[i]);
                }
            }
            if(new_payload.size()==0){
                unit.payload = new int[]{};
            } else {
                unit.payload = ListToArrayUtility.toIntArray(new_payload);
            }
        }
    }
    public boolean updateUnitEnter(ComponentUnit unit,GameState gs){
        super.updateUnitEnter(unit, gs);
        if(GameState.enable_delays){
            unit.delay = gs.getBoardState().time_delivery_min;
            // TODO select from the min-max range?
        }
        return false;
    }
    public boolean updateUnitLeave(ComponentUnit unit,GameState gs){
        super.updateUnitLeave(unit, gs);
        // TODO strict is handled in canMove, probably redundant here, confirm
        boolean fail_to_deliver = false;
        // if there are other untis delivering when this leaves, fails            
        // TODO Q do delivery fails if there are other units but don't have packages? A No, (original design for prototype-2 used to fail always)
        for(ComponentUnit cu: gs.getUnitState().getUnitsByPosition(this.x, this.y, unit)){
            if(cu.payload.length>0){
                fail_to_deliver = true;
                break;
            }
        }
        // TODO Q do delivery fails even when the time_delivery is 0?, same for pickup and exchange then?
        this.consume(unit, gs, fail_to_deliver);
        return false;
    }
  
    public boolean canUnitLeave(ComponentUnit unit, GameState gs) {
        if (this.strict == ComponentDelivery.IS_STRICT && this.color != 0) {
            // TODO Q no color delivery accepts any package color? A yes
            // this is in conflict with the customs, but customs are currently not considered
            for (int unit_package_id : unit.payload) {
                ComponentPickup cp = (ComponentPickup) gs.getComponentState().getComponentById(unit_package_id);
                if (this.color == cp.color) {
                    return true; // A package matches
                }
            }
            return false; // It's strict and no package matches
        } else {
            return true; // It's ok, can go
        }
    }
}
