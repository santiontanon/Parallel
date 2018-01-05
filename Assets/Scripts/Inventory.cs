using UnityEngine;
using System;
using System.Collections.Generic;
using UnityEngine.UI;
using UnityEngine.Events;

[System.Serializable]
public class Inventory
{
	public int levelNumber;
	public List<Tool> availableTools;

	public Inventory () {
		availableTools = new List<Tool> ();

		FillInventory ();
	}

	public void ClearInventory() {
		availableTools.Clear ();
		Debug.Log ("Inventory Cleared");
	}

	public void FillInventory() {
		foreach (Tool t in Enum.GetValues(typeof(Tool))) {
			availableTools.Add (t); 
		}
		Debug.Log ("Inventory Filled: " + toString(availableTools));
	}

	public void RemoveTool(Tool t) {
		if(availableTools.Contains(t))
			availableTools.Remove (t);
		
		Debug.Log ("Tool, " + (int) t + ", removed");
	}

	public void AddTool(Tool t) {
		if(!availableTools.Contains(t))
			availableTools.Add (t);

		Debug.Log ("Tool, " + (int) t + ", Added");
	}

	public string toString(List<Tool> tools) {
		string v = "";

		foreach (Tool t in tools) {
			v += t.ToString () + ",";
		}

		return v;
	}
}

public enum Tool {
	Signal,
	Semaphore,
	Link_Viewer,
	Help_Glossary,
	Flow_Arrows,
	Submit_Button
}
