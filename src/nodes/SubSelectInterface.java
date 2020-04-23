package nodes;

import java.util.HashSet;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.PrimitiveValue;

public class SubSelectInterface extends OperatorNode{
	
	private Tuple temp;
	private String str;
	private PrimitiveValue value;
	
	// This should be something else maybe
	public SubSelectInterface(SQLParse parser, String str) {
		super(parser);
		this.str = str;
	}

	// replaces old table name with new one
	public Tuple next() {
		if (children.isEmpty()) return null;
		
		temp = children.get(0).next();
		if(temp == null) return null;
		
		String[] columnNames = temp.getColumnNames();
		addTable(temp, columnNames);
		
		return temp;
	}
	
	public HashSet<String> tablesInvolved() {
		
		for(OperatorNode c : children) {
			c.tablesInvolved();
		}
		
		tableNames = new HashSet<String>();
		tableNames.add(str);
		return tableNames;
	}

	private void addTable(Tuple tuple, String[] columns) {	// reads every column, gets its value, and stores it in a primitive value, and then remove that coluumn frmo the tuple
		for (String columnName : columns) {
			value = tuple.get(columnName);
			tuple.remove(columnName);
			int index = columnName.indexOf('.');
			
			if (index > 0) {
				columnName.replace(columnName.substring(0, index), str+'.');
			}
			else {
				columnName = str +'.'+columnName;
			}

			tuple.add(columnName, value);
		} // then it finds out where the index of the dot '.' is
	}	// replaces with new alias (str)
		// once it is renamed, the column is added back into the tuple with the newly edited name (to have the new alias instead of original table name)
}		// and also adds the value 