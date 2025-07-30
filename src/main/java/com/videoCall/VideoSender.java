package com.videoCall;

import com.github.sarxos.webcam.Webcam;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VideoSender implements Runnable {
    private final String serverIP;
    private final int serverPort;

    public VideoSender(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverIP);
            Webcam webcam = Webcam.getDefault();
            webcam.setViewSize(new java.awt.Dimension(320, 240));
            webcam.open();

            while (true) {
                BufferedImage image = webcam.getImage();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] data = baos.toByteArray();

                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddr, serverPort);
                socket.send(packet);

                Thread.sleep(67); // ~15 FPS
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
