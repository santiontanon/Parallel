using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class GridTrack 
{
	public char type;
	public Vector2 position;
	public string colorAllowanceKey;
	public bool isOccupied = false;
	public List<int> colorBitmask = new List<int>();
	public string direction;

	public void SetBitmask(string inputColorKey)
	{
		colorAllowanceKey = inputColorKey;
		int inputColorKey_ = ((int) inputColorKey[0])-((int)' ');
		for (int i = 0; i <= 6; i++) {
			if (inputColorKey_ == 0) {
				colorBitmask.Add (i);
			} else {
				if (((1 << i) & inputColorKey_) != 0) {
					colorBitmask.Add (i+1);
				}
			}
		
		}
	}

	public bool CanUsePath(int trainColor)
	{
		if( colorBitmask.Contains(trainColor) )
		{
			return true;
		}
		else 
		{
			return false;
		}
	}
}