using UnityEngine;
using System.Collections;
using UnityEngine.EventSystems;
using System.Collections.Generic;

public class GridObjectBehavior : MonoBehaviour
{
	
	#region all types
	public enum BehaviorTypes {track, component}
	public BehaviorTypes behaviorType;
	public enum InteractTypes {leftClick, rightClick}
	[Header("General")]
	public GridComponent component;
	public GridTrack track;
	public Camera levelCamera;
	public GameObject highlightObject;
	public GameObject teleportTrail;
	public GameObject lockObject;
	public Vector2 lastSimulationPosition;
	#endregion
	[Header("Track")]
	#region tracks
	public GameObject directionObject;
	#endregion
	[Header("Conditional")]
	#region conditional
	public List<GameObject> conditionalDirections = new List<GameObject>();
    #endregion
    public float lineColorVariant = 1f;

	void Awake()
	{
		levelCamera = GameManager.Instance.GetGridManager().worldCamera;
        lastSimulationPosition = gameObject.transform.position;
	}

	public virtual void InitializeGridComponentBehavior()
	{
		if(component == null) return;

        lineColorVariant = Random.Range(0.4f, 1f);

		if(component.type == "conditional" || component.type == "diverter" || component.type == "signal" || component.type == "semaphore") gameObject.layer = 8;

		//GetComponent<SpriteRenderer>().sortingOrder  = (int)Constants.ComponentSortingOrder.Parse(typeof(Constants), component.type);
		int sortingOrder = 0;
		if(component.type == "conditional" || component.type == "diverter" || component.type == "signal" || component.type == "semaphore") sortingOrder = Constants.ComponentSortingOrder.connectionComponents;
		else if(component.type == "track") sortingOrder = Constants.ComponentSortingOrder.track;
		else sortingOrder = Constants.ComponentSortingOrder.basicComponents;
		GetComponent<SpriteRenderer>().sortingOrder = sortingOrder;
		
		if(component.editable=="L" && component.type!="intersection" && component.type!="exchange" && component.type!="conditional" && component.type!="pickup") 
		{ 
			if(lockObject != null) {
				lockObject.SetActive(true);
				if(lockObject.GetComponent<SpriteRenderer>()) lockObject.GetComponent<SpriteRenderer>().sortingOrder = sortingOrder + 1;
			}
			
			//if(lockObject.GetComponent<SpriteRenderer>()) lockObject.GetComponent<SpriteRenderer>().sortingOrder = sortingOrder + 1;
		}

		switch(component.type)
		{
			case "diverter":
			break;
			case "intersection":
			{
				SpriteRenderer instanceSpriteRenderer = GetComponent<SpriteRenderer>();
				instanceSpriteRenderer.enabled = false;
			}
			break;
			case "conditional":
			{
				component.configuration.current_original = component.configuration.current;
				int index = 0;
				foreach(string direction in component.configuration.directions)
				{
					GameObject arrowInstance = new GameObject();
					arrowInstance.transform.position = transform.position;
					SpriteRenderer instanceSpriteRenderer = arrowInstance.AddComponent<SpriteRenderer>();
					instanceSpriteRenderer.sprite = GameManager.Instance.GetGridManager().GetSprite("direction_switch_pointer_" + direction.ToUpper()[0]);
					instanceSpriteRenderer.sortingOrder = GetComponent<SpriteRenderer>().sortingOrder + 1;
					arrowInstance.name = "conditional_direction_"+direction;
					arrowInstance.transform.SetParent(transform);
					conditionalDirections.Add( arrowInstance );
					if(index != component.configuration.current)
					{
						instanceSpriteRenderer.color = new Color(1f,1f,1f,0.5f);
					}
					index++;
				}
			}
			break;
		}
	}

	public void UpdateGridObjectPosition(Vector2 inputPosition)
	{ 
		if( behaviorType == BehaviorTypes.track )
		{
			track.position = inputPosition;	
		}
		else if( behaviorType == BehaviorTypes.component )
		{
			
			component.posX = Mathf.RoundToInt(inputPosition.x);
			component.posY = Mathf.RoundToInt(inputPosition.y);
		}
		lastSimulationPosition = inputPosition;
	}

	public void EnableGridObjectEventBehaviors(InteractTypes interactEventType = InteractTypes.leftClick )
	{
		switch(interactEventType){
			case InteractTypes.leftClick:
				{
					
				}
				break;
			case InteractTypes.rightClick:
				{
					if(behaviorType == BehaviorTypes.component)
					{
						if(component.type=="signal")
						{
							
						}
					}
				}
				break;
		}
	}

	public void ResetSprite()
	{
		GetComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite( component );
	}

