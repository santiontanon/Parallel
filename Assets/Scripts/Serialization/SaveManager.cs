using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class SaveManager : MonoBehaviour{

    public List<ParallelSave> saves;

    public ParallelSave currentSave;

    public void Init()
    {
        saves = new List<ParallelSave>();
        saves = Serializer.LoadSaves();
        if(saves.Count > 0)
        {
            currentSave = saves[0];
        }
    }

    public ParallelSave GetSave(string s)
    {
        for (int i = 0; i < saves.Count; i++)
        {
            if (saves[i].name == s)
            {
                return saves[i];
            }
        }
        return null;
    }

    public void LoadSave(string s)
    {
        ParallelSave save = GetSave(s);
        if (save != null)
        {
            currentSave = save;
            GameManager.Instance.GetScoreManager().LoadScores();
            GameManager.Instance.GetDataManager().GetLevels();
            //GameManager.Instance.GetScoreManager().LoadScoresPCGScores();???
            //GameManager.Instance.GetDataManager().GetPCGLevels();???
        }
        else
        {
            Debug.Log("Unable to load save: " + s + ". File does not exist.");
            LevelScore[] scores = new LevelScore[0];
            LevelScore[] pcgScores = new LevelScore[0];
            List<string> levels = new List<string>();
            NewSave(s, scores, pcgScores, levels);
        }
    }

    public void UpdateSave(string s, LevelScore[] scores, LevelScore[] pcgScores, List<string> pcgLevels)
    {
        if(s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                UpdateScores(s, scores);
                UpdatePCGLevels(s, pcgLevels, pcgScores);
            }
            else
            {
                NewSave(s, scores, pcgScores, pcgLevels);
            }
        }
    }

    public void UpdateScores(string s, LevelScore[] scores)
    {
        if (s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                save.scores = scores;
                Serializer.SerializeData(save);
            }
            else
            {
                Debug.LogError("No current save, can't update scores");
            }
        }
    }

    public void UpdatePCGLevels(string s, List<string> pcgLevels, LevelScore[] pcgScores)
    {
        if (s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                save.pcgScores = pcgScores;
                save.pcgLevels = pcgLevels;
                Serializer.SerializeData(save);
            }
            else
            {
                Debug.LogError("No current save, can't update pcg levels");
            }
        }
    }

    public void NewSave(string s, LevelScore[] scores, LevelScore[] pcgScores, List<string> levels)
    {
        if(s != "")
        {
            ParallelSave save = new ParallelSave();
            save.name = s;
            save.scores = scores;
            save.pcgScores = pcgScores;
            save.pcgLevels = levels;
            Serializer.SerializeData(save);
        }
    }

    public void AddSave(ParallelSave save)
    {
        if(save.name != "")
        {
            if (saves == null)
                saves = new List<ParallelSave>();
            saves.Add(save);
            Serializer.SerializeData(save);
        }
    }

}
