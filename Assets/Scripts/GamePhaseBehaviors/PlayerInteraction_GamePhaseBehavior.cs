using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections.Generic;
using System;

public class PlayerInteraction_GamePhaseBehavior : GamePhaseBehavior {
    public enum InteractionPhases { ingame_default, ingame_dragging, ingame_placing, ingame_connecting, ingame_help, simulation, awaitingSimulation }
    public InteractionPhases interactionPhase = InteractionPhases.simulation;

    public Playback_PlayerInteractionPhaseBehavior playbackBehavior;

    public enum InGamePhases { none, optionClicked, movingObject, placingObject }
    public enum MenuOptions { pause, semaphore, button, trash, simulate }

    bool flowVisibility = false;
    public bool trashHover = false;
    bool connectVisibility;
    public bool connectVisibilityLock = false;
    public bool colorFlowVisibilityLock = false;

    public PlayerInteraction_UI playerInteraction_UI;

    GridObjectBehavior currentGridObject;

    public LevelScore score;

    public bool tutorialMode;
    bool dragging;

    float stationaryTime = 0f;
    Dictionary<int, List<StepData>> simulationDStepDictionary = new Dictionary<int, List<StepData>>();

    Vector3 stationaryMousePosition;
    GridObjectBehavior hoverObject;
    GridObjectBehavior hoverObject_;

    public LayerMask defaultMask, draggingMask;

    //for zooming
    float originalOrthographicSize = 0f;
    float zoomLevel = 1f;
    float maxOrtho = 1f;
    float minOrtho = 4f;
    [SerializeField]
    float xMin;
    [SerializeField]
    float xMax;
    [SerializeField]
    float yMin;
    [SerializeField]
    float yMax;
    Coroutine zoomRoutine;
    Coroutine panRoutine;
    Vector3 originalCameraPosition = Vector3.zero;
    Vector3 currentCameraPosition = Vector3.zero;
    bool isZooming = false;

    Vector2 lastMousePos;
    Vector2 currentMousePos;
    Vector2 deltaMousePos;

    public delegate void OnSimulationStepDelegate(StepData step);
    public static OnSimulationStepDelegate onSimulationStep;

    public delegate void PauseSimulationDelegate();
    public static PauseSimulationDelegate pauseSimulation;

    public delegate void UnpauseSimulationDelegate();
    public static UnpauseSimulationDelegate unpauseSimulation;

    public delegate void DelayedUnpauseDelegate(float delay, TutorialEvent t);
    public static DelayedUnpauseDelegate delayedUnpause;

    public delegate void OnCompletionDelegate();
    public static OnCompletionDelegate onCompletion;

    public delegate void OnMenuInteractionDelegate(MenuOptions inputMenuOption);
    public static OnMenuInteractionDelegate onMenuInteraction;
    
    public override void BeginPhase()
    {
        pauseSimulation += PauseSimulation;
        unpauseSimulation += UnpauseSimulation;
        delayedUnpause += DelayedUnpauseSimulation;

        playerInteraction_UI.SetText(GameManager.Instance.GetDataManager().currentLevelData);
        playerInteraction_UI.OpenUI();
        DefineButtonBehaviors();

        playerInteraction_UI.startTime = Time.fixedTime;

        score.index = GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id;

        if (GameManager.Instance.GetDataManager().currentLevelData.metadata.level_type != -1)
        {
            GameManager.Instance.TriggerLevelTutorial
            (
                GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id,
                interactionPhase == InteractionPhases.awaitingSimulation || interactionPhase == InteractionPhases.simulation ? TutorialEvent.TutorialInitializeTriggers.duringSimulation : TutorialEvent.TutorialInitializeTriggers.beforePlay
            );
        }

        GameManager.Instance.SetUpLevelInventory();

        //for zooming
        originalOrthographicSize = GameManager.Instance.GetGridManager().worldCamera.orthographicSize;
        maxOrtho = originalOrthographicSize * 1.5f;
        zoomLevel = 0.66f;
        playerInteraction_UI.zoomMeter.SetMeterValue(zoomLevel);
        originalCameraPosition = GameManager.Instance.GetGridManager().worldCamera.transform.position;
        xMax = originalCameraPosition.x * 2;
        yMax = originalCameraPosition.y * 2;
        currentCameraPosition = originalCameraPosition;
        isZooming = false;

        lastMousePos = currentMousePos;
        currentMousePos = Input.mousePosition;

        if (PlayerPrefs.HasKey("LinkHover"))
        {

        }
        else
        {

        }
        if (PlayerPrefs.HasKey(""))
        {

        }
        else
        {

        }
    }

	public override void UpdatePhase()
	{
		if(interactionPhase == InteractionPhases.simulation)
		{
			GameManager.Instance.TriggerTrackUpdate();
            PlayerInteractionListener();
        }
		else 
		{
			PlayerInteractionListener();
		}
        //playerInteraction_UI.Timer();
	}

	public override void EndPhase()
	{
		playerInteraction_UI.CloseUI();
        LockFlowVisibility(-1);

        //cleanup tutorials
        onCompletion = null;
        onMenuInteraction = null;
        onSimulationStep = null;
    }

