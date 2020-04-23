package main;
import java.sql.SQLException;
import java.util.Hashtable;

import net.sf.jsqlparser.eval.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;
import nodes.OperatorNode;
import nodes.SubSelectInterface;

// EVALUATOR: PASS IN EXPRESSION
// 				RETURNS WHATEVER THE RESULT IS

// e.g. if you pass in addition, it will return the sum to you
// if you pass in multiplication, it will return the product

/* all operator classes haev an instance of Evaluator in it because they all have to evaluate something
 *  E.g. Order By... need to evaluate what is bigger, what is smaller
 */

public class Evaluator {
    
    Eval eval;
    Hashtable<String, String> alias;
    PrimitiveValue temp;				// not normal integers.. This is a new primitive value class
    IsNullExpression tp;
    Hashtable<String, OperatorNode> subs;
	public SQLParse parser;
    
    public Evaluator(SQLParse parser) {		// when you call Evaluator, pass in alias so the Evaluator 
    	
    	this.parser = parser;
    	this.alias = parser.alias;
    	this.eval = new Eval(){public PrimitiveValue eval(Column c) throws SQLException {return null;}};  // empty for now. forget this line
    	subs = new Hashtable<String, OperatorNode>();
    }
    
    public String getNameFromAlias(String alia) {	//if we have From R as COOL, we can also do SELECT COOL.A. Must keep track of aliases
		if(alias.get(alia) == null) return alia;		// check if alias is inside the hashtable. if not in hashtable, there is an alias.
		return alias.get(alia);									// then we must get the actual column name from the alias
	}
	
    public PrimitiveValue evaluate(Expression exp, Tuple tuple) throws SQLException{ // the fxn that evaluates the expression you pass into it (also pass in a tuple)
    	// we pass in the tuple in case we have COUNT(A * B + 5), A * B + 5 are an expression. A, B are columns. Must replace with proper value.
    	// in one around, if A = 2, B = 4, we have 2 * 4 + 5. we would have to go into the tuple to get the proper values and replace A, B with them
		
		eval = new Eval() {  // to handle some of the special properties the evaluator must evaluate (such as columns)
			public PrimitiveValue eval(Column c) { // if COUNT(A)... get the column name A. returns the column name as a string.
				String name = c.getColumnName();

				if(c.getTable() != null && c.getTable().getName() != null) name = getNameFromAlias(c.getTable().getName()) + "." + c.getColumnName();
				// ^ sometimes the column may not be passed as a string. if they put SELECT R.A or SELECT A
				// getColumnName() always returns a value (don't need to check for null)
				// sometimes if it is in the format we store it (i.e. R.A), make sure R is not an alias but the actual name itself
				// if no table reference, e.g. just A, we must figure out what A actually is
				
				return tuple.get(tuple.getColumnName(name));
			}
			
			public PrimitiveValue eval(Function f) {
				
				switch(f.getName().toUpperCase()) {		//we can implement later. not required
					case "EXTRACT":
						
						break;
					case "SUBSTRING":
						
						break;
					case "DATE_PART":
						try {
							DateValue temp = (DateValue) evaluate(f.getParameters().getExpressions().get(1), tuple);
							
							switch(((StringValue) f.getParameters().getExpressions().get(0)).toRawString().toUpperCase()) {
								case "YEAR":
									return new LongValue(temp.getYear());
								case "MONTH":
									return new LongValue(temp.getMonth());
								case "DATE":
									return new LongValue(temp.getDate());
							}
						} catch (SQLException e) {}
						break;
				}
								
				return null;
			}
			
			public PrimitiveValue eval(InExpression i) throws SQLException {
				
				PrimitiveValue temp = evaluate(i.getLeftExpression(), tuple);
				Tuple temp2;
				
				if(i.getItemsList() instanceof ExpressionList){
					for(Expression exp : ((ExpressionList) i.getItemsList()).getExpressions()) {
						if(evaluate(new EqualsTo(temp, exp), tuple).toBool()) return BooleanValue.TRUE;
					}
				} else {
					if(!subs.containsKey(i.toString())) {
						
						SubSelect sb = (SubSelect) i.getItemsList();
						OperatorNode top;
			            
			            if (sb.getAlias() != null) {
			            	top = new SubSelectInterface(parser, sb.getAlias());
			            	top.addChild(parser.buildSelect(sb.getSelectBody()));
			            }else {
			            	top = parser.buildSelect(sb.getSelectBody());
			            }
						subs.put(i.toString(), top);
					}
					
					while((temp2 = subs.get(i.toString()).next()) != null) {
						if(evaluate(new EqualsTo(temp, temp2.get()[0]), tuple).toBool()) return BooleanValue.TRUE;
					}
					
					subs.get(i.toString()).reset();
				}
				
				return BooleanValue.FALSE;
			}
			
			public PrimitiveValue eval(ExistsExpression f) {	// not required
				
				return null;
			}
		};
		
		return eval.eval(exp);
	}
	
