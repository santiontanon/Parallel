using UnityEngine;
using System.Collections;
using UnityEngine.UI;

public class Exit_GamePhaseBehavior : GamePhaseBehavior {

    [System.Serializable] public class Exit_UI
    {
        [System.Serializable]
        public class ExitErrorOverlay : ParallelProg.UI.UIOverlay
        {
            public Text errorTextBox;
            public Button exitButton;
            public override void OpenPanel()
            {
                base.OpenPanel();
                exitButton.onClick.RemoveAllListeners();
                exitButton.onClick.AddListener(() => { GameManager.Instance.tracker.TriggerForceQuit(); });
            }
        }
        [SerializeField] public ExitErrorOverlay exitErrorOverlay;

    }
    [SerializeField] public Exit_UI exitUI;

	public override void BeginPhase()
	{
        Debug.Log("Exit.");
        GameManager.Instance.tracker.OnApplicationQuit();
	}

	public override void UpdatePhase()
	{

	}

	public override void EndPhase()
	{
		
	}

    public void ReportQuitError( string inputErrorDescription )
    {
        Debug.Log("Should open panel");
        exitUI.exitErrorOverlay.errorTextBox.text = inputErrorDescription;
        exitUI.exitErrorOverlay.OpenPanel();
    }


}