	#region "Left Click Drags"
	public void BeginDrag()
	{
		Debug.Log("Start drag!");
	}
	public void ContinueDrag()
	{
		Vector3 dragToPosition = levelCamera.ScreenToWorldPoint(Input.mousePosition);
		dragToPosition.z = 0;
		transform.position = Vector3.Lerp(transform.position, dragToPosition, 1f);
	}
	public void EndDrag()
	{
		Debug.Log("Ending the drag");

		if( GameManager.Instance.GetGridManager().IsValidLocation( Input.mousePosition ) 
			&& !GameManager.Instance.GetGridManager().IsOccupied( Input.mousePosition )
		) 
		{
			Debug.Log("It's a good spot!");
			Vector3 dragToPosition = levelCamera.ScreenToWorldPoint(Input.mousePosition);
			Vector3 previousPosition = new Vector3( component.posX, GameManager.Instance.GetLevelHeight() - component.posY, 0 );
			dragToPosition = new Vector3( Mathf.RoundToInt(dragToPosition.x), Mathf.RoundToInt(dragToPosition.y), 0);
			transform.position = dragToPosition;

			Vector3 reverseYposition =  dragToPosition;
			reverseYposition.y = GameManager.Instance.GetLevelHeight() - reverseYposition.y;

			UpdateGridObjectPosition( reverseYposition );

			GameManager.Instance.GetGridManager().UpdateGridObjectPosition( previousPosition, dragToPosition );
		}
		else 
		{
			if( behaviorType == BehaviorTypes.track )
			{
				transform.position = track.position;
			}
			else if( behaviorType == BehaviorTypes.component )
			{
				transform.position = new Vector3(component.posX, GameManager.Instance.GetLevelHeight() - component.posY, 0);
			}
		}
	}
	#endregion

	#region "Basic Interaction Behavior"
	public virtual void BeginInteraction()
	{
		if(behaviorType == BehaviorTypes.component)
		{
			switch( component.type ) 
			{
				case "signal":
					//lineRenderer.SetPositions( new Vector3[]{ transform.position, transform.position } );
				break;
				case "intersection":
					GetComponent<SpriteRenderer>().enabled = true;
				break;
				case "semaphore":
					component.configuration.value = (component.configuration.value+1)%2;
					GetComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite( component );
				break;
				case "conditional":
					component.configuration.current++;
					if(component.configuration.current >= component.configuration.directions.Length) { component.configuration.current = 0;}
					int index = 0;
					foreach(GameObject g in conditionalDirections)
					{
						SpriteRenderer instanceSpriteRenderer = g.GetComponent<SpriteRenderer>();
						if(index==component.configuration.current) {instanceSpriteRenderer.color = new Color(1f,1f,1f,1f);}
						else {instanceSpriteRenderer.color = new Color(1f,1f,1f,0.5f);}
						index++;
					}
				break;
			}
		}
		else 
		{
			if(track != null && track.direction != null)
			{
				if(directionObject == null)
				{
					directionObject = new GameObject();
					directionObject.SetActive(false);
					directionObject.transform.position = new Vector3(transform.position.x,transform.position.y,transform.position.z-1.0f);
					directionObject.transform.SetParent( transform );
					//directionObject.transform.localScale = Vector3.one * 0.7f;
					directionObject.AddComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite( "flowArrow" );
					switch( track.direction ) 
					{
					case "V": directionObject.transform.Rotate(0,0,-90f); break;
					case "A": directionObject.transform.Rotate(0,0,90f); break;
					case ">": break;
					case "<": directionObject.transform.Rotate(0,0,180f); break;
					default: break;
					}
					if( track.type !='J' && track.type != 'E' )
					{
						directionObject.GetComponent<SpriteRenderer>().enabled = false;
					}
					directionObject.SetActive(true);
				}
				else 
				{
					directionObject.SetActive(true);
				}
			}
		}
	}
	public virtual void ContinueInteraction()
	{
		
	}

	public virtual void EndInteraction()
	{
		if(behaviorType == BehaviorTypes.component)
		{
			switch( component.type ) 
			{
				case "intersection":
					GetComponent<SpriteRenderer>().enabled = false;
				break;
			}
		}
		else 
		{
			if(track != null && track.direction != null)
			{
				if(directionObject)
				{
					directionObject.SetActive(false);
				}
			}
		}
	}
	#endregion

	public void LinkTo(GridObjectBehavior g)
	{
		if(component!=null && g.component!=null)
		{
			component.configuration.link = g.component.id;
		}
	}


