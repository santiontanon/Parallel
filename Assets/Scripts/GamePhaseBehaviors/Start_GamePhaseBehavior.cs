using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System;
using ParallelProg.UI;

public class Start_GamePhaseBehavior : GamePhaseBehavior {
	public GameObject startGameUI;

	public Button gameStart;
	public Button gameEnd;
	public Credit_UIOverlay credits;
    public ParallelProg.UI.UIOverlay fetchConfigInProgressOverlay;
    [System.Serializable] public class StartErrorOverlay : ParallelProg.UI.UIOverlay { public Text errorText; }
    public StartErrorOverlay fetchConfigErrorOverlay;
    public Start_DebugUI debugOverlay;

    public string currentPlayerID;

	public delegate void StartPlayingWithLevelInformationDelegate(string json);
	public delegate void NoInternetErrorDelegate();

	void Start()
	{
		GameManager.Instance.GetSaveManager().LoadSave("input");
		GameManager.Instance.UpdatePlayerField("input");
	//	GameManager.Instance.tracker.StartTrackerWithCallback(LoadRemoteData, null);
		GameManager.Instance.tracker.StartTrackerWithCallback(null, null, "NA", true, false);
		
		
		gameStart.interactable = true;
		gameStart.onClick.AddListener( ()=> {
			StartPlaying(); 
		});
		
		credits.creditButton.interactable = true;
		credits.creditButton.onClick.AddListener(()=> {
			credits.OpenPanel(); 
		});
		
		credits.creditCloseButton.interactable = true;
		credits.creditCloseButton.onClick.AddListener(() => {
			credits.ClosePanel(true);
		});
		
		gameEnd.interactable = true;
		gameEnd.onClick.AddListener( ()=> GameManager.Instance.SetGamePhase(GameManager.GamePhases.CloseGame) );		
	}

	public override void BeginPhase()
	{
		startGameUI.SetActive(true);
    }

    public void LoadRemoteData(string s)
    {
        GameManager.Instance.GetDataManager().GetLevels(s);
    }

	void StartPlaying()
	{
        GameManager.Instance.SetGamePhase(GameManager.GamePhases.LoadScreen);
    }

	public void StartPlayingWithLevelInformation(string json){
		// Get the level selection from the json
		Debug.Log("Get the level selection from the json: "+json);
        //if(!GameManager.Instance.GetDataManager().debugLevelLoader) GameManager.Instance.GetDataManager().GetLevels(json);
        fetchConfigInProgressOverlay.ClosePanel(true);
        GameManager.Instance.SetGamePhase( GameManager.GamePhases.LoadScreen );
    }

    public void NoInternetError(){
		Debug.Log("Show some error message here and do not continue, that is, remove the following line");
        fetchConfigInProgressOverlay.ClosePanel();
        fetchConfigErrorOverlay.errorText.text = Constants.Messages.DisconnectedOnStart;
        fetchConfigErrorOverlay.OpenPanel();
    }

	public override void EndPhase()
	{
		startGameUI.SetActive(false);
	}
	
	[System.Serializable]
	public class Credit_UIOverlay : UIOverlay {
		
		public Button creditButton;
		
		public Button creditCloseButton;
		
		public UIOverlayBackground background;
		
		public override void OpenPanel() {
			background.SetTargetAlpha(1f);
			base.OpenPanel();
		}
		
		public override void ClosePanel(bool forceClose = false) {
			background.SetTargetAlpha(0f);
			base.ClosePanel(forceClose);
		}
	}
}
