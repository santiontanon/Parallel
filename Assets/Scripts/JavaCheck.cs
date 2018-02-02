using UnityEngine;
using System.Collections;
using System.Diagnostics;

public class JavaCheck : MonoBehaviour {

    public static bool FindJavaVersion()
    {
        try
        {
            ProcessStartInfo process = new ProcessStartInfo();
            process.FileName = "java.exe";
            process.Arguments = " -version";
            process.RedirectStandardError = true;
            process.RedirectStandardOutput = true;
            process.UseShellExecute = false;
            process.CreateNoWindow = true;
            Process pr = Process.Start(process);

            string strOutput = pr.StandardError.ReadLine().Split(' ')[2].Replace("\"", "");
            UnityEngine.Debug.Log(strOutput);

            return true;
        }
        catch (UnityException e)
        {
            UnityEngine.Debug.Log("Exception: " + e.Message);
            return false;
        }
    }
}
