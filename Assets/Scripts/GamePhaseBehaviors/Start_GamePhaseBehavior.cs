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

	public delegate void StartPlayingWithLevelInformationDelegate(string json);
	public delegate void NoInternetErrorDelegate();

	public override void BeginPhase()
	{
        if (GameManager.Instance.is_demo_build)
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
        if (!GameManager.Instance.is_demo_build)
        {
            postSurvey.onClick.RemoveAllListeners();
            preSurvey.onClick.RemoveAllListeners();
            playerIdField.onEndEdit.RemoveAllListeners();
        }

        if (!GameManager.Instance.is_demo_build)
        {
            gameStart.interactable = false;
            postSurvey.interactable = GameManager.Instance.preSurveyComplete;
        }
        else
        {
            gameStart.interactable = true;
        }

		startGameUI.SetActive(true);
		gameStart.onClick.AddListener( ()=> StartPlaying() );
		gameEnd.onClick.AddListener( ()=> GameManager.Instance.SetGamePhase(GameManager.GamePhases.CloseGame) );

        if (!GameManager.Instance.is_demo_build)
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
        if (!GameManager.Instance.is_demo_build)
        {
            if (playerIdField.text.Length > 0)
            {
                gameStart.interactable = (true && GameManager.Instance.preSurveyComplete);
                GameManager.Instance.GetSaveManager().LoadSave(playerIdField.text);
            }
            else gameStart.interactable = false;
            GameManager.Instance.UpdatePlayerField(playerIdField.text);
        }
    }

	void StartPlaying(){
        if (GameManager.Instance.is_demo_build)
        {
            GameManager.Instance.SetGamePhase(GameManager.GamePhases.LoadScreen);
        }
        else
        {


            //Show some waiting message here
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
                Debug.Log("missing PlayerId");
                //playerIdField.transform.FindChild("Text").GetComponent<Text>().color = Color.red;
                //playerIdField.Select();
                //playerIdField.ActivateInputField();
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
        GameManager.Instance.preSurveyComplete = true;
        gameStart.interactable = GameManager.Instance.preSurveyComplete;
        //postSurvey.interactable = GameManager.Instance.preSurveyComplete;
        Application.OpenURL("http://unity3d.com/");
    }
    public void PostSurveyButtonClicked()
    {
        GameManager.Instance.postSurveyComplete = true;

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
	}



	public override void EndPhase()
	{
		startGameUI.SetActive(false);
	}
}
