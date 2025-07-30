package com.videoCall;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static com.videoCall.VideoReceiver.convertBufferedImageToFxImage;

public class VideoSender extends Thread {
    private  String serverIP;
    private  int serverPort;
    private ImageView imageView;
    private volatile boolean running = true;

    public VideoSender(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }
    public VideoSender(ImageView imageView) {
        this.imageView = imageView;

    }

    public void stopThread() {
        running = false;
    }

    @Override
    public void run() {
//
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverIP);
            Webcam webcam = Webcam.getDefault();
            webcam.setViewSize(new java.awt.Dimension(320, 240));
            webcam.open();

            while (running) {
                BufferedImage image = webcam.getImage();
//                Image fxImage= convertBufferedImageToFxImage(image);
//                Platform.runLater(() -> imageView.setImage(fxImage));
                System.out.println("video running ..");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] data = baos.toByteArray();

//                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddr, serverPort);
//                socket.send(packet);

                Thread.sleep(40); // ~15 FPS
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
