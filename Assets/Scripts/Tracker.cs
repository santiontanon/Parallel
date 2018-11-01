using UnityEngine;
using System.Collections;
using System.Collections.Generic;

using System.Text;
//using SimpleJSON; // http://wiki.unity3d.com/index.php/SimpleJSON
#if UNITY_STANDALONE
using System.IO;
using System.Net.NetworkInformation;
#endif

public class Tracker : MonoBehaviour {
	public string DebugInfoLabel = "NOT TRACKING";
	public bool ShowDebugInfoLabel = true;
	private bool allowQuitting = false;
	private bool allowQuittingRequested = false;
    public string hostname;
    public string port;
	public string url
    {
        get { return "http://" + hostname + ":" + port; }
    }
	public string url_id
    {
        get { return url +"/id"; }
    }
	public string url_logs
    {
        get { return url + "/log"; }
    }
    public string url_data
    {
        get { return url + "/data"; }
    }
    public string level_data = "";
	public float logs_uploaded_time = 0f;
    public string session_id
    {
        get { return tracking_session_id; }
    }
	private string tracking_session_id = "NA";
	private string tracking_session_user = "NONE";
	private string tracking_session_version = "Study3/RC2";
	public bool connected = false;
	public bool ready = false;

	public bool local_tracking_enabled = true;
    public bool remote_tracking_enabled = true;
    public bool tracking_message_collection = true;
	
	StreamWriter log = null;
	private StringBuilder log2 = null;
    public StreamWriter modelLog = null;
    public string modelLogPath;

    private static int[] keys_of_interest = new int[] {
		(int)KeyCode.Return, (int)KeyCode.Escape, (int)KeyCode.Space,
		(int)KeyCode.Keypad8, (int)KeyCode.Keypad2, (int)KeyCode.Keypad4, (int)KeyCode.Keypad6,
		(int)KeyCode.UpArrow, (int)KeyCode.DownArrow, (int)KeyCode.LeftArrow, (int)KeyCode.RightArrow,
		(int)KeyCode.W, (int)KeyCode.S, (int)KeyCode.A, (int)KeyCode.D
	};
	
	private static Vector2 mouse_position_last = new Vector2(0,0);
	private static Vector2 mouse_vector_last = new Vector2(0,0);
	private static float mouse_position_delta;
	private static float mouse_vector_delta;
	private static float mouse_probe_dist_sensitivity = 100.0f;
	private static float mouse_probe_vect_sensitivity = 0.5f;
	private static float mouse_probe_vect_sensitivity_max = 0.75f;
	private static float mouse_probe_time = 0.0f;
	private static float mouse_probe_freq = 0.1f;
	private static bool mouse_moving = false;
	private static Start_GamePhaseBehavior.StartPlayingWithLevelInformationDelegate onSuccess = null;
	private static Start_GamePhaseBehavior.NoInternetErrorDelegate onFail = null;

	void Start () {
		DebugInfoLabel = "NOT TRACKING";
        if (GameManager.Instance.currentGameMode == GameManager.GameMode.Demo)
        {
            allowQuitting = true;
        }
        else
        {
            allowQuitting = false;
        }
		allowQuittingRequested = false;
		connected = false;
		ready = false;
		hostname = "https://tkv2t9v8ad.execute-api.us-east-1.amazonaws.com/prod"; // this is out of date, not to be used
		hostname = "129.25.12.216"; // this is the CCI cloud "centos" server
		hostname = "129.25.141.236"; // this is the "magic" server in Santi's office
        port = "8787";
		// to start the service in the server, start a `screen` session, then go into LogVisualizer and type `python httpserver.py -i 10000 -s 144.118.172.191 -p 8787`
		// then you can close the `screen` by hitting `control+a`, `d`, and then you can `exit`.
		// the logs are saved in a directory called `saved_data` relative to the location of the server script
	}

