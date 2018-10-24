using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System.Collections.Generic;
using UnityEngine.EventSystems;
using ParallelProg.UI;

public class Load_GamePhaseBehavior : GamePhaseBehavior {

	[System.Serializable]
	public class Load_UI
	{
		public GameObject loadPhaseUI;

        public RectTransform requiredLevelContainer, requiredTransform;
        public RectTransform optionalLevelContainer, optionalTransform;
        public RectTransform previousLevelContainer, previousTransform;
        public RectTransform generateLevelContainer, generateTransform;

        public GameObject levelButtonPrefab;

        public Button exitLevelSelectionButton;
        [SerializeField] public UIOverlay levelLoadingOverlay;
        public Button requiredLevelsButton, optionalLevelsButton, previousLevelsButton, generateLevelButton, pcgButton;
	}
	public Load_UI loadUI;


	public override void BeginPhase()
	{
        GameManager.Instance.GetDataManager().GetLevels();
        ClearOldButtons();
        SetupButtonFunctions();
        SetupContainers();
        loadUI.levelLoadingOverlay.ClosePanel(true);
        loadUI.loadPhaseUI.SetActive(true);
    }

    void ClearOldButtons()
    {
        foreach (Transform child in loadUI.requiredLevelContainer)
        {
            GameObject.Destroy(child.gameObject);
        }
        foreach (Transform child in loadUI.optionalLevelContainer)
        {
            GameObject.Destroy(child.gameObject);
        }
        foreach (Transform child in loadUI.previousLevelContainer)
        {
            GameObject.Destroy(child.gameObject);
        }
        foreach (Transform child in loadUI.generateLevelContainer)
        {
            if (loadUI.generateLevelContainer.GetChild(0) != child)
            {
                Destroy(child.gameObject);
            }
        }
    }

    void SetupContainers()
    {
        if(GameManager.Instance.currentGameMode == GameManager.GameMode.Study_9)
        {
            loadUI.optionalLevelContainer.gameObject.SetActive(false);
            loadUI.previousLevelContainer.gameObject.SetActive(false);
            loadUI.requiredLevelsButton.gameObject.SetActive(false);
            loadUI.previousLevelsButton.gameObject.SetActive(false);
            loadUI.generateLevelButton.gameObject.SetActive(false);
            loadUI.optionalLevelsButton.gameObject.SetActive(false);

            loadUI.requiredTransform.gameObject.SetActive(true);
            loadUI.generateTransform.gameObject.SetActive(true);
        }
        else
        {
            TriggerPanelSwap(true, false, false, false);
        }
    }

