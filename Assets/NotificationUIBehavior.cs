using UnityEngine;
using System.Collections;
using UnityEngine.UI;

///
/// Notification UI Behavior Script
/// 
/// Script for displaying notifications on the UI
/// Contains Display() for safe global usage and UpdateUI() for updating the UI instance
/// This script should be attatched to Notification_UI
/// 

public class NotificationUIBehavior : MonoBehaviour
{
    // Private Fields
    private static NotificationUIBehavior instance;

    private GameObject canvas;
    private GameObject obj_notification;
    private GameObject obj_title;
    private GameObject obj_description;
    private GameObject obj_button_container;
    private GameObject[] obj_button_layouts;

    private Text title;
    private Text description;

    private void Start()
    {
        instance = this;

        // UI Objects
        canvas = transform.GetChild(0).gameObject;
        obj_notification = canvas.transform.GetChild(1).gameObject;
        obj_title = obj_notification.transform.GetChild(0).gameObject;
        obj_description = obj_notification.transform.GetChild(1).gameObject;
        obj_button_container = obj_notification.transform.GetChild(2).gameObject;

        // UI Layouts for 1, 2, or 3 Buttons
        obj_button_layouts = new GameObject[obj_button_container.transform.childCount];
        for (int i = 0; i < obj_button_layouts.Length; i++)
        {
            obj_button_layouts[i] = obj_button_container.transform.GetChild(i).gameObject;
        }

        // UI Text Components
        title = obj_title.GetComponent<Text>();
        description = obj_description.GetComponent<Text>();
    }

    /// 
    /// Notification UI Methods
    /// void Display(UINotifications.Notification)
    /// void UpdateUI(UINotifications.Notification)
    /// 

    // Display Notification if UI Exists
    public static void Display(UINotifications.Notification notification)
    {
        if (instance != null)
        {
            instance.UpdateUI(notification);
        }
    }

    // Update Notification UI based on Notification object
    private void UpdateUI(UINotifications.Notification notification)
    {
        // Update Notification Text
        title.text = notification.title;
        description.text = notification.description;

        // Select right button layout UI based on number of buttons
        int button_count = notification.GetButtonCount();
        GameObject button_layout = obj_button_layouts[button_count - 1];

        // Turn off all button layouts
        for (int i = 0; i < obj_button_layouts.Length; i++)
        {
            obj_button_layouts[i].SetActive(false);
        }

        // Turn on right button layout 
        obj_button_layouts[button_count - 1].SetActive(true);

        // Set up each button
        for (int i = 0; i < button_count; i++)
        {
            // Get this button
            GameObject obj_button = button_layout.transform.GetChild(i).gameObject;
            Button button = obj_button.GetComponent<Button>();
            
            // Attach this button method for onclick functionality
            int index = i;
            button.onClick.AddListener(delegate { notification.GetButtonMethod(index)(); });

            // Update this button label
            GameObject obj_text = obj_button.transform.GetChild(0).gameObject;
            Text text = obj_text.GetComponent<Text>();
            text.text = notification.GetButtonName(i);
        }

        // Display UI Canvas
        canvas.SetActive(true);
    }
}
