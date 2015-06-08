/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JavaParser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.idioticdev.javagrande;

import static com.github.javaparser.PositionUtils.sortByBeginPosition;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.github.javaparser.ast.internal.Utils.isNullOrEmpty;

/**
 * Dumps the AST to formatted Java source code.
 * Modified to keep original line and column placement.
 */
public final class DumpVisitor implements VoidVisitor<Object> {

    private boolean printComments;

    public DumpVisitor() {
        this(true);
    }

    public DumpVisitor(boolean printComments) {
        this.printComments = printComments;
    }

	private static class SourcePrinter {

		private int level = 0;

		private final StringBuilder buf = new StringBuilder();

		private int line = 1;
		private int column = 1;
		public void print(String s, int start, int col)
		{
			while (line < start)
			{
				buf.append ("\n");
				line++;
				column = 1;
			}

			while (column < col)
			{
				buf.append (" ");
				column++;
			}

			buf.append (s);
			column += s.length ();
		}

		public String getSource() {
			return buf.toString();
		}

		@Override public String toString() {
			return getSource();
		}
	}

	private final SourcePrinter printer = new SourcePrinter();

	public String getSource() {
		return printer.getSource();
	}

	private void printModifiers(final int modifiers, int line, int col) {
		if (ModifierSet.isPrivate(modifiers)) {
			printer.print("private ", line, col);
		}
		if (ModifierSet.isProtected(modifiers)) {
			printer.print("protected ", line, col);
		}
		if (ModifierSet.isPublic(modifiers)) {
			printer.print("public ", line, col);
		}
		if (ModifierSet.isAbstract(modifiers)) {
			printer.print("abstract ", line, col);
		}
		if (ModifierSet.isStatic(modifiers)) {
			printer.print("static ", line, col);
		}
		if (ModifierSet.isFinal(modifiers)) {
			printer.print("final ", line, col);
		}
		if (ModifierSet.isNative(modifiers)) {
			printer.print("native ", line, col);
		}
		if (ModifierSet.isStrictfp(modifiers)) {
			printer.print("strictfp ", line, col);
		}
		if (ModifierSet.isSynchronized(modifiers)) {
			printer.print("synchronized ", line, col);
		}
		if (ModifierSet.isTransient(modifiers)) {
			printer.print("transient ", line, col);
		}
		if (ModifierSet.isVolatile(modifiers)) {
			printer.print("volatile ", line, col);
		}
	}

	private void printMembers(final List<BodyDeclaration> members, final Object arg) {
		for (final BodyDeclaration member : members) {
			member.accept(this, arg);
		}
	}

	private void printMemberAnnotations(final List<AnnotationExpr> annotations, final Object arg) {
		if (!isNullOrEmpty(annotations)) {
			for (final AnnotationExpr a : annotations) {
				a.accept(this, arg);
			}
		}
	}

	private void printAnnotations(final List<AnnotationExpr> annotations, final Object arg) {
		if (!isNullOrEmpty(annotations)) {
			for (final AnnotationExpr a : annotations) {
				a.accept(this, arg);
				printer.print(" ", a.getBeginLine (), a.getBeginColumn ());
			}
		}
	}