	public void StartTrackerWithCallback(Start_GamePhaseBehavior.StartPlayingWithLevelInformationDelegate onSuccessD, Start_GamePhaseBehavior.NoInternetErrorDelegate onFailD, string _url = "NA", bool trackLocal = true, bool trackRemote = true){
		onSuccess = onSuccessD;
		onFail = onFailD;
        if (_url != "NA")
            hostname = _url;
        local_tracking_enabled = trackLocal;
        remote_tracking_enabled = trackRemote;
        StartTracker();
	}

	public void StartTracker()
    {
        if (ready && tracking_session_user == PlayerPrefs.GetString("PlayerId", "NONE")) return;
        tracking_session_user = PlayerPrefs.GetString("PlayerId", "NONE");
        if (remote_tracking_enabled)
        {
            FetchConfig();
        }
        else
        {
            SetupTracking();
        }
    }

    public void ChangeRemoteAddress(string ipAndPort, System.Action callback = null)
    {
        hostname =  ipAndPort;
        Debug.Log(url);
        FetchConfig(callback);
    }

    public void UpdateTracking(bool trackLocal, bool trackRemote)
    {
        local_tracking_enabled = trackLocal;
        remote_tracking_enabled = trackRemote;
    }

    public void FetchConfig(System.Action callback = null)
    {
        StartCoroutine(FetchConfigRoutine(callback));
    }
    
	IEnumerator FetchConfigRoutine(System.Action callback = null)
    {
        DebugInfoLabel = "FetchConfig from " + url_id;
        Debug.Log(url_id);
        string ourPostData = "{\"user\":\"" + tracking_session_user + "\",\"version\":\"" + tracking_session_version + "\"}";
        Dictionary<string, string> headers = new Dictionary<string, string>();
        headers.Add("Content-Type", "application/json");
        byte[] pData = System.Text.Encoding.ASCII.GetBytes(ourPostData.ToCharArray());
        WWW www = new WWW(url_id, pData, headers);
        yield return www;
        if (www.isDone && www.error == null && www.text != null && www.text.Trim() != "")
        {
            try
            {
                Debug.Log(www.text);
                JSONObject ob = new JSONObject(www.text.Trim());
                tracking_session_id = ob.GetField("id").ToString();
                PlayerModelingServerCall();
                level_data = www.text;
                SetupTracking();
            }
            catch (System.Exception e)
            {
                Debug.Log(e);
            }
            if (onSuccess != null) onSuccess(www.text);
        }
        else
        {
            if (onFail != null) onFail();
            Debug.Log(www.error);
            tracking_session_id = "NA";
        }
        if (callback != null)
            callback();
        //tracking_session_id = {"id": 11, "user": "hello"}
        yield return null; 
	}

    void SetupTracking()
    {
        string fileName = GameManager.Instance.GetLinkJava().localPath + "Session-" + tracking_session_id.ToString() + "-" + System.DateTime.Now.ToString("MM-dd-yy-HH-mm-ss") + ".log";
        log = File.CreateText(fileName);
        if (log == null)
        {
            Debug.Log("Error opening local log " + fileName);
        }
        {
            Debug.Log("Logging locally to " + fileName);
        }
        log2 = new StringBuilder();
        CreateEvent("SessionID", tracking_session_id);
        CreateEvent("SessionUser", tracking_session_user);
        CreateEvent("SessionVersion", tracking_session_version);
        CreateEvent("Calibration", Screen.width.ToString() + "x" + Screen.height.ToString());
        ready = true;
        DebugInfoLabel = "TRACKING " + tracking_session_id;
    }

    IEnumerator SaveLogs()
    {
        Debug.Log("SaveLogs");
        if (remote_tracking_enabled)
        {
            yield return StartCoroutine(UploadLogsWWW());
        }
        if (local_tracking_enabled)
        {
            if (log != null) log.Close();
        }
        yield return new WaitForSeconds(1f);
        allowQuitting = true;
        Application.Quit();
    }

    public void UploadData(string data)
    {
        StartCoroutine(UploadDataWWW(data));
    }

