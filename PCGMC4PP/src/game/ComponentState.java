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
package game;

import game.component.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ComponentState {

    private List<Component> components;
    
    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }
    public int addComponent(Component component){
        this.components.add(component);
        return this.components.size()-1;
    }
    public Component getComponent(int index){
        return this.components.get(index);
    }
    public Component getComponentById(int id){
        for(Component component : this.components){
            if(component.id==id){
                return component;
            }
        }
        return null;
    }
    public List<Component> getComponentsByType(Class component_type){
        List<Component> ret = new ArrayList();
        for(Component component : this.components){
            if(component_type.isInstance(component)){
                ret.add(component);
            }
        }
        return ret;
    }

    public Component getComponentByPosition(int x, int y){
        // Only one component can be in each tile according to the GDD 20160429
        for(Component component : this.components){
            if(component.x==x && component.y==y){
                return component;
            }
        }
        return null;
    }

    public ComponentState() {
        components = new ArrayList<Component>();
    }

    public ComponentState(List<Component> components) {
        this.components = components;
    }

    public ComponentState clone() {
        List<Component> new_components = new ArrayList<Component>();
        for(Component c:this.components){
            new_components.add(c.clone());
        }
        return new ComponentState(new_components);
    }

    public String toTextRepresentation() {
        return "COMPONENTS\n\n";
    }
}
