using UnityEngine;
using System.Collections;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using ParallelProg.UI;

#region UI Layout / UI Event Setup
[System.Serializable]
public class PlayerInteraction_UI
{
	[Header("UI Containers")]
	public RectTransform UIOverlayContainer;
	public RectTransform UICameraContainer;
	//public RectTransform UIOverlay_Pause_Container;
	[SerializeField] public Pause_UIOverlay pauseOverlay;
	//public RectTransform UIOverlay_Goal_Container;
	[SerializeField] public Goal_UIOverlay goalOverlay;
	public RectTransform UIOverlay_Hint_Container;
	public Transform hint_button_container;
	[SerializeField] public Hint_UIOverlay hintOverlay;
	[SerializeField] public Tooltip_UIOverlay tooltipOverlay;
    [SerializeField] public Lightbox_UIOverlay onHoverLightbox;
    [SerializeField] public UIOverlay loadingOverlay;
	[SerializeField] public UIOverlay simulationErrorOverlay;

	[Header("Banner Event Triggers")]
	public EventTrigger place_semaphore;
	public EventTrigger place_button;
	public EventTrigger trash;
	public EventTrigger preview;
	//public EventTrigger exit;
	public Button simulationButton;
	public Button stopSimulationButton;
    public Button pauseSimulationButton;
	public Button submitButton;
    [SerializeField] public ToggleUIElement revealHintsToggle;
	public Button exitButton;
	public EventTrigger[] rightPanelColors;
	public HintButton[] hintButtons;
	public TooltipEvent[] tooltipEvents;
    public Slider playbackSlider;

    [Header("Goal Description Elements")]
    public GoalDescription_UIOverlay goalDescriptionOverlay;
    public Text goalDescriptionText;
    public Text goalDescription_Number;
    public Text goalDescription_Title;

    [Header("Updatable Elements")]
    public Text levelNameText;
    public Text loadingText;
    public Text levelTimer;
	public Image draggableElement;
	public Text text_hintPopUp;
	public Image image_hintPopUp;
	public Text levelText;
    public Image rightPanelColorLock;
	public Image topPanelConnectionLock;
	public UIMeter zoomMeter;
    public Image playbackButton;
    public Sprite[] playbackButtonSprites;

    [Header("UI Containers")]
    public GameObject playbackControls;

	[Header("Prefabs")]
	public GameObject hint_button_prefab;

	public void OpenUI()
	{
		draggableElement.gameObject.SetActive(false);
		UICameraContainer.gameObject.SetActive(true);
		UIOverlayContainer.gameObject.SetActive(true);
		pauseOverlay.ClosePanel(true);
		goalOverlay.ClosePanel(true);
		hintOverlay.ClosePanel(true);
		tooltipOverlay.ClosePanel(true);
        if (GameManager.Instance.GetDataManager().currentLevelData.metadata.pcg_id != null && GameManager.Instance.GetDataManager().currentLevelData.metadata.pcg_id != "N/A")
            levelText.text = GameManager.Instance.GetDataManager().currentLevelData.metadata.pcg_id.Substring(GameManager.Instance.GetDataManager().currentLevelData.metadata.pcg_id.Length - 3);
        else
            levelText.text = GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id.ToString();
        if(levelText.text.Length == 1)
        {
            levelText.text = 0 + levelText.text;
        }
        levelTimer.gameObject.SetActive(false);
		zoomMeter.OpenMeter();
	}

	public void CloseUI()
	{
		draggableElement.gameObject.SetActive(false);
		UICameraContainer.gameObject.SetActive(false);
		UIOverlayContainer.gameObject.SetActive(false);
		pauseOverlay.ClosePanel(true);
		goalOverlay.ClosePanel(true);
		hintOverlay.ClosePanel(true);
		tooltipOverlay.ClosePanel(true);
        simulationErrorOverlay.ClosePanel(true);
		UIOverlay_Hint_Container.gameObject.SetActive(false);
		zoomMeter.CloseMeter();
	}

