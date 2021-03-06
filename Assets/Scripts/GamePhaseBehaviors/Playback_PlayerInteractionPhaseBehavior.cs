﻿using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Playback_PlayerInteractionPhaseBehavior : MonoBehaviour {

    [SerializeField]
    PlayerInteraction_GamePhaseBehavior playerInteraction;

    [SerializeField]
    Dictionary<int, List<StepData>> stepDictionary;

    [SerializeField]
    List<TimeStepData> timeSteps;

    [SerializeField]
    int currentStep = 0;
    
    public bool paused;

    //temp variables to resolve goal screen issues
    public bool success; //was the solution successful

    public void StartPhase()
    {
        //reset values
        //start parse steps
        StartCoroutine(ParseSteps());
        playerInteraction.playerInteraction_UI.playbackButton.sprite = playerInteraction.playerInteraction_UI.playbackButtonSprites[0];
    }

    public void EndPhase()
    {
        //end all coroutines
        StopAllCoroutines();
        //reset values
        currentStep = 0;
        playerInteraction.playerInteraction_UI.playbackSlider.value = 0;
        paused = false;
        playerInteraction.playerInteraction_UI.playbackButton.sprite = playerInteraction.playerInteraction_UI.playbackButtonSprites[0];
    }

    public void PhaseUpdate()
    {

    }

    IEnumerator ParseSteps()
    {
        playerInteraction.playerInteraction_UI.loadingText.text = "Parsing...";
        currentStep = 0;
        paused = false;
        Level lvl = GameManager.Instance.GetDataManager().currentLevelData;
        if(lvl.execution.Count > 0)
        {
            int maxStep = 0;
            stepDictionary = new Dictionary<int, List<StepData>>();
            Dictionary<int, List<int>> componentStepsDictionary = new Dictionary<int, List<int>>();
            timeSteps = new List<TimeStepData>();
            List<GridComponent> threads = new List<GridComponent>();

            foreach (GridComponent g in lvl.components)
            {
                if (g.type == "thread")
                {
                    threads.Add(g);
                }
            }

            yield return new WaitForSeconds(1f);
            System.Diagnostics.Stopwatch stopwatch = new System.Diagnostics.Stopwatch();
            stopwatch.Start();
            for (int i = 0; i < lvl.execution.Count; i++)
            {
                StepData step = lvl.execution[i];

                if (step.timeStep > maxStep)
                {
                    maxStep = step.timeStep;
                }

                if (step.eventType == "M")
                {
                    if (!componentStepsDictionary.ContainsKey(step.componentID)) { componentStepsDictionary.Add(step.componentID, new List<int>()); componentStepsDictionary[step.componentID].Add(i); }
                    else { componentStepsDictionary[step.componentID].Add(i); }
                }

                if (stepDictionary.ContainsKey(step.timeStep))
                {
                    if (step.eventType == "D")
                    {
                        stepDictionary[step.timeStep].Insert(0, step);
                    }
                    else
                    {
                        stepDictionary[step.timeStep].Add(step);
                    }
                }
                else
                {
                    stepDictionary[step.timeStep] = new List<StepData>();
                    stepDictionary[step.timeStep].Add(step);
                }
                if(stopwatch.ElapsedMilliseconds > 1000 / 60)
                {
                    stopwatch.Stop();
                    stopwatch.Reset();
                    yield return new WaitForEndOfFrame();
                    stopwatch.Start();
                }
            }

            //create in between steps for thread movements
            for (int i = 0; i <= maxStep; i++)
            {
                if (stepDictionary.ContainsKey(i) == false)
                    stepDictionary.Add(i, new List<StepData>());
                if (stepDictionary[i] == null)
                    stepDictionary[i] = new List<StepData>();
                if (stepDictionary[i].Count == 0)
                {
                    for (int j = 0; j < threads.Count; j++)
                    {
                        Vector2 prevPos = new Vector2();
                        if (i > 0)
                        {
                            for (int k = 0; k < stepDictionary[i - 1].Count; k++)
                            {
                                if (stepDictionary[i - 1][k].eventType == "M" && stepDictionary[i - 1][k].componentID == threads[j].id)
                                {
                                    prevPos = stepDictionary[i - 1][k].componentPos;
                                }
                            }
                        }
                        Vector2 nextPos = prevPos;
                        bool end = false;
                        for (int k = i + 1; k < maxStep+1; k++)
                        {
                            if (stepDictionary.ContainsKey(k))
                            {
                                for (int l = 0; l < stepDictionary[k].Count; l++)
                                {
                                    if (stepDictionary[k][l].eventType == "M" && stepDictionary[k][l].componentID == threads[j].id)
                                    {
                                        if (stepDictionary[k][l].componentPos != prevPos)
                                        {
                                            if (Vector2.Distance(prevPos, stepDictionary[k][l].componentPos) > 1 || Vector2.Distance(prevPos, stepDictionary[k][l].componentPos) < -1)
                                            {
                                                if (k - Mathf.Abs(Vector2.Distance(prevPos, stepDictionary[k][l].componentPos)) == i - 1)
                                                {
                                                    if (prevPos.x != stepDictionary[k][l].componentPos.x)
                                                    {
                                                        if (prevPos.x > stepDictionary[k][l].componentPos.x)
                                                        {
                                                            nextPos = new Vector2(nextPos.x - 1, nextPos.y);
                                                        }
                                                        else
                                                        {
                                                            nextPos = new Vector2(nextPos.x + 1, nextPos.y);
                                                        }
                                                    }
                                                    else
                                                    {
                                                        if (prevPos.y > stepDictionary[k][l].componentPos.y)
                                                        {
                                                            nextPos = new Vector2(nextPos.x, nextPos.y - 1);
                                                        }
                                                        else
                                                        {
                                                            nextPos = new Vector2(nextPos.x, nextPos.y + 1);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        end = true;
                                        break;
                                    }
                                }
                                if (end == true)
                                {
                                    break;
                                }
                            }
                        }

                        StepData newStep = new StepData();
                        newStep.componentID = threads[j].id;
                        newStep.componentPos = nextPos;
                        newStep.eventType = "M";
                        newStep.timeStep = i;

                        stepDictionary[i].Add(newStep);
                    }

                }
                else
                {
                    List<bool> hasTimeStepData = new List<bool>();
                    for (int j = 0; j < threads.Count; j++)
                    {
                        hasTimeStepData.Add(false);
                    }

                    for (int j = 0; j < stepDictionary[i].Count; j++)
                    {
                        if (stepDictionary[i][j].eventType == "M")
                        {
                            for (int k = 0; k < threads.Count; k++)
                            {
                                if (threads[k].id == stepDictionary[i][j].componentID)
                                {
                                    hasTimeStepData[k] = true;
                                }
                            }
                        }
                    }

                    for (int j = 0; j < hasTimeStepData.Count; j++)
                    {
                        if (hasTimeStepData[j] == false)
                        {
                            Vector2 prevPos = new Vector2();
                            for (int k = 0; k < stepDictionary[i - 1].Count; k++)
                            {
                                if (stepDictionary[i - 1][k].eventType == "M" && stepDictionary[i - 1][k].componentID == threads[j].id)
                                {
                                    prevPos = stepDictionary[i - 1][k].componentPos;
                                }
                            }
                            Vector2 nextPos = prevPos;
                            bool end = false;
                            for (int k = i + 1; k < maxStep+1; k++)
                            {
                                if (stepDictionary.ContainsKey(k))
                                {
                                    for (int l = 0; l < stepDictionary[k].Count; l++)
                                    {
                                        if (stepDictionary[k][l].eventType == "M" && stepDictionary[k][l].componentID == threads[j].id)
                                        {
                                            if (stepDictionary[k][l].componentPos != prevPos)
                                            {
                                                if (Vector2.Distance(prevPos, stepDictionary[k][l].componentPos) > 1 || Vector2.Distance(prevPos, stepDictionary[k][l].componentPos) < -1)
                                                {
                                                    if (k - Mathf.Abs(Vector2.Distance(prevPos, stepDictionary[k][l].componentPos)) == i - 1)
                                                    {
                                                        if (prevPos.x != stepDictionary[k][l].componentPos.x)
                                                        {
                                                            if (prevPos.x > stepDictionary[k][l].componentPos.x)
                                                            {
                                                                nextPos = new Vector2(nextPos.x - 1, nextPos.y);
                                                            }
                                                            else
                                                            {
                                                                nextPos = new Vector2(nextPos.x + 1, nextPos.y);
                                                            }
                                                        }
                                                        else
                                                        {
                                                            if (prevPos.y > stepDictionary[k][l].componentPos.y)
                                                            {
                                                                nextPos = new Vector2(nextPos.x, nextPos.y - 1);
                                                            }
                                                            else
                                                            {
                                                                nextPos = new Vector2(nextPos.x, nextPos.y + 1);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            end = true;
                                            break;
                                        }
                                    }
                                    if (end == true)
                                    {
                                        break;
                                    }
                                }
                            }

                            StepData newStep = new StepData();
                            newStep.componentID = threads[j].id;
                            newStep.componentPos = nextPos;
                            newStep.eventType = "M";
                            newStep.timeStep = i;

                            stepDictionary[i].Add(newStep);
                        }
                    }
                }
                if (stopwatch.ElapsedMilliseconds > 1000 / 60)
                {
                    stopwatch.Stop();
                    stopwatch.Reset();
                    yield return new WaitForEndOfFrame();
                    stopwatch.Start();
                }
            }

            yield return new WaitForSeconds(1f);
            TimeStepData timeStep = new TimeStepData();
            for (int i = 0; i < lvl.components.Count; i++)
            {
                switch (lvl.components[i].type)
                {
                    case "thread":
                        ThreadData thread = new ThreadData();
                        thread.id = lvl.components[i].id;
                        thread.pos = Vector2.zero;
                        switch (lvl.components[i].configuration.initial_direction)
                        {
                            case "North": thread.rotation = new Vector3(0, 0, 90); break;
                            case "South": thread.rotation = new Vector3(0, 0, -90); break;
                            case "East": thread.rotation = new Vector3(0, 0, 0); break;
                            case "West": thread.rotation = new Vector3(0, 0, 180); break;
                        }
                        timeStep.threads.Add(thread);
                        break;

                    case "pickup":
                        PickupData pickup = new PickupData();
                        pickup.id = lvl.components[i].id;
                        pickup.available = lvl.components[i].configuration.value;
                        timeStep.pickups.Add(pickup);
                        break;

                    case "delivery":
                        DeliveryData delivery = new DeliveryData();
                        delivery.id = lvl.components[i].id;
                        delivery.deliveries = 0;
                        timeStep.deliveryPoints.Add(delivery);
                        break;

                    case "semaphore":
                        SemaphoreData semaphore = new SemaphoreData();
                        semaphore.id = lvl.components[i].id;
                        semaphore.open = lvl.components[i].configuration.value;
                        timeStep.sempahores.Add(semaphore);
                        break;

                    case "conditional":
                        ConditionalData conditional = new ConditionalData();
                        conditional.id = lvl.components[i].id;
                        conditional.current = lvl.components[i].configuration.current;
                        conditional.directions = lvl.components[i].configuration.directions;
                        timeStep.conditionals.Add(conditional);
                        break;

                    case "signal":
                        SignalData signal = new SignalData();
                        signal.id = lvl.components[i].id;
                        signal.passed = lvl.components[i].configuration.passed;
                        timeStep.signals.Add(signal);
                        break;

                }
                if (stopwatch.ElapsedMilliseconds > 1000 / 60)
                {
                    stopwatch.Stop();
                    stopwatch.Reset();
                    yield return new WaitForEndOfFrame();
                    stopwatch.Start();
                }
            }

            yield return new WaitForSeconds(1f);
            for (int i = 0; i < stepDictionary.Count; i++)
            {
                if (i != 0)
                {
                    timeStep = timeStep.Copy(timeSteps[i - 1]);
                    timeStep.timeStep = i;
                    timeStep.previousStep = timeSteps[i - 1];
                    timeSteps[i - 1].nextStep = timeStep;
                }
                for (int j = 0; j < stepDictionary[i].Count; j++)
                {
                    switch (stepDictionary[i][j].eventType)
                    {
                        case "M":
                            timeStep.GetThread(stepDictionary[i][j].componentID).pos = stepDictionary[i][j].componentPos;
                            if (i != 0)
                            {
                                Vector2 difference = timeSteps[timeSteps.Count - 1].GetThread(stepDictionary[i][j].componentID).pos - timeStep.GetThread(stepDictionary[i][j].componentID).pos;
                                if (difference.x > 0) { timeStep.GetThread(stepDictionary[i][j].componentID).rotation = new Vector3(0, 0, 180); }
                                else if (difference.x < 0) { timeStep.GetThread(stepDictionary[i][j].componentID).rotation = new Vector3(0, 0, 0); }
                                else if (difference.y > 0) { timeStep.GetThread(stepDictionary[i][j].componentID).rotation = new Vector3(0, 0, 90); }
                                else if (difference.y < 0) { timeStep.GetThread(stepDictionary[i][j].componentID).rotation = new Vector3(0, 0, -90); }
                            }
                            break;
                        case "D":
                            if (stepDictionary[i][j].componentStatus.delivered_to != 0)
                            {
                                timeStep.GetDeliveryPoint(stepDictionary[i][j].componentStatus.delivered_to).deliveries++;
                            }
                            break;
                        case "E":
                            if (timeStep.GetSemaphore(stepDictionary[i][j].componentID) != null)
                            {
                                timeStep.GetSemaphore(stepDictionary[i][j].componentID).open = stepDictionary[i][j].componentStatus.value;
                            }
                            else if (timeStep.GetPickup(stepDictionary[i][j].componentID) != null)
                            {
                                timeStep.GetPickup(stepDictionary[i][j].componentID).available = stepDictionary[i][j].componentStatus.available;
                            }
                            else if (timeStep.GetThread(stepDictionary[i][j].componentID) != null)
                            {
                                if (stepDictionary[i][j].componentStatus.payload != null)
                                {
                                    timeStep.GetThread(stepDictionary[i][j].componentID).DisablePackages();
                                    for (int k = 0; k < stepDictionary[i][j].componentStatus.payload.Length; k++)
                                    {
                                        for (int l = 0; l < i; l++)
                                        {
                                            if (timeSteps[l].GetThread(stepDictionary[i][j].componentID).ContainsPackage(stepDictionary[i][j].componentStatus.payload[k]) == false)
                                            {
                                                PackageData package = new PackageData();
                                                package.active = false;
                                                package.id = stepDictionary[i][j].componentStatus.payload[k];
                                                timeSteps[l].GetThread(stepDictionary[i][j].componentID).packages.Add(package);
                                            }
                                        }
                                        if (timeStep.GetThread(stepDictionary[i][j].componentID).ContainsPackage(stepDictionary[i][j].componentStatus.payload[k]))
                                        {
                                            timeStep.GetThread(stepDictionary[i][j].componentID).GetPackage(stepDictionary[i][j].componentStatus.payload[k]).active = true;
                                        }
                                        else
                                        {
                                            PackageData package = new PackageData();
                                            package.active = true;
                                            package.id = stepDictionary[i][j].componentStatus.payload[k];
                                            timeStep.GetThread(stepDictionary[i][j].componentID).packages.Add(package);
                                        }
                                    }
                                }
                            }
                            else if (timeStep.GetConditional(stepDictionary[i][j].componentID) != null)
                            {
                                if (stepDictionary[i][j].componentStatus.current != -1)
                                {
                                    timeStep.GetConditional(stepDictionary[i][j].componentID).current = stepDictionary[i][j].componentStatus.current;
                                }
                            }
                            break;
                    }
                }
                timeSteps.Add(timeStep);
                if (stopwatch.ElapsedMilliseconds > 1000 / 60)
                {
                    stopwatch.Stop();
                    stopwatch.Reset();
                    yield return new WaitForEndOfFrame();
                    stopwatch.Start();
                }
            }

            playerInteraction.playerInteraction_UI.playbackSlider.maxValue = maxStep;
            playerInteraction.playerInteraction_UI.loadingOverlay.ClosePanel();
			playerInteraction.playerInteraction_UI.overlayBackground.SetTargetAlpha(0f);
            playerInteraction.playerInteraction_UI.playbackControls.gameObject.SetActive(true);
            playerInteraction.playerInteraction_UI.stopSimulationButton.interactable = true;
            playerInteraction.playerInteraction_UI.stopSimulationButton.gameObject.SetActive(true);
            playerInteraction.playerInteraction_UI.pauseSimulationButton.interactable = true;
            playerInteraction.playerInteraction_UI.pauseSimulationButton.gameObject.SetActive(true);
            playerInteraction.playerInteraction_UI.playbackSlider.interactable = true;
            playerInteraction.playerInteraction_UI.playbackSlider.gameObject.SetActive(true);
            yield return PlaySimulation(maxStep);
        }
        else
        {
            yield return null;
        }
    }

    IEnumerator PlaySimulation(int maxStep)
    {
        int maxGoalsCompleted = 0;
        currentStep = 0;
        paused = false;
        playerInteraction.playerInteraction_UI.playbackSlider.value = 0;
        bool nextLevelButtonVisibility = false;
        while (currentStep <= maxStep)
        {
            if (paused != true)
            {
                if (stepDictionary.ContainsKey(currentStep))
                {
                    float waitTime = 0f;
                    int count = 0;
                    foreach (StepData step in stepDictionary[currentStep])
                    {
                        count++;
                        if (step.componentID == 0)
                        {
                            yield return new WaitForSeconds(0.5f);
                            if (step.componentStatus == null) continue;
                            if (step.componentStatus.goals_completed != null && step.componentStatus.final_condition != -1)
                            {
                                if (maxGoalsCompleted < step.componentStatus.goals_completed) { maxGoalsCompleted = step.componentStatus.goals_completed; }
                            }
                            if (step.componentStatus.final_condition != null && step.componentStatus.final_condition != -1)
                            {
                                playerInteraction.score.stepCount = maxStep;
                                string titleFormatString = "<size=18><b>{0}</b></size>\n";
                                string titleString = "";
                                string goalString = "";
                                string levelFileName = "";
                                if (GameManager.Instance.currentLevelReferenceObject != null) levelFileName = GameManager.Instance.currentLevelReferenceObject.file;

                                switch (step.componentStatus.final_condition)
                                {
                                    case 2:
                                    case 8:
                                    case 10:
                                        success = true;
                                        //if "test" versus "submit" change this text
                                        if (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.ME)
                                        {
                                            titleString = "SUCCESSFUL SOLUTION";
                                            goalString += "\n• Congratulations! This solution will always work. Please proceed to the next level.";
                                            playerInteraction.score.completed = true;

                                            //get current score
                                            int currentScore = GameManager.Instance.GetScoreManager().GetCalculatedScore(playerInteraction.score);
                                            //update saved score
                                            GameManager.Instance.GetScoreManager().ScoreLevel(playerInteraction.score);
                                            int lvlScore = GameManager.Instance.GetScoreManager().GetCalculatedScore(playerInteraction.score.index);

                                            GameManager.Instance.currentLevelReferenceObject.completionRank = lvlScore;
                                            GameManager.Instance.GetDataManager().UpdateLevelRank(levelFileName, lvlScore);

                                            //use 'current' not 'best' score for the feedback
                                            playerInteraction.playerInteraction_UI.goalOverlay.SetFeedbackScore(currentScore);

                                            nextLevelButtonVisibility = true;
                                        }
                                        else if (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.Play)
                                        {
                                            titleString = "TEST COMPLETE";
                                            goalString += "\n• This solution was successful this time. Submit to check if it's always successful.";
                                            playerInteraction.playerInteraction_UI.goalOverlay.SetFeedbackScore(-1);
                                        }
                                        break;
                                    default:
                                        success = false;
                                        //if "test" versus "submit" change this text
                                        if (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.ME)
                                        {
                                            titleString = "UNSUCCESSFUL SOLUTION";
                                        }
                                        else if (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.Play)
                                        {
                                            titleString = "TEST COMPLETE";
                                            playerInteraction.playerInteraction_UI.goalOverlay.SetFeedbackScore(-1);
                                        }

                                        goalString = "";
                                        if ((step.componentStatus.final_condition & 1) != 0)
                                        {
                                            goalString += "\n• Make sure arrows aren't blocked.";
                                        }
                                        if ((step.componentStatus.final_condition & 4) != 0)
                                        {
                                            goalString += "\n• This solution was unsuccessful.";
                                        }
                                        if ((step.componentStatus.final_condition & 16) != 0)
                                        {
                                            goalString += "\n• Make sure arrows can't deliver at the same time.";
                                        }
                                        if ((step.componentStatus.final_condition & 32) != 0)
                                        {
                                            goalString += "\n• Make sure all arrows can move.";
                                        }
                                        if ((step.componentStatus.final_condition & 64) != 0)
                                        {
                                            goalString += "\n• Make sure arrows don't get caught in an infinite loop.";
                                        }
                                        if ((step.componentStatus.final_condition & 512) != 0)
                                        {
                                            goalString += "\n• Wrong turn! Check the Flow Arrows on the right of the screen.";
                                        }
                                        List<string> errorFeedback = new List<string>();
                                        foreach (string errorKey in step.componentStatus.goal_descriptions)
                                        {
                                            string key = errorKey.Substring(0, 3);
                                            if (Constants.GoalFeedbackValues.GoalErrorFeedback.ContainsKey(key))
                                            {
                                                if (!errorFeedback.Contains(Constants.GoalFeedbackValues.GoalErrorFeedback[key]))
                                                    errorFeedback.Add(Constants.GoalFeedbackValues.GoalErrorFeedback[key]);
                                            }
                                        }
                                        foreach (string s in errorFeedback) goalString += ("• " + s + "\n");

                                        break;
                                }

                                if (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.ME)
                                {
                                    playerInteraction.playerInteraction_UI.goalOverlay.levels.gameObject.SetActive(true);
                                }
                                else if (GameManager.Instance.GetCurrentSimulationType() == LinkJava.SimulationTypes.Play)
                                {
                                    playerInteraction.playerInteraction_UI.goalOverlay.levels.gameObject.SetActive(false);
                                }

                                yield return StartCoroutine(playerInteraction.playerInteraction_UI.TriggerGoalPopUp(titleString, goalString));
                            }
                        }
                        else
                        {
                            GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByID(step.componentID);
                            if (g != null)
                            {
                                if (g.timeStep == null)
                                    g.SetTimestep(timeSteps[currentStep]);
                                float time = g.DoStep(step, stepDictionary);
                                if (time > waitTime)
                                    waitTime = time;
                            }
                            else { Debug.Log("Could not find " + step.componentID); }
                        }

                        if (PlayerInteraction_GamePhaseBehavior.onSimulationStep != null)
                        {
                            PlayerInteraction_GamePhaseBehavior.onSimulationStep(step);
                            //PauseSimulation();
                            //UnpauseAfterDelay(5);
                        }
                    }
                    while (paused) yield return new WaitForSeconds(0.1f);
                    yield return new WaitForSeconds(waitTime);
                }
                currentStep++;
                playerInteraction.playerInteraction_UI.playbackSlider.value = currentStep;
                //Debug.Log(currentStep);
            }
            else
            {
                yield return new WaitForEndOfFrame();
            }
        }

        yield return StartCoroutine(FinishSimulation(maxStep));
    }

    IEnumerator FinishSimulation(int maxStep)
    {
        Debug.Log("FinishSimulation");

        yield return new WaitForEndOfFrame();

        if (PlayerInteraction_GamePhaseBehavior.onCompletion != null) PlayerInteraction_GamePhaseBehavior.onCompletion();

        //yield return new WaitForSeconds(1f);
        //todo: switch statement of the selected goal option

        playerInteraction.ResetStartValues();

        switch (playerInteraction.playerInteraction_UI.goalOverlay.userInput)
        {
            case PlayerInteraction_UI.Goal_UIOverlay.UserInputs.exit:
            case PlayerInteraction_UI.Goal_UIOverlay.UserInputs.levels:
                playerInteraction.TriggerPlayPhaseEnd();
                //Debug.Log("User input for exit or levels hit.");
                playerInteraction.EndSimulation();
                break;
            case PlayerInteraction_UI.Goal_UIOverlay.UserInputs.stop:
                playerInteraction.TriggerPlayPhaseEnd();
                //Debug.Log("User input for exit or levels hit.");
                playerInteraction.EndSimulation();
                break;
            case PlayerInteraction_UI.Goal_UIOverlay.UserInputs.replay:

                playerInteraction.playerInteraction_UI.goalOverlay.ClosePanel();
                StartCoroutine(PlaySimulation(maxStep));
                PauseSimulation();
                DelayedUnpause(1f);

                break;
            case PlayerInteraction_UI.Goal_UIOverlay.UserInputs.retry:
                //Debug.Log("Retry");
                playerInteraction.interactionPhase = PlayerInteraction_GamePhaseBehavior.InteractionPhases.ingame_default;
                playerInteraction.EndSimulation();
                if (GameManager.Instance.GetDataManager().currentLevelData.metadata.level_type != -1)
                {
                    GameManager.Instance.TriggerLevelTutorial
                    (
                        GameManager.Instance.GetDataManager().currentLevelData.metadata.level_id,
                        playerInteraction.interactionPhase == PlayerInteraction_GamePhaseBehavior.InteractionPhases.awaitingSimulation || playerInteraction.interactionPhase == PlayerInteraction_GamePhaseBehavior.InteractionPhases.simulation ? TutorialEvent.TutorialInitializeTriggers.duringSimulation : TutorialEvent.TutorialInitializeTriggers.beforePlay
                    );
                }
                break;
            case PlayerInteraction_UI.Goal_UIOverlay.UserInputs.levelsNext:
                playerInteraction.TriggerPlayPhaseEnd(GameManager.GamePhases.LoadScreen, true);
                break;
            default:
                //Debug.Log("No case defined for " + playerInteraction.playerInteraction_UI.goalOverlay.userInput.ToString());
                playerInteraction.interactionPhase = PlayerInteraction_GamePhaseBehavior.InteractionPhases.ingame_default;
                break;
        }
    }

    void ChangePlaybackStep()
    {
        Level lvl = GameManager.Instance.GetDataManager().currentLevelData;
        for (int i = 0; i < lvl.components.Count; i++)
        {
            GridObjectBehavior g = GameManager.Instance.GetGridManager().GetGridObjectByID(lvl.components[i].id);
            g.ReturnToStep(timeSteps[currentStep]);
        }
    }

    public void PauseSimulation()
    {
        paused = true;
        GameManager.Instance.tracker.CreateEventExt("PauseSimulation", paused.ToString());
        playerInteraction.playerInteraction_UI.pauseSimulationButton.onClick.RemoveAllListeners();
        playerInteraction.playerInteraction_UI.pauseSimulationButton.onClick.AddListener(UnpauseSimulation);
        playerInteraction.playerInteraction_UI.playbackButton.sprite = playerInteraction.playerInteraction_UI.playbackButtonSprites[1];
    }

    public void UnpauseSimulation()
    {
        paused = false;
        GameManager.Instance.tracker.CreateEventExt("PauseSimulation", paused.ToString());
        playerInteraction.playerInteraction_UI.pauseSimulationButton.onClick.RemoveAllListeners();
        playerInteraction.playerInteraction_UI.pauseSimulationButton.onClick.AddListener(PauseSimulation);
        playerInteraction.playerInteraction_UI.playbackButton.sprite = playerInteraction.playerInteraction_UI.playbackButtonSprites[0];
    }

    public void OnTimeSliderValueChanged(int i)
    {
        if (i != currentStep)
        {
            PauseSimulation();
            currentStep = i;
            ChangePlaybackStep();
        }
    }

    public void DelayedUnpause(float delay = 0, TutorialEvent t = null)
    {
        //Debug.Log(delay);
        if (delay == 0)
        {
            paused = false;
            GameManager.Instance.tracker.CreateEventExt("PauseSimulation", paused.ToString());
        }
        else
        {
            StartCoroutine(DelayedUnpauseRoutine(delay, t));
        }
    }

    IEnumerator DelayedUnpauseRoutine(float delay, TutorialEvent t)
    {
        yield return new WaitForSeconds(delay);
        paused = false;
        if (t != null) GameManager.Instance.ReportTutorialEventComplete(t);
        GameManager.Instance.tracker.CreateEventExt("PauseSimulation", paused.ToString());
    }
}
