package actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.Behaviors;
import messages.Command;
import messages.QueryTrigger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Launcher {
    public static Behavior<Void> create() {
        return Behaviors.setup(
            context -> {
                ActorRef<Command> dispatcher = context.spawn(
                        Behaviors.supervise(Dispatcher.create())
                                .onFailure(Exception.class, SupervisorStrategy.restart()),
                        "Dispatcher",
                        DispatcherSelector.fromConfig("satellites-dispatcher")
                );

                ActorRef<Command> databaseAgent = context.spawn(
                        Behaviors.supervise(DatabaseAgent.create())
                                .onFailure(Exception.class, SupervisorStrategy.restart()),
                        "DatabaseAgent",
                        DispatcherSelector.fromConfig("satellites-dispatcher")
                );

                List<ActorRef<Command>> stations = new ArrayList<>();
                for (String name : List.of("Blue", "Green", "Orange")) {
                    stations.add(context.spawn(
                            Behaviors.supervise(Station.create(name, dispatcher, databaseAgent))
                                    .onFailure(Exception.class, SupervisorStrategy.restart()),
                            name,
                            DispatcherSelector.fromConfig("satellites-dispatcher")
                    ));
                }

                Thread.sleep(1000);
                stations.get(2).tell(new QueryTrigger());

                return Behaviors.receive(Void.class)
                        .onSignal(Terminated.class, sig -> Behaviors.stopped())
                        .build();
            });
    }

    public static void main(String[] args) {
        ActorSystem.create(Launcher.create(), "Launcher");
    }
}
