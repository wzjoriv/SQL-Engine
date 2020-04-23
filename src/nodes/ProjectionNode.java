package nodes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

public class ProjectionNode extends OperatorNode {
	private ArrayList<String> keepColumns;
	private List<SelectExpressionItem> keepExpressions = null;
	private Tuple temp;
// arraylist of columns we want to keep
	// smoetimes we dont have columns, sometimes we have expressions like (A+B), so we have expression list
	// fyi columns by themselves are also considered expressions
	// we have list of actual expressions themselves, and we have a list of aliases
	public ProjectionNode(SQLParse parser, ArrayList<String> keepColumns, List<SelectExpressionItem> keepExpressions) {
		super(parser);
		this.keepColumns = keepColumns;
		this.keepExpressions = keepExpressions;
	}
	
	public ProjectionNode(SQLParse parser, ArrayList<String> keepColumns) {
		super(parser);
		this.keepColumns = keepColumns;
	}

	public Tuple next() {
		if (children.isEmpty()) return null;
		if((temp = children.get(0).next()) == null) return null;
		
		if(keepExpressions != null || !keepExpressions.isEmpty()) evaluateExpressions(temp);
		
		return temp.keepColumms(keepColumns);
	}
	
	public void evaluateExpressions(Tuple tuple) {
		for(SelectExpressionItem item : keepExpressions) {
			try {
				if(item.getAlias() != null) tuple.set(item.getAlias(), eval.evaluate(item.getExpression(), tuple));
				else tuple.set(item.toString(), eval.evaluate(item.getExpression(), tuple));
			} catch (SQLException e) {}
		}
	}
	
	public ArrayList<Expression> getExpressionsInvolved() {
		involvedExp = new ArrayList<Expression>();
		
		for(SelectExpressionItem exp : keepExpressions) {
			involvedExp.add(exp.getExpression());
		}
		return involvedExp;
	}
}