package com.videoCall;


import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VideoReceiver implements Runnable {
    private final ImageView imageView;
    private final int listenPort;

    public VideoReceiver(ImageView imageView, int listenPort) {
        this.imageView = imageView;
        this.listenPort = listenPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            byte[] buffer = new byte[65535];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                BufferedImage image = ImageIO.read(bais);
                if (image != null) {
//                    javafx.scene.image.Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(image, null);
//                    Platform.runLater(() -> imageView.setImage(fxImage));
                    // TODO show the image in the ImageView dialog
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
