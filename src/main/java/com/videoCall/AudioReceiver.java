package com.videoCall;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AudioReceiver implements Runnable {
    private final int listenPort;

    public AudioReceiver(int listenPort) {
        this.listenPort = listenPort;
    }

    @Override
    public void run() {
        AudioFormat format = getAudioFormat();
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            byte[] buffer = new byte[512];

            while (true) {
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
