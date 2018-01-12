using UnityEngine;
using System.Collections;
using System.Collections.Generic;


class Tile{
	public int id;
	public int x;
	public int y;
	public int type;
	public List<Tile> neighbors;
	public bool busy = false;
	public Tile(int _x, int _y, int _type,int _id){
		this.x = _x;
		this.y = _y;
		this.type = _type;
		this.id = _id;
		this.neighbors = new List<Tile>();
	}
}

class Player{
	public int x;
	public int y;
	public int type;
	public int id;
	public List<Tile> path;
	public int path_position;
	public GameObject go;
	public List<GameObject> waypoints;
	public int deadlock_count = 0;
	public Color color;
	public Material material;
	public Vector3 position_from= Vector3.zero;
	public Vector3 position_to= Vector3.zero;
	public Player(int _x, int _y, int _type, GameObject _go, int _id){
		this.x = _x;
		this.y = _y;
		this.type = _type;
		this.go = _go;
		this.id = _id;
		this.path_position = 0;
		this.path = new List<Tile>();
		this.waypoints = new List<GameObject> ();
	}
}

public class Tracks : MonoBehaviour {
	public bool lerpling =true;
	float last_update = 0.0f;
	public float next_update_delay = 0.5f;
	public Transform trackParent;

	GameObject block;
	GameObject ground;
	private enum WORLD_TYPE {
		TYPE_PREDEFINED,
		TYPE_SMALLCROSS
	}
	WORLD_TYPE world_type = WORLD_TYPE.TYPE_SMALLCROSS;

	float nextUpdate = 0.0f;

	Tile[] tiles;
	List<Player> players;
	List<Tile> goals;

	public void StartGeneration () {
		int size_x = 0;
		int size_y = 0;
		switch (world_type) {
		case WORLD_TYPE.TYPE_PREDEFINED:
			size_x = 15;
			size_y = 15;
			break;
		case WORLD_TYPE.TYPE_SMALLCROSS:
			size_x = 15;
			size_y = 15;
			break;
		}
		tiles = new Tile[size_x*size_y];
		int zoom = 20;
		block = GameObject.CreatePrimitive(PrimitiveType.Cube);
		//block = GameObject.Find("Block");
		//ground = GameObject.Find("Ground");
		//ground.transform.position = new Vector3(size_x/2,-0.5f,size_y/2);
		//ground.transform.localScale = new Vector3(size_x/2,size_x/4,size_y/2);
		block.transform.position = new Vector3(0,0,0);
		switch (world_type) {
		case WORLD_TYPE.TYPE_PREDEFINED:
			CreatePredefinedWorld (size_x, size_y);
			break;
		case WORLD_TYPE.TYPE_SMALLCROSS:
			CreateSmallCrossWorld (size_x, size_y);
			break;

		}
		ConnectTiles(size_x,size_y);
		StartGoals ();
		StartPlayers (tiles,size_x);
		UpdatePlayers ();


		Camera.main.transform.position = new Vector3(size_x/2,zoom,size_y/2);
		Camera.main.transform.rotation = Quaternion.LookRotation(new Vector3(0,-1,0));
		Camera.main.transform.rotation.SetFromToRotation(new Vector3(size_x/2,size_x,size_y/2),new Vector3(size_x/2,0,size_y/2));
	}
	void StartGoals(){
		goals = new List<Tile> ();
		goals.Add (tiles [15*15/2]);
		goals.Add (tiles [0]);
		goals.Add (tiles [14]);
		goals.Add (tiles [15*15-1]);
		goals.Add (tiles [15*15-15]);

	}
	void StartPlayers(Tile[] tiles, int size_x){
		players = new List<Player> ();
		GameObject player;
		//player = GameObject.Find("Player");
		player = GameObject.CreatePrimitive(PrimitiveType.Sphere);
		player.transform.position = new Vector3(0,0,0);
		//List<Tile> path = new List<Tile> ();
		//for (int i = 0; i < size_x; i++) {
		//		path.Add (tiles [i]);
		//}
		for (int i = 0; i < 5; i++) {
			GameObject go = Instantiate (player);
			Renderer r = go.GetComponent<Renderer> ();
			Color color = new Color(Random.value,Random.value,Random.value,1);
			r.material.color = color;
			r.material.shader =  Shader.Find("Sprites/Default");
			Player new_player = new Player (0, 0, 0, go, i);
			new_player.path = new List<Tile> ();//path;
			new_player.path.Add(tiles[i]);
			new_player.path_position = 0; //i
			new_player.color = color;
			new_player.material = r.material;
			players.Add (new_player);
		}
	}

