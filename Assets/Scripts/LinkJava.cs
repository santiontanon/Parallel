using System;
using UnityEngine;
using System.Collections;
using System.Diagnostics;
using System.IO;

public class LinkJava : MonoBehaviour 
{
	public string externalPath;
	private string pathCPSeparator = ":";
	public string pathSeparator = "/";

	public enum SimulationTypes {ME, Play, PCG}
	public SimulationTypes simulationMode = SimulationTypes.ME;

	public string filename;

	GameManager gameManager;

    private Process externalProcess = null;

    public delegate void ME_Simulation(SimulationFeedback feedback);
	public static event ME_Simulation OnSimulationCompleted;
	public enum SimulationFeedback {none, success, failure}

    private string _lastPCGLevelGenerated;

    void Start()
    {
    	CheckEnvironment();
        if (IntPtr.Size == 8)
        {
            UnityEngine.Debug.Log("Running as 64 Bit process");
        }
        else
        {
            UnityEngine.Debug.Log("Running as 32 Bit process");
        }
    	if(Application.isEditor){
        	externalPath = Application.dataPath + "/../PCGMC4PP/dist/".Replace("/",pathSeparator);
        } else {
        	externalPath = Application.dataPath + "/PCGMC4PP/dist/".Replace("/",pathSeparator);
        }
        filename = Application.dataPath + "Resources/Exports/levels/level-2-prototype.txt";
		gameManager = GameManager.Instance;
    }

    void DisplayError(string title, string description, string buttonA = "", Action actionA = null, string buttonB = "", Action actionB = null)
    {
        UnityEngine.Debug.Log("Display error " + title);
        UINotifications.Notification error_message = new UINotifications.Notification(title, description);
        if (buttonA != "" && actionA != null)
            error_message.AddButton(buttonA, new UINotifications.ButtonMethod(() => { actionA(); }));
        if(buttonB != "" && actionB != null)
            error_message.AddButton(buttonB, new UINotifications.ButtonMethod(() => { actionB(); }));
        error_message.AddButton("Exit", new UINotifications.ButtonMethod(() => { Application.Quit(); }));
        UINotifications.Notify(error_message);
    }

    /// <summary>
    /// Checks the OS to determine how to format file paths
    /// </summary>
    /// <returns></returns>
	private int CheckEnvironment()
	{
		switch (Application.platform) 
		{
            case RuntimePlatform.WindowsPlayer:
            case RuntimePlatform.WindowsEditor:
                pathCPSeparator = ";";
                pathSeparator = "\\";
                break;
            case RuntimePlatform.OSXEditor:
            case RuntimePlatform.OSXPlayer:
                pathCPSeparator = ":";
                pathSeparator = "/";
                break;
            case RuntimePlatform.LinuxPlayer:
                pathCPSeparator = ":";
                pathSeparator = "/";
                break;
            default:
                DisplayError("Environment Error", "Parallel currently only supports Windows, OSX and Linux.", "Close", UINotifications.Close);
                return 1;
        }
		return 0;
	}

    /// <summary>
    /// Checks to see if Java installed an accessible
    /// ExitCode = 0: OK
    /// ExitCode = 1: Not found
    /// ExitCode = -1: Some exception
    /// </summary>
    /// <returns></returns>
    public int CheckJava()
	{
		int ExitCode = -1;
        Process myProcess = new Process();
        try 
		{
			myProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
			myProcess.StartInfo.CreateNoWindow = true;
			myProcess.StartInfo.UseShellExecute = false;
			myProcess.StartInfo.FileName = "java";
			myProcess.StartInfo.Arguments = "-version";
			myProcess.EnableRaisingEvents = true;
			myProcess.Start();
			myProcess.WaitForExit();
			ExitCode = myProcess.ExitCode;
		} 
		catch (Exception e)
		{
			UnityEngine.Debug.Log(e);
            UnityEngine.Debug.Log(myProcess.StartInfo.Arguments);
		}
		return ExitCode;
	}

    public void StopExternalProcess()
    {
        if(externalProcess != null)
        {
            externalProcess.Dispose();
        }
    }

