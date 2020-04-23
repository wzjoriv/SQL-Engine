package main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import nodes.*;

public class Optimizer {
	OperatorNode curr, prev = null, temp = null;
	Queue<OperatorNode> queue;
 	RATree tree;
 	
 	public Optimizer(RATree tree) {
 		this.tree = tree;
 	}

	public ArrayList<Expression> binaryParse(Expression exp) {
		ArrayList<Expression> found = new ArrayList<Expression>();
		if (exp instanceof AndExpression) {
			found.addAll(binaryParse(((AndExpression) exp).getLeftExpression()));
			found.addAll(binaryParse(((AndExpression) exp).getRightExpression()));
		}else found.add(exp);
		return found;
	}
	
	public Expression andMerge(ArrayList<Expression> exps) {
		if(exps.isEmpty()) return null;
		Expression out = exps.get(0);
		for(int i = 1; i< exps.size(); i++) out = new AndExpression(out, exps.get(i));
		return out;
	}
	
	public HashSet<String> columnParse(Expression exp) {
		HashSet<String> retrieve = new HashSet<>();
		
		if(exp instanceof Column) retrieve.add(tree.getColumnName((Column) exp));
		else if(exp instanceof BinaryExpression) {
			retrieve = columnParse(((BinaryExpression) exp).getLeftExpression());
			retrieve.addAll(columnParse(((BinaryExpression) exp).getRightExpression()));
		}
		else if(exp instanceof Function) retrieve = columnParse(((Function) exp).getParameters().getExpressions().get(0));
		
		return retrieve;
	}
	
	public HashSet<String> tableParse(Expression exp) {
		HashSet<String> retrieve = new HashSet<>();
		
		for(String k : columnParse(exp)) {
			retrieve.add(tree.getTable(k));
		}
		return retrieve;
	}
    
	public void insertNode(OperatorNode curr, OperatorNode newNode, int c) {
		OperatorNode temp = curr.getChild(c);
		
		curr.children.set(c, newNode);
		newNode.addChild(temp);
		
		tree.getRoot().tablesInvolved();
	}
	
	public void removeNode(OperatorNode curr, int child) {
		
		curr.children = curr.getChild(child).children;
		tree.getRoot().tablesInvolved();
	}

	public void optimize() {
		tree.getRoot().tablesInvolved();
		
		selectionPushdown();
		//tree.printTree();
		//System.out.println("----------------------------------------------------------");
		crossProductToJoin();
		//projectionPushdown();
		
	}
	
	public void selectionPushdown() {
		temp = tree.getRoot();
		queue = new LinkedList<OperatorNode>();
		ArrayList<Expression> collects = new ArrayList<Expression>();
		ArrayList<Expression> tp;
		
		queue.add(temp);
		
		while(!queue.isEmpty()) {
			curr = queue.remove();
			
			for(int c = 0; c < curr.children.size(); c++) {
				if(curr.getChild(c) instanceof TableNode) {
					tp = getExpressionsWithTable(collects, curr.children.get(c).tableNames);
					if(!tp.isEmpty()) insertNode(curr, new SelectionNode(tree.parser, andMerge(tp)), c);
					collects.removeAll(tp);
				}else if(curr.getChild(c) instanceof GroupByNode){
					tp = getExpressionsWithTable(collects, curr.children.get(c).tableNames);
					if(!tp.isEmpty()) insertNode(curr, new SelectionNode(tree.parser, andMerge(tp)), c);
					collects.removeAll(tp);
					
					if(!tp.isEmpty()) queue.add(curr.getChild(c).getChild(0));
					else queue.add(curr.getChild(c));
				}else if(curr.getChild(c) instanceof CrossProductNode){
					
					for(int i = 0; i < collects.size(); i++) {
						if(joinCondition(collects.get(i)) && hasTables(collects.get(i), (CrossProductNode) curr.getChild(c))) {
							insertNode(curr, new SelectionNode(tree.parser, collects.remove(i)), c);
							i--;
							break;
						}
					}
					queue.add(curr.getChild(c));
				}else if(curr.getChild(c) instanceof SelectionNode){
					tp = binaryParse(((SelectionNode) curr.getChild(c)).exp);
					
					removeNode(curr, c);
					collects.addAll(tp);
					
					queue.add(curr); //add curr because we removed child so it needs to be checked again
				}else{
					queue.add(curr.getChild(c));
				}
			}
		}

		//System.out.println("Alias: " + tree.alias);
		//System.out.println("Remainig Ands: " + collects);
	}
	
