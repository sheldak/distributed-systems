package io.grpc.telemetry;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) throws Exception {
        String target = "localhost:50051";

        ManagedChannel channel =
                ManagedChannelBuilder
                        .forTarget(target)
                        .usePlaintext()
                        .build();
        try {
            InputHandler inputHandler = new InputHandler(channel);
            Runnable inputRunnable = inputHandler::handler;

            Thread inputThread = new Thread(inputRunnable);
            inputThread.start();

            inputThread.join();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
