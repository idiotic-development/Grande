package com.idioticdev.javagrande;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.github.javaparser.SourcesHelper;
import com.github.javaparser.ast.CompilationUnit;

public class JavaGrande
{
	public static void main (String[] argv)
	{
		try
		{
			// creates an input stream for the file to be parsed
			FileInputStream in = new FileInputStream("Test.java");

			CompilationUnit cu;
			try {
				// parse the file
				cu = parse (in);
			} finally {
				in.close();
			}
			// prints the resulting compilation unit to default system output
			// System.out.println(cu.toString());
			new CodeVisitor().visit(cu, null);
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