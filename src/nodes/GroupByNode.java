package nodes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;

//sort. then, when we return a group, we return all the elements of that group.
// after we return all the elements of that group, we return a null
//that's how we 
public class GroupByNode extends OperatorNode {
	
	private Hashtable<String, ArrayList<Tuple>> mem2;
	private boolean memLoaded;				// we saw this in xproduct. first time we run the program, we load everything into memory. after the first time, we dont have to do that again
	private ArrayList<Column> sortby;
	private Tuple prev = null, curr = null;
	String temp;

	public GroupByNode(SQLParse parser, ArrayList<Column> columns) {
		super(parser);
		mem2 = new Hashtable<String, ArrayList<Tuple>>();
		memLoaded = false;
		sortby = columns;
	}

	public Tuple next() {
		if(children.isEmpty()) return null;
		
		if(!memLoaded) {		
			while((curr = children.get(0).next()) != null) {
				temp = getGroup(curr);
				if(!mem2.containsKey(temp)) mem2.put(temp, new ArrayList<Tuple>());
				mem2.get(temp).add(curr);
			}
			memLoaded = true;
		}
		if(mem2.isEmpty()) return null;		// checks if mem is empty at some point. returns null.
											//else if mem is not empty:
		curr = getNextTuple();				// remove element from mem index 0. set equal to curr
		
		if(!sameGroup(curr, prev)) {//first time we go thru this, prev = null. we pass the previous element & the current element
			prev = null;
			
			temp = getGroup(curr);
			if(!mem2.containsKey(temp)) mem2.put(temp, new ArrayList<Tuple>());
			mem2.get(temp).add(curr);
			
			return null;
		}else {
			prev = curr;
			return curr;
		}
	}
	
	private boolean sameGroup(Tuple tuple1, Tuple tuple2) {	//finds if one of the columns is different > return false
		if(tuple1 == null || tuple2 == null) return true;
		
		try {
			for (Column currColumn : sortby)	// if the columns match, return true
				if (!eval.equals(eval.evaluate(currColumn, tuple1), eval.evaluate(currColumn, tuple2)).toBool())
					return false;

		} catch (Exception e) {}
		return true;
	}
	
	private String getGroup(Tuple tuple) {
		String out = "";
		try {
			for(int i = 0; i < sortby.size(); i++) {
				out+= eval.evaluate(sortby.get(i), tuple).toString() + "|";
			}
		} catch (SQLException e) {}
		return out;
	}
	
	private Tuple getNextTuple() {
		ArrayList<String> keys = new ArrayList<String>(mem2.keySet());
		Tuple temp;
		
		Collections.sort(keys); //makes sure to keep order
		
		for(String tp : keys) {
			temp = mem2.get(tp).remove(0);
			if(mem2.get(tp).isEmpty()) mem2.remove(tp);
			return temp;
		}
		return null;
	}
	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		for(Expression exp : sortby) {
			involvedExp.add(exp);
		}
		return involvedExp;
	}
}