    IEnumerator UploadDataWWW(string data)
    {
        Debug.Log("UploadDataWWW");
        Dictionary<string, string> headers = new Dictionary<string, string>();
        headers.Add("Content-Type", "text/plain");
        byte[] pData = System.Text.Encoding.ASCII.GetBytes(data.ToCharArray());
        WWW www = new WWW(url_data, pData, headers);
        yield return www;
        if (www.error == null)
        {
            CreateEvent("UploadDataWWW", "OK");
            Debug.Log("UploadDataWWW OK");
        }
        else
        {
            Debug.Log("UploadDataWWW Error");
            Debug.Log(www.error);
            CreateEvent("UploadDataWWW", "Error");
        }
    }

    public void UploadLogs(){
        if (log2 != null && (Time.time - logs_uploaded_time) > 3.0f)
        {
            logs_uploaded_time = Time.time;
            CreateEvent("UploadRequest", "Sent");
            StartCoroutine("UploadLogsWWW");
        }
    }

    IEnumerator UploadLogsWWW() {
        Debug.Log("UploadLogsWWW");
        string ourPostData = log2.ToString();
        Dictionary<string, string> headers = new Dictionary<string, string>();
        headers.Add("Content-Type", "text/plain");
        byte[] pData = System.Text.Encoding.ASCII.GetBytes(ourPostData.ToCharArray());
        WWW www = new WWW(url_logs, pData, headers);
        yield return www;
        if (www.isDone && www.error == null && www.text != null && www.text.Trim() != "")
        {
            CreateEvent("UploadRequest", "OK");
            Debug.Log("UploadLogsWWW OK");
            Debug.Log(www.text);
        }
        else
        {
            Debug.Log("UploadLogsWWW Error");
            Debug.Log(www.error);
            CreateEvent("UploadRequest", "Error");
            Debug.Log("Show a message here instructing the players to zip all the files in this folder and send to pxl@gmail.com");
            string path = GameManager.Instance.GetLinkJava().localPath.TrimEnd(new[] { '\\', '/' }); // Mac doesn't like trailing slash
            System.Diagnostics.Process.Start(path);
            if (allowQuittingRequested)
            {
                Exit_GamePhaseBehavior exitBehaviorCast = GameManager.Instance.exitGameBehavior as Exit_GamePhaseBehavior;
                //string error = "Error uploading content to Server. Please zip content at location" + path + "and email it to pxl@gmail.com";
                exitBehaviorCast.ReportQuitError(Constants.Messages.DisconnectedOnExit);
            }

        }
    }
	
	public struct TrackedEvent{
		public string e;
		public float time;
		public float mouse_x;
		public float mouse_y;
		public string data;
	}
	
	public void MessageEvent(string event_)    {
    	if(tracking_message_collection)
			CreateEvent(event_,"");
	}

	public void MessageEventData(ArrayList event_data)    {
    	if(tracking_message_collection)
			CreateEvent((string)event_data[0],(string)event_data[1]);
	}

	public TrackedEvent CreateEventExt(string tracked_event, string data){
		DebugInfoLabel = tracked_event + " " + data;
		return CreateEvent(tracked_event, data);
	}

	public TrackedEvent CreateEvent(string tracked_event, string data){
		TrackedEvent e;
		e.e = tracked_event;
		e.data = data;
		e.time = Time.time;
        e.mouse_x = GameManager.Instance.GetGridManager().worldCamera.ScreenToWorldPoint(Input.mousePosition).x;
		e.mouse_y = GameManager.Instance.GetGridManager().worldCamera.ScreenToWorldPoint(Input.mousePosition).y;

        string line = System.DateTime.Now.ToString("MM-dd-yy-HH-mm-ss") + "\t" 
			+ e.e + "\t" + e.data + "\t" + e.time + "\t" 
			+ e.mouse_x + "\t" + e.mouse_y;

		if(local_tracking_enabled && log!=null){
			log.WriteLine(line);
		}
		if(log2!=null){
			log2.Append(line+"\n");
		}
        if(modelLog != null)
        {
            modelLog.WriteLine(line);
        }
		return e;
	}

    [ContextMenu("Test Player Modeling Server")]
    public void PlayerModelingServerCall()
    {
        GameManager.Instance.GetLinkJava().StartPlayerModelingServerCall(tracking_session_user, hostname);
    }

