///-----------------------------------------------------------------
///   Class:          UIHistogram
///   Description:    Class for creating a unity ui based histogram
///                   to visualize data
///                   WIP - logic for the individual bars is weird
///   Author:         Boyd Fox                    Date: 1/07/2019
///-----------------------------------------------------------------

using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using System.Linq;

public class UIHistogram : MonoBehaviour {

    int Degrees;
    int Base;

    [SerializeField]
    Text Title;
    [SerializeField]
    Transform BarParent;
    [SerializeField]
    Image BarPrefab;
    [SerializeField]
    List<Image> Bars;
    [SerializeField]
    Image Target;

    public void Init(string title, List<int> values, int target, int degrees = 5, int @base = 0)
    {
        Title.text = title;
        Degrees = degrees;
        Base = @base;
        if (Bars.Count > 0)
            ClearBars();
        CreateBars(values, target);
    }

    void CreateGraph()
    {

    }

    void CreateBars(List<int> values, int target)
    {
        values.Sort();
        int min = 0;
        int max = 0;
        int interval = 0;
        foreach (int i in values)
        {
            if (i > max)
                max = i;
            else if (i < min)
                min = i;
        }
        interval = (max - min) / Degrees;

        int b = 0;
        int e = 0;
        int count = 0;
        int maxCount = 0;
        for (int index = 0; index < Degrees; index++)
        {
            b = min + (interval * index);
            e = min + (interval * (index + 1));
            count = 0;
            foreach (int i in values)
            {
                if (i >= b && i < e)
                {
                    count++;
                    if (count > maxCount)
                        maxCount = count;
                }
            }
            Image image = Instantiate(BarPrefab, BarParent);
            image.fillAmount = Mathf.Max(
                BarParent.GetComponent<HorizontalLayoutGroup>().spacing/100,
                (float)count / (float)maxCount);
            Bars.Add(image);
        }
        
        float offset = target / (float)max;
        float width = Target.rectTransform.parent.gameObject.GetComponent<RectTransform>().rect.width;
        Target.rectTransform.localPosition = new Vector3(offset * width - (width/2f),0);
    }

    void ClearBars()
    {
        foreach (Image image in Bars)
            Destroy(image.gameObject);
        Bars.Clear();
    }

    [ContextMenu("Test")]
    public void Test()
    {
        Init("Test Histogram", new List<int> { 0, 1, 2, 3, 4, 5, 6 }, 4, 4, 0);
    }

    // 0 - 1.5 1.5 - 3 3 - 4.5 4.5 - 6

}
