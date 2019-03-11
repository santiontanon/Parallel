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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import support.GameStateParser;
import static support.GameStateParser.componentList;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class GoalCondition {

    public static final int GOAL_REQUIRED = 0;
    public static final int GOAL_DESIRED = 1;
    public int component_id;
    public String component_class;
    public String component_property;
    public int component_value;
    public String condition;
    public int goal_type = GOAL_REQUIRED;
    private Class _class = null;
    private Field _field = null;
    public int thread_id = 0; // special case used for thread-specific deliveries

    public Field getField() {
        return this._field;
    }

    public Class getComponentClass() {
        return this._class;
    }

    public GoalCondition(int component_id, String component_class, String property, int value, String condition, int goal_type, int thread_id) {
        this.component_id = component_id;
        this.component_class = component_class;
        this.component_property = property;
        this.component_value = value;
        this.condition = condition;
        this.goal_type = goal_type;
        this.thread_id = thread_id;

        for (Class<? extends Component> componentClass : componentList) {
            String representation = null;
            try {
                representation = (String) componentClass.getField("representation").get(componentClass);
                
                if (this.component_class.equals(representation)) {
                    this._class = componentClass;
                    boolean found = true;
                    for(Field _field: getAllFields(componentClass)){
                        if(_field.getName().equals(this.component_property)){
                            this._field = _field;
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        Logger.getLogger(GoalCondition.class.getName()).log(Level.SEVERE, "Wrong Property for Condition", new Exception());
                    }
                    
                }
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(GoalCondition.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            } catch (SecurityException ex) {
                Logger.getLogger(GoalCondition.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(GoalCondition.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(GoalCondition.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }

        }
    }
    
    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    @Override
    public String toString() {
        return String.format("{\"id\":%d,\"type\":\"%s\",\"property\":\"%s\",\"value\":%s,\"condition\":\"%s\"}", this.component_id, this.component_class, this.component_property, this.component_value, this.condition);
    }
}
