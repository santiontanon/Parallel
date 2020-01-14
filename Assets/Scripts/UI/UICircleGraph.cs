///-----------------------------------------------------------------
///   Class:          UICircleGraph
///   Description:    Class for creating a unity ui based circle 
///                   graphs to visualize data
///                   WIP
///   Author:         Boyd Fox                    Date: 1/10/2019
///-----------------------------------------------------------------

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class UICircleGraph : MonoBehaviour {

    [SerializeField]
    Text Title;
    [SerializeField]
    Transform CircleParent;
    [SerializeField]
    Image CirclePrefab;
    [SerializeField]
    List<Image> Circles;

    void Init(string title, List<float> values)
    {
        CreateGraph(values);
    }

    void CreateGraph(List<float> values)
    {
        float total = 0f;
        foreach(float f in values)
            total += f;

        float current = 0f;
        for(int index = 0; index < values.Count; index++)
        {
            Image circle = Instantiate(CirclePrefab, CircleParent);
            circle.fillAmount = (values[index] / total);
            circle.color = RandomColor();
            circle.rectTransform.eulerAngles = new Vector3(0f, 0f, 360 * current);
            current += values[index] / total;
            Circles.Add(circle);
        }
    }

    void ClearGraph()
    {
        foreach(Image i in Circles)
        {
            Destroy(i.gameObject);
        }
        Circles.Clear();
    }

    Color RandomColor()
    {
        return new Color(Random.Range(0f, 1f), Random.Range(0f, 1f), Random.Range(0f, 1f));
    }

    [ContextMenu("Test")]
    public void Test()
    {
        Init("Test Histogram", new List<float> { 0.1f, 0.2f, 0.35f, 0.05f, 0.09f, 0.01f, 0.2f });
    }
}
