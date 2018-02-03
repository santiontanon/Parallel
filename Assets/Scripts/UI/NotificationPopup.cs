using UnityEngine;
using System.Collections;
using ParallelProg.UI;
using UnityEngine.UI;

public class NotificationPopup : MonoBehaviour {

    [SerializeField]
    GameObject buttonPrefab;
    [SerializeField]
    Transform buttonContainer;
    [SerializeField]
    Button exitButton;
    [SerializeField]
    Button[] buttons;
    [SerializeField]
    Text title;
    [SerializeField]
    Text body;

    public void SetNotification(string _title, string _body, bool enableExit, UnityEngine.Events.UnityAction[] actions)
    {
        title.text = _title;
        body.text = _body;
        exitButton.gameObject.SetActive(enableExit);

        buttons = new Button[actions.Length];
        for(int i = 0; i < actions.Length; i++)
        {
            int index = i;
            GameObject temp = GameObject.Instantiate(buttonPrefab);
            buttons[index] = temp.GetComponent<Button>();
            buttons[index].onClick.AddListener(actions[index]);
        }
    }

}
