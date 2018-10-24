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

    void Awake()
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
			UnityEngine.Debug.Log ("Environment Error");
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
		try 
		{
			Process myProcess = new Process();
			myProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
			myProcess.StartInfo.CreateNoWindow = true;
			myProcess.StartInfo.UseShellExecute = false;
			myProcess.StartInfo.FileName = "java";
			myProcess.StartInfo.Arguments = "-version";
			myProcess.EnableRaisingEvents = true;
			myProcess.Start();
			myProcess.WaitForExit();
			ExitCode = myProcess.ExitCode;
			//while ((line = myProcess.StandardOutput.ReadLine()) != null) { mpout += line + "\n";}
		} 
		catch (Exception e)
		{
			UnityEngine.Debug.Log(e);
		}
		return ExitCode;
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
            else if (simulationMode == SimulationTypes.Play) //testing
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed;
            }
            else //pcg
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                int rSize = UnityEngine.Random.Range(0, 3);
                UnityEngine.Debug.Log("RSize: " + rSize);
                string size = " " + "\"" + rSize.ToString() + "\"";
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed + size;
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
			UnityEngine.Debug.Log ("Process is null");
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
                UnityEngine.Debug.Log(line);
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

    public void SendToME () 
	{
		bool java_found = false;
		if (CheckEnvironment () != 0) 
		{
            UINotifications.Notification error_message = new UINotifications.Notification("Environment Error", "An error with Java has been encountered.");
            error_message.AddButton("Exit", new UINotifications.ButtonMethod(() => { Application.Quit(); }));
            UINotifications.Notify(error_message);
			//UnityEngine.Debug.Log ("Cannot work here, give some feedback to the user...");
		} 
		else 
		{
			java_found = (CheckJava () == 0);
		}
		if (!java_found) 
		{
            UINotifications.Notification error_message = new UINotifications.Notification("Java Error", "An error with Java has been encountered.");
            error_message.AddButton("Exit", new UINotifications.ButtonMethod(() => { Application.Quit(); }));
            UINotifications.Notify(error_message);
            //UnityEngine.Debug.LogError ("Java is not found, give some feedback to the user...");
        }
		else {
		//UnityEngine.Debug.Log ("Calling Java...");
		UnityEngine.Debug.Log(StartSimulationProcess ()); //this is what calls the ME simulation
		//UnityEngine.Debug.Log ("Finished calling Java...");
		}
	}
    #endregion

    #region Player Modeling
    public string StartPlayerModelingServerCall(string username, string hostname, Action callback = null)
    {
        // prevent concurrent calls
        if (externalProcess != null)
        {
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
