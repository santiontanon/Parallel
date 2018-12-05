using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.Serialization.Formatters.Binary;
using System.IO;

public static class Serializer
{

    public static List<ParallelSave> LoadSaves()
    {
        if(Directory.Exists(GameManager.Instance.GetLinkJava().localPath + "Saves")){
            return LoadDirectory();
        }
        else
        {
            Directory.CreateDirectory(GameManager.Instance.GetLinkJava().localPath + "Saves");
            return LoadDirectory();
        }
    }

    static List<ParallelSave> LoadDirectory()
    {
        // Process the list of files found in the directory.
        string[] fileEntries = Directory.GetFiles(GameManager.Instance.GetLinkJava().localPath + "Saves");
        List<ParallelSave> saves = new List<ParallelSave>();
        for(int i = 0; i < fileEntries.Length; i++)
        {
            saves.Add(DeserializeData(fileEntries[i]));
        }
        return saves;
    }

    static ParallelSave DeserializeData(string s)
    {
        ParallelSave save = null;
        try
        {
            BinaryFormatter bf = new BinaryFormatter();
            FileStream file = File.Open(s, FileMode.Open);
            save = (ParallelSave)bf.Deserialize(file);
        }
        catch
        {
            Debug.Log("Error Reading Save");
        }
        if(save == null)
        {
            save = new ParallelSave();
        }
        return save;
    }

    public static void SerializeData(ParallelSave save)
    {
        BinaryFormatter bf = new BinaryFormatter();
        FileStream file = File.Create(GameManager.Instance.GetLinkJava().localPath + "Saves/" + save.name + ".prl");
        bf.Serialize(file, save);
        file.Close();
    }

}
