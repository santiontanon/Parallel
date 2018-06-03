using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using UnityEngine.Events;

[System.Serializable]
public class TutorialEvent
{
    public int levelNumber;
    public enum TutorialTypes { popup, video, image, simulation }
    public TutorialTypes type;
    public enum TutorialInitializeTriggers { beforePlay, duringSimulation }
    public TutorialInitializeTriggers init_trigger;
    public enum TutorialCompletionTriggers { placeSignal, placeSemaphore, clickButton, simulationInteraction, clickPopup }
    public TutorialCompletionTriggers complete_trigger;

    public string popupDescription;

    [Header("Simulation Pausing")]
    [Tooltip("Should this event pause the simulation?")]
    public bool pause;
    [Tooltip("Leave at zero for an indefinite pause which only ends when the popup is clicked")]
    public float pauseDuration;

    [Header("Tutorial Simulation")]
    [Tooltip("Should the simulation for testing usea predetermined sequence?")]
    public bool overrideSimulation;
    [Tooltip("Filepath for the txt file to run the tutorial sim from")]
    public string filepath;

    public bool hasCompleted = false;

    [Header("Tutorial completes when: (complete_trigger)")]
    [Space(5)]
    
    [Header("a button is pressed (clickButton)...")]
    public Button targetButton;

    [Header("a component is interacted with (componentType)...")]
    public string targetComponentType = "";

    [Header("a simulation event occurs (simulationInteraction) ...")]
    [SerializeField] public StepData targetSimulationStep;


    UnityAction callReference;

    public void ActivateTutorialEventListener()
    {
        switch (type)
        {
            case TutorialTypes.image:
            case TutorialTypes.video:
                Debug.Log("Tutorial type " + type.ToString() + " not currently supported.");
                break;
            case TutorialTypes.popup:
                if (complete_trigger == TutorialCompletionTriggers.clickButton)
                {
                    Debug.Log("Click Button");
                    if (targetButton == null) return;
                    callReference = () => { TriggerTutorialEventListener(); };
                    targetButton.onClick.AddListener(callReference);
                }
                else if (complete_trigger == TutorialCompletionTriggers.clickPopup)
                {
                    GameManager.Instance.SetTutorialPopupClickToClose(this, true);
                }
                else if (complete_trigger == TutorialCompletionTriggers.placeSemaphore)
                {
                    PlayerInteraction_GamePhaseBehavior.onMenuInteraction += MenuInteractionListener;
                }
                else if (complete_trigger == TutorialCompletionTriggers.placeSignal)
                {
                    Debug.Log("Place Signal tutorial.");
                    PlayerInteraction_GamePhaseBehavior.onMenuInteraction += MenuInteractionListener;
                    //Debug.Log("Completion event for tutorial " + complete_trigger.ToString() + " not currently supported.");
                }
                else if (complete_trigger == TutorialCompletionTriggers.simulationInteraction)
                {
                    Debug.Log("Completion event for tutorial " + complete_trigger.ToString() + " not currently supported.");
                    // create listener to judge against targetSimulationStep
                    PlayerInteraction_GamePhaseBehavior.onSimulationStep += StepListener;
                }
                break;
            case TutorialTypes.simulation:
                PlayerInteraction_GamePhaseBehavior.onCompletion = CompletionListener;
                break;
        }
    }
    public void TriggerTutorialEventListener()
    {
        Debug.Log("TRIGGER TUTORIAL EVENT LISTERNER");
        TerminateTutorialEventListener();
        GameManager.Instance.ReportTutorialEventComplete(this);
    }

