package two_clients;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        int javaPort = 9009;
        int pythonPort = 9010;

        Runnable pythonClients = () -> {
            communication(pythonPort, "Pong Python");
        };

        Thread pythonThread = new Thread(pythonClients);
        pythonThread.start();

        communication(javaPort, "Pong Java");

        pythonThread.join();
    }

    public static void communication(int port, String message) {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(port);

            while(true) {
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket =
                        new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                String msg = new String(receivePacket.getData());
                System.out.println("received msg: " + msg);

                byte[] sendBuffer = message.getBytes();
                DatagramPacket sendPacket =
                        new DatagramPacket(sendBuffer, sendBuffer.length, receivePacket.getAddress(), receivePacket.getPort());
                socket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally  {
            if (socket != null) socket.close();
        }
    }
}
