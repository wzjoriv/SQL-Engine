package main;
import java.util.Hashtable;

import net.sf.jsqlparser.schema.Column;
import nodes.JoinNode;
import nodes.OperatorNode;
import nodes.SelectionNode;
import nodes.SubSelectInterface;
import nodes.TableNode;

public class RATree {					// root: top of the tree
	protected OperatorNode root;		// keeps track of the root: what is the root operator node?
	Tuple temp;							// used to collect the result
	Hashtable<String, String[]> schemas;
	Hashtable<String, String> alias;
	SQLParse parser;

	public RATree() {}					// empty constructor. root isn't set to anything; left empty
	
	public RATree(OperatorNode root) {			// constructor where we pass root into constructor itself
		setRoot(root);							// sets this root as the root you passed
	}
	
	public void setRoot(OperatorNode root) {
		this.root = root;
	}
	
	public void setParser(SQLParse parser) {
		this.parser = parser;
		this.schemas = parser.schemas;
		this.alias = parser.alias;
	}
	
	public OperatorNode getRoot() {
		return root;
	}
	
	public String getNameFromAlias(String alia) {
        if (alias.get(alia) == null) return alia;
        return alias.get(alia);
    }

	public String getColumnName(Column c) {
		
		String name = c.getColumnName();

		if(c.getTable() != null && c.getTable().getName() != null) name = getNameFromAlias(c.getTable().getName()) + "." + c.getColumnName();
		
		return getColumnFormatted(name);
	}
	
	public String getTable(String column) {
		String tp = getColumnFormatted(column);
		
		return tp.substring(0, tp.indexOf('.'));
	}
	
	public String getColumnFormatted(String name) {
		
		for(String[] t : schemas.values())
			for(String s : t)
				if(s.equals(name)) return name;
		
		for(String[] t : schemas.values())
			for(String s : t)
				if(s.contains("." + name)) return s;
		
		return name;
	}
	
	public void computeTree() {		//keeps running to get the next tuple until it reads NULL
		while((temp = root.next()) != null) {		//as long as temp =/= null, keep printing.
			System.out.println(temp);
		}
	}
	
	public void printTree() {		// 2nd print tree function. printTree(root) shows from the root
		printTree(root);			// printTree() prints from the node you specify in the (), not necessarily the root.
	}
	
	public void printTree(OperatorNode root) {					//recursion:
		for(OperatorNode child : root.children) {				// for the node you pass here, it looks at all the children
			if(root instanceof SelectionNode) {
				System.out.print(root +"(" +((SelectionNode)root).exp+") -> child " + child);
			}else if(root instanceof JoinNode) {
				System.out.print(root +"(" +((JoinNode)root).exp+") -> child " + child);
			}else System.out.print(root + " -> child " + child);
			
			if(child instanceof TableNode || child instanceof SubSelectInterface) System.out.println(child.tableNames);
			else System.out.println("");
			
			printTree(child);									// calls printTree on the child
		}
	}			// table node doesn't print because it doesn't have any children (it is the last node)
}
