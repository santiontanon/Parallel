using UnityEngine;
using System.Collections;
using UnityEngine.UI;

public class Generate_GamePhaseBehavior : GamePhaseBehavior {
	public override void BeginPhase()
	{
		GameManager.Instance.InitiateTrackGeneration(true);
		GameManager.Instance.SetGamePhase(GameManager.GamePhases.PlayerInteraction);
	}

	public override void UpdatePhase()
	{

	}

	public override void EndPhase()
	{
		
	}
}
