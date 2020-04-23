package nodes;
import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;


public class SelectionNode extends OperatorNode {
    public Expression exp;
	Hashtable<String, String> alias = new Hashtable<String, String>();
	Tuple temp;

    public SelectionNode(SQLParse parser, Expression exp) {
        super(parser);
        this.exp = exp;
    }

	public Tuple next() {
		if (children.isEmpty()) return null;

        try {
			do{
			    temp =  children.get(0).next();
			}while(temp != null && !(eval.evaluate(exp, temp).toBool()));	// evaluate expression by passing the expression and the tuple
		} catch (SQLException e) {			// will keep returning next as long as the expression isn't equal to true. it will return next child until one is equal to true or null
			temp = null;			// whichever of the two it finds, once it exists the do-while, it returns the tuple
		}

       return temp;
    }
	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		involvedExp.add(exp);
		
		return involvedExp;
	}
}