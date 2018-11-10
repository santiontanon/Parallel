using System;
using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class GridManager : MonoBehaviour {
	int currentGridWidth;
	int currentGridHeight;
	List<GridObjectBehavior> currentLevelObjects = new List<GridObjectBehavior>();

	public Transform gridContainer;
	public GameObject gridElementPrefab;
    public Camera worldCamera;
    /*
    public Sprite[] roadPieces;
	public Sprite[] componentPieces;
    */
    //public Sprite[] gridSprites;
	Dictionary<string,Sprite> gridElementDictionary = new Dictionary<string,Sprite>();
	Dictionary<int,Color> colorDictionary = new Dictionary<int,Color>();

	Dictionary<int,GridObjectBehavior> gridObjectLevelIDDictionary = new Dictionary<int,GridObjectBehavior>();
	Dictionary<Vector3,GridObjectBehavior> gridObjectLevelPositionDictionary = new Dictionary<Vector3,GridObjectBehavior>();
	//Dictionary<int, List<GridTrack>> gridTrackColorDictionary = new Dictionary<int, List<GridTrack>>();

	int playerPlacedElementCount = 0;

	void Awake()
	{
		//TODO: Automate this process (pull all sprite files from the resource folder
        Sprite[] gridSprites = Resources.LoadAll<Sprite>("Sprites/gridsprites_v3") as Sprite[];
        foreach (Sprite g in gridSprites)
        {
            gridElementDictionary[g.name] = g;
        }
		gridSprites = Resources.LoadAll<Sprite>("Sprites/DeliveryPoint_Sprites") as Sprite[];
		foreach (Sprite g in gridSprites)
		{
			gridElementDictionary[g.name] = g;
		}
        gridSprites = Resources.LoadAll<Sprite>("Sprites/gridsprites_deadEnd_showLinks") as Sprite[];
        foreach (Sprite g in gridSprites)
        {
            gridElementDictionary[g.name] = g;
        }
        gridSprites = Resources.LoadAll<Sprite>("Sprites/Tracks_03.27.18-white") as Sprite[];
        foreach (Sprite g in gridSprites)
        {
            gridElementDictionary[g.name] = g;
        }
        gridSprites = Resources.LoadAll<Sprite>("Sprites/Tracks_03.27.18-black") as Sprite[];
        foreach (Sprite g in gridSprites)
        {
            gridElementDictionary[g.name] = g;
        }

        //colorDictionary.Add(0, Color.white);
        colorDictionary.Add(0, Color.green);
		colorDictionary.Add(1, Color.yellow);
		colorDictionary.Add(2, new Color(1f, .5f, .1f));
		colorDictionary.Add(3, Color.magenta);
		colorDictionary.Add(4, Color.red);
		colorDictionary.Add(5, new Color(0.4f,0f,0.8f));
		colorDictionary.Add(6, Color.black);

		if(!gridContainer) gridContainer = GameObject.Find("Level").transform;

	}

    public void ClearGrid(bool all)
    {
        if (all == true)
        {
            currentLevelObjects.Clear();
            gridObjectLevelIDDictionary.Clear();
            gridObjectLevelPositionDictionary.Clear();
            //gridTrackColorDictionary.Clear();

            if (gridContainer)
            {
                foreach (Transform t in gridContainer)
                {
                    GameObject.Destroy(t.gameObject);
                }
            }
        }
        else
        {
            for (int i = 0; i < currentLevelObjects.Count; i++)
            {
                if (currentLevelObjects[i].component.placedBy == "P")
                {
                    GridObjectBehavior g = currentLevelObjects[i];
                    gridObjectLevelIDDictionary.Remove(g.component.id);
                    gridObjectLevelPositionDictionary.Remove(g.transform.position);
                    currentLevelObjects.Remove(g);
                    Destroy(g.gameObject);
                    i--;
                }
            }
        }
    }

    public void GenerateGrid(List<GridTrack>gridTracks, List<GridComponent> gridComponents)
	{
        ClearGrid(true);

		/*
		currentGridWidth = layoutList[0].Length;
		currentGridHeight = layoutList.Count;
		*/

		Vector3 averagePosition = new Vector3();

		//Debug.Log(gridTracks.Count + ", " + gridComponents.Count);

		foreach(GridTrack track in gridTracks)
		{
			Vector2 reversedYposition = track.position;
			reversedYposition.y = GameManager.Instance.GetLevelHeight() - reversedYposition.y;

			GameObject gridElementInstance = Instantiate(gridElementPrefab, reversedYposition, Quaternion.identity) as GameObject;

			SpriteRenderer instanceSpriteRenderer = gridElementInstance.GetComponent<SpriteRenderer>();
			GridObjectBehavior behavior = gridElementInstance.GetComponent<GridObjectBehavior>();

			gridElementInstance.transform.SetParent(gridContainer);

			averagePosition += new Vector3(reversedYposition.x, reversedYposition.y, 0);

			gridElementInstance.name = track.type+"_track";

			behavior.behaviorType = GridObjectBehavior.BehaviorTypes.track;
			behavior.track = track;
            
			switch (track.type)
			{
				case 'J':
					// Straight_00
					instanceSpriteRenderer.sprite = gridElementDictionary["white_straight"];
					gridElementInstance.transform.Rotate(0,0,90);

					break;
				case 'E':
					// Straight_01
					instanceSpriteRenderer.sprite = gridElementDictionary["white_straight"];
					break;
				case 'N':
					// ThreeWay_00
					instanceSpriteRenderer.sprite = gridElementDictionary["white_3_intersection"];
					gridElementInstance.transform.Rotate(0, 0, 90);
					break;
				case 'M':
					// ThreeWay_01
					instanceSpriteRenderer.sprite = gridElementDictionary["white_3_intersection"];
					gridElementInstance.transform.Rotate(0,0,180);	
					break;
				case 'K':
					// ThreeWay_02
					instanceSpriteRenderer.sprite = gridElementDictionary["white_3_intersection"];	
					gridElementInstance.transform.Rotate(0, 0, -90);
					break;
				case 'G':
					// ThreeWay_03
					instanceSpriteRenderer.sprite = gridElementDictionary["white_3_intersection"];
					break;
				case 'F':
					// Turn_00
					instanceSpriteRenderer.sprite = gridElementDictionary["white_corner"];
					gridElementInstance.transform.Rotate(0,0,90);
					break;
				case 'C':
					// Turn_01
					instanceSpriteRenderer.sprite = gridElementDictionary["white_corner"];

					break;
				case 'I':
					// Turn_02
					instanceSpriteRenderer.sprite = gridElementDictionary["white_corner"];
					gridElementInstance.transform.Rotate(0,0,270);
					break;
				case 'L':
					// Turn_03
					instanceSpriteRenderer.sprite = gridElementDictionary["white_corner"];
					gridElementInstance.transform.Rotate(0,0,180);
					break;
				case 'O':
					// FourWay
					instanceSpriteRenderer.sprite = gridElementDictionary["white_4_intersection"];
					gridElementInstance.transform.Rotate(0,0,180);
					break;
				/*A - EAST DEAD
				 *B - NORTH DEAD
				 *D - WEST DEAD
				 *H - SOUTH DEAD*/

				///* TODO: Enable Dead ends
				case 'A':
					instanceSpriteRenderer.sprite = gridElementDictionary["deadEnd_road"];
					gridElementInstance.transform.Rotate(0,0,90);
					break;
				case 'B':
					instanceSpriteRenderer.sprite = gridElementDictionary["deadEnd_road"];
					//gridElementInstance.transform.Rotate(0,0,180);
					break;
				case 'D':
					instanceSpriteRenderer.sprite = gridElementDictionary["deadEnd_road"];
					gridElementInstance.transform.Rotate(0,0,-90);
					break;
				case 'H':
					instanceSpriteRenderer.sprite = gridElementDictionary["deadEnd_road"];
					gridElementInstance.transform.Rotate(0,0,180);
					break;
				//*/
				default:
					instanceSpriteRenderer.sprite= null;
					break;
			}
            instanceSpriteRenderer.color = new Color(0.2f,0.2f,0.2f,1f);
			currentLevelObjects.Add(behavior);
		}

		averagePosition = averagePosition / gridTracks.Count;
		worldCamera.transform.position = new Vector3( averagePosition.x, averagePosition.y, -15);
		float levelheight = GameManager.Instance.GetLevelHeight ();
		float levelRatio = GameManager.Instance.GetLevelWidth () / GameManager.Instance.GetLevelHeight ();
		if (levelRatio > 1.78f) { // 16:9
			Debug.Log("WIDER LEVEL "+levelRatio);
			levelheight *= (levelRatio/1.78f);
			levelheight /= 0.9f; // To make room for the right banner
			
		} else {
			levelheight +=1;
		}
		worldCamera.orthographicSize = ((levelheight/2.0f)/0.78f); // To make room for the bottom banner
        
		foreach(GridComponent gridComponent in gridComponents)
		{
			int color = gridComponent.configuration.color;
			int positionX = gridComponent.posX;
			int positionY = GameManager.Instance.GetLevelHeight() - gridComponent.posY;
			string type = gridComponent.type;

			GameObject gridElementInstance = Instantiate(gridElementPrefab, new Vector3(positionX,positionY), Quaternion.identity) as GameObject;

			SpriteRenderer instanceSpriteRenderer = gridElementInstance.GetComponent<SpriteRenderer>();
			instanceSpriteRenderer.sortingOrder = Constants.ComponentSortingOrder.basicComponents;
			//instanceSpriteRenderer.color = colorDictionary[color];

			GridObjectBehavior behavior = gridElementInstance.GetComponent<GridObjectBehavior>();
			if(type == "signal")
			{
				Signal_GridObjectBehavior signal_Behavior = gridElementInstance.AddComponent<Signal_GridObjectBehavior>();
				//signal_Behavior.highlightObject = behavior.highlightObject;
				signal_Behavior.lineRenderer = gridElementInstance.GetComponentInChildren<LineRenderer>();
				signal_Behavior.teleportTrail = behavior.teleportTrail;
				signal_Behavior.lockObject = behavior.lockObject;
				Destroy( behavior );
				behavior = signal_Behavior;
			}
			else if(type == "thread")
			{
				Thread_GridObjectBehavior thread_Behavior = gridElementInstance.AddComponent<Thread_GridObjectBehavior>();
				//thread_Behavior.highlightObject = behavior.highlightObject;
				thread_Behavior.teleportTrail = behavior.teleportTrail;
				Destroy( behavior );
				behavior = thread_Behavior;
			}
			else if(type == "diverter")
			{
				Diverter_GridObjectBehavior diverter_Behavior = gridElementInstance.AddComponent<Diverter_GridObjectBehavior>();
				//diverter_Behavior.highlightObject = behavior.highlightObject;
				diverter_Behavior.teleportTrail = behavior.teleportTrail;
				Destroy( behavior );
				behavior = diverter_Behavior;
				gridElementInstance.layer = 8;
			}
			else if(type == "delivery")
			{
				Delivery_GridObjectBehavior delivery_Behavior = gridElementInstance.AddComponent<Delivery_GridObjectBehavior>();
				//delivery_Behavior.highlightObject = behavior.highlightObject;
				delivery_Behavior.teleportTrail = behavior.teleportTrail;
				Destroy( behavior );
                //delivery_Behavior.name = 
				behavior = delivery_Behavior;
                if(gridComponent.configuration.accepted_types.Length == 0)
                {
                    gridElementInstance.name = "universal_delivery";
                }
                else
                {
                    if (gridComponent.configuration.accepted_types[0] == "Unconditional")
                    {
                        gridElementInstance.name = "unconditional_delivery";
                    }
                    else if (gridComponent.configuration.accepted_types[0] == "Conditional")
                    {
                        gridElementInstance.name = "conditional_delivery";
                    }
                    else if (gridComponent.configuration.accepted_types[0] == "Limited")
                    {
                        gridElementInstance.name = "limited_delivery";
                    }
                }
			}
            else if(type == "pickup")
            {
                if(gridComponent.configuration.type == "Conditional")
                {

                    gridElementInstance.name = "conditional_pickup";
                }
                else if (gridComponent.configuration.type == "Unconditional")
                {
                    gridElementInstance.name = "unconditional_pickup";
                }
                else if(gridComponent.configuration.type == "Limited")
                {
                    gridElementInstance.name = "limited_pickup";
                }
            }

			gridElementInstance.transform.SetParent(gridContainer);

            if(type != "delivery" && type != "pickup")
			    gridElementInstance.name = type;

			behavior.behaviorType = GridObjectBehavior.BehaviorTypes.component;
			behavior.component = gridComponent;

            instanceSpriteRenderer.sprite = GetSprite(gridComponent);

			currentLevelObjects.Add(behavior);
			gridObjectLevelIDDictionary.Add(gridComponent.id,behavior);
			gridObjectLevelPositionDictionary.Add(behavior.transform.position,behavior);


            /* NOTE 001 moving initialization to after full spawn, so objects can have a full view of the level for avoiding neighbors
               behavior.InitializeGridComponentBehavior();
            */
        }

        //ADDITION 001: moving initialization to after full spawn
        foreach (GridObjectBehavior behavior in currentLevelObjects)
        {
           if(behavior.behaviorType != GridObjectBehavior.BehaviorTypes.track) behavior.InitializeGridComponentBehavior();
        }
    }

	public void RevealGridColorMask(int mask)
	{
		GameManager.Instance.tracker.CreateEventExt("RevealGridColorMask",mask.ToString());
		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.track)
			{
				if(g.track.CanUsePath( mask ))
				{
					g.GetComponent<SpriteRenderer>().color = colorDictionary[mask];
				}
				else 
				{
					g.GetComponent<SpriteRenderer>().color = new Color(0.2f,0.2f,0.2f,1f);
				}
			}
		}
	}
	public void HideGridColorMask()
	{
		GameManager.Instance.tracker.CreateEventExt("HideGridColorMask","");
		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.track)
			{
				g.GetComponent<SpriteRenderer>().color = new Color(0.2f,0.2f,0.2f,1f);
			}
		}
	}


	public List<GridObjectBehavior> GetGridComponentsOfType(string inputType)
	{
		List<GridObjectBehavior> returnComponents = new List<GridObjectBehavior>();

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.component )
			{
				if(g.component.type == inputType)
				{
					returnComponents.Add( g );
				}
			}
		}

		return returnComponents;
	}

	public List<GridObjectBehavior> GetGridComponentsOfType(  List<string> inputTypes )
	{
		List<GridObjectBehavior> returnComponents = new List<GridObjectBehavior>();

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.component )
			{
				if(inputTypes.Contains( g.component.type ))
				{
					returnComponents.Add( g );
				}
			}
		}

		return returnComponents;
	}

	public bool IsValidLocation(Vector2 inputPosition)
	{
		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputPosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);

		bool returnValid = false;

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.transform.position == testPosition && g.behaviorType == GridObjectBehavior.BehaviorTypes.track && !g.track.type.Equals('-') && g.track.type != null)
			{
				//Debug.Log("Object " + g.transform.name + " is at position! ");
				returnValid = true;
			}
            
		}
			
		return returnValid;	
	}

	public bool IsOccupied(Vector2 inputPosition)
	{
		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputPosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);

		bool returnOccupy = false;

		if(gridObjectLevelPositionDictionary.ContainsKey( testPosition )) { returnOccupy = true; }

		return returnOccupy;
	}

	public bool IsCurrentThreadColor(int colorQuery)
	{
		bool isCurrentThreadColor = false;

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.component && g.component != null)
			{
				if(g.component.type == "thread" && g.component.configuration.color == colorQuery) 
				{
					//Debug.Log("I found color " + colorQuery + " at " + g.name + ", " + g.component.id);
					isCurrentThreadColor = true; 
				}
			}
		}
		return isCurrentThreadColor;
	}

	public void PlaceGridElementAtLocation(Vector2 inputPosition, PlayerInteraction_GamePhaseBehavior.MenuOptions inputGridElement)
	{
		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputPosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);


		GridComponent c = new GridComponent();
		c.configuration = new Configuration();

		string type = "package_exchangepoint_00";
		switch(inputGridElement)
		{
			case PlayerInteraction_GamePhaseBehavior.MenuOptions.semaphore:
				type = "semaphore";
			break;
			case PlayerInteraction_GamePhaseBehavior.MenuOptions.button:
				type = "signal";
			break;
		}

		c.editable = "E";
		c.placedBy = "P";

		playerPlacedElementCount++;
		c.id = 9000 + playerPlacedElementCount;

		c.posX = (int)testPosition.x;
		c.posY = (int)testPosition.y;
		c.type = type;//inputGridElement.ToString();
		c.configuration.link = -1;

		GameObject gridElementInstance = Instantiate(gridElementPrefab, testPosition, Quaternion.identity) as GameObject;
		SpriteRenderer instanceSpriteRenderer = gridElementInstance.GetComponent<SpriteRenderer>();
		instanceSpriteRenderer.sortingOrder = Constants.ComponentSortingOrder.basicComponents;

		instanceSpriteRenderer.sprite = GetSprite(c);

		gridElementInstance.transform.SetParent(gridContainer);

		//instanceSpriteRenderer.color = colorDictionary[color];
		GridObjectBehavior behavior = gridElementInstance.GetComponent<GridObjectBehavior>();
		if(type == "signal")
		{
			Signal_GridObjectBehavior signal_Behavior = gridElementInstance.AddComponent<Signal_GridObjectBehavior>();
			//signal_Behavior.highlightObject = behavior.highlightObject;
			signal_Behavior.lineRenderer = gridElementInstance.GetComponentInChildren<LineRenderer>();
			signal_Behavior.teleportTrail = behavior.teleportTrail;
			Destroy( behavior );
			behavior = signal_Behavior;
		}

		behavior.component = c;
		behavior.behaviorType = GridObjectBehavior.BehaviorTypes.component;

		Vector2 reverseYposition = new Vector2(c.posX, GameManager.Instance.GetLevelHeight() - c.posY);
		behavior.UpdateGridObjectPosition( reverseYposition );

		currentLevelObjects.Add(behavior);

		gridObjectLevelIDDictionary.Add(behavior.component.id, behavior);
		gridObjectLevelPositionDictionary.Add(behavior.transform.position,behavior);

		behavior.InitializeGridComponentBehavior();
	}
		
	public void ForgetGridElement(GridObjectBehavior objectToRemove)
	{
		Vector3 testPosition = new Vector3( objectToRemove.component.posX, objectToRemove.component.posY, 0 );
		Vector3 reversedYposition = new Vector3( testPosition.x, GameManager.Instance.GetLevelHeight()-testPosition.y, 0);
		string s = "At location " + reversedYposition.ToString() + ", Removing object from ";
		if( gridObjectLevelPositionDictionary.ContainsKey( reversedYposition ) ) { s+="Grid Object Level Position Dictionary, "; gridObjectLevelPositionDictionary.Remove( reversedYposition ); }
		if( gridObjectLevelIDDictionary.ContainsKey( objectToRemove.component.id ) ) { s+="Grid Object Level ID Dictionary, "; gridObjectLevelIDDictionary.Remove( objectToRemove.component.id ); }
		if( currentLevelObjects.Contains( objectToRemove ) ) { s+="and Current Level Objects"; currentLevelObjects.Remove( objectToRemove ); }

	}

	public void UpdateGridObjectPosition(Vector3 oldPosition, Vector3 newPosition)
	{
		Debug.Log("Checking to update object from position " + oldPosition + " to " + newPosition);
		if( gridObjectLevelPositionDictionary.ContainsKey( oldPosition ) )
		{
			Debug.Log("Updating object from position " + oldPosition + " to " + newPosition);
			GridObjectBehavior g = gridObjectLevelPositionDictionary[oldPosition];
			gridObjectLevelPositionDictionary.Remove( oldPosition );
			gridObjectLevelPositionDictionary.Add( newPosition, g );
		}
	}

	public Sprite GetSprite(GridComponent inputComponent)
	{
		Sprite returnSprite = null;
		string inputType = inputComponent.type;
		int color = inputComponent.configuration.color;
		int value = inputComponent.configuration.value;

		switch(inputType)
		{
		case "thread":
			string threadIndex = "01";

			if(color!=-1)
			{
				if(color<=0) { color = 0; }
				threadIndex = (color).ToString("00");
			}
			try{
				returnSprite = gridElementDictionary["train_"+threadIndex]; 
			}
			catch
			{
				Debug.LogError( "Couldn't find train sprite for: train_" + threadIndex );
			}
			break;
		case "diverter":
			returnSprite = gridElementDictionary["diverter"];
			break;
		case "spawner":
			break;
		case "signal":
			if(inputComponent.configuration.passed == 1) { returnSprite = gridElementDictionary["button_down"]; }
			else { returnSprite = gridElementDictionary["button_up"]; }
			break;
		case "semaphore":
			returnSprite = gridElementDictionary["semaphore"];
			if(value==1)
			{
				returnSprite = gridElementDictionary["semaphore_inactive"];	
			}
			break;
		case "pickup":
			if(inputComponent.configuration.type == "Unconditional")
			{
				returnSprite = gridElementDictionary["package_02"];
			}
			else if(inputComponent.configuration.type == "Conditional")
			{
				returnSprite = gridElementDictionary["package_03"];
			}
			else if(inputComponent.configuration.type == "Limited")
			{
				returnSprite = gridElementDictionary["package_01"];
			}
			break;
		case "delivery":
			returnSprite = gridElementDictionary["package_deliverypoint_02"];
			if(inputComponent.configuration.accepted_types.Length == 0)
			{
				returnSprite = gridElementDictionary["package_deliverypoint_00"];
			}
			else 
			{
				if(inputComponent.configuration.accepted_types[0] == "Unconditional")
				{
					returnSprite = gridElementDictionary["package_deliverypoint_02"];
				}
				else if(inputComponent.configuration.accepted_types[0] == "Conditional")
				{
					returnSprite = gridElementDictionary["package_deliverypoint_03"];
				}
				else if(inputComponent.configuration.accepted_types[0] == "Limited")
				{
					returnSprite = gridElementDictionary["package_deliverypoint_01"];
				}
			}
			break;
		case "customs":
			break;
		case "exchange":
			returnSprite = gridElementDictionary["package_exchangepoint_00"];
			break;
		case "painter":
			break;
		case "conditional":
			returnSprite = gridElementDictionary["direction_switch_0000"];
			break;
		case "arrow":
			break;
		case "intersection":
			returnSprite = gridElementDictionary["flowArrow"];
			break;
		case "line":
			break;
		}
		return returnSprite;
	}
	public Sprite GetSprite(String inputKey)
	{
		Sprite returnSprite = null;
		if( gridElementDictionary.ContainsKey(inputKey) ) { returnSprite = gridElementDictionary[inputKey]; }
		return returnSprite;
	}

	public bool IsEditableElement(Vector3 inputMousePosition)
	{
		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputMousePosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);

		bool returnValid = false;

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.transform.position == testPosition && g.behaviorType == GridObjectBehavior.BehaviorTypes.component && g.component.editable == "E")
			{
				returnValid = true;
			}
		}

		return returnValid;	
	}


	public bool IsObjectOfType(Vector3 inputMousePosition, string inputType)
	{
		bool returnIsObjectOfType = false;
		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputMousePosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);


		if( gridObjectLevelPositionDictionary.ContainsKey( testPosition )) 
		{
			if( gridObjectLevelPositionDictionary[ testPosition ].behaviorType == GridObjectBehavior.BehaviorTypes.component &&  gridObjectLevelPositionDictionary[ testPosition ].component.type == inputType)
			{
				returnIsObjectOfType = true;
			}
		}
			
		return returnIsObjectOfType;
	}

	public GridObjectBehavior RetrieveGridObjectOfType(Vector3 inputMousePosition, string inputType)
	{
		GridObjectBehavior returnObject = new GridObjectBehavior();

		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputMousePosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);

		if( gridObjectLevelPositionDictionary.ContainsKey( testPosition )) 
		{
			if( gridObjectLevelPositionDictionary[ testPosition ].behaviorType == GridObjectBehavior.BehaviorTypes.component &&  gridObjectLevelPositionDictionary[ testPosition ].component.type == inputType)
			{
				returnObject = gridObjectLevelPositionDictionary[testPosition];
			}
		}
		return returnObject;
	}

	public GridObjectBehavior[] RetrieveComponentsOfType(string subType = "")
	{
		List<GridObjectBehavior> returnObjects = new List<GridObjectBehavior>();

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.component && (subType == "" || g.component.type == subType )) { returnObjects.Add(g); }
		}

		return returnObjects.ToArray();
	}

	public GridObjectBehavior[] RetrieveTracks()
	{
		List<GridObjectBehavior> returnObjects = new List<GridObjectBehavior>();
		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.behaviorType == GridObjectBehavior.BehaviorTypes.track) { returnObjects.Add(g); }
		}
		return returnObjects.ToArray();
	}

	public GridObjectBehavior RetrieveEditableGridObject(Vector3 inputMousePosition)
	{
		Vector3 testPosition = worldCamera.ScreenToWorldPoint(inputMousePosition);
		testPosition.z = 0;
		testPosition.x = Mathf.RoundToInt(testPosition.x);
		testPosition.y = Mathf.RoundToInt(testPosition.y);

		GridObjectBehavior returnGridObject = new GridObjectBehavior();

		foreach(GridObjectBehavior g in currentLevelObjects)
		{
			if(g.transform.position == testPosition && g.behaviorType == GridObjectBehavior.BehaviorTypes.component && g.component.editable == "E")
			{
				returnGridObject = g;
			}
		}

		return returnGridObject;
	}

	public GridObjectBehavior GetGridObjectByID(int id)
	{
		if( id == 0 ) return null;
		// 2016-11-02 JOSEP: THIS IS NOT A FIX, WE NEED TO FIGURE OUT WHY THIS HAPPENS!!!
		if (gridObjectLevelIDDictionary.ContainsKey(id)) {
			return gridObjectLevelIDDictionary [id];
		} else {
			return null;
		}
	}

	public GridObjectBehavior GetGridObjectByPosition(Vector3 position)
	{
		return gridObjectLevelPositionDictionary[position];
	}

    public bool GridComponentAtPosition(Vector3 position) {
        bool returnBool = false;
        if (gridObjectLevelPositionDictionary.ContainsKey(position))
        {
            returnBool = true;
        }
        return returnBool;
    }

	public bool ExchangeAtPosition(Vector3 position)
	{
		bool returnBool = false;
		if( gridObjectLevelPositionDictionary.ContainsKey(position) )
		{
			if(gridObjectLevelPositionDictionary[position].component.type == "exchange")
			{
				returnBool = true;
			}
		}
		return returnBool;
	}

	public GridObjectBehavior GetGridObjectByMousePosition(Vector3 inputMousePosition)
	{
		Vector3 position = worldCamera.ScreenToWorldPoint(inputMousePosition);
		position.z = 0;
		position.x = Mathf.RoundToInt(position.x);
		position.y = Mathf.RoundToInt(position.y);
		if( gridObjectLevelPositionDictionary.ContainsKey(position) ){
			return gridObjectLevelPositionDictionary[position];
		} else {
			return null;

		}
	}

	public Color GetColorByIndex(int colorIndex)
	{
		return colorDictionary[colorIndex]; 
	}

	public int RetrieveDeliveredToObject(StepData inputStep)
	{
		int targetStepIndex = inputStep.timeStep;
		int returnComponentID = 0;

		Level lvl = GameManager.Instance.GetDataManager().currentLevelData;
		foreach( StepData step in lvl.execution ) 
		{
			if( step.timeStep == targetStepIndex )
			{
				if ((step.componentID != inputStep.componentID) && step.eventType == "E")
				{
					if(step.componentPos == inputStep.componentPos) 
					{
						if(step.componentStatus != null && step.componentStatus.delivered != null)
						{
							if(step.componentStatus.delivered != 0)
							{
								returnComponentID = step.componentID;
							}
						}
					}
				}
			}
		}

		return returnComponentID;
	}

}
