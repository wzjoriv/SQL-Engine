package main;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SQLEngine {

	public static void main(String[] args) { 
		RATree tree;
		SQLParse parser;
		Optimizer opt;
		String fullQuery = "";
		String queries[] = null;
		
		if (args.length < 1) {
		   System.err.println("Invalid arguments count:" + args.length);
		   System.exit(0);
		}
		
		parser = new SQLParse(args[1], args[2]);
		
        try {
			fullQuery = new String(Files.readAllBytes(Paths.get(args[1]))); //Read entire file
		} catch (IOException e1) {
			System.exit(0);
		}
        
        queries = fullQuery.split(";");
        
        for (int i = 0; i < queries.length; i++) {
        	queries[i] = queries[i] + ";";
        }
        
	    for (int i = 0; i < queries.length; i++) {
	        try {
	        	tree = parser.CreateTree(queries[i]);
	        	if(tree!=null) {
	        		opt = new Optimizer(tree);
	        		tree.printTree();
	        		System.out.println("----------------------------------------------------------");
	        		opt.optimize();
	        		tree.printTree();
	        		System.out.println("----------------------------------------------------------");
	        		tree.computeTree();
	        		//System.gc(); //about 2 extra seconds
	        	}
	        } catch (Exception e) {
	        	//e.printStackTrace();
	        } catch (Error e) {} //based on what Kul suggested
	        
	        System.out.println("=");
        }

    }
}
		
		

