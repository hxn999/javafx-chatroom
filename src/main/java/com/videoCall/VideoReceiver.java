package com.videoCall;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VideoReceiver extends Thread {
    private final ImageView imageView;
    private final int listenPort;
    @FXML private ImageView video;
    private volatile boolean running = true;

    public VideoReceiver(ImageView imageView, int listenPort) {
        this.imageView = imageView;
        this.listenPort = listenPort;
    }
    public static Image convertBufferedImageToFxImage(BufferedImage bImage) {
        if (bImage == null) {
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(bImage, "jpg", bos); // Write as PNG to the stream
            byte[] imageBytes = bos.toByteArray();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                return new Image(bis); // Create JavaFX Image from the stream
            }
        } catch (Exception e) {
            System.err.println("Error converting BufferedImage to JavaFX Image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void stopThread() {
        running = false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            byte[] buffer = new byte[65535];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                BufferedImage image = ImageIO.read(bais);
                if (image != null) {

                    // TODO show the image in the ImageView dialog
                    Image fxImage= convertBufferedImageToFxImage(image);
                    Platform.runLater(() -> imageView.setImage(fxImage));

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
