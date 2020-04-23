package nodes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

public class AggregatorNode extends OperatorNode{
	
	private ArrayList<SelectExpressionItem> aggs;
	private Hashtable<String, HashSet<PrimitiveValue>> mem;	// mem keeps track of values we've seen before. used in DISTINCT
	private Tuple temp, prev = null;
	private Function fu, tp = new Function();
	private PrimitiveValue val;
	
	public AggregatorNode(SQLParse parser,  ArrayList<SelectExpressionItem> aggs) {
		super(parser);
		this.aggs = aggs;
	}
	
	public Tuple next() {
		if (children.isEmpty()) return null;
		mem = new Hashtable<String, HashSet<PrimitiveValue>>();	// must treat every group of queries you are aggregating separately
		
		for(SelectExpressionItem agg : aggs) {
			mem.put(agg.toString(), new HashSet<PrimitiveValue>());
		}
		
		temp = children.get(0).next();
		if (temp == null) return null;
		
		prev = new Tuple();
		
		while(temp != null) {
			for(SelectExpressionItem agg : aggs) {	
				fu = (Function) agg.getExpression();
				
				if(fu.isDistinct()) { // if fxn is distinct, evaluate everything inside DISTINCT()
					try {
						val = eval.evaluate(fu.getParameters().getExpressions().get(0), temp);
						if(inSet(val, agg.toString())) {
							if(agg.getAlias() != null) temp.add(agg.getAlias(), prev.get(agg.getAlias()));
							else temp.add(agg.toString(), prev.get(agg.toString()));
							continue;
						}else {
							mem.get(agg.toString()).add(val);
						}
					} catch (SQLException e) {}
				}
				
				if(fu.getName().toUpperCase().equals("AVG")) {		
					try {
						tp.setName("SUM");
						tp.setParameters(fu.getParameters());
						temp.add(fu.toString() + ".SUM", eval.evaluate(tp, temp, prev.get(fu.toString() + ".SUM")));
					} catch (SQLException e) {}
					
					try {
						tp.setName("COUNT");
						tp.setParameters(fu.getParameters());
						temp.add(fu.toString() + ".COUNT", eval.evaluate(tp, temp, prev.get(fu.toString() + ".COUNT")));
					} catch (SQLException e) {}
				}
				
				try {
					if(agg.getAlias() != null) temp.add(agg.getAlias(), eval.evaluate(fu, temp, prev.get(agg.getAlias())));
					else temp.add(agg.toString(), eval.evaluate(fu, temp, prev.get(agg.toString())));
				} catch (SQLException e) {}
			}

			
			prev = temp;
			temp = children.get(0).next();
		}
		
		for(SelectExpressionItem agg : aggs) { //remove temporary columns
			fu = (Function) agg.getExpression();
			if(fu.getName().toUpperCase().equals("AVG")) {
				prev.remove(fu.toString() + ".COUNT");
				prev.remove(fu.toString() + ".SUM");
			}
		}
		
		return prev;
	}
	
	private boolean inSet(PrimitiveValue val, String fun) {	// chick if this primitive value is in the set for this specific column
		
		for (PrimitiveValue d: mem.get(fun)) {
			try {
				if(eval.equals(val, d).toBool()) return true;
			} catch (Exception e) {}
		}		
		return false;
	}
	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		for(SelectExpressionItem agg : aggs) {
			involvedExp.add(agg.getExpression());
		}
		return involvedExp;
	}
}