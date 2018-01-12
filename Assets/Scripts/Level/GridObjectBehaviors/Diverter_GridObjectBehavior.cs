using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Diverter_GridObjectBehavior : GridObjectBehavior {

	[Header("Signal")]
	#region signal type
	public LineRenderer lineRenderer;
	#endregion
	public override void InitializeGridComponentBehavior()
	{
		base.InitializeGridComponentBehavior();
        
		if(component == null) return;
		switch(component.type)
		{
		case "diverter":
			{
				/* generate the sections */
				SpriteRenderer instanceSpriteRenderer = GetComponent<SpriteRenderer>();
				instanceSpriteRenderer.enabled = false;

				Sprite directionSection = GameManager.Instance.GetGridManager().GetSprite("diverter_W");

				int west = 0;
				int south = 1;
				int east = 2;
				int north = 3;

				int index = 0;
				foreach( List<string> direction in component.configuration.directions_types)
				{
					//section instance -> direction node -> [types]
					GameObject directionNode = new GameObject();
					Vector3 offset = Vector3.zero;
					int typeLocation = 0;
					foreach(string type in direction)
					{
						GameObject typeNode = new GameObject();

						float typeNodeOffset = 0;
						if(typeLocation == 1) typeNodeOffset = -1f;
						else if(typeLocation == 2) typeNodeOffset = 1f;
						typeNode.transform.position = directionNode.transform.position + Vector3.up*0.15f*typeNodeOffset + Vector3.left*0.08f*(1f-Mathf.Abs(typeNodeOffset));
						typeLocation++;
						typeLocation = Mathf.Clamp(typeLocation, 0, 3);

						typeNode.transform.SetParent( directionNode.transform );
						SpriteRenderer typeSprite = typeNode.AddComponent<SpriteRenderer>();
						string targetPackage = "diverter_package_03";
						if(type=="Conditional"){}
						if(type=="Unconditional"){targetPackage = "diverter_package_02";}
						if(type=="Limited"){targetPackage = "diverter_package_01";}
						if(type=="Empty"){ typeSprite.color = Color.white*0f;}

						typeSprite.sprite = GameManager.Instance.GetGridManager().GetSprite(targetPackage);
						typeNode.name = targetPackage;
						typeSprite.sortingOrder = Constants.ComponentSortingOrder.basicComponents + 1;
					}

					if(index==north){offset=Vector3.up;}
					else if(index==south){offset=Vector3.down;}
					else if(index==east){offset=Vector3.right;}
					else if(index==west){offset=Vector3.left;}

					GameObject sectionInstance = new GameObject("section_"+index);
					sectionInstance.transform.position = transform.position;
					sectionInstance.transform.localScale = Vector3.one * 0.9f;
					sectionInstance.AddComponent<SpriteRenderer>().sprite = directionSection;
					sectionInstance.GetComponent<SpriteRenderer>().sortingOrder = Constants.ComponentSortingOrder.basicComponents;
					sectionInstance.transform.SetParent( transform );
					sectionInstance.transform.Rotate( Vector3.forward * index * 90f);

					directionNode.transform.position = transform.position + offset*0.35f;
					directionNode.transform.rotation = sectionInstance.transform.rotation;
					directionNode.transform.SetParent( sectionInstance.transform );
					directionNode.name = "direction_"+index.ToString();

					index++;
				}
			}
			break;
		}
	}
	/*
	public override void BeginInteraction()
	{
	}

	public override void ContinueInteraction()
	{
	}

	public override void EndInteraction()
	{
	}
	*/
	public override void OnHoverBehavior()
	{
		if(component!=null)
		{
			switch(component.type)
			{
			case "diverter":
				foreach(Transform t in transform)
				{
					if(t.childCount>0)
					{
						if(t.GetChild(0).childCount>0) {iTween.ScaleTo(t.gameObject, Vector3.one*1.5f, 0.5f);}
						else {iTween.ScaleTo(t.gameObject, Vector3.one*0.8f, 0.5f);}
					}
				}

				break;
			}
		}
	}

	public override void EndHoverBehavior()
	{
		if(component!=null)
		{
			switch(component.type)
			{
				case "diverter":
				foreach(Transform t in transform)
				{
					iTween.ScaleTo(t.gameObject, Vector3.one, 0.5f);
				}

				break;
			}

		}
	}


	/*
	public override void DoStep(StepData inputStep)
	{
	}
	*/
}