    void TerminateTutorialEventListener()
    {
        switch (type)
        {
            case TutorialTypes.image:
            case TutorialTypes.video:
                Debug.Log("Tutorial type " + type.ToString() + " not currently supported.");
                break;
            case TutorialTypes.popup:
                if (complete_trigger == TutorialCompletionTriggers.clickButton)
                {
                    if (targetButton == null) return;
                    if (callReference == null) { Debug.LogWarning("Ref is Null"); return; }
                    targetButton.onClick.RemoveListener(callReference);
                    callReference = null;
                }
                else if (complete_trigger == TutorialCompletionTriggers.clickPopup)
                {
                    GameManager.Instance.SetTutorialPopupClickToClose(this, false);
                }
                else if (complete_trigger == TutorialCompletionTriggers.placeSemaphore)
                {
                    Debug.Log("Terminate event for tutorial " + complete_trigger.ToString() + " not currently supported.");
                }
                else if (complete_trigger == TutorialCompletionTriggers.placeSignal)
                {
                    Debug.Log("Terminate event for tutorial " + complete_trigger.ToString() + " not currently supported.");
                }
                else if (complete_trigger == TutorialCompletionTriggers.simulationInteraction)
                {
                    Debug.Log("Terminate event for tutorial " + complete_trigger.ToString() + " not currently supported.");
                }
                break;
        }
    }

    void StepListener(StepData inputStep)
    {
        if (inputStep.componentID == targetSimulationStep.componentID)
        {
            /*
            if (targetButton != null)
            {
                targetButton.onClick.RemoveAllListeners();
                targetButton.enabled = true;
                targetButton.onClick.AddListener(() => TriggerTutorialEventListener());
            }
            */
            if (PlayerInteraction_GamePhaseBehavior.pauseSimulation != null && pause == true)
            {
                PlayerInteraction_GamePhaseBehavior.pauseSimulation();
                //targetButton.onClick.AddListener(() => PlayerInteraction_GamePhaseBehavior.unpauseSimulation());
                if (pauseDuration != 0)
                {
                    PlayerInteraction_GamePhaseBehavior.delayedUnpause(pauseDuration, this);
                }
            }

            GameManager.Instance.CreateTutorialPopup(this, GameManager.Instance.GetGridManager().GetGridObjectByID(inputStep.componentID));
            Debug.Log("Listen for step!");
            PlayerInteraction_GamePhaseBehavior.onSimulationStep -= StepListener;
        }
        //if greater or equal 9000, the component is USER created so we can't assume its id
        else if (targetSimulationStep.componentID >= 9000 && inputStep.componentID >= 9000)
        {
            if (targetSimulationStep.componentStatus.passed > 0)
            {
                if (inputStep.componentStatus != null && inputStep.componentStatus.passed != null)
                {
                    if (inputStep.componentStatus.passed > 0)
                    {
                        if (PlayerInteraction_GamePhaseBehavior.pauseSimulation != null && pause == true)
                        {
                            PlayerInteraction_GamePhaseBehavior.pauseSimulation();
                            //targetButton.onClick.AddListener(() => PlayerInteraction_GamePhaseBehavior.unpauseSimulation());
                            if (pauseDuration != 0)
                            {
                                PlayerInteraction_GamePhaseBehavior.delayedUnpause(pauseDuration, this);
                            }
                        }
                        
                        GameManager.Instance.CreateTutorialPopup(this, GameManager.Instance.GetGridManager().GetGridObjectByID(inputStep.componentID));
                        Debug.Log("Listen for step!");
                        PlayerInteraction_GamePhaseBehavior.onSimulationStep -= StepListener;
                    }
                } 
            }
        }
    }
    
    void CompletionListener()
    {
        TriggerTutorialEventListener();
        PlayerInteraction_GamePhaseBehavior.onCompletion -= CompletionListener;
    }

    void MenuInteractionListener( PlayerInteraction_GamePhaseBehavior.MenuOptions inputOption)
    {
        Debug.Log("MenuInteractionListener");
        if (complete_trigger == TutorialCompletionTriggers.placeSignal || complete_trigger == TutorialCompletionTriggers.placeSemaphore)
        {
            if (inputOption == PlayerInteraction_GamePhaseBehavior.MenuOptions.button)
            {
                PlayerInteraction_GamePhaseBehavior.onMenuInteraction -= MenuInteractionListener;
                TriggerTutorialEventListener();
            }
            else if(inputOption == PlayerInteraction_GamePhaseBehavior.MenuOptions.semaphore)
            {
                PlayerInteraction_GamePhaseBehavior.onMenuInteraction -= MenuInteractionListener;
                TriggerTutorialEventListener();
            }
        }
    }
}
