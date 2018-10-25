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
    public Outline outline;
    public Image fill;

    [System.Serializable]
    public class ButtonImageSpriteReference
    { public Sprite requiredComplete, requiredIncomplete, optionalComplete, optionalIncomplete; }

    [SerializeField] public ButtonImageSpriteReference buttonImageSpriteReference;

    public void SetLevelSprite(bool isPCG, bool isComplete)
    {
        if (isPCG)
        {
            if (isComplete)
            {
                fill.color = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
                outline.effectColor = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
                levelName.color = new Color(205f / 255f, 89f / 255f, 40f / 255f, 1f);
            }
            else
            {
                fill.color = new Color(61f / 255f, 81f / 255f, 85f / 255f, 1f);
                outline.effectColor = new Color(205f / 255f, 89f / 255f, 40f / 255f, 1f);
                levelName.color = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
            }
        }
        else
        {
            if (isComplete)
            {
                fill.color = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
                outline.effectColor = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
                levelName.color = new Color(138f / 255f, 209f / 255f, 217f / 255f, 1f);
            }
            else
            {
                fill.color = new Color(61f / 255f, 81f / 255f, 85f / 255f, 1f);
                outline.effectColor = new Color(138f / 255f, 209f / 255f, 217f / 255f, 1f);
                levelName.color = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
            }
        }
    }

    public void SetLevelRank(int rank)
    {
        goalRegion.star01.sprite = rank >= 1 ? goalRegion.fillStarReference : goalRegion.emptyStarReference;
        goalRegion.star02.sprite = rank >= 2 ? goalRegion.fillStarReference : goalRegion.emptyStarReference;
        goalRegion.star03.sprite = rank >= 3 ? goalRegion.fillStarReference : goalRegion.emptyStarReference;
    }
}
