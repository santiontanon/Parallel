using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System.Collections.Generic;
using UnityEngine.EventSystems;

public class TutorialManager : MonoBehaviour {

    public TutorialEvent[] tutorialEvents;
    TutorialEvent[] currentTutorialEventQueue;
    int tutorialEventIndex = 0;

    [System.Serializable]
    public class Tutorial_UIOverlay : ParallelProg.UI.UIOverlay
    {
        public Text tutorialDescription;
        public Canvas canvas;
        public CanvasScaler scaler;
        public RectTransform tutorialArrow;
        public Button tutorialCloseButton;
        public Button skipTutorialsButton;
        public Button nextTutorialButton;

        public override void OpenPanel()
        {
            Vector2 pivotOffset = Vector2.zero;
            if (panelContainer.position.x < Screen.width * 0.2f) { pivotOffset.x = 0.0f; }
            if (panelContainer.position.x > Screen.width * 0.8f) { pivotOffset.x = 1.0f; }

            if (panelContainer.position.y < Screen.height * 0.2f) { pivotOffset.y = 0.0f; }
            if (panelContainer.position.y > Screen.height * 0.8f) { pivotOffset.y = 1.0f; }
            //panelContainer.pivot = pivotOffset;

            if (skipTutorialsButton)
            {
                skipTutorialsButton.onClick.RemoveAllListeners();
                skipTutorialsButton.onClick.AddListener( ()=> GameManager.Instance.TriggerLevelTutorialSkip(true) );
            }
            base.OpenPanel();
        }

        public void SetTooltip(string inDescription, Button button, bool nextTutorial = false)
        {
            if (button != null)
            {
                RectTransform elementRect = button.gameObject.GetComponent<RectTransform>();
                //panelContainer.position = button.gameObject.transform.position;
                float posX = button.gameObject.transform.position.x;
                float posY = button.gameObject.transform.position.y;

                //account for canvas size and resolution
                float multiplier = canvas.pixelRect.width / scaler.referenceResolution.x;
                RectTransform tooltip = panelContainer.GetChild(0).GetComponent<RectTransform>();

                Vector3 nextPanelPosition = new Vector3(posX, posY, panelContainer.position.z);
                PositionTutorialPanel(nextPanelPosition, button.transform.position, inDescription.Length);
            }
            else
            {
                float posX = Screen.width / 2f;
                float posY = Screen.height / 2f;
                Vector3 nextPanelPosition = new Vector3(posX, posY, panelContainer.position.z);
                PositionTutorialPanel(nextPanelPosition, nextPanelPosition, inDescription.Length);
            }
            if (nextTutorial)
            {
                nextTutorialButton.gameObject.SetActive(true);
                nextTutorialButton.onClick.RemoveAllListeners();
                nextTutorialButton.onClick.AddListener(() => GameManager.Instance.TriggerLevelTutorialSkip(false));
            }
            else
            {
                nextTutorialButton.gameObject.SetActive(false);
            }
            tutorialDescription.text = inDescription;
        }

        public void SetTooltip(string inDescription, GameObject element, bool nextTutorial = false)
        {
            SpriteRenderer sprite = element.GetComponent<SpriteRenderer>();
            panelContainer.position = element.transform.position;
            float posX = element.transform.position.x;
            float posY = element.transform.position.y;

            float multiplier = canvas.pixelRect.width / scaler.referenceResolution.x;
            RectTransform tooltip = panelContainer.GetChild(0).GetComponent<RectTransform>();
            
            Camera gameCamera = GameObject.Find("UICamera").GetComponent<Camera>();

            Vector3 start_tooltipCenter = new Vector3(Screen.width / 2, Screen.height / 2, panelContainer.position.z);
            start_tooltipCenter = gameCamera.WorldToScreenPoint(new Vector3(posX, posY, panelContainer.position.z));

            Vector3 end_tooltipFocusPoint = gameCamera.WorldToScreenPoint(new Vector3(posX, posY, panelContainer.position.z));

            PositionTutorialPanel(start_tooltipCenter, end_tooltipFocusPoint, inDescription.Length);

            if (nextTutorial)
                nextTutorialButton.gameObject.SetActive(true);
            else
                nextTutorialButton.gameObject.SetActive(false);
            tutorialDescription.text = inDescription;
        }