    public float startTime;
    public void Timer()
    {
        float rawTime = Time.fixedTime - startTime;
        float milliseconds = (Time.fixedTime % 1);
        rawTime -= milliseconds;
        int hours = (int)rawTime / 3600;
        int minutes = ((int)rawTime % 3600) / 60;
        int seconds = (int)rawTime % 60;
        string sMinutes = minutes.ToString();
        while (sMinutes.Length < 2) sMinutes = "0" + sMinutes;
        string sSeconds = seconds.ToString();
        while (sSeconds.Length < 2) sSeconds = "0" + sSeconds;
        string sMilliseconds = ((int)(milliseconds * 1000)).ToString();
        while (sMilliseconds.Length < 3) sMilliseconds = "0" + sMilliseconds;
        levelTimer.text = hours.ToString() + ":" + sMinutes + ":" + sSeconds;// + ":" + sMilliseconds;
    }

	public void ClearButtonBehaviors()
	{
		place_semaphore.triggers.Clear();
		place_button.triggers.Clear();
		trash.triggers.Clear();
		preview.triggers.Clear();
		//exit.triggers.Clear();
	}

	public void SetDraggableElement ( Sprite inputTexture )
	{
		draggableElement.sprite = inputTexture;
		draggableElement.gameObject.SetActive(true);
	}

	public void SetDraggableElementPosition(Vector2 inputPosition)
	{
		draggableElement.transform.position = Vector3.Lerp(draggableElement.transform.position, inputPosition, 1f);
	}

	public void ReleaseDraggableElement ()
	{
		draggableElement.gameObject.SetActive(false);
	}

	public void SetText(Level inputLevel)
	{
        if (inputLevel.metadata.pcg_id != "N/A")
        {
            levelNameText.text = inputLevel.metadata.pcg_id.Substring(inputLevel.metadata.pcg_id.Length - 3);
            goalDescription_Number.text = inputLevel.metadata.pcg_id.Substring(inputLevel.metadata.pcg_id.Length - 3);
            goalDescription_Title.text = inputLevel.metadata.pcg_id.ToUpper();
        }
        else{
            levelNameText.text = inputLevel.metadata.level_title;
            goalDescription_Number.text = inputLevel.metadata.level_id.ToString();
            goalDescription_Title.text = inputLevel.metadata.level_title.ToString().ToUpper();
        }
		goalDescriptionText.text = inputLevel.metadata.goal_string;
        goalDescriptionOverlay.SetFeedbackScore(GameManager.Instance.GetScoreManager().GetCalculatedScore(inputLevel.metadata.level_id));
    }

	public IEnumerator TriggerGoalPopUp(string titleText, string feedbackText)
	{
        feedbackText = feedbackText.Replace("\n•", "");
        feedbackText = feedbackText.Replace("\n", "");
        GameManager.Instance.tracker.CreateEventExt("TriggerGoalPopUp", titleText + (feedbackText.TrimStart(new char[2] { '/', 'n' })));
		goalOverlay.SetText( titleText, feedbackText );
		goalOverlay.OpenPanel();
		while( goalOverlay.waitForUserInput ) { yield return new WaitForEndOfFrame(); }
		//goalOverlay.ClosePanel();
	}

	public bool IsSubPanelOpen()
	{
		return (pauseOverlay.isOpen || goalOverlay.isOpen);
	}

    [System.Serializable]
    public class GoalDescription_UIOverlay : UIOverlay
    {
        public Image star_1, star_2, star_3;
        public Sprite star_empty, star_fill;
        public void SetFeedbackScore(int inScore)
        {
            star_1.sprite = inScore > 0 ? star_fill : star_empty;
            star_2.sprite = inScore > 1 ? star_fill : star_empty;
            star_3.sprite = inScore > 2 ? star_fill : star_empty;
        }
    }

	[System.Serializable]
	public class Goal_UIOverlay : UIOverlay
	{
        public Text titleText;
		public Text feedbackText;
        public Button retry, replay, levels, levelsConfirm, levelsDeny, exit, exitConfirm, exitDeny, levelsNext, saveLevel;
        public ToggleUIElement goalToggle;

