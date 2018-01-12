using UnityEngine;
using System.Collections;
using UnityEngine.UI;

public class LevelButtonBehavior : MonoBehaviour {
    [System.Serializable]public class GoalRegion
    {
        [Header("Rank Elements in Prefab")]
        public Image star01;
        public Image star02;
        public Image star03;
        [Header("Rank Sprite References")]
        public Sprite fillStarReference;
        public Sprite emptyStarReference;
    }
    [SerializeField] public GoalRegion goalRegion;
    public Text levelName;
    public Image buttonImage;

    [System.Serializable]
    public class ButtonImageSpriteReference
    { public Sprite requiredComplete, requiredIncomplete, optionalComplete, optionalIncomplete; }

    [SerializeField] public ButtonImageSpriteReference buttonImageSpriteReference;

    public void SetLevelSprite(bool isRequired, bool isComplete)
    {
        if (isRequired && isComplete) buttonImage.sprite = buttonImageSpriteReference.requiredComplete;
        if (isRequired && !isComplete) buttonImage.sprite = buttonImageSpriteReference.requiredIncomplete;
        if (!isRequired && isComplete) buttonImage.sprite = buttonImageSpriteReference.optionalComplete;
        if (!isRequired && !isComplete) buttonImage.sprite = buttonImageSpriteReference.optionalIncomplete;
    }

    public void SetLevelRank(int rank)
    {
        goalRegion.star01.sprite = rank >= 1 ? goalRegion.fillStarReference : goalRegion.emptyStarReference;
        goalRegion.star02.sprite = rank >= 2 ? goalRegion.fillStarReference : goalRegion.emptyStarReference;
        goalRegion.star03.sprite = rank >= 3 ? goalRegion.fillStarReference : goalRegion.emptyStarReference;
    }
}
