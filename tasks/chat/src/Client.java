import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private Socket tcpSocket = null;
    private DatagramSocket udpSocket = null;
    private MulticastSocket multicastSocket = null;

    private InetAddress address = null;
    private InetAddress groupAddress = null;
    private final int portNumber = 12345;
    private final int multicastPortNumber = 12346;

    private String name = "";

    private String mode = "tcp";

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.run();
    }

    public void run() throws IOException {
        try {
            String hostName = "localhost";
            address = InetAddress.getByName(hostName);
            groupAddress = InetAddress.getByName("228.5.6.7");

            tcpSocket = new Socket(hostName, portNumber);
            udpSocket = new DatagramSocket();
            multicastSocket = new MulticastSocket(multicastPortNumber);

            Runnable receivingTCP = this::receiveTCP;
            Runnable receivingUDP = this::receiveUDP;
            Runnable receivingMulticast = this::receiveMulticast;
            Runnable readingAndSending = this::readAndSend;

            Thread receiverTCP = new Thread(receivingTCP);
            Thread receiverUDP = new Thread(receivingUDP);
            Thread receiverMulticast = new Thread(receivingMulticast);
            Thread readerSender = new Thread(readingAndSending);

            receiverTCP.start();
            receiverUDP.start();
            receiverMulticast.start();
            readerSender.start();

            receiverTCP.join();
            receiverUDP.join();
            receiverMulticast.join();
            readerSender.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tcpSocket != null){
                tcpSocket.close();
            }
        }
    }

    private void receiveTCP() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            String msg;

            while(true) {
                msg = in.readLine();
                if(msg.equals("\\quit")) {
                    break;
                }
                System.out.println(msg);
            }

            tcpSocket.close();
            udpSocket.close();
            multicastSocket.close();
            multicastSocket.leaveGroup(groupAddress);
            System.exit(0);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    private void receiveUDP() {
        try {
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                udpSocket.receive(receivePacket);

                if((new String(Arrays.copyOf(receiveBuffer, 5))).equals("\\quit")) {
                    break;
                }
                System.out.println(new String(receivePacket.getData()));
            }
            tcpSocket.close();
            udpSocket.close();
            multicastSocket.close();
            multicastSocket.leaveGroup(groupAddress);
            System.exit(0);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    private void receiveMulticast() {
        try {
            multicastSocket.joinGroup(groupAddress);

            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                multicastSocket.receive(receivePacket);

                String msg = new String(receiveBuffer);

                int nameEnd = 2;
                while (nameEnd < msg.length()+1) {
                    if(msg.startsWith(">:", nameEnd)) {
                        break;
                    }
                    nameEnd++;
                }

                if (!msg.substring(1, nameEnd).equals(name)) {
                    System.out.println(new String(receivePacket.getData()));
                }
            }
        } catch (Exception e) {}
    }

    private void readAndSend() {
        try {
            Scanner scanner = new Scanner(System.in);
            PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);

            System.out.println("Enter your name");
            name = scanner.nextLine();

            out.println(name);

            byte[] sendBuffer = name.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
            udpSocket.send(sendPacket);

            String msg;

            while(true) {
                msg = scanner.nextLine();

                if (msg.equals("\\quit")) {
                    out.println(msg);

                    sendBuffer = msg.getBytes();
                    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
                    udpSocket.send(sendPacket);

                    break;
                }

                if (msg.startsWith("\\A ")) {
                    StringBuilder contentsBuilder = new StringBuilder();

                    File file = new File(msg.substring(3));
                    Scanner fileReader = new Scanner(file);
                    while (fileReader.hasNextLine()) {
                        contentsBuilder.append(fileReader.nextLine());
                        contentsBuilder.append("\n");
                    }
                    fileReader.close();

                    msg = "<" + name + ">: " + contentsBuilder.toString();
                    sendBuffer = msg.getBytes();
                    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
                    udpSocket.send(sendPacket);
                } else {
                    switch (msg) {
                        case "\\U":
                            mode = "udp";
                            break;
                        case "\\T":
                            mode = "tcp";
                            break;
                        case "\\M":
                            mode = "multicast";
                            break;
                        default:
                            switch (mode) {
                                case "tcp":
                                    out.println(msg);
                                    break;
                                case "udp":
                                    msg = "<" + name + ">: " + msg;
                                    sendBuffer = msg.getBytes();
                                    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
                                    udpSocket.send(sendPacket);
                                    break;
                                case "multicast":
                                    msg = "<" + name + ">: " + msg;
                                    sendBuffer = msg.getBytes();
                                    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, groupAddress, multicastPortNumber);
                                    multicastSocket.send(sendPacket);
                                    break;
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}