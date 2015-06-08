package com.idioticdev.javagrande;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.DiagnosticListener;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler.CompilationTask;

import com.github.javaparser.SourcesHelper;
import com.github.javaparser.ast.CompilationUnit;

public class JavaGrande
{
	public static void main (String[] argv)
	{

		List<JavaFileObject> compilationUnits = new LinkedList<> ();
		List<String> options = new LinkedList<> ();

		for (String file : argv)
		{
			// Collect options
			if (!file.endsWith (".java"))
			{
				options.add (file);
				continue;
			}

			try
			{
				FileInputStream in = new FileInputStream(file);
				CompilationUnit cu;
				try
				{
					// Build AST
					cu = parse (in);

					CodeVisitor visitor = new CodeVisitor();
					visitor.visit(cu, null); // Collect information
					visitor.generate (); // First pass

					compilationUnits.add (new JavaSource(file.substring (0, file.lastIndexOf (".")), cu, visitor));
				} catch (ParseException e)
				{
					System.out.println (e);
				} finally
				{
					in.close();
				}
			}
			catch (IOException e)
			{
				System.out.println (e);
			}
		}

		if (compilationUnits.size () < 1)
			return;

		// Try to compile. Errors needed for second pass
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		CompilationTask task = compiler.getTask(null, null, (e) ->
		{
			((JavaSource) e.getSource ()).getVisitor ().resolveError (e.getLineNumber (), e.getColumnNumber());
		}, options, null, compilationUnits);
		task.call();

		// Compile resulting sources
		task = compiler.getTask(null, null, null, options, null, compilationUnits);
		task.call();
	}

	/**
	 * Parse java code using modified ASTParser to support JavaGrande syntax.
	 *
	 * @param in Data to parse
	 * @return Base node of the AST
	 * @throws ParseException Unable to parse
	 */
	public static CompilationUnit parse (final InputStream in) throws ParseException
	{
		java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
		if (!s.hasNext ())
			return null;
		InputStream in1 = new ByteArrayInputStream(s.next ().getBytes ());
		CompilationUnit cu = new ASTParser(in1, null).CompilationUnit();
		return cu;
	}
}