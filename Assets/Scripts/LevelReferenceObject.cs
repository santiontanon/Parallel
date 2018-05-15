using UnityEngine;
using System.Collections;

using System.Collections.Generic;

[System.Serializable]
public class LevelReference
{
    public int id = -1;
    public string user = "default";
    public string version = "default";
    public string date = "";
    public string presurvey = "";
    public string postsurvey = "";
    [SerializeField] public LevelReferenceContainer levels;
}

[System.Serializable]
public class LevelReferenceContainer
{
    [SerializeField] public LevelReferenceObject[] required;
    [SerializeField] public LevelReferenceObject[] optional;
    [SerializeField] public LevelReferenceObject[] previous;
    [SerializeField] public List<LevelReferenceObject> pcg;
}

[System.Serializable]
public class LevelReferenceObject
{
    public string file = "level01";
    public string title = "";
    //in-game behaviors that aren't given by josep's file
    public int completionRank = 0;
    public int levelId = -1;
    public void SetLevelCompletionRank(int inputRank)
    { completionRank = inputRank; }
    public int GetLevelCompletionRank() { return completionRank; }
}