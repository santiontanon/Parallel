using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GraphLine : MonoBehaviour
{
    [SerializeField]
    public LineRenderer Line;

    [SerializeField]
    public GraphNode Begin;
    [SerializeField]
    public GraphNode End;

    void Awake()
    {
        Line = this.GetComponent<LineRenderer>();
        Line.alignment = LineAlignment.TransformZ;

        while(Line.positionCount != 2)
            if (Line.positionCount > 2)
                Line.positionCount--;
            else
                Line.positionCount++;
    }

    void Update()
    {
        if (Begin != null)
            Line.SetPosition(0, Begin.transform.position);
        if (End != null)
            Line.SetPosition(1, End.transform.position);
    }
}
