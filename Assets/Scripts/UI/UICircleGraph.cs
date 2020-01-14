using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class UIPlayerModel : MonoBehaviour {

    Transform CircleParent;
    Image CirclePrefab;
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
        foreach(float f in values)
        {
            Image circle = Instantiate(CirclePrefab, CircleParent);
            circle.fillAmount = (f / total);
            circle.rectTransform.eulerAngles = new Vector3(0f, 0f, current);
            current += f / total;
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
}
