package messages;

public class DatabaseResponse implements Command {
    public final int satelliteID;
    public final int errorsNumber;

    public DatabaseResponse(int satelliteID, int errorsNumber) {
        this.satelliteID = satelliteID;
        this.errorsNumber = errorsNumber;
    }
}
