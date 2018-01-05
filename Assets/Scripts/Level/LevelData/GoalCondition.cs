using UnityEngine;
using System.Collections;
using System.Collections.Generic;

[System.Serializable]
public class GoalCondition {
	/*
	id (int): the identifier of the component from the components section in the file this assertion refers to
	type (string): the type of the component, each with different properties described in the components section of this document
	property (string): the property of the component to check
	value (integer for now): the value to of the component to check
	condition (string) the comparison to perform, one of:
	eq: equal
	gt: greater than
	lt: less than
	ne: not equal 
	*/
	/*
	{"required":[{"id":3001,"type":"delivery","property":"delivered","value":3,"condition":"eq"}],"desired":[]}
	*/
	[System.Serializable]
	public class Goal
	{
		public int id;
		public string type;
		public string property;
		public int value;
		public string condition;
		public int thread_id;
	
		public Goal()
		{
			id = 0;
			type = "type";
			property = "property";
			value = 0;
			condition = "comparison";
			thread_id = 0;
		}
	}


	public Goal[] required = new Goal[]{};
	public Goal[] desired = new Goal[]{};


}
