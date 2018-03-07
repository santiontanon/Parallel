/*Event type (M, S or E): Whether this is a "turn", change of speed or another event
Time step (int): The current time
Component id (int)
Component x position (int)
Component y position (int)
Status (string_json): A json object representing a map of key-value pairs. 
*/

using UnityEngine;
using System.Collections.Generic;

[System.Serializable]
public class StepData {
	public string eventType = "exampleType";
	public int timeStep = -1;
	public int componentID = -1;
	public Vector2 componentPos = new Vector2(-1,-1);
	public Status componentStatus = new Status();

    public StepData GetNextMove(Dictionary<int, List<StepData>> dictionary)
    {
        if(timeStep + 1 < dictionary.Count)
        {
            for (int i = 0; i < dictionary[timeStep + 1].Count; i++)
            {
                if (dictionary[timeStep + 1][i].componentID == componentID && dictionary[timeStep + 1][i].eventType == "M")
                    return dictionary[timeStep + 1][i];
            }
        }
        return null;
    }

    public StepData GetPreviousMove(Dictionary<int, List<StepData>> dictionary)
    {
        if(timeStep - 1 > -1)
        {
            for (int i = 0; i < dictionary[timeStep - 1].Count; i++)
            {
                if (dictionary[timeStep - 1][i].componentID == componentID && dictionary[timeStep - 1][i].eventType == "M")
                    return dictionary[timeStep - 1][i];
            }
        }
        return null;
    }
}

[System.Serializable]
public class Status 
{
	public int final_condition = -1;
    public string[] goal_descriptions = new string[] { };
	public int speed = -1;
	public int value = 0;
	public int passed = 0;
	public int delivered = 0;
	public int available = -1;
	//public int[] missed = new int[]{};
	public int[] payload = new int[]{};
	public int goals_completed = 0;
	public int exchange_between_b = 0;
	public int exchange_between_a = 0;
	public int delivered_to = 0;
	public int delivered_from = 0;
	//public int missed_items = 0;
	public int missed = 0;
	public int[] missed_items = new int[]{};
	public int[] delivered_items = new int[]{};
}