        void PositionTutorialPanel(Vector3 position, Vector3 tutorialFocusTargetPosition, int descriptionSize = 0 )
        {
            float multiplier = canvas.pixelRect.width / scaler.referenceResolution.x;

            //BOYD: MAYBE WE CAN FIND A BETTER WAY TO SIZE OUR TUTORIAL BOXES
            Vector2 targetPanelSize = new Vector2(200f, 100f);
            if (descriptionSize <= 20) { }
            else if (descriptionSize <= 50) { targetPanelSize.y = 200f; }
            else if (descriptionSize <= 100) { targetPanelSize.x = 250f;  targetPanelSize.y = 250f; }
            else { targetPanelSize.x = 420f; targetPanelSize.y = 300f; }

            panelContainer.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, targetPanelSize.x);
            panelContainer.SetSizeWithCurrentAnchors(RectTransform.Axis.Vertical, targetPanelSize.y);

            float topBannerHeight = multiplier * GameObject.Find("Top_Banner").GetComponent<RectTransform>().rect.height;
            float rightBannerWidth = multiplier * GameObject.Find("Right_Banner").GetComponent<RectTransform>().rect.width;
            float bottomBannerHeight = multiplier * GameObject.Find("Bottom_Banner").GetComponent<RectTransform>().rect.height;
            float panelClearHeight = multiplier * panelContainer.rect.height / 2f;
            float panelClearWidth = multiplier * panelContainer.rect.width / 2f;
            float tooltipNubSize = 50f * multiplier;


            Vector3 start = position;

            //height bounds exceeded?
            if (start.y >= (Screen.height - topBannerHeight - panelClearHeight))
            {
                start = new Vector3(start.x, Screen.height - topBannerHeight - panelClearHeight - tooltipNubSize, position.z);
            }
            else if (start.y <= bottomBannerHeight + panelClearHeight)
            {
                start = new Vector3(start.x, bottomBannerHeight + panelClearHeight + tooltipNubSize, position.z);
            }

            //width bounds exceeded?
            if (start.x >= (Screen.width - rightBannerWidth - panelClearWidth))
            {
                start = new Vector3(Screen.width - panelClearWidth - rightBannerWidth - tooltipNubSize, start.y, position.z);
            }
            else if (start.x <= 0f + panelClearWidth)
            {
                start = new Vector3(0f + panelClearWidth + tooltipNubSize, start.y, position.z);
            }

            Vector3 end = tutorialFocusTargetPosition;
            end.z = start.z;

            if (Vector3.Distance(start, end) <= panelClearHeight)
            {
                Vector3 centerPoint = new Vector3(Screen.width / 2f, Screen.height / 2f, start.z);
                Vector3 rayToCenter = (centerPoint - start).normalized;
                start += rayToCenter * panelClearHeight * 2f;
            }

            panelContainer.position = start;
            TutorialPanelTail(start, end);
        }

        void TutorialPanelTail(Vector3 start, Vector3 end)
        {
            float multiplier = canvas.pixelRect.width / scaler.referenceResolution.x;
            Vector3 ray = end - start;
            float rad = Mathf.Atan2(ray.y, ray.x); // In radians
            float deg = rad * (180 / Mathf.PI) + 90f; //starts from bottom instead of from right
            tutorialArrow.localRotation = Quaternion.Euler(0,0,deg);
            float targetNubSize = ray.magnitude;
            tutorialArrow.localScale = Vector3.one;
            tutorialArrow.SetSizeWithCurrentAnchors(RectTransform.Axis.Vertical, targetNubSize + 10f);
        }


        
    }

    [SerializeField]
    public Tutorial_UIOverlay tutorialOverlay;

    #region Legacy Tutorial Code
    //OLD WAY IS BELOW
    [System.Serializable]
	public class TutorialScreen 
	{
		public int tutorialLevelId = 0;
		public Sprite tutorialLevelImage;
		public MovieTexture tutorialLevelMovie;
        public string tutorialLevelMovieName;
	}
	[SerializeField]
	public TutorialScreen[] tutorialScreens;

	public EventTrigger tutorialImageTriggerObject;
	public EventTrigger tutorialMovieTriggerObject;
	public EventTrigger imageBackTrigger;
	public EventTrigger imageForwardTrigger;
	public EventTrigger movieBackTrigger;
	public EventTrigger movieForwardTrigger;
	public EventTrigger moviePauseTrigger;
	
	public Sprite pauseSprite;
	public Sprite playSprite;
