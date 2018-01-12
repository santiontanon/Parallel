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
public class ComponentSignal extends Component{
    public static final String representation = "signal";
    
    public int link = 0;
    
    public ComponentSignal clone(){
        ComponentSignal clone = new ComponentSignal(this.x,this.y,this.id,this.color,this.owner,this.locked);
        Component.copyProperties(clone, this);
        clone.link = this.link;
        return clone;
    }
    
    public ComponentSignal(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }
    
    public boolean updateUnitEnter(ComponentUnit unit,GameState gs){
        super.updateUnitEnter(unit, gs);
        // Q Does the button get triggered on enter or on leave? A on enter
        if(this.link>0){
            Component c = gs.getComponentState().getComponentById(this.link);
            if(c!=null) c.updateExternalSignal(this, gs);
        }
        return false;
    }
    /*public boolean updateUnitLeave(ComponentUnit unit,GameState gs){
        super.updateUnitLeave(unit, gs);
        // Q Does the button get triggered on enter or on leave? A on leave
        if(this.link>0){
            Component c = gs.getComponentState().getComponentById(this.link);
            if(c!=null) c.updateExternalSignal(this, gs);
        }
        return false;

    }*/
    
}
