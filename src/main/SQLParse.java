package main;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.*;
import nodes.*;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


public class SQLParse {

    Hashtable<String, String[]> schemas = new Hashtable<String, String[]>();
    ArrayList<String> schemasOrder = new ArrayList<String>();
    Hashtable<String, String[]> schemasType = new Hashtable<String, String[]>();
    Hashtable<String, String> alias = new Hashtable<String, String>();
    String folderPath, fileName;

    Reader input;
    RATree currentTree;
    Statement statement;
    CCJSqlParser parser;
    CreateTable table;

    public SQLParse(String path, String tmpFile) {
        folderPath = path;
        fileName = tmpFile;
    }

    public static String[] GetStringArray(ArrayList<String> arr) {

        // declaration and initialize String Array
        String str[] = new String[arr.size()];

        // ArrayList to Array Conversion
        for (int j = 0; j < arr.size(); j++) {

            // Assign each value to String array
            str[j] = arr.get(j);
        }

        return str;
    }

    public String[] getSchema(String table) {
        return schemas.get(table);
    }

    public String getNameFromAlias(String alia) {
        if (alias.get(alia) == null) return alia;
        return alias.get(alia);
    }

    //uses SQL library to detect component of the tree needed and builds it
    public RATree CreateTree(String query) throws ParseException {
        currentTree = new RATree();

        input = new StringReader(query);

        parser = new CCJSqlParser(input);
        
        statement = parser.Statement();

        if (statement instanceof Select) {
            Select select = (Select) statement;
            //select implementation here
            SelectBody body = select.getSelectBody();

            currentTree.setRoot(buildSelect(body));
            currentTree.setParser(this);
            currentTree.getRoot().tablesInvolved();

            return currentTree;

        } else if (statement instanceof CreateTable) {
            table = (CreateTable) statement;
            //create table implementation here
            String tableName = table.getTable().getName();
            ArrayList<String> columnsNames = new ArrayList<String>();
            ArrayList<String> dataTypes = new ArrayList<String>();

            for (ColumnDefinition c : table.getColumnDefinitions()) {
                columnsNames.add(tableName + "." + c.getColumnName());
                dataTypes.add(c.getColDataType().getDataType()); // Return whatever primitive data type to
            }

            String tableAlias = table.getTable().getAlias();

            //key: tableName		values: string array of column names
            schemas.put(tableName, GetStringArray(columnsNames));
            schemasType.put(tableName, GetStringArray(dataTypes));
            schemasOrder.add(tableName);

            if (tableAlias != null) alias.put(tableAlias, tableName);

            return null;

        } else {
            throw new UnsupportedOperationException();
        }
    }

    public OperatorNode addChildNode(OperatorNode currentNode, OperatorNode newNode) {

        if (currentNode != null) currentNode.addChild(newNode);
        return newNode;
    }

    public OperatorNode buildSelect(SelectBody b) {

        OperatorNode top = null, currentNode = null;

        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<SelectExpressionItem> exprs = new ArrayList<SelectExpressionItem>();
        ArrayList<SelectExpressionItem> aggs = new ArrayList<SelectExpressionItem>();


        if (b instanceof PlainSelect) {			//we all get this part

            PlainSelect plain = (PlainSelect) b;

            /**			Limit		**/
            Limit lim = plain.getLimit();
            if (lim != null) currentNode = addChildNode(currentNode, new LimitNode(this, lim.getRowCount(), lim.getOffset()));
            if (top == null) top = currentNode;
                    

            /**			Distinct		**/
            Distinct d = plain.getDistinct();
            if(d != null) currentNode = addChildNode(currentNode, new DistinctNode(this, d.getOnSelectItems()));
            if (top == null) top = currentNode;
            
            
            /**			Order by		**/
            ArrayList<OrderByElement> orderByElements = (ArrayList<OrderByElement>) plain.getOrderByElements();
            if (orderByElements != null) currentNode = addChildNode(currentNode, new OrderByNode(this, orderByElements));
            if (top == null) top = currentNode;

            
            /**			Projection		**/
            parseSelectItems(plain.getSelectItems(), currentNode, top, columns, exprs, aggs);
            currentNode = addChildNode(currentNode, new ProjectionNode(this, columns, exprs));
            if (top == null) top = currentNode;


            /**            Having			**/
            Expression havingCondition = plain.getHaving();
            if (havingCondition != null) currentNode = addChildNode(currentNode, new SelectionNode(this, havingCondition));


            /**			Aggregation			**/
            if (aggs != null && !aggs.isEmpty()) currentNode = addChildNode(currentNode, new AggregatorNode(this, aggs));

            
            /**          Group By			**/
            ArrayList<Column> sortby = (ArrayList<Column>) plain.getGroupByColumnReferences();
            if (sortby != null && !sortby.isEmpty()) currentNode = addChildNode(currentNode, new GroupByNode(this, sortby));
            

            /**            Where		**/
            Expression whereCondition = plain.getWhere();
            if (whereCondition != null) currentNode = addChildNode(currentNode, new SelectionNode(this, whereCondition));


            /**            From/Joins		**/
            List<Join> joins = (plain.getJoins() != null) ? plain.getJoins() : new ArrayList<Join>();
            FromItem from = plain.getFromItem();
            if (from != null) addChildNode(currentNode, buildJoins(from, joins)); //currentNode doesn't need to be assigned since from is the last part added
            	// FROM & JOINs... in case you have multiple tables
            /*
             * e.g. SELECT SUM(A+B) FROM R, K, L
             * plain.getFromItem() returns only R (the first one)
             * joins = returns K and L
             * 
             * You have a list of joins. you check to see if it is one table or multiple table. R is a FROM item. K, L are JOINS
             * if getJoins is null, return empty arraylist of joins.
             * if not null, add child nodes. buildJoins creates the whole bottom of tree. returns top node (a x-product node). 
             */
        } else if (b instanceof Union) {
            top = new UnionNode(this);

            for (PlainSelect u : ((Union) b).getPlainSelects()) top.addChild(buildSelect(u));
        }
        return top;	//buildselect returns top
    }

