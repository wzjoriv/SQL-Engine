package nodes;

import main.SQLParse;
import main.Tuple;
import net.sf.jsqlparser.expression.*;
import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;


public class TableNode extends OperatorNode {
	
	BufferedReader reader = null;
	File file = null;
	public String[] schemas;
	String[] dataType;
	String line;
	String temp = "";
	PrimitiveValue[] tempPrimitive;
	String[] tempCols;
	Queue<Tuple> queue = new LinkedList<Tuple>();
	public String tableName;

	// all operator nodes need to get the first two parameters
	// pass file class (the file itself). pass the schema  (arraylsist of name of columns). dayatype (like INT, STRING, DOUBLE, etc.)
	public TableNode(SQLParse parser, File file, String[] schemas, String[] dataType, String tableName){
		super(parser);
		this.file = file;
		this.schemas = schemas;
		this.dataType = dataType;
		this.tableNames = new HashSet<String>();
		this.tableNames.add(tableName);
		this.tableName = tableName;
		
		reset();
	}
	
	public HashSet<String> tablesInvolved() {	// return array list that the table node has because it can't ask the children what the table node is. can only return it
		return tableNames;
	}
	
	public void reset() {	// resets file reader and buffer reader to start reading from beginning of file (in case we implement our join in some way)
		try {
			FileReader fileReader = new FileReader(file);
			reader = new BufferedReader(fileReader);
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	// if you find info after you read it, you return it right away. you don't add it to the queue
	public Tuple next(){
		
		if(!queue.isEmpty()) return queue.remove();		// if queue has info, we dont read anything. just return info inside queue > queue remove
		
		try {
			if ((temp = reader.readLine()) == null) return null;	// if queue is empty, it still may have values. so you read the line. if line is null, return null
		} catch (IOException e) {return null;}	// if line has information there, you return new tuple with that info
		
		return new Tuple(schemas, parseLine()); // cuts out each value in "1|2|3|4|5" data in that format. it will convert each value to its datatype
	}
	
	
	// if you find info after you read it, you add it to the queue
	public boolean hasNext() {		// actually valuable hasNext here() bc table node is last node. use queue that loads every tuple that it reads. if queue isn't empty, it hasNext() -> returns true bc it has values
		if(!queue.isEmpty()) return true;
		
		try {
			if ((temp = reader.readLine()) == null) return false; // if queue is empty, it doesn't automatically tell you that there's no more to read. now you have to read the next line in the file
		} catch (IOException e) {return false;}	// if you read it and temp = null, that means you actually got to the end. returns false.
		
		queue.add(new Tuple(schemas, parseLine())); // if queue is empty, check if there's more info in the file > return true. 
								// schemas is column names from the constructor. parseline gets us the array of primitive values
		return true;
	}
	
	public PrimitiveValue[] parseLine() {	// returns the actual data, the string
		
		tempCols = temp.split("\\|");			// splits by the pipe symbol |    separates into array of strings
		tempPrimitive = new PrimitiveValue[tempCols.length]; // after we make array of prim values, we have to make array of the actual elements
		
		for(int i = 0; i < tempCols.length; i++) {	
			switch(dataType[i].toUpperCase()) {	// datatype array we got in the constructor. it tells us what type of data each element is
				case "STRING":
					tempPrimitive[i] = new StringValue(tempCols[i]); // if element is empty string
					break;
				case "INT":
					if(tempCols[i].equals("")) tempPrimitive[i] = new NullValue();	// if empty
					else tempPrimitive[i] = new LongValue(tempCols[i]); 
					break;
				case "DECIMAL":
				case "DOUBLE":
					if(tempCols[i].equals("")) tempPrimitive[i] = new NullValue();	// if empty
					else tempPrimitive[i] = new DoubleValue(tempCols[i]); 
					break; 
				case "DATE":
					if(tempCols[i].equals("")) tempPrimitive[i] = new NullValue();	// if empty
					else tempPrimitive[i] = new DateValue(tempCols[i]); 
					break;
				default: 
					if(dataType[i].toUpperCase().contains("CHAR")) {		// char is weird so we convert it to string (after checking if it's equal to empty)
						tempPrimitive[i] = new StringValue(tempCols[i]);	

					}else tempPrimitive[i] = new NullValue();
			}
		}
		
		return tempPrimitive;	// return array of primitive values
	}
	
}