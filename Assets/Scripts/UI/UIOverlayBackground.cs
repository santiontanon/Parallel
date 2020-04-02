using System.Collections;
using System.Collections.Generic;
using UnityEngine.UI;
using UnityEngine;

public class UIOverlayBackground : MonoBehaviour
{
    private CanvasGroup canvGroup;
	
	[HideInInspector]
	public float targetAlpha = 0f;
	
	private bool isFading = false;
	
	private void Awake() {
		this.canvGroup = this.GetComponent<CanvasGroup>();
		this.targetAlpha = 0f;
		this.canvGroup.gameObject.SetActive(false);
		this.isFading = false;
	}
	
	private void Update() {
		if (!this.isFading) {
			this.ToggleObject();
		}
		this.OverlayFade();
	}
	
	private void ToggleObject() {
		if (this.canvGroup.alpha == 0f && this.canvGroup.gameObject.active) {
			this.canvGroup.gameObject.SetActive(false);
		}
	}
	
	private void OverlayFade() {
		if (this.canvGroup.alpha != targetAlpha) {
			if (this.canvGroup.alpha < targetAlpha) {
				this.canvGroup.alpha += Time.deltaTime * 4f;
			} else {
				this.canvGroup.alpha -= Time.deltaTime * 4f;
			}				
		} else {
			this.isFading = false;
		}
	}
	
	public void SetTargetAlpha(float val) {
		this.canvGroup.gameObject.SetActive(true);
		this.isFading = true;
		this.targetAlpha = val;
	}	
	
	public void SetAlpha(float val) {
		this.canvGroup.alpha = val;
		if (this.isFading) {
			this.isFading = false;
		}
	}
}
