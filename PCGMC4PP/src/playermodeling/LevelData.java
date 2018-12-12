package playermodeling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by pavankantharaju on 3/15/18.
 */
public class LevelData {

    public HashMap<String, Object> data;

    public LevelData() {
        data = new HashMap<>();

        /* Player Execution Information */
        data.put("cur_track", -1);
        data.put("cur_mouse_comp", "");
        data.put("cur_mouse_time", 0.0);
        data.put("linking", false);
        data.put("filename","");
        data.put("dragging",false);
        data.put("test_before_submit",false);
        data.put("num_dragged",0);

        /* Static level information */
        data.put("skills_per_level", new ArrayList<>());

        /* Dynamic level information */
        data.put("comp_link_map",new HashMap<String, String >());
        data.put("comp_color_map",new HashMap<String, String >());
        data.put("comp_loc_layout",new HashMap<String, ArrayList<Integer> >());
        data.put("comp_loc_map",new HashMap<String, ArrayList<Integer> >());

        /* Level structural information */
        data.put("width",0);
        data.put("height",0);
        data.put("goal_struct",new HashMap<String, Object>());
        data.put("direction_layout",new ArrayList< ArrayList<String> >());
        data.put("color_layout",new ArrayList< ArrayList<String> >());
        data.put("critical_section",new ArrayList< ArrayList<String> >());
    }

    public Set<String> getLevelDataAttributes() {
        return data.keySet();
    }

}
