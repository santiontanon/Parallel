using UnityEngine;
using System.Collections;
using System.Collections.Generic;

///
/// UI Notifications Script
/// 
/// Script for building new notifications and interfacing them to the UI
/// Contains the Notification class, ButtonMethod type for notification buttons, and Notify() for updating the UI the globally
/// This script should not by instantiated or attached to a game object
/// 

public class UINotifications : MonoBehaviour
{
    // Delegate for making buttons functional
    public delegate void ButtonMethod();

    // Send Notification to UI
    public static void Notify(Notification notification)
    {
        NotificationUIBehavior.Display(notification);
    }

    ///
    /// Notification Class
    /// 
    /// Class for making a new notification
    /// Supports a title, description, and a maximum of 3 buttons
    /// Each button has a display name and a method on click
    /// Notifications are used by UINotifications.Notify() to display via the UI
    /// 

    public class Notification
    {
        // Public Fields
        public string title;
        public string description;

        // Private Fields
        private List<string> button_names;
        private List<ButtonMethod> button_methods;
        private int buttons_max = 3;

        // Default Constructor
        public Notification()
        {
            this.title = "Title";
            this.description = "description";
            this.button_names = new List<string>();
            this.button_methods = new List<ButtonMethod>();
        }

        // Title/Description Constructor
        public Notification(string title, string description)
        {
            this.title = title;
            this.description = description;
            this.button_names = new List<string>();
            this.button_methods = new List<ButtonMethod>();
        }

        /// 
        /// Notification Methods
        /// 
        /// AddButton(string, ButtonMethod) : void
        /// GetButtonCount() : int
        /// GetButtonMethod() : ButtonMethod
        /// GetButtonName() : string
        /// RemoveButton(int) : void
        /// SetButton(int, string, ButtonMethod) : void
        /// 

        // Add New Button
        public void AddButton(string button_name, ButtonMethod button_method)
        {
            // Add new button name and method if less than the max
            if (button_names.Count < buttons_max)
            {
                button_names.Add(button_name);
                button_methods.Add(button_method);
            }
        }

        // Get Number of Buttons
        public int GetButtonCount()
        {
            return button_names.Count;
        }

        // Get Button Method
        public ButtonMethod GetButtonMethod(int button_number)
        {
            // If button number is in range, return method
            if (button_number >= 0 && button_number < buttons_max)
            {
                return button_methods[button_number];
            }

            // Else, return nothing
            return null;
        }

        // Get Button Name
        public string GetButtonName(int button_number)
        {
            // If button number is in range, return name
            if (button_number >= 0 && button_number < buttons_max)
            {
                return button_names[button_number];
            }

            // Else, return nothing
            return null;
        }

        // Remove Exisiting Button
        public void RemoveButton(int button_number)
        {
            // If button number is in range, remove it
            if (button_number >= 0 && button_number < buttons_max)
            {
                // Remove button
                button_names.RemoveAt(button_number);
                button_methods.RemoveAt(button_number);
            }
        }

        // Overwrite Existing Button
        public void SetButton(int button_number, string button_name, ButtonMethod button_method)
        {
            // If button number is in range, overwrite it
            if (button_number >= 0 && button_number < buttons_max)
            {
                // Remove old button
                button_names.RemoveAt(button_number);
                button_methods.RemoveAt(button_number);

                // Insert new button
                button_names.Insert(button_number, button_name);
                button_methods.Insert(button_number, button_method);
            }
        }
    }
}