        public GameObject starContainer;
        public Image star_1, star_2, star_3;
        public Sprite star_empty, star_fill;

        [SerializeField] public UIOverlay rootOverlay, confirmExitOverlay, confirmLevelsOverlay;

        public bool waitForUserInput = false;
		public enum UserInputs {none, retry, replay, levels, exit, levelsNext, stop}
		public UserInputs userInput = UserInputs.none;

        bool visibilityToggle = true;
        bool[] visibilitySettings = new bool[] { false, false, false };

		public override void OpenPanel()
		{
			waitForUserInput = true;
			base.OpenPanel();
            rootOverlay.OpenPanel();
            confirmExitOverlay.ClosePanel(true);
            confirmLevelsOverlay.ClosePanel(true);
			EnableButtonBehaviors();
		}

		public override void ClosePanel(bool forceClose = false) 
		{
			waitForUserInput = false;
			base.ClosePanel(forceClose);
		}

        public void OpenRootScreen()
        {
            rootOverlay.OpenPanel();
            confirmExitOverlay.ClosePanel();
            confirmLevelsOverlay.ClosePanel();
        }

        public void OpenExitConfirmationScreen()
        {
            rootOverlay.ClosePanel();
            confirmExitOverlay.OpenPanel();
            confirmLevelsOverlay.ClosePanel();
        }
        public void OpenLevelsConfirmationScreen()
        {
            rootOverlay.ClosePanel();
            confirmExitOverlay.ClosePanel();
            confirmLevelsOverlay.OpenPanel();
        }

		public void SetText(string titleText, string feedbackText)
		{
            this.titleText.text = titleText;
			this.feedbackText.text = feedbackText;
		}

        public void SetFeedbackScore(int inScore)
        {
            if (inScore < 0) { starContainer.SetActive(false); }
            else
            {
                starContainer.SetActive(true);
                star_1.sprite = inScore > 0 ? star_fill : star_empty;
                star_2.sprite = inScore > 1 ? star_fill : star_empty;
                star_3.sprite = inScore > 2 ? star_fill : star_empty;
            }
        }

		public void EnableButtonBehaviors()
		{
			retry.onClick.RemoveAllListeners();
			replay.onClick.RemoveAllListeners();

            levels.onClick.RemoveAllListeners();
            levelsConfirm.onClick.RemoveAllListeners();
            levelsDeny.onClick.RemoveAllListeners();
            levelsNext.onClick.RemoveAllListeners();

            PlayerInteraction_GamePhaseBehavior playerInteraction = GameManager.Instance.playerInteractionBehavior as PlayerInteraction_GamePhaseBehavior;
            bool showNextLevelButton = (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.ME && 
                                        playerInteraction.playbackBehavior.success && 
                                        GameManager.Instance.currentLevelReferenceObject.completionRank > 0 &&
                                        !GameManager.Instance.IsInPCG());
            levelsNext.gameObject.SetActive(showNextLevelButton);

            exit.onClick.RemoveAllListeners();
            exitConfirm.onClick.RemoveAllListeners();
            exitDeny.onClick.RemoveAllListeners();

			retry.onClick.AddListener(()=> {userInput = UserInputs.retry; ClosePanel();} );
			replay.onClick.AddListener(()=> {userInput = UserInputs.replay; ClosePanel();} );
			levels.onClick.AddListener(()=> {userInput = UserInputs.levels; OpenLevelsConfirmationScreen(); } );
            levelsConfirm.onClick.AddListener(() => { ClosePanel(); } );
            levelsDeny.onClick.AddListener(() => OpenRootScreen());

            levelsNext.onClick.AddListener(() => { userInput = UserInputs.levelsNext; /*GameManager.Instance.TriggerAdvanceToNextLevel();*/ ClosePanel(); });
            //levelsNext.gameObject.SetActive(!GameManager.Instance.IsInPCG());

            exit.onClick.AddListener(()=> {userInput = UserInputs.exit; OpenExitConfirmationScreen(); } );
            exitConfirm.onClick.AddListener(() => { GameManager.Instance.SetGamePhase(GameManager.GamePhases.CloseGame);/* /*ClosePanel();*/ });
            exitDeny.onClick.AddListener(() => OpenRootScreen());

            goalToggle.toggleButton.onClick.RemoveAllListeners();
            goalToggle.toggleButton.onClick.AddListener(() => ToggleGoalVisibility());

            saveLevel.onClick.RemoveAllListeners();
            saveLevel.onClick.AddListener(() => GameManager.Instance.TriggerPCGLevelSave() );
            saveLevel.gameObject.SetActive(false);
            //saveLevel.gameObject.SetActive( GameManager.Instance.IsInPCG() );
            //just for november 2018 study
        }

