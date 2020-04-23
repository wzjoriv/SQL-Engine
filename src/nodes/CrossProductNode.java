package nodes;

import java.util.ArrayList;

import main.SQLParse;
import main.Tuple;

public class CrossProductNode extends OperatorNode{ // "take two things and merge everything with everything else" lol nice josue :D
	

	Tuple left, right, temp;
	ArrayList<Tuple> mem = new ArrayList<Tuple>();
	Boolean memLoaded;
	int track;
	
	public CrossProductNode(SQLParse parser) {
		super(parser);
		memLoaded = false;		// memloaded is flag to lmk if one side of the table has been loaded into arraylist table
		track = 0;
	}

	public Tuple next() {
		
		if(!memLoaded) {		// first time we run memloaded will be false. while loop that says:
			while((temp =  children.get(1).next()) != null) {		//loads entire right side to memory
			    if(temp == null) continue;
				mem.add(temp);
			}
			memLoaded = true;				// then set mem to true. this happens only the first time that next() is called
		}
		//LEFT SIDE
		if(track == 0) left = children.get(0).next();	// if track = 0, we change the left child
		if(left == null) {		// null if there are no more left values
			mem = null;			// set mem to null
			return null;		// return null
		}
		//RIGHT SIDE
		right = mem.get(track);	//load track into the right side. do some simple % math
		
		track = (track + 1) % mem.size();// merge right & left sides

		return left.merge(right);
	}
	
	public boolean hasNext() {	// for a xproduct to have next, it has to have a child in one of the sides. we don't really use hasNext()
		if (children.isEmpty()) return false;
		
		nextAvailable = false;
		
		for(OperatorNode child : children) {
			nextAvailable = nextAvailable || child.hasNext();
		}
		
		return nextAvailable;
	}

}
