package server.videoCallRelay;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioRelayServer implements Runnable {

    private final int listenPort;
    private final InetAddress client1Addr;
    private final int client1Port;
    private final InetAddress client2Addr;
    private final int client2Port;

    public AudioRelayServer(int listenPort, InetAddress client1Addr, int client1Port,
                      InetAddress client2Addr, int client2Port) {
        this.listenPort = listenPort;
        this.client1Addr = client1Addr;
        this.client1Port = client1Port;
        this.client2Addr = client2Addr;
        this.client2Port = client2Port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            byte[] buffer = new byte[512];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetAddress senderAddr = packet.getAddress();
                int senderPort = packet.getPort();

                DatagramPacket forwardPacket;
                if (senderAddr.equals(client1Addr) && senderPort == client1Port) {
                    forwardPacket = new DatagramPacket(packet.getData(), packet.getLength(), client2Addr, client2Port);
                } else if (senderAddr.equals(client2Addr) && senderPort == client2Port) {
                    forwardPacket = new DatagramPacket(packet.getData(), packet.getLength(), client1Addr, client1Port);
                } else {
                    continue; // ignore unknown source
                }

                socket.send(forwardPacket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