    public void ResetModelLog()
    {
        Debug.Log(modelLogPath);
        if (modelLog != null)
        {
            Debug.Log("MODEL LOG IS NOT NULL");
            modelLog.Close();
            modelLog = null;
        }
        modelLogPath = GameManager.Instance.GetLinkJava().localPath + "Session-" + tracking_session_id.ToString() + "-" + System.DateTime.Now.ToString("MM-dd-yy-HH-mm-ss") + "model.log";
        modelLog = File.CreateText(modelLogPath);
        Debug.Log(modelLogPath);
    }

    public void SendModelLog(string executionPath)
    {
        modelLog.Close();
        modelLog = null;
        GameManager.Instance.GetLinkJava().StartPlayerModelingProcess(executionPath, modelLogPath, tracking_session_user, "level"+GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id.ToString(), hostname);
    }

	void Update(){
		if(ready) EventCollection();
	}

	void EventCollection(){
		// Mouse clicks
		for(int i=0;i<=2;i++){
			if(Input.GetMouseButtonDown(i)){
				CreateEvent("MouseDown",i.ToString());

			}
			if(Input.GetMouseButtonUp(i)){
				CreateEvent("MouseUp",i.ToString());
			}
		}
		// Key presses
		foreach(KeyCode i in keys_of_interest){
			if(Input.GetKeyDown(i)){
				CreateEvent("KeyDown",i.ToString());
			}
			if(Input.GetKeyUp(i)){
				CreateEvent("KeyUp",i.ToString());
			}

		}
		mouse_probe_time+=1*Time.deltaTime;
		if(mouse_probe_time>mouse_probe_freq){
			mouse_probe_time=0.0f;
			// Mouse Movement (Distance)
			mouse_position_delta = Vector2.Distance((Vector2)Input.mousePosition, mouse_position_last);
			Vector2 vector = new Vector2(0.0f,0.0f);
			if(mouse_position_delta>mouse_probe_dist_sensitivity){
				CreateEvent("MouseMoveD",mouse_position_delta.ToString());
				mouse_moving = true;
			}
			// Mouse Movement (Direction)
			if(mouse_position_delta>0.0f){
				vector = (Vector2)Input.mousePosition - mouse_position_last;
				vector.Normalize();
				mouse_vector_delta = Vector2.Dot(mouse_vector_last,vector);
				mouse_vector_last = vector;
				if(mouse_vector_delta<mouse_probe_vect_sensitivity){
					CreateEvent("MouseMoveV",mouse_vector_delta.ToString());
					mouse_probe_vect_sensitivity -= 0.2f;
					if (mouse_probe_vect_sensitivity<-1.0f) mouse_probe_vect_sensitivity=-1.0f;
				}
				mouse_moving = true;
			}else{
				mouse_vector_delta = 0.0f;
			}
			// Mouse Movement (Stop)
			if(mouse_position_delta==0.0f && mouse_moving){
				mouse_moving = false;
				CreateEvent("MouseMoveS",mouse_position_delta.ToString());
			}
			// Book keeping
			mouse_probe_vect_sensitivity += 0.02f;
			if (mouse_probe_vect_sensitivity>mouse_probe_vect_sensitivity_max) mouse_probe_vect_sensitivity=mouse_probe_vect_sensitivity_max;
			mouse_position_last = Input.mousePosition;
		}
	}

	void OnGUI() {
		if(ShowDebugInfoLabel){
			GUI.Label(new Rect(2,2,3000,500),DebugInfoLabel);
		}
	}

	public void OnApplicationQuit () {
        Debug.Log("OnApplicationQuit");
		if(allowQuitting){
            Debug.Log("AllowQuit");
			Application.Quit();
		} else {
            Debug.Log("CancelQuit");
			Application.CancelQuit();
			allowQuittingRequested = true;
			CreateEvent("End","Quit");
            StartCoroutine(SaveLogs());
		}
	}

    public void TriggerForceQuit()
    {
        allowQuitting = true;
        OnApplicationQuit();
    }
}
