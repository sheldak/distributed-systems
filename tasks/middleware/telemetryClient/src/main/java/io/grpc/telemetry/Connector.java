package io.grpc.telemetry;

import io.grpc.Channel;

public class Connector {
    private final TelemetryGrpc.TelemetryBlockingStub blockingStub;

    private Temperature temperature;
    private Humidity humidity;
    private Weather weather;

    public Connector(Channel channel) {
        blockingStub = TelemetryGrpc.newBlockingStub(channel);
    }

    public Connector withTemperature(Temperature temperature) {
        this.temperature = temperature;
        return this;
    }

    public Connector withHumidity(Humidity humidity) {
        this.humidity = humidity;
        return this;
    }

    public Connector withWeather(Weather weather) {
        this.weather = weather;
        return this;
    }

    public void sendTemperature() {
        System.out.println("Saving temperature measurement...");
        try {
            Reply reply = blockingStub.saveTemperature(temperature);
            printReply(reply);
        } catch (Exception ex) {
            System.out.printf("Client error when saving temperature measurement. Status: ERROR\n %s\n", ex.getMessage());
        }
    }

    public void sendHumidity() {
        System.out.println("Saving humidity measurement...");
        try {
            Reply reply = blockingStub.saveHumidity(humidity);
            printReply(reply);
        } catch (Exception ex) {
            System.out.printf("Client error when saving humidity measurement. Status: ERROR\n %s\n", ex.getMessage());
        }
    }

    public void sendWeather() {
        System.out.println("Saving weather measurement...");
        try {
            Reply reply = blockingStub.saveWeather(weather);
            printReply(reply);
        } catch (Exception ex) {
            System.out.printf("Client error when saving weather measurement. Status: ERROR\n %s\n", ex.getMessage());
        }
    }

    private void printReply(Reply reply) {
        if (reply.getStatus() == 0) {
            System.out.println("Measurement Saved. Status: OK");
        } else {
            System.out.println("Measurement Saved. Status: ERROR");
        }
        System.out.println(reply.getMessage());
    }
}
