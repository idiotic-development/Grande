package com.idioticdev.javagrande;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class CodeVisitor<T> extends VoidVisitorAdapter<T>
{
	private List<PropertyDeclaration> props = new LinkedList<> ();

	public void visit (final PropertyDeclaration prop, final T arg)
	{
		props.add (prop);
	}

	public void transform ()
	{
		for (PropertyDeclaration prop : props)
		{
			MethodDeclaration set = prop.getSet ();
			if (set.getModifiers () == 0)
				set.setModifiers (Modifier.PUBLIC);

			if (set.getBody () == null)
			{
				List<Statement> stmts = new LinkedList<> ();
				FieldAccessExpr target = new FieldAccessExpr(new ThisExpr (null), "_"+prop.getName ());
				stmts.add (new ExpressionStmt(new AssignExpr(target, new NameExpr ("value"), AssignExpr.Operator.assign)));
				set.setBody (new BlockStmt (stmts));
			}

			MethodDeclaration get = prop.getGet ();
			if (get.getModifiers () == 0)
				get.setModifiers (Modifier.PUBLIC);
			
			if (get.getBody () == null)
			{
				List<Statement> stmts = new LinkedList<> ();
				stmts.add (new ReturnStmt (new FieldAccessExpr(new ThisExpr (null), "_"+prop.getName ())));
				get.setBody (new BlockStmt (stmts));
			}

			List<VariableDeclarator> variables = new LinkedList<> ();
			variables.add (new VariableDeclarator (new VariableDeclaratorId ("_"+prop.getName ())));
			FieldDeclaration field = new FieldDeclaration (Modifier.PRIVATE, prop.getType (), variables);

			TypeDeclaration parent = (TypeDeclaration) prop.getParentNode ();
			List<BodyDeclaration> members = parent.getMembers ();
			members.add (field);
			members.add (set);
			members.add (get);

			parent.setMembers (members);
		}
	}
}