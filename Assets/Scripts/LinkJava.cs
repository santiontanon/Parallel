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

    private Process javaProcess = null;
    private Process modelingProcess = null;

    [SerializeField]
    private GameObject[] processIndicators;

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

    void Update()
    {
        if (javaProcess != null)
        {
            if (processIndicators[0] != null && processIndicators[0].activeInHierarchy != true)
                processIndicators[0].SetActive(true);
        }
        else if (processIndicators[0] != null && processIndicators[0].activeInHierarchy != false)
            processIndicators[0].SetActive(false);
        if(modelingProcess != null)
        {
            if (processIndicators[1] != null && processIndicators[1].activeInHierarchy != true)
                processIndicators[1].SetActive(true);
        }
        else if (processIndicators[1] != null && processIndicators[1].activeInHierarchy != false)
            processIndicators[1].SetActive(false);
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
                DisplayError("Environment Error", "Parallel currently only supports Windows, OSX and Linux.", "Close", GameManager.Instance.AbortLinkJavaProcess);
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
        if(javaProcess != null)
        {
            javaProcess.Kill();
        }
    }

    IEnumerator ExternalProcessFailure()
    {
        UnityEngine.Debug.LogError(javaProcess.StartInfo.Arguments);
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
		if (javaProcess != null) 
		{
            DisplayError("Blocked Process", "Another external process is already running. Please close this and wait for it to complete, or click close to end the process early.", "Close", GameManager.Instance.AbortLinkJavaProcess, "Terminate", GameManager.Instance.AbortLinkJavaProcess);
            return "Process may not have finished";
		} 
		else 
		{
			javaProcess = new Process ();
			javaProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
			javaProcess.StartInfo.CreateNoWindow = true;
			javaProcess.StartInfo.UseShellExecute = false;
			javaProcess.StartInfo.RedirectStandardOutput = true;
			javaProcess.StartInfo.RedirectStandardError = true;
			javaProcess.StartInfo.FileName = "java";
            //externalProcess.OutputDataReceived += new DataReceivedEventHandler(OutputHandler);
            //externalProcess.ErrorDataReceived += new DataReceivedEventHandler(OutputHandler);
			javaProcess.StartInfo.Arguments = "";
            javaProcess.StartInfo.Arguments += Constants.JVMSettings.MemoryAllocation[GameManager.Instance.JVMMemorySelection] + " ";
            javaProcess.StartInfo.Arguments += "-cp \"";
			javaProcess.StartInfo.Arguments +=externalPath+"PCGMC4PP.jar";
			javaProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"gson-2.6.2.jar";
			javaProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"jdom.jar";
			javaProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"prefuse.jar";
			javaProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"OGE.jar";
			javaProcess.StartInfo.Arguments +=pathCPSeparator+externalPath+"lib"+pathSeparator+"JGGS.jar";
            javaProcess.StartInfo.Arguments += "\" support." + simulationMode.ToString();
            if (simulationMode == SimulationTypes.ME) //submit
            {
                int budget = 600000;
                javaProcess.StartInfo.Arguments += " \"" + filename + "\" " + budget;
            }
            else if (simulationMode == SimulationTypes.Play) //test
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                javaProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed;
            }
            else //pcg
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                javaProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed + " -1";
                javaProcess.StartInfo.Arguments += " \"" + Application.dataPath + "/../data" + "\"";
            }
            UnityEngine.Debug.Log(javaProcess.StartInfo.Arguments);
            javaProcess.EnableRaisingEvents = true;
            UnityEngine.Debug.Log(externalPath + "PCGMC4PP.jar");
            UnityEngine.Debug.Log(pathCPSeparator + externalPath + "lib" + pathSeparator + "gson-2.6.2.jar");
            javaProcess.Start ();
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

		if (javaProcess == null) 
		{
            DisplayError("Process is NULL", "An uknown error has occurred when starting the simulation. Please try again, if the issue persists contact the research team.", "Close", GameManager.Instance.AbortLinkJavaProcess, "Terminate", GameManager.Instance.AbortLinkJavaProcess);
		} 
		else 
		{
            string line = null;
            UnityEngine.Debug.Log("Waiting for process to complete");
            while (!javaProcess.HasExited)
            {
                line = javaProcess.StandardOutput.ReadLine();
                if (line != null && line.Contains(".txt"))
                    filename = line;
                yield return new WaitForSeconds(0.1f);
            }
            UnityEngine.Debug.Log("Process has completed");
            int ExitCode = javaProcess.ExitCode;
            while ((line = javaProcess.StandardOutput.ReadLine()) != null)
            {
                if (line.Contains(".txt"))
                    filename = line;
            }
            while ((line = javaProcess.StandardError.ReadLine()) != null)
            {
                UnityEngine.Debug.Log("ERROR " + line);
                string errorText = "";
                if (line.IndexOf("out of memory exception", StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    DisplayError("Out of Memory Exception", "Java has run out of memory. Return to level select, and contact the research team if the issue persists.", "Level Select", GameManager.Instance.ResetToLevelSelect);
                    javaProcess.StandardError.ReadToEnd();
                }
                else if (line.IndexOf("unsupported major.minor", StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    DisplayError("Unsupported Major.Minor Version", "Java appears to be outdated, make sure you have the latest version of Java 8, and that environment variables are set correctly. If you have to update Java, don't forget to restart afterwards.", "Level Select", GameManager.Instance.ResetToLevelSelect);
                    javaProcess.StandardError.ReadToEnd();
                }
                else if (line.IndexOf("Could not find or load main class support.PCG", StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    DisplayError("PCG System Files Missing", "Unable to locate PCG system files, please contact the research team.", "Level Select", GameManager.Instance.ResetToLevelSelect);
                    javaProcess.StandardError.ReadToEnd();
                }
                else if (line.IndexOf("java.io.FileNotFoundException", StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    DisplayError("Data Files Missing", "Unable to locate data files, please contact the research team.", "Level Select", GameManager.Instance.ResetToLevelSelect);
                    javaProcess.StandardError.ReadToEnd();
                }
                else if (!line.Contains("tile"))
                {
                    errorText += line;
                    DisplayError("Unknown Error", line, "Close", GameManager.Instance.AbortLinkJavaProcess, "Level Select", GameManager.Instance.ResetToLevelSelect);
                }
            }
            GameManager.Instance.tracker.CreateEventExt("SimulationFeedback", javaProcess.ExitCode.ToString());
            if (ExitCode == 0)
            {
                javaProcess = null;
                if(simulationMode == SimulationTypes.ME || simulationMode == SimulationTypes.Play)
                    GameManager.Instance.tracker.SendModelLog(filename);
                yield return StartCoroutine(SimulationSuccess());

            }
            else 
			{
                yield return StartCoroutine(ExternalProcessFailure());
                javaProcess = null;
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
        if (modelingProcess != null)
        {
            DisplayError("Blocked Process", "Another external process is already running. Please close this and wait for it to complete, or click close to end the process early.", "Close", GameManager.Instance.AbortLinkJavaProcess, "Terminate", GameManager.Instance.AbortLinkJavaProcess);
            return "Process may not have finished";
        }
        else
        {
            modelingProcess = new Process();
            modelingProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            modelingProcess.StartInfo.CreateNoWindow = true;
            modelingProcess.StartInfo.UseShellExecute = false;
            modelingProcess.StartInfo.RedirectStandardOutput = true;
            modelingProcess.StartInfo.RedirectStandardError = true;
            modelingProcess.StartInfo.FileName = "java";
            modelingProcess.StartInfo.Arguments = " -jar " + "\"" + externalPath + "ServerInterface.jar" + "\"";
            modelingProcess.StartInfo.Arguments += " -mode read";
            modelingProcess.StartInfo.Arguments += " -user " + username;
            modelingProcess.StartInfo.Arguments += " -path " + "\"" + Application.persistentDataPath + pathSeparator + "currentParameters.txt" + "\"";
            modelingProcess.StartInfo.Arguments += " -hostname " + hostname + " -port 8787";
            UnityEngine.Debug.Log(modelingProcess.StartInfo.Arguments);
            modelingProcess.Start();
            StartCoroutine(PlayerModelingServerRoutine(callback));
            return "sucess";
        }
    }

    IEnumerator PlayerModelingServerRoutine(Action callback = null)
    {
        while (!modelingProcess.HasExited)
        {
            yield return new WaitForSeconds(1f);
        }
        modelingProcess = null;
        UnityEngine.Debug.Log("Player Modeling Server Routine Complete");
        if (callback != null)
            callback();
        yield return null;
    }

    public string StartPlayerModelingProcess(string executionPath, string logPath, string username, string levelname, string hostname, Action callback = null)
    {
        // prevent concurrent calls
        if (modelingProcess != null)
        {
            DisplayError("Blocked Process", "Another external process is already running. Please close this and wait for it to complete, or click close to end the process early.", "Close", GameManager.Instance.AbortLinkJavaProcess, "Terminate", GameManager.Instance.AbortLinkJavaProcess);
            return "Process may not have finished";
        }
        else
        {
            modelingProcess = new Process();
            modelingProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            modelingProcess.StartInfo.CreateNoWindow = true;
            modelingProcess.StartInfo.UseShellExecute = false;
            modelingProcess.StartInfo.RedirectStandardOutput = true;
            modelingProcess.StartInfo.RedirectStandardError = true;
            modelingProcess.StartInfo.FileName = "java";
            modelingProcess.StartInfo.Arguments = " -jar " + "\"" + externalPath + "PlayerModel.jar" + "\"";
            modelingProcess.StartInfo.Arguments += " -mepath " + "\"" + executionPath + "\"";
            modelingProcess.StartInfo.Arguments += " -telemetrypath " + "\"" + logPath + "\"";
            modelingProcess.StartInfo.Arguments += " -user " + username;
            modelingProcess.StartInfo.Arguments += " -level " + levelname;
            modelingProcess.StartInfo.Arguments += " -parameterpath " + "\"" + Application.persistentDataPath + pathSeparator + "currentParameters.txt" + "\"";
            modelingProcess.StartInfo.Arguments += " -pmdir " + "\"" + externalPath.TrimEnd(new char[2] {'/', '\\' }) + "\"";
            modelingProcess.StartInfo.Arguments += " -hostname " + hostname + " -port 8787";
            UnityEngine.Debug.Log(modelingProcess.StartInfo.Arguments);
            modelingProcess.Start();
            StartCoroutine(PlayerModelingRoutine(callback));
            return "sucess";
        }
    }

    IEnumerator PlayerModelingRoutine(Action callback = null)
    {
        while (!modelingProcess.HasExited)
        {
            yield return new WaitForSeconds(1f);
        }
        modelingProcess = null;
        UnityEngine.Debug.Log("Player Modeling Routine Complete");
        if (callback != null)
            callback();
        yield return null;
    }
    #endregion
}
