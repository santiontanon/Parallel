﻿using UnityEngine;
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
        LoadSave("default");
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
        Debug.Log("LoadSave");
        if (s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                currentSave = save;
                GameManager.Instance.GetScoreManager().LoadScores();
                GameManager.Instance.GetDataManager().GetLevels();
                if (currentSave.pcgLevels == null)
                {
                    currentSave.pcgLevels = new List<string>();
                }
                GameManager.Instance.GetDataManager().GetPCGLevels(currentSave.pcgLevels);
            }
            else
            {
                Debug.Log("Unable to load save: " + s + ". File does not exist.");
                LevelScore[] scores = new LevelScore[0];
                List<LevelScore> pcgScores = new List<LevelScore>();
                List<string> pcgLevels = new List<string>();
                Debug.Log("Time for a new save");
                currentSave = NewSave(s, scores, pcgScores, pcgLevels);
                Debug.Log("Save created");
                GameManager.Instance.GetScoreManager().LoadScores();
                GameManager.Instance.GetDataManager().GetLevels();
                GameManager.Instance.GetDataManager().GetPCGLevels(currentSave.pcgLevels);
            }
        }
    }

    public void UpdateSave()
    {
        Debug.Log("UpdateSave");
        UpdateSave(currentSave.name, currentSave.scores, currentSave.pcgScores, currentSave.pcgLevels);
    }

    public void UpdateSave(string s, LevelScore[] scores, List<LevelScore> pcgScores, List<string> pcgLevels)
    {
        if(s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                save.scores = scores;
                save.pcgScores = pcgScores;
                try { Serializer.SerializeData(save); }
                catch (System.Exception e) { Debug.LogError("UNABLE TO SAVE SCORES: " + e.Message); }
            }
            else
            {
                Debug.Log("Save does not exist, creating new save");
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
                try { Serializer.SerializeData(save); }
                catch (System.Exception e) { Debug.LogError("UNABLE TO SAVE SCORES: " + e.Message); }
            }
            else
            {
                Debug.LogError("No current save, can't update scores");
            }
        }
    }

    public void UpdatePCGLevels(string s, List<string> pcgLevels, List<LevelScore> pcgScores)
    {
        if (s != "")
        {
            ParallelSave save = GetSave(s);
            if (save != null)
            {
                save.pcgScores = pcgScores;
                save.pcgLevels = pcgLevels;
                try { Serializer.SerializeData(save); }
                catch (System.Exception e) { Debug.LogError("UNABLE TO UPDATE PCG LEVEL SAVE: " + e.Message); }
            }
            else
            {
                Debug.LogError("No current save, can't update pcg levels");
            }
        }
    }

    public ParallelSave NewSave(string s, LevelScore[] scores, List<LevelScore> pcgScores, List<string> levels)
    {
        Debug.Log("NewSave");
        if(s != "")
        {
            ParallelSave save = new ParallelSave();
            save.name = s;
            save.scores = scores;
            save.pcgScores = pcgScores;
            save.pcgLevels = levels;
            Serializer.SerializeData(save);
            return save;
        }
        else
        {
            return null;
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
