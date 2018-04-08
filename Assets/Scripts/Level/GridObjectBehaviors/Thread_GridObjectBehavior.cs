using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Thread_GridObjectBehavior : GridObjectBehavior {

	[Header("Thread")]
	#region thread
	string currentDirection = "";
	public List<GameObject> trailObjectList = new List<GameObject>();
	#endregion

	public override void InitializeGridComponentBehavior()
	{
		if(component == null) return;
		switch(component.type)
		{
		case "thread":
			{
				SpriteRenderer instanceSpriteRenderer = GetComponent<SpriteRenderer>();
				instanceSpriteRenderer.sortingOrder = Constants.ComponentSortingOrder.thread;
				switch(component.configuration.initial_direction.ToLower())
				{
				case "east":
					transform.Rotate(0,0,0);
					break;
				case "north":
					transform.Rotate(0,0,90);
					break;
				case "south":
					transform.Rotate(0,0,-90);
					break;
				case "west":
					transform.Rotate(0,0,180);
					break;
				}
			}
			break;
		}
	}

	public override void ResetPosition()
	{
		Debug.Log("Reseting " + this.component.id.ToString());
		SpriteRenderer instanceSpriteRenderer = GetComponent<SpriteRenderer>();
		instanceSpriteRenderer.sortingOrder = Constants.ComponentSortingOrder.thread;
		base.ResetPosition();
		transform.rotation = Quaternion.identity;
		InitializeGridComponentBehavior();
		ClearCabooses();
	}

    public override void ReturnToStep(TimeStepData timeStep)
    {
        //thread
        iTween.Stop(gameObject);
        this.timeStep = timeStep;
        gameObject.transform.position = new Vector3(timeStep.GetThread(component.id).pos.x, GameManager.Instance.GetLevelHeight() - timeStep.GetThread(component.id).pos.y, 0);
        gameObject.transform.eulerAngles = timeStep.GetThread(component.id).rotation;
        lastSimulationPosition = gameObject.transform.position;

        //packages
        List<int> packageIds = new List<int>();
        for(int i = 0; i < timeStep.GetThread(component.id).packages.Count; i++)
        {
            if(timeStep.GetThread(component.id).packages[i].active == true)
            {
                packageIds.Add(timeStep.GetThread(component.id).packages[i].id);
            }
        }
        UpdateCabooses(packageIds, true);
    }

	public void ClearCabooses()
	{
		if(trailObjectList.Count > 0)
		{
			for(int trailIndex = trailObjectList.Count-1; trailIndex >= 0; trailIndex--)
			{
				GameObject destroyTrail = trailObjectList[trailIndex];
				trailObjectList.Remove( destroyTrail );
				Debug.Log("I'm gonna clear this caboose: " + destroyTrail);
				destroyTrail.GetComponent<CabooseObject>().Disconnect();
				Destroy( destroyTrail );
			}
		}
	}

	public override float DoStep(StepData inputStep, Dictionary<int, List<StepData>> dictionary = null)
	{
        while(timeStep.timeStep != inputStep.timeStep)
        {
            if(timeStep.timeStep > inputStep.timeStep)
            {
                timeStep = timeStep.previousStep;
            }
            else
            {
                timeStep = timeStep.nextStep;
            }
        }
		if(behaviorType==BehaviorTypes.component && component!=null)
		{
            if (inputStep.eventType == "M")
            {
                Vector2 reverseYposition = new Vector2(inputStep.componentPos.x, GameManager.Instance.GetLevelHeight() - inputStep.componentPos.y);
                /*if (inputStep.GetNextMove(dictionary) != null)
                {
                    StepData nextStep = inputStep.GetNextMove(dictionary);
                    reverseYposition = new Vector2(nextStep.componentPos.x, GameManager.Instance.GetLevelHeight() - nextStep.componentPos.y);
                }*/

                float xDiff = Mathf.Abs(Vector2.Distance(new Vector2(gameObject.transform.position.x, 0), new Vector2(reverseYposition.x, 0)));
                float yDiff = Mathf.Abs(Vector2.Distance(new Vector2(0, gameObject.transform.position.y), new Vector2(0, reverseYposition.y)));
                float travelTime = (xDiff + yDiff) * 0.25f;
                iTween.MoveTo(gameObject, iTween.Hash("x", reverseYposition.x, "y", reverseYposition.y, "time", travelTime, "easetype", iTween.EaseType.linear));

                Vector2 difference = lastSimulationPosition - reverseYposition;
                Quaternion targetRotation = new Quaternion();
                if (difference.x > 0) { targetRotation = Quaternion.Euler(0f, 0f, 180f); }
                else if (difference.x < 0) { targetRotation = Quaternion.Euler(0f, 0f, 0f); }
                else if (difference.y > 0) { targetRotation = Quaternion.Euler(0f, 0f, -90f); }
                else if (difference.y < 0) { targetRotation = Quaternion.Euler(0f, 0f, 90f); }
                if (difference.x == 0 && difference.y == 0) { }
                else { iTween.RotateTo(gameObject, targetRotation.eulerAngles, .5f); }
                lastSimulationPosition = reverseYposition;
                return travelTime;
            }
            else if (inputStep.eventType == "E")
            {
                if (inputStep.componentStatus != null)
                {
                    if (inputStep.componentStatus.passed != 0)
                    {
                        //passed is returned in increments for Thread E steps.  What is this for?
                    }
                    else if (inputStep.componentStatus.payload != null)
                    {
                        if (inputStep.componentStatus.payload.Length == 0)
                        {
                            // Moved swapping logic
                        }
                        else
                        {
                            UpdateCabooses(inputStep.componentStatus.payload, false);
                        }
                    }
                }
            }
            else if (inputStep.eventType == "D")
            {
                if (inputStep.componentStatus.exchange_between_b != 0)
                {
                    SwapTrails(inputStep.componentStatus.exchange_between_b);
                }

                if (inputStep.componentStatus.delivered_to != 0)
                {
                    //Debug.Log("Delivery Attempt");
                    //Debug.Log("Should perform delivery for " + inputStep.componentID);
                    GridObjectBehavior deliverToObject = GameManager.Instance.GetGridManager().GetGridObjectByID(inputStep.componentStatus.delivered_to);

                    if (inputStep.componentStatus.delivered_items.Length > 0)
                    {
                        /* Feedback for Cabooses */
                        foreach (int deliveryId in inputStep.componentStatus.delivered_items)
                        {
                            for (int i = trailObjectList.Count - 1; i >= 0; i--)
                            {
                                CabooseObject caboose = trailObjectList[i].GetComponent<CabooseObject>();
                                if (caboose.packageOriginID == deliveryId)
                                {
                                    //Debug.Log("Disconnecting caboose " + caboose.packageOriginID );
                                    caboose.Disconnect();
                                    if (inputStep.componentStatus.delivered_to != null && inputStep.componentStatus.delivered_to != 0)
                                    {
                                        iTween.MoveTo(caboose.gameObject, deliverToObject.transform.position, 1.5f);
                                    }
                                    iTween.ScaleTo(caboose.gameObject, Vector3.zero, 1.5f);
                                    Destroy(caboose.gameObject, 2f);
                                    trailObjectList.RemoveAt(i);
                                }
                            }
                        }

                        /* Feedback for Delivery point */
                        if (
                            (inputStep.componentStatus.missed != null && inputStep.componentStatus.missed != 0)
                            || (inputStep.componentStatus.missed_items != null && inputStep.componentStatus.missed_items.Length > 0)
                            )
                        {
                            deliverToObject.ErrorBehavior(Color.black);
                        }

                        else
                        {
                            deliverToObject.SuccessBehavior(GameManager.Instance.GetGridManager().GetColorByIndex(component.configuration.color));
                            Delivery_GridObjectBehavior deliveryCast = deliverToObject as Delivery_GridObjectBehavior;
                            Debug.Log("INCREMENT " + deliverToObject.component.id + " BY " + inputStep.componentStatus.delivered_items.Length);
                            deliveryCast.deliveryPopup.IncrementNumerator(inputStep.componentStatus.delivered_items.Length);
                        }
                    }
                    else if (inputStep.componentStatus.missed_items.Length > 0)
                    {
                        deliverToObject.ErrorBehavior(Color.black);
                    }

                }
            }
            else if (inputStep.eventType == "F")
            {
                GridObjectBehavior failed = GameManager.Instance.GetGridManager().GetGridObjectByID(inputStep.componentID);
                failed.ErrorBehavior(Color.black);
            }
        }
        return 0;
	}

    void UpdateCabooses(IEnumerable<int> payloadIds, bool instant)
    {
        List<int> currentCabooses = new List<int>();
        currentCabooses.AddRange(payloadIds);

        List<int> newCabooses = new List<int>();
        newCabooses.AddRange(payloadIds);

        List<GameObject> popCabooses = new List<GameObject>();

        /* figure out what cabooses to add to current */
        foreach (GameObject g in trailObjectList)
        {
            CabooseObject c = g.GetComponent<CabooseObject>();
            int currentId = c.packageOriginID;

            if (newCabooses.Contains(currentId))
            {
                if(c.instant != instant)
                {
                    c.UpdateFollow(instant);
                }
                newCabooses.Remove(currentId);

            }
            if (!currentCabooses.Contains(currentId)) { popCabooses.Add(g); }
        }

        /* figure out what cabooses to remove from current */
        for (int p = popCabooses.Count - 1; p >= 0; p--)
        {
            CabooseObject caboose = popCabooses[p].GetComponent<CabooseObject>();
            caboose.Disconnect();
            trailObjectList.Remove(popCabooses[p]);
            Destroy(popCabooses[p]);
        }

        foreach (int payloadId in newCabooses)
        {
            GridObjectBehavior payloadObject = GameManager.Instance.GetGridManager().GetGridObjectByID(payloadId);
            GameObject g = new GameObject();
            g.transform.position = gameObject.transform.position;
            g.transform.localScale = Vector3.zero;

            trailObjectList.Add(g);
            g.name = "payload_" + payloadId;
            g.AddComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite("package_tint_01");//payloadObject.GetComponent<SpriteRenderer>().sprite;
            g.GetComponent<SpriteRenderer>().color = GameManager.Instance.GetGridManager().GetColorByIndex(component.configuration.color);

            GameObject gChild = new GameObject();
            switch (payloadObject.component.configuration.type)
            {
                case "Conditional":
                    {
                        gChild.AddComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite("package_logo_03");
                    }
                    break;
                case "Unconditional":
                    {
                        gChild.AddComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite("package_logo_02");
                    }
                    break;
                case "Limited":
                    {
                        gChild.AddComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite("package_logo_01");
                    }
                    break;
                case "Empty":
                    {

                    }
                    break;
            }
            gChild.GetComponent<SpriteRenderer>().sortingOrder = Constants.ComponentSortingOrder.basicComponents + 1;
            gChild.transform.position = g.transform.position;
            gChild.transform.SetParent(g.transform);
            gChild.transform.localScale = Vector3.one;

            if (true /* item is in exchange */ )
            {
                /* figure out where the exchange point is, spawn it there */
                g.AddComponent<CabooseObject>().BeginFollow(this, (float)trailObjectList.Count, payloadId, instant);
            }
            else
            {
                g.AddComponent<CabooseObject>().BeginFollow(this, (float)trailObjectList.Count, payloadId, instant);
            }

        }
    }

	void SwapTrails(int thread_b_id) {
		Debug.Log("Swapping between " + this.component.id + " and " + thread_b_id);
		Thread_GridObjectBehavior otherTrain = GameManager.Instance.GetGridManager().GetGridObjectByID( thread_b_id ) as Thread_GridObjectBehavior;

		Vector3 fromPosition = transform.position;
		Vector3 toPosition = otherTrain.transform.position;

		if( teleportTrail ) 
		{
			GameObject teleportInstance = (GameObject) Instantiate( teleportTrail, fromPosition, Quaternion.identity ); 
			iTween.MoveTo( teleportInstance, toPosition, 2f );
			Destroy( teleportInstance, 2.5f );

		}

		otherTrain.ClearCabooses ();
		ClearCabooses ();
	}
}
