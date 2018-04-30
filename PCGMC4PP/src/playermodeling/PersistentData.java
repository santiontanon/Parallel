package playermodeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by pavankantharaju on 3/15/18.
 */
public class PersistentData {

    public HashMap<String, Object > persistent_data;

    public PersistentData() {
        persistent_data = new HashMap<String,Object>();

        persistent_data.put("cur_track", -1);
        persistent_data.put("cur_mouse_comp", "");
        persistent_data.put("cur_mouse_time", 0.0);
        persistent_data.put("linking", false);
        persistent_data.put("filename","");
        persistent_data.put("dragging",false);
        persistent_data.put("test_before_submit",false);
        persistent_data.put("num_dragged",0);


        persistent_data.put("width",0);
        persistent_data.put("height",0);

        persistent_data.put("goal_struct",new HashMap<String, Object>());
        persistent_data.put("direction_layout",new ArrayList< ArrayList<String> >());
        persistent_data.put("color_layout",new ArrayList< ArrayList<String> >());
        persistent_data.put("critical_section",new ArrayList< ArrayList<String> >());
        persistent_data.put("comp_link_map",new HashMap<String, String >());
        persistent_data.put("comp_color_map",new HashMap<String, String >());
        persistent_data.put("comp_loc_layout",new HashMap<String, ArrayList<Integer> >());
        persistent_data.put("comp_loc_map",new HashMap<String, ArrayList<Integer> >());

    }

    public Set<String> getPersistentInfo() {
        return persistent_data.keySet();
    }


}
