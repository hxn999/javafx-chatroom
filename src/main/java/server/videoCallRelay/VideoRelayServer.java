package server.videoCallRelay;



import java.net.*;

public class VideoRelayServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(5555);
        System.out.println("Relay server started on port 5555");

        InetAddress client1 = null, client2 = null;
        int port1 = -1, port2 = -1;

        byte[] buffer = new byte[65535];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            InetAddress senderAddr = packet.getAddress();
            int senderPort = packet.getPort();

            if (client1 == null) {
                client1 = senderAddr;
                port1 = senderPort;
                System.out.println("Client 1 joined: " + senderAddr + ":" + senderPort);
            } else if (client2 == null && !(client1.equals(senderAddr) && port1 == senderPort)) {
                client2 = senderAddr;
                port2 = senderPort;
                System.out.println("Client 2 joined: " + senderAddr + ":" + senderPort);
            }

            InetAddress targetAddr = null;
            int targetPort = -1;

            if (senderAddr.equals(client1) && senderPort == port1 && client2 != null) {
                targetAddr = client2;
                targetPort = port2;
            } else if (senderAddr.equals(client2) && senderPort == port2) {
                targetAddr = client1;
                targetPort = port1;
            }

            if (targetAddr != null) {
                DatagramPacket forward = new DatagramPacket(packet.getData(), packet.getLength(), targetAddr, targetPort);
                socket.send(forward);
            }
        }
    }
}
