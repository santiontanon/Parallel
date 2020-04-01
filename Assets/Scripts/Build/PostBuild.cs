#if UNITY_EDITOR
using UnityEngine;
using UnityEditor;
using UnityEditor.Callbacks;

public class PostBuild : MonoBehaviour {

    [PostProcessBuild(0)]
    public static void MoveFiles(BuildTarget target, string pathToBuild)
    {
        string dataLocation = Application.dataPath + "/../data/";
        string dataPath ="";
        string PCGMC4PPLocation = Application.dataPath + "/../PCGMC4PP/";
        string PCGMC4PPPath = "";
        string versionLocation = Application.dataPath + "/version.txt";
        string versionPath = "";

        string path = pathToBuild.Remove(pathToBuild.Length - 12);

        switch (target)
        {
            case BuildTarget.StandaloneLinux:
            case BuildTarget.StandaloneLinux64:
            case BuildTarget.StandaloneLinuxUniversal:
                dataPath = path + "/data";
                PCGMC4PPPath = path + "/Parallel_Data/PCGMC4PP";
                versionPath = path + "/Parallel_Data/version.txt";
                break;

            case BuildTarget.StandaloneOSX:
                path += "Parallel.app/";
                dataPath = path + "/data";
                PCGMC4PPPath = path + "Contents/PCGMC4PP";
                versionPath = path + "/Contents/version.txt";
                break;

            case BuildTarget.StandaloneWindows:
            case BuildTarget.StandaloneWindows64:
            default:
                dataPath = path + "/data";
                PCGMC4PPPath = path + "/Parallel_Data/PCGMC4PP";
                versionPath = path + "/Parallel_Data/version.txt";
                break;
        }

        FileUtil.CopyFileOrDirectory(dataLocation, dataPath);
        FileUtil.CopyFileOrDirectory(PCGMC4PPLocation, PCGMC4PPPath);
        FileUtil.CopyFileOrDirectory(versionLocation, versionPath);
    }

}
#endif
