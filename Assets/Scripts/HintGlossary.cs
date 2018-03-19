using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class HintGlossary : MonoBehaviour
{
    [SerializeField] HintConstructor[] hints;
    
    public HintConstructor GetHintForComponent(string componentName, out bool success)
    {
        HintConstructor h = new HintConstructor();
        success = false;
        foreach (HintConstructor hint in hints)
        {
            if (hint.hint_component_type == componentName)
            {
                h = hint;
                success = true;
            }
        }
        return h;
    }
}
