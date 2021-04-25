package actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.Command;
import messages.StatusRequest;
import messages.StatusResponse;

public class Satellite extends AbstractBehavior<StatusRequest> {
    private final int id;
    private final ActorRef<Command> worker;

    public Satellite(ActorContext<StatusRequest> context, int id, ActorRef<Command> worker) {
        super(context);
        this.id = id;
        this.worker = worker;
    }

    public static Behavior<StatusRequest> create(int id, ActorRef<Command> worker) {
        return Behaviors.setup(context -> new Satellite(context, id, worker));
    }

    @Override
    public Receive<StatusRequest> createReceive() {
        return newReceiveBuilder()
                .onMessage(StatusRequest.class, this::onStatusRequest)
                .build();
    }

    public Behavior<StatusRequest> onStatusRequest(StatusRequest statusRequest) {
        SatelliteAPI.Status status = SatelliteAPI.getStatus(id);
        worker.tell(new StatusResponse(id, status, statusRequest.timestamp));
        return this;
    }
}
