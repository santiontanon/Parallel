using UnityEngine;
using System.Collections;
using System.Collections.Generic;
[System.Serializable]
public class GridComponent {
	public int id = 0;
	public string type = "";
	public int posX;
	public int posY;
	public string placedBy = "";
	public string editable = "";
	public Configuration configuration;

	public string GetComponentString()
	{
		string configString = "";
		configString = id + "\t" + type + "\t" + posX + "\t" + posY + "\t" + placedBy + "\t" + editable;
		return configString;
	}
	string FormatConfigurationString(
		bool type, bool color, bool initial_direction, 
		bool directions, bool capacity, bool consumer, 
		bool delay, bool time_pickup_min, bool time_delivery_min, 
		bool time_exchange_min, bool time_pickup_max, bool time_delivery_max, 
		bool time_exchange_max, bool picked, bool delivered, 
		bool missed, bool link)
	{
		string returnString = "";
		if(type){ returnString += "\"type\":"+configuration.type+","; }
		if(color){ returnString += configuration.color; }
		if(initial_direction){ returnString += configuration.initial_direction; }
		return "";
	}

	public string GetConfigurationString()
	{
		string returnConfigruation = "";
		switch(type)
		{
		case "thread":
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"color",
				"initial_direction",
				"capacity",
				"delay",
				"time_pickup_min",
				"time_delivery_min",
				"time_exchange_min",
				"time_pickup_max",
				"time_delivery_max",
				"time_exchange_max",
				"picked",
				"delivered",
				"missed"
			});
			break;
		case "spawner":
			/*NOTE: NO NUMBER OR FREQUENCY */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"color",
				"initial_direction",
				"capacity",
				"delay",
				"number",
				"frequency"
			});
			break;
		case "signal":
			/*NOTE: NO PASSED*/
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"link",
				"passed"
			});
			break;
		case "semaphore":
			/*NOTE: NO PASSED*/
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"value",
				"passed"
			});
			break;
		case "pickup":
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"type",
				"color",
				"passed",
				"picked"
			});
			break;
		case "delivery":
			/*NOTE: NO PASSED*/
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"color",
				"accepted_types",
				"accepted_colors",
				"consumer",
				"strict",
				"passed",
				"delivered",
				"denominator",
				"missed"
			});
			break;
		case "customs":
			/*not implemented yet */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"color",
				"consumer",
				"strict",
				"passed"
			});
			break;
		case "diverter":
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"direction_condition",
				"direction_default",
				"passed",
				"directions_types",
				"directions_colors"
			});
			break;
		case "exchange":
			/*NOTE: NO STRICT, STOP, PASSED, EXCHANGED */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"link",
				"color",
				"point",
				"strict",
				"stop",
				"passed",
				"exchanged"
			});
			break;
		case "painter":
			/*not implemented yet */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"passed",
				"painted"
			});
			break;
		case "conditional":
			/*NOTE: NO CURRENT, PASSED */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"directions",
				"current",
				"passed"
			});
			break;
		case "arrow":
			/*NOTE: NO DIRECTION, PASSED */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"directions",
				"passed"
			});
			break;
		case "intersection":
			/*NOTE: NO PASSED*/
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"directions",
				"passed"
			});
			break;
		case "line":
			/*NOTE: NO PASSED*/
			/*not implemented yet */
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{
				"passed"
			});
			break;
		default:
			returnConfigruation = configuration.GenerateConfigurationJSON(new string[]{"type","color","initial_directions","directions","capacity","consumer","delay","time_pickup_min"});
			break;
		}	
		return returnConfigruation;
	}
}

[System.Serializable]
public class Configuration
{
	/*{"color":1,"initial_direction":"East","capacity":1,"delay":0,"picked":0,"delivered":0,"missed":0}*/
	public string type;
	public string[] accepted_types = new string[]{};
	public int[] accepted_colors = new int[]{};
	public int color;
	public string initial_direction;
	public string[] directions = new string[]{}; 
	public string direction_condition;
	public string direction_default;
	public List<List<string>> directions_types = new List<List<string>>();
	public List<List<int>> directions_colors = new List<List<int>>();
	public int capacity;
	public int consumer;
	public int delay;
	public int time_pickup_min;
	public int time_delivery_min;
	public int time_exchange_min;
	public int time_pickup_max;
	public int time_delivery_max;
	public int time_exchange_max;
	public int picked;
	public int delivered;
	public int missed;
	public int link;
	public int value;
	public int passed;
	public int strict;
	public int current;
	public int current_original;
	public string direction;
	public int denominator;


	public string GenerateConfigurationJSON(string[] values)
	{
		string configs = "{";
		foreach(string s in values)
		{
			switch(s)
			{
			case "type": { configs+= "\"type\":"+ "\""+type+"\","; } break;
			case "color": { configs+= "\"color\":"+color+","; } break;
			case "initial_direction": { configs+= "\"initial_direction\":"+ "\""+initial_direction+"\","; } break;
			case "directions":
				{ 
					configs+= "\"directions\":[";
					for(int i = 0; i<directions.Length; i++)
					{
						configs+= "\""  + directions[i] + "\"";
						if(i<directions.Length-1) { configs+=","; }
					}
					configs+="],";
				}
			break;
			case "accepted_types":
				{ 
					configs+= "\"accepted_types\":[";
					for(int i = 0; i<accepted_types.Length; i++)
					{
						configs+= "\""  + accepted_types[i] + "\"";
						if(i<accepted_types.Length-1) { configs+=","; }
					}
					configs+="],";
				}
				break;
			case "accepted_colors":
				{ 
					configs+= "\"accepted_colors\":[";
					for(int i = 0; i<accepted_colors.Length; i++)
					{
						configs+= "\""  + accepted_colors[i] + "\"";
						if(i<accepted_colors.Length-1) { configs+=","; }
					}
					configs+="],";
				}
				break;
			case "directions_types":
				{ 
					configs+= "\"directions_types\":[";
					for(int directionIndex = 0; directionIndex<directions_types.Count; directionIndex++)
					{
						configs+="[";
						for(int typeIndex = 0; typeIndex < directions_types[directionIndex].Count; typeIndex++)
						{
							configs+= "\"" + directions_types[directionIndex][typeIndex] + "\"";
							if( typeIndex != directions_types[directionIndex].Count-1 ) {configs+=",";}
						}
						configs+="]";
						if(directionIndex != directions_types.Count-1){configs+=",";}
					}
					configs+="],";
					Debug.Log( configs );
				}
				break;
			case "directions_colors":
				{ 
					configs+= "\"directions_colors\":[";
					if(directions_colors.Count!=4)
					{
						configs+= "[],[],[],[]";
					}
					else
					{
						for(int directionIndex = 0; directionIndex<directions_colors.Count; directionIndex++)
						{
							configs+="[";
							for(int typeIndex = 0; typeIndex < directions_colors[directionIndex].Count; typeIndex++)
							{
								configs+= directions_colors[directionIndex][typeIndex] ;
								if( typeIndex != directions_colors[directionIndex].Count-1 ) {configs+=",";}
							}
							configs+="]";
							if(directionIndex != directions_colors.Count-1){configs+=",";}
						}
					}


					configs+="],";
					Debug.Log( configs );
				}
				break;
			case "capacity": { configs+= "\"capacity\":"+capacity+","; } break;
			case "consumer": { configs+= "\"consumer\":"+consumer+","; } break;
			case "delay": { configs+= "\"delay\":"+delay+","; } break;
			case "time_pickup_min": { configs+= "\"time_pickup_min\":"+time_pickup_min+","; } break;
			case "time_delivery_min":break;
			case "time_exchange_min": { configs+= "\"time_exchange_min\":"+time_exchange_min+","; } break;
			case "time_pickup_max": { configs+= "\"time_pickup_max\":"+time_pickup_max+","; } break;
			case "time_delivery_max": { configs+= "\"time_delivery_max\":"+time_delivery_max+","; } break;
			case "time_exchange_max": { configs+= "\"time_exchange_max\":"+time_exchange_max+","; } break;
			case "picked": { configs+= "\"picked\":"+picked+","; } break;
			case "delivered":  { configs+= "\"delivered\":"+delivered+","; } break;
			case "missed": { configs+= "\"missed\":"+missed+","; } break;
			case "link": { configs+= "\"link\":"+link+","; } break;
			case "value": { configs+= "\"value\":"+value+","; } break;
			
			case "passed": { configs+= "\"passed\":"+passed+","; } break;
			case "strict": { configs+= "\"strict\":"+strict+","; } break;
			case "current": { configs+= "\"current\":"+current+","; } break;
			case "direction": { configs+= "\"direction\":\""+direction+"\","; } break;
			case "denominator": { configs+= "\"denominator\":"+denominator+","; } break;
				
			default: Debug.LogWarning("No value for configuration string: " + s ); break;
			}
		}
		if(configs[configs.Length-1] == ',')
		{
			configs = configs.Substring(0, configs.Length-1);
		}
		configs+="}";
		return configs;
	}
}
