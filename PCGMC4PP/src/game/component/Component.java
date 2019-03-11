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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class Component implements Cloneable {

    public static final String representation = "?";

    public static final char OWNER_SYSTEM = 'S';
    public static final char OWNER_PLAYER = 'P';

    public static final int EAST = 2;
    public static final int NORTH = 3;
    public static final int SOUTH = 1;
    public static final int WEST = 0;

    public static final String[] DIRECTIONS = new String[]{"West", "South", "East", "North"};

    public static final int[] DIRECTION_OFFSET_DICT_X = new int[]{-1, 0, 1, 0};
    public static final int[] DIRECTION_OFFSET_DICT_Y = new int[]{0, 1, 0, -1};

    public int id;
    public int x;
    public int y;
    public int color;
    public char owner;
    public boolean locked;

    public int passed = 0;

    public Component(int x, int y, int id, int color, char owner, boolean locked) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.color = color;
        this.owner = owner;
        this.locked = locked;
    }

    public Component clone() {
        //return new Component(x,y,id,color,owner,locked);
        throw new RuntimeException("Not implemented");
    }

    public static void copyProperties(Component to, Component from) {
        to.passed = from.passed;
    }

    public void updateExternalSignal(ComponentSignal signal, GameState gs) {
        // triggered by a unit traveling by a signal component elsewhere
    }

    public void updateClockTick(GameState gs) {
        // triggered by time passing (currently used for ComponentUnit delays)
    }

    public boolean canUpdateClockTick(GameState gs) {
        return false;
    }

    public boolean canUnitEnter(ComponentUnit unit, GameState gs) {
        // Units stop in the tile before a semaphore according to the GDD 20160429        
        return true;
    }

    public boolean canUnitLeave(ComponentUnit unit, GameState gs) {
        return true;
    }

    public boolean updateUnitEnter(ComponentUnit unit, GameState gs) {
        // triggered by a unit traveling into this component
        // returns wether the unit can continue moving in the current step
        return false;
    }

    public boolean updateUnitLeave(ComponentUnit unit, GameState gs) {
        // triggered by a unit traveling away from this component
        // returns wether the unit can continue moving in the current step
        this.passed++;
        return false;
    }

    public int forcesDirection(ComponentUnit unit, GameState gs) {
        // -1 doesn't force direction, either value WEST ... NORTH does
        return -1;
    }

    public String getRepresentation(){
        try {
            return (String) this.getClass().getField("representation").get(this);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Component.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Component.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Component.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Component.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "other";
    }
    public String toString() {
        try {
            return this.getRepresentation() + " " + this.id + " (" + this.x + "," + this.y + ")";
        } catch (SecurityException ex) {
            Logger.getLogger(Component.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Component.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "N/A " + this.id;
    }

}
