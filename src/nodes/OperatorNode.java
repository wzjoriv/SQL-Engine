package nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import main.Evaluator;
import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;

public abstract class OperatorNode implements Iterator<Tuple> {
	public ArrayList<OperatorNode> children = new ArrayList<OperatorNode>();
	public Evaluator eval;
	boolean nextAvailable;
	public HashSet<String> tableNames = null;
	ArrayList<Expression> involvedExp;

	public OperatorNode(SQLParse parser) {
		eval = new Evaluator(parser);
	}

	public void addChild(OperatorNode node) {		//adds child node to arraylist
		children.add(node);
	}
	
	public void removeChild(OperatorNode node) {		//removes child node from arraylist
		children.remove(node);
	}
	
	public void reset() {							// resets the rows. might be needed later, maybe to implement hash join in a certain way. might need to reset table.
		for(OperatorNode c : children) c.reset();	
	}
	
	public OperatorNode getChild(int index) {	
		return children.get(index);
	}
	
	public HashSet<String> tablesInvolved() {		// returns the tables that node is involved with
		
		tableNames = new HashSet<String>();		// writes into tablenNames
		
		for(OperatorNode c : children) {
			tableNames.addAll(c.tablesInvolved());
		}
		
		return tableNames;		// returns all the tables involved
	}
	
	public boolean hasNext() {		// doesnt work for some of the operators
		if (children.isEmpty()) return false;
		
		nextAvailable = true;
		
		for(int i = 0; i < children.size(); i++) {
			nextAvailable = nextAvailable && children.get(i).hasNext();
		}
		
		return nextAvailable;
	}

	public abstract Tuple next();
	
	public ArrayList<Expression> getExpressionsInvolved(){
		return new ArrayList<Expression>();
	}
	
}