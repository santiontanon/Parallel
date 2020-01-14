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
    RectTransform TargetIndicator;
    [SerializeField]
    Text TargetText;

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
        float min = 0;
        float max = 0;
        float interval = 0;
        foreach (int i in values)
        {
            if (i > max)
                max = i;
            else if (i < min)
                min = i;
        }
        interval = (max - min) / Degrees;

        float start = 0;
        float end = 0;
        int count = 0;
        int maxCount = 0;
        List<int> counts = new List<int>();
        for (int index = 0; index < Degrees; index++)
        {
            start = min + (interval * index);
            end = min + (interval * (index + 1));
            count = 0;
            foreach (int i in values)
            {
                if (i >= start && i < end)
                {
                    count++;
                    if (count > maxCount)
                        maxCount = count;
                }
            }
            counts.Add(count);
        }
        for (int index = 0; index < Degrees; index++)
        {
            Image image = Instantiate(BarPrefab, BarParent);
            image.fillAmount = Mathf.Max(
                BarParent.GetComponent<HorizontalLayoutGroup>().spacing / 100,
                (float)counts[index] / (float)maxCount);
            Bars.Add(image);
        }

        float offset = target / (float)max;
        float width = TargetIndicator.parent.gameObject.GetComponent<RectTransform>().rect.width;
        TargetIndicator.localPosition = new Vector3(offset * width - (width/2f),0);
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
        Init("Test Histogram", new List<int> { 4, 4, 2, 12, 12, 2, 12, 8, 11, 6, 11, 2, 8, 11, 1, 6, 9, 4, 2, 11, 10, 12, 11, 8, 8 }, 7, 5, 0);
    }

    // 0 - 1.5 1.5 - 3 3 - 4.5 4.5 - 6

}
