package messages;

import actors.SatelliteAPI;

import java.util.Map;

public class DatabaseUpdate implements Command {
    public final Map<Integer, Integer> errors;

    public DatabaseUpdate(Map<Integer, Integer> errors) {
        this.errors = errors;
    }
}