    public void DefineButtonBehaviors()
    {
        playerInteraction_UI.ClearButtonBehaviors();
        foreach (Transform t in playerInteraction_UI.hint_button_container) { Destroy(t.gameObject); }

        /* semaphore placement events */
        EventTrigger.Entry beginDrag_semaphore = new EventTrigger.Entry();
        beginDrag_semaphore.eventID = EventTriggerType.BeginDrag;
        beginDrag_semaphore.callback.AddListener((eventData) => { BeginDrag(MenuOptions.semaphore); });
        playerInteraction_UI.place_semaphore.triggers.Add(beginDrag_semaphore);

        EventTrigger.Entry continueDrag_semaphore = new EventTrigger.Entry();
        continueDrag_semaphore.eventID = EventTriggerType.Drag;
        continueDrag_semaphore.callback.AddListener((eventData) => { ContinueDrag(MenuOptions.semaphore); });
        playerInteraction_UI.place_semaphore.triggers.Add(continueDrag_semaphore);

        EventTrigger.Entry endDrag_semaphore = new EventTrigger.Entry();
        endDrag_semaphore.eventID = EventTriggerType.EndDrag;
        endDrag_semaphore.callback.AddListener((eventData) => { EndDrag(MenuOptions.semaphore); });
        playerInteraction_UI.place_semaphore.triggers.Add(endDrag_semaphore);

        /* signal placement events */
        EventTrigger.Entry beginDrag_button = new EventTrigger.Entry();
        beginDrag_button.eventID = EventTriggerType.BeginDrag;
        beginDrag_button.callback.AddListener((eventData) => { BeginDrag(MenuOptions.button); });
        playerInteraction_UI.place_button.triggers.Add(beginDrag_button);

        EventTrigger.Entry continueDrag_button = new EventTrigger.Entry();
        continueDrag_button.eventID = EventTriggerType.Drag;
        continueDrag_button.callback.AddListener((eventData) => { ContinueDrag(MenuOptions.button); });
        playerInteraction_UI.place_button.triggers.Add(continueDrag_button);

        EventTrigger.Entry endDrag_button = new EventTrigger.Entry();
        endDrag_button.eventID = EventTriggerType.EndDrag;
        endDrag_button.callback.AddListener((eventData) => { EndDrag(MenuOptions.button); });
        playerInteraction_UI.place_button.triggers.Add(endDrag_button);

        /* trash events */
        EventTrigger.Entry hover_trash = new EventTrigger.Entry();
        hover_trash.eventID = EventTriggerType.PointerEnter;
        hover_trash.callback.AddListener((eventData) => { BeginHover(MenuOptions.trash); });
        playerInteraction_UI.trash.triggers.Add(hover_trash);

        EventTrigger.Entry click_trash = new EventTrigger.Entry();
        click_trash.eventID = EventTriggerType.PointerDown;
        //click_trash.callback.AddListener((eventData) => { ResetPlacedObjects(); });
        playerInteraction_UI.trash.triggers.Add(click_trash);

        EventTrigger.Entry endHover_trash = new EventTrigger.Entry();
        endHover_trash.eventID = EventTriggerType.PointerExit;
        endHover_trash.callback.AddListener((eventData) => { EndHover(MenuOptions.trash); });
        playerInteraction_UI.trash.triggers.Add(endHover_trash);

        EventTrigger.Entry hover_bezier = new EventTrigger.Entry();
        hover_bezier.eventID = EventTriggerType.PointerEnter;
        hover_bezier.callback.AddListener((eventData) => { Debug.Log("Bezier"); connectVisibility = false; ToggleConnectionVisibility(); });
        playerInteraction_UI.preview.triggers.Add(hover_bezier);

        EventTrigger.Entry click_bezier = new EventTrigger.Entry();
        click_bezier.eventID = EventTriggerType.PointerDown;
        click_bezier.callback.AddListener((eventData) => { LockConnectionVisibility(); });
        playerInteraction_UI.preview.triggers.Add(click_bezier);

        EventTrigger.Entry endHover_bezier = new EventTrigger.Entry();
        endHover_trash.eventID = EventTriggerType.PointerExit;
        endHover_trash.callback.AddListener((eventData) => { connectVisibility = true; ToggleConnectionVisibility(); });
        playerInteraction_UI.preview.triggers.Add(endHover_trash);

        playerInteraction_UI.exitButton.onClick.RemoveAllListeners();
        playerInteraction_UI.exitButton.onClick.AddListener(() => ToggleExitMenu());
        playerInteraction_UI.exitButton.interactable = true;

        LinkJava.SimulationTypes playSimulation = LinkJava.SimulationTypes.Play;
        playerInteraction_UI.simulationButton.onClick.RemoveAllListeners();
        playerInteraction_UI.simulationButton.onClick.AddListener(() => TriggerSimulation(playSimulation)/*GameManager.Instance.SubmitCurrentLevel(playSimulation)*/ );
        playerInteraction_UI.simulationButton.onClick.AddListener(() => playerInteraction_UI.simulationButton.interactable = false);
        playerInteraction_UI.simulationButton.interactable = true;

        playerInteraction_UI.stopSimulationButton.onClick.RemoveAllListeners();
        playerInteraction_UI.stopSimulationButton.onClick.AddListener(() => { Debug.Log("STOP"); GameManager.Instance.tracker.CreateEventExt("StopSimulation", "stoppedViaButton"); EndSimulation(); });
        playerInteraction_UI.stopSimulationButton.interactable = false;
        playerInteraction_UI.stopSimulationButton.gameObject.SetActive(false);

        playerInteraction_UI.pauseSimulationButton.onClick.RemoveAllListeners();
        playerInteraction_UI.pauseSimulationButton.onClick.AddListener(() => { playbackBehavior.PauseSimulation(); });
        playerInteraction_UI.pauseSimulationButton.interactable = false;
        playerInteraction_UI.pauseSimulationButton.gameObject.SetActive(false);

        playerInteraction_UI.playbackSlider.onValueChanged.RemoveAllListeners();
        playerInteraction_UI.playbackSlider.onValueChanged.AddListener((float value) => { playbackBehavior.OnTimeSliderValueChanged((int)value); });
        playerInteraction_UI.playbackSlider.interactable = false;
        playerInteraction_UI.playbackSlider.gameObject.SetActive(false);

		LinkJava.SimulationTypes fullSimulation = LinkJava.SimulationTypes.ME;
		playerInteraction_UI.submitButton.onClick.RemoveAllListeners();
		playerInteraction_UI.submitButton.onClick.AddListener( ()=> TriggerSimulation(fullSimulation)/*GameManager.Instance.SubmitCurrentLevel(fullSimulation)*/ );
		playerInteraction_UI.submitButton.onClick.AddListener( ()=> playerInteraction_UI.submitButton.interactable = false );
        playerInteraction_UI.submitButton.onClick.AddListener(() => score.attemptCount++ );
        playerInteraction_UI.submitButton.interactable = true;

		playerInteraction_UI.revealHintsToggle.toggleButton.onClick.RemoveAllListeners();
		playerInteraction_UI.revealHintsToggle.toggleButton.onClick.AddListener( ()=> ToggleHintsVisibility() );

/* Track Color Hover Setup */
		for(int triggerIndex = 0; triggerIndex < playerInteraction_UI.rightPanelColors.Length; triggerIndex++)
		{
            playerInteraction_UI.rightPanelColors[triggerIndex].triggers.Clear();
            if ( GameManager.Instance.GetGridManager().IsCurrentThreadColor( triggerIndex ) )
			{
				playerInteraction_UI.rightPanelColors[triggerIndex].gameObject.SetActive(true);
				int loadColors = triggerIndex;// + 1;

                EventTrigger.Entry beginHoverColor = new EventTrigger.Entry();
				beginHoverColor.eventID = EventTriggerType.PointerEnter;
				beginHoverColor.callback.AddListener( (eventData) => {
					if ( !colorFlowVisibilityLock )  GameManager.Instance.GetGridManager().RevealGridColorMask(loadColors);
                    ToggleFlowVisibility(true);
                } );
				playerInteraction_UI.rightPanelColors[triggerIndex].triggers.Add(beginHoverColor);

                EventTrigger.Entry lockHoverColor = new EventTrigger.Entry();
                lockHoverColor.eventID = EventTriggerType.PointerDown;
                int lockIndex = triggerIndex;
                lockHoverColor.callback.AddListener((eventData) => { LockFlowVisibility(lockIndex); });
                playerInteraction_UI.rightPanelColors[triggerIndex].triggers.Add(lockHoverColor);


                EventTrigger.Entry endHoverColor = new EventTrigger.Entry();
				endHoverColor.eventID = EventTriggerType.PointerExit;
				endHoverColor.callback.AddListener( (eventData) => {
					if ( !colorFlowVisibilityLock ) GameManager.Instance.GetGridManager().HideGridColorMask();
                    ToggleFlowVisibility(false);
                } );
				playerInteraction_UI.rightPanelColors[triggerIndex].triggers.Add(endHoverColor);
			}
			else 
			{
//				Debug.Log(playerInteraction_UI.rightPanelColors[triggerIndex].gameObject.name);
				playerInteraction_UI.rightPanelColors[triggerIndex].gameObject.SetActive(false);
			}
		}

/* Question Mark Hint Setup */
		for(int hintIndex = 0; hintIndex < playerInteraction_UI.hintButtons.Length; hintIndex++)
		{
			HintConstructor h = playerInteraction_UI.hintButtons[hintIndex].hint;
			if(playerInteraction_UI.hintButtons[hintIndex].levelIds.Count == 0 || playerInteraction_UI.hintButtons[hintIndex].levelIds.Contains( GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id )) 
			{
				GameObject hintInstance = Instantiate( playerInteraction_UI.hint_button_prefab, playerInteraction_UI.hint_button_container ) as GameObject;
				hintInstance.transform.localScale = Vector3.one;
				Button instanceButton = hintInstance.GetComponent<Button>();
				Image instanceImage = hintInstance.GetComponent<Image>();
				instanceButton.onClick.AddListener( ()=> TriggerHint( h.hintTitle, h.hintDescription, h.hintImage ) );

				if(playerInteraction_UI.hintButtons[hintIndex].hint.hintButtonImage!=null)
				{
					//	playerInteraction_UI.hintButtons[hintIndex].hintButtonTrigger.GetComponent<Image>().sprite = playerInteraction_UI.hintButtons[hintIndex].hint.hintButtonImage;
					instanceImage.sprite =  playerInteraction_UI.hintButtons[hintIndex].hint.hintButtonImage;
				}
				else if(playerInteraction_UI.hintButtons[hintIndex].hint.hintImage!=null)
				{
					//playerInteraction_UI.hintButtons[hintIndex].hintButtonTrigger.GetComponent<Image>().sprite = playerInteraction_UI.hintButtons[hintIndex].hint.hintImage;
					instanceImage.sprite =  playerInteraction_UI.hintButtons[hintIndex].hint.hintImage;
				}
				//playerInteraction_UI.hintButtons[hintIndex].hintButtonTrigger.onClick.AddListener( ()=> TriggerHint( h.hintTitle, h.hintDescription, h.hintImage ) );	
			}
		}
        /* Toolbar Tooltips */
        foreach (TooltipEvent t in playerInteraction_UI.tooltipEvents)
        {
            EventTrigger.Entry beginHover_event = new EventTrigger.Entry();
            string tooltipText = t.tooltipContent.tooltipText;
            string tooltipName = t.tooltipUiElement.name;
            string refString = t.refString;
            GameObject tooltipElement = t.tooltipUiElement.gameObject;
            beginHover_event.eventID = EventTriggerType.PointerEnter;
            if (t.permanent)
            {
                beginHover_event.callback.AddListener((eventData) => 
                {
                    if (interactionPhase == InteractionPhases.ingame_default)
                    {
                        playerInteraction_UI.tooltipOverlay.OpenPanel();
                        playerInteraction_UI.tooltipOverlay.SetTooltip(tooltipText, tooltipElement);
                        GameManager.Instance.tracker.CreateEventExt("tooltip", tooltipName);
                    }
                });
            }
            else
            {
                if(!PlayerPrefs.HasKey(refString) || PlayerPrefs.GetInt(refString) == 0)
                {
                    beginHover_event.callback.AddListener((eventData) => 
                    {
                        if (interactionPhase == InteractionPhases.ingame_default)
                        {
                            playerInteraction_UI.tooltipOverlay.OpenPanel(); playerInteraction_UI.tooltipOverlay.SetTooltip(tooltipText, tooltipElement); GameManager.Instance.tracker.CreateEventExt("tooltip", tooltipName);
                            PlayerPrefs.SetInt(refString, 1); beginHover_event.callback.RemoveAllListeners();
                        }
                    });
                }
            }
            t.tooltipUiElement.triggers.Add(beginHover_event);
        }
    }

