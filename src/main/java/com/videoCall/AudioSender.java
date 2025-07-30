package com.videoCall;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioSender implements Runnable {
    private final String serverIP;
    private final int serverPort;

    public AudioSender(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        AudioFormat format = getAudioFormat();
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverIP);
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[512];

            while (true) {
                int count = microphone.read(buffer, 0, buffer.length);
                if (count > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, count, serverAddr, serverPort);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AudioFormat getAudioFormat() {
        return new AudioFormat(16000.0f, 16, 1, true, false);
    }
}
