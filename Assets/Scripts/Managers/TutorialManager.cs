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

        public override void OpenPanel()
        {
            iTween.Stop(panelContainer.gameObject);
            Vector2 pivotOffset = Vector2.zero;
            if (panelContainer.position.x < Screen.width * 0.2f) { pivotOffset.x = 0.0f; }
            if (panelContainer.position.x > Screen.width * 0.8f) { pivotOffset.x = 1.0f; }

            if (panelContainer.position.y < Screen.height * 0.2f) { pivotOffset.y = 0.0f; }
            if (panelContainer.position.y > Screen.height * 0.8f) { pivotOffset.y = 1.0f; }
            panelContainer.pivot = pivotOffset;
            base.OpenPanel();
        }

        public void SetTutorialText(string inDescription)
        {
            tutorialDescription.text = inDescription;
        }
        public void PrepareTutorialPopup(string inDescription, Vector3 inputPosition)
        {
            panelContainer.position = inputPosition;
            SetTutorialText(inDescription);
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
                    Vector3 defaultPosition = new Vector3(Screen.width / 2, Screen.height / 2, 0);
                    if (t.targetButton != null)
                    {
                        defaultPosition = t.targetButton.transform.position;
                        if (defaultPosition.x > (Screen.width / 2)) { defaultPosition.x -= t.targetButton.GetComponent<RectTransform>().rect.width/2; }
                        else { defaultPosition.x += t.targetButton.GetComponent<RectTransform>().rect.width/2; }
                        if (defaultPosition.y > (Screen.height / 2)) { defaultPosition.x -= t.targetButton.GetComponent<RectTransform>().rect.height/2; }
                        else { defaultPosition.x += t.targetButton.GetComponent<RectTransform>().rect.height/2; }
                    }
                    tutorialOverlay.PrepareTutorialPopup(t.popupDescription,defaultPosition);
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
