package sr.ice.server;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Identity;

public class Server {
	public static void main(String[] args)
	{
		int status = 0;
		Communicator communicator = null;

		try	{
			communicator = Util.initialize(args);
			ObjectAdapter adapter = communicator.createObjectAdapter("Adapter");

			adapter.addServantLocator(new Locator(), "separate");

			NegatorI negatorServant = new NegatorI(0);

			adapter.add(negatorServant, new Identity("neg1", "shared"));
			adapter.add(negatorServant, new Identity("neg2", "shared"));
			adapter.add(negatorServant, new Identity("neg3", "shared"));

			adapter.activate();
			communicator.waitForShutdown();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			status = 1;
		}
		if (communicator != null) {
			try {
				communicator.destroy();
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
				status = 1;
			}
		}
		System.exit(status);
	}
}