#endregion

    public TutorialScreen[] tutorialDisplays;
	public int tutorialIndex = 0;
	private bool isPlaying;

	public void PerformTutorialSeries(int inputLevelId, TutorialEvent.TutorialInitializeTriggers inputInitPhase = TutorialEvent.TutorialInitializeTriggers.beforePlay)
	{
        List<TutorialEvent> returnEvents = RetrieveTutorialEvents(inputLevelId, inputInitPhase);
        currentTutorialEventQueue = returnEvents.ToArray();
        tutorialEventIndex = 0;
        if (currentTutorialEventQueue.Length > 0)
        {
            InitializeCurrentTutorialEvent();
        }
        /*
        List<TutorialScreen> returnDisplays = RetrieveTutorialDisplays( inputLevelId );
		//tutorialIndex = 0;
		if(returnDisplays.Count >= 0)
		{
			tutorialDisplays = returnDisplays.ToArray();
			InitializeTutorial();
		}
		else 
		{
			tutorialImageTriggerObject.gameObject.SetActive(false);
			tutorialMovieTriggerObject.gameObject.SetActive(false);
		}
        */
	}

    public void ResetTutorialCompletionData()
    {
        foreach (TutorialEvent t in tutorialEvents)
        {
            t.hasCompleted = false;
        }
    }

    List<TutorialEvent> RetrieveTutorialEvents(int inputLevelId, TutorialEvent.TutorialInitializeTriggers inputInitPhase)
    {
        //Debug.Log("Retrieving Tutorial Events... Level " + inputLevelId + " Phase " + inputInitPhase.ToString());
        List<TutorialEvent> returnEvents = new List<TutorialEvent>();
        PlayerInteraction_GamePhaseBehavior p = (PlayerInteraction_GamePhaseBehavior)GameManager.Instance.playerInteractionBehavior;

        bool foundSimEvents = false; //triggered once events made to play during the simulation are found, prevents queuing of beforePlay events that follow it

        int currentSequence = -1;

        foreach (TutorialEvent t in tutorialEvents)
        {
            if (t.levelNumber == inputLevelId && !t.hasCompleted)
            {
                if (t.init_trigger == TutorialEvent.TutorialInitializeTriggers.duringSimulation) foundSimEvents = true;
                else if (t.init_trigger == TutorialEvent.TutorialInitializeTriggers.beforePlay && foundSimEvents == true) break;
                if (currentSequence == -1)
                    currentSequence = t.sequenceId;
                if(currentSequence == t.sequenceId)
                    returnEvents.Add(t);
            }
            else if (t.levelNumber > inputLevelId) break;
            /*if (t.init_trigger == inputInitPhase)
            {
                if (t.levelNumber == inputLevelId && !t.hasCompleted) returnEvents.Add(t);
            }*/
            //else if (t.init_trigger != inputInitPhase && returnEvents.Count > 0) break;
        }
        foreach (TutorialEvent t in returnEvents) { } //Debug.Log("TUTORIAL QUEUED: " + t.popupDescription + "\n");
        return returnEvents;
    }

    void InitializeCurrentTutorialEvent()
    {
        if (tutorialEventIndex >= currentTutorialEventQueue.Length )
        {
            //Debug.Log("All tutorials complete for this level.");
        }
        else
        {
            //Debug.Log(tutorialIndex.ToString() + " is current Tutorial index.");
            TutorialEvent currentTutorial = currentTutorialEventQueue[tutorialEventIndex];
            PerformTutorial(currentTutorial);
            if(currentTutorialEventQueue.Length > tutorialEventIndex + 2) //is there a next tutorial?
            {
                TutorialEvent nextTutorial = currentTutorialEventQueue[tutorialEventIndex + 1];
                if (nextTutorial.type == TutorialEvent.TutorialTypes.simulation)
                {
                    //Debug.Log("PREFORMING SIM");
                    PerformTutorial(nextTutorial);
                }
            }
        }
    }

    void PerformTutorial(TutorialEvent t)
    {
        //Debug.Log("Performing Tutorial Event");
        if (t.type == TutorialEvent.TutorialTypes.popup)
        {
            switch (t.complete_trigger)
            {
                case TutorialEvent.TutorialCompletionTriggers.clickButton:
                case TutorialEvent.TutorialCompletionTriggers.placeSignal:
                case TutorialEvent.TutorialCompletionTriggers.placeSemaphore:
                    GameManager.Instance.tracker.CreateEventExt("PerformTutorial",t.popupDescription);
                    tutorialOverlay.SetTooltip(t.popupDescription, t.targetButton, t.nextTutorial);
                    tutorialOverlay.OpenPanel();
                    t.ActivateTutorialEventListener();
                    break;

                case TutorialEvent.TutorialCompletionTriggers.clickPopup:
                    GameManager.Instance.tracker.CreateEventExt("PerformTutorial", t.popupDescription);
                    if (t.targetComponentType.Length > 0)
                    {
                        List<GridObjectBehavior> objectsOfType = GameManager.Instance.GetGridManager().GetGridComponentsOfType("signal");
                        if(objectsOfType.Count>0) tutorialOverlay.SetTooltip(t.popupDescription, objectsOfType[0].gameObject, t.nextTutorial);
                        else tutorialOverlay.SetTooltip(t.popupDescription, t.targetButton, t.nextTutorial);
                    }
                    else
                    {
                        tutorialOverlay.SetTooltip(t.popupDescription, t.targetButton, t.nextTutorial);
                    }
                    tutorialOverlay.SetTooltip(t.popupDescription, t.targetButton, t.nextTutorial);

                    tutorialOverlay.OpenPanel();
                    t.ActivateTutorialEventListener();
                    break;
                case TutorialEvent.TutorialCompletionTriggers.simulationInteraction:
					GameManager.Instance.tracker.CreateEventExt("PerformTutorial","simulationInteraction");
                    t.ActivateTutorialEventListener();
                    break;
            }
        }
        else if (t.type == TutorialEvent.TutorialTypes.video) { }
        else if(t.type == TutorialEvent.TutorialTypes.simulation)
        {
            PlayerInteraction_GamePhaseBehavior playerInteractionBehavior = (PlayerInteraction_GamePhaseBehavior)GameManager.Instance.playerInteractionBehavior;
            if (t.overrideSimulation == true)
            {
                playerInteractionBehavior.tutorialMode = true;
                GameManager.Instance.TriggerLoadTutorialLevel(t.filepath);
                t.ActivateTutorialEventListener();
            }
        }
    }

    void CloseTutorial (TutorialEvent t)
    {
        if (t.type == TutorialEvent.TutorialTypes.popup)
        {
            switch (t.complete_trigger)
            {
                case TutorialEvent.TutorialCompletionTriggers.clickButton:
                case TutorialEvent.TutorialCompletionTriggers.clickPopup:
                    tutorialOverlay.ClosePanel();
					GameManager.Instance.tracker.CreateEventExt("CloseTutorial","clickPopup");
                    break;

                case TutorialEvent.TutorialCompletionTriggers.placeSemaphore:
                case TutorialEvent.TutorialCompletionTriggers.placeSignal:
                    tutorialOverlay.ClosePanel();
                    GameManager.Instance.tracker.CreateEventExt("CloseTutorial", "placeDraggable");
                    break;

                case TutorialEvent.TutorialCompletionTriggers.simulationInteraction:
                    tutorialOverlay.ClosePanel();
					GameManager.Instance.tracker.CreateEventExt("CloseTutorial","simulationInteraction");
                    break;
            }
            t.hasCompleted = true;
        }
        else if (t.type == TutorialEvent.TutorialTypes.video) { }
        else if(t.type == TutorialEvent.TutorialTypes.simulation)
        {
            PlayerInteraction_GamePhaseBehavior playerInteractionBehavior = (PlayerInteraction_GamePhaseBehavior)GameManager.Instance.playerInteractionBehavior;
            t.hasCompleted = true;
        }
    }

    public void ReportTutorialEventComplete(TutorialEvent t)
    {
        CloseTutorial(t);
        tutorialEventIndex++;
        if (tutorialEventIndex < currentTutorialEventQueue.Length) InitializeCurrentTutorialEvent();
        else { 
			//Debug.Log("No more tutorials."); 
			GameManager.Instance.tracker.CreateEventExt("ReportTutorialEventComplete","NoMore");
		}
    }

    public void ReportTutorialEventSkip (bool allSubsequentForLevel)
    {
        GameManager.Instance.tracker.CreateEventExt("ReportTutorialEventSkip", "AllSubsequent="+allSubsequentForLevel.ToString());
        
        if (allSubsequentForLevel)
        {
            if (tutorialEventIndex < currentTutorialEventQueue.Length) CloseTutorial(currentTutorialEventQueue[tutorialEventIndex]);
            foreach (TutorialEvent t in currentTutorialEventQueue)
            {
                t.hasCompleted = true;
            }
            tutorialEventIndex = currentTutorialEventQueue.Length;

            int levelIndex = GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id;
            ForceTutorialSeriesCompletion(levelIndex);
        }
        else
        {
            if (tutorialEventIndex < currentTutorialEventQueue.Length)
            { 
                TutorialEvent targetTutorial = currentTutorialEventQueue[tutorialEventIndex];
                ReportTutorialEventComplete(targetTutorial);
            }
        }
    }

    void ForceTutorialSeriesCompletion(int levelIndex)
    {
        foreach (TutorialEvent t in tutorialEvents)
        {
            if(t.levelNumber == levelIndex) t.hasCompleted = true;
        }
    }

    public void ClearActiveTutorials()
    {
        //Debug.Log("ClearActiveTutorials");
        tutorialOverlay.ClosePanel();
    }

    [SerializeField]
    int tutorialEditIndex = 0; //this variable is for editing the tutorial events array without having to manually move data in conjunction with tutorial edit mode and the EditTutorial function
    [SerializeField]
    int tutorialEditMode = 1; // 0 -> Adds new tutorial at the above index; 1 -> Removes tutorial at the above index

    [ContextMenu("Edit Tutorial")]
    public void EditTutorial()
    {
        switch (tutorialEditMode)
        {
            case 0:
                AddNewTutorial(tutorialEditIndex);
                break;
            case 1:
                RemoveTutorial(tutorialEditIndex);
                break;
        }
    }

    void AddNewTutorial(int index)
    {
        TutorialEvent[] newTutorialEvents = new TutorialEvent[tutorialEvents.Length + 1];
        for(int i = 0; i < newTutorialEvents.Length; i++)
        {
            if(i < index)
            {
                newTutorialEvents[i] = tutorialEvents[i];
            }
            else if(i == index)
            {
                newTutorialEvents[i] = new TutorialEvent();
            }
            else
            {
                newTutorialEvents[i] = tutorialEvents[i-1];
            }
        }
        tutorialEvents = newTutorialEvents;
    }

    void RemoveTutorial(int index)
    {
        TutorialEvent[] newTutorialEvents = new TutorialEvent[tutorialEvents.Length - 1];
        for (int i = 0; i < newTutorialEvents.Length; i++)
        {
            if (i < index)
            {
                newTutorialEvents[i] = tutorialEvents[i];
            }
            else
            {
                newTutorialEvents[i] = tutorialEvents[i + 1];
            }
        }
        tutorialEvents = newTutorialEvents;
    }

    #region Legacy Tutorial Code
    List<TutorialScreen> RetrieveTutorialDisplays(int inputLevelId)
    {
        List<TutorialScreen> returnDisplays = new List<TutorialScreen>();
        foreach (TutorialScreen t in tutorialScreens)
        {
            if (t.tutorialLevelId == inputLevelId) returnDisplays.Add(t);
        }
        return returnDisplays;
    }

    void InitializeTutorial()
	{
		if(tutorialIndex >= tutorialDisplays.Length || tutorialIndex < 0) 
		{
			tutorialImageTriggerObject.gameObject.SetActive(false);
			tutorialMovieTriggerObject.gameObject.SetActive(false);
		}
		else 
		{
            if ( !string.IsNullOrEmpty( tutorialDisplays[tutorialIndex].tutorialLevelMovieName ) )
            {
                MovieTexture m = Resources.Load(System.IO.Path.Combine( "Videos", tutorialDisplays[tutorialIndex].tutorialLevelMovieName)) as MovieTexture;
                if (m != null)
                {
                    Debug.Log("Found video" + (tutorialDisplays[tutorialIndex].tutorialLevelMovieName));
                }
                else
                {
                    Debug.Log("Could not find video " + (tutorialDisplays[tutorialIndex].tutorialLevelMovieName));
                }
            }

            if (tutorialDisplays[tutorialIndex].tutorialLevelMovie != null)
            {
                tutorialMovieTriggerObject.GetComponent<RawImage>().texture = tutorialDisplays[tutorialIndex].tutorialLevelMovie;
                tutorialMovieTriggerObject.triggers.Clear();
                movieForwardTrigger.triggers.Clear();
                moviePauseTrigger.triggers.Clear();
				movieBackTrigger.gameObject.SetActive(false);

                tutorialDisplays[tutorialIndex].tutorialLevelMovie.Play();

                EventTrigger.Entry tap_tutorial = new EventTrigger.Entry();
                tap_tutorial.eventID = EventTriggerType.PointerUp;
                tutorialMovieTriggerObject.triggers.Add(tap_tutorial);
                tutorialMovieTriggerObject.gameObject.SetActive(true);

                EventTrigger.Entry next_tutorial = new EventTrigger.Entry();
                next_tutorial.eventID = EventTriggerType.PointerUp;
                next_tutorial.callback.AddListener((eventData) => { NextTutorial(); });

                EventTrigger.Entry pause_tutorial = new EventTrigger.Entry();
                pause_tutorial.eventID = EventTriggerType.PointerUp;
                pause_tutorial.callback.AddListener((eventData) => { TogglePauseMovie(); });
                isPlaying = true;

                movieForwardTrigger.triggers.Add(next_tutorial);
                movieForwardTrigger.gameObject.SetActive(true);
                moviePauseTrigger.triggers.Add(pause_tutorial);
                moviePauseTrigger.gameObject.SetActive(true);
                moviePauseTrigger.GetComponent<Image>().sprite = pauseSprite;
            }
            else
            {
                tutorialImageTriggerObject.GetComponent<Image>().sprite = tutorialDisplays[tutorialIndex].tutorialLevelImage;
                tutorialImageTriggerObject.triggers.Clear();
                imageForwardTrigger.triggers.Clear();
                tutorialImageTriggerObject.gameObject.SetActive(true);
				imageBackTrigger.gameObject.SetActive(false);

                EventTrigger.Entry next_tutorial = new EventTrigger.Entry();
                next_tutorial.eventID = EventTriggerType.PointerUp;
                next_tutorial.callback.AddListener((eventData) => { NextTutorial(); });

                imageForwardTrigger.triggers.Add(next_tutorial);
                imageForwardTrigger.gameObject.SetActive(true);
            }
		}
	}

    public void NextTutorial()
	{
		if(tutorialDisplays[tutorialIndex].tutorialLevelMovie != null) {
			tutorialDisplays[tutorialIndex].tutorialLevelMovie.Stop();
			tutorialMovieTriggerObject.gameObject.SetActive(false);
			movieForwardTrigger.gameObject.SetActive(false);
			movieBackTrigger.gameObject.SetActive(false);
			moviePauseTrigger.gameObject.SetActive(false);
		} else {
			tutorialImageTriggerObject.gameObject.SetActive(false);
			imageForwardTrigger.gameObject.SetActive(false);
			imageBackTrigger.gameObject.SetActive(false);
		}
		tutorialIndex++;
		if(tutorialIndex >= tutorialDisplays.Length)
		{
			GameManager.Instance.tracker.CreateEventExt("endTutorial",tutorialIndex.ToString());
			tutorialImageTriggerObject.gameObject.SetActive(false);
			tutorialMovieTriggerObject.gameObject.SetActive(false);
		}
		else 
		{ 
			if(tutorialIndex>=1)
			{
				InitializeTutorial();
				imageBackTrigger.triggers.Clear();
				movieBackTrigger.triggers.Clear();
				EventTrigger.Entry last_tutorial = new EventTrigger.Entry();
				last_tutorial.eventID = EventTriggerType.PointerUp;
				last_tutorial.callback.AddListener( (eventData) => { PreviousTutorial(); } );
				if(tutorialDisplays[tutorialIndex].tutorialLevelMovie != null) {
					movieBackTrigger.triggers.Add( last_tutorial );
					movieBackTrigger.gameObject.SetActive(true);
				} else {
					imageBackTrigger.triggers.Add( last_tutorial );
					imageBackTrigger.gameObject.SetActive(true);
				}
			}
			GameManager.Instance.tracker.CreateEventExt("nextTutorial",tutorialIndex.ToString());
		}
	}

	public void PreviousTutorial()
	{
		if(tutorialDisplays[tutorialIndex].tutorialLevelMovie != null) {
			tutorialDisplays[tutorialIndex].tutorialLevelMovie.Stop();
			tutorialMovieTriggerObject.gameObject.SetActive(false);
		}
		tutorialIndex--;
		if(tutorialIndex < 0)
		{
			tutorialImageTriggerObject.gameObject.SetActive(false);
			tutorialMovieTriggerObject.gameObject.SetActive(false);
		} else {
			InitializeTutorial();
			if(tutorialIndex == 0) {
				imageBackTrigger.triggers.Clear();
				movieBackTrigger.triggers.Clear();
				imageBackTrigger.gameObject.SetActive(false);
				movieBackTrigger.gameObject.SetActive(false);
			} else {
				imageBackTrigger.triggers.Clear();
				movieBackTrigger.triggers.Clear();
				EventTrigger.Entry last_tutorial = new EventTrigger.Entry();
				last_tutorial.eventID = EventTriggerType.PointerUp;
				last_tutorial.callback.AddListener( (eventData) => { PreviousTutorial(); } );
				if(tutorialDisplays[tutorialIndex].tutorialLevelMovie != null) {
					movieBackTrigger.gameObject.SetActive(true);
					movieBackTrigger.triggers.Add( last_tutorial );
				} else {
					imageBackTrigger.gameObject.SetActive(true);
					imageBackTrigger.triggers.Add( last_tutorial );
				}
				GameManager.Instance.tracker.CreateEventExt("previousTutorial",tutorialIndex.ToString());
			}
		}
	}
	
	public void TogglePauseMovie()
	{
		if(isPlaying) {
			tutorialDisplays[tutorialIndex].tutorialLevelMovie.Pause();
		} else {
			tutorialDisplays[tutorialIndex].tutorialLevelMovie.Play();
		}
		moviePauseTrigger.GetComponent<Image>().sprite = isPlaying? playSprite:pauseSprite;
		isPlaying = !isPlaying;
	}
    #endregion
}
