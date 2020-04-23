package nodes;

import main.SQLParse;
import main.Tuple;

public class LimitNode extends OperatorNode{

	private long limitvalue;
	private long offset;
	private long count;
	
	
	public LimitNode(SQLParse parser, long limitvalue, long offset) {
		super(parser);
		this.count = 0;				// increments
		this.offset = offset;		// offsets the value of the limit
		this.limitvalue = limitvalue;
	}
	
	@Override
	public Tuple next() {
		if (children.isEmpty()) return null;
		
		while (offset > 0) {
			if (children.get(0).hasNext()) children.get(0).next();
			offset--;
		}
		
		if(children.get(0).hasNext() && count < limitvalue) { // also checks if count value is less than the limit value
			count++;
			return children.get(0).next();
		}
		
		return null;
	}
	
	public boolean hasNext(){
		
		return (children.get(0).hasNext()) && (count < limitvalue);
	}
}
