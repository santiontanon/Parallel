using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using ParallelProg.UI;

public class Start_DebugUI : MonoBehaviour
{
    bool isOpen;
    [SerializeField]
    Dropdown JVMMemoryDropdown;
    [SerializeField]
    Toggle localToggle;
    [SerializeField]
    Toggle remoteToggle;
    [SerializeField]
    InputField ipInput;
    [SerializeField]
    Image connectedImage;
    [SerializeField]
    Color successColor;
    [SerializeField]
    Color failureColor;

    void Start() {
        PopulateJVMMemoryList();
        ToggleDebugUI(false);
        JVMMemoryDropdown.value = GameManager.Instance.JVMMemorySelection;
    }

    public void ToggleDebugUI(bool force)
    {
        isOpen = force;
        UpdateDebugUIVisibility();
    }
    public void ToggleDebugUI()
    {
        isOpen = !isOpen;
        UpdateDebugUIVisibility();
    }
    void PopulateJVMMemoryList()
    {
        JVMMemoryDropdown.ClearOptions();
        JVMMemoryDropdown.AddOptions( new List<string>(Constants.JVMSettings.MemoryAllocation));
    }
    void UpdateDebugUIVisibility()
    {
        gameObject.SetActive(isOpen);
    }

    public void ReportJVMMemoryAllocationChange()
    {
        int selection = JVMMemoryDropdown.value;
        Debug.Log("Updating JVM Selection To: " + selection);
        GameManager.Instance.JVMMemorySelection = selection;
    }

    public void UpdateIPAddress()
    {

    }

    public void CheckConnection()
    {

    }

    public void SaveSettings()
    {
        ReportJVMMemoryAllocationChange();
        GameManager.Instance.tracker.ChangeRemoteAddress(ipInput.text);
        GameManager.Instance.tracker.UpdateTracking(localToggle.isOn, remoteToggle.isOn);
    }

    public void ConnectionSuccess(bool success)
    {
        if (success)
            connectedImage.color = successColor;
        else
            connectedImage.color = failureColor;
    }
}
