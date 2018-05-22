using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class ParallelSave {

    public static ParallelSave current;

    public LevelScore[] scores;
    public List<LevelScore> pcgScores;
    public List<string> pcgLevels;
    public string name;

    public void UpdateName(string s)
    {

    }

    public int AddNewPCGLevel(string level)
    {
        if (pcgLevels == null)
            pcgLevels = new List<string>();
        if (pcgScores == null)
            pcgScores = new List<LevelScore>();
        pcgLevels.Add(level);
        pcgScores.Add(new LevelScore());
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
}
