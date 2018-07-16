using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using UnityEngine.UI;

public class Delivery_GridObjectBehavior : GridObjectBehavior {

	int deliveries = 0;
	int goalDeliveries = 0;

	public class DeliveryFractionPopUp
	{
		public int numerator, denominator = 0;
		TextMesh fractionText;
		GameObject container;
		public DeliveryFractionPopUp(Transform parent, int inputNumerator, int inputDenominator)
		{
			denominator = inputDenominator;
			numerator = inputNumerator;

            if (denominator != -1)
            {
                container = new GameObject();
                container.transform.SetParent(parent.parent);
                container.name = "DeliveryPopUp_" + parent.name;
                SpriteRenderer containerSprite = container.AddComponent<SpriteRenderer>();
                containerSprite.sprite = GameManager.Instance.GetGridManager().GetSprite("delivery_bubble");
                containerSprite.sortingOrder = Constants.ComponentSortingOrder.basicComponents;
                GridManager gridManagerInstance = GameManager.Instance.GetGridManager();

                Vector3 position = parent.position;
                float targetScale = gridManagerInstance.worldCamera.orthographicSize / 5.7f;
                targetScale = Mathf.Round(targetScale);

                //above stuff isn't working right. Just being 1 for now.
                targetScale = 1f;

                if (!gridManagerInstance.GridComponentAtPosition(position + Vector3.up * targetScale) && gridManagerInstance.worldCamera.WorldToViewportPoint(position + Vector3.up * targetScale).y < 0.85f)
                {
                    container.transform.position = parent.position + new Vector3(0f, 1f * targetScale, 0f);
                }
                else if (!gridManagerInstance.GridComponentAtPosition(position + Vector3.down * targetScale) && gridManagerInstance.worldCamera.WorldToViewportPoint(position + Vector3.down * targetScale).y > 0.15f)
                {
                    container.transform.position = parent.position + new Vector3(0f, -1f * targetScale, 0f);
                    containerSprite.flipY = true;
                }
                else if (!gridManagerInstance.GridComponentAtPosition(position + Vector3.left * targetScale) && gridManagerInstance.worldCamera.WorldToViewportPoint(position + Vector3.left * targetScale).x > 0.15f)
                {
                    container.transform.position = parent.position + new Vector3(1f * targetScale, 0f, 0f);
                    container.transform.Rotate(0f, 0f, -90f); //point toward right
                }
                else if (!gridManagerInstance.GridComponentAtPosition(position + Vector3.right * targetScale) && gridManagerInstance.worldCamera.WorldToViewportPoint(position + Vector3.left * targetScale).x < 0.85f)
                {
                    container.transform.position = parent.position + new Vector3(-1f * targetScale, 0f, 0f);
                    container.transform.Rotate(0f, 0f, 90f);
                }
                else
                {
                    container.transform.position = parent.position + new Vector3(0f, 1f * targetScale, 0f);
                }

                container.transform.localScale = Vector3.one * targetScale;
                /*
				if(parent.transform.position.y > (GameManager.Instance.GetLevelHeight()/2)) 
				{
					container.transform.position = parent.position + new Vector3(0f, -1f, 0f);
					containerSprite.flipY = true;
				}
				else 
				{
					container.transform.position = parent.position + new Vector3(0f, 1f, 0f);
				}
                */
				GameObject fractionObject = new GameObject();

				fractionObject.transform.position = container.transform.position - Vector3.forward;
				fractionObject.transform.SetParent( container.transform );
                fractionObject.transform.localScale = Vector3.one;
				fractionText = fractionObject.AddComponent<TextMesh>();
				fractionText.GetComponent<MeshRenderer>().sortingOrder = Constants.ComponentSortingOrder.basicComponents + 1;
				fractionText.characterSize = 0.2f;
				fractionText.anchor = TextAnchor.MiddleCenter;
				fractionText.color = new Color(0.1f,0.1f,0.1f);
				fractionText.text = inputNumerator.ToString() + "/" + inputDenominator.ToString();
			}
		}

		public void IncrementNumerator(int incrementBy = 1)
		{
			numerator+=incrementBy;
			if(denominator==-1) return;
			fractionText.text = numerator.ToString() + "/" + denominator.ToString();
			if(container.GetComponent<iTween>()) return;
			iTween.ScaleFrom( container, Vector3.one * 1.8f, 2f );
			//Destroy(g, 2f);	
		}
		public void Reset()
		{
			numerator = 0;
			if(denominator==-1) return;
			fractionText.text = numerator.ToString() + "/" + denominator.ToString();
		}

        public void SetTo(int i)
        {
            numerator = i;
            if (denominator == -1) return;
            fractionText.text = numerator.ToString() + "/" + denominator.ToString();
        }

	}
	public DeliveryFractionPopUp deliveryPopup;

