using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Signal_GridObjectBehavior : GridObjectBehavior {

	[Header("Signal")]
	#region signal type
	public LineRenderer lineRenderer;
	#endregion

	int lineRendererSegments = 10;

	Vector3[] lineRendererPoints;


	public override void InitializeGridComponentBehavior(){ 
		base.InitializeGridComponentBehavior();
		if (component.type == "signal") {
			lineRenderer.SetVertexCount (lineRendererSegments + 1);
			lineRendererPoints = new Vector3[lineRendererSegments + 1];
			for (int i = 0; i <= lineRendererSegments; i++) {
				lineRendererPoints [i] = new Vector3(-1000,-1000,-1000);// transform.position;
			}
			UpdateBezier (transform.position, transform.position);
			lineRenderer.SetColors(Constants.ComponentLinkBezierCurves.LinkColor, Constants.ComponentLinkBezierCurves.LinkColor);
            
            // IF DYNAMIC COLORS FOR COMPONENTS ...
            /*if (lineRenderer)
            {
                Color lineColor = Constants.LinkColors.fullColorSet[Random.Range(0, Constants.LinkColors.fullColorSet.Count)];
                lineColor.a = 1f;
                lineRenderer.SetColors(lineColor, lineColor);
            }*/
        }

	 }


	public override void BeginInteraction()
	{
		lineRenderer.SetPositions( new Vector3[]{ transform.position, transform.position } );	
		lineRenderer.sortingOrder = Constants.ComponentSortingOrder.connectionOverlay + 1;
		UpdateBezier (transform.position, transform.position);
	}


	private void UpdateBezier(Vector3 p0, Vector3 p3){
		float lineZ = 0.0f;
		float extra_y = 0.0f;
		float extra_x = 0.0f;
		//if (Mathf.Abs (p0.x - p3.x) + Mathf.Abs (p0.y - p3.y) < 2.0f) {
		if((p3-p0).magnitude > 0.1){
			if (Mathf.Abs (p0.x - p3.x) > Mathf.Abs (p0.y - p3.y)) {
				extra_y = Constants.ComponentLinkBezierCurves.ExtraBias;
			} else {
				extra_x = Constants.ComponentLinkBezierCurves.ExtraBias;
			}
		}
		if(p0.y<GameManager.Instance.GetLevelWidth()/2) extra_y *= -1;
		if(p0.x<GameManager.Instance.GetLevelWidth()/2) extra_x *= -1;


		Vector3 p1 = new Vector3(p0.x+extra_x, (p0.y+p3.y)/2.0f+extra_y, lineZ);
		Vector3 p2 = new Vector3((p0.x+p3.x)/2.0f+extra_x, p3.y+extra_y, lineZ);
		for (int j = 0; j <= lineRendererSegments; j++)
		{
			lineRendererPoints[j] = GetBezierPoint(((float)j / lineRendererSegments), p0, p1, p2, p3, lineZ);
		}
		lineRenderer.SetPositions( lineRendererPoints );	
	}
		

	public override void ContinueInteraction()
	{
		if(behaviorType == BehaviorTypes.component)
		{
			switch( component.type ) 
			{
				case "signal":
					Vector3 interactToPosition = levelCamera.ScreenToWorldPoint(Input.mousePosition);
					interactToPosition.z = 0;
					if(lineRenderer!=null){
						UpdateBezier(transform.position,interactToPosition);
					}
				break;
			}
		}
	}

	private Vector2 GetBezierPoint(float t, Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, float pZ)
    {
        float cx = 3 * (p1.x - p0.x);
        float cy = 3 * (p1.y - p0.y);
        float bx = 3 * (p2.x - p1.x) - cx;
        float by = 3 * (p2.y - p1.y) - cy;
        float ax = p3.x - p0.x - cx - bx;
        float ay = p3.y - p0.y - cy - by;
        float Cube = t * t * t;
        float Square = t * t;
 
        float resX = (ax * Cube) + (bx * Square) + (cx * t) + p0.x;
        float resY = (ay * Cube) + (by * Square) + (cy * t) + p0.y;
 
        return new Vector3(resX, resY, pZ);
    }

	public override void EndInteraction()
	{
		if(behaviorType == BehaviorTypes.component)
		{
			switch( component.type ) 
			{
			case "signal":
				UpdateBezier (transform.position, transform.position);
			break;
			}
		}
	}

	public override void OnHoverBehavior()
	{
		if(component!=null)
		{
			switch(component.type)
			{
			case "signal":
				SetHighlight(true);
				if(component.configuration.link != -1)
				{
					GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).SetHighlight(true);
				//	UpdateBezier(transform.position,GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).transform.position);
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
			case "signal":
				SetHighlight(false);
				if(component.configuration.link != -1)
				{
					GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link ).SetHighlight(false);
				//	UpdateBezier (transform.position, transform.position);
				}
			break;
			}

		}
	}


	public override float DoStep(StepData inputStep, Dictionary<int, List<StepData>> dictionary = null)
	{
		if(behaviorType==BehaviorTypes.component && component!=null){
			switch(component.type.ToLower())
			{
			case "signal":
				if( inputStep.eventType == "E" )
				{
					if( inputStep.componentStatus != null ) 
					{
						if( inputStep.componentStatus.passed != null )
						{
							if (inputStep.componentStatus.passed >= 1)
							{
								component.configuration.passed = inputStep.componentStatus.passed;
								GetComponent<SpriteRenderer>().sprite = GameManager.Instance.GetGridManager().GetSprite( component );
								iTween.ScaleFrom( gameObject, iTween.Hash( 
									"scale", Vector3.one*1.5f,
									"time",0.5f,
									"oncomplete", "ResetSprite"
								));
								component.configuration.passed = 0;
							}
						}
					}

					GridObjectBehavior linkedObject = GameManager.Instance.GetGridManager().GetGridObjectByID( component.configuration.link );
					if( linkedObject !=null && linkedObject.component != null && linkedObject.component.type == "conditional" )
					{
						linkedObject.BeginInteraction();
					}

				}
				break;
			}
		}
        return 0;
	}

	public override void SetHighlight(bool isEnabled)
	{
		base.SetHighlight(isEnabled);

        if (isEnabled && component.configuration.link != -1)
        {
            GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByID(component.configuration.link);
            if(g != null)
            {
                g.SetHighlight(isEnabled);
                //IF CONSTANT COLORS FOR COMPONENTS...
                /**/
                if (Constants.ComponentLinkColor.componentLinkColors.ContainsKey(g.component.type))
                {
                    Debug.Log(g.component.type + "Should have a colored line.");
                    if (lineRenderer)
                    {
                        //int randomLineIndex = Random.Range(0, Constants.ComponentLinkColor.componentLinkColors[g.component.type].Count);
                        int randomLineIndex = component.id % Constants.ComponentLinkColor.componentLinkColors[g.component.type].Count;
                        Color lineColor = Constants.ComponentLinkColor.componentLinkColors[g.component.type][randomLineIndex];// * g.lineColorVariant;
                        lineColor.a = 1f;
                        lineRenderer.SetColors(lineColor, lineColor);
                        lineRenderer.sortingOrder = Constants.ComponentSortingOrder.connectionOverlay + 1;
                    }
                }
                else
                {
                    if (lineRenderer) lineRenderer.SetColors(Constants.ComponentLinkColor.componentLinkColors["default"][0], Constants.ComponentLinkColor.componentLinkColors["default"][0]);
                }
                /**/

                UpdateBezier(transform.position, g.transform.position);
            }
        }
        else if (!isEnabled)
        {
            if (component.configuration.link != -1)
            {
                GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByID(component.configuration.link);
                if(g != null)
                    g.SetHighlight(isEnabled);
            }
            UpdateBezier(transform.position, transform.position);
		}
	}
}
