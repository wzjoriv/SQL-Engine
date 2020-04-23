package main;
import java.util.ArrayList;
import java.util.Hashtable;

import net.sf.jsqlparser.expression.PrimitiveValue;

// class that keeps track of all the elements
// each row = a tuple
// Tuple class manages each row
// e.g. if you want to add/remove certain columns, or change the variable in a column

/*
 * Tuple class keeps track of two things:
 * 	1) order in which the columns exists		(order = all the column names the tuple has in their respective order)
 * 	2) hashtable to keep the values of the column
 */

public class Tuple {
	public ArrayList<String> order = new ArrayList<String>();
	Hashtable<String, PrimitiveValue> hash = new Hashtable<String, PrimitiveValue>();
	
	public Tuple() {}
	
	public Tuple(String[] keys, PrimitiveValue[] values) {
		addColumns(keys, values);			// keys = column names, values = list of primitive values with the specific value
											// order of columns matters here (same order that the primitive values come in)
	}										// when you get all the columns and values, add them
	
	public PrimitiveValue get(String column) {	//1st get(): pass columns. returns primitive values in the column
		return hash.get(column); //Return null if not there
											// goes into the hash table and returns primitive value for that column
	}
	
	public void set(String column, PrimitiveValue value) {
		
		hash.put(column, value);			// use set() ONLY if the column already exists and you want to update it
	}										// hashtable uses 'put' to add a column AND to change a column
	
	public void add(String column, PrimitiveValue value) {		// use add() to add a new column into the order
		
		order.add(column);			// adds the column to the right side in the order
		set(column, value);			// set() puts the value into the hashtable
	}
	
	public void remove(String column) {		// removes a certain column from a tuple
							
		order.remove(column);				// order.remove() looks for the column from the arraylist
		hash.remove(column);				// hash.remove() looks for the column from the hashtable
	}
	
	public PrimitiveValue[] get() {			// 2nd get: don't pass any columns. returns all the values.
		PrimitiveValue[] temp = new PrimitiveValue[order.size()];
		
		for(int i = 0; i < order.size(); i++) temp[i] = hash.get(order.get(i));
		
		return temp;
	}
	
	public String[] getColumnNames() {			// takes this.order() and returns this.order() as a stringarray of all the columns
		return GetStringArray(this.order);		
	}

	public String getColumnName(String column) {		// getColumnName() ... if SELECT A or SELECT R.A ... how do you distinguish between them?
		if(this.order.contains(column)) return column;	// how to match R.A or only A? how to match it to the column itself?
														// returns full column name: R.A          
														// We are using this convention to store col names: ["R.A", "R.B"]    must map A to R.A
														// 													["S.B", "S.C"] 
		
		for(String s : this.order) {					// if not in above format, check every string in "order" to see if it contains a column that has a period "."
			if(s.contains("." + column)) return s;
		}
		
		return null;
	}
	
	public String getTable(String column) {
		String tp = getColumnName(column);
		
		return tp.substring(0, tp.indexOf('.'));
	}
	
	public boolean containsColumn(String column) {		// if you already have the column in the proper format (i.e. R.A), it checks to see if hashtable contains that column
		return hash.containsKey(column);
	}
	
	public void addColumns(String[] keys, PrimitiveValue[] values) {	//pass array of column names and array of value names, it will add those keys into order & values into the hashtable (i.e. adds to the tuple)
		if(keys.length != values.length) throw new UnsupportedOperationException();
		
		for(int i = 0; i < keys.length; i++) {
			hash.put(keys[i], values[i]);
			order.add(keys[i]);
		}
	}

	public Tuple keepColumms(ArrayList<String> keep) {	// tuple will keep all the columns you pass in here & it will delete all other columns
		order = new ArrayList<String>();
											// in a SELECT statement, you enter columns in the order you want them
		for(String s : keep) {							// enter for loop to see if hash table contains that column(s)
			if(hash.containsKey(s)) order.add(s);		// if it contains the column you want to keep:	adds it into order
			else hash.remove(s);						// if hash table doesn't contain column you want to keep: removes it
		}
		
		return this;
	}

	public Tuple dropColumms(ArrayList<String> drop) {	// drops all the columns you specify. returns the remaining undropped columns
		order = new ArrayList<String>();
		
		for(String s : drop) {
			if(!hash.containsKey(s)) order.add(s);		// opposite of line 93
			else hash.remove(s);
		}
		
		return this;
	}

	public String toString() {							// shows how it prints when we enter System.out.print()
		PrimitiveValue arr[] = get();
		
		if(arr.length < 1) return "";
		
		String temp = arr[0].toString();				// takes the first string
		
		for(int i = 1; i < arr.length; i++) temp += "|" + arr[i].toString();		// adds a line to the string with the other parts
								
		
		return temp;
	}
	
	public static String[] GetStringArray(ArrayList<String> arr) { 	// takes arraylist of strings and converts it into a string array. returns the string array
		  
        // declaration and initialize String Array 
        String str[] = new String[arr.size()]; 
  
        // ArrayList to Array Conversion 
        for (int j = 0; j < arr.size(); j++) { 
  
            // Assign each value to String array 
            str[j] = arr.get(j); 
        } 
  
        return str; 
    }
	
	public Tuple merge(Tuple temp) {	// used a lot (x-product, join). merges two tuples together.
		
		Tuple newT = new Tuple(GetStringArray(this.order), this.get());
		newT.addColumns(GetStringArray(temp.order), temp.get());
		
		return newT;					// tuple3 = tuple1.merge(tuple2);	
	}									// tuple1 would have elements on the LHS
										// tuple2 would have the elements on the RHS
}