	public void TriggerHint(string title, string description, Sprite texture)
	{
		GameManager.Instance.tracker.CreateEventExt("hint",title);
		playerInteraction_UI.hintOverlay.OpenPanel();
		playerInteraction_UI.hintOverlay.SetHint(title,description,texture);
	}

    public void TriggerHint(string objectName)
    {
        bool success = false;
        HintConstructor h = GameManager.Instance.hintGlossary.GetHintForComponent(objectName, out success);
        if (success)
            TriggerHint(h.hintTitle, h.hintDescription, h.hintImage);
        else
            Debug.LogWarning("Failed to find hint for component of type: " + objectName);
    }

	public void BeginDrag(MenuOptions selectedOption)
	{
		if(interactionPhase != InteractionPhases.ingame_default) return;

        interactionPhase = InteractionPhases.ingame_placing;

		Sprite[] spriteSheet = Resources.LoadAll<Sprite>("Sprites/gridsprites_v3") as Sprite[];
		GameManager.Instance.tracker.CreateEventExt("startDrag",selectedOption.ToString());

        if (onMenuInteraction != null) onMenuInteraction(selectedOption);
        switch (selectedOption)
		{
			case MenuOptions.semaphore:
				foreach(Sprite s in spriteSheet)
				{
					if(s.name == "semaphore")
					{
						playerInteraction_UI.SetDraggableElement( s );	
					}
				}
			break;
			case MenuOptions.button:
				foreach(Sprite s in spriteSheet)
				{
					if(s.name == "button_up")
					{
						playerInteraction_UI.SetDraggableElement( s );	
					}
				}
			break;
		}
        Vector3 draggableItemScale = Vector3.one;
        draggableItemScale *= originalOrthographicSize /* default ortho size */ / (GameManager.Instance.GetGridManager().worldCamera.orthographicSize) /* current ortho size */;
        playerInteraction_UI.draggableElement.transform.localScale = draggableItemScale;
        dragging = true;
    }

