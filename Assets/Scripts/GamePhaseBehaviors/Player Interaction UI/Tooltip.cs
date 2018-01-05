using UnityEngine;
using System.Collections;
using UnityEngine.EventSystems;

[System.Serializable]
public class Tooltip
{
	public string tooltipText;
}

[System.Serializable]
public class TooltipEvent 
{
	public EventTrigger tooltipUiElement;
    public bool permanent;
    public string refString;
	[SerializeField] public Tooltip tooltipContent;
}