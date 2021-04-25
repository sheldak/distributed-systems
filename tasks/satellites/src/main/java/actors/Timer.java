package actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;

public class Timer extends AbstractBehavior<StartTimer> {
    private final ActorRef<Command> worker;

    public Timer(ActorContext<StartTimer> context, ActorRef<Command> worker) {
        super(context);
        this.worker = worker;
    }

    public static Behavior<StartTimer> create(ActorRef<Command> worker) {
        return Behaviors.setup(context -> new Timer(context, worker));
    }

    @Override
    public Receive<StartTimer> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartTimer.class, this::onStartTimer)
                .build();
    }

    public Behavior<StartTimer> onStartTimer(StartTimer startTimer) {
        try {
            Thread.sleep(startTimer.time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        worker.tell(new Timeout());
        return this;
    }
}
