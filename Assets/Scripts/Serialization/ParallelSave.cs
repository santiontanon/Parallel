using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class ParallelSave {

    public static ParallelSave current;

    public LevelScore[] scores;
    public string name;

    public void UpdateName(string s)
    {

    }

    public void UpdateScore(LevelScore score, int index)
    {
        if (scores.Length <= index)
        {
            LevelScore[] oldScores = scores;
            scores = new LevelScore[index + 1];
            for (int i = 0; i < oldScores.Length; i++)
            {
                scores[i] = oldScores[i];
            }
        }
    }
}
