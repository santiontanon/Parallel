using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class Metadata {
	public int level_id;
    public string pcg_id;
    public int level_type; //0 for tutorial, 1 for advanced, 2 for pcg
	public string level_title;
	public string goal_string;
	public GoalCondition goal_struct;
	public PlayerPalette player_palette;
	public int time_pickup_min = 0;
	public int time_pickup_max = 0;
	public int time_delivery_min = 3;
	public int time_delivery_max = 3;
	public int time_exchange_min = 1;
	public int time_exchange_max = 1;
	public int max_colors = 0;
	public int board_width = 0;
	public int board_height = 0;
    public float time_efficiency = 0;

	public Metadata()
	{
		level_id = 0;
        pcg_id = "";
		level_title = "level title";
		goal_string = "goal string";
		goal_struct = new GoalCondition();

	}
}