        void ToggleGoalVisibility()
        {
            visibilityToggle = !visibilityToggle;
            goalToggle.SetToggle(visibilityToggle);
            if (visibilityToggle == false)
            {
                visibilitySettings[0] = rootOverlay.isOpen;
                visibilitySettings[1] = confirmLevelsOverlay.isOpen;
                visibilitySettings[2] = confirmLevelsOverlay.isOpen;
            }
            rootOverlay.panelContainer.gameObject.SetActive(visibilityToggle && visibilitySettings[0]);
            confirmLevelsOverlay.panelContainer.gameObject.SetActive(visibilityToggle && visibilitySettings[1]);
            confirmLevelsOverlay.panelContainer.gameObject.SetActive(visibilityToggle && visibilitySettings[2]);
            Image overlayImg = panelContainer.GetComponent<Image>();
            overlayImg.color = new Color(overlayImg.color.r, overlayImg.color.g, overlayImg.color.b, (visibilityToggle?0.75f:0.5f));
        }
	}

	[System.Serializable]
	public class Hint_UIOverlay : UIOverlay
	{
		public Text descriptionTitle;
		public Text descriptionText;
		public Image descriptionImage;

		public Button exit;

		public override void OpenPanel()
		{
			base.OpenPanel();
			EnableButtonBehaviors();
		}

		public override void ClosePanel(bool forceClose = false) 
		{
			base.ClosePanel(forceClose);
		}

		public void SetHint(string inTitle, string inDescription, Sprite inImage)
		{
			descriptionTitle.text = inTitle;
			descriptionText.text = inDescription;
			descriptionImage.sprite = inImage;
		}

		public void EnableButtonBehaviors()
		{
			exit.onClick.RemoveAllListeners();
			exit.onClick.AddListener( ()=> ClosePanel() );
		}
	}

	[System.Serializable]
	public class Pause_UIOverlay : UIOverlay
	{
		public Button resume, exit, exitConfirmed, exitDenied;
		public bool isPaused = false;

        [SerializeField] public UIOverlay rootMenu, exitConfirmationMenu;

		public override void OpenPanel()
		{
			isPaused = true;
			base.OpenPanel();
			EnableButtonBehaviors();

            rootMenu.OpenPanel();
            exitConfirmationMenu.ClosePanel(true);
		}

		public override void ClosePanel(bool forceClose = false)
		{
			isPaused = false;
			base.ClosePanel(forceClose);
		}
		public void EnableButtonBehaviors()
		{
			resume.onClick.RemoveAllListeners();
			resume.onClick.AddListener( ()=> ClosePanel() );

            exit.onClick.RemoveAllListeners();
            exit.onClick.AddListener(() => { OpenConfirmationScreen(); });

            exitDenied.onClick.RemoveAllListeners();
            exitDenied.onClick.AddListener(() => CloseConfirmationScreen());

            exitConfirmed.onClick.RemoveAllListeners();
            exitConfirmed.onClick.AddListener( () => { OnExitConfrimed(); } );
			//exit uses a PlayerInteraction_GamePhaseBehavior method so I define it there
		}

        void OpenConfirmationScreen()
        {
            rootMenu.ClosePanel();
            exitConfirmationMenu.OpenPanel();
        }

        void CloseConfirmationScreen()
        {
            rootMenu.OpenPanel();
            exitConfirmationMenu.ClosePanel();
        }

