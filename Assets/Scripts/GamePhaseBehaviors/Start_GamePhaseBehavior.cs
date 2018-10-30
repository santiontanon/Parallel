using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System;

public class Start_GamePhaseBehavior : GamePhaseBehavior {
	public GameObject startGameUI;

	public Button gameStart;
	public Button gameEnd;
    public Button preSurvey;
    public Button postSurvey;
    public InputField playerIdField;
    public Text versionNumber;
    public ParallelProg.UI.UIOverlay fetchConfigInProgressOverlay;
    [System.Serializable] public class StartErrorOverlay : ParallelProg.UI.UIOverlay { public Text errorText; }
    public StartErrorOverlay fetchConfigErrorOverlay;
    public Start_DebugUI debugOverlay;

	public delegate void StartPlayingWithLevelInformationDelegate(string json);
	public delegate void NoInternetErrorDelegate();

	public override void BeginPhase()
	{
        if (GameManager.Instance.currentGameMode == GameManager.GameMode.Demo)
        {
            preSurvey.gameObject.SetActive(false);
            postSurvey.gameObject.SetActive(false);
            playerIdField.gameObject.SetActive(false);

            versionNumber.text = "Demo Build v" + Application.version;
        }
        else
        {
            versionNumber.text = "Release Build v" + Application.version;
        }

        gameStart.onClick.RemoveAllListeners();
        gameEnd.onClick.RemoveAllListeners();
        if (GameManager.Instance.currentGameMode != GameManager.GameMode.Demo)
        {
            postSurvey.onClick.RemoveAllListeners();
            preSurvey.onClick.RemoveAllListeners();
            playerIdField.onEndEdit.RemoveAllListeners();
        }

        if (GameManager.Instance.currentGameMode != GameManager.GameMode.Demo && GameManager.Instance.preSurveyComplete == false)
        {
            //gameStart.interactable = false;
            //gameStart.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
            //postSurvey.interactable = GameManager.Instance.preSurveyComplete;
        }
        else
        {
            gameStart.interactable = true;
            gameStart.GetComponentInChildren<Text>().color = new Color(1f, 1f, 1f, 1f);
        }

		startGameUI.SetActive(true);
		gameStart.onClick.AddListener( ()=> StartPlaying() );
		gameEnd.onClick.AddListener( ()=> GameManager.Instance.SetGamePhase(GameManager.GamePhases.CloseGame) );

        if (GameManager.Instance.currentGameMode != GameManager.GameMode.Demo)
        {
            preSurvey.onClick.AddListener(() => PreSurveyButtonClicked());
            postSurvey.onClick.AddListener(() => PostSurveyButtonClicked());
            playerIdField.onEndEdit.AddListener(delegate { PlayerFieldChangedEvent(); });
            //IMPORTANT, COMMENT THE FOLLOWING LINE IF TESTING USING THE EDITOR
            if (PlayerPrefs.HasKey("PlayerId"))
            {
                playerIdField.text = PlayerPrefs.GetString("PlayerId");
                PlayerFieldChangedEvent();
            }
            //gameEnd.interactable = false;
        }
    }

    void RequirementsCheck()
    {
        if(JavaCheck.FindJavaVersion() == false)
        {
            //display error
        }
    }

    void PlayerFieldChangedEvent()
    {
        Debug.Log("PlayerFieldChangedEvent");
        if (GameManager.Instance.currentGameMode != GameManager.GameMode.Demo)
        {
            if (playerIdField.text.Length > 0)
            {
                GameManager.Instance.GetSaveManager().LoadSave(playerIdField.text);
                GameManager.Instance.UpdatePlayerField(playerIdField.text);
                GameManager.Instance.tracker.StartTrackerWithCallback(LoadRemoteData, null);
                //float opacity = (true && GameManager.Instance.preSurveyComplete) ? 1f : 0.25f;
                gameStart.interactable = true;//(true && GameManager.Instance.preSurveyComplete);
                gameStart.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 1f);//opacity);
                if(GameManager.Instance.GetDataManager().levRef.presurvey != "")
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
                gameStart.interactable = false;
                gameStart.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
                preSurvey.interactable = false;
                preSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
                postSurvey.interactable = false;
                postSurvey.GetComponentInChildren<Graphic>().color = new Color(1f, 1f, 1f, 0.25f);
            }
        }
    }

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
        if (GameManager.Instance.currentGameMode == GameManager.GameMode.Demo)
        {
            GameManager.Instance.SetGamePhase(GameManager.GamePhases.LoadScreen);
        }
        else
        {
            if (PlayerPrefs.HasKey("PlayerId") && !String.IsNullOrEmpty(PlayerPrefs.GetString("PlayerId")))
            {
                if (GameManager.Instance.tracker.ready)
                {
                    GameManager.Instance.SetGamePhase(GameManager.GamePhases.LoadScreen);
                }
                else
                {
                    GameManager.Instance.tracker.StartTrackerWithCallback(StartPlayingWithLevelInformation, NoInternetError);
                    fetchConfigInProgressOverlay.OpenPanel();
                }
            }
            else
            {

            }
        }
    }
	public void StartPlayingWithLevelInformation(string json){
		// Get the level selection from the json
		Debug.Log("Get the level selection from the json: "+json);
        //if(!GameManager.Instance.GetDataManager().debugLevelLoader) GameManager.Instance.GetDataManager().GetLevels(json);
        fetchConfigInProgressOverlay.ClosePanel(true);
        GameManager.Instance.SetGamePhase( GameManager.GamePhases.LoadScreen );
    }

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

    public void NoInternetError(){
		// Show some error message here and do not continue, that is, remove the following line
		Debug.Log("Show some error message here and do not continue, that is, remove the following line");
        fetchConfigInProgressOverlay.ClosePanel();
        fetchConfigErrorOverlay.errorText.text = Constants.Messages.DisconnectedOnStart;
        fetchConfigErrorOverlay.OpenPanel();
        //GameManager.Instance.SetGamePhase( GameManager.GamePhases.LoadScreen );
        //FetchingConfigPopup.ClosePanel(true);
    }


	public override void UpdatePhase()
	{
        if (Input.GetKeyDown(KeyCode.BackQuote) && Input.GetKey(KeyCode.LeftShift))
        {
            debugOverlay.ToggleDebugUI();
        }
	}



	public override void EndPhase()
	{
		startGameUI.SetActive(false);
	}
}
