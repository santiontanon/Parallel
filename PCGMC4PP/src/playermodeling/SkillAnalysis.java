package playermodeling;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pavankantharaju on 3/5/18.
 */
public class SkillAnalysis {

    public HashMap< String,Pair<Integer,Double> > skill_map;

    public SkillAnalysis() {
        skill_map = new HashMap< String, Pair<Integer,Double> >();
    }

    public void readSkillList() {
        /* TODO: Finish this */
    }

    public void updateSkills(String classification) {
        double val = 0.0;
        if (classification.equals("C")) {
            val = 1.0;
        } else if (classification.equals("B")) {
            val = 0.5;
        } else if (classification.equals("A")) {
            val = 0.0;
        } else {
            val = 0.0;
        }
        for ( String s : skill_map.keySet() ) {
            Pair<Integer, Double> tmp = skill_map.get(s);
            tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + val )/(tmp.p1+1);
            tmp.p1++;
            skill_map.replace(s,tmp);
        }
    }

}
