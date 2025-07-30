package com.videoCall;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AudioReceiver extends Thread {
    private final int listenPort;
    private volatile boolean running = true;

    public AudioReceiver(int listenPort) {
        this.listenPort = listenPort;
    }

    public void stopThread() {
        running = false;
    }

    @Override
    public void run() {
        AudioFormat format = getAudioFormat();
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            byte[] buffer = new byte[512];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                speakers.write(packet.getData(), 0, packet.getLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AudioFormat getAudioFormat() {
        return new AudioFormat(16000.0f, 16, 1, true, false);
    }
}
