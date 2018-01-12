/*
    Serializable class for holding level scoring data
    index - the id of the level the data pertains to
    completed - has the player finished the level sucessfully
    attemptCount - how many times did the player submit a solution
    stepCount - how many execution steps were in the final solution
*/

using UnityEngine;
using System.Collections;

[System.Serializable]
public class LevelScore {

    public int index;

    public bool completed;

    public int attemptCount;

    public int stepCount;

}
