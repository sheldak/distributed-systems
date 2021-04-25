package messages;

import actors.SatelliteAPI;

import java.util.Map;


public class Response implements Command {
    public final int queryID;
    public final Map<Integer, SatelliteAPI.Status> statuses;
    public final int returnedStatuses;

    public Response(int queryID, Map<Integer, SatelliteAPI.Status> statuses, int returnedStatuses) {
        this.queryID = queryID;
        this.statuses = statuses;
        this.returnedStatuses = returnedStatuses;
    }
}
