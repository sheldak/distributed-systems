package sr.ice.server;

import java.util.HashMap;
import java.util.Map;

public class Locator implements com.zeroc.Ice.ServantLocator {
    private int servantID = 1;
    private final Map<String, NegatorI> servants = new HashMap<>();

    @Override
    public com.zeroc.Ice.ServantLocator.LocateResult locate(com.zeroc.Ice.Current c) {
        String objectID = c.id.toString().split("@")[1];

        if (!servants.containsKey(objectID)) {
            servants.put(objectID, new NegatorI(servantID++));
        }

        return new com.zeroc.Ice.ServantLocator.LocateResult(servants.get(objectID), null);
    }

    @Override
    public void finished(com.zeroc.Ice.Current c, com.zeroc.Ice.Object servant, Object cookie) {
    }

    @Override
    public void deactivate(String category) {
    }
}