    public OperatorNode buildFrom(FromItem from) {
        OperatorNode top = null;		
        SubSelect sb;

        if (from instanceof SubJoin) {	//could be a subquery or subjoin or many other things that it could be
            from = (SubJoin) from;

            top = new CrossProductNode(this);
            top.addChild(buildFrom(((SubJoin) from).getLeft()));
            top.addChild(buildFrom(((SubJoin) from).getJoin().getRightItem()));
            
        } else if (from instanceof SubSelect) {
            sb = (SubSelect) from;
            
            if (sb.getAlias() != null) {
            	
            	top = new SubSelectInterface(this, sb.getAlias());
            	top.addChild(buildSelect(sb.getSelectBody()));
            }else {
            	top = buildSelect(sb.getSelectBody());
            }

        } else if (from instanceof Table) {	//
            from = (Table) from;
            //to get the table name:
            String tablename = getNameFromAlias(((Table) from).getName());		// check if table has alias
            if(from.getAlias() != null) alias.put(from.getAlias(), tablename);	//if there is an alias, store it in alias hashtable
            	
            String[] tableschema = schemas.get(tablename);	// schema is stored from CREATE TABLE
            File tableFile;
            
            tableFile = new File(fileName + "/" + tablename + ".csv");
            
            top = new TableNode(this, tableFile, tableschema, schemasType.get(tablename), tablename);
        }
        return top;
    }

    public OperatorNode buildJoins(FromItem from, List<Join> joins) {	//has R by itself (see From/Joins above to understand R). sets it equal to temp.
        OperatorNode temp;												// if joins is empty, it only returns top R.
        OperatorNode top = temp = buildFrom(from);						

        for (Join j : joins) {		 
        	
            top = new CrossProductNode(this);		// if we have R, take top and set it equal to a x-product.
            top.addChild(temp);						// add R as the left child.
            top.addChild(buildFrom(j.getRightItem()));	// use buildFrom to add the right item from the join

            temp = top;	//set temp = top. 	
        }
        return top;		//if no more joins, return the tree structure we just made
    }					// if we have another join, create a x-product. set top = that xproduct. add to top the child (which is temp) on the left side. add the other to the right
    					// now return top
    
    public void parseSelectItems(List<SelectItem> items, OperatorNode currentNode, OperatorNode top, ArrayList<String> columns, ArrayList<SelectExpressionItem> exprs, ArrayList<SelectExpressionItem> aggs) {
    	//columns returns A+B as a string. if one of them has an alias , like (A+B) AS J, J will be in the columns instead of A+B
    	// if we have the * operation, convert * into a list of all the columns that exist
    	// if we have R.*, get all the columns from R and put them into columns
    	// exprs keeps everything that is an expression. Doesn't keep * or R.*. Keeps (A+B) and just A (because FYI columns themselves are also considered expressions)
        // aggregator (aggs) are passed in as SelectExpressionItem
    	SelectExpressionItem in;

        for (SelectItem item : items) {
            if (item instanceof AllColumns) {	//if it finds a * operation, go thru all our currently saved schemas. Write the names of the columns into into "columns" arraylist
                for (String table : schemasOrder) //may need to change to be depending on tree order
                    for (String column : schemas.get(table))
                        columns.add(column);

            } else if (item instanceof AllTableColumns) { // get table name. using the table name, we access the schema for only that table. then get all columns (in for loop) into "columns" arraylist
                for (String column : schemas.get(getNameFromAlias(((AllTableColumns) item).getTable().getName())))
                    columns.add(column);

            } else if (item instanceof SelectExpressionItem) { // if it's not a * or R.*   ->   first check if it has an alias (=/= null).
                in = (SelectExpressionItem) item;

                if (in.getAlias() != null) columns.add(in.getAlias());	// if it has an alias, save the alias into "columns" arraylist. 
                else columns.add(in.toString());						//if no alias, take the expression (A+B) as is, turn it into a string, name the column as string of "A+B"

                if (in.getExpression() instanceof Function) aggs.add(in);	// after puttin in names, we check for Functions
                else exprs.add(in);				//SUM(B) is an example of a Function.
            }			// if it is a Function, add it to aggregators (aggs). if not a Function, add ad Expression (exprs)
        }
    }
}