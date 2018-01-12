using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using System.Collections.Generic;
using UnityEngine.EventSystems;

public class InventoryManager : MonoBehaviour {
	/// <summary>
	/// The currently active Inventory
	/// </summary>
	Inventory currentInventory;
	[HideInInspector]
	public List<Tool> availableTools;
	public Inventory[] LevelInventories;

	public ToolLookup[] ToolArray = new ToolLookup[0];

	/// <summary>
	/// Adds a tool to the user's inventory
	/// </summary>
	/// <param name="t">The tool.</param>
	public void AddTool(Tool t) {
		if (availableTools == null)
			return;

		if (!availableTools.Contains (t)) {
			availableTools.Add (t);
		}
	}

	/// <summary>
	/// Removes a tool from the user's inventory
	/// </summary>
	/// <param name="t">The tool.</param>
	public void RemoveTool(Tool t) {
		if (availableTools == null)
			return;

		if (availableTools.Contains (t)) {
			availableTools.Remove (t);
		}
	}

	/// <summary>
	/// Sets a tool's status to active
	/// </summary>
	/// <param name="t">The tool,</param>
	public void ActivateTool(Tool t) {
		for (int i = 0; i < ToolArray.Length; i++) {
			if (ToolArray [i].tool == t) {
				ToolArray [i].toolInstance.SetActive (true);
			}
		}
	}

	/// <summary>
	/// Sets a tool's status to inactive
	/// </summary>
	/// <param name="t">The tool.</param>
	public void DeactivateTool(Tool t) {
		for (int i = 0; i < ToolArray.Length; i++) {
			if (ToolArray [i].tool == t) {
				ToolArray [i].toolInstance.SetActive (false);
			}
		}
	}

	/// <summary>
	/// Activates all tools in the current inventory.
	/// </summary>
	public void ActivateCurrentInventory() {
		if (availableTools == null)
			return;

		foreach (Tool t in availableTools) {
			ActivateTool (t);
		}
	}

	/// <summary>
	/// Deactivates all tools in the current inventory.
	/// </summary>
	public void DeactivateCurrentInventory() {
		if (availableTools == null)
			return;

		foreach (Tool t in availableTools) {
			DeactivateTool (t);
		}
	}

	/// <summary>
	/// Used for the initial level setup of the inventory.
	/// </summary>
	/// <param name="id">Identifier.</param>
	public void SetUpLevelInventory(int id) {
		availableTools = new Inventory ().availableTools;

		DeactivateCurrentInventory ();

		availableTools = GetInventoryByLevelID (id).availableTools;

		ActivateCurrentInventory ();
	}

	/// <summary>
	/// Returns an Inventory given an ID. Returns a full inventory if one is not found for the ID.
	/// </summary>
	/// <returns>The inventory by level ID.</returns>
	/// <param name="id">Level identifier.</param>
	public Inventory GetInventoryByLevelID(int id) {
		Inventory inv = new Inventory ();

		for (int i = 0; i < LevelInventories.Length; i++) {
			Debug.Log ("TESTING !!! ----- " + id + " " + LevelInventories[i].levelNumber);
			if (LevelInventories [i].levelNumber == id) {
				inv = LevelInventories [i];
				break;
			}
		}

		Debug.Log ("Inventory gotten: " + inv.toString(inv.availableTools));

		return inv;
	}

	/// <summary>
	/// Updates the inventory UI based on the currently active / available tools.
	/// </summary>
	public void UpdateInventoryUI() {
		if (availableTools == null)
			return;
		
		for (int i = 0; i < ToolArray.Length; i++) {
			if (availableTools.Contains (ToolArray [i].tool)) {
				ActivateTool (ToolArray [i].tool);
			} else {
				DeactivateTool (ToolArray [i].tool);
			}
		}
	}
}

[System.Serializable]
public class ToolLookup {
	public Tool tool;
	public GameObject toolInstance;
}
