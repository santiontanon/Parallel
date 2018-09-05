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

	public delegate void ME_Simulation(SimulationFeedback feedback);
	public static event ME_Simulation OnSimulationCompleted;
	public enum SimulationFeedback {none, success, failure}

    private string _lastPCGLevelGenerated;

    void Awake()
    {
    	checkEnvironment();
    	if(Application.isEditor){
        	externalPath = Application.dataPath + "/../PCGMC4PP/dist/".Replace("/",pathSeparator);
        } else {
        	externalPath = Application.dataPath + "/PCGMC4PP/dist/".Replace("/",pathSeparator);
        }
        filename = Application.dataPath + "Resources/Exports/levels/level-2-prototype.txt";
		gameManager = GameManager.Instance;
    }

	private int checkEnvironment()
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

	private int checkJava()
	{
		// ExitCode = 0: OK
		// ExitCode = 1: Not found
		// ExitCode = -1: Some exception
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

	private Process externalProcess = null;
	private string externalNonBlocking()
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
			externalProcess.StartInfo.FileName = "java";
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
            if (simulationMode == SimulationTypes.ME)
            {
                int budget = 600000;
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + budget;
            }
            else if (simulationMode == SimulationTypes.Play)
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed;
            }
            else
            {
                int rSeed = UnityEngine.Random.Range(-100000, -1);
                UnityEngine.Debug.Log(rSeed);
                string size = " " + "\"" + "any" + "\"";
                externalProcess.StartInfo.Arguments += " \"" + filename + "\" " + rSeed + size;
            }
            UnityEngine.Debug.Log(externalProcess.StartInfo.Arguments);
            externalProcess.EnableRaisingEvents = true;
            UnityEngine.Debug.Log(externalPath + "PCGMC4PP.jar");
            UnityEngine.Debug.Log(pathCPSeparator + externalPath + "lib" + pathSeparator + "gson-2.6.2.jar");
            externalProcess.Start ();
			StartCoroutine (externalNonBlockingWait ());
			return "External Async";
		}
	}

	IEnumerator externalNonBlockingWait()
	{
        // ExitCode = 0: OK
        // ExitCode = 1: System Errors
        // ExitCode = 2: Errors parsing input, use support.validate to check input
        // ExitCode = 3: Search budget depleted, no results, try a bigger budget
        // ExitCode = 4: No results found, search space exhausted (other search errors)
        // ExitCode = 5: Wrong arguments
        // ExitCode = 6: Other exception within the ME Java code

		SimulationFeedback simulationFeedback = SimulationFeedback.none;

		if (externalProcess == null) 
		{
			UnityEngine.Debug.Log ("Process is null");
		} 
		else 
		{
            UnityEngine.Debug.Log("Waiting for process to complete");
            while (!externalProcess.HasExited) 
			{
                //UnityEngine.Debug.Log("Process exited? " + externalProcess.HasExited);
                yield return null;
            }
            UnityEngine.Debug.Log("Process has completed");
            int ExitCode = externalProcess.ExitCode;
			string mpout = "";
			string line = null;
			string filename = "";
			while ((line = externalProcess.StandardOutput.ReadLine()) != null) 
			{
				filename = line;
				mpout += line + "\n";
			}
			mpout += "Exit code: "+ExitCode.ToString ();
            UnityEngine.Debug.Log(line);
            UnityEngine.Debug.Log(mpout);
			UnityEngine.Debug.Log ("Java finished here...");
			if (ExitCode == 0) 
			{
				UnityEngine.Debug.Log (externalProcess.StartInfo.Arguments);	
				// send this to the server first
				string upload_data = "filename\t" + filename + "\ntimestamp\t" + DateTime.Now.ToString("yyyyMMddHHmmss") +"\n\n" + System.IO.File.ReadAllText(filename);
				GameManager.Instance.tracker.UploadData(upload_data);
				UnityEngine.Debug.Log ("It's now time to load "+filename);
                StreamReader reader = new StreamReader(filename);

                /* should wait to save this */
                //GameManager.Instance.GetSaveManager().currentSave.AddNewPCGLevel(reader.ReadToEnd());
                _lastPCGLevelGenerated = reader.ReadToEnd();

                GameManager.Instance.GetSaveManager().UpdateSave();
                bool restartPhase = (simulationMode == SimulationTypes.PCG);
				GameManager.Instance.TriggerLoadLevel(restartPhase, DataManager.LoadType.FILEPATH, filename);
				//in case it takes time to load larger levels
				while(GameManager.Instance.gamePhase != GameManager.GamePhases.PlayerInteraction) yield return new WaitForEndOfFrame();
				simulationFeedback = SimulationFeedback.success;
			} 
			else 
			{
				UnityEngine.Debug.LogError (mpout);	
				UnityEngine.Debug.LogError (externalProcess.StartInfo.Arguments);	
				simulationFeedback = SimulationFeedback.failure;
			}
				
			GameManager.Instance.tracker.CreateEventExt("SimulationFeedback", externalProcess.ExitCode.ToString());

			externalProcess = null;	
		}
		//UnityEngine.Debug.Log ("Finished waiting.");

		if(OnSimulationCompleted!=null) { OnSimulationCompleted(simulationFeedback); }
	}

    public string GetLastPCGGeneratedLevel() { return _lastPCGLevelGenerated; }
    public void ClearLastPCGGeneratedLevel() { _lastPCGLevelGenerated = ""; }

    public void SendToME () 
	{
		bool java_found = false;
		if (checkEnvironment () != 0) 
		{
            UINotifications.Notification error_message = new UINotifications.Notification("Environment Error", "An error with Java has been encountered.");
            error_message.AddButton("Exit", new UINotifications.ButtonMethod(() => { Application.Quit(); }));
            UINotifications.Notify(error_message);
			//UnityEngine.Debug.Log ("Cannot work here, give some feedback to the user...");
		} 
		else 
		{
			java_found = (checkJava () == 0);
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
		UnityEngine.Debug.Log(externalNonBlocking ()); //this is what calls the ME simulation
		//UnityEngine.Debug.Log ("Finished calling Java...");
		}
	}
}
