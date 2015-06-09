package com.idioticdev.javagrande;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.LinkedList;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * Walks the AST and modifies it, replacing the JavaGrande syntax with it's counter part.
 * <p>
 * Use as follows. Call visit(CompilationUnit cu, null) to start the information gathering pass.
 * Call generate for the first code generation pass.
 * Finally resolveError should be called from the compile's {@link DiagnosticListener} for the second code generation pass.
 */
public class CodeVisitor<T> extends VoidVisitorAdapter<T>
{
	private List<PropertyDeclaration> props = new LinkedList<> ();
	private List<FieldAccessExpr> fields = new LinkedList<> ();
	private CompilationUnit cu;

	@Override
	public void visit (final CompilationUnit n, final T arg)
	{
		super.visit (n, arg);
		cu = n;
	}


	/**
	 * Collects {@link PropertryDeclaration}s to convert later.
	 * 
	 * @param prop Property to be collected
	 */
	public void visit (final PropertyDeclaration prop, final T arg)
	{
		props.add (prop);
	}

	/**
	 * Collects {@link FieldAccessExpr}s to convert (if needed) later.
	 *
	 * @param field Field to be collected
	 */
	@Override
	public void visit (final FieldAccessExpr field, final T arg)
	{
		fields.add (field);
	}

	public boolean hasProperties ()
	{
		return props.size () > 0;
	}


	/**
	 * Preforms first pass of code generation and transforming.
	 * Currently generates the field, getter, and setter for each property.
	 */
	public void generate ()
	{
		if (hasProperties ())
		{
			List<ImportDeclaration> imports = cu.getImports ();
			if (imports == null) imports = new LinkedList<ImportDeclaration> ();
			imports.add (new ImportDeclaration(new NameExpr (PropertyObserverSource.getPackage ()), false, false));
			cu.setImports (imports);
		}

		for (PropertyDeclaration prop : props)
		{
			TypeDeclaration parent = (TypeDeclaration) prop.getParentNode ();
			List<BodyDeclaration> members = parent.getMembers ();
			int last = members.get (members.size ()-1).getEndLine ()+1;
			int col = members.get (members.size ()-1).getBeginColumn ();

			// Field to back the property. Name of the property prefixed with _
			List<VariableDeclarator> variables = new LinkedList<> ();
			Expression defaultVal = prop.getDefault ();
			if (defaultVal != null)
				defaultVal.setBeginColumn (0);
			variables.add (new VariableDeclarator (new VariableDeclaratorId ("_"+prop.getName ()), defaultVal));
			FieldDeclaration field = new FieldDeclaration (Modifier.PRIVATE, prop.getType (), variables);
			field.setBeginLine (++last);
			field.setBeginColumn (col);

			// Observer field
			variables = new LinkedList<> ();
			List<Type> typeArgs = new LinkedList ();
			switch (prop.getType ().toString ())
			{
				case "boolean":
					typeArgs.add (new ClassOrInterfaceType ("Boolean"));
					break;
				case "char":
					typeArgs.add (new ClassOrInterfaceType ("Character"));
					break;
				case "byte":
					typeArgs.add (new ClassOrInterfaceType ("Byte"));
					break;
				case "short":
					typeArgs.add (new ClassOrInterfaceType ("Short"));
					break;
				case "int":
					typeArgs.add (new ClassOrInterfaceType ("Integer"));
					break;
				case "long":
					typeArgs.add (new ClassOrInterfaceType ("Long"));
					break;
				case "float":
					typeArgs.add (new ClassOrInterfaceType ("Float"));
					break;
				case "double":
					typeArgs.add (new ClassOrInterfaceType ("Double"));
					break;
				default:
					typeArgs.add (prop.getType ());
					break;
			}
			variables.add (new VariableDeclarator (new VariableDeclaratorId (prop.getName ()+"Observer")));
			ClassOrInterfaceType type = new ClassOrInterfaceType ("PropertyObserver");
			type.setTypeArgs (typeArgs);
			FieldDeclaration observer = new FieldDeclaration (Modifier.PUBLIC, type, variables);
			observer.setBeginLine (++last);
			observer.setBeginColumn (col);

			last++;

			MethodDeclaration set = prop.getSet ();
			set.setBeginLine (++last);
			if (set.getModifiers () == 0)
				set.setModifiers (Modifier.PUBLIC);

			// Default setter
			if (set.getBody () == null)
			{
				List<Statement> stmts = new LinkedList<> ();

				// Nodify observer
				BinaryExpr condition = new BinaryExpr (new NameExpr (prop.getName ()+"Observer"), new NullLiteralExpr (), BinaryExpr.Operator.notEquals);
				List<Expression> args = new LinkedList<> ();
				args.add (new NameExpr ("_"+prop.getName ()));
				args.add (new NameExpr ("value"));
				ExpressionStmt thenStmt = new ExpressionStmt (new MethodCallExpr(new NameExpr (prop.getName ()+"Observer"), "changed", args));
				Statement stmt = new IfStmt (condition, thenStmt, null);
				stmt.setBeginColumn (col+4);
				stmts.add (stmt);

				// Set backing field
				FieldAccessExpr target = new FieldAccessExpr(new ThisExpr (null), "_"+prop.getName ());
				stmt = new ExpressionStmt(new AssignExpr(target, new NameExpr ("value"), AssignExpr.Operator.assign));
				stmt.setBeginColumn (col+4);
				stmts.add (stmt);
				set.setBody (new BlockStmt (stmts));
			}

			BlockStmt body = set.getBody ();
			for (Statement stmt : body.getStmts ())
				stmt.setBeginLine (++last);

			body.setEndLine (++last);
			body.setBeginColumn (col);
			set.setEndLine (last++);

			MethodDeclaration get = prop.getGet ();
			get.setBeginLine (++last);
			if (get.getModifiers () == 0)
				get.setModifiers (Modifier.PUBLIC);

			// Default getter
			if (get.getBody () == null)
			{
				List<Statement> stmts = new LinkedList<> ();
				Statement stmt = new ReturnStmt (new FieldAccessExpr(new ThisExpr (null), "_"+prop.getName ()));
				stmt.setBeginColumn (col+4);
				stmts.add (stmt);
				get.setBody (new BlockStmt (stmts));
			}

			body = get.getBody ();
			for (Statement stmt : body.getStmts ())
				stmt.setBeginLine (++last);

			body.setEndLine (++last);
			body.setBeginColumn (col);
			get.setEndLine (last);

			parent.setEndLine (++last);

			// Add field, getter, and setter to class body
			members.add (field);
			members.add (observer);
			members.add (set);
			members.add (get);

			// Remove prepossessed property from the AST
			members.remove (prop);
		}
	}

