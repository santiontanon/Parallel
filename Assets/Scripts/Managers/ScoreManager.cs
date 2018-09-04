using UnityEngine;
using System.Collections;

public class ScoreManager : MonoBehaviour {

    /// <summary>
    /// LevelScore array for level scores
    /// Loaded and saved via SaveManager
    /// </summary>
    [SerializeField]
    LevelScore[] scores;

    [SerializeField]
    LevelScore[] pcgScores;

    /// <summary>
    /// Array of level solutions
    /// 0 = allowed submit attempts
    /// 1 = allowed solution steps
    /// 2 = allowed number of objects to place
    /// </summary>
    [SerializeField]
    LevelScore[] solutions;

    public void Init()
    {
        LoadScores();
    }

    /// <summary>
    /// Clears score data and save the now empty score
    /// </summary>
    public void ClearScores()
    {
        foreach(LevelScore score in scores)
        {
            score.completed = false;
            score.attemptCount = -1;
            score.index = -1;
            score.stepCount = -1;
        }
        SaveScores();
    }

    /// <summary>
    /// Saves the current level scores to player prefs
    /// </summary>
	public void SaveScores()
    {
        GameManager.Instance.GetSaveManager().UpdateSave(GameManager.Instance.GetSaveManager().currentSave.name, scores, new System.Collections.Generic.List<LevelScore>(pcgScores), GameManager.Instance.GetSaveManager().currentSave.pcgLevels);
    }

    /// <summary>
    /// Loads the current level scores from player prefs
    /// </summary>
    public void LoadScores()
    {
        scores = GameManager.Instance.GetSaveManager().currentSave.scores;
        for (int i = 0; i < scores.Length; i++)
        {
            if(scores[i] == null)
            {
                scores[i] = new LevelScore();
            }
        }
        if(GameManager.Instance.GetSaveManager().currentSave.pcgScores != null)
        {
            pcgScores = GameManager.Instance.GetSaveManager().currentSave.pcgScores.ToArray();
        }
        if (pcgScores != null)
        {
            for (int i = 0; i < pcgScores.Length; i++)
            {
                if (pcgScores[i] == null)
                {
                    pcgScores[i] = new LevelScore();
                }
            }
        }
        else
        {
            pcgScores = new LevelScore[0];
        }
    }

    /// <summary>
    /// Retrieves the score for a specific level
    /// </summary>
    /// <param name="index">index of the target level</param>
    public LevelScore GetScore(int index)
    {
        if(scores.Length > index)
        {
            return scores[index];
        }
        else
        {
            Debug.LogWarning("Requested score for Level Id " + index.ToString() + " does not exist");
            return null;
        }
    }

    /// <summary>
    /// Calculates an int score based on a LevelScore
    /// Compares data to a solution
    /// </summary>
    /// <param name="score"></param>
    /// <returns></returns>
    public int GetCalculatedScore(LevelScore score)
    {
        int _score = 0;
        if (score.completed == true)
        {
            _score++;
            if (score.attemptCount <= solutions[score.index].attemptCount)
            {
                _score++;
            }
            if (score.stepCount <= solutions[score.index].stepCount)
            {
                _score++;
            }
        }
        return _score;
    }

    public LevelScore GetSolutionInfo(LevelScore score, out bool success)
    {
        if (solutions.Length > score.index && score.index >= 0)
        {
            success = true;
            return solutions[score.index];
        }
        else
        {
            success = false;
            return score;
        }
    }

    /// <summary>
    /// Gets a LevelScore object from scores and then
    /// calls the main GetCalculatedScore function
    /// </summary>
    /// <param name="index"></param>
    /// <returns></returns>
    public int GetCalculatedScore(int index)
    {
        if (scores.Length > index && index >= 0)
        {
            return GetCalculatedScore(scores[index]);
        }
        else
        {
            if(index >= 0)
            {
                AddNewScores(index);
                return GetCalculatedScore(scores[index]);
            }
            else
            {
                return 0;
            }
        }
    }

    /// <summary>
    /// Takes a new score and compares it to the old score 
    /// to determine if the new data should be saved
    /// </summary>
    /// <param name="score">LevelScore object that holds data for scoring</param>
    public void ScoreLevel(LevelScore score)
    {
        if(solutions.Length > score.index)
        {
            int _score = GetCalculatedScore(score);
            if(scores.Length <= score.index)
            {
                AddNewScores(score.index);
            }
            if(GetCalculatedScore(score.index) < _score)
            {
                scores[score.index] = score;
                SaveScores();
                Debug.Log("New level " + score.index + " score is " + _score);
            }
            else
            {
                Debug.Log("Old level " + score.index + " score is higher than " + _score + ", retaining");
            }
        }
        else
        {
            Debug.LogError("Solution Not Found, please setup on ScoreManager object");
        }
    }

    void AddNewScores(int index)
    {
        LevelScore[] oldScores = scores;
        scores = new LevelScore[index + 1];
        for (int i = 0; i < oldScores.Length; i++)
        {
            scores[oldScores[i].index] = oldScores[i];
        }
        for (int i = 0; i < scores.Length; i++)
        {
            if(scores[i] == null)
            {
                scores[i] = new LevelScore();
            }
        }
        SaveScores();
    }

    [ContextMenu("Setup Solution Default Values")]
    void SetupSolutions()
    {
        for(int i = 0; i < solutions.Length; i++)
        {
            LevelReferenceObject l = GameManager.Instance.GetDataManager().GetLevelById(i);
            if (l != null)
            {
                if (solutions[i].attemptCount < 2)
                {
                    solutions[i].attemptCount = 2;
                }
                if (solutions[i].completed == false)
                {
                    solutions[i].completed = true;
                }
                if (solutions[i].index != i)
                {
                    solutions[i].index = i;
                }
                if(solutions[i].stepCount != GameManager.Instance.GetDataManager().GetLevelEfficiency(l.file))
                {
                    solutions[i].stepCount = (int)GameManager.Instance.GetDataManager().GetLevelEfficiency(l.file);
                }
            }
        }
    }
}
