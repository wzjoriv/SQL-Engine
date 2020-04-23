package nodes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;

public class JoinNode extends OperatorNode {
	
	Tuple left, right, temp;
	Expression correctExp;
	ArrayList<Tuple> mem;
	Hashtable<String, ArrayList<Tuple>> hash;
	Boolean memLoaded = false;
	String tpstr;
	
	public Expression exp;
	int track;
	// join is like a combination of xproduct and selection
	public JoinNode(SQLParse parser, Expression exp) {
		super(parser);
		memLoaded = false;
		track = 0;
		this.exp = exp;
		
		if(exp instanceof EqualsTo) hash = new Hashtable<String, ArrayList<Tuple>>();
		else mem = new ArrayList<Tuple>();
	}

	public Tuple next() {
		if (children.isEmpty()) return null;
		if(exp instanceof EqualsTo) return equalToNext();
		
		if(!memLoaded) {								// loads everything into memory
			while(children.get(1).hasNext()) {
				temp =  children.get(1).next();
			    if(temp == null) continue;
				mem.add(temp);
			}
			memLoaded = true;			// only runs one time
		}
		
		try {	// when you have a join, you have to pass the expression that puts the join together
			do{	// while this expression evaluated =/= true, keep trying different matches until you find a join that works and yields true
				if(track == 0) left = children.get(0).next();	// track is same as xproduct
				if(left == null) {
					mem = null;
					return null;
				}
				right = mem.get(track);
				
				track = (track + 1) % mem.size();
				temp = left.merge(right);				// merge
				
			}while(temp != null && !(eval.evaluate(this.exp, temp).toBool()));	// condition that keeps getting the next value until the condition is matched
		} catch (SQLException e) {											// once the condition is matched, 
			temp = null;
		}

		return temp;
	}
	
	public Tuple equalToNext() {
		
		if(!memLoaded) {							// loads everything into memory
			EqualsTo uq = (EqualsTo) exp;
			
			if((temp = children.get(1).next()) == null) return null;
			try {
				try {
					if(temp.getColumnName(getColumnName((Column) uq.getRightExpression())) == null) correctExp = uq.getLeftExpression();
					else correctExp = uq.getRightExpression();
				}catch(Exception e) {}
				
				//System.out.println("Entry (" + uq + "): " + correctExp);
				
				while(temp != null) {
					tpstr = eval.evaluate(correctExp, temp).toString();
					
					if(!hash.containsKey(tpstr)) hash.put(tpstr, new ArrayList<Tuple>());
					hash.get(tpstr).add(temp);
					temp = children.get(1).next();
				}
				
				if(correctExp == uq.getLeftExpression()) correctExp = uq.getRightExpression();
				else correctExp = uq.getLeftExpression();
				
				//System.out.println("Exit (" + uq + "): " + correctExp);
			}catch(Exception e) {}
			
			memLoaded = true;			// only runs one time
		}
		
		while(true) {
			//LEFT SIDE
			if(track == 0) left = children.get(0).next();	// if track = 0, we change the left child
			if(left == null) {		// null if there are no more left values
				mem = null;			// set mem to null
				return null;		// return null
			}
			
			//RIGHT SIDE
			try {
				tpstr = eval.evaluate(correctExp, left).toString();
				mem = hash.get(tpstr);
				if(mem == null) {
					track = 0;
					continue;
				}
			} catch (Exception e) {}
			
			right = mem.get(track);	//load track into the right side. do some simple % math
			track = (track + 1) % mem.size();// merge right & left sides
			
			return left.merge(right);
		}
	}
	
	public boolean hasNext() {	// not impo
		if (children.isEmpty()) return false;
		
		nextAvailable = false;
		
		for(OperatorNode child : children) {
			nextAvailable = nextAvailable || child.hasNext();
		}
		
		return nextAvailable;
	}
	
	public String getColumnName(Column c) {
		
		String name = c.getColumnName();

		if(c.getTable() != null && c.getTable().getName() != null) name = eval.parser.getNameFromAlias(c.getTable().getName()) + "." + c.getColumnName();
		
		return name;
	}
	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		involvedExp.add(exp);
		return involvedExp;
	}
}
