package messages;

public class StatusRequest implements Command {
    public final long timestamp;

    public StatusRequest(long timestamp) {
        this.timestamp = timestamp;
    }
}
