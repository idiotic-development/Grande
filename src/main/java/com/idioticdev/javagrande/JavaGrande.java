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
import javax.tools.JavaCompiler.CompilationTask;

import com.github.javaparser.SourcesHelper;
import com.github.javaparser.ast.CompilationUnit;

public class JavaGrande
{
	public static void main (String[] argv)
	{
		try
		{
			List<JavaFileObject> compilationUnits = new LinkedList<> ();
			List<String> options = new LinkedList<> ();

			for (String file : argv)
			{
				if (!file.endsWith (".java"))
				{
					options.add (file);
					continue;
				}

				// creates an input stream for the file to be parsed
				FileInputStream in = new FileInputStream(file);

				CompilationUnit cu;
				try {
					// parse the file
					cu = parse (in);
				} finally {
					in.close();
				}
				// prints the resulting compilation unit to default system output
				CodeVisitor visitor = new CodeVisitor();
				visitor.visit(cu, null);
				visitor.transform ();

				compilationUnits.add ((JavaFileObject) new JavaSourceFromString(file.substring (0, file.lastIndexOf (".")), cu.toString ()));
			}

			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			CompilationTask task = compiler.getTask(null, null, null, options, null, compilationUnits);
			task.call();
		}
		catch (IOException|ParseException e)
		{
			System.out.println (e);
		}
	}

	public static CompilationUnit parse (final InputStream in) throws ParseException
	{
		java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
		String code = s.hasNext() ? s.next() : "";
		InputStream in1 = new ByteArrayInputStream(code.getBytes ());
		CompilationUnit cu = new ASTParser(in1, null).CompilationUnit();
		return cu;
	}
}