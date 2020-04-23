package nodes;

import main.SQLParse;
import main.Tuple;

// union: node and two children
// you return all the elements from left child and right child, return them all
// can implement it in different ways
public class UnionNode extends OperatorNode {
	private Tuple temp;
	
	public UnionNode(SQLParse parser) {
		super(parser);
	}

	@Override
	public Tuple next() {
		
		for(OperatorNode child : children) {
			if((temp = child.next()) != null) return temp;
		}
		return null;
	}
	
	public boolean hasNext() {
		if (children.isEmpty()) return false;
		
		nextAvailable = false;
		
		for(int i = 0; i < children.size(); i++) {
			nextAvailable = nextAvailable || children.get(i).hasNext();
		}
		
		return nextAvailable;
	}

}