	public void ContinueDrag(MenuOptions selectedOption)
	{
		playerInteraction_UI.SetDraggableElementPosition(Input.mousePosition);
	}

	public void EndDrag(MenuOptions selectedOption)
	{
		if(interactionPhase != InteractionPhases.ingame_placing) return;
        interactionPhase = InteractionPhases.ingame_default;
		playerInteraction_UI.ReleaseDraggableElement();
		GameManager.Instance.tracker.CreateEventExt("endDrag",selectedOption.ToString());
		if( GameManager.Instance.GetGridManager().IsValidLocation(Input.mousePosition) && !GameManager.Instance.GetGridManager().IsOccupied(Input.mousePosition) ) 
		{ 
			GameManager.Instance.GetGridManager().PlaceGridElementAtLocation( Input.mousePosition, selectedOption );
        }
        dragging = false;
	}

	public void BeginHover(MenuOptions selectedOption)
	{
		GameManager.Instance.tracker.CreateEventExt("beginHover",selectedOption.ToString());
		switch(selectedOption)
		{
			case MenuOptions.trash:
			trashHover = true;
			break;
		}
	}

	public void EndHover(MenuOptions selectedOption)
	{
		GameManager.Instance.tracker.CreateEventExt("endHover",selectedOption.ToString());
		switch(selectedOption)
		{
			case MenuOptions.trash:
			trashHover = false;
			break;
		}
	}

	public void ResetStartValues()
	{
		List<GridObjectBehavior> resetObjects = GameManager.Instance.GetGridManager().GetGridComponentsOfType(new List<string>(){"thread","delivery","pickup","exchange","semaphore","conditional","signal"});
		foreach(GridObjectBehavior resetObject in resetObjects)
		{
            //Debug.Log(resetObject.component.id);
		    resetObject.ResetPosition();
		}

		flowVisibility = false;

        if (connectVisibilityLock) { ToggleConnectionVisibility(); }
    }

    public void TriggerSimulation(LinkJava.SimulationTypes simulationType)
    {

        playerInteraction_UI.revealHintsToggle.toggleRoot.SetActive(false);
        playerInteraction_UI.simulationButton.interactable = false;
        playerInteraction_UI.simulationButton.gameObject.SetActive(false);
        playerInteraction_UI.submitButton.interactable = false;
        playerInteraction_UI.submitButton.gameObject.SetActive(false);

        interactionPhase = InteractionPhases.awaitingSimulation;
        playerInteraction_UI.loadingOverlay.OpenPanel();
		this.playerInteraction_UI.overlayBackground.SetTargetAlpha(1f);
        playerInteraction_UI.loadingText.text = "Simulating...";
        if (tutorialMode)
        {
            GameManager.Instance.PlayTutorialLevel();
        }
        else
        {
            GameManager.Instance.SubmitCurrentLevel(simulationType);
        }
    }

    [ContextMenu("Reset Placed Objects")]
    public void ResetPlacedObjects()
    {
		if (interactionPhase == InteractionPhases.ingame_default || interactionPhase == InteractionPhases.ingame_help) {
			GameManager.Instance.GetGridManager().ClearGrid(false);
		}
    }

    public void TriggerTutorialSimulation()
    {

    }

	public void StartSimulation()
	{
		if(interactionPhase != InteractionPhases.awaitingSimulation) return;
        interactionPhase = InteractionPhases.simulation;
        //Debug.Log("Setting to Simulation.");
		GridObjectBehavior[] gridObjs = GameManager.Instance.GetGridManager().RetrieveComponentsOfType("thread");
		foreach(GridObjectBehavior g in gridObjs) g.GetComponent<SpriteRenderer>().sortingOrder = Constants.ComponentSortingOrder.thread_simulation;

		playerInteraction_UI.goalOverlay.userInput = PlayerInteraction_UI.Goal_UIOverlay.UserInputs.none;
        playbackBehavior.StartPhase();
	}

    public void EndSimulation()
	{
        Debug.Log("EndSimulation");
        playbackBehavior.EndPhase();

        tutorialMode = false;

        interactionPhase = InteractionPhases.ingame_default;

		ResetStartValues();

        playerInteraction_UI.revealHintsToggle.toggleRoot.SetActive(true);
        playerInteraction_UI.playbackControls.gameObject.SetActive(false);
		playerInteraction_UI.simulationButton.interactable = true;
		playerInteraction_UI.simulationButton.gameObject.SetActive(true);
		playerInteraction_UI.submitButton.interactable = true;
        playerInteraction_UI.submitButton.gameObject.SetActive(true);
		playerInteraction_UI.stopSimulationButton.interactable = false;
		playerInteraction_UI.stopSimulationButton.gameObject.SetActive(false);
        playerInteraction_UI.pauseSimulationButton.interactable = false;
        playerInteraction_UI.pauseSimulationButton.gameObject.SetActive(false);
        playerInteraction_UI.playbackSlider.interactable = false;
        playerInteraction_UI.playbackSlider.gameObject.SetActive(false);
        playerInteraction_UI.simulationErrorOverlay.ClosePanel();
    }	

    public enum MouseInput
    {
        LeftMouse,
        RightMouse,
        MiddleMouse,
        None
    }

    public MouseInput mouseInput;