	public override void InitializeGridComponentBehavior()
	{
		base.InitializeGridComponentBehavior();
		if(component.configuration.denominator != null)
		{
			deliveryPopup = new DeliveryFractionPopUp(transform, 0, component.configuration.denominator);
		}
		else 
		{
			Debug.Log("Null configuration for grid object " + component.id.ToString());
			deliveryPopup = new DeliveryFractionPopUp(transform, 0, 0);
		}

	}

	public override void BeginInteraction()
	{
		base.BeginInteraction();
	}

	public override void ContinueInteraction()
	{
		base.ContinueInteraction();
	}

	public override void EndInteraction()
	{
		base.EndInteraction();
	}

	public override void OnHoverBehavior()
	{
		if(component.type != "delivery") return; 

		SetHighlight(true);
		List<GridObjectBehavior> packages = GameManager.Instance.GetGridManager().GetGridComponentsOfType("pickup");
		if( component.configuration.accepted_types.Length == 0 )
		{
			foreach(GridObjectBehavior g in packages) 
			{
				if( component.configuration.accepted_colors.Length == 0 ){ g.SetHighlight(true); }
				else 
				{
					foreach(int colorIndex in component.configuration.accepted_colors)
					{
						if(colorIndex == g.component.configuration.color)
						{
							g.SetHighlight(true);  
						}
					}
				}
			}
		}
		else 
		{
			foreach(string accepted_type in component.configuration.accepted_types)
			{
				foreach(GridObjectBehavior g in packages) 
				{ 
					if(g.component.configuration.type == accepted_type || accepted_type == "Unconditional") 
					{
						if( component.configuration.accepted_colors.Length == 0 ){ g.SetHighlight(true); }
						else 
						{
							foreach(int colorIndex in component.configuration.accepted_colors)
							{
								Debug.Log(colorIndex + "vs" + g.component.configuration.color);
								if(colorIndex == g.component.configuration.color) 
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

	public override void EndHoverBehavior()
	{
		if(component.type != "delivery") return;

		SetHighlight(false);
		List<GridObjectBehavior> packages = GameManager.Instance.GetGridManager().GetGridComponentsOfType("pickup");
		if( component.configuration.accepted_types.Length == 0 )
		{
			foreach(GridObjectBehavior g in packages) 
			{
				if( component.configuration.accepted_colors.Length == 0 ){ g.SetHighlight(false); }
				else 
				{
					foreach(int colorIndex in component.configuration.accepted_colors)
					{
						if(colorIndex == g.component.configuration.color)
						{
							g.SetHighlight(false);  
						}
					}
				}
			}
		}
		else 
		{
			foreach(string accepted_type in component.configuration.accepted_types)
			{
				foreach(GridObjectBehavior g in packages) 
				{ 
					if(g.component.configuration.type == accepted_type || accepted_type == "Unconditional") 
					{ 
						if( component.configuration.accepted_colors.Length == 0 ){ g.SetHighlight(false); }
						else 
						{
							foreach(int colorIndex in component.configuration.accepted_colors)
							{
								Debug.Log(colorIndex + "vs" + g.component.configuration.color);
								if(colorIndex == g.component.configuration.color) 
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

	public override void ResetPosition()
	{
        Debug.Log("Delivery Reset");
		iTween.Stop(gameObject);
		deliveryPopup.Reset();
		transform.localScale = Vector3.one;
		GetComponent<SpriteRenderer>().material.color = Color.white;
	}

    public override void ReturnToStep(TimeStepData timeStep)
    {
        deliveryPopup.SetTo(timeStep.GetDeliveryPoint(component.id).deliveries);
    }

    public override float DoStep(StepData inputStep, Dictionary<int, List<StepData>> dictionary = null)
	{
		base.DoStep(inputStep);
		if(inputStep.componentStatus.delivered != null)
		{
		//	deliveryPopup.IncrementNumerator( inputStep.componentStatus.delivered );
		}
        if (inputStep.eventType == "F")
        {
            GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByID(inputStep.componentID);
            g.ErrorBehavior(Color.black);
        }
        return 0;
    }

	public override void SuccessBehavior(Color successColor)
	{
        if (GetComponent<iTween>()) { return; }
		iTween.ScaleFrom( gameObject, Vector3.one*1.4f, 1.5f );
        iTween.ColorFrom(gameObject, Color.green, 1.5f);
    }
    public override void ErrorBehavior(Color errorColor)
	{
        iTween.Stop(this.gameObject);
        iTween.ScaleFrom( gameObject, Vector3.zero, 1.5f );
        iTween.ColorFrom(gameObject, Color.red, 1.5f);
	}
}
