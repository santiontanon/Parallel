using UnityEngine;
using System.Collections;

public class CabooseObject : MonoBehaviour {
	public Transform followObject;
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
			if(Vector3.Distance(transform.position, followObject.position) <= 0.1f ) 
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

	public void BeginFollow(Transform inputObject, float inputDistance, int inputOriginID)
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
		transform.position = Vector3.Lerp(transform.position, followObject.transform.position + (followObject.rotation*Vector3.right*-followDistance), 0.3f);
	}

	public void Disconnect()
	{
        Debug.Log("Disconnecting caboose: " + packageOriginID);
		followObject = null;
		followDistance = 0;
	}
}
