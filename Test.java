public class Test
{
	public String field = "set";

	String property
	{
		public get
		{
			System.out.println ("getting property");
			return _property;
		}
		private set
		{
			System.out.println ("setting property");
			_property = value;
		}
	}

	int defaultProp { get; set; def = 0 }

	public static void main (String[] argv)
	{
		Test test = new Test ();
		test.property = "test";

		test.defaultPropObserver = (oldVal, newVal) ->
			System.out.println ("DefaultProp changed from "+oldVal+" to "+newVal);

		test.defaultProp = 1;
	}
}