	void UpdatePlayers(){
		foreach (Player player in players) {
			UpdatePlayer (player);
			if (!lerpling) {
				Tile tile = player.path [player.path_position];
				player.go.transform.position = new Vector3 (tile.x, 1, tile.y);
			}
		}
	}
	void UpdatePlayer(Player player){
		if (lerpling) {
			Tile tile = player.path [player.path_position];
			player.position_from = new Vector3 (tile.x, 1, tile.y);			
			player.position_to = new Vector3 (tile.x, 1, tile.y);			
		}

		//Debug.Log ("UpdatePlayers "+player.id.ToString());
		bool recompute_path = false;
		// check if can walk!
		player.path_position += 1;
		if (player.path_position < player.path.Count){
			if (player.path [player.path_position].busy) {
				// deadlock
				player.path_position -= 1;
				player.deadlock_count +=1;
				if (player.deadlock_count % 2 == 0) {
					player.material.color = new Color (1, 1, 1, 1);
				} else {
					player.material.color = player.color;
				}
				Debug.Log("deadlock " + player.id.ToString() + " for "+player.deadlock_count.ToString());
				if (player.deadlock_count < 10) {
					return;
				} else {
					recompute_path = true;
				}
			} else {
				if (player.waypoints.Count > 0) {
					Destroy (player.waypoints [0]);
					player.waypoints.RemoveAt(0);
				}
				player.deadlock_count = 0;
				player.material.color = player.color;
				player.path [player.path_position-1].busy = false;
				player.path [player.path_position].busy = true;

				if (lerpling) {
					Tile tile = player.path [player.path_position];
					player.position_to = new Vector3 (tile.x, 1, tile.y);			
				}

			}

		} else {
			recompute_path = true;
			player.path_position -= 1;
			if (player.path_position > 0) { // edge case for paths of length 1
				player.path [player.path_position - 1].busy = false;
			}
			player.path [player.path_position].busy = true;
		}
		if (recompute_path) {
			Tile current = player.path [player.path_position];
			current.busy = true;
			Tile goal = goals[Random.Range (0, goals.Count)];
			while (current.id == goal.id) {
				goal = goals [Random.Range (0, goals.Count)];
			}
			player.path_position = 0;
			//player.path = new List<Tile> ();
			//player.path.Add (goal);
			player.path = FindPath(current,goal);
			foreach (GameObject go in player.waypoints) {
				Destroy (go);
			}
			player.waypoints.Clear ();
			foreach (Tile tile in player.path) {
				GameObject go = Instantiate (player.go);
				go.transform.localScale = new Vector3 (0.2f, 0.2f, 0.2f);
				go.transform.position = new Vector3 (tile.x+player.id*0.1f, 1.0f, tile.y+player.id*0.1f);
				Renderer r = go.GetComponent<Renderer> ();
				r.material.color = player.color;
				player.waypoints.Add (go);
				go.transform.SetParent(trackParent);
			}

		}

	}
	List<Tile> FindPath(Tile start,Tile goal){
		//Debug.Log("Find path from/to "+start.id.ToString()+ " " + goal.id.ToString());
		List<Tile> path = new List<Tile> ();

		Dictionary<int,int> from_tile = new Dictionary<int, int>();
		List<int> open = new List<int>();
		HashSet<int> closed = new HashSet<int>();
		open.Add (start.id);
		from_tile.Add (start.id, -1);
		while (open.Count>0) {
			int tile_id = open [0];
			open.RemoveAt (0);
			if (tile_id == goal.id) {
				// reconstruct
				int current_tile = goal.id;
				while (current_tile >= 0) {
					path.Add(tiles[current_tile]);
					current_tile = from_tile [current_tile];
				}
				path.Reverse();
				return path;
			}
			Tile tile = tiles [tile_id];
			foreach (Tile neighbor in tile.neighbors) {
				if (from_tile.ContainsKey (neighbor.id)) {
					// already explored
				} else {
					from_tile.Add (neighbor.id, tile.id);
					open.Add (neighbor.id);
				}
			}
		}
		Debug.Log ("No path found from_tile " + from_tile.Keys.Count.ToString ());
		path.Add (goal);
		return path;

	}
	GameObject CreateBlock(int x, int y, int size_x){
		tiles[x+y*size_x] = new Tile(x,y,1,x+y*size_x);
		GameObject new_block;
		if (x==0 && y==0)
			new_block = block;
		else{
			new_block = (GameObject)GameObject.Instantiate(block,new Vector3(x,0,y),block.transform.rotation);
			if(trackParent) new_block.transform.SetParent(trackParent);
			//new_block.transform.parent = block.transform;
		}
		return new_block;

	}
	GameObject CreateEmptyBlock(int x, int y, int size_x){
		tiles[x+y*size_x]=new Tile(x,y,0,x+y*size_x);
		return null;

	}
	void ConnectTiles(int size_x,int size_y){
		for (int y = 0; y < size_y; y++) {
			for (int x = 0; x < size_x; x++) {
				if (y > 0 && tiles [x + y * size_x - size_x].type == 1)
					tiles [x + y * size_x].neighbors.Add (tiles [x + y * size_x - size_x]);
				if (x > 0 && tiles [x + y * size_x - 1].type == 1)
					tiles [x + y * size_x].neighbors.Add (tiles [x + y * size_x -1]);
				if (x < size_x-1 && tiles [x + y * size_x + 1].type == 1)
					tiles [x + y * size_x].neighbors.Add (tiles [x + y * size_x +1]);
				if (y < size_y-1 && tiles [x + y * size_x + size_x].type == 1)
					tiles [x + y * size_x].neighbors.Add (tiles [x + y * size_x +size_x]);
			}
		}
	}
	void CreatePredefinedWorld(int size_x, int size_y){
		string world_string = "LLLIIITTTIIILLL\nL      T      L\nL      T      L\nI      I      I\nI      I      I\nI      I      I\nT             T\nTTTIIITTTIIITTT\nT      T      T\nI      I      I\nI      I      I\nI      I      I\nL      T      L\nL      T      L\nLLLIIITTTIIILLL";
		world_string = world_string.Replace("\n","");
		int i = 0;
		for (int y = 0; y < size_y; y++) {
			for (int x = 0; x < size_x; x++) {
				if (world_string [i] == ' ') {
					CreateEmptyBlock (x, y,size_x);
				} else {
					CreateBlock(x,y,size_x);
				}
				++i;
			}
		}

	}
	void CreateSmallCrossWorld(int size_x, int size_y){
		for (int y = 0; y < size_y; y++) {
			for (int x = 0; x < size_x; x++) {
				//if (x % 3 == 0 || x % 5 == 0 || y % 3 == 0 || y % 5 == 0) {
				if (x == 0 || y ==0 || x==size_x-1 || y == size_y-1 || x == size_x/2 || y == size_y/2) {
					CreateBlock (x, y,size_x);
				} else {
					CreateEmptyBlock (x, y, size_x);
				}
			}
		}

	}

	public void UpdateTracks () {
		if (nextUpdate < Time.time) {
			UpdatePlayers ();
			nextUpdate += next_update_delay;
			last_update = Time.time;
		}
		if (lerpling) {
			float t = (Time.time - last_update) / next_update_delay;
			foreach (Player player in players) {
				player.go.transform.position = Vector3.Lerp (player.position_from, player.position_to, t);
			}
		}

	}
}
