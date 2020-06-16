using UnityEngine;
using System.Collections.Generic;

[System.Serializable]
public class TimeStepData{

    public int timeStep;

    [SerializeField]
    public TimeStepData previousStep;
    [SerializeField]
    public TimeStepData nextStep;

    public List<ThreadData> threads;
    public List<PickupData> pickups;
    public List<DeliveryData> deliveryPoints;
    public List<SemaphoreData> sempahores;
    public List<ConditionalData> conditionals;
    public List<SignalData> signals;

    public TimeStepData()
    {
        threads = new List<ThreadData>();
        pickups = new List<PickupData>();
        deliveryPoints = new List<DeliveryData>();
        sempahores = new List<SemaphoreData>();
        conditionals = new List<ConditionalData>();
        signals = new List<SignalData>();
    }

    public TimeStepData Copy(TimeStepData _timeStep)
    {
        TimeStepData timeStep = new TimeStepData();
        for (int i = 0; i < _timeStep.threads.Count; i++)
        {
            ThreadData thread = new ThreadData();
            thread.id = _timeStep.threads[i].id;
            thread.pos = new Vector2(_timeStep.threads[i].pos.x, _timeStep.threads[i].pos.y);
            thread.rotation = new Vector3(_timeStep.threads[i].rotation.x, _timeStep.threads[i].rotation.y, _timeStep.threads[i].rotation.z);
            thread.packages = new List<PackageData>();
            for(int j = 0; j < _timeStep.threads[i].packages.Count; j++)
            {
                PackageData package = new PackageData();
                package.id = _timeStep.threads[i].packages[j].id;
                package.active = _timeStep.threads[i].packages[j].active;
                thread.packages.Add(package);
            }
            timeStep.threads.Add(thread);
        }
        for (int i = 0; i < _timeStep.pickups.Count; i++)
        {
            PickupData pickup = new PickupData();
            pickup.id = _timeStep.pickups[i].id;
            pickup.available = _timeStep.pickups[i].available;
            timeStep.pickups.Add(pickup);
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
        for (int i = 0; i < _timeStep.conditionals.Count; i++)
        {
            ConditionalData conditional = new ConditionalData();
            conditional.id = _timeStep.conditionals[i].id;
            conditional.current = _timeStep.conditionals[i].current;
            conditional.directions = new string[_timeStep.conditionals[i].directions.Length];
            for(int j = 0; j < _timeStep.conditionals[i].directions.Length; j++)
            {
                conditional.directions[j] = _timeStep.conditionals[i].directions[j];
            }
            timeStep.conditionals.Add(conditional);
        }
        for(int i = 0; i < _timeStep.signals.Count; i++)
        {
            SignalData signal = new SignalData();
            signal.id = _timeStep.signals[i].id;
            signal.passed = _timeStep.signals[i].passed;
            timeStep.signals.Add(signal);
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

    public PickupData GetPickup(int id)
    {
        for (int i = 0; i < pickups.Count; i++)
        {
            if (pickups[i].id == id)
            {
                return pickups[i];
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

    public ConditionalData GetConditional(int id)
    {
        for (int i = 0; i < conditionals.Count; i++)
        {
            if (conditionals[i].id == id)
            {
                return conditionals[i];
            }
        }
        return null;
    }

    public SignalData GetSignal(int id)
    {
        for (int i = 0; i < signals.Count; i++)
        {
            if (signals[i].id == id)
            {
                return signals[i];
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
    public Vector3 rotation;
    public List<PackageData> packages;

    public ThreadData()
    {
        packages = new List<PackageData>();
    }

    public bool ContainsPackage(int id)
    {
        for(int i = 0; i < packages.Count; i++)
        {
            if(packages[i].id == id)
            {
                return true;
            }
        }
        return false;
    }

    public PackageData GetPackage(int id)
    {
        for (int i = 0; i < packages.Count; i++)
        {
            if (packages[i].id == id)
            {
                return packages[i] ;
            }
        }
        return null;
    }

    public void DisablePackages()
    {
        for(int i = 0; i < packages.Count; i++)
        {
            packages[i].active = false;
        }
    }
}

[System.Serializable]
public class PackageData
{
    public int id;
    public bool active;
    public int following;
}

[System.Serializable]
public class PickupData
{
    public int id;
    public int available;
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

[System.Serializable]
public class ConditionalData
{
    public int id;
    public int current;
    public string[] directions;
}

[System.Serializable]
public class SignalData
{
    public int id;
    public int passed;
}
