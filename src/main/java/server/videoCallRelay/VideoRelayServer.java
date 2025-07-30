package server.videoCallRelay;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VideoRelayServer extends Thread {
    private  int listenPort;
    private  InetAddress client1Address;
    private  int client1Port=5556;
    private  InetAddress client2Address;
    private  int client2Port=5556;
    private volatile boolean running = true;

    public VideoRelayServer(int listenPort, InetAddress client1Address, int client1Port,
                      InetAddress client2Address, int client2Port) {
        this.listenPort = listenPort;
        this.client1Address = client1Address;
        this.client1Port = client1Port;
        this.client2Address = client2Address;
        this.client2Port = client2Port;
    }

    public VideoRelayServer(int listenPort, InetAddress client1Address, InetAddress client2Address) {
        this.listenPort = listenPort;
        this.client1Address = client1Address;
        this.client2Address = client2Address;
    }

    public void stopThread() {
        running = false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            System.out.println("🎥 VideoRelay started on port " + listenPort);

            byte[] buffer = new byte[65535];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetAddress senderAddr = packet.getAddress();
                int senderPort = packet.getPort();

                InetAddress targetAddr = null;
                int targetPort = -1;

                if (senderAddr.equals(client1Address) && senderPort == client1Port) {
                    targetAddr = client2Address;
                    targetPort = client2Port;
                } else if (senderAddr.equals(client2Address) && senderPort == client2Port) {
                    targetAddr = client1Address;
                    targetPort = client1Port;
                }

                if (targetAddr != null) {
                    DatagramPacket forward = new DatagramPacket(
                            packet.getData(), packet.getLength(), targetAddr, targetPort
                    );
                    socket.send(forward);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ VideoRelay error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