    IEnumerator ExternalProcessFailure()
    {
        UnityEngine.Debug.LogError(externalProcess.StartInfo.Arguments);
        simulationFeedback = SimulationFeedback.failure;
        yield return null;
    }

    #region Simulation
    /// <summary>
    /// Sets up java arguments for the PCG/ME
    /// </summary>
    /// <returns></returns>
    private string StartSimulationProcess()
    {
		// prevent concurrent calls
		if (externalProcess != null) 
		{
            DisplayError("Blocked Process", "Another external process is already running. Please close this and wait for it to complete, or click close to end the process early.", "Close", UINotifications.Close, "Terminate", StopExternalProcess);
            return "Process may not have finished";
		} 
		else 
		{
			externalProcess = new Process ();
			externalProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
			externalProcess.StartInfo.CreateNoWindow = true;
			externalProcess.StartInfo.UseShellExecute = false;
			externalProcess.StartInfo.RedirectStandardOutput = true;
			externalProcess.StartInfo.RedirectStandardError = true;
			externalProcess.StartInfo.FileName = "java";
            //externalProcess.OutputDataReceived += new DataReceivedEventHandler(OutputHandler);
            //externalProcess.ErrorDataReceived += new DataReceivedEventHandler(OutputHandler);
			externalProcess.StartInfo.Arguments = "";
            externalProcess.StartInfo.Arguments += Constants.JVMSettings.MemoryAllocation[GameManager.Instance.JVMMemorySelection] + " ";
            externalProcess.StartInfo.Arguments += "-cp \"";
			externalProcess.StartInfo.Arguments +=externalPath+"PCGMC4PP.jar";
			externalProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"gson-2.6.2.jar";
			externalProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"jdom.jar";
			externalProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"prefuse.jar";
			externalProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"OGE.jar";
			externalProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"JGGS.jar";
            externalProcess.StartInfo.Arguments += "\" support." + simulationMode.ToString();
            if (simulationMode == SimulationTypes.ME) //submit
            {
                int budget = 600000;
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + budget;
            }
            else if (simulationMode == SimulationTypes.Play) //test
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed;
            }
            else //pcg
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed + " -1";
                externalProcess.StartInfo.Arguments += " \"" + Application.dataPath + "/../data" + "\"";
            }
            UnityEngine.Debug.Log(externalProcess.StartInfo.Arguments);
            externalProcess.EnableRaisingEvents = true;
            UnityEngine.Debug.Log(externalPath + "PCGMC4PP.jar");
            UnityEngine.Debug.Log(pathCPSeparator + externalPath + "lib" + pathSeparator + "gson-2.6.2.jar");
            externalProcess.Start ();
			StartCoroutine (SimulationRoutine ());
			return "External Async";
		}
	}

    SimulationFeedback simulationFeedback = SimulationFeedback.none;
    string _filename;

    /// <summary>
    /// Waits for the PCG/ME process to finish, then handles the result
    /// </summary>
    /// <returns></returns>
    IEnumerator SimulationRoutine()
	{
        // ExitCode = 0: OK
        // ExitCode = 1: System Errors
        // ExitCode = 2: Errors parsing input, use support.validate to check input
        // ExitCode = 3: Search budget depleted, no results, try a bigger budget
        // ExitCode = 4: No results found, search space exhausted (other search errors)
        // ExitCode = 5: Wrong arguments
        // ExitCode = 6: Other exception within the ME Java code

		if (externalProcess == null) 
		{
            DisplayError("Process is NULL", "An uknown error has occurred when starting the simulation. Please try again, if the issue persists contact the research team.", "Close", UINotifications.Close, "Terminate", StopExternalProcess);
		} 
		else 
		{
            string line = null;
            UnityEngine.Debug.Log("Waiting for process to complete");
            while (!externalProcess.HasExited)
            {
                line = externalProcess.StandardOutput.ReadLine();
                if (line != null && line.Contains(".txt"))
                    filename = line;
                yield return new WaitForSeconds(0.1f);
            }
            UnityEngine.Debug.Log("Process has completed");
            int ExitCode = externalProcess.ExitCode;
            while ((line = externalProcess.StandardOutput.ReadLine()) != null)
            {
                if (line.Contains(".txt"))
                    filename = line;
            }
            while ((line = externalProcess.StandardError.ReadLine()) != null)
            {
                UnityEngine.Debug.Log("ERROR " + line);
                if (line.Contains("OutOfMemoryError"))
                {
                    DisplayError("Out of Memory Exception", "Java has run out of memory. Return to level select, and contact the research team if the issue persists.", "Level Select", GameManager.Instance.AbortLinkJavaProcess);
                    externalProcess.StandardError.ReadToEnd();
                }
                else if(line.Contains("Unsupported major.minor"))
                {
                    DisplayError("Unsupported Major.Minor Version", "Java appears to be outdated, make sure you have the latest version of Java 8, and that environment variables are set correctly. If you have to update Java, don't forget to restart afterwards.", "Level Select", GameManager.Instance.AbortLinkJavaProcess);
                    externalProcess.StandardError.ReadToEnd();
                }
                else
                {
                    DisplayError("Unknown Error", "And unkown or unhandeled error has occured with the simulation. Please try again, and contact the research team if the issue persists.", "Level Select", GameManager.Instance.AbortLinkJavaProcess);
                }
            }
            GameManager.Instance.tracker.CreateEventExt("SimulationFeedback", externalProcess.ExitCode.ToString());
            if (ExitCode == 0)
            {
                externalProcess = null;
                if(simulationMode == SimulationTypes.ME || simulationMode == SimulationTypes.Play)
                    GameManager.Instance.tracker.SendModelLog(filename);
                yield return StartCoroutine(SimulationSuccess());

            }
            else 
			{
                yield return StartCoroutine(ExternalProcessFailure());
                externalProcess = null;
			}
        }

		if(OnSimulationCompleted!=null) { OnSimulationCompleted(simulationFeedback); }
	}

    IEnumerator SimulationSuccess()
    {
        // send this to the server first
        string upload_data = "filename\t" + filename + "\ntimestamp\t" + DateTime.Now.ToString("yyyyMMddHHmmss") + "\n\n" + System.IO.File.ReadAllText(filename);
        GameManager.Instance.tracker.UploadData(upload_data);
        UnityEngine.Debug.Log("It's now time to load " + filename);
        StreamReader reader = new StreamReader(filename);

        if(simulationMode == SimulationTypes.PCG)
            GameManager.Instance.GetSaveManager().currentSave.AddNewPCGLevel(reader.ReadToEnd());
        _lastPCGLevelGenerated = reader.ReadToEnd();

        GameManager.Instance.GetSaveManager().UpdateSave();
        bool restartPhase = (simulationMode == SimulationTypes.PCG);
        GameManager.Instance.TriggerLoadLevel(restartPhase, DataManager.LoadType.FILEPATH, filename);
        //in case it takes time to load larger levels
        while (GameManager.Instance.gamePhase != GameManager.GamePhases.PlayerInteraction) yield return new WaitForEndOfFrame();
        simulationFeedback = SimulationFeedback.success;
    }

    public string GetLastPCGGeneratedLevel() { return _lastPCGLevelGenerated; }
    public void ClearLastPCGGeneratedLevel() { _lastPCGLevelGenerated = ""; }

    public void SendToJava() 
	{
		bool java_found = false;
		if (CheckEnvironment () != 0) 
		{
            DisplayError("Environment Error", "Parallel currently only supports Windows, OSX and Linux.");
		} 
		else 
		{
			java_found = (CheckJava () == 0);
		}
		if (!java_found) 
		{
            DisplayError("Java Not Found", "Parallel could not confirm that the Java Development Kit is installed. Ensure JDK version 8, updated 171 or higher is installed.");
        }
		else {
		    UnityEngine.Debug.Log(StartSimulationProcess()); //this is what calls the ME/PCG processes
		}
	}
    #endregion

    #region Player Modeling
    public string StartPlayerModelingServerCall(string username, string hostname, Action callback = null)
    {
        // prevent concurrent calls
        if (externalProcess != null)
        {
            DisplayError("Blocked Process", "Another external process is already running. Please close this and wait for it to complete, or click close to end the process early.", "Close", UINotifications.Close, "Terminate", StopExternalProcess);
            return "Process may not have finished";
        }
        else
        {
            externalProcess = new Process();
            externalProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            externalProcess.StartInfo.CreateNoWindow = true;
            externalProcess.StartInfo.UseShellExecute = false;
            externalProcess.StartInfo.RedirectStandardOutput = true;
            externalProcess.StartInfo.RedirectStandardError = true;
            externalProcess.StartInfo.FileName = "java";
            externalProcess.StartInfo.Arguments = " -jar " + "\"" + externalPath + "ServerInterface.jar" + "\"";
            externalProcess.StartInfo.Arguments += " -mode read";
            externalProcess.StartInfo.Arguments += " -user " + username;
            externalProcess.StartInfo.Arguments += " -path " + "\"" + Application.persistentDataPath + pathSeparator + "currentParameters.txt" + "\"";
            externalProcess.StartInfo.Arguments += " -hostname " + hostname + " -port 8787";
            UnityEngine.Debug.Log(externalProcess.StartInfo.Arguments);
            externalProcess.Start();
            StartCoroutine(PlayerModelingServerRoutine(callback));
            return "sucess";
        }
    }

    IEnumerator PlayerModelingServerRoutine(Action callback = null)
    {
        while (!externalProcess.HasExited)
        {
            yield return new WaitForSeconds(1f);
        }
        externalProcess = null;
        UnityEngine.Debug.Log("Player Modeling Server Routine Complete");
        if (callback != null)
            callback();
        yield return null;
    }

    public string StartPlayerModelingProcess(string executionPath, string logPath, string username, string levelname, string hostname, Action callback = null)
    {
        // prevent concurrent calls
        if (externalProcess != null)
        {
            DisplayError("Blocked Process", "Another external process is already running. Please close this and wait for it to complete, or click close to end the process early.", "Close", UINotifications.Close, "Terminate", StopExternalProcess);
            return "Process may not have finished";
        }
        else
        {
            externalProcess = new Process();
            externalProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            externalProcess.StartInfo.CreateNoWindow = true;
            externalProcess.StartInfo.UseShellExecute = false;
            externalProcess.StartInfo.RedirectStandardOutput = true;
            externalProcess.StartInfo.RedirectStandardError = true;
            externalProcess.StartInfo.FileName = "java";
            externalProcess.StartInfo.Arguments = " -jar " + "\"" + externalPath + "PlayerModel.jar" + "\"";
            externalProcess.StartInfo.Arguments += " -mepath " + "\"" + executionPath + "\"";
            externalProcess.StartInfo.Arguments += " -telemetrypath " + "\"" + logPath + "\"";
            externalProcess.StartInfo.Arguments += " -user " + username;
            externalProcess.StartInfo.Arguments += " -level " + levelname;
            externalProcess.StartInfo.Arguments += " -parameterpath " + "\"" + Application.persistentDataPath + pathSeparator + "currentParameters.txt" + "\"";
            externalProcess.StartInfo.Arguments += " -pmdir " + "\"" + externalPath.TrimEnd(new char[2] {'/', '\\' }) + "\"";
            externalProcess.StartInfo.Arguments += " -hostname " + hostname + " -port 8787";
            UnityEngine.Debug.Log(externalProcess.StartInfo.Arguments);
            externalProcess.Start();
            StartCoroutine(PlayerModelingRoutine(callback));
            return "sucess";
        }
    }

    IEnumerator PlayerModelingRoutine(Action callback = null)
    {
        while (!externalProcess.HasExited)
        {
            yield return new WaitForSeconds(1f);
        }
        externalProcess = null;
        UnityEngine.Debug.Log("Player Modeling Routine Complete");
        if (callback != null)
            callback();
        yield return null;
    }
    #endregion
}
