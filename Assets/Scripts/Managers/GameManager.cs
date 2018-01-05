using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class GameManager : MonoBehaviour {
	public static GameManager Instance = null;

	public enum GamePhases {StartScreen, LoadScreen, GenerateTrack, PlayerInteraction, GradeSubmission, GradeReport, EndScreen, CloseGame}
	public GamePhases gamePhase = GamePhases.StartScreen;
	public GamePhaseBehavior startScreenBehavior, loadScreenBehavior, generateTrackBehavior, playerInteractionBehavior, gradeSubmissionBehavior, gradeReportBehavior, endScreenBehavior, exitGameBehavior;
	public bool hideTestsForBuild = false;
	GamePhaseBehavior currentPhase;

    //FOR DEBUG, REMOVE THIS LATER
    string lastPhase = "";

	public Tracks trackGrid;

	DataManager dataManager;
	GridManager gridManager;
	TutorialManager tutorialManager;
	InventoryManager inventoryManager;
	LinkJava linkJava;
	public Tracker tracker;

    

    void Awake()
	{
		if(Instance == null)
		{
			Instance = this;
			DontDestroyOnLoad(this);
		}

		else
		{
			Destroy(this);
		}

		dataManager = GetComponent<DataManager>();
		gridManager = GetComponent<GridManager>();
		tutorialManager = GetComponent<TutorialManager>();
		inventoryManager = GetComponent<InventoryManager> ();
		linkJava = GetComponent<LinkJava>();
		tracker = GetComponent<Tracker>();
	}

	void Start()
	{
		SetGamePhase(GamePhases.StartScreen);
	}

	public void SetGamePhase(GamePhases inputPhase)
	{
		if(currentPhase!=null) { EndGamePhaseBehavior(); }

		gamePhase = inputPhase;
		switch(gamePhase)
		{
			case GamePhases.StartScreen:
				currentPhase = startScreenBehavior;
			break;
			case GamePhases.LoadScreen:
                Debug.Log("LOAD SCREEN PHASE");
				currentPhase = loadScreenBehavior;
				tutorialManager.tutorialIndex = 0;
                tutorialManager.ResetTutorialCompletionData();
			break;
			case GamePhases.GenerateTrack:
				currentPhase = generateTrackBehavior;
			break;
			case GamePhases.PlayerInteraction:
				currentPhase = playerInteractionBehavior;
			break;
			case GamePhases.GradeSubmission:
				currentPhase = gradeSubmissionBehavior;
			break;
			case GamePhases.GradeReport:
				currentPhase = gradeReportBehavior;
			break;
			case GamePhases.EndScreen:
				currentPhase = endScreenBehavior;
			break;
            case GamePhases.CloseGame:
                currentPhase = exitGameBehavior;
                break;
        }

		BeginGamePhaseBehavior();
	}

	void BeginGamePhaseBehavior()
	{
		currentPhase.BeginPhase();
	}

	void EndGamePhaseBehavior()
	{
        Debug.Log("Ending phase " + gamePhase.ToString());
		switch( gamePhase )
		{
		case GamePhases.PlayerInteraction:
			gridManager.ClearGrid();
            tutorialManager.ClearActiveTutorials();
		break;
		}
		currentPhase.EndPhase();
	}

	void UpdateGamePhaseBehavior()
	{
		currentPhase.UpdatePhase();
	}

	void Update()
	{
        PlayerInteraction_GamePhaseBehavior p = (PlayerInteraction_GamePhaseBehavior)playerInteractionBehavior;
        string currPhase = p.interactionPhase.ToString();
        if (!currPhase.Equals(lastPhase))
        {
            lastPhase = currPhase;
            Debug.Log(currPhase);
        }

		UpdateGamePhaseBehavior();
	}

	public void UpdatePlayerField(string inputPlayerId)
	{
		Debug.Log("Player ID is now:" + inputPlayerId);
		PlayerPrefs.SetString("PlayerId", inputPlayerId);
	}


	public void TriggerLoadLevel(string inputLevelName = "")
	{
		tracker.CreateEventExt("TriggerLoadLevel",inputLevelName);
		if(inputLevelName.Length == 0) inputLevelName = dataManager.levelname;
		dataManager.InitializeLoadLevel( inputLevelName );
        //TODO: Since this gets called when a simulation completes and re-opens everything, it can cause bugs with the tutorials loading
		SetGamePhase(GameManager.GamePhases.GenerateTrack);
	}

    public void TriggerLoadTutorialLevel(string path)
    {
        tracker.CreateEventExt("TriggerLoadLevel", path);
        if (path.Length == 0) path = dataManager.levelname;
        dataManager.InitializeLoadLevel(path, true);
    }

    public void TriggerPCG(string inputLevelName = "")
	{
		tutorialManager.tutorialIndex = -1;
		string filename = Application.persistentDataPath + linkJava.pathSeparator + "currentParameters.txt";
		System.IO.File.WriteAllText(filename, "");
		tracker.CreateEventExt("TriggerPCG",filename);
		linkJava.filename = filename;
		//LinkJava.OnSimulationCompleted += TriggerLevelSimulation;
		linkJava.simulationMode = LinkJava.SimulationTypes.PCG;
		linkJava.SendToME();
		//SetGamePhase(GameManager.GamePhases.GenerateTrack);
	}

	public void InitiateTrackGeneration()
	{
		gridManager.GenerateGrid(/*dataManager.currentLevelData.layoutList,*/ dataManager.currentLevelData.tracks, dataManager.currentLevelData.components);
	}


	public void TriggerTrackUpdate()
	{
		//trackGrid.UpdateTracks();
	}

	public void TriggerLevelTutorial(int inputLevelId, TutorialEvent.TutorialInitializeTriggers inputPlayPhase = TutorialEvent.TutorialInitializeTriggers.beforePlay)
	{
		tutorialManager.PerformTutorialSeries( inputLevelId, inputPlayPhase);
	}

    public void CreateTutorialPopup(TutorialEvent t, Vector3 position)
    {
        //two versions, depending on if we want to set the position at the same time as setting the pop up to open
        tutorialManager.tutorialOverlay.PrepareTutorialPopup(t.popupDescription, position);
        tutorialManager.tutorialOverlay.tutorialCloseButton.onClick.AddListener(() => tutorialManager.tutorialOverlay.ClosePanel() );
        tutorialManager.tutorialOverlay.OpenPanel();
    }

    public void SetTutorialPopupClickToClose(TutorialEvent t, bool shouldClickToClose)
    {
        tutorialManager.tutorialOverlay.tutorialCloseButton.onClick.RemoveAllListeners();
        tutorialManager.tutorialOverlay.tutorialCloseButton.enabled = shouldClickToClose;
        if (shouldClickToClose) tutorialManager.tutorialOverlay.tutorialCloseButton.onClick.AddListener(() => t.TriggerTutorialEventListener());
    }

    public void ReportTutorialEventComplete(TutorialEvent t)
    {
        tutorialManager.ReportTutorialEventComplete(t);
    }

    public void PlayTutorialLevel()
    {
        string levelToString = SerializeCurrentLevel();
        Debug.Log(levelToString);
        string filename =
            Application.persistentDataPath
            + linkJava.pathSeparator
            + Constants.FilePrefixes.inputLevelFile + "_PLAY_"
            + System.DateTime.Now.ToString("yyyyMMddHHmmss") + ".txt";
        linkJava.simulationMode = LinkJava.SimulationTypes.Play;
        GameManager.Instance.tracker.CreateEventExt("SubmitCurrentLevel" + "PLAY", filename);

        PlayerInteraction_GamePhaseBehavior castBehavior = (PlayerInteraction_GamePhaseBehavior)playerInteractionBehavior;
        castBehavior.StartSimulation();
    }

	public void SetUpLevelInventory () {
		inventoryManager.SetUpLevelInventory (dataManager.currentLevelData.metadata.level_id);
	}

	public void SubmitCurrentLevel(LinkJava.SimulationTypes inputSimulationType)
	{        
		string levelToString = SerializeCurrentLevel();
		Debug.Log( levelToString );
		string filename = 
            Application.persistentDataPath 
            + linkJava.pathSeparator 
            + Constants.FilePrefixes.inputLevelFile + "_"  + inputSimulationType.ToString().ToUpper() + "_"
            + System.DateTime.Now.ToString("yyyyMMddHHmmss") + ".txt";
        Debug.Log(filename);
        System.IO.File.WriteAllText(filename, levelToString);
		linkJava.filename = filename;
		linkJava.simulationMode = inputSimulationType;
		GameManager.Instance.tracker.CreateEventExt("SubmitCurrentLevel"+inputSimulationType.ToString(),filename);

		LinkJava.OnSimulationCompleted += TriggerLevelSimulation;
		linkJava.SendToME();
	}

	public void TriggerLevelSimulation(LinkJava.SimulationFeedback feedback)
	{
		LinkJava.OnSimulationCompleted -= TriggerLevelSimulation;

        PlayerInteraction_GamePhaseBehavior castBehavior = (PlayerInteraction_GamePhaseBehavior)playerInteractionBehavior;

        //none indicates hitting the replay button
        if (feedback != LinkJava.SimulationFeedback.none)
        {
            Debug.Log("Feedback from LinkJava to GameManager was " + feedback.ToString());
            castBehavior.StartSimulation();
        }

		if(feedback == LinkJava.SimulationFeedback.success || feedback == LinkJava.SimulationFeedback.none) castBehavior.StartSimulation();

		else if(feedback == LinkJava.SimulationFeedback.failure) 
		{
			castBehavior.playerInteraction_UI.simulationErrorOverlay.OpenPanel();
		}
	}

	public string SerializeCurrentLevel()
	{
		string levelJSON = "";
		//levelJSON = JsonUtility.ToJson(dataManager.currentLevelData, true);//dataManager.currentLevelData
		levelJSON = dataManager.GetLevelJson();
		return levelJSON;
	}
		
	public int GetLevelHeight() { return GetDataManager().currentLevelData.metadata.board_height; }
	public int GetLevelWidth() { return GetDataManager().currentLevelData.metadata.board_width; }
	public GridManager GetGridManager(){ return gridManager;}
	public DataManager GetDataManager(){ return dataManager;}
    public LinkJava.SimulationTypes GetCurrentSimulationType() { return linkJava.simulationMode; }

}
