﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class VersionInfo : MonoBehaviour
{

    public Text VersionText;
    public Text PathText;

    public void Start()
    {
        if (GameManager.Instance.currentGameMode == GameManager.GameMode.Demo)
        {
            VersionText.text = "Demo Build v1.3." + System.IO.File.ReadAllText(Application.dataPath + "/version.txt");
        }
        else if (GameManager.Instance.currentGameMode == GameManager.GameMode.Class)
        {
            VersionText.text = "Class Build v1.3." + System.IO.File.ReadAllText(Application.dataPath + "/version.txt");
        }
        else if (GameManager.Instance.currentGameMode == GameManager.GameMode.Study)
        {
            VersionText.text = "Study Build v1.3." + System.IO.File.ReadAllText(Application.dataPath + "/version.txt");

        }
        else if (GameManager.Instance.currentGameMode == GameManager.GameMode.Test)
        {
            VersionText.text = "Test Build v1.3." + System.IO.File.ReadAllText(Application.dataPath + "/version.txt");
        }
        else
        {
            VersionText.text = "Build v1.3." + System.IO.File.ReadAllText(Application.dataPath + "/version.txt");
        }

        PathText.text = GameManager.Instance.GetLinkJava().savePath;
    }
}
