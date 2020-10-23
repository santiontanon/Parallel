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
        string text = "";
        // Try
		TextAsset lr_text = null;
		switch (GameManager.Instance.currentGameMode) {
			case GameManager.GameMode.Test:
				lr_text = Resources.Load("TestLoadSelection") as TextAsset;
				text = lr_text.text;
				break;
			case GameManager.GameMode.Class:
				if(GameManager.Instance.tracker.level_data != "")
				{
					text = GameManager.Instance.tracker.level_data;
				}
				else
				{
					if (GameManager.Instance.currentGameMode == GameManager.GameMode.Class)
						lr_text = Resources.Load("ClassLoadSelection") as TextAsset;
					else
						lr_text = Resources.Load("StudyLoadSelection") as TextAsset;
					text = lr_text.text;
				}
				break;
			case GameManager.GameMode.Study:
				if(GameManager.Instance.tracker.level_data != "")
				{
					text = GameManager.Instance.tracker.level_data;
				}
				else
				{
					if (GameManager.Instance.currentGameMode == GameManager.GameMode.Class)
						lr_text = Resources.Load("ClassLoadSelection") as TextAsset;
					else
						lr_text = Resources.Load("StudyLoadSelection") as TextAsset;
					text = lr_text.text;
				}
				break;
			case GameManager.GameMode.Final:
				lr_text = Resources.Load("LevelLoadSelection") as TextAsset;
				text = lr_text.text;
				break;
			default:
				lr_text = Resources.Load("DemoLoadSelection") as TextAsset;
				text = lr_text.text;
				break;
		}
        GetLevels(text);
	}

    public void GetLevels(string inputJson)
    {
        allLevels.Clear();
        levRef = JsonUtility.FromJson<LevelReference>(inputJson);

        //Load pre-existing scores
        //GameManager.Instance.GetScoreManager().LoadScores();

        try{
            //link level int ids to each level reference object
            foreach (LevelReferenceObject lrObj in levRef.levels.required)
            {
                if (lrObj.levelId == -99999)
                {
                    lrObj.levelId = GetLevelId(lrObj.file);
                }
                lrObj.completionRank = GameManager.Instance.GetScoreManager().GetCalculatedScore(lrObj.levelId);
            }
            foreach (LevelReferenceObject lrObj in levRef.levels.previous)
            {
                if (lrObj.levelId == -99999)
                {
                    lrObj.levelId = GetLevelId(lrObj.file);
                }
                lrObj.completionRank = GameManager.Instance.GetScoreManager().GetCalculatedScore(lrObj.levelId);
            }
            foreach (LevelReferenceObject lrObj in levRef.levels.optional)
            {
                if (lrObj.levelId == -99999)
                {
                    lrObj.levelId = GetLevelId(lrObj.file);
                }
                lrObj.completionRank = GameManager.Instance.GetScoreManager().GetCalculatedScore(lrObj.levelId);
            }

            GetPCGLevels(GameManager.Instance.GetSaveManager().currentSave.pcgLevels);
        }
        catch (System.Exception e)
        {
            Debug.Log(e);
            TextAsset lr_text = null;
            switch (GameManager.Instance.currentGameMode)
            {
                case GameManager.GameMode.Class:
                    lr_text = Resources.Load("ClassLoadSelection") as TextAsset;
                    break;
                case GameManager.GameMode.Demo:
                    lr_text = Resources.Load("DemoLoadSelection") as TextAsset;
                    break;
                case GameManager.GameMode.Test:
                    lr_text = Resources.Load("TestLoadSelection") as TextAsset;
                    break;
                case GameManager.GameMode.Study:
                    lr_text = Resources.Load("StudyLoadSelection") as TextAsset;
                    break;
				case GameManager.GameMode.Final:
                    lr_text = Resources.Load("LevelLoadSelection") as TextAsset;
					break;
            }
            GetLevels(lr_text.text);
        }
    }

    public void GetPCGLevels(List<string> levels)
    {
        if(levels != null)
        {
            levRef.levels.pcg.Clear();
            List<LevelReferenceObject> refs = new List<LevelReferenceObject>();
            for (int i = 0; i < levels.Count; i++)
            {
                if (levels[i] != "")
                {
                    LevelReferenceObject lro = new LevelReferenceObject();
                    lro.file = "levelX";
                    lro.title = "X";
                    if (i < 10)
                    {
                        lro.file += 0;
                        lro.title += 0;
                    }
                    lro.file += i;
                    lro.title += i;
                    lro.data = GameManager.Instance.GetSaveManager().currentSave.pcgLevels[i];
                    lro.levelId = i;
                    lro.completionRank = 0;
                    refs.Add(lro);
                }
            }
            levRef.levels.pcg = refs;
        }
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
        FILEPATH,
        STRING
    }
   
	public void InitializeLoadLevel(string inputString, LoadType loadType)
	{
		levelname = inputString;
        string[] bindata_split = new string[0];

        switch (loadType)
        {
            case LoadType.RESOURCES:
                TextAsset bindata = Resources.Load(foldername + levelname) as TextAsset;
                bindata_split = bindata.ToString().Split('\n');
                break;
            case LoadType.FILENAME:
                bindata_split = System.IO.File.ReadAllLines(Application.dataPath + "/" + inputString);
                break;
            case LoadType.FILEPATH:
                bindata_split = System.IO.File.ReadAllLines(inputString);
                break;
            case LoadType.STRING:
                bindata_split = inputString.Split('\n');
                break;
        }
        currentLevelData = LoadLevel(bindata_split);
    }

    public void UpdateLevelRank(string inputLevelFile, int inputNewRank)
    {
        if(GetLevelByFile(inputLevelFile) != null)
            GetLevelByFile(inputLevelFile).completionRank = inputNewRank;
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
        returnLevel.skillList = GetSectionData("SKILLS", bindata_split);

		returnLevel.metadata = ParseMetadata( returnLevel.metadataList );
		returnLevel.components = ParseComponents( returnLevel.componentList );
		returnLevel.tracks = ParseTracks( returnLevel.layoutList, returnLevel.colorList, returnLevel.directionList );
		returnLevel.execution = ParseExecution( returnLevel.executionList );

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
                case "pcg_id":
                    returnMetadata.pcg_id = sLine[1];
                    break;
                case "level_title":
                    returnMetadata.level_title = sLine[1];
                    if (returnMetadata.level_title == "PCG Level")
                    {
                        returnMetadata.level_id = -1;
                    }
                    break;
                case "goal_string":
                    returnMetadata.goal_string = sLine[1];
                    break;
                case "goal_struct":
                    returnMetadata.goal_struct = JsonUtility.FromJson<GoalCondition>(sLine[1]);
                    break;
                case "player_palette":
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
            "level_id\t{0}" +
            "\npcg_id\t{1}" +
            "\nlevel_title\t{2}" +
            "\ngoal_string\t{3}" +
            "\ngoal_struct\t{4}" +
            "\nplayer_palette\t{5}" +
            "\ntime_pickup_min\t{6}" +
            "\ntime_delivery_min\t{7}" +
            "\ntime_exchange_min\t{8}" +
            "\ntime_pickup_max\t{9}" +
            "\ntime_delivery_max\t{10}" +
            "\ntime_exchange_max\t{11}" +
            "\nboard_width\t{12}" +
            "\nboard_height\t{13}";

        string metadataJson = string.Format(metadataFormat,
            currentLevelData.metadata.level_id,
            currentLevelData.metadata.pcg_id,
            currentLevelData.metadata.level_title,
            currentLevelData.metadata.goal_string,
            JsonUtility.ToJson(currentLevelData.metadata.goal_struct),
            JsonUtility.ToJson(currentLevelData.metadata.player_palette),
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
        foreach (string s in currentLevelData.layoutList)
        {
            layoutJson += s;
            layoutJson += '\n';
        }

        string colorJson = "";
        foreach (string s in currentLevelData.colorList)
        {
            colorJson += s + '\n';
        }

        string directionJson = "";
        foreach (string s in currentLevelData.directionList)
        {
            directionJson += s + '\n';
        }

        string skillJson = "";
        foreach (string s in currentLevelData.skillList)
        {
            skillJson += s + '\n';
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
            + "\nSKILLS\n" + skillJson
            + "\nCOMPONENTS\n" + componentString
            + "\nEXECUTION\n\nPLAYER\n";

		return levelDataString;
	}

}
