package com.idioticdev.javagrande;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclaratorId;

public class CodeVisitor<T> extends VoidVisitorAdapter<T>
{
	public void visit (final PropertyDeclaration n, final T arg)
	{
		System.out.println ("Name: "+n.getName ());
	}
}