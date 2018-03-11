using UnityEngine;
using System.Collections;

public class CabooseObject : MonoBehaviour {
	public Thread_GridObjectBehavior followObject;
	public float followDistance;
	public int packageOriginID;
	Sprite cabooseSprite;

	enum CabooseTypes {Package}

	bool followObjectPassed = false;

	void Appear()
	{
		iTween.ScaleTo(gameObject, Vector3.one, 0.5f);	
	}

	void Update () {
		if(!followObjectPassed)
		{
			if(Vector3.Distance(transform.position, followObject.gameObject.transform.position) <= 0.1f ) 
			{
				Appear();
				followObjectPassed = true;
			}
		}
		else if(followObject != null && followDistance != null) 
		{
			FollowBehavior();
		}

	}

	public void BeginFollow(Thread_GridObjectBehavior inputObject, float inputDistance, int inputOriginID)
	{
		followObject = inputObject;
		followDistance = inputDistance;
		packageOriginID = inputOriginID;

		/*if( Vector3.Distance ( GameManager.Instance.GetGridManager().GetGridObjectByID( inputOriginID ).transform.position, transform.position) >= 1f ) 
		{ 
			followObjectPassed = true;
		}*/

		followObjectPassed = true;
		Appear();
	}

	public void FollowBehavior()
	{
        //transform.position = Vector3.Lerp(transform.position, followObject.transform.position + (followObject.gameObject.transform.rotation*Vector3.right*-followDistance), 0.3f);
        TimeStepData timeStep = followObject.timeStep;
        int followStep = timeStep.timeStep;
        while(timeStep.timeStep != followStep - followDistance)
        {
            timeStep = timeStep.previousStep;
        }
        Vector3 targetPos = new Vector3(timeStep.GetThread(followObject.component.id).pos.x, GameManager.Instance.GetLevelHeight() - timeStep.GetThread(followObject.component.id).pos.y, 0);
        transform.position = Vector3.Lerp(transform.position, targetPos, 0.3f);
	}

	public void Disconnect()
	{
        Debug.Log("Disconnecting caboose: " + packageOriginID);
		followObject = null;
		followDistance = 0;
	}
}
