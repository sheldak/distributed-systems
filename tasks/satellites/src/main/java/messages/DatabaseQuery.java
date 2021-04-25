package messages;

import akka.actor.typed.ActorRef;

public class DatabaseQuery implements Command {
    public final ActorRef<Command> sender;
    public final int satelliteID;

    public DatabaseQuery(ActorRef<Command> sender, int satelliteID) {
        this.sender = sender;
        this.satelliteID = satelliteID;
    }
}
