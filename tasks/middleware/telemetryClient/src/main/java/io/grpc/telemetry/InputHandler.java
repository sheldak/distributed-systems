package io.grpc.telemetry;

import io.grpc.Channel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class InputHandler {
    private final Channel channel;
    private final List<Thread> threads = new ArrayList<>();

    private String name;
    private int x = -1;
    private int y = -1;

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    String possibleCommandsInfo =
            "You can send measurement or quit by typing number:\n" +
            "1 - temperature\n" +
            "2 - humidity\n" +
            "3 - weather\n" +
            "4 - quit";

    public InputHandler(Channel channel) {
        this.channel = channel;
    }

    public void handler() {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.println("Enter your name");
            name = scanner.nextLine();

            while (x < 0 && y < 0) {
                System.out.println("Enter positive coordinates in format: \"x y\"");
                String[] coords = scanner.nextLine().split(" ");

                try {
                    if (coords.length == 2) {
                        x = Integer.parseInt(coords[0]);
                        y = Integer.parseInt(coords[1]);
                    } else {
                        System.out.println("Coordinates should consist of two numbers");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid format of coordinates. They should be a pair of positive integers");
                }
            }

            String command;

            System.out.println(possibleCommandsInfo);

            while(true) {
                command = scanner.nextLine();

                if ("4".equals(command)) {
                    for (Thread thread : threads) {
                        thread.join();
                    }
                    break;
                }

                switch (command) {
                    case "1":
                        sendTemperature();
                        break;
                    case "2":
                        sendHumidity();
                        break;
                    case "3":
                        sendWeather();
                        break;
                    default:
                        System.out.println("Invalid command");
                        System.out.println(possibleCommandsInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTemperature() {
        float temperature = ((int) (-200 + Math.random() * 500) / 10f);

        Connector temperatureConnector = new Connector(channel)
                .withTemperature(
                        Temperature
                                .newBuilder()
                                .setInfo(buildInfo())
                                .setTemperature(temperature)
                                .build()
                );

        Runnable sendRunnable =  temperatureConnector::sendTemperature;
        Thread temperatureThread = new Thread(sendRunnable);
        threads.add(temperatureThread);
        temperatureThread.start();
    }

    private void sendHumidity() {
        int humidity = (int) (Math.random() * 101);

        Connector humidityConnector = new Connector(channel)
                .withHumidity(
                        Humidity
                                .newBuilder()
                                .setInfo(buildInfo())
                                .setHumidity(humidity)
                                .build()
                );

        Runnable sendRunnable =  humidityConnector::sendHumidity;
        Thread humidityThread = new Thread(sendRunnable);
        threads.add(humidityThread);
        humidityThread.start();
    }

    private void sendWeather() {
        Weather.Builder weatherBuilder = Weather.newBuilder();

        int type1 = (int) (Math.random() * 3);
        switch (type1) {
            case 0:
                weatherBuilder.addWeatherType(Weather.WeatherType.SUN);
                break;
            case 1:
                weatherBuilder.addWeatherType(Weather.WeatherType.CLOUDS);

                int type2 = (int) (Math.random() * 3);
                switch (type2) {
                    case 0:
                        weatherBuilder.addWeatherType(Weather.WeatherType.RAIN);
                        break;
                    case 1:
                        weatherBuilder.addWeatherType(Weather.WeatherType.SNOW);
                }
                break;
            case 2:
                weatherBuilder.addWeatherType(Weather.WeatherType.FOG);
                break;
        }

        if (Math.random() > 0.67) {
            weatherBuilder.addWeatherType(Weather.WeatherType.WIND);
        }

        Connector weatherConnector = new Connector(channel)
                .withWeather(
                        weatherBuilder
                                .setInfo(buildInfo())
                                .build()
                );

        Runnable sendRunnable =  weatherConnector::sendWeather;
        Thread weatherThread = new Thread(sendRunnable);
        threads.add(weatherThread);
        weatherThread.start();
    }

    private Info buildInfo() {
        return Info
                .newBuilder()
                .setTimestamp(formatter.format(new Date()))
                .setLocationX(x)
                .setLocationY(y)
                .setSubscriber(name)
                .build();
    }
}