    public PrimitiveValue evaluate(Expression exp, Tuple tuple, PrimitiveValue cum) throws SQLException{   //2nd evaluate instruction that handles more complicated functions. takes in exp, tuple, and cumulative
		// cumulative is just a number you are collecting
		eval = new Eval() {
			public PrimitiveValue eval(Column c) {	
				try {
					return evaluate(c, tuple);		// for the more simple evaluate

				} catch (SQLException e) {}
				return null;
			}
			
			public PrimitiveValue eval(Function f) throws SQLException {
				
				switch(f.getName().toUpperCase()) {	//convert count() or cOuNt() to COUNT() 
				
				// if we have something like MAX(A+B) or MAX(A), we can put whatever we want inside the ()
				
					case "MAX":
						temp = evaluate(f.getParameters().getExpressions().get(0), tuple);	// convert all params to expression. stores result after evaluating into temp
						
						if(cum == null || eval(new GreaterThan(temp, cum)).toBool()) return temp;	// check if cum is null, which happens if one of the input values is null (checks if first row. if so, it will return first row as result)
						else return cum;															// OR if we evaluate A+B and it is greater than the cumulative, return temp (the A+B result) as the new maximum. 
							
					case "MIN":	// same way that MAX works, except GreaterThan is now MinorThan
						temp = evaluate(f.getParameters().getExpressions().get(0), tuple);
						
						if(cum == null || eval(new MinorThan(temp, cum)).toBool()) return temp;
						else return cum;
					
					case "COUNT": 	// can do COUNT * or COUNT(A) or COUNT(A+B)

						if(!f.isAllColumns()) {	//checks if it is not *... then it is a single column or an expression like A+B
							temp = evaluate(f.getParameters().getExpressions().get(0), tuple);
							
							// COUNT(A) only counts true values. excludes null values
							// COUNT * counts blindly. it even includes a row if it has a null value. includes everything
							if(temp instanceof NullValue) return cum;	// cum == current count value
						}
							
						if(cum == null) return new LongValue(1);	// if all cols, check if a cumulative count exist
																	// long value 1 returned in case 

						return eval(new Addition(cum, new LongValue(1)));	// adds 1 (long) to cumulative i.e. increments the cumulative count
						
					case "AVG":
						// fake sum fxn and a fake count fxn. calculates average as fakesum/fakecount converts it to double (bc average could be a decimal)
						return evaluate(new Division(new Multiplication(tuple.get(f.toString() + ".SUM"), new DoubleValue(1.0)), tuple.get(f.toString() + ".COUNT")), tuple);
																	// multiply by 1.0 double to convert entire expression into double
					case "SUM": //e.g. SUM(A+B)
						temp = evaluate(f.getParameters().getExpressions().get(0), tuple); // first param --> call eval function --> return results
						
						if(cum == null) return temp;	// if no cumulative, meaning you only have one number, that number becomes current sum
						
						return eval(new Addition(cum, temp));	// return addition of cumulative value and the value you currently have
				}
				
				return null;
			}
		};
		
		return eval.eval(exp);
	}
	
	public PrimitiveValue equals(PrimitiveValue a, PrimitiveValue b) throws SQLException{ // to check if two primitive values are equal (pass the two primitive values)
		
		if((a instanceof StringValue || a instanceof StringValue)) {
			if(a.toString().equals(b.toString())) {
				return BooleanValue.TRUE;
			}else {
				return BooleanValue.FALSE;
			}
		}
		
		return eval.eval(new EqualsTo(a, b));
	}
	
	public PrimitiveValue greaterThan(PrimitiveValue a, PrimitiveValue b) throws SQLException{ // check if primitive value a is greater than primitive value b
		
		if((a instanceof StringValue || a instanceof StringValue)) {
			if(a.toString().compareTo(b.toString()) > 0) {
				return BooleanValue.TRUE;
			}else {
				return BooleanValue.FALSE;
			}
		}
		
		return eval.eval(new GreaterThan(a, b));
	}
}
