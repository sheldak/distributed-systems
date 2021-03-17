import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Supplier {
    public static int orderNumber = 0;

    public static void main(String[] argv) throws Exception {
        System.out.println("-- SUPPLIER --");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Enter supplier name: ");
        final String name = br.readLine();

        Set<String> equipment = new HashSet<>();
        System.out.println("Provide equipment available from that supplier. At the end type \"done\"");
        while (true) {
            String input = br.readLine();
            if (input.equals("done")) {
                break;
            } else {
                equipment.add(input);
            }
        }

        System.out.println("Supplier is ready to accept orders");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String EXCHANGE = "expedition_exchange";
        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);

        for (String product : equipment) {
            channel.queueDeclare(product, false, false, false, null);
            channel.queueBind(product, EXCHANGE, "order." + product);
        }
        channel.basicQos(1);

        channel.queueDeclare(name, false, false, false, null);
        channel.queueBind(name, EXCHANGE, "all.#.suppliers.#");

        Consumer supplierConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                String sender = message.split(" ")[0];
                String product = message.split(" ")[1];

                System.out.printf("Received: %s%n", message);
                channel.basicAck(envelope.getDeliveryTag(), false);

                if (!sender.equals("admin")) {
                    String returnMessage = String.format("Order %s-%s-%s-%d completed", sender, name, product, orderNumber++);
                    channel.basicPublish(EXCHANGE, "order." + sender, null, returnMessage.getBytes());
                    System.out.printf("Sent: %s%n", returnMessage);
                }
            }
        };

        for (String product : equipment) {
            channel.basicConsume(product, supplierConsumer);
        }
        channel.basicConsume(name, supplierConsumer);
    }
}