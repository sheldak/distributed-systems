package actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;

import java.util.*;

public class Station extends AbstractBehavior<Command> {
    private final String name;
    private final List<Long> startTimes = new ArrayList<>();

    private final ActorRef<Command> databaseAgent;
    private final Map<Integer, Integer> errors = new HashMap<>();

    public Station(ActorContext<Command> context, String name, ActorRef<Command> dispatcher, ActorRef<Command> databaseAgent) {
        super(context);
        this.name = name;
        this.databaseAgent = databaseAgent;

        dispatcher.tell(createRequest(0));
        startTimes.add(System.currentTimeMillis());
        dispatcher.tell(createRequest(1));
        startTimes.add(System.currentTimeMillis());
    }

    public static Behavior<Command> create(String name, ActorRef<Command> dispatcher, ActorRef<Command> databaseAgent) {
        return Behaviors.setup(context -> new Station(context, name, dispatcher, databaseAgent));
    }

    public Request createRequest(int index) {
        Random random = new Random();

        return new Request(
                index,
                100 + random.nextInt(50),
                50,
                300,
                getContext().getSelf()
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Response.class, this::onResponse)
                .onMessage(QueryTrigger.class, this::onQueryTrigger)
                .onMessage(DatabaseResponse.class, this::onDatabaseResponse)
                .build();
    }

    public Behavior<Command> onResponse(Response response) {
        formatAndPrint(response);

        Map<Integer, Integer> errors = new HashMap<>();
        for (Map.Entry<Integer, SatelliteAPI.Status> entry : response.statuses.entrySet()) {
            if (entry.getValue() != SatelliteAPI.Status.OK) {
                errors.put(entry.getKey(), 1);
            }
        }

        databaseAgent.tell(new DatabaseUpdate(errors));
        return this;
    }

    public Behavior<Command> onQueryTrigger(QueryTrigger queryTrigger) {
        for (int i=100; i<=199; i++) {
            databaseAgent.tell(new DatabaseQuery(getContext().getSelf(), i));
        }
        return this;
    }

    public Behavior<Command> onDatabaseResponse(DatabaseResponse databaseResponse) {
        errors.put(databaseResponse.satelliteID, databaseResponse.errorsNumber);

        if (errors.size() == 100) {
            boolean first = true;
            for (Map.Entry<Integer, Integer> entry : errors.entrySet()) {
                if (entry.getValue() > 0) {
                    if (first) {
                        System.out.printf("Errors by %s:\n", name);
                        first = false;
                    }
                    System.out.printf("%d -> %d\n", entry.getKey(), entry.getValue());
                }
            }
        }
        return this;
    }

    private void formatAndPrint(Response response) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Station \"%s\"\n", name));
        builder.append(String.format(
                "Response time: %dms\n",
                System.currentTimeMillis() - startTimes.get(response.queryID)
        ));
        builder.append(String.format("Found %d errors:\n", response.statuses.size()));
        for (Map.Entry<Integer, SatelliteAPI.Status> entry : response.statuses.entrySet()) {
            builder.append(String.format("%d -> %s\n", entry.getKey(), entry.getValue().toString()));
        }

        System.out.println(builder);
    }
}
