import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Crew {
    public static void main(String[] argv) throws Exception {
        System.out.println("-- CREW --");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Enter crew name: ");
        final String name = br.readLine();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String EXCHANGE = "expedition_exchange";
        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);

        Runnable reading = () -> {
            String input, message;

            System.out.println("Now you can order equipment");

            try {
                while (true) {
                    input = br.readLine();
                    if (input.equals("quit")) {
                        System.exit(0);
                    } else {
                        message = String.format("%s %s", name, input);
                        channel.basicPublish(EXCHANGE, "order." + input, null, message.getBytes());
                        System.out.printf("Sent: %s%n", message);
                    }
                }
            } catch (Exception e) {
                System.exit(1);
            }
        };

        Thread reader = new Thread(reading);

        channel.queueDeclare(name, false, false, false, null);
        channel.queueBind(name, EXCHANGE, "order." + name);
        channel.queueBind(name, EXCHANGE, "all.#.crew.#");

        Consumer crewConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.printf("Received: %s%n", message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        reader.start();
        channel.basicConsume(name, crewConsumer);
    }
}
