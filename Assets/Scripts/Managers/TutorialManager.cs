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
        public Button tutorialCloseButton;
        public Canvas canvas;
        public CanvasScaler scaler;
        public RectTransform tutorialArrow;

        public override void OpenPanel()
        {
            Vector2 pivotOffset = Vector2.zero;
            if (panelContainer.position.x < Screen.width * 0.2f) { pivotOffset.x = 0.0f; }
            if (panelContainer.position.x > Screen.width * 0.8f) { pivotOffset.x = 1.0f; }

            if (panelContainer.position.y < Screen.height * 0.2f) { pivotOffset.y = 0.0f; }
            if (panelContainer.position.y > Screen.height * 0.8f) { pivotOffset.y = 1.0f; }
            //panelContainer.pivot = pivotOffset;
            base.OpenPanel();
        }

        public void SetTooltip(string inDescription, Button button)
        {
            if (button != null)
            {
                RectTransform elementRect = button.gameObject.GetComponent<RectTransform>();
                panelContainer.position = button.gameObject.transform.position;
                float posX = button.gameObject.transform.position.x;
                float posY = button.gameObject.transform.position.y;

                float multiplier = canvas.pixelRect.width / scaler.referenceResolution.x;
                RectTransform tooltip = panelContainer.GetChild(0).GetComponent<RectTransform>();

                float ttXMin = panelContainer.position.x + tooltip.rect.xMin;
                float ttXMax = panelContainer.position.x + tooltip.rect.xMax;
                float ttYMin = panelContainer.position.y + tooltip.rect.yMin;
                float ttYMax = panelContainer.position.y + tooltip.rect.yMax;
                float ttWidth = tooltip.rect.width * multiplier;
                float ttHeight = tooltip.rect.height * multiplier;

                float eXMin = elementRect.position.x + elementRect.rect.xMin;
                float eXMax = elementRect.position.x + elementRect.rect.xMax;
                float eYMin = elementRect.position.y + elementRect.rect.yMin;
                float eYMax = elementRect.position.y + elementRect.rect.yMax;
                float eWidth = elementRect.rect.width * multiplier;
                float eHeight = elementRect.rect.width * multiplier;

                float topBannerHeight = GameObject.Find("Top_Banner").GetComponent<RectTransform>().rect.height;
                float rightBannerWidth = GameObject.Find("Right_Banner").GetComponent<RectTransform>().rect.width;

                if (ttXMin < 0)
                {
                    posX += Mathf.Abs(posX - (ttWidth / 2));
                }
                else if (ttXMax > Screen.width)
                {
                    posX -= (ttWidth / 2) + posX - Screen.width;
                }
                if (ttYMin < 0)
                {
                    posY += Mathf.Abs(posY - (ttHeight / 2));
                }
                else if (ttYMax > Screen.height)
                {
                    posY -= (ttHeight / 2) + posY - Screen.height /* NEW: + topBannerHeight */;
                }

                ttXMin = posX - (ttWidth / 2);
                ttXMax = posX + (ttWidth / 2);
                ttYMin = posY - (ttHeight / 2);
                ttYMax = posY + (ttHeight / 2);

                float normalizedX = posX / canvas.pixelRect.width;
                float normalizedY = posY / canvas.pixelRect.height;

                if (ttXMin <= eXMax)
                {
                    if (ttXMin >= eXMin || ttXMax >= eXMin)
                    {
                        if (ttYMin <= eYMax)
                        {
                            if (ttYMin >= eYMin || ttYMax >= eYMin)
                            {
                                if (normalizedX >= 0.5f)
                                {
                                    if (normalizedY >= 0.5f)
                                    {
                                        if (normalizedX > normalizedY)
                                        {
                                            posX = posX - (posX - elementRect.position.x) - ((elementRect.position.x - eXMin) * multiplier) - (ttWidth / 2);
                                        }
                                        else
                                        {
                                            posY = posY - (posY - elementRect.position.y) - ((elementRect.position.y - eYMin) * multiplier) - (ttHeight / 2);
                                        }
                                    }
                                    else
                                    {
                                        if (normalizedX - 0.5f >= 0.5f - normalizedY)
                                        {
                                            posX = posX - (posX - elementRect.position.x) - ((elementRect.position.x - eXMin) * multiplier) - (ttWidth / 2);
                                        }
                                        else
                                        {
                                            posY = posY + (elementRect.position.y - posY) + ((elementRect.position.y + eYMax) * multiplier) + (ttHeight / 2);
                                        }
                                    }
                                }
                                else
                                {
                                    if (normalizedY < 0.5f)
                                    {
                                        if (normalizedX < normalizedY)
                                        {
                                            posX = posX + (elementRect.position.x - posX) + ((elementRect.position.x + eXMax) * multiplier) + (ttWidth / 2);
                                        }
                                        else
                                        {
                                            posY = posY + (elementRect.position.y - posY) + ((elementRect.position.y + eYMax) * multiplier) + (ttHeight / 2);
                                        }
                                    }
                                    else
                                    {
                                        if (normalizedY - 0.5f >= 0.5f - normalizedX)
                                        {
                                            posY = posY - (posY - elementRect.position.y) - ((elementRect.position.y - eYMin) * multiplier) - (ttHeight / 2);
                                        }
                                        else
                                        {
                                            posX = posX + (elementRect.position.x - posX) + ((elementRect.position.x + eXMax) * multiplier) + (ttWidth / 2);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Vector3 nextPanelPosition = new Vector3(posX, posY, panelContainer.position.z);
                PositionTutorialPanel(nextPanelPosition, button.transform.position);
            }
            tutorialDescription.text = inDescription;
        }

        void PositionTutorialPanel(Vector3 position, Vector3 tutorialFocusTargetPosition )
        {
            Vector3 start = position;
            panelContainer.position = start;
            Vector3 end = tutorialFocusTargetPosition;
            end.z = start.z;
            TutorialBubbleNubRotateToward(start, end);
        }

        void TutorialBubbleNubRotateToward(Vector3 start, Vector3 end)
        {

            Vector3 ray = end - start;
            float rad = Mathf.Atan2(ray.y, ray.x); // In radians
            float deg = rad * (180 / Mathf.PI) + 90f; //starts from bottom instead of from right
            tutorialArrow.localRotation = Quaternion.Euler(0,0,deg);
            Debug.Log("DEG: " + deg);

            //DOWN is default rotation. (0,0,0)
            //Vector3 ray = end - start;
            /*
            Vector3 avgPos = (start + end) / 2f;
            Vector3 targetRot = Vector3.zero;
            if (ray.y == 0) // not up or down
            {
                if (ray.x == 0) { }
                else if (ray.x < 0) { targetRot = Vector3.forward * -90f; }
                else if (ray.x > 0) { targetRot = Vector3.forward * 90f; }
            }
            else if (ray.y < 0) // down
            {
                targetRot = Vector3.forward * 0f;
                if (ray.x == 0) { }
                else if (ray.x < 0) { targetRot += Vector3.forward * -45f; }
                else if (ray.x > 0) { targetRot += Vector3.forward * 45f; }
            }
            else if (ray.y > 0) // up
            {
                targetRot = Vector3.forward * 180f;
                if (ray.x == 0) { }
                else if (ray.x < 0) { targetRot += Vector3.forward * 45f; }
                else if (ray.x > 0) { targetRot += Vector3.forward * -45f; }
            }
            */
            //tutorialArrow.rotation = Quaternion.Euler(targetRot);
        }


        public void SetTooltip(string inDescription, GameObject element)
        {
            SpriteRenderer sprite = element.GetComponent<SpriteRenderer>();
            panelContainer.position = element.transform.position;
            float posX = element.transform.position.x;
            float posY = element.transform.position.y;
            
            float multiplier = canvas.pixelRect.width / scaler.referenceResolution.x;
            RectTransform tooltip = panelContainer.GetChild(0).GetComponent<RectTransform>();

            float ttXMin = panelContainer.position.x + tooltip.rect.xMin;
            float ttXMax = panelContainer.position.x + tooltip.rect.xMax;
            float ttYMin = panelContainer.position.y + tooltip.rect.yMin;
            float ttYMax = panelContainer.position.y + tooltip.rect.yMax;
            float ttWidth = tooltip.rect.width * multiplier;
            float ttHeight = tooltip.rect.height * multiplier;

            float eXMin = sprite.bounds.min.x;
            float eXMax = sprite.bounds.max.x;
            float eYMin = sprite.bounds.min.y;
            float eYMax = sprite.bounds.max.y;
            float eWidth = sprite.bounds.size.x;
            float eHeight = sprite.bounds.size.y;
            /*
            if (ttXMin < 0)
            {
                posX += Mathf.Abs(posX - (ttWidth / 2));
            }
            else if (ttXMax > Screen.width)
            {
                posX -= (ttWidth / 2) + posX - Screen.width;
            }
            if (ttYMin < 0)
            {
                posY += Mathf.Abs(posY - (ttHeight / 2));
            }
            else if (ttYMax > Screen.height)
            {
                posY -= (ttHeight / 2) + posY - Screen.height;
            }

            ttXMin = posX - (ttWidth / 2);
            ttXMax = posX + (ttWidth / 2);
            ttYMin = posY - (ttHeight / 2);
            ttYMax = posY + (ttHeight / 2);

            float normalizedX = posX / canvas.pixelRect.width;
            float normalizedY = posY / canvas.pixelRect.height;

            if (ttXMin <= eXMax)
            {
                if (ttXMin >= eXMin || ttXMax >= eXMin)
                {
                    if (ttYMin <= eYMax)
                    {
                        if (ttYMin >= eYMin || ttYMax >= eYMin)
                        {
                            if (normalizedX >= 0.5f)
                            {
                                if (normalizedY >= 0.5f)
                                {
                                    if (normalizedX > normalizedY)
                                    {
                                        posX = posX - (posX - sprite.bounds.center.x) - ((sprite.bounds.center.x - eXMin) * multiplier) - (ttWidth / 2);
                                    }
                                    else
                                    {
                                        posY = posY - (posY - sprite.bounds.center.y) - ((sprite.bounds.center.y - eYMin) * multiplier) - (ttHeight / 2);
                                    }
                                }
                                else
                                {
                                    if (normalizedX - 0.5f >= 0.5f - normalizedY)
                                    {
                                        posX = posX - (posX - sprite.bounds.center.x) - ((sprite.bounds.center.x - eXMin) * multiplier) - (ttWidth / 2);
                                    }
                                    else
                                    {
                                        posY = posY + (sprite.bounds.center.y - posY) + ((sprite.bounds.center.y + eYMax) * multiplier) + (ttHeight / 2);
                                    }
                                }
                            }
                            else
                            {
                                if (normalizedY < 0.5f)
                                {
                                    if (normalizedX < normalizedY)
                                    {
                                        posX = posX + (sprite.bounds.center.x - posX) + ((sprite.bounds.center.x + eXMax) * multiplier) + (ttWidth / 2);
                                    }
                                    else
                                    {
                                        posY = posY + (sprite.bounds.center.y - posY) + ((sprite.bounds.center.y + eYMax) * multiplier) + (ttHeight / 2);
                                    }
                                }
                                else
                                {
                                    if (normalizedY - 0.5f >= 0.5f - normalizedX)
                                    {
                                        posY = posY - (posY - sprite.bounds.center.y) - ((sprite.bounds.center.y - eYMin) * multiplier) - (ttHeight / 2);
                                    }
                                    else
                                    {
                                        posX = posX + (sprite.bounds.center.x - posX) + ((sprite.bounds.center.x + eXMax) * multiplier) + (ttWidth / 2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            */

            Camera gameCamera = GameObject.Find("UICamera").GetComponent<Camera>();

            Vector3 start= new Vector3(Screen.width / 2, Screen.height / 2, panelContainer.position.z);
            //start = new Vector3(posX, posY, panelContainer.position.z);

            Vector3 end = gameCamera.WorldToScreenPoint(new Vector3(posX, posY, panelContainer.position.z));

            PositionTutorialPanel(start, end);
            panelContainer.position = start;
            
            tutorialDescription.text = inDescription;
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
        Debug.Log("perform tutorial series");
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
        Debug.Log("Retrieving Tutorial Events... Level " + inputLevelId + " Phase " + inputInitPhase.ToString());
        List<TutorialEvent> returnEvents = new List<TutorialEvent>();
        PlayerInteraction_GamePhaseBehavior p = (PlayerInteraction_GamePhaseBehavior)GameManager.Instance.playerInteractionBehavior;

        foreach (TutorialEvent t in tutorialEvents)
        {
            if (t.init_trigger == inputInitPhase)
            {
                if (t.levelNumber == inputLevelId && !t.hasCompleted) returnEvents.Add(t);
            }
            else if (t.init_trigger != inputInitPhase && returnEvents.Count > 0) break;
        }
        foreach (TutorialEvent t in returnEvents) Debug.Log("TUTORIAL QUEUED: " + t.popupDescription + "\n");
        return returnEvents;
    }

    void InitializeCurrentTutorialEvent()
    {
        if (tutorialEventIndex >= currentTutorialEventQueue.Length )
        {
            Debug.Log("All tutorials complete for this level.");
        }
        else
        {
            Debug.Log(tutorialIndex.ToString() + " is current Tutorial index.");
            TutorialEvent currentTutorial = currentTutorialEventQueue[tutorialEventIndex];
            PerformTutorial(currentTutorial);
            if(currentTutorialEventQueue.Length > tutorialEventIndex + 2) //is there a next tutorial?
            {
                TutorialEvent nextTutorial = currentTutorialEventQueue[tutorialEventIndex + 1];
                if (nextTutorial.type == TutorialEvent.TutorialTypes.simulation)
                {
                    Debug.Log("PREFORMING SIM");
                    PerformTutorial(nextTutorial);
                }
            }
        }
    }

    void PerformTutorial(TutorialEvent t)
    {
        if (t.type == TutorialEvent.TutorialTypes.popup)
        {
            switch (t.complete_trigger)
            {
                case TutorialEvent.TutorialCompletionTriggers.clickButton:
                case TutorialEvent.TutorialCompletionTriggers.clickPopup:
                case TutorialEvent.TutorialCompletionTriggers.placeSignal:
                case TutorialEvent.TutorialCompletionTriggers.placeSemaphore:
                    GameManager.Instance.tracker.CreateEventExt("PerformTutorial",t.popupDescription);
                    tutorialOverlay.SetTooltip(t.popupDescription, t.targetButton);
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
        if (tutorialIndex < currentTutorialEventQueue.Length) InitializeCurrentTutorialEvent();
        else { 
			Debug.Log("No more tutorials."); 
			GameManager.Instance.tracker.CreateEventExt("ReportTutorialEventComplete","NoMore");
		}
    }

    public void ClearActiveTutorials()
    {
        Debug.Log("ClearActiveTutorials");
        tutorialOverlay.ClosePanel();
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
