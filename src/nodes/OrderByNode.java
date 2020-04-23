package nodes;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.OrderByElement;

import java.util.*;

public class OrderByNode extends OperatorNode {
	ArrayList<OrderByElement> orderByElements;
    boolean memLoaded;
	ArrayList<Tuple> mem;
    Tuple temp;
    // arraylist shows the elements we want to order by
    public OrderByNode(SQLParse parser, ArrayList<OrderByElement> orderbyElements) {
        super(parser);
        this.orderByElements = orderbyElements;
        memLoaded = false;
		mem = new ArrayList<Tuple>();
    }

    public boolean hasNext() {
    	return !(memLoaded && mem.isEmpty());
    }

    @Override		// just checks if statement is equal to null
    public Tuple next() {
		if(children.isEmpty()) return null;		// first time program runs after this line, we check to see if memory has been loaded. we go into the if statement.
		if(!memLoaded) {
			while((temp = children.get(0).next()) != null) mem.add(temp);	// do a while loop and load everything from the child into memory
			sort(mem, orderByElements);
			memLoaded = true;
		}
		if(mem.isEmpty()) return null;
		return mem.remove(0);
	}
    // we set mem = true
    //sorts the elements
    // every time they call next() we pop off the next element in the arraylist
	private void sort(ArrayList<Tuple> list, ArrayList<OrderByElement> sortby) {
		Collections.sort(list, new Comparator<Tuple>() {
			public int compare(final Tuple tuple1, final Tuple tuple2) {
				double sum = 0;
				try {
					for(int i = 0; i < sortby.size() && sum == 0; i++)
						//define scoring system for two tuples. 
						//calculate expression, check if result for tuple 1 is greater than for tuple 2
						// once we check if result is greater than, we do +1
						//	if they are the same, we do +0
						// if they are less than, we do -1
						// if ascending order, +1. if descending order, -1"    ??
						if(eval.greaterThan(eval.evaluate(sortby.get(i).getExpression(), tuple1), eval.evaluate(sortby.get(i).getExpression(), tuple2)).toBool()) sum += sortby.get(i).isAsc() ? 1 :-1;
						else if(eval.equals(eval.evaluate(sortby.get(i).getExpression(), tuple1), eval.evaluate(sortby.get(i).getExpression(), tuple2)).toBool()) sum += 0;
						else sum -= sortby.get(i).isAsc() ? 1 :-1;
				}catch (Exception e) {}
				
				return (int) sum;
			}
		}
		);
	}
	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		for(OrderByElement exp : orderByElements) {
			involvedExp.add(exp.getExpression());
		}
		return involvedExp;
	}
}
