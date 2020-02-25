using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GraphTest : MonoBehaviour
{
    public GraphNode Node;
    public GraphLine Line;

    List<GraphNode> Nodes = new List<GraphNode>();
    List<GraphLine> Lines = new List<GraphLine>();

    [SerializeField]
    GPDV Data;

    void Awake()
    {
        TextAsset json = (TextAsset)Resources.Load("glyph_test");
        Data = JsonUtility.FromJson<GPDV>(json.text);
        for(int i = 0; i < Data.nodes.Count; i++)
        {
            GraphNode node = Instantiate(Node, this.transform);
            float angle = 360 / (Data.nodes.Count) * i;
            float sin = Mathf.Sin(angle * Mathf.PI / 180);
            float cosin = Mathf.Cos(angle * Mathf.PI / 180);
            node.transform.localPosition = new Vector3(650 * sin, 650 * cosin);
            node.Text.text = Data.nodes[i].id.ToString();
            Nodes.Add(node);
        }
        for (int i = 0; i < Data.links.Count; i++)
        {
            GraphLine line = Instantiate(Line, this.transform);
            line.Begin = Nodes[Data.links[i].source];
            line.End = Nodes[Data.links[i].target];
            float width = Data.links[i].user_ids.Count * 2f;
            line.Line.startWidth = width;
            line.Line.endWidth = width;
            Lines.Add(line);
        }
    }
}

//Glyph-Parallel Data Visualization
[System.Serializable]
public class GPDV
{
    [SerializeField]
    public string level_info;
    [SerializeField]
    public int num_patterns;
    [SerializeField]
    public int num_users;
    [SerializeField]
    public List<DataNode> nodes;
    [SerializeField]
    public List<DataLink> links;
}

[System.Serializable]
public class DataNode
{
    [SerializeField]
    public int id;
    [SerializeField]
    public string type;
}

[System.Serializable]
public class DataLink
{
    [SerializeField]
    public string id;
    [SerializeField]
    public int source;
    [SerializeField]
    public int target;
    [SerializeField]
    public List<int> user_ids;
}
