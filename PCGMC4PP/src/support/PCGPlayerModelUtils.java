/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package support;

import java.util.LinkedHashMap;

/**
 *
 * @author santi
 */
public class PCGPlayerModelUtils {
    static final LinkedHashMap<String, String> skills = new LinkedHashMap<>();
    static{
        skills.put("Hover over objects to see what they do", "hover_objects");
        skills.put("Use help bar", "help_bar");
        skills.put("Drag objects", "drag_objects");
        skills.put("Place objects on the track", "place_objects");
        skills.put("Hover over side arrows to see different colored tracks", "hover_colors");
        skills.put("Remove unnecessary elements", "remove");
        
        skills.put("Deliver packages", "deliver_packages");
        skills.put("Be able to link buttons to direction switches", "link_direction_switches");
        skills.put("Be able to link semaphores to buttons", "link_semaphores");
        skills.put("Understand the use of semaphores", "semaphores");
        skills.put("Use diverters", "diverters");
        skills.put("Understand exchange points", "exchange_points");
        skills.put("Understand specific delivery points", "specific_deliveries");
        skills.put("Understand that arrows move at unpredictable rates", "nondeterministic_arrows");
        skills.put("Testing before submitting", "test_submit");
        
        skills.put("Understand that events happen in different orders", "nondeterminism");
        skills.put("Prevent starvation", "starvation");
        skills.put("Block critical sections", "critical_sections");
        skills.put("Synchronize multiple arrows", "synchronize");
        skills.put("Alternating access with semaphores and buttons (ensure mutual exclusion)", "mutual_exclusion");

        skills.put("Deliver packages with multiple synchronized arrows", "synchronized_delivery");
    }
    
    
    public static String skillNameToSkillCode(String name)
    {
        return skills.get(name);
    }
    
}