	/**
	 * Preforms second pass of code generation and transforming.
	 * When an error occurs during compilation this should be called to transform the code if necessary.
	 * Currently replaces property access or assignment with the appropriate getter or setter.
	 * Note: Uses reflection. Could there be a better way?
	 *
	 * @return If the error was able to be resolved or not
	 * @param line Line the error occurred
	 * @param col Column the error occurred
	 */
	public boolean resolveError (long line, long col)
	{
		// Find field where the error occurred by matching line and col
		for (FieldAccessExpr field : fields)
		{
			Expression scope = field.getScope ();
			if (scope.getEndLine () != line || scope.getEndColumn ()+1 != col)
				continue;

			// if field is the right side of an assignment it's a setter
			if (field.getParentNode () instanceof AssignExpr)
			{
				AssignExpr ae = (AssignExpr) field.getParentNode ();
				if (ae.getTarget () == field)
				{
					// insert a method call to the setter instead of the field access
					Node parent = ae.getParentNode();
					String name = field.getField ();
					name = "set"+Character.toUpperCase (name.charAt (0)) + name.substring (1);
					List<Expression> args = new LinkedList<>();
					args.add (ae.getValue ());
					MethodCallExpr mc = new MethodCallExpr(field.getBeginLine (), field.getBeginColumn (), field.getEndLine (), field.getEndColumn (),
					scope, field.getTypeArgs (), name, args);

					try
					{
						replaceValue (parent, ae, mc);
						mc.setParentNode (parent);
					}
					catch (IllegalAccessException e)
					{
						System.out.println (e);
					}

					return true;
				}
			}

			// Else it's a getter

			// Insert method call to the getter instead of the field access
			String name = field.getField ();
			name = "get"+Character.toUpperCase (name.charAt (0)) + name.substring (1);
			MethodCallExpr mc = new MethodCallExpr(field.getBeginLine (), field.getBeginColumn (), field.getEndLine (), field.getEndColumn (),
			scope, field.getTypeArgs (), name, null);

			try
			{
				Node parent = field.getParentNode ();
				replaceValue (parent, field, mc);
				mc.setParentNode (parent);
			}
			catch (IllegalAccessException e)
			{
				System.out.println (e);
			}

			return true;
		}

		return false;
	}

	/**
	 * Sets a field in parent contained oldVal to newVal.
	 *
	 * @param parent {@link Node} containing the field
	 * @param oldVal Value the field is currently set to
	 * @param newVal Value to set the field to
	 * @throws IllegalAccessException Should never be thrown
	 */
	public static void replaceValue (Node parent, Node oldVal, Node newVal) throws IllegalAccessException
	{
		Class classVar = parent.getClass ();

		// Loop through super types so we can access all private fields
		do
		{
			// Find field that matches oldVal and set it to newVal
			Field[] fields = classVar.getDeclaredFields ();
			for (Field field : fields)
			{
				field.setAccessible(true);

				// Could be contained in a list
				if (field.getType ().getTypeName ().equals ("java.util.List"))
				{
					List list = (List) field.get (parent);
					if (list != null && list.contains (oldVal))
					{
						list.remove (oldVal);
						list.add (newVal);
						return;
					}
				}
				else if (field.get (parent) == oldVal)
				{
					field.set (parent, newVal);
					return;
				}

				field.setAccessible(false);
			}
		} while ((classVar = classVar.getSuperclass ()) != null);
	}
}
