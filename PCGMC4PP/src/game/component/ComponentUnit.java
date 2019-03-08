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
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentUnit extends Component {
    public static final String representation = "thread";

    // Initialization properties
    public int initial_direction = Component.EAST;
    public int direction = Component.EAST;
    public int capacity = -1;
    public int delay = 0;
    
    // Assert properties
    public int[] payload = {}; // This is a list of packages as pickup point ids
    public int picked = 0;
    public int delivered = 0;
    public int missed = 0;
    
    public int consecutive_unscheduled = 0; // to trim successors with unfair schedules
    public int consecutive_blocked = 0; // to check for starvation
    
    // Component movement properties
    public Tile tile_current = null;
    public Tile tile_next = null;
    
    // Internal properties
    public List<Tile> path;
    public int path_position;
    public int deadlock_count = 0;
    
    public int time_pickup_min = -1;
    public int time_delivery_min = -1;
    public int time_exchange_min = -1;
    public int time_pickup_max = -1;
    public int time_delivery_max = -1;
    public int time_exchange_max = -1;

    public ComponentUnit(int x,int y,int id, int color, char owner, boolean locked) {
        super(x,y,id,color,owner,locked);
        this.path_position = 0;
        this.path = new ArrayList<Tile>(); // UNITY List<>
    }
    public ComponentUnit(int x,int y,int id, int color){
        this(x,y,id,color,Component.OWNER_SYSTEM,true);
    }
    public ComponentUnit clone(){
        ComponentUnit clone = new ComponentUnit(x,y,id,color,owner,locked);
        Component.copyProperties(clone, this);
        clone.initial_direction = this.initial_direction;
        clone.direction = this.direction;
        clone.capacity = this.capacity;
        clone.delay = this.delay;
        clone.payload = this.payload;
        clone.picked = this.picked;
        clone.delivered = this.delivered;
        clone.missed = this.missed;
        
        clone.tile_current = this.tile_current;
        clone.tile_next = this.tile_next;
        
        clone.consecutive_unscheduled = this.consecutive_unscheduled;
        clone.consecutive_blocked = this.consecutive_blocked;
        
        
        clone.path = this.path;
        clone.path_position = this.path_position;
        clone.deadlock_count = this.deadlock_count;

        return clone;
    }
    
    public void updateClockTick(GameState gs){
        if(this.delay>0) this.delay--;
    }
    
    public boolean canUpdateClockTick(GameState gs){
        return (this.delay>0);
    }
    
    public boolean updateUnitEnter(ComponentUnit unit,GameState gs){
        super.updateUnitEnter(unit, gs);
        return (unit.id!=this.id);
    }
    public boolean updateUnitLeave(ComponentUnit unit,GameState gs){
        super.updateUnitLeave(unit, gs);
        return (unit.id!=this.id);
    }

}
