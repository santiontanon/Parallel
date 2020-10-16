using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System;
using ParallelProg.UI;

public class Start_GamePhaseBehavior : GamePhaseBehavior {
	public GameObject startGameUI;

	public Button gameStart;
	public Button gameEnd;
    public Button preSurvey;
    public Button postSurvey;
	public Credit_UIOverlay credits;
    public InputField playerIdField;
    public ParallelProg.UI.UIOverlay fetchConfigInProgressOverlay;
    [System.Serializable] public class StartErrorOverlay : ParallelProg.UI.UIOverlay { public Text errorText; }
    public StartErrorOverlay fetchConfigErrorOverlay;
    public Start_DebugUI debugOverlay;

    public string currentPlayerID;

	public delegate void StartPlayingWithLevelInformationDelegate(string json);
	public delegate void NoInternetErrorDelegate();

	public override void BeginPhase()
	{
		/*   
		if (GameManager.Instance.currentGameMode == GameManager.GameMode.Demo)
        {
            preSurvey.gameObject.SetActive(false);
            postSurvey.gameObject.SetActive(false);
            playerIdField.gameObject.SetActive(false);
        }
        else
        {
            postSurvey.onClick.RemoveAllListeners();
            preSurvey.onClick.RemoveAllListeners();
            playerIdField.onEndEdit.RemoveAllListeners();

            if (GameManager.Instance.currentGameMode != GameManager.GameMode.Demo)
            {
                preSurvey.onClick.AddListener(() => PreSurveyButtonClicked());
                postSurvey.onClick.AddListener(() => PostSurveyButtonClicked());
                playerIdField.onEndEdit.AddListener(delegate { PlayerFieldChangedEvent(); });
                if (PlayerPrefs.HasKey("PlayerId"))
                {
                    playerIdField.text = PlayerPrefs.GetString("PlayerId");
                    PlayerFieldChangedEvent();
                }
                playerIdField.onValueChanged.AddListener((string s) => { GameManager.Instance.ResetInitStatus(); });
            }
        }
		*/

        gameStart.onClick.RemoveAllListeners();

		startGameUI.SetActive(true);
		gameStart.onClick.AddListener( ()=> {
			StartPlaying(); 
			GameManager.Instance.GetSaveManager().LoadSave("input");
			GameManager.Instance.UpdatePlayerField("input");
		//	GameManager.Instance.tracker.StartTrackerWithCallback(LoadRemoteData, null);
            GameManager.Instance.tracker.StartTrackerWithCallback(null, null, "NA", true, false);
		});
		gameStart.interactable = true;
		
		credits.creditButton.onClick.AddListener(()=> {
			credits.OpenPanel(); 
		});

		credits.creditCloseButton.onClick.AddListener(() => {
			credits.ClosePanel(false);
		});
		
		gameEnd.onClick.AddListener( ()=> GameManager.Instance.SetGamePhase(GameManager.GamePhases.CloseGame) );
    }

/*   
	void PlayerFieldChangedEvent()
    {
        Debug.Log("PlayerFieldChangedEvent");
        if(playerIdField.text != currentPlayerID)
        {
            currentPlayerID = playerIdField.text;
            GameManager.Instance.trackerIntialized = false;
            GameManager.Instance.playerModelingIntialized = false;
            if (GameManager.Instance.currentGameMode != GameManager.GameMode.Demo && GameManager.Instance.currentGameMode != GameManager.GameMode.Test)
            {
                if (playerIdField.text.Length > 0)
                {
                    GameManager.Instance.GetSaveManager().LoadSave(playerIdField.text);
                    GameManager.Instance.UpdatePlayerField(playerIdField.text);
                    GameManager.Instance.tracker.StartTrackerWithCallback(LoadRemoteData, null);
                    if (GameManager.Instance.GetDataManager().levRef.presurvey != "")
                    {
                        preSurvey.interactable = true;
                        preSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 1f);
                        postSurvey.interactable = true;
                        postSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 1f);
                    }
                    else
                    {
                        preSurvey.interactable = false;
                        preSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
                        postSurvey.interactable = false;
                        postSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
                    }
                }
                else
                {
                    preSurvey.interactable = false;
                    preSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
                    postSurvey.interactable = false;
                    postSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
                }
            }
            else if(GameManager.Instance.currentGameMode == GameManager.GameMode.Test)
            {
                Debug.Log("test");
                GameManager.Instance.tracker.StartTrackerWithCallback(null, null, "NA", true, false);
            }
        }
        else
        {
            Debug.Log("No Text Change");
        }
    } 
*/

    public void LoadRemoteData(string s)
    {
        GameManager.Instance.GetDataManager().GetLevels(s);
        if (GameManager.Instance.GetDataManager().levRef.presurvey != "")
        {
            preSurvey.interactable = true;
            preSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 1f);
            postSurvey.interactable = true;
            postSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 1f);
        }
        else
        {
            preSurvey.interactable = false;
            preSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
            postSurvey.interactable = false;
            postSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
        }
    }

	void StartPlaying(){
        GameManager.Instance.SetGamePhase(GameManager.GamePhases.LoadScreen);
    }

	public void StartPlayingWithLevelInformation(string json){
		// Get the level selection from the json
		Debug.Log("Get the level selection from the json: "+json);
        //if(!GameManager.Instance.GetDataManager().debugLevelLoader) GameManager.Instance.GetDataManager().GetLevels(json);
        fetchConfigInProgressOverlay.ClosePanel(true);
        GameManager.Instance.SetGamePhase( GameManager.GamePhases.LoadScreen );
    }

/*   
	public void PreSurveyButtonClicked()
    {
        //gameStart.interactable = GameManager.Instance.preSurveyComplete;
        //float opacity = GameManager.Instance.preSurveyComplete ? 1f : 0.25f;
        //gameStart.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, opacity);
        //postSurvey.interactable = GameManager.Instance.preSurveyComplete;
        GameManager.Instance.preSurveyComplete = true;
        Application.OpenURL(GameManager.Instance.GetDataManager().levRef.presurvey);
    }
    public void PostSurveyButtonClicked()
    {
        GameManager.Instance.postSurveyComplete = true;
        Application.OpenURL(GameManager.Instance.GetDataManager().levRef.postsurvey);
    } 
*/

    public void NoInternetError(){
		Debug.Log("Show some error message here and do not continue, that is, remove the following line");
        fetchConfigInProgressOverlay.ClosePanel();
        fetchConfigErrorOverlay.errorText.text = Constants.Messages.DisconnectedOnStart;
        fetchConfigErrorOverlay.OpenPanel();
    }

	public override void UpdatePhase()
	{
		
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
