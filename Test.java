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
		}
	}

	String defaultProp { get; set; }
}