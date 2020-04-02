using UnityEngine;
using System.Collections;

namespace ParallelProg.UI
{
    [System.Serializable]
    public class UIOverlay
    {
        public RectTransform panelContainer;
        public bool isOpen;
		
		public virtual void Update() {
		}

        public virtual void OpenPanel()
        {
 //         iTween.Stop(panelContainer.gameObject);
            panelContainer.gameObject.SetActive(true);
 /*           if (isOpen || iTween.Count(panelContainer.gameObject) > 0)
            {
                panelContainer.gameObject.transform.localScale = Vector3.one;
                isOpen = true;
            }
            else
            {*/
                isOpen = true;
                panelContainer.gameObject.transform.localScale = Vector3.zero;
                iTween.ScaleTo(panelContainer.gameObject, iTween.Hash("scale", Vector3.one, "time", 0.25f));
        //    }
        }

        public virtual void ClosePanel(bool forceClose = false)
        {
            iTween.Stop(panelContainer.gameObject);
            isOpen = false;
            if (forceClose)
            {
                panelContainer.gameObject.SetActive(false);
                panelContainer.transform.localScale = Vector3.zero;
            }
            else
            {
                iTween.ScaleTo(panelContainer.gameObject, iTween.Hash("scale", Vector3.zero, "time", 0.25f));
            }
        }
    }

	[System.Serializable] 
	public class UIMeter 
	{
		public RectTransform meterContainer;
		public UnityEngine.UI.Image meter_foreground;
		public UnityEngine.UI.Image meter_background;
		/// <summary>
		/// Sets the meter value.
		/// </summary>
		/// <param name="inputValue">Input value goes from 0 to 1.</param>
		public void OpenMeter(){meterContainer.gameObject.SetActive(true);}
		public void CloseMeter(){meterContainer.gameObject.SetActive(false);}
		public void SetMeterValue( float inputValue, float inputLerpTime = 0f)
		{
			if(meter_foreground == null) return;
			inputValue = Mathf.Clamp( inputValue, 0f, 1f);
			if( inputLerpTime == 0f ) meter_foreground.fillAmount = inputValue;
			else 
			{
				//TODO: LERP
				meter_foreground.fillAmount = inputValue;
			}
		}

	}
}