	public virtual float DoStep(StepData inputStep)
	{
		if(behaviorType==BehaviorTypes.component && component!=null){
			switch(component.type.ToLower())
			{
			case "diverter":
				break;
			case "pickup":
				if( inputStep.eventType == "E" )
				{
					if(inputStep.componentStatus.available!=-1) 
					{
						if(inputStep.componentStatus.available==1) {iTween.ColorTo(gameObject, Color.white, 0.5f); iTween.ScaleTo(gameObject, Vector3.one, 0.5f);}
						else if(inputStep.componentStatus.available==0) {iTween.ColorTo(gameObject, new Color(0.5f,0.5f,0.5f), 0.5f);  iTween.ScaleTo(gameObject, Vector3.one*0.8f, 0.5f);}
					}
					else if(inputStep.componentStatus.passed!=0) {}
				}
				break;
			case "semaphore":
				int originalValue = component.configuration.value;
				component.configuration.value = inputStep.componentStatus.value;
				GetComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite( component );
				component.configuration.value = originalValue;
				break;
			case "exchange":
				if(inputStep.componentStatus.value==1)
				{
					Hashtable ht = new Hashtable();
					ht.Add("amount",Vector3.forward*0.5f);
					ht.Add("time",1);
					ht.Add("easetype",iTween.EaseType.linear);
					ht.Add("looptype",iTween.LoopType.loop);
					iTween.RotateBy(gameObject, ht);

					if(component.configuration.link!=-1)
					{
						GameObject linkExchange = GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).gameObject;
						iTween.RotateBy(linkExchange, ht);
					}
				}
				else 
				{
					iTween.Stop(gameObject,"RotateBy");
					if(component.configuration.link!=-1)
					{
						GameObject linkExchange = GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).gameObject;
						iTween.Stop(linkExchange,"RotateBy");
					}
				}
				break;
			}
		}
        return 0;
	}


	public virtual void SuccessBehavior(Color successColor)
	{
		iTween.ScaleFrom( gameObject, Vector3.one*1.4f, 1.5f );
		iTween.ColorFrom( gameObject, successColor, 1.5f );
	}

	public virtual void ErrorBehavior(Color errorColor)
	{
		iTween.ScaleFrom( gameObject, Vector3.zero, 1.5f );
		iTween.ColorFrom( gameObject, errorColor, 1.5f );
	}

	public virtual void ResetPosition()
	{
		iTween.Stop(gameObject);
		//iTween.MoveTo(gameObject, new Vector3(component.posX, GameManager.Instance.GetLevelHeight() - component.posY, 0), 0f);
		gameObject.transform.position = new Vector3(component.posX, GameManager.Instance.GetLevelHeight() - component.posY, 0);
		lastSimulationPosition = new Vector3(component.posX,  GameManager.Instance.GetLevelHeight() - component.posY, 0);
		transform.rotation = Quaternion.identity;

		if(component.type == "pickup")
		{
			iTween.ColorTo(gameObject, Color.white, 0.1f);
			transform.localScale = Vector3.one;
		}
		else if(component.type == "exchange")
		{
			iTween.Stop(gameObject);
			transform.localScale = Vector3.one;
			transform.rotation = Quaternion.identity;
		}
		else if(component.type == "semaphore")
		{
			GetComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite( component );
		}
		else if(component.type == "conditional")
		{
			if(component.configuration.current_original != null) component.configuration.current = component.configuration.current_original;
			int index = 0;
			foreach(GameObject g in conditionalDirections)
			{
				if(index==component.configuration.current) g.GetComponent<SpriteRenderer>().color = new Color(1f,1f,1f,1f);
				else g.GetComponent<SpriteRenderer>().color = new Color(1f,1f,1f,0.5f);
				index++;
			}
		}
	}

	public virtual void OnHoverBehavior()
	{
		if(component!=null)
		{
			switch(component.type)
			{
			case "exchange":
				//GetComponent<SpriteRenderer>().color = Color.black;
				SetHighlight(true);
				if(component.configuration.link != -1)
				{
					GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).SetHighlight(true);
				}
			break;
			case "delivery":
				{
					
				}
				break;
			case "conditional":
			case "semaphore":
				SetHighlight(true);
				GridObjectBehavior[] possibleSignalLinks = GameManager.Instance.GetGridManager().RetrieveComponentsOfType("signal");
				foreach(GridObjectBehavior g in possibleSignalLinks)
				{
					if(g.component.configuration.link == component.id) { g.SetHighlight(true); }
				}
			break;
			case "pickup":
				{
					SetHighlight(true);
					List<GridObjectBehavior> deliveries = GameManager.Instance.GetGridManager().GetGridComponentsOfType("delivery");
					foreach(GridObjectBehavior g in deliveries) 
					{ 
						
						if (component.configuration.type == "Unconditional") 
							{ 
								//g.SetHighlight(true); 
								if( g.component.configuration.accepted_colors.Length == 0 )
								{
									g.SetHighlight(true);
								}
								else 
								{
									foreach(int colorIndex in g.component.configuration.accepted_colors)
									{
										if(colorIndex == component.configuration.color) 
										{
											g.SetHighlight(true);  
										}
									} 
								}
							}
						else
						{
							if( g.component.configuration.accepted_types.Length == 0 )
							{
								if( g.component.configuration.accepted_colors.Length == 0 )
								{
									g.SetHighlight(true);
								}
								else 
								{
									foreach(int colorIndex in g.component.configuration.accepted_colors)
									{
										if(colorIndex == component.configuration.color) 
										{
											g.SetHighlight(true);  
										}
									} 
								}
							}
							else 
							{
								foreach(string accepted_type in g.component.configuration.accepted_types) 
								{ 
									if(component.configuration.type == accepted_type) 
									{ 
										if( g.component.configuration.accepted_colors.Length == 0 )
										{
											g.SetHighlight(true);
										}
										else 
										{
											foreach(int colorIndex in g.component.configuration.accepted_colors)
											{
												if(colorIndex == component.configuration.color) 
												{
													g.SetHighlight(true);  
												}
											} 
										}
									} 
								}
							}
							 
						} 
					}
				}
				break;
			case "diverter":
			break;
			}
		}
	}
	public virtual void EndHoverBehavior()
	{
		if(component!=null)
		{
			switch(component.type)
			{
				case "signal":
				case "exchange":
					//GetComponent<SpriteRenderer>().color = Color.white;
					SetHighlight(false);
					if(component.configuration.link != -1)
					{
						GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).SetHighlight(false);
					}
				break;
				case "delivery":
					
				break;
				case "conditional":
				case "semaphore":
					SetHighlight(false);
					GridObjectBehavior[] possibleSignalLinks = GameManager.Instance.GetGridManager().RetrieveComponentsOfType("signal");
					foreach(GridObjectBehavior g in possibleSignalLinks)
					{
						if(g.component.configuration.link == component.id) { g.SetHighlight(false); }
					}
				break;
				case "pickup":
				{
					SetHighlight(false);
					List<GridObjectBehavior> deliveries = GameManager.Instance.GetGridManager().GetGridComponentsOfType("delivery");

					foreach(GridObjectBehavior g in deliveries) 
					{ 
						if(component.configuration.type == "Unconditional") 
						{ 
							if( g.component.configuration.accepted_colors.Length == 0 )
							{
								g.SetHighlight(false);
							}
							else 
							{
								foreach(int colorIndex in g.component.configuration.accepted_colors)
								{
									if(colorIndex == component.configuration.color) 
									{
										g.SetHighlight(false);  
									}
								} 
							}
						}
						else
						{
							if( g.component.configuration.accepted_types.Length == 0 )
							{
								if( g.component.configuration.accepted_colors.Length == 0 )
								{
									g.SetHighlight(false);
								}
								else 
								{
									foreach(int colorIndex in g.component.configuration.accepted_colors)
									{
										if(colorIndex == component.configuration.color) 
										{
											g.SetHighlight(false);  
										}
									} 
								}
							}
							else 
							{
								foreach(string accepted_type in g.component.configuration.accepted_types) 
								{ 
									if(component.configuration.type == accepted_type) 
									{ 
										if( g.component.configuration.accepted_colors.Length == 0 )
										{
											g.SetHighlight(false);
										}
										else 
										{
											foreach(int colorIndex in g.component.configuration.accepted_colors)
											{
												if(colorIndex == component.configuration.color) 
												{
													g.SetHighlight(false);  
												}
											} 
										}
									} 
								}
							}

						} 
					}
				}
				break;
				case "diverter":
				break;
			}

		}
	}

	public virtual void SetHighlight(bool isEnabled)
	{
		if(isEnabled)
		{
			highlightObject.transform.localScale = Vector3.zero;
			iTween.ScaleTo( highlightObject, iTween.Hash("scale", Vector3.one * 1.5f, "time", 0.5f) );
			highlightObject.SetActive(true);
		}
		else
		{
			highlightObject.transform.localScale = Vector3.one * 1.5f;
			iTween.ScaleTo( highlightObject, iTween.Hash("scale", Vector3.one * 0f, "time", 1f, "onComplete", "TurnOffHighlight") );
		}
	}

	void TurnOffHighlight()
	{
		highlightObject.SetActive(false);
	}

}
