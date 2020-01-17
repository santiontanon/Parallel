#if UNITY_EDITOR
using UnityEditor;
using UnityEngine;
using System.Collections.Generic;

public class MultiBuild : MonoBehaviour {

    public static List<BuildTarget> TargetPlatforms = new List<BuildTarget>
      { BuildTarget.StandaloneLinux,
        BuildTarget.StandaloneLinux64,
        BuildTarget.StandaloneLinuxUniversal,
        BuildTarget.StandaloneOSX,
        BuildTarget.StandaloneWindows,
        BuildTarget.StandaloneWindows64 };

    [MenuItem("Parallel/Start Builds")]
    public static void StartBuilds()
    {
        int version = int.Parse(System.IO.File.ReadAllText(Application.dataPath + "/version.txt"));
        version++;
        System.IO.File.WriteAllText(Application.dataPath + "/version.txt", version.ToString());
        foreach (BuildTarget b in TargetPlatforms)
        {
            string path = Application.dataPath + "/../Builds/" + b.ToString();
            string[] levels = new string[] { "Assets/Scenes/main.unity" };
            try
            {
                BuildPipeline.BuildPlayer(levels, path + "/Parallel.exe", b, BuildOptions.None);
                Debug.Log(b + " build suceeded!");
            }
            catch (System.Exception e)
            {
                Debug.Log(b + " build failed with error: ");
                Debug.Log(e);
            }
        }
    }

}
#endif
