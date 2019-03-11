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

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentCustoms extends ComponentDelivery {
    // TODO Q Should this behave like a semaphore by preventing going into the tile? Kinda makes sense if used for diversion.
    public static final String representation = "customs";

    public ComponentCustoms clone(){
        ComponentCustoms clone = new ComponentCustoms(this.x,this.y,this.id,this.color,this.owner,this.locked);
        Component.copyProperties(clone, this);
        clone.consumer = this.consumer;
        clone.strict = this.strict;
        clone.delivered = this.delivered;
        clone.missed = this.missed;
        return clone;
    }

    
    public ComponentCustoms(int x, int y, int id, int color, char owner, boolean locked) {
        super(x, y, id, color, owner, locked);
    }
    
}
