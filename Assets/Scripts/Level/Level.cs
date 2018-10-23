using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class Level {
	//public Metadata metadata;
	public Metadata metadata = new Metadata();
	public List<string> metadataList;
	public List<string> layoutList;
	public List<string> colorList;
	public List<string> directionList;
	public List<string> componentList;
	public List<string> executionList;
    public List<string> skillList;

	public List<GridComponent> components;
	public List<GridTrack> tracks;
	public List<StepData> execution;

	public string colors;

	public Level()
	{
		
	}

}
