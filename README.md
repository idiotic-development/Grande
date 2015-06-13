# JavaGrande
JavaGrande extends the Java programming language to support observable properties. Not only does grande allow you to define new properties, but any api with standard getters and setters can be accessed as if they were a property.

## How it works
JavaGrande works by internally converting the property definitions and access into valid java source code then passing it to javac to compile normally.

## Usage

    java -jar JavaGrande [javac options] -o|--output [directory] FILES...

If the output directory is specified processed source files will be written there. Otherwise source files will be passed to *javac* internally.

## The syntax

### Basics

The basic syntax is as follows.

    String foo { get; set; default = "bar" }

This defines a property named *foo* with a default value of *bar*. Now you can access the property as if it was a public field.

    this.foo = "foobar";
    System.out.println (this.foo);

### Custom Blocks

A property has a *get* and a *set* block for getting and setting its value. A field, the property name prefixed with an underscore, is automatically provide to store the value. In the set block the variable *value* contains the new value that should be assigned to the property.

    String foo {
        get {
            System.out.println ("Getting foo's value");
            return _foo;
        }
        set {
            fooObserver.changed (_foo, value) // Need to notify the observer
            System.out.println ("Settings foo's value");
            _foo = value;
        }
    }

### Inferred Properties

Any class that defines standard getter and setter methods can be accessed with the property syntax.

    public class Inferred
    {
        private String foo;

        public void setFoo (String foo)
        {
            this.foo = foo;
        }

        public String getFoo ()
        {
            return foo;
        }

        public static main (String[] args)
        {
            Inferred in = new Inferred ();
            in.foo = bar;
            System.out.println (in.foo);
        }
    }

### PropertyObserver

> Note: Observers only work for defined properties, not inferred properties.

Properties can be watched for changes using a *PropertyObserver*. Simply set the observer field with an instance of *PropertyObserver<field type>* (or use a lambda).

    String foo { get; set; }

    public void bar ()
    {
        fooObserver = (oldVal, newVal) -> {
            System.out.println ("Foo changed from "+oldVal+" to "+newVal);
        }

        foo = "bar";
        foo = "foobar"
    }

The above code would output the following.

    Foo changed from to bar
    Foo changed from bar to foobar

Multiple observers for one property are not supported as the observer is stored in a single field. However the same effect can be achieved by chaining the methods together.

    fooObserver = (oldVal, newVal) -> {
        System.out.println ("Obserber one called");
    }
    
    fooObserver = new PropertyObserver () {
        PropertyObserver observer = fooObserver;

        public void changed (oldVal, newVal)
        {
            if (observer != null)
                observer.changed (oldVal, newVal);

            System.out.println ("Obserber two called");
        }
    }

    // Simpiler alternative
    final oldObserver = fooObserver;
    fooObserver = (oldVal, newVal) -> {
        if (observer != null)
            oldObserver.changed (oldVal, newVal);

        System.out.println ("Obserber three called");
    }



## Running

Clone the repository.

    git clone https://github.com/idiotic-development/JavaGrande.git

Package the Jar.

    buildr package

Run the jar

    java -jar target/JavaGrande-0.0.1.jar -d . *.java

Your class files should now be ready to run.
