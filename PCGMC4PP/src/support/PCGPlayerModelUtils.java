/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import lgraphs.sampler.LGraphRewritingGrammar;
import lgraphs.sampler.LGraphRewritingRule;

/**
 *
 * @author santi
 */
public class PCGPlayerModelUtils {
    static final LinkedHashMap<String, String> skills = new LinkedHashMap<>();
    static final LinkedHashMap<String, String> skills_inv = new LinkedHashMap<>();
    static final ArrayList<String> targetSkills = new ArrayList<>();
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
        
        targetSkills.add("Be able to link buttons to direction switches");
        targetSkills.add("Understand exchange points");
        targetSkills.add("Block critical sections");
        targetSkills.add("Synchronize multiple arrows");
        targetSkills.add("Alternating access with semaphores and buttons (ensure mutual exclusion)");
        
        for(String skill:skills.keySet()) {
            skills_inv.put(skills.get(skill), skill);
        }
    }
    
    
    public static String skillNameToSkillCode(String name)
    {
        return skills.get(name);
    }

    
    public static String skillCodeToSkillName(String name)
    {
        return skills_inv.get(name);
    }
    

    public static int determineLevelSize(LinkedHashMap<String, Double> playerModel)
    {
        double minimum = 1;
        double average = 0;
        double count = 0;
        
        for(String skill:playerModel.keySet()) {
            average += playerModel.get(skill);
            minimum = Math.min(minimum, playerModel.get(skill));
            count++;
        }
        average/=count;

        if (average <= 0.5) {
            return 0;
        } else if (average <= 0.8) {
            if (minimum < 0.3) return 1;
            return 2;
        } else if (average <= 0.9) {
            return 3;
        } else {
            return 4;
        }
    }   
    
    
    public static void applyPlayerModel(LinkedHashMap<String, Double> playerModel, LGraphRewritingGrammar grammar)
    {
        // identify the lowest of the target skills:
        double min_skill_mastery = 0;
        String min_skill = null;
        for(String skill:targetSkills) {
            if (playerModel.get(skill) != null) {
                double m = playerModel.get(skill);
                if (min_skill == null || m < min_skill_mastery) {
                    min_skill = skill;
                    min_skill_mastery = m;
                }
            }
        }
        
        System.out.println("applyPlayerModel: min_skill: " + min_skill);
        System.out.println("applyPlayerModel: min_skill_mastery: " + min_skill_mastery);
        
        if (min_skill == null) return;
        
        String skill_code = skillNameToSkillCode(min_skill);
        for(LGraphRewritingRule rule:grammar.getRules()) {
            if (rule.getTags().contains(skill_code)) {
                System.out.println("applyPlayerModel: increasing the weight of rule " + rule.getName());
                rule.weight *= 2;
            } else {
                rule.weight /= 2;
            }
        }
    }
    
}
