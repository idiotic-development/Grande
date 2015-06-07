package com.idioticdev.javagrande;

import com.github.javaparser.ast.DocumentableNode;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.ArrayList;
import java.util.List;

public final class PropertyDeclaration extends BodyDeclaration implements DocumentableNode
{
	private Type type;
	private String name;
	private MethodDeclaration set;
	private MethodDeclaration get;

	public PropertyDeclaration() {}

	public PropertyDeclaration(Type type, String name, MethodDeclaration get, MethodDeclaration set)
	{
		setType(type);
		setName(name);
		setSet(set);
		setGet(get);
	}

	public PropertyDeclaration(int beginLine, int beginColumn, int endLine, int endColumn, Type type, String name,  MethodDeclaration get, MethodDeclaration set)
	{
		super(beginLine, beginColumn, endLine, endColumn, null);
		setType(type);
		setName(name);
		setSet(set);
		setGet(get);
	}

	@Override
	public <R, A> R accept(GenericVisitor<R, A> v, A arg) { return null; }

	@Override
	public <A> void accept(VoidVisitor<A> v, A arg)
	{
		((CodeVisitor<A>) v).visit (this, arg);
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
		setAsParentNodeOf(this.type);
	}

	public MethodDeclaration getSet()
	{
		return set;
	}

	public void setSet(MethodDeclaration set)
	{
		this.set = set;
		setAsParentNodeOf(this.set);
	}

	public MethodDeclaration getGet()
	{
		return get;
	}

	public void setGet(MethodDeclaration get)
	{
		this.get = get;
		setAsParentNodeOf(this.get);
	}

	public String getName ()
	{
		return name;
	}

	public void setName (String name)
	{
		this.name = name;
	}

	@Override
	public void setJavaDoc(JavadocComment javadocComment) {
		this.javadocComment = javadocComment;
	}

	@Override
	public JavadocComment getJavaDoc() {
		return javadocComment;
	}

	private JavadocComment javadocComment;
}