	public void crossProductToJoin() {
		
		// :')
		temp = tree.getRoot();
		queue = new LinkedList<OperatorNode>();
		Expression exp;
		
		queue.add(temp);
		
		while(!queue.isEmpty()) {
			curr = queue.remove();
			
			for(int c = 0; c < curr.children.size(); c++) {
				if(curr.getChild(c) instanceof SelectionNode && curr.getChild(c).getChild(0) instanceof CrossProductNode){
					exp = ((SelectionNode) curr.getChild(c)).exp;
					if(joinCondition(exp)) {
						removeNode(curr.getChild(c), 0);
						insertNode(curr, new JoinNode(tree.parser, exp), c);
						removeNode(curr.getChild(0), c);
					}
					
					queue.add(curr.getChild(c));
				}else {
					queue.add(curr.getChild(c));
				}
			}
		}
		
	}
	
	/*public void projectionPushdown() {
		temp = tree.getRoot();
		queue = new LinkedList<OperatorNode>();
		Queue<HashSet<String>> collects = new LinkedList<HashSet<String>>();
		HashSet<String> tp;
		
		queue.add(temp);
		collects.add(getColumns(curr.getExpressionsInvolved()));
		
		while(!queue.isEmpty()) {
			curr = queue.remove();
			
			for(OperatorNode c : curr.children) {
				if(c instanceof TableNode) {
					insertNode(curr, new ProjectionNode(tree.parser, new ArrayList<String>(getColumnsWithTable(collects.remove(), ((TableNode) c).tableNames))));
				}else {
					queue.add(c);
					tp = getColumns(c.getExpressionsInvolved());
					tp.addAll(collects.remove());
					collects.add(tp);
				}
			}
		}
	}*/
	
	public HashSet<String> getColumnsWithTable(HashSet<String> set, HashSet<String> tables){
		HashSet<String> out = new HashSet<String>();
		
		for(String str : set)
			if(tables.contains(tree.getTable(str))) out.add(str);
		
		return out;
	}
	
	public HashSet<String> getColumns(ArrayList<Expression> exps) {
		HashSet<String> colm = new HashSet<String>();

		for(Expression exp : exps) colm.addAll(columnParse(exp));
		
		return colm;
	}
	
	public HashSet<String> inter(HashSet<String> s1, HashSet<String> s2){
		HashSet<String> intersection = new HashSet<String>(s1); // use the copy constructor
		intersection.retainAll(s2);
		return intersection;
	}
	
	public ArrayList<Expression> getExpressionsWithTable(ArrayList<Expression> set, HashSet<String> tables){
		ArrayList<Expression> out = new ArrayList<Expression>();
		HashSet<String> tabs;
		
		for(Expression exp : set) {
			tabs = tableParse(exp);
			if(!(inter(tabs, tables).isEmpty()) && tabs.size() == tables.size()) {
				out.add(exp);
			}
		}
		
		return out;
	}

	public boolean joinCondition(Expression exp) {
		if (exp instanceof EqualsTo) {
			Expression lhs = ((EqualsTo) exp).getLeftExpression();
			Expression rhs = ((EqualsTo) exp).getRightExpression();
			if(lhs instanceof Column && rhs instanceof Column) {
				if (!(tree.getTable(tree.getColumnName((Column) lhs)).equals(tree.getTable(tree.getColumnName((Column) rhs))))) {
					return true;
				}
			}
		}
		return false;
		
	}

	public boolean hasTables(Expression exp, CrossProductNode op) {
		
		if (exp instanceof EqualsTo) {
			Expression lhs = ((EqualsTo) exp).getLeftExpression();
			Expression rhs = ((EqualsTo) exp).getRightExpression();
			if(lhs instanceof Column && rhs instanceof Column) {
				if(op.getChild(0).tableNames.contains(tree.getTable(tree.getColumnName((Column) lhs))) && op.getChild(1).tableNames.contains(tree.getTable(tree.getColumnName((Column) rhs)))) return true;
				if(op.getChild(1).tableNames.contains(tree.getTable(tree.getColumnName((Column) lhs))) && op.getChild(0).tableNames.contains(tree.getTable(tree.getColumnName((Column) rhs)))) return true;
			}
		}
		return false;
		
	}
}
