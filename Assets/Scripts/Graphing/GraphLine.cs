using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GraphLine : MonoBehaviour
{
    [SerializeField]
    public LineRenderer Line;
    [SerializeField]
    public float HeadSize = 50f;
    [SerializeField]
    public float Width;

    [SerializeField]
    public GraphNode Begin;
    [SerializeField]
    public GraphNode End;

    void Awake()
    {
        Line = this.GetComponent<LineRenderer>();
        Line.alignment = LineAlignment.TransformZ;

        while (Line.positionCount != 4)
            if (Line.positionCount > 4)
                Line.positionCount--;
            else
                Line.positionCount++;
    }

    void Update()
    {
        float AdaptiveSize = (float)(HeadSize / Vector3.Distance(Begin.transform.position, End.transform.position));

        Line.widthCurve = new AnimationCurve(
             new Keyframe(0, Width)
             , new Keyframe(0.999f - AdaptiveSize, Width)
             , new Keyframe(1 - AdaptiveSize, Mathf.Max(Width*2,20))
             , new Keyframe(1, 0f));
        Line.SetPositions(new Vector3[] {
              Begin.transform.position
              , Vector3.Lerp(Begin.transform.position, End.transform.position, 0.999f - AdaptiveSize)
              , Vector3.Lerp(Begin.transform.position, End.transform.position, 1 - AdaptiveSize)
              , End.transform.position });
    }
}

/*
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

*/