    void SetupButtonFunctions()
    {
        foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.required)
        {
            SetupLevelButton(lr, loadUI.requiredLevelContainer, false);
        }
        foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.optional)
        {
            SetupLevelButton(lr, loadUI.optionalLevelContainer, false);
        }
        if (GameManager.Instance.GetDataManager().levRef.levels.previous != null)
        {
            foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.previous)
            {
                SetupLevelButton(lr, loadUI.previousLevelContainer, false);
            }
        }
        if (GameManager.Instance.GetDataManager().levRef.levels.pcg != null)
        {
            foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.pcg)
            {
                SetupLevelButton(lr, loadUI.generateLevelContainer, true);
            }
        }

        loadUI.exitLevelSelectionButton.onClick.AddListener(() => GameManager.Instance.SetGamePhase(GameManager.GamePhases.StartScreen));

        loadUI.requiredLevelsButton.onClick.RemoveAllListeners();
        loadUI.requiredLevelsButton.onClick.AddListener(() =>
        {
            TriggerPanelSwap(true, false, false, false);
        });

        loadUI.optionalLevelsButton.onClick.RemoveAllListeners();
        loadUI.optionalLevelsButton.onClick.AddListener(() =>
        {
            TriggerPanelSwap(false, false, true, false);
        });

        loadUI.previousLevelsButton.onClick.RemoveAllListeners();
        loadUI.previousLevelsButton.onClick.AddListener(() =>
        {
            TriggerPanelSwap(false, true, false, false);
        });

        loadUI.generateLevelButton.onClick.RemoveAllListeners();
        loadUI.generateLevelButton.onClick.AddListener(() =>
        {
            TriggerPanelSwap(false, false, false, true);
        });

        loadUI.pcgButton.onClick.RemoveAllListeners();
        loadUI.pcgButton.onClick.AddListener(() => LoadPCGBehavior());
    }

    void TriggerPanelSwap(bool requiredPanel, bool previousPanel, bool optionalPanel, bool generatePanel )
    {
        //loadUI.requiredLevelsButton.gameObject.SetActive(!requiredPanel);
        loadUI.requiredLevelsButton.interactable = !requiredPanel;
        loadUI.requiredLevelsButton.GetComponentInChildren<Text>().color = requiredPanel ? new Color(0.7f,0.7f,0.7f) : Color.white;
        loadUI.requiredLevelsButton.GetComponentInChildren<Image>().color = requiredPanel ? new Color(1f,1f,1f,0.7f) : Color.white;
        loadUI.requiredTransform.gameObject.SetActive(requiredPanel);

        //loadUI.previousLevelsButton.gameObject.SetActive(!previousPanel);
        loadUI.previousLevelsButton.interactable = !previousPanel;
        loadUI.previousLevelsButton.GetComponentInChildren<Text>().color = previousPanel ? new Color(0.7f, 0.7f, 0.7f) : Color.white;
        loadUI.previousLevelsButton.GetComponentInChildren<Image>().color = previousPanel ? new Color(1f, 1f, 1f, 0.7f) : Color.white;
        loadUI.previousTransform.gameObject.SetActive(previousPanel);

        //loadUI.optionalLevelsButton.gameObject.SetActive(!optionalPanel);
        loadUI.optionalLevelsButton.interactable = !optionalPanel;
        loadUI.optionalLevelsButton.GetComponentInChildren<Text>().color = optionalPanel ? new Color(0.7f, 0.7f, 0.7f) : Color.white;
        loadUI.optionalLevelsButton.GetComponentInChildren<Image>().color = optionalPanel ? new Color(1f, 1f, 1f, 0.7f) : Color.white;
        loadUI.optionalTransform.gameObject.SetActive(optionalPanel);

        //loadUI.optionalLevelsButton.gameObject.SetActive(!optionalPanel);
        loadUI.generateLevelButton.interactable = !generatePanel;
        loadUI.generateLevelButton.GetComponentInChildren<Text>().color = generatePanel ? new Color(0.7f, 0.7f, 0.7f) : Color.white;
        loadUI.generateLevelButton.GetComponentInChildren<Image>().color = generatePanel ? new Color(1f, 1f, 1f, 0.7f) : Color.white;
        loadUI.generateTransform.gameObject.SetActive(generatePanel);
    }

    void SetupLevelButton(LevelReferenceObject lr, Transform container, bool pcg)
    {
        char[] trimArray = new char[5] { 'L', 'l', 'e', 'v', ' ' };
        string levelName = lr.file;
        GameObject g = Instantiate(loadUI.levelButtonPrefab) as GameObject;
        LevelButtonBehavior buttonInstance = g.GetComponent<LevelButtonBehavior>();
        if (buttonInstance != null)
        {
            buttonInstance.SetLevelSprite(pcg, lr.completionRank > 0);
            buttonInstance.SetLevelRank(lr.GetLevelCompletionRank());
        }

        Button gButton = g.GetComponent<Button>();
        g.transform.SetParent(container);
        g.transform.localScale = Vector3.one;
        Text gText = g.GetComponentInChildren<Text>();
        gText.text = levelName.TrimStart(trimArray);
        if (!pcg)
        {
            gButton.onClick.AddListener(() => LoadButtonBehavior(levelName));
        }
        else
        {
            gButton.onClick.AddListener(() => LoadPCGButtonBehavior(lr.data));
        }
    }

    public void AddPCGButton(){
			GameObject g = Instantiate(loadUI.levelButtonPrefab) as GameObject;
			Button gButton = g.GetComponent<Button>();
			g.transform.SetParent( loadUI.requiredLevelContainer );
			g.transform.localScale = Vector3.one;
			Text gText = g.GetComponentInChildren<Text>();
			gText.text = "PCG";
			gButton.onClick.AddListener( ()=> LoadPCGBehavior() );
	}

	public override void UpdatePhase()
	{
	}

	public override void EndPhase()
	{
        if(loadUI.levelLoadingOverlay.isOpen) loadUI.levelLoadingOverlay.ClosePanel();
        loadUI.loadPhaseUI.SetActive(false);
	}

	public void LoadButtonBehavior( string levelName ) 
	{
        loadUI.levelLoadingOverlay.OpenPanel();
        GameManager.Instance.TriggerLoadLevel(true, DataManager.LoadType.RESOURCES, levelName );
	}

    public void LoadPCGButtonBehavior(string level)
    {
        loadUI.levelLoadingOverlay.OpenPanel();
        GameManager.Instance.TriggerLoadLevel(true, DataManager.LoadType.STRING, level);
    }

    public void LoadButtonBehavior(LevelReferenceObject levelReference)
    {
        loadUI.levelLoadingOverlay.OpenPanel();
        GameManager.Instance.TriggerLoadLevel(levelReference);
    }

    public void LoadPCGBehavior()
    {
        loadUI.levelLoadingOverlay.OpenPanel();
        GameManager.Instance.TriggerPCG();
    }
}
