package actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.Command;
import messages.Request;

public class Dispatcher extends AbstractBehavior<Command> {
    private int workers = 0;

    public Dispatcher(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Dispatcher::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Request.class, this::onRequest)
                .build();
    }

    public Behavior<Command> onRequest(Request request) {
        ActorRef<Command> worker = getContext().spawn(
                Behaviors.supervise(Worker.create(request.sender, workers))
                        .onFailure(Exception.class, SupervisorStrategy.restart()),
                String.format("Worker-%d", workers++),
                DispatcherSelector.fromConfig("satellites-dispatcher")
        );

        worker.tell(request);
        return this;
    }
}
