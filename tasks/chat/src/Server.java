import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Server {
    private DatagramSocket udpSocket = null;
    private ServerSocket tcpSocket = null;

    private final Set<Thread> threads = new HashSet<>();

    private final HashMap<String, PrintWriter> writersToClient = new HashMap<>();
    private final HashSet<PrintWriter> notNamedWriters = new HashSet<>();

    private final HashMap<Integer, String> udpClientsPorts = new HashMap<>();

    boolean quit = false;

    private final Semaphore quitSemaphore = new Semaphore(1);
    private final Semaphore writeSemaphore = new Semaphore(1, true);

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.run();
    }

    public void run() throws Exception {
        int portNumber = 12345;
        try {
            Runnable reading = this::reading;
            Thread reader = new Thread(reading);
            threads.add(reader);
            reader.start();

            udpSocket = new DatagramSocket(portNumber);
            tcpSocket = new ServerSocket(portNumber);

            Runnable udpCommunicationRunnable = this::communicateUDP;
            Thread udpCommunicationThread = new Thread(udpCommunicationRunnable);
            threads.add(udpCommunicationThread);
            udpCommunicationThread.start();

            while(true) {
                quitSemaphore.acquire();
                if (quit) {
                    break;
                }
                quitSemaphore.release();

                final Socket clientSocket = tcpSocket.accept();
                Runnable communicationRunnable = () -> communicateTCP(clientSocket);
                Thread communicationThread = new Thread(communicationRunnable);
                threads.add(communicationThread);
                communicationThread.start();
            }
        } catch (Exception e) {
            System.out.println("Server stopped");
        }
        finally {
            if (tcpSocket != null && !tcpSocket.isClosed()) {
                tcpSocket.close();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    private void communicateTCP(Socket socket) {
        String clientName = "";
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writeSemaphore.acquire();
            notNamedWriters.add(out);
            writeSemaphore.release();

            clientName = in.readLine();

            writeSemaphore.acquire();

            for (Map.Entry<String, PrintWriter> entry : writersToClient.entrySet()) {
                entry.getValue().println("<SERVER>: " + clientName + " enters the chat");
            }
            notNamedWriters.remove(out);
            writersToClient.put(clientName, out);

            writeSemaphore.release();

            String msg;

            while(true) {
                msg = in.readLine();
                if (msg.equals("\\quit")) {
                    break;
                }

                writeSemaphore.acquire();
                for (Map.Entry<String, PrintWriter> entry : writersToClient.entrySet()) {
                    if (!entry.getKey().equals(clientName)) {
                        entry.getValue().println("<" + clientName + ">: " + msg);
                    }
                }
                writeSemaphore.release();
            }

            writeSemaphore.acquire();

            out.println("\\quit");
            writersToClient.remove(clientName);
            for (Map.Entry<String, PrintWriter> entry : writersToClient.entrySet()) {
                if (!entry.getKey().equals(clientName)) {
                    entry.getValue().println("<SERVER>: " + clientName + " disconnected");
                }
            }
            writeSemaphore.release();
        } catch (Exception e) {
            try {
                if (!clientName.equals("")) {
                    writeSemaphore.acquire();

                    writersToClient.remove(clientName);
                    for (Map.Entry<String, PrintWriter> entry : writersToClient.entrySet()) {
                        if (!entry.getKey().equals(clientName)) {
                            entry.getValue().println("<SERVER>: " + clientName + " disconnected");
                        }
                    }
                    writeSemaphore.release();
                }
            } catch (Exception ex) {}
        }
    }

    private void communicateUDP() {
        try{
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket);
                InetAddress address = receivePacket.getAddress();
                int port = receivePacket.getPort();

                if ((new String(Arrays.copyOf(receiveBuffer, 5))).equals("\\quit")) {
                    udpClientsPorts.remove(port);
                } else {
                    if (udpClientsPorts.containsKey(port)) {
                        for (Map.Entry<Integer, String> entry : udpClientsPorts.entrySet()) {
                            if (!entry.getKey().equals(port)) {
                                DatagramPacket sendPacket = new DatagramPacket(
                                        receiveBuffer, receiveBuffer.length,
                                        address, entry.getKey()
                                );
                                udpSocket.send(sendPacket);
                            }
                        }

                    } else {
                        String name = new String(receivePacket.getData());
                        udpClientsPorts.put(port, name);
                    }
                }
            }
        }
        catch(Exception e) {}
    }

    private void reading() {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("To stop server type: \"\\quit\"");
            String command = "";

            while(!command.equals("\\quit")) {
                System.out.println(command);
                command = scanner.nextLine();
            }

            quitSemaphore.acquire();
            quit = true;
            quitSemaphore.release();

            writeSemaphore.acquire();
            for (Map.Entry<String, PrintWriter> entry : writersToClient.entrySet()) {
                entry.getValue().println("\\quit");
            }
            for (PrintWriter writer : notNamedWriters) {
                writer.println("\\quit");
            }
            tcpSocket.close();
            udpSocket.close();
            writeSemaphore.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}