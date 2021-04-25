package actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Worker extends AbstractBehavior<Command> {
    private final ActorRef<Command> station;

    private final int id;
    private int queryID;
    private int satellitesNumber;
    private int timeout;

    private final ArrayList<ActorRef<StatusRequest>> satellites = new ArrayList<>();
    private final Map<Integer, SatelliteAPI.Status> statuses = new HashMap<>();
    private int returnedStatuses = 0;

    public Worker(ActorContext<Command> context, ActorRef<Command> station, int id) {
        super(context);
        this.station = station;
        this.id = id;
    }

    public static Behavior<Command> create(ActorRef<Command> station, int id) {
        return Behaviors.setup(context -> new Worker(context, station, id));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Request.class, this::onRequest)
                .onMessage(StatusResponse.class, this::onStatusResponse)
                .onMessage(Timeout.class, this::onTimeout)
                .build();
    }

    public Behavior<Command> onRequest(Request request) {
        queryID = request.queryID;
        satellitesNumber = request.range;
        timeout = request.timeout;

        ActorRef<StartTimer> timer = getContext().spawn(
                Behaviors.supervise(Timer.create(getContext().getSelf()))
                        .onFailure(Exception.class, SupervisorStrategy.restart()),
                String.format("Timer-%d", id),
                DispatcherSelector.fromConfig("satellites-dispatcher")
        );

        for (int i = request.firstSatelliteID; i < request.firstSatelliteID + request.range; i++) {
            ActorRef<StatusRequest> satellite = getContext().spawn(
                    Behaviors.supervise(Satellite.create(i, getContext().getSelf()))
                            .onFailure(Exception.class, SupervisorStrategy.restart()),
                    String.format("Satellite-%d-%d", id, i),
                    DispatcherSelector.fromConfig("satellites-dispatcher")
            );
            satellites.add(satellite);
            satellite.tell(new StatusRequest(System.currentTimeMillis()));
        }

        timer.tell(new StartTimer(request.timeout));
        return this;
    }

    public Behavior<Command> onStatusResponse(StatusResponse statusResponse) {
        if (System.currentTimeMillis() - statusResponse.timestamp <= timeout) {
            if (statusResponse.status != SatelliteAPI.Status.OK) {
                statuses.put(statusResponse.id, statusResponse.status);
            }
            returnedStatuses++;
        }

        return this;
    }

    public Behavior<Command> onTimeout(Timeout timeout) {
        station.tell(new Response(
                queryID,
                statuses,
                Math.round((float) returnedStatuses / satellitesNumber)
        ));
        return this;
    }
}