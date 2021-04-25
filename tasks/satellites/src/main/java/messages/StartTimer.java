package messages;

public class StartTimer implements Command {
    public final int time;

    public StartTimer(int time) {
        this.time = time;
    }
}
