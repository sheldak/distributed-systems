package messages;

import akka.actor.typed.ActorRef;

public class Request implements Command {
    public final int queryID;
    public final int firstSatelliteID;
    public final int range;
    public final int timeout;
    public final ActorRef<Command> sender;

    public Request(int queryID, int firstSatelliteID, int range, int timeout, ActorRef<Command> sender) {
        this.queryID = queryID;
        this.firstSatelliteID = firstSatelliteID;
        this.range = range;
        this.timeout = timeout;
        this.sender = sender;
    }
}