    // Behavior for Player Interaction
	void PlayerInteractionListener()
	{
        // Mouse movement tracking
        lastMousePos = currentMousePos;
        currentMousePos = Input.mousePosition / new Vector2(Screen.width, Screen.height);
        deltaMousePos = currentMousePos - lastMousePos;

        if (Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl) ||
            Input.GetKey(KeyCode.LeftCommand) || Input.GetKey(KeyCode.RightCommand) ||
            Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift)) {
            if (Input.GetKeyDown(KeyCode.Mouse0))
            {
                mouseInput = MouseInput.RightMouse;
            }
            else
            {
                mouseInput = MouseInput.None;
            }
        }
        else if (Input.GetKeyDown(KeyCode.LeftAlt) || Input.GetKeyDown(KeyCode.LeftAlt))
        {
            if (Input.GetKeyDown(KeyCode.Mouse0))
            {
                mouseInput = MouseInput.MiddleMouse;
            }
            else
            {
                mouseInput = MouseInput.None;
            }
        }
        else if (Input.GetKey(KeyCode.Mouse0))
        {
            mouseInput = MouseInput.LeftMouse;
        }
        else if (Input.GetKeyDown(KeyCode.Mouse1))
        {
            mouseInput = MouseInput.RightMouse;
        }
        else if (Input.GetKey(KeyCode.Mouse2))
        {
            mouseInput = MouseInput.MiddleMouse;
        }
        else
        {
            mouseInput = MouseInput.None;
        }

        if (interactionPhase != InteractionPhases.ingame_connecting && interactionPhase != InteractionPhases.ingame_dragging && interactionPhase != InteractionPhases.ingame_placing)
        {
            if (mouseInput == MouseInput.LeftMouse)
            {
                GraphicRaycaster uiRaycast = FindObjectOfType<GraphicRaycaster>();
                PointerEventData uiRaycastData = new PointerEventData(FindObjectOfType<EventSystem>());
                uiRaycastData.position = Input.mousePosition;
                List<RaycastResult> uiResults = new List<RaycastResult>();
                uiRaycast.Raycast(uiRaycastData, uiResults);
                if (uiResults.Count == 0)
                {
                    if (!GameManager.Instance.GetGridManager().IsEditableElement(Input.mousePosition))
                    {
                        if (dragging == false)
                        {
                            UpdatePan();
                        }
                    }
                }
            }
            else if (mouseInput == MouseInput.MiddleMouse)
            {
                ResetZoom();
            }
            else
            {
                float scrollAxis = Input.GetAxis("Mouse ScrollWheel");
                if (scrollAxis != 0)
                    UpdateZoom(scrollAxis * -1); //invert so the scrolling works in the expected direction
            }
        }
        // Interaction Phases
        switch (interactionPhase)
		{
            // Default Phase
			case InteractionPhases.ingame_default:
		
			if (!this.playerInteraction_UI.place_semaphoreButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_semaphoreButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.place_buttonButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_buttonButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.trashButton.button.enabled)
			{ 
				this.playerInteraction_UI.trashButton.ToggleButton(true);
			}
			
			if(playerInteraction_UI.IsSubPanelOpen()) return;
			/*
			 * if player LEFT clicks during basic play, they can 
			 * (1) Click and drag movable elements
			*/
			if (mouseInput == MouseInput.LeftMouse)
			{
				if (GameManager.Instance.GetGridManager().IsEditableElement(Input.mousePosition))
				{
					currentGridObject = GameManager.Instance.GetGridManager().RetrieveEditableGridObject(Input.mousePosition);
					currentGridObject.BeginDrag();
					interactionPhase = InteractionPhases.ingame_dragging;
					GameManager.Instance.tracker.CreateEventExt("BeginReposition", currentGridObject.component.type);

					if (currentGridObject.component.type == "signal" && connectVisibilityLock)
					{
						Signal_GridObjectBehavior s = (Signal_GridObjectBehavior)currentGridObject;
						s.SetHighlight(false);
					}
				}
				else
				{
					if(dragging == false)
					{
						UpdatePan();
					}
				}
				if (hoverObject)
				{
					if (!connectVisibility) hoverObject.EndHoverBehavior();
					hoverObject = null;
				}
			}
			/*
			* if player RIGHT clicks during basic play, they can:
			* (1) link connectable elements through Signals
			* (2) Open/Close Semaphores
		   */
			else if (mouseInput == MouseInput.RightMouse)
			{
				if (GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "signal") && GameManager.Instance.GetGridManager().IsEditableElement( Input.mousePosition ) )
				{
					currentGridObject = GameManager.Instance.GetGridManager().RetrieveGridObjectOfType(Input.mousePosition, "signal");
					currentGridObject.EnableGridObjectEventBehaviors(GridObjectBehavior.InteractTypes.rightClick);
					interactionPhase = InteractionPhases.ingame_connecting;
					currentGridObject.BeginInteraction();

					List<GridObjectBehavior> otherSignals = GameManager.Instance.GetGridManager().GetGridComponentsOfType(new List<string>() { "signal" });
					foreach (GridObjectBehavior otherSignal in otherSignals)
					{
						if (currentGridObject != otherSignal) { otherSignal.GetComponent<SpriteRenderer>().sortingOrder = Constants.ComponentSortingOrder.connectionOverlay - 1; }
					}

					GameManager.Instance.tracker.CreateEventExt("BeginLink", currentGridObject.component.type);

					playerInteraction_UI.onHoverLightbox.OpenPanel();

				}
				else if (GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "semaphore") && GameManager.Instance.GetGridManager().IsEditableElement(Input.mousePosition))
				{
					currentGridObject = GameManager.Instance.GetGridManager().RetrieveGridObjectOfType(Input.mousePosition, "semaphore");
					currentGridObject.EnableGridObjectEventBehaviors(GridObjectBehavior.InteractTypes.rightClick);
					currentGridObject.BeginInteraction();
					GameManager.Instance.tracker.CreateEventExt("BeginLink", currentGridObject.component.type);
				}

				if (hoverObject /*&& !connectVisibilityLock*/)
				{
					hoverObject.EndHoverBehavior();
					hoverObject = null;
				}
			}
			else if (mouseInput == MouseInput.MiddleMouse)
			{
				//ResetZoom();
			}
			/*
			 * if a player isn't clicking the mouse, we should check for hover behaviors AND zoom behaviors
			*/
			else
			{
				if (Input.mousePosition == stationaryMousePosition) //if mouse is stationary
				{
					if (hoverObject == null)
					{
						stationaryTime += Time.deltaTime;
						if (stationaryTime >= 0.2f)
						{
							if (
									GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "signal")
									|| GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "diverter")
									|| GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "exchange")
									|| GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "delivery")
									|| GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "pickup")
									|| GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "conditional")
									|| GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "semaphore")
								)
							{
								hoverObject = GameManager.Instance.GetGridManager().GetGridObjectByMousePosition(Input.mousePosition);
								hoverObject.OnHoverBehavior();
								GameManager.Instance.tracker.CreateEventExt("OnHoverBehavior", hoverObject.component.type);
							}
						}
					}

					//float scrollAxis = Input.GetAxis("Mouse ScrollWheel");
					//if (scrollAxis != 0)
						//UpdateZoom(scrollAxis*-1); //invert so the scrolling works in the expected direction
				}
				else //if mouse has moved since last frame 
				{
					stationaryMousePosition = Input.mousePosition;
					if (hoverObject)
					{
						if (GameManager.Instance.GetGridManager().IsOccupied(Input.mousePosition))
						{
							if (hoverObject != GameManager.Instance.GetGridManager().GetGridObjectByMousePosition(Input.mousePosition))
							{ EndHoverEvent(); }
						}
						else { EndHoverEvent(); }
					}
					else
					{
						stationaryTime = 0f;
					}

					//pop up tooltip close check
					if (playerInteraction_UI.tooltipOverlay.tooltipActive && Time.time - playerInteraction_UI.tooltipOverlay.openTime > 0.5f)
					{ playerInteraction_UI.tooltipOverlay.ClosePanel(); }
				}
				stationaryMousePosition = Input.mousePosition;
				GridObjectBehavior hoverObject__ = GameManager.Instance.GetGridManager().GetGridObjectByMousePosition(Input.mousePosition);
				if (hoverObject__ != null && hoverObject__ != hoverObject_)
				{
					hoverObject_ = hoverObject__;
					if (hoverObject_.component != null)
					{
						GameManager.Instance.tracker.CreateEventExt("OnMouseComponent", hoverObject_.component.type.ToString() + "/" + hoverObject_.component.id.ToString());
					}
				}
				if (hoverObject__ == null && hoverObject_ != null)
				{
					GameManager.Instance.tracker.CreateEventExt("OutMouseComponent", hoverObject_.component.type.ToString() + "/" + hoverObject_.component.id.ToString());
					hoverObject_ = null;
				}

			}
		break;

        // Dragging Phase
		case InteractionPhases.ingame_dragging:
		
			if (!this.playerInteraction_UI.place_semaphoreButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_semaphoreButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.place_buttonButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_buttonButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.trashButton.button.enabled)
			{ 
				this.playerInteraction_UI.trashButton.ToggleButton(true);
			}
			
			if(mouseInput == MouseInput.LeftMouse)
			{
				if( currentGridObject != null )
				{
					currentGridObject.ContinueDrag();
					if(trashHover) { }
					else { }
				}
				else 
				{
                        interactionPhase = InteractionPhases.ingame_default;
				}
			}
			else 
			{
				if(trashHover)
				{
					GameManager.Instance.GetGridManager().ForgetGridElement( currentGridObject );
					if( currentGridObject.component.configuration.link != null && currentGridObject.component.configuration.link > 0 )
					{
					//	GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByID( currentGridObject.component.configuration.link );
					//	g.component.configuration.link = 0;
					}
					GameManager.Instance.tracker.CreateEventExt("Destroying",currentGridObject.component.type);	
					Destroy( currentGridObject.gameObject );
					currentGridObject = null;
                    interactionPhase = InteractionPhases.ingame_default;
				}
				else 
				{
					GameManager.Instance.tracker.CreateEventExt("EndReposition",currentGridObject.component.type);	
					currentGridObject.EndDrag();

                    if (currentGridObject.component.type == "signal" && connectVisibilityLock)
                    {
                        Signal_GridObjectBehavior s = (Signal_GridObjectBehavior)currentGridObject;
                        s.SetHighlight(true);
                    }

                    currentGridObject = null;
                        
					interactionPhase = InteractionPhases.ingame_default;
                }


			}
		break;

		case InteractionPhases.ingame_placing:
			if(mouseInput == MouseInput.LeftMouse)
			{

			}
			break;

        // Connection Phase
		case InteractionPhases.ingame_connecting:
		
			if (!this.playerInteraction_UI.place_semaphoreButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_semaphoreButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.place_buttonButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_buttonButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.trashButton.button.enabled)
			{ 
				this.playerInteraction_UI.trashButton.ToggleButton(true);
			}

			if(mouseInput == MouseInput.RightMouse)
			{
				currentGridObject.EndInteraction();
				if( GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "semaphore") ) 
				{
					GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByMousePosition(Input.mousePosition);
					currentGridObject.LinkTo( g );
					GameManager.Instance.tracker.CreateEventExt("LinkTo",currentGridObject.component.type);
                    currentGridObject.SetHighlight(true);
                }

				else if( GameManager.Instance.GetGridManager().IsObjectOfType(Input.mousePosition, "conditional") ) 
				{
					GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByMousePosition(Input.mousePosition);
					currentGridObject.LinkTo( g );
					GameManager.Instance.tracker.CreateEventExt("LinkTo",currentGridObject.component.type);
                    currentGridObject.SetHighlight(true);
                }

				playerInteraction_UI.onHoverLightbox.ClosePanel();

				List<GridObjectBehavior> otherSignals = GameManager.Instance.GetGridManager().GetGridComponentsOfType(new List<string>(){"signal"});
				foreach(GridObjectBehavior otherSignal in otherSignals)
				{
					if(currentGridObject != otherSignal ) { otherSignal.GetComponent<SpriteRenderer>().sortingOrder = Constants.ComponentSortingOrder.connectionComponents;}
					if(connectVisibilityLock) otherSignal.SetHighlight( true );
				}

				interactionPhase = InteractionPhases.ingame_default;
			}
			else 
			{
				currentGridObject.ContinueInteraction();
			}
		break;

        // Help Phase
        case InteractionPhases.ingame_help:
		
			if (!this.playerInteraction_UI.place_semaphoreButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_semaphoreButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.place_buttonButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_buttonButton.ToggleButton(true);
			}
			if (!this.playerInteraction_UI.trashButton.button.enabled)
			{ 
				this.playerInteraction_UI.trashButton.ToggleButton(true);
			}
		
			// On Left Click
			if (mouseInput == MouseInput.LeftMouse)
			{
				Button current_button = null;
				Image current_image = null;
				GridObjectBehavior current_object = GameManager.Instance.GetGridManager().GetGridObjectByMousePosition(Input.mousePosition);

				//raycasting to find buttons for glossary
				GraphicRaycaster uiRaycast = FindObjectOfType<GraphicRaycaster>();
				PointerEventData uiRaycastData = new PointerEventData(FindObjectOfType<EventSystem>());
				uiRaycastData.position = Input.mousePosition;
				List<RaycastResult> uiResults = new List<RaycastResult>();
				uiRaycast.Raycast(uiRaycastData, uiResults);

				foreach (RaycastResult r in uiResults)
				{
					if (r.gameObject.GetComponent<Button>() != null)
					{
						current_button = r.gameObject.GetComponent<Button>();
						break;
					}
					if (r.gameObject.GetComponent<Image>() != null)
					{
						current_image = r.gameObject.GetComponent<Image>();
						break;
					}
				}

				if (current_button)
				{
					string obj_name = current_button.name;
					TriggerHint(obj_name);
				}
				else if (current_image)
				{
					string obj_name = current_image.name;
					TriggerHint(obj_name);
				}
				else if (current_object)
				{
					string obj_name = current_object.component.type;
					if (obj_name == "delivery" || obj_name == "pickup")
						obj_name = current_object.name;
					TriggerHint(obj_name);
				}
			}
			break;
				
		// Awaiting Simulation Phase
		case InteractionPhases.awaitingSimulation:
			if (this.playerInteraction_UI.place_semaphoreButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_semaphoreButton.ToggleButton(false);
			}
			if (this.playerInteraction_UI.place_buttonButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_buttonButton.ToggleButton(false);
			}
			if (this.playerInteraction_UI.trashButton.button.enabled)
			{ 
				this.playerInteraction_UI.trashButton.ToggleButton(false);
			}
			break;
			
		case InteractionPhases.simulation:
			if (this.playerInteraction_UI.place_semaphoreButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_semaphoreButton.ToggleButton(false);
			}
			if (this.playerInteraction_UI.place_buttonButton.button.enabled)
			{ 
				this.playerInteraction_UI.place_buttonButton.ToggleButton(false);
			}
			if (this.playerInteraction_UI.trashButton.button.enabled)
			{ 
				this.playerInteraction_UI.trashButton.ToggleButton(false);
			}
			break;			
		}

	}

	public void ToggleFlowVisibility()
	{
		flowVisibility = !flowVisibility;
		GameManager.Instance.tracker.CreateEventExt("ToggleFlowVisibility",flowVisibility.ToString());

		GridObjectBehavior[] tracks = GameManager.Instance.GetGridManager().RetrieveTracks();//.RetrieveComponentsOfType("intersection");
		foreach(GridObjectBehavior track in tracks)
		{
			if(flowVisibility) { track.BeginInteraction(); }
			else { track.EndInteraction(); }
		}
	}

	public void ToggleFlowVisibility(bool setVisibility)
	{
		if(setVisibility == flowVisibility) return;
		if (setVisibility == false && /*connectVisibilityLock*/colorFlowVisibilityLock) return;
		flowVisibility = setVisibility;
		GameManager.Instance.tracker.CreateEventExt("ToggleFlowVisibility",flowVisibility.ToString());

		GridObjectBehavior[] tracks = GameManager.Instance.GetGridManager().RetrieveTracks();//.RetrieveComponentsOfType("intersection");
		foreach(GridObjectBehavior track in tracks)
		{
			if(flowVisibility) { track.BeginInteraction(); }
			else { track.EndInteraction(); }
		}
	}

    public void LockFlowVisibility(int lockTarget)
    {
    	GameManager.Instance.tracker.CreateEventExt("LockFlowVisibility",lockTarget.ToString());
        if (lockTarget == -1) //force quit
        {
            playerInteraction_UI.rightPanelColorLock.gameObject.SetActive(false);
			colorFlowVisibilityLock = false;
            return;
        }
		colorFlowVisibilityLock = !colorFlowVisibilityLock;
		if (/*connectVisibilityLock*/colorFlowVisibilityLock)
        {
            playerInteraction_UI.rightPanelColorLock.rectTransform.position = playerInteraction_UI.rightPanelColors[lockTarget].transform.position;
            playerInteraction_UI.rightPanelColorLock.gameObject.SetActive(true);
        }
        else
        {
            playerInteraction_UI.rightPanelColorLock.gameObject.SetActive(false);
            GameManager.Instance.GetGridManager().RevealGridColorMask(lockTarget);
        }
    }

    public void ToggleConnectionVisibility(float duration = -1.0f)
	{
		connectVisibility = !connectVisibility;

		if(connectVisibilityLock && !connectVisibility) return;

		GameManager.Instance.tracker.CreateEventExt("ToggleConnectionVisibility",connectVisibility.ToString());

		GridObjectBehavior[] gridObjects = GameManager.Instance.GetGridManager().RetrieveComponentsOfType("signal");
		foreach(GridObjectBehavior g in gridObjects)
		{
			Signal_GridObjectBehavior s = (Signal_GridObjectBehavior) g;
			s.SetHighlight(connectVisibility);
		}
        if(duration > 0)
        {
            StartCoroutine(ToggleConnectionVisibilityRoutine(duration));
        }
	}

    IEnumerator ToggleConnectionVisibilityRoutine(float duration)
    {
        yield return new WaitForSeconds(duration);
        ToggleConnectionVisibility();
    }

    public void LockConnectionVisibility()
	{
		connectVisibilityLock = !connectVisibilityLock;
		GameManager.Instance.tracker.CreateEventExt("LockConnectionVisibility",connectVisibilityLock.ToString());
		playerInteraction_UI.topPanelConnectionLock.enabled =  connectVisibilityLock;
	}

	public void ToggleExitMenu()
	{
		if(playerInteraction_UI.pauseOverlay.isPaused)
		{
            playerInteraction_UI.pauseOverlay.ClosePanel();	
			GameManager.Instance.tracker.CreateEventExt("ClosePausePanel","");
		}
		else 
		{
            playerInteraction_UI.pauseOverlay.OpenPanel();
			GameManager.Instance.tracker.CreateEventExt("OpenPausePanel","");
		}
	}

    public void ToggleHintsVisibility()
	{
        // If the game is in the help phase
        if (interactionPhase == InteractionPhases.ingame_help)
        {
            // Turn it off
            interactionPhase = InteractionPhases.ingame_default;
            GameManager.Instance.tracker.CreateEventExt("ToggleHintsVisibility", (false).ToString());
            playerInteraction_UI.revealHintsToggle.SetToggle(true);
            playerInteraction_UI.simulationButton.interactable = true;
            playerInteraction_UI.submitButton.interactable = true;
            playerInteraction_UI.trash.enabled = true;
            playerInteraction_UI.preview.enabled = true;
            playerInteraction_UI.place_semaphore.enabled = true;
            playerInteraction_UI.place_button.enabled = true;
            foreach (EventTrigger e in playerInteraction_UI.rightPanelColors)
                e.enabled = true;
        }
        else
        {
            // Else, turn it on
            interactionPhase = InteractionPhases.ingame_help;
            GameManager.Instance.tracker.CreateEventExt("ToggleHintsVisibility", (true).ToString());
            playerInteraction_UI.revealHintsToggle.SetToggle(false);
            playerInteraction_UI.simulationButton.interactable = false;
            playerInteraction_UI.submitButton.interactable = false;
            playerInteraction_UI.trash.enabled = false;
            playerInteraction_UI.preview.enabled = false;
            playerInteraction_UI.place_semaphore.enabled = false;
            playerInteraction_UI.place_button.enabled = false;
            foreach(EventTrigger e in playerInteraction_UI.rightPanelColors)
                e.enabled = false;
        }
        TriggerHintFader();

        // Old Hint System
		//GameManager.Instance.tracker.CreateEventExt("ToggleHintsVisibility",(!playerInteraction_UI.UIOverlay_Hint_Container.gameObject.activeSelf).ToString());
		//if(playerInteraction_UI.UIOverlay_Hint_Container.gameObject.activeSelf) { playerInteraction_UI.hintOverlay.ClosePanel(); }
		//playerInteraction_UI.UIOverlay_Hint_Container.gameObject.SetActive( !playerInteraction_UI.UIOverlay_Hint_Container.gameObject.activeSelf );

	}

    void TriggerHintFader()
    {
        bool fadeNonInteractables = (interactionPhase == InteractionPhases.ingame_help);
        GridObjectBehavior[] gridObjects = GameManager.Instance.GetGridManager().RetrieveComponentsOfType();
        foreach (GridObjectBehavior g in gridObjects)
        {
            bool success = false;
            HintConstructor h = GameManager.Instance.hintGlossary.GetHintForComponent(g.component.type, out success);
            if (success == false)
            {
                SpriteRenderer s = g.GetComponent<SpriteRenderer>();
                s.color = new Color(s.color.r, s.color.g, s.color.b, fadeNonInteractables ? 0.5f : 1f);
            } 
            else
            {
                g.SetHighlight(fadeNonInteractables);
            }
        }

        gridObjects = GameManager.Instance.GetGridManager().RetrieveTracks();
        foreach (GridObjectBehavior g in gridObjects)
        {
            SpriteRenderer s = g.GetComponent<SpriteRenderer>();
            s.color = new Color(s.color.r, s.color.g, s.color.b, fadeNonInteractables ? 0.5f : 1f);
        }

        Image backgroundImage = playerInteraction_UI.UICameraContainer.GetComponentInChildren<Image>();
        backgroundImage.color = new Color(backgroundImage.color.r, backgroundImage.color.g, backgroundImage.color.b, fadeNonInteractables ? 0.5f : 1f);
    }

    void EndHoverEvent()
    {
        if ( connectVisibilityLock || hoverObject==null ) return;
        stationaryTime = 0f;
        hoverObject.EndHoverBehavior();
        GameManager.Instance.tracker.CreateEventExt("EndHoverBehavior", hoverObject.component.type);
        hoverObject = null;
    }

	public void TriggerPlayPhaseEnd( GameManager.GamePhases nextPhase  = GameManager.GamePhases.LoadScreen, bool autoOpenNextLevel = false )
	{
		if(interactionPhase == InteractionPhases.simulation) 
		{
			StopAllCoroutines();
			EndSimulation();
		}
        score.attemptCount = 0;
        tutorialMode = false;
		GameManager.Instance.SetGamePhase( nextPhase );
        if (autoOpenNextLevel)
        {
            GameManager.Instance.TriggerAdvanceToNextLevel();
        }
	}

    public void UpdateZoom(float zoom)
    {
        if (zoom == 0)
            return;
        else
        {
            if (zoomLevel < 0.01) //round small enough values off to 0
                zoomLevel = 0;
            if ((zoomLevel != 0f || zoom > 0) && (zoomLevel != 1f || zoom < 0))
            {
                float newZoom = zoomLevel + zoom;
                newZoom = Mathf.Clamp(newZoom, 0f, 1f);
                Camera orthoCam = GameManager.Instance.GetGridManager().worldCamera;
                float targetOrtho = ((maxOrtho - minOrtho) * newZoom) + minOrtho;

                Vector3 newTargetPosition = GameManager.Instance.GetGridManager().worldCamera.ScreenToWorldPoint(Input.mousePosition);
                newTargetPosition = new Vector3(Mathf.Clamp(newTargetPosition.x, 0 + (xMax / 2 * zoomLevel), xMax - (xMax / 2 * zoomLevel)),
                                                Mathf.Clamp(newTargetPosition.y, 0 + (yMax / 2 * zoomLevel), yMax - (yMax / 2 * zoomLevel)),
                                                orthoCam.transform.position.z);

                zoomLevel = newZoom;
                orthoCam.orthographicSize = targetOrtho;
                orthoCam.transform.position = newTargetPosition;
                playerInteraction_UI.zoomMeter.SetMeterValue(zoomLevel);
            }
        }
    }

    void UpdatePan()
    {
        Camera orthoCam = GameManager.Instance.GetGridManager().worldCamera;
        orthoCam.transform.Translate(-deltaMousePos.x * (xMax - xMin), -deltaMousePos.y * (yMax - yMin), 0);
        orthoCam.transform.position = new Vector3(Mathf.Clamp(orthoCam.transform.position.x, 0 + (xMax / 2 * zoomLevel), xMax - (xMax / 2 * zoomLevel)),
                                                    Mathf.Clamp(orthoCam.transform.position.y, 0 + (yMax / 2 * zoomLevel), yMax - (yMax / 2 * zoomLevel)),
                                                    orthoCam.transform.position.z);
    }

    void ResetZoom()
    {
        zoomLevel = 0f;
        currentCameraPosition = originalCameraPosition;
        GameManager.Instance.GetGridManager().worldCamera.orthographicSize = originalOrthographicSize;
        GameManager.Instance.GetGridManager().worldCamera.transform.position = originalCameraPosition;
		playerInteraction_UI.zoomMeter.SetMeterValue( 1f );
    }

    void PauseSimulation()
    {
        playbackBehavior.PauseSimulation();
    }

    void UnpauseSimulation()
    {
        playbackBehavior.UnpauseSimulation();
    }

    void DelayedUnpauseSimulation(float delay = 0, TutorialEvent t = null)
    {
        playbackBehavior.DelayedUnpause(delay, t);
    }
}


