using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class ParallelSave {

    public static ParallelSave current;

    public LevelScore[] scores;
    public LevelScore[] pcgScores;
    public List<string> pcgLevels;
    public string name;

    public void UpdateName(string s)
    {

    }

    public int AddNewPCGLevel(string level)
    {
        pcgLevels.Add(level);
        LevelReferenceObject lrObj = new LevelReferenceObject();
        lrObj.file = "P" + pcgLevels.Count;
        lrObj.title = lrObj.file;
        return pcgLevels.IndexOf(level);
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

    public void UpdatePCGScore(LevelScore score, int index)
    {
        if (pcgScores.Length <= index)
        {
            LevelScore[] oldScores = pcgScores;
            pcgScores = new LevelScore[index + 1];
            for (int i = 0; i < oldScores.Length; i++)
            {
                pcgScores[i] = oldScores[i];
            }
        }
    }
}
