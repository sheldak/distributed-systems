import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Administrator {
    public static void main(String[] argv) throws Exception {
        System.out.println("-- ADMINISTRATOR --");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String EXCHANGE = "expedition_exchange";
        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);

        Runnable reading = () -> {
            String input, command, message;

            System.out.println("Administrator is ready for sending and getting messages");

            try {
                while (true) {
                    input = br.readLine();
                    command = input.split(" ")[0];
                    message = "admin " + input.split(" ")[1];

                    switch (command) {
                        case "quit":
                            System.exit(0);
                        case "all":
                            channel.basicPublish(EXCHANGE, "all.crew.suppliers", null, message.getBytes());
                            System.out.printf("Sent: %s%n", message);
                            break;
                        case "crew":
                        case "suppliers":
                            channel.basicPublish(EXCHANGE, "all." + command, null, message.getBytes());
                            System.out.printf("Sent: %s%n", message);
                            break;
                        default:
                            System.out.println("Incorrect input. It should be \"all\" or \"crew\" or \"suppliers\"");
                            break;
                    }
                }
            } catch (Exception e) {
                System.exit(1);
            }
        };

        Thread reader = new Thread(reading);

        channel.queueDeclare("admin", false, false, false, null);
        channel.queueBind("admin", EXCHANGE, "order.*");

        Consumer crewConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Received: " + message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        reader.start();
        channel.basicConsume("admin", crewConsumer);
    }
}
