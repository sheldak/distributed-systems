package sr.ice.server;

import com.zeroc.Ice.Current;

import Demo.Negator;

public class NegatorI implements Negator
{
	private final int id;
	public NegatorI(int id) {
		this.id = id;
		System.out.println("New servant instantiated. ID = " + id);
	}

	@Override
	public int negate(int number, Current __current)
	{
		System.out.println(
				"Negate function called.\n" +
				"Number = " + number + "\n" +
				"Result = " + (-number) + "\n" +
				"Object:  " + __current.id.toString().split("@")[1] + "\n" +
				"Servant:  " + id);

		return -number;
	}
}
