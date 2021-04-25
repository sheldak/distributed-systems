package messages;

import actors.SatelliteAPI;

public class StatusResponse implements Command {
    public final int id;
    public final SatelliteAPI.Status status;
    public final long timestamp;

    public StatusResponse(int id, SatelliteAPI.Status status, long timestamp) {
        this.id = id;
        this.status = status;
        this.timestamp = timestamp;
    }
}
