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
        if(save != null)
        {
            currentSave = save;
            GameManager.Instance.GetScoreManager().LoadScores();
            GameManager.Instance.GetDataManager().GetLevels();
        }
        else
        {
            Debug.Log("Unable to load save: " + s + ". File does not exist.");
            LevelScore[] scores = new LevelScore[0];
            NewSave(scores, s);
        }
    }

    public void UpdateSave(LevelScore[] scores, string s)
    {
        if(s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                save.scores = scores;
                Serializer.SerializeData(save);
            }
            else
            {
                NewSave(scores, s);
            }
        }
    }

    public void NewSave(LevelScore[] scores, string s)
    {
        if(s != "")
        {
            ParallelSave save = new ParallelSave();
            save.name = s;
            save.scores = scores;
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