	private void printTypeArgs(final List<Type> args, final Object arg) {
        if (!isNullOrEmpty(args)) {
			printer.print("<", 0, 0);
			for (final Iterator<Type> i = args.iterator(); i.hasNext();) {
				final Type t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", t.getBeginLine (), t.getBeginColumn ());
				}
			}
			printer.print(">", 0, 0);
		}
	}

	private void printTypeParameters(final List<TypeParameter> args, final Object arg) {
        if (!isNullOrEmpty(args)) {
			printer.print("<", 0, 0);
			for (final Iterator<TypeParameter> i = args.iterator(); i.hasNext();) {
				final TypeParameter t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", t.getBeginLine (), t.getBeginColumn ());
				}
			}
			printer.print(">", 0, 0);
		}
	}

	private void printArguments(final List<Expression> args, final Object arg) {
		printer.print("(", 0, 0);
        if (!isNullOrEmpty(args)) {
			for (final Iterator<Expression> i = args.iterator(); i.hasNext();) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", e.getBeginLine (), e.getBeginColumn ());
				}
			}
		}
		printer.print(")", 0, 0);
	}

	private void printJavadoc(final JavadocComment javadoc, final Object arg) {
		if (javadoc != null) {
			javadoc.accept(this, arg);
		}
	}

	private void printJavaComment(final Comment javacomment, final Object arg) {
		if (javacomment != null) {
			javacomment.accept(this, arg);
		}
	}

	@Override public void visit(final CompilationUnit n, final Object arg) {
		printJavaComment(n.getComment(), arg);

		if (n.getPackage() != null) {
			n.getPackage().accept(this, arg);
		}

		if (n.getImports() != null) {
			for (final ImportDeclaration i : n.getImports()) {
				i.accept(this, arg);
			}
		}

		if (n.getTypes() != null) {
			for (final Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
				i.next().accept(this, arg);
				if (i.hasNext()) {
				}
			}
		}

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final PackageDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printAnnotations(n.getAnnotations(), arg);
		printer.print("package ", n.getBeginLine (), n.getBeginColumn ());
		n.getName().accept(this, arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final NameExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final QualifiedNameExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getQualifier().accept(this, arg);
		printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final ImportDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("import ", n.getBeginLine (), n.getBeginColumn ());
		if (n.isStatic()) {
			printer.print("static ", n.getBeginLine (), n.getBeginColumn ());
		}
		n.getName().accept(this, arg);
		if (n.isAsterisk()) {
			printer.print(".*", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final ClassOrInterfaceDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

		if (n.isInterface()) {
			printer.print("interface ", n.getBeginLine (), n.getBeginColumn ());
		} else {
			printer.print("class ", n.getBeginLine (), n.getBeginColumn ());
		}

		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

		printTypeParameters(n.getTypeParameters(), arg);

		if (!isNullOrEmpty(n.getExtends())) {
			printer.print(" extends ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}

		if (!isNullOrEmpty(n.getImplements())) {
			printer.print(" implements ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}

		printer.print(" {", n.getBeginLine (), n.getBeginColumn ());
		if (!isNullOrEmpty(n.getMembers())) {
			printMembers(n.getMembers(), arg);
		}

        printOrphanCommentsEnding(n);
		printer.print("}", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final EmptyTypeDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());

        printOrphanCommentsEnding(n);
	}

	@Override public void visit(final JavadocComment n, final Object arg) {
		printer.print("/**", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getContent(), n.getBeginLine (), n.getBeginColumn ());
		printer.print("*/", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ClassOrInterfaceType n, final Object arg) {
		printJavaComment(n.getComment(), arg);

		if (n.getAnnotations() != null) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				ae.accept(this, arg);
				printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			}
		}

		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		printTypeArgs(n.getTypeArgs(), arg);
	}

	@Override public void visit(final TypeParameter n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getAnnotations() != null) {
			for (AnnotationExpr ann : n.getAnnotations()) {
				ann.accept(this, arg);
				printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			}
		}
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		if (n.getTypeBound() != null) {
			printer.print(" extends ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(" & ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
	}

	@Override public void visit(final PrimitiveType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getAnnotations() != null) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				ae.accept(this, arg);
				printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			}
		}
		switch (n.getType()) {
		case Boolean:
			printer.print("boolean", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Byte:
			printer.print("byte", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Char:
			printer.print("char", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Double:
			printer.print("double", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Float:
			printer.print("float", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Int:
			printer.print("int", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Long:
			printer.print("long", n.getBeginLine (), n.getBeginColumn ());
			break;
		case Short:
			printer.print("short", n.getBeginLine (), n.getBeginColumn ());
			break;
		}
	}

	@Override public void visit(final ReferenceType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getAnnotations() != null) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				ae.accept(this, arg);
				printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			}
		}
		n.getType().accept(this, arg);
		List<List<AnnotationExpr>> arraysAnnotations = n.getArraysAnnotations();
		for (int i = 0; i < n.getArrayCount(); i++) {
			if (arraysAnnotations != null && i < arraysAnnotations.size()) {
				List<AnnotationExpr> annotations = arraysAnnotations.get(i);
				if (annotations != null) {
					for (AnnotationExpr ae : annotations) {
						printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
						ae.accept(this, arg);

					}
				}
			}
			printer.print("[]", n.getBeginLine (), n.getBeginColumn ());
		}
	}

	@Override public void visit(final WildcardType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getAnnotations() != null) {
			for (AnnotationExpr ae : n.getAnnotations()) {
				printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
				ae.accept(this, arg);
			}
		}
		printer.print("?", n.getBeginLine (), n.getBeginColumn ());
		if (n.getExtends() != null) {
			printer.print(" extends ", n.getBeginLine (), n.getBeginColumn ());
			n.getExtends().accept(this, arg);
		}
		if (n.getSuper() != null) {
			printer.print(" super ", n.getBeginLine (), n.getBeginColumn ());
			n.getSuper().accept(this, arg);
		}
	}

	@Override public void visit(final UnknownType n, final Object arg) {
		// Nothing to dump
	}

	@Override public void visit(final FieldDeclaration n, final Object arg) {
        printOrphanCommentsBeforeThisChildNode(n);

		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());
		n.getType().accept(this, arg);

		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		for (final Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext();) {
			final VariableDeclarator var = i.next();
			var.accept(this, arg);
			if (i.hasNext()) {
				printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
			}
		}

		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final VariableDeclarator n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getId().accept(this, arg);
		if (n.getInit() != null) {
			printer.print(" = ", n.getBeginLine (), n.getBeginColumn ());
			n.getInit().accept(this, arg);
		}
	}

	@Override public void visit(final VariableDeclaratorId n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		for (int i = 0; i < n.getArrayCount(); i++) {
			printer.print("[]", n.getBeginLine (), n.getBeginColumn ());
		}
	}

	@Override public void visit(final ArrayInitializerExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("{", n.getBeginLine (), n.getBeginColumn ());
		if (n.getValues() != null) {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
				final Expression expr = i.next();
				expr.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print("}", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final VoidType n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("void", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ArrayAccessExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getName().accept(this, arg);
		printer.print("[", n.getBeginLine (), n.getBeginColumn ());
		n.getIndex().accept(this, arg);
		printer.print("]", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ArrayCreationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("new ", n.getBeginLine (), n.getBeginColumn ());
		n.getType().accept(this, arg);
		List<List<AnnotationExpr>> arraysAnnotations = n.getArraysAnnotations();
		if (n.getDimensions() != null) {
			int j = 0;
			for (final Expression dim : n.getDimensions()) {

				if (arraysAnnotations != null && j < arraysAnnotations.size()) {
					List<AnnotationExpr> annotations = arraysAnnotations.get(j);
					if (annotations != null) {
						for (AnnotationExpr ae : annotations) {
							printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
							ae.accept(this, arg);
						}
					}
				}
				printer.print("[", n.getBeginLine (), n.getBeginColumn ());
				dim.accept(this, arg);
				printer.print("]", n.getBeginLine (), n.getBeginColumn ());
				j++;
			}
			for (int i = 0; i < n.getArrayCount(); i++) {
				if (arraysAnnotations != null && i < arraysAnnotations.size()) {

					List<AnnotationExpr> annotations = arraysAnnotations.get(i);
					if (annotations != null) {
						for (AnnotationExpr ae : annotations) {
							printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
							ae.accept(this, arg);

						}
					}
				}
				printer.print("[]", n.getBeginLine (), n.getBeginColumn ());
			}

		} else {
			for (int i = 0; i < n.getArrayCount(); i++) {
				if (arraysAnnotations != null && i < arraysAnnotations.size()) {
					List<AnnotationExpr> annotations = arraysAnnotations.get(i);
					if (annotations != null) {
						for (AnnotationExpr ae : annotations) {
							ae.accept(this, arg);
							printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
						}
					}
				}
				printer.print("[]", n.getBeginLine (), n.getBeginColumn ());
			}
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			n.getInitializer().accept(this, arg);
		}
	}

	@Override public void visit(final AssignExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getTarget().accept(this, arg);
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		switch (n.getOperator()) {
		case assign:
			printer.print("=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case and:
			printer.print("&=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case or:
			printer.print("|=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case xor:
			printer.print("^=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case plus:
			printer.print("+=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case minus:
			printer.print("-=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case rem:
			printer.print("%=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case slash:
			printer.print("/=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case star:
			printer.print("*=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case lShift:
			printer.print("<<=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case rSignedShift:
			printer.print(">>=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case rUnsignedShift:
			printer.print(">>>=", n.getBeginLine (), n.getBeginColumn ());
			break;
		}
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		n.getValue().accept(this, arg);
	}

	@Override public void visit(final BinaryExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getLeft().accept(this, arg);
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		switch (n.getOperator()) {
		case or:
			printer.print("||", n.getBeginLine (), n.getBeginColumn ());
			break;
		case and:
			printer.print("&&", n.getBeginLine (), n.getBeginColumn ());
			break;
		case binOr:
			printer.print("|", n.getBeginLine (), n.getBeginColumn ());
			break;
		case binAnd:
			printer.print("&", n.getBeginLine (), n.getBeginColumn ());
			break;
		case xor:
			printer.print("^", n.getBeginLine (), n.getBeginColumn ());
			break;
		case equals:
			printer.print("==", n.getBeginLine (), n.getBeginColumn ());
			break;
		case notEquals:
			printer.print("!=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case less:
			printer.print("<", n.getBeginLine (), n.getBeginColumn ());
			break;
		case greater:
			printer.print(">", n.getBeginLine (), n.getBeginColumn ());
			break;
		case lessEquals:
			printer.print("<=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case greaterEquals:
			printer.print(">=", n.getBeginLine (), n.getBeginColumn ());
			break;
		case lShift:
			printer.print("<<", n.getBeginLine (), n.getBeginColumn ());
			break;
		case rSignedShift:
			printer.print(">>", n.getBeginLine (), n.getBeginColumn ());
			break;
		case rUnsignedShift:
			printer.print(">>>", n.getBeginLine (), n.getBeginColumn ());
			break;
		case plus:
			printer.print("+", n.getBeginLine (), n.getBeginColumn ());
			break;
		case minus:
			printer.print("-", n.getBeginLine (), n.getBeginColumn ());
			break;
		case times:
			printer.print("*", n.getBeginLine (), n.getBeginColumn ());
			break;
		case divide:
			printer.print("/", n.getBeginLine (), n.getBeginColumn ());
			break;
		case remainder:
			printer.print("%", n.getBeginLine (), n.getBeginColumn ());
			break;
		}
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		n.getRight().accept(this, arg);
	}

	@Override public void visit(final CastExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		n.getType().accept(this, arg);
		printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		n.getExpr().accept(this, arg);
	}

	@Override public void visit(final ClassExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getType().accept(this, arg);
		printer.print(".class", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ConditionalExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getCondition().accept(this, arg);
		printer.print(" ? ", n.getBeginLine (), n.getBeginColumn ());
		n.getThenExpr().accept(this, arg);
		printer.print(" : ", n.getBeginLine (), n.getBeginColumn ());
		n.getElseExpr().accept(this, arg);
	}

	@Override public void visit(final EnclosedExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		if (n.getInner() != null) {
		n.getInner().accept(this, arg);
		}
		printer.print(")", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final FieldAccessExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getScope().accept(this, arg);
		printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getField(), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final InstanceOfExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getExpr().accept(this, arg);
		printer.print(" instanceof ", n.getBeginLine (), n.getBeginColumn ());
		n.getType().accept(this, arg);
	}

	@Override public void visit(final CharLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("'", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
		printer.print("'", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final DoubleLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final IntegerLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final LongLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final IntegerLiteralMinValueExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final LongLiteralMinValueExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final StringLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("\"", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getValue(), n.getBeginLine (), n.getBeginColumn ());
		printer.print("\"", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final BooleanLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(String.valueOf(n.getValue()), n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final NullLiteralExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("null", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ThisExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getClassExpr() != null) {
			n.getClassExpr().accept(this, arg);
			printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print("this", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final SuperExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getClassExpr() != null) {
			n.getClassExpr().accept(this, arg);
			printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print("super", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final MethodCallExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		}
		printTypeArgs(n.getTypeArgs(), arg);
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		printArguments(n.getArgs(), arg);
	}

	@Override public void visit(final ObjectCreationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			printer.print(".", n.getBeginLine (), n.getBeginColumn ());
		}

		printer.print("new ", n.getBeginLine (), n.getBeginColumn ());

		printTypeArgs(n.getTypeArgs(), arg);
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());

		n.getType().accept(this, arg);

		printArguments(n.getArgs(), arg);

		if (n.getAnonymousClassBody() != null) {
			printer.print(" {", n.getBeginLine (), n.getBeginColumn ());
			printMembers(n.getAnonymousClassBody(), arg);
			printer.print("}", n.getBeginLine (), n.getBeginColumn ());
		}
	}

	@Override public void visit(final UnaryExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		switch (n.getOperator()) {
		case positive:
			printer.print("+", n.getBeginLine (), n.getBeginColumn ());
			break;
		case negative:
			printer.print("-", n.getBeginLine (), n.getBeginColumn ());
			break;
		case inverse:
			printer.print("~", n.getBeginLine (), n.getBeginColumn ());
			break;
		case not:
			printer.print("!", n.getBeginLine (), n.getBeginColumn ());
			break;
		case preIncrement:
			printer.print("++", n.getBeginLine (), n.getBeginColumn ());
			break;
		case preDecrement:
			printer.print("--", n.getBeginLine (), n.getBeginColumn ());
			break;
		default:
		}

		n.getExpr().accept(this, arg);

		switch (n.getOperator()) {
		case posIncrement:
			printer.print("++", n.getBeginLine (), n.getBeginColumn ());
			break;
		case posDecrement:
			printer.print("--", n.getBeginLine (), n.getBeginColumn ());
			break;
		default:
		}
	}

	@Override public void visit(final ConstructorDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

		printTypeParameters(n.getTypeParameters(), arg);
		if (n.getTypeParameters() != null) {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

		printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		if (n.getParameters() != null) {
			for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
				final Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		printer.print(")", n.getBeginLine (), n.getBeginColumn ());

		if (!isNullOrEmpty(n.getThrows())) {
			printer.print(" throws ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
				final NameExpr name = i.next();
				name.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		n.getBlock().accept(this, arg);
	}

	@Override public void visit(final MethodDeclaration n, final Object arg) {
        printOrphanCommentsBeforeThisChildNode(n);

		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());
		if (n.isDefault()) {
			printer.print("default ", n.getBeginLine (), n.getBeginColumn ());
		}
		printTypeParameters(n.getTypeParameters(), arg);
		if (n.getTypeParameters() != null) {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		}

		n.getType().accept(this, arg);
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

		printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		if (n.getParameters() != null) {
			for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
				final Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		printer.print(")", n.getBeginLine (), n.getBeginColumn ());

		for (int i = 0; i < n.getArrayCount(); i++) {
			printer.print("[]", n.getBeginLine (), n.getBeginColumn ());
		}

		if (!isNullOrEmpty(n.getThrows())) {
			printer.print(" throws ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
				final NameExpr name = i.next();
				name.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		if (n.getBody() == null) {
			printer.print(";", n.getBeginLine (), n.getBeginColumn ());
		} else {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			n.getBody().accept(this, arg);
		}
	}

	@Override public void visit(final Parameter n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());
		if (n.getType() != null) {
			n.getType().accept(this, arg);
		}
		if (n.isVarArgs()) {
			printer.print("...", n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		n.getId().accept(this, arg);
	}
	
    @Override public void visit(MultiTypeParameter n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

        Iterator<Type> types = n.getTypes().iterator();
        types.next().accept(this, arg);
        while (types.hasNext()) {
        	printer.print(" | ", n.getBeginLine (), n.getBeginColumn ());
        	types.next().accept(this, arg);
        }
        
        printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
        n.getId().accept(this, arg);
    }

	@Override public void visit(final ExplicitConstructorInvocationStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.isThis()) {
			printTypeArgs(n.getTypeArgs(), arg);
			printer.print("this", n.getBeginLine (), n.getBeginColumn ());
		} else {
			if (n.getExpr() != null) {
				n.getExpr().accept(this, arg);
				printer.print(".", n.getBeginLine (), n.getBeginColumn ());
			}
			printTypeArgs(n.getTypeArgs(), arg);
			printer.print("super", n.getBeginLine (), n.getBeginColumn ());
		}
		printArguments(n.getArgs(), arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final VariableDeclarationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

		n.getType().accept(this, arg);
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());

		for (final Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
			final VariableDeclarator v = i.next();
			v.accept(this, arg);
			if (i.hasNext()) {
				printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
			}
		}
	}

	@Override public void visit(final TypeDeclarationStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		n.getTypeDeclaration().accept(this, arg);
	}

	@Override public void visit(final AssertStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("assert ", n.getBeginLine (), n.getBeginColumn ());
		n.getCheck().accept(this, arg);
		if (n.getMessage() != null) {
			printer.print(" : ", n.getBeginLine (), n.getBeginColumn ());
			n.getMessage().accept(this, arg);
		}
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final BlockStmt n, final Object arg) {
        printOrphanCommentsBeforeThisChildNode(n);
		printJavaComment(n.getComment(), arg);
		printer.print("{", n.getBeginLine (), n.getBeginColumn ());
		if (n.getStmts() != null) {
			for (final Statement s : n.getStmts()) {
				s.accept(this, arg);
			}
		}
		printer.print("}", n.getBeginLine (), n.getBeginColumn ());

	}

	@Override public void visit(final LabeledStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getLabel(), n.getBeginLine (), n.getBeginColumn ());
		printer.print(": ", n.getBeginLine (), n.getBeginColumn ());
		n.getStmt().accept(this, arg);
	}

	@Override public void visit(final EmptyStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ExpressionStmt n, final Object arg) {
		printOrphanCommentsBeforeThisChildNode(n);
		printJavaComment(n.getComment(), arg);
		n.getExpression().accept(this, arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final SwitchStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("switch(", n.getBeginLine (), n.getBeginColumn ());
		n.getSelector().accept(this, arg);
		printer.print(") {", n.getBeginLine (), n.getBeginColumn ());
		if (n.getEntries() != null) {
			for (final SwitchEntryStmt e : n.getEntries()) {
				e.accept(this, arg);
			}
		}
		printer.print("}", n.getBeginLine (), n.getBeginColumn ());

	}

	@Override public void visit(final SwitchEntryStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		if (n.getLabel() != null) {
			printer.print("case ", n.getBeginLine (), n.getBeginColumn ());
			n.getLabel().accept(this, arg);
			printer.print(":", n.getBeginLine (), n.getBeginColumn ());
		} else {
			printer.print("default:", n.getBeginLine (), n.getBeginColumn ());
		}
		if (n.getStmts() != null) {
			for (final Statement s : n.getStmts()) {
				s.accept(this, arg);
			}
		}
	}

	@Override public void visit(final BreakStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("break", n.getBeginLine (), n.getBeginColumn ());
		if (n.getId() != null) {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			printer.print(n.getId(), n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ReturnStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("return", n.getBeginLine (), n.getBeginColumn ());
		if (n.getExpr() != null) {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			n.getExpr().accept(this, arg);
		}
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final EnumDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

		printer.print("enum ", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

		if (n.getImplements() != null) {
			printer.print(" implements ", n.getBeginLine (), n.getBeginColumn ());
			for (final Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}

		printer.print(" {", n.getBeginLine (), n.getBeginColumn ());
		if (n.getEntries() != null) {
			for (final Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
				final EnumConstantDeclaration e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		if (n.getMembers() != null) {
			printer.print(";", n.getBeginLine (), n.getBeginColumn ());
			printMembers(n.getMembers(), arg);
		} else {
			if (n.getEntries() != null) {
			}
		}
		printer.print("}", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final EnumConstantDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());

		if (n.getArgs() != null) {
			printArguments(n.getArgs(), arg);
		}

		if (n.getClassBody() != null) {
			printer.print(" {", n.getBeginLine (), n.getBeginColumn ());
			printMembers(n.getClassBody(), arg);
			printer.print("}", n.getBeginLine (), n.getBeginColumn ());
		}
	}

	@Override public void visit(final EmptyMemberDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final InitializerDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		if (n.isStatic()) {
			printer.print("static ", n.getBeginLine (), n.getBeginColumn ());
		}
		n.getBlock().accept(this, arg);
	}

	@Override public void visit(final IfStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("if (", n.getBeginLine (), n.getBeginColumn ());
		n.getCondition().accept(this, arg);
		final boolean thenBlock = n.getThenStmt() instanceof BlockStmt;
		if (thenBlock) // block statement should start on the same line
			printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		else {
			printer.print(")", n.getBeginLine (), n.getBeginColumn ());
		}
		n.getThenStmt().accept(this, arg);
		if (!thenBlock)
		if (n.getElseStmt() != null) {
			if (thenBlock)
				printer.print(" ", n.getBeginLine (), n.getBeginColumn ());

			final boolean elseIf = n.getElseStmt() instanceof IfStmt;
			final boolean elseBlock = n.getElseStmt() instanceof BlockStmt;
			if (elseIf || elseBlock) // put chained if and start of block statement on a same level
				printer.print("else ", n.getBeginLine (), n.getBeginColumn ());
			else {
				printer.print("else", n.getBeginLine (), n.getBeginColumn ());
			}
			n.getElseStmt().accept(this, arg);
		}
	}

	@Override public void visit(final WhileStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("while (", n.getBeginLine (), n.getBeginColumn ());
		n.getCondition().accept(this, arg);
		printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		n.getBody().accept(this, arg);
	}

	@Override public void visit(final ContinueStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("continue", n.getBeginLine (), n.getBeginColumn ());
		if (n.getId() != null) {
			printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
			printer.print(n.getId(), n.getBeginLine (), n.getBeginColumn ());
		}
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final DoStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("do ", n.getBeginLine (), n.getBeginColumn ());
		n.getBody().accept(this, arg);
		printer.print(" while (", n.getBeginLine (), n.getBeginColumn ());
		n.getCondition().accept(this, arg);
		printer.print(");", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final ForeachStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("for (", n.getBeginLine (), n.getBeginColumn ());
		n.getVariable().accept(this, arg);
		printer.print(" : ", n.getBeginLine (), n.getBeginColumn ());
		n.getIterable().accept(this, arg);
		printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		n.getBody().accept(this, arg);
	}

	@Override public void visit(final ForStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("for (", n.getBeginLine (), n.getBeginColumn ());
		if (n.getInit() != null) {
			for (final Iterator<Expression> i = n.getInit().iterator(); i.hasNext();) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		printer.print("; ", n.getBeginLine (), n.getBeginColumn ());
		if (n.getCompare() != null) {
			n.getCompare().accept(this, arg);
		}
		printer.print("; ", n.getBeginLine (), n.getBeginColumn ());
		if (n.getUpdate() != null) {
			for (final Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		n.getBody().accept(this, arg);
	}

	@Override public void visit(final ThrowStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("throw ", n.getBeginLine (), n.getBeginColumn ());
		n.getExpr().accept(this, arg);
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final SynchronizedStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("synchronized (", n.getBeginLine (), n.getBeginColumn ());
		n.getExpr().accept(this, arg);
		printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		n.getBlock().accept(this, arg);
	}

	@Override public void visit(final TryStmt n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("try ", n.getBeginLine (), n.getBeginColumn ());
		if (!n.getResources().isEmpty()) {
			printer.print("(", n.getBeginLine (), n.getBeginColumn ());
			Iterator<VariableDeclarationExpr> resources = n.getResources().iterator();
			boolean first = true;
			while (resources.hasNext()) {
				visit(resources.next(), arg);
				if (resources.hasNext()) {
					printer.print(";", n.getBeginLine (), n.getBeginColumn ());
					if (first) {
					}
				}
				first = false;
			}
			if (n.getResources().size() > 1) {
			}
			printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		}
		n.getTryBlock().accept(this, arg);
		if (n.getCatchs() != null) {
			for (final CatchClause c : n.getCatchs()) {
				c.accept(this, arg);
			}
		}
		if (n.getFinallyBlock() != null) {
			printer.print(" finally ", n.getBeginLine (), n.getBeginColumn ());
			n.getFinallyBlock().accept(this, arg);
		}
	}

	@Override public void visit(final CatchClause n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(" catch (", n.getBeginLine (), n.getBeginColumn ());
		n.getExcept().accept(this, arg);
		printer.print(") ", n.getBeginLine (), n.getBeginColumn ());
		n.getCatchBlock().accept(this, arg);

	}

	@Override public void visit(final AnnotationDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

		printer.print("@interface ", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		printer.print(" {", n.getBeginLine (), n.getBeginColumn ());
		if (n.getMembers() != null) {
			printMembers(n.getMembers(), arg);
		}
		printer.print("}", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final AnnotationMemberDeclaration n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printJavadoc(n.getJavaDoc(), arg);
		printMemberAnnotations(n.getAnnotations(), arg);
		printModifiers(n.getModifiers(), n.getBeginLine (), n.getBeginColumn ());

		n.getType().accept(this, arg);
		printer.print(" ", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		printer.print("()", n.getBeginLine (), n.getBeginColumn ());
		if (n.getDefaultValue() != null) {
			printer.print(" default ", n.getBeginLine (), n.getBeginColumn ());
			n.getDefaultValue().accept(this, arg);
		}
		printer.print(";", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final MarkerAnnotationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("@", n.getBeginLine (), n.getBeginColumn ());
		n.getName().accept(this, arg);
	}

	@Override public void visit(final SingleMemberAnnotationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("@", n.getBeginLine (), n.getBeginColumn ());
		n.getName().accept(this, arg);
		printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		n.getMemberValue().accept(this, arg);
		printer.print(")", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final NormalAnnotationExpr n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print("@", n.getBeginLine (), n.getBeginColumn ());
		n.getName().accept(this, arg);
		printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		if (n.getPairs() != null) {
			for (final Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext();) {
				final MemberValuePair m = i.next();
				m.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		printer.print(")", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final MemberValuePair n, final Object arg) {
		printJavaComment(n.getComment(), arg);
		printer.print(n.getName(), n.getBeginLine (), n.getBeginColumn ());
		printer.print(" = ", n.getBeginLine (), n.getBeginColumn ());
		n.getValue().accept(this, arg);
	}

	@Override public void visit(final LineComment n, final Object arg) {
		if (!this.printComments) {
            return;
        }
        printer.print("//", n.getBeginLine (), n.getBeginColumn ());
		String tmp = n.getContent();
		tmp = tmp.replace('\r', ' ');
		tmp = tmp.replace('\n', ' ');
		printer.print(tmp, n.getBeginLine (), n.getBeginColumn ());
	}

	@Override public void visit(final BlockComment n, final Object arg) {
        if (!this.printComments) {
            return;
        }
        printer.print("/*", n.getBeginLine (), n.getBeginColumn ());
		printer.print(n.getContent(), n.getBeginLine (), n.getBeginColumn ());
		printer.print("*/", n.getBeginLine (), n.getBeginColumn ());
	}

	@Override
	public void visit(LambdaExpr n, Object arg) {
        printJavaComment(n.getComment(), arg);

        List<Parameter> parameters = n.getParameters();
		boolean printPar = false;
		printPar = n.isParametersEnclosed();

		if (printPar) {
			printer.print("(", n.getBeginLine (), n.getBeginColumn ());
		}
		if (parameters != null) {
			for (Iterator<Parameter> i = parameters.iterator(); i.hasNext();) {
				Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
				}
			}
		}
		if (printPar) {
			printer.print(")", n.getBeginLine (), n.getBeginColumn ());
		}

		printer.print("->", n.getBeginLine (), n.getBeginColumn ());
		Statement body = n.getBody();
		String bodyStr = body.toString();
		if (body instanceof ExpressionStmt) {
			// removing ';'
			bodyStr = bodyStr.substring(0, bodyStr.length() - 1);
		}
		printer.print(bodyStr, n.getBeginLine (), n.getBeginColumn ());

	}


    @Override
    public void visit(MethodReferenceExpr n, Object arg) {
        printJavaComment(n.getComment(), arg);
        Expression scope = n.getScope();
        String identifier = n.getIdentifier();
        if (scope != null) {
            n.getScope().accept(this, arg);
        }

        printer.print("::", n.getBeginLine (), n.getBeginColumn ());
        if (n.getTypeParameters() != null) {
            printer.print("<", n.getBeginLine (), n.getBeginColumn ());
            for (Iterator<TypeParameter> i = n.getTypeParameters().iterator(); i
                    .hasNext();) {
                TypeParameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ", n.getBeginLine (), n.getBeginColumn ());
                }
            }
            printer.print(">", n.getBeginLine (), n.getBeginColumn ());
        }
        if (identifier != null) {
            printer.print(identifier, n.getBeginLine (), n.getBeginColumn ());
        }

    }

    @Override
    public void visit(TypeExpr n, Object arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getType() != null) {
            n.getType().accept(this, arg);
        }
    }

    private void printOrphanCommentsBeforeThisChildNode(final Node node){
        if (node instanceof Comment) return;

        Node parent = node.getParentNode();
        if (parent==null) return;
        List<Node> everything = new LinkedList<Node>();
        everything.addAll(parent.getChildrenNodes());
        sortByBeginPosition(everything);
        int positionOfTheChild = -1;
        for (int i=0;i<everything.size();i++){
            if (everything.get(i)==node) positionOfTheChild=i;
        }
        if (positionOfTheChild==-1) throw new RuntimeException("My index not found!!! "+node);
        int positionOfPreviousChild = -1;
        for (int i=positionOfTheChild-1;i>=0 && positionOfPreviousChild==-1;i--){
            if (!(everything.get(i) instanceof Comment)) positionOfPreviousChild = i;
        }
        for (int i=positionOfPreviousChild+1;i<positionOfTheChild;i++){
            Node nodeToPrint = everything.get(i);
            if (!(nodeToPrint instanceof Comment)) throw new RuntimeException("Expected comment, instead "+nodeToPrint.getClass()+". Position of previous child: "+positionOfPreviousChild+", position of child "+positionOfTheChild);
            nodeToPrint.accept(this,null);
        }
    }


    private void printOrphanCommentsEnding(final Node node){
        List<Node> everything = new LinkedList<Node>();
        everything.addAll(node.getChildrenNodes());
        sortByBeginPosition(everything);
        if (everything.size()==0) return;

        int commentsAtEnd = 0;
        boolean findingComments = true;
        while (findingComments&&commentsAtEnd<everything.size()){
            Node last = everything.get(everything.size()-1-commentsAtEnd);
            findingComments = (last instanceof Comment);
            if (findingComments) commentsAtEnd++;
        }
        for (int i=0;i<commentsAtEnd;i++){
            everything.get(everything.size()-commentsAtEnd+i).accept(this,null);
        }
    }
}
