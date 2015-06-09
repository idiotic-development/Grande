package com.idioticdev.javagrande;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.github.javaparser.ast.CompilationUnit;

class PropertyObserverSource extends SimpleJavaFileObject
{
	public PropertyObserverSource ()
	{
		super(URI.create("string:///PropertyObserver" + Kind.SOURCE.extension),Kind.SOURCE);
	}

	public static String getPackage ()
	{
		return "com.idioticdev.javagrande.PropertyObserver";
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors)
	{
		return "package com.idioticdev.javagrande;\n"
			+  "public interface PropertyObserver<T> {\n"
			+  "	public void changed (T oldVal, T newVal);\n}";
	}
}