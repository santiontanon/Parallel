using UnityEngine;
using System.Collections;

public class CabooseObject : MonoBehaviour {
	public Thread_GridObjectBehavior followObject;
    public bool instant;
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

	public void BeginFollow(Thread_GridObjectBehavior inputObject, float inputDistance, int inputOriginID, bool instant)
	{
		followObject = inputObject;
		followDistance = inputDistance;
		packageOriginID = inputOriginID;
		followObjectPassed = true;
        this.instant = instant;
		Appear();
	}

    public void UpdateFollow(bool instant)
    {
        this.instant = instant;
    }

	public void FollowBehavior()
	{
        TimeStepData timeStep = followObject.timeStep;
        int followStep = timeStep.timeStep;
        while(timeStep.timeStep != followStep - followDistance)
        {
            if (timeStep.timeStep > followStep - followDistance)
                timeStep = timeStep.previousStep;
            else
                timeStep = timeStep.nextStep;
        }
        Vector3 targetPos = new Vector3(timeStep.GetThread(followObject.component.id).pos.x, GameManager.Instance.GetLevelHeight() - timeStep.GetThread(followObject.component.id).pos.y, 0);
        if (instant == false)
            transform.position = Vector3.MoveTowards(transform.position, targetPos, 0.065f);
        else
            transform.position = targetPos;
	}

	public void Disconnect()
	{
		followObject = null;
		followDistance = 0;
	}
}
