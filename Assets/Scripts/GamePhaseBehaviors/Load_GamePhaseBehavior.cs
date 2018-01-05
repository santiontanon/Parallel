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

        public RectTransform requiredLevelContainer;
        public RectTransform optionalLevelContainer;
        public RectTransform previousContainer;

        public GameObject levelButtonPrefab;
        public Button exitLevelSelectionButton;
        [SerializeField] public UIOverlay levelLoadingOverlay;
	}
	public Load_UI loadUI;


	public override void BeginPhase()
	{
		
        foreach (Transform child in loadUI.requiredLevelContainer)
        {
            GameObject.Destroy(child.gameObject);
        }
        foreach (Transform child in loadUI.optionalLevelContainer)
        {
            GameObject.Destroy(child.gameObject);
        }
        foreach (Transform child in loadUI.previousContainer)
        {
            GameObject.Destroy(child.gameObject);
        }

        foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.required)
        {
            string levelName = lr.title;
            /*if (GameManager.Instance.hideTestsForBuild && levelName.StartsWith("level-"))
            {
                continue;
            }*/
            string targetLevel = lr.file;
            GameObject g = Instantiate(loadUI.levelButtonPrefab) as GameObject;
            Button gButton = g.GetComponent<Button>();
            g.transform.SetParent(loadUI.requiredLevelContainer);
            g.transform.localScale = Vector3.one;
            Text gText = g.GetComponentInChildren<Text>();
            gText.text = levelName;
            gButton.onClick.AddListener(() => LoadButtonBehavior(targetLevel));
        }
        foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.optional)
        {
            string levelName = lr.title;
            /*if (GameManager.Instance.hideTestsForBuild && levelName.StartsWith("level-"))
            {
                continue;
            }*/
            string targetLevel = lr.file;
            GameObject g = Instantiate(loadUI.levelButtonPrefab) as GameObject;
            Button gButton = g.GetComponent<Button>();
            g.transform.SetParent(loadUI.optionalLevelContainer);
            g.transform.localScale = Vector3.one;
            Text gText = g.GetComponentInChildren<Text>();
            gText.text = levelName;
            gButton.onClick.AddListener(() => LoadButtonBehavior(targetLevel));
        }
        if (GameManager.Instance.GetDataManager().levRef.levels.previous != null)
        {
            foreach (LevelReferenceObject lr in GameManager.Instance.GetDataManager().levRef.levels.previous)
            {
                string levelName = lr.title;
                /*if (GameManager.Instance.hideTestsForBuild && levelName.StartsWith("level-"))
                {
                    continue;
                }*/
                string targetLevel = lr.file;
                GameObject g = Instantiate(loadUI.levelButtonPrefab) as GameObject;
                Button gButton = g.GetComponent<Button>();
                g.transform.SetParent(loadUI.previousContainer);
                g.transform.localScale = Vector3.one;
                Text gText = g.GetComponentInChildren<Text>();
                gText.text = levelName;
                gButton.onClick.AddListener(() => LoadButtonBehavior(targetLevel));
            }
        }
        
        if (!GameManager.Instance.hideTestsForBuild){
			//AddPCGButton();
		}

        loadUI.levelLoadingOverlay.ClosePanel(true);

        loadUI.loadPhaseUI.SetActive(true);

        loadUI.exitLevelSelectionButton.onClick.AddListener(() => GameManager.Instance.SetGamePhase(GameManager.GamePhases.StartScreen));
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
        GameManager.Instance.TriggerLoadLevel( levelName );
	}
    public void LoadPCGBehavior()
    {
        loadUI.levelLoadingOverlay.OpenPanel();
        GameManager.Instance.TriggerPCG();
    }
}
