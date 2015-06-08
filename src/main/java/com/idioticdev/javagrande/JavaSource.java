package com.idioticdev.javagrande;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.github.javaparser.ast.CompilationUnit;

/**
 * Wraps a {@link CompilationUnit} and {@link CodeVisitor} for compilation.
 * Uses {@link DumpVisitor} for conversion to string.
 */
class JavaSource extends SimpleJavaFileObject
{
	private CompilationUnit cu;
	private CodeVisitor visitor;

	/**
	 * @param name Name of class repesented
	 * @param cu AST to compile
	 * @param visitor Visitor used to build the AST
	 */
	public JavaSource(String name, CompilationUnit cu, CodeVisitor visitor)
	{
		super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
		this.visitor = visitor;
		this.cu = cu;
	}

	public CodeVisitor getVisitor ()
	{
		return visitor;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors)
	{
		DumpVisitor dump = new DumpVisitor ();
		dump.visit (cu, null);
		return dump.getSource ();
	}
}