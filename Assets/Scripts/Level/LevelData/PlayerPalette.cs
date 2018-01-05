using UnityEngine;
using System.Collections;

[System.Serializable]
public class PlayerPalette {
	//{"semaphore":{"count":3},"signal":{"count":3},"painter":{"count":0},"colors":{"count":0}}
	[System.Serializable]
	public class PaletteItem
	{
		public int count = 0;	
	}

	public PaletteItem semaphore;
	public PaletteItem signal;
	public PaletteItem painter;
	public PaletteItem colors;
}
