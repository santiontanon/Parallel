using UnityEngine;
using System.Collections.Generic;

[System.Serializable]
public class TimeStepData{

    public int timeStep;

    public List<ThreadData> threads;
    public List<PackageData> packages;
    public List<DeliveryData> deliveryPoints;
    public List<SemaphoreData> sempahores;

    public TimeStepData()
    {
        threads = new List<ThreadData>();
        packages = new List<PackageData>();
        deliveryPoints = new List<DeliveryData>();
        sempahores = new List<SemaphoreData>();
    }

    public TimeStepData Copy(TimeStepData _timeStep)
    {
        TimeStepData timeStep = new TimeStepData();
        for (int i = 0; i < _timeStep.threads.Count; i++)
        {
            ThreadData thread = new ThreadData();
            thread.id = _timeStep.threads[i].id;
            thread.pos = new Vector2(_timeStep.threads[i].pos.x, _timeStep.threads[i].pos.y);
            thread.rotation = new Vector2(_timeStep.threads[i].rotation.x, _timeStep.threads[i].rotation.y);
            timeStep.threads.Add(thread);
        }
        for (int i = 0; i < _timeStep.deliveryPoints.Count; i++)
        {
            DeliveryData delivery = new DeliveryData();
            delivery.id = _timeStep.deliveryPoints[i].id;
            delivery.deliveries = _timeStep.deliveryPoints[i].deliveries;
            timeStep.deliveryPoints.Add(delivery);
        }
        for (int i = 0; i < _timeStep.sempahores.Count; i++)
        {
            SemaphoreData semaphore = new SemaphoreData();
            semaphore.id = _timeStep.sempahores[i].id;
            semaphore.open = _timeStep.sempahores[i].open;
            timeStep.sempahores.Add(semaphore);
        }
        return timeStep;
    }

    public ThreadData GetThread(int id)
    {
        for(int i = 0; i < threads.Count; i++)
        {
            if(threads[i].id == id)
            {
                return threads[i];
            }
        }
        return null;
    }

    public DeliveryData GetDeliveryPoint(int id)
    {
        for(int i = 0; i < deliveryPoints.Count; i++)
        {
            if (deliveryPoints[i].id == id)
            {
                return deliveryPoints[i];
            }
        }
        return null;
    }

    public SemaphoreData GetSemaphore(int id)
    {
        for(int i = 0; i < sempahores.Count; i++)
        {
            if (sempahores[i].id == id)
            {
                return sempahores[i];
            }
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
    public int open;
}
