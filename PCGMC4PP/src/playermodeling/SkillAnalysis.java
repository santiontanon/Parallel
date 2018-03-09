package playermodeling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

    public void readSkillList(String level_name) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("skills/"+level_name));
            String line = "";
            while ((line = br.readLine()) != null) {
                skill_map.put( line , new Pair<Integer,Double>(0,0.0) );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        skill_map.clear();
    }

    public void updateSkills(String classification) {
        /* TODO: This is based on an update function ( try to use Bayesian Knowledge Tracing ) */

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

    @Override
    public String toString() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skill_map.keySet() ) {
            String frmt = String.format("SKill %s (%d,%f)",s,skill_map.get(s).p1,skill_map.get(s).p2);
            tmp.add(frmt);
        }
        return  String.join("\n",tmp);
    }
}