        void OnExitConfrimed()
        {
            PlayerInteraction_GamePhaseBehavior playPhase = (PlayerInteraction_GamePhaseBehavior)GameManager.Instance.playerInteractionBehavior;
            playPhase.TriggerPlayPhaseEnd();
        }
	}

	[System.Serializable]
	public class Tooltip_UIOverlay : UIOverlay 
	{
        [SerializeField]
        Canvas canvas;
        [SerializeField]
        CanvasScaler scaler;

        public Text tooltipText;
		public bool tooltipActive = false;
		public float openTime = 0f;

		public override void OpenPanel()
		{
			base.OpenPanel();
			tooltipActive = true;
			openTime = Time.time;
		}

		public override void ClosePanel(bool forceClose = false) 
		{
			base.ClosePanel(forceClose);
			tooltipActive = false;
		}

        public void SetTooltip(string inDescription, GameObject element)
        {
            RectTransform elementRect = element.GetComponent<RectTransform>();
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

            float eXMin = elementRect.position.x + elementRect.rect.xMin;
            float eXMax = elementRect.position.x + elementRect.rect.xMax;
            float eYMin = elementRect.position.y + elementRect.rect.yMin;
            float eYMax = elementRect.position.y + elementRect.rect.yMax;
            float eWidth = elementRect.rect.width * multiplier;
            float eHeight = elementRect.rect.width * multiplier;

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

            panelContainer.position = new Vector3(posX, posY, panelContainer.position.z);
            tooltipText.text = inDescription;
        }
    }

    [System.Serializable]
    public class Lightbox_UIOverlay : UIOverlay
    {
        public Transform lightboxElement;
        public override void OpenPanel()
        {
            isOpen = true;
            lightboxElement.gameObject.transform.localScale = Vector3.zero;

            lightboxElement.gameObject.SetActive(true); SpriteRenderer sr = lightboxElement.GetComponent<SpriteRenderer>();
            float width = sr.sprite.bounds.size.x;
            float height = sr.sprite.bounds.size.y;

            GameObject worldCamera = GameObject.Find("UICamera");
            if (worldCamera == null) worldCamera = Camera.main.gameObject;
            double worldScreenHeight = worldCamera.GetComponent<Camera>().orthographicSize * 2.0;
            double worldScreenWidth = worldScreenHeight / Screen.height * Screen.width;
            Vector3 targetScale = new Vector3((float)worldScreenWidth / width, (float)worldScreenHeight / height, 1f);

            iTween.ScaleTo(lightboxElement.gameObject, iTween.Hash("scale", targetScale, "time", 0.5f));
        }

        public override void ClosePanel(bool forceClose = false)
        {
            isOpen = false;
            if (forceClose)
            {
                lightboxElement.gameObject.SetActive(false);
            }
            else
            {
                iTween.ScaleTo(lightboxElement.gameObject, iTween.Hash("scale", Vector3.zero, "time", 0.5f));
            }
        }
    }

    [System.Serializable]
    public class ToggleUIElement
    {
        public GameObject toggleRoot;
        public RectTransform container;
        public RectTransform toggle;
        public Text leftText, rightText;
        public Button toggleButton;
        public void SetToggle(bool isA)
        {
            if (isA)
            {
                toggle.pivot = new Vector2(1, 0.5f);
                toggle.anchoredPosition = new Vector3(0, 0, 0);
                leftText.color = new Color(1f, 1f, 1f);
                rightText.color = new Color(88f / 255f, 89f / 255f, 97f / 255f);
            }
            else
            {
                toggle.pivot = new Vector2(0, 0.5f);
                toggle.anchoredPosition = new Vector3(0, 0, 0);
                rightText.color = new Color(1f, 1f, 1f);
                leftText.color = new Color(88f / 255f, 89f / 255f, 97f / 255f);
            }
        }
    }
}

public class GoalDescription_UIOverlay : UIOverlay
{
 //TODO: make goal description popup controls HERE instead of at top level   
}
#endregion