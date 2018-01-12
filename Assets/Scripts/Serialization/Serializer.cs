using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.Serialization.Formatters.Binary;
using System.IO;

public static class Serializer
{

    public static List<ParallelSave> LoadSaves()
    {
        if(Directory.Exists(Application.persistentDataPath + "/Saves")){
            Debug.Log("Parallel save directory found");
            return LoadDirectory();
        }
        else
        {
            Debug.Log("Parallel save directory not found");
            Directory.CreateDirectory(Application.persistentDataPath + "/Saves");
            return LoadDirectory();
        }
    }

    static List<ParallelSave> LoadDirectory()
    {
        // Process the list of files found in the directory.
        string[] fileEntries = Directory.GetFiles(Application.persistentDataPath + "/Saves");
        List<ParallelSave> saves = new List<ParallelSave>();
        for(int i = 0; i < fileEntries.Length; i++)
        {
            saves.Add(DeserializeData(fileEntries[i]));
        }
        return saves;
    }

    static ParallelSave DeserializeData(string s)
    {
        BinaryFormatter bf = new BinaryFormatter();
        FileStream file = File.Open(s , FileMode.Open);
        ParallelSave save = (ParallelSave)bf.Deserialize(file);
        return save;
    }

    public static void SerializeData(ParallelSave save)
    {
        BinaryFormatter bf = new BinaryFormatter();
        FileStream file = File.Create(Application.persistentDataPath + "/Saves/" + save.name + ".prl");
        bf.Serialize(file, save);
        file.Close();
    }

}
