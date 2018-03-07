using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections.Generic;
[System.Serializable]
public class HintConstructor {
    public string hint_component_type;
	public string hintTitle;
	public string hintDescription;
	public Sprite hintImage;
	public Sprite hintButtonImage;
}

[System.Serializable]
public class HintButton 
{
	public List<int> levelIds = new List<int>();
	public HintConstructor hint;
}