using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class DataManager : MonoBehaviour {
	public string foldername = "Levels/";
	public string levelname = "level01";
	public string fileExtension = ".txt";
	string data;

	public Level currentLevelData;

	public List<Object> allLevels;
	public List<string> allLevelNames;

    public bool debugLevelLoader;
    [SerializeField] public LevelReference levRef;
    
	void Start()
	{
		GetLevels();
	}

	public void GetLevels()
	{
        TextAsset lr_text = null;

        // Try
        if (!GameManager.Instance.is_demo_build)
        {
            lr_text = Resources.Load("LevelLoadSelection") as TextAsset;
        }
        else
        {
            lr_text = Resources.Load("DemoLoadSelection") as TextAsset;
        }

        // Change Loading
        GetLevels(lr_text.text);
	}

    public void GetLevels(string inputJson)
    {
        allLevels.Clear();

        levRef = JsonUtility.FromJson<LevelReference>(inputJson);

        //Load pre-existing scores
        GameManager.Instance.GetScoreManager().LoadScores();

        //link level int ids to each level reference object
        foreach (LevelReferenceObject lrObj in levRef.levels.required)
        {
            if (lrObj.levelId == -1)
            {
                lrObj.levelId = GetLevelId(lrObj.file);
            }
            lrObj.completionRank = GameManager.Instance.GetScoreManager().GetCalculatedScore(lrObj.levelId);
        }
        foreach (LevelReferenceObject lrObj in levRef.levels.previous)
        {
            if (lrObj.levelId == -1)
            {
                lrObj.levelId = GetLevelId(lrObj.file);
            }
            lrObj.completionRank = GameManager.Instance.GetScoreManager().GetCalculatedScore(lrObj.levelId);
        }
        foreach (LevelReferenceObject lrObj in levRef.levels.optional)
        {
            if (lrObj.levelId == -1)
            {
                lrObj.levelId = GetLevelId(lrObj.file);
            }
            lrObj.completionRank = GameManager.Instance.GetScoreManager().GetCalculatedScore(lrObj.levelId);
        }


        Object[] loadedObjects = Resources.LoadAll("Levels");
        foreach (Object o in loadedObjects)
        {
            //TextAsset t = o as TextAsset;
            allLevels.Add(o);
            allLevelNames.Add(o.name);
 
        }
        Debug.Log(allLevels.Count + " is all files in resources folder");
    }

    int GetLevelId(string levelFileName)
    {
        TextAsset bindata = Resources.Load(foldername + levelFileName) as TextAsset;
        string[] bindata_split = bindata.ToString().Split('\n');
        currentLevelData = LoadLevel(bindata_split);
        List<string> metadataList = GetSectionData("METADATA", bindata_split);
        Metadata m = ParseMetadata(metadataList);
        return m.level_id;
    }

    public float GetLevelEfficiency(string levelFileName)
    {
        TextAsset bindata = Resources.Load(foldername + levelFileName) as TextAsset;
        string[] bindata_split = bindata.ToString().Split('\n');
        currentLevelData = LoadLevel(bindata_split);
        List<string> metadataList = GetSectionData("METADATA", bindata_split);
        Metadata m = ParseMetadata(metadataList);
        Debug.Log(m.level_id);
        Debug.Log(m.time_efficiency);
        return m.time_efficiency;
    }

    public LevelReferenceObject GetLevelById(int id)
    {
        foreach (LevelReferenceObject l in levRef.levels.required)
        {
            if (l.levelId == id) { return l; }
        }
        foreach (LevelReferenceObject l in levRef.levels.previous)
        {
            if (l.levelId == id) { return l; }
        }
        foreach (LevelReferenceObject l in levRef.levels.optional)
        {
            if (l.levelId == id) { return l; }
        }
        Debug.LogWarning("No level with id: " + id + " found");
        return null;
    }

    public LevelReferenceObject GetLevelByFile(string file)
    {
        foreach (LevelReferenceObject l in levRef.levels.required)
        {
            if (l.file == file) { return l; }
        }
        foreach (LevelReferenceObject l in levRef.levels.previous)
        {
            if (l.file == file) { return l; }
        }
        foreach (LevelReferenceObject l in levRef.levels.optional)
        {
            if (l.file == file) { return l; }
        }
        Debug.LogWarning("No level with file: " + file +  " found");
        return null;
    }

    public LevelReferenceObject GetNextLevel(LevelReferenceObject currentLevelRef)
    {
        bool levelFound = false;
        foreach (LevelReferenceObject l in levRef.levels.required)
        {
            if(levelFound) return l;
            if (l.levelId == currentLevelRef.levelId) { levelFound = true; }
        }
        foreach (LevelReferenceObject l in levRef.levels.previous)
        {
            if (levelFound) return l;
            if (l.levelId == currentLevelRef.levelId) { levelFound = true; }
        }
        foreach (LevelReferenceObject l in levRef.levels.optional)
        {
            if (levelFound) return l;
            if (l.levelId == currentLevelRef.levelId) { levelFound = true; }
        }
        Debug.LogError("No level after " + currentLevelRef.file + " found");
        return null;
    }

    public enum LoadType
    {
        RESOURCES,
        FILENAME,
        FILEPATH
    }
   
	public void InitializeLoadLevel(string inputLevelName, LoadType loadType)
	{
		levelname = inputLevelName;
        string[] bindata_split = new string[0];

        switch (loadType)
        {
            case LoadType.RESOURCES:
                TextAsset bindata = Resources.Load(foldername + levelname) as TextAsset;
                bindata_split = bindata.ToString().Split('\n');
                break;
            case LoadType.FILENAME:
                bindata_split = System.IO.File.ReadAllLines(Application.dataPath + "/" + inputLevelName);
                break;

            case LoadType.FILEPATH:
                bindata_split = System.IO.File.ReadAllLines(inputLevelName);
                break;
        }
        currentLevelData = LoadLevel(bindata_split);
    }

    public void UpdateLevelRank(string inputLevelFile, int inputNewRank)
    {
        bool isLevelFound = false;
        GetLevelByFile(inputLevelFile).completionRank = inputNewRank;

        if (isLevelFound) {
            Debug.Log("Located file " + inputLevelFile);
            return;
        }
        Debug.Log("Could not locate level " + inputLevelFile + " in level references of Data Manager.");
    }

    Level LoadLevel(string[] levelDataArray)
	{
		Level returnLevel = new Level();

		string[] bindata_split = levelDataArray;

		returnLevel.metadataList = GetSectionData("METADATA", bindata_split);
		returnLevel.layoutList = GetSectionData("LAYOUT", bindata_split);
		returnLevel.colorList = GetSectionData("COLORS", bindata_split);
		returnLevel.directionList = GetSectionData("DIRECTIONS", bindata_split);
		returnLevel.componentList = GetSectionData("COMPONENTS", bindata_split);
		returnLevel.executionList = GetSectionData("EXECUTION", bindata_split);

		returnLevel.metadata = ParseMetadata( returnLevel.metadataList );
		returnLevel.components = ParseComponents( returnLevel.componentList );
		returnLevel.tracks = ParseTracks( returnLevel.layoutList, returnLevel.colorList, returnLevel.directionList );
		returnLevel.execution = ParseExecution( returnLevel.executionList );
		//Debug.Log( returnLevel.metadata.goal_struct.desired[0].id );

		return returnLevel;
	}


	List<string> GetSectionData(string sectionName, string[] bindata_split)
	{
		List<string> returnStringList = new List<string>();

		bool isSection = false;
		foreach(string bindata_line in bindata_split)
		{
            string comparison = "";
            if (sectionName.Length <= bindata_line.Length) comparison = bindata_line.Substring(0, sectionName.Length);
            if (string.Compare(comparison, sectionName)==0)
			{
                //Debug.Log("I've found the section " + sectionName);
				isSection = true;
				continue;
			}
			if(isSection)
			{
				if(bindata_line == "\n" || bindata_line == "\r" || bindata_line.Length==0) 
				{
					isSection = false;
				}
				else
				{
					returnStringList.Add(bindata_line);	
				}
			}
		}
        //Debug.Log("Section " + sectionName + " is length " + returnStringList.Count);
		return returnStringList;
	}

	Metadata ParseMetadata(List<string> inputMetadataList)
	{
		Metadata returnMetadata = new Metadata();
		/*
		level_id	3
		level_title	Mutex
		goal_string	Deliver 4 packages. Extra bonus for no starvation and no race conditions.
			goal_struct	{"required":[{"id":1002,"type":"delivery","property":"delivered","value":4,"condition":"eq"},],}
		player_palette	{"semaphore":{"count":3},"signal":{"count":3},"painter":{"count":0},"colors":{"count":1},}
		time_pickup_min		0
		time_pickup_max		0
		time_delivery_min	1
		time_delivery_max	5
		time_exchange_min	0
		time_exchange_max	0
		board_width	27
		board_height	21
		
		*/

		foreach(string s in inputMetadataList)
		{
			string[] sLine = s.Split('\t');

			switch(sLine[0])
			{
			case "level_id":
				returnMetadata.level_id = int.Parse(sLine[1]);
				break;
			case "level_title":
				returnMetadata.level_title = sLine[1];
				break;
			case "goal_string":
				returnMetadata.goal_string = sLine[1];
				break;
			case "goal_struct":
				//Debug.Log( sLine[1] );
				returnMetadata.goal_struct = JsonUtility.FromJson<GoalCondition>(sLine[1]);
				//Debug.Log(sLine[1] + "\n" + returnMetadata.goal_struct.desired.Length + "\n" + returnMetadata.goal_struct.required.Length);
				break;
			case "player_palette":
				//returnMetadata.player_palette = sLine[1];
				returnMetadata.player_palette = JsonUtility.FromJson<PlayerPalette>(sLine[1]);
				break;
			case "time_delivery_min":
				returnMetadata.time_delivery_min = int.Parse(sLine[1]);
				break;
			case "time_delivery_max":
				returnMetadata.time_exchange_max = int.Parse(sLine[1]);
				break;
			case "time_pickup_min":
				returnMetadata.time_pickup_min = int.Parse(sLine[1]);
				break;
			case "time_pickup_max":
				returnMetadata.time_pickup_max = int.Parse(sLine[1]);
				break;
			case "time_exchange_min":
				returnMetadata.time_exchange_min = int.Parse(sLine[1]);
				break;
			case "time_exchange_max":
				returnMetadata.time_exchange_max = int.Parse(sLine[1]);
				break;
			case "board_width":
				returnMetadata.board_width = int.Parse(sLine[1]);
				break;
			case "board_height":
				returnMetadata.board_height = int.Parse(sLine[1]);
				break;
            case "time_efficiency":
                returnMetadata.time_efficiency = float.Parse(sLine[1]);
                break;
			}
		}

		return returnMetadata;
	}

	List<GridComponent> ParseComponents(List<string> inputComponentStringList)
	{
		/*
		1001	thread	5	3	S	L	{"color":1,"initial_direction":"East","capacity":1,"delay":0,"picked":0,"delivered":0,"missed":0}
		1002	thread	13	3	S	L	{"color":3,"initial_direction":"West","capacity":1,"delay":0,"picked":0,"delivered":0,"missed":0}
		2001	pickup	9	9	S	L	{"type":"Conditional","color":0,"picked":0,"passed":0}
		3001	delivery	9	12	S	L	{"color":0,"consumer":-1,"strict":0,"passed":0,"delivered":0,"missed":0}
		4001	intersection	9	14	S	L	{"directions":["North","West","North","East","North","North"],"passed":0}
		*/
		List<GridComponent> returnComponents = new List<GridComponent>();
		foreach(string row in inputComponentStringList)
		{
			GridComponent gc = new GridComponent();
			string[] s = row.Split('\t');
			gc.id = int.Parse(s[0]);
			gc.type = s[1];
			gc.posX =  int.Parse(s[2]);
			gc.posY =  int.Parse(s[3]);
			gc.placedBy = s[4];
			gc.editable = s[5];

			/*CONFIGURATION DECODING NOT WORKING*/
			/*TODO: TRANSLATE TO JSONObject*/
			try 
			{
				gc.configuration = JsonUtility.FromJson<Configuration>( s[6] );
				JSONObject ob = new JSONObject( s[6] );
				if(ob.GetField("directions_types")!=null) 
				{
					List<string> directions_types_west = ob.GetField ("directions_types").list[0].list.ConvertAll(x => x.str);
					List<string> directions_types_north = ob.GetField ("directions_types").list[3].list.ConvertAll(x => x.str);
					List<string> directions_types_east = ob.GetField ("directions_types").list[2].list.ConvertAll(x => x.str);
					List<string> directions_types_south = ob.GetField ("directions_types").list[1].list.ConvertAll(x => x.str);

					gc.configuration.directions_types.Add(directions_types_west);
					gc.configuration.directions_types.Add(directions_types_south);
					gc.configuration.directions_types.Add(directions_types_east);
					gc.configuration.directions_types.Add(directions_types_north);
                    /*
					Debug.Log( directions_types_west.Count + " IS WEST COUNT " );
					Debug.Log( directions_types_south.Count + " IS SOUTH COUNT " );
					Debug.Log( directions_types_east.Count + " IS EAST COUNT " );
					Debug.Log( directions_types_north.Count + " IS NORTH COUNT " );
                    */
				}

				if(ob.GetField("directions_colors")!=null) 
				{
					if( ob.GetField ("directions_colors").list.Count>0) 
					{
						List<int> directions_colors_west = ob.GetField ("directions_colors").list[0].list.ConvertAll(x => (int)x.i);
						List<int> directions_colors_north = ob.GetField ("directions_colors").list[3].list.ConvertAll(x =>(int)x.i);
						List<int> directions_colors_east = ob.GetField ("directions_colors").list[2].list.ConvertAll(x => (int)x.i);
						List<int> directions_colors_south = ob.GetField ("directions_colors").list[1].list.ConvertAll(x =>(int)x.i);
						gc.configuration.directions_colors.Add(directions_colors_west);
						gc.configuration.directions_colors.Add(directions_colors_south);
						gc.configuration.directions_colors.Add(directions_colors_east);
						gc.configuration.directions_colors.Add(directions_colors_north);
					}
				}
				if(ob.GetField("accepted_colors")!=null) 
				{
					if( ob.GetField ("accepted_colors").list.Count>0) 
					{
						List<int> accepted_colors = ob.GetField("accepted_colors").list.ConvertAll(x => (int)x.i);
						gc.configuration.accepted_colors = accepted_colors.ToArray();

					}
				}

			}
			catch
			{
				Debug.LogError("Error with json utility: " + s[6]);
				JSONObject j = new JSONObject(s[6]);
				gc.configuration = new Configuration();
			}

			returnComponents.Add( gc );

		}
		return returnComponents;
	}

	List<GridTrack> ParseTracks(List<string> inputLayoutList, List<string> inputColorList, List<string> inputDirectionList)
	{
		List<GridTrack> returnTracks = new List<GridTrack>();

		bool useColorList = true;
		bool useDirectionList = true;

		if( inputColorList.Count==0 || inputColorList[0]=="N/A" || inputColorList.Count < inputLayoutList.Count ) 
		{
			useColorList = false;
		}

		if( inputDirectionList.Count==0 || inputDirectionList[0]=="N/A" || inputDirectionList.Count < inputLayoutList.Count ) 
		{
			useDirectionList = false;
		}


		int rowIndex = 0;
		foreach(string row in inputLayoutList)
		{
			int columnIndex =0;
			foreach(char track in row)
			{
                if (track != '\n' && track != '\r')
                {
                    GridTrack newTrack = new GridTrack();
                    newTrack.type = track;

                    newTrack.position = new Vector2(columnIndex, rowIndex);

                    char colorMask = ' ';
                    if (useColorList)
                    {
                        colorMask = inputColorList[rowIndex][columnIndex];
                    }
                    newTrack.SetBitmask(colorMask + "");

                    if (useDirectionList)
                    {
                        if (inputDirectionList.Count > rowIndex && inputDirectionList[rowIndex].Length > columnIndex)
                        {
                            newTrack.direction = inputDirectionList[rowIndex][columnIndex] + "";
                        }
                        else
                        {
                            newTrack.direction = null;
                        }

                    }

                    columnIndex++;
                    returnTracks.Add(newTrack);
                }

                
			}
			rowIndex++;
		}

		return returnTracks;
	}

	List<StepData> ParseExecution(List<string> inputExecutionList)
	{
		List<StepData> steps = new List<StepData>();
		foreach(string row in inputExecutionList)
		{
			StepData newStep = new StepData();
			string[] s = row.Split('\t');
			newStep.eventType = (s[0]);
			newStep.timeStep = int.Parse(s[1]);
			newStep.componentID =  int.Parse(s[2]);
			newStep.componentPos =  new Vector3( int.Parse(s[3]), int.Parse(s[4]));
			newStep.componentStatus = new Status();
			try{
				newStep.componentStatus = JsonUtility.FromJson<Status>( s[5] );
			} catch (System.Exception e) {
                Debug.Log("Error loading status");
            }
			

			steps.Add( newStep );

		}
		return steps;

	}

	public string GetLevelJson()
	{
		string levelDataString = "";

		/*
METADATA
level_id	3
level_title	Introduction (3) - Prototype
goal_string	Get each car to deliver one package.
goal_struct	{"required":[{"id":3001,"type":"delivery","property":"delivered","value":3,"condition":"gt"}],"desired":[{"id":1001,"type":"thread","property":"delivered","value":1,"condition":"gt"},{"id":1002,"type":"thread","property":"delivered","value":1,"condition":"gt"}]}
player_palette	{"semaphore":{"count":3},"signal":{"count":3},"painter":{"count":0},"colors":{"count":0}}
time_pickup_min	0
time_delivery_min	1
time_exchange_min	0
time_pickup_max	0
time_delivery_max	3
time_exchange_max	0
board_width	19
board_height	15
		*/
		string metadataFormat = "";//JsonUtility.ToJson( currentLevelData.metadata );
		metadataFormat = 
			"level_id\t{0}"+
			"\nlevel_title\t{1}"+
			"\ngoal_string\t{2}"+
			"\ngoal_struct\t{3}"+
			"\nplayer_palette\t{4}"+
			"\ntime_pickup_min\t{5}"+
			"\ntime_delivery_min\t{6}"+
			"\ntime_exchange_min\t{7}"+
			"\ntime_pickup_max\t{8}"+
			"\ntime_delivery_max\t{9}"+
			"\ntime_exchange_max\t{10}"+
			"\nboard_width\t{11}"+
			"\nboard_height\t{12}";

		string metadataJson = string.Format(metadataFormat, 
			currentLevelData.metadata.level_id, 
			currentLevelData.metadata.level_title,
			currentLevelData.metadata.goal_string,
			JsonUtility.ToJson (currentLevelData.metadata.goal_struct),
			JsonUtility.ToJson (currentLevelData.metadata.player_palette),
			currentLevelData.metadata.time_pickup_min,
			currentLevelData.metadata.time_delivery_min,
			currentLevelData.metadata.time_exchange_min,
			currentLevelData.metadata.time_pickup_max,
			currentLevelData.metadata.time_delivery_max,
			currentLevelData.metadata.time_exchange_max,
			currentLevelData.metadata.board_width,
			currentLevelData.metadata.board_height
		);

		string layoutJson = "";
		foreach(string s in currentLevelData.layoutList)
		{
			layoutJson+=s;
			layoutJson+='\n';
		}

		string colorJson = "";
		foreach(string s in currentLevelData.colorList)
		{
			colorJson+= s + '\n';
		}

		string directionJson = "";
		foreach(string s in currentLevelData.directionList)
		{
			directionJson+= s + '\n';
		}

		/*
9	14	S	L	{"directions":["North","West","North","East","North","North"],"passed":0}
6001	intersection	9	3	S	L	{"directions":["South","South","South","South","South","South"],"passed":0}
4001	semaphore	8	3	P	E	{"value":1}
4002	semaphore	10	3	P	E	{"value":0}
5001	signal	8	14	S	L	{"link":4002,"passed":0}
5002	signal	10	14	S	L	{"link":4001,"passed":0}
		*/
		string componentFormat ="{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\n";
		string componentString = "";


		foreach(GridObjectBehavior gridObject in GameObject.FindObjectsOfType<GridObjectBehavior>())
		{
			if(gridObject.behaviorType == GridObjectBehavior.BehaviorTypes.component)
			{
				componentString += string.Format(componentFormat,
					gridObject.component.id, 
					gridObject.component.type, 
					gridObject.component.posX, 
					gridObject.component.posY, 
					gridObject.component.placedBy,
					gridObject.component.editable,
					//JsonUtility.ToJson(gridObject.component.configuration)
					gridObject.component.GetConfigurationString()
				);
			}
		}


		levelDataString = 
			"METADATA\n" + metadataJson
			+ "\n\nLAYOUT\n" + layoutJson
			+ "\nCOLORS\n" + colorJson
			+ "\nDIRECTIONS\n" + directionJson
			+ "\nCOMPONENTS\n" + componentString + "\nEXECUTION\n\nPLAYER\n";

		return levelDataString;
	}

}
