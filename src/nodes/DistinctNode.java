package nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

public class DistinctNode extends OperatorNode{
	private Tuple temp;
	HashSet<Tuple> mem;	// set that keeps track of the things we see
	private List<SelectExpressionItem> distinctItems;
	
	public DistinctNode(SQLParse parser, List<SelectExpressionItem> distinctItems) { //distinctItems
        super(parser);
        this.distinctItems = distinctItems;
        mem = new HashSet<Tuple>();
    }

	@Override
	public Tuple next() {
		if (children.isEmpty()) return null;
		
		do {
			temp = children.get(0).next();
		}while(temp != null && inSet(temp));		// if the value it isnt null and it's also equal to a value in the set... keep repeating
		
		if(temp != null) mem.add(temp);		//if value isn't null, add temp to mem
		
		return temp;
	}
	
	private boolean inSet(Tuple tuple1) {	// for loop to check every tuple in mem. checks the tuple you pass vs all the tuples in mem
		for (Tuple t: mem) {
			if(sameTuple(tuple1, t)) return true;
		}
		
		return false;
	}
	
	private boolean sameTuple(Tuple tuple1, Tuple tuple2) {	// returns true if tuples are same, returns false if not same
		try {	
			for (String currColumn : tuple1.getColumnNames())	
				// Case of Distinct on specific columns
				if(distinctItems != null) {		
					if(distinctItems.toString().contains(currColumn)) {
						if (!eval.equals(tuple1.get(currColumn), tuple2.get(currColumn)).toBool()) // retrieve 2 tuples, evaluator checks if they're equal (true) or not (false)
							return false;
					}
				}
			
				// Case of Distinct by itself
				else {
					if (!eval.equals(tuple1.get(currColumn), tuple2.get(currColumn)).toBool())
						return false;
				}

		} catch (Exception e) {}
		
		return true;
	}
	

	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		for(SelectExpressionItem exp : distinctItems) {
			involvedExp.add(exp.getExpression());
		}
		return involvedExp;
	}
}