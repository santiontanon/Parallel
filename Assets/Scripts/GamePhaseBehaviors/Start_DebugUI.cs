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
    InputField trackingID;
    [SerializeField]
    Color successColor;
    [SerializeField]
    Color failureColor;

    void Start() {
        PopulateJVMMemoryList();
        ToggleDebugUI(false);
        JVMMemoryDropdown.value = GameManager.Instance.JVMMemorySelection;
        ipInput.placeholder.GetComponent<Text>().text = GameManager.Instance.tracker.url;
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
        GameManager.Instance.tracker.ChangeRemoteAddress(ipInput.text, CheckConnection);
    }

    public void UpdateTracking()
    {
        GameManager.Instance.tracker.UpdateTracking(localToggle.isOn, remoteToggle.isOn);
    }

    public void CheckConnection()
    {
        trackingID.text = GameManager.Instance.tracker.session_id;
        if (trackingID.text != "NA")
            connectedImage.color = successColor;
        else
            connectedImage.color = failureColor;
    }

    public void SaveSettings()
    {
        ReportJVMMemoryAllocationChange();

        GameManager.Instance.tracker.UpdateTracking(localToggle.isOn, remoteToggle.isOn);
    }
}
