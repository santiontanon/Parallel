using UnityEngine;
using System.Collections.Generic;

[System.Serializable]
public class TimeStepData{

    public int timeStep;

    public List<ThreadData> threads;
    public List<PackageData> packages;
    public List<DeliveryData> deliveryPoints;
    public List<SemaphoreData> sempahores;

    public ThreadData GetThread(int id)
    {
        for(int i = 0; i < threads.Count; i++)
        {

        }
        return null;
    }

    public DeliveryData GetDeliveryPoint(int id)
    {
        for(int i = 0; i < deliveryPoints.Count; i++)
        {

        }
        return null;
    }

    public SemaphoreData GetSemaphore(int id)
    {
        for(int i = 0; i < sempahores.Count; i++)
        {

        }
        return null;
    }

}

[System.Serializable]
public class ThreadData
{
    public int id;
    public Vector2 pos;
    public Vector2 rotation;
    public Dictionary<int, bool> packages;
}

[System.Serializable]
public class PackageData
{
    public int id;
    public bool active;
    public int following;
}

[System.Serializable]
public class DeliveryData
{
    public int id;
    public int deliveries;
}

[System.Serializable]
public class SemaphoreData
{
    public int id;
    public bool open;
}
