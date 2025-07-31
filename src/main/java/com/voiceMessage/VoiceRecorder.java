package com.voiceMessage;

//import com.client.chatwindow.Listener;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class VoiceRecorder extends VoiceUtil {

    public static byte[] store; // Moved to class level

    public static void captureAudio() {
        try {
            final AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            Runnable runner = new Runnable() {
                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                public void run() {
                    out = new ByteArrayOutputStream();
                    isRecording = true;
                    try {
                        while (isRecording) {
                            int count = line.read(buffer, 0, buffer.length);
                            if (count > 0) {
                                out.write(buffer, 0, count);
                                System.out.println("recording... " + count + " bytes captured");
                            }
                        }
//                        VoicePlayback.playAudio(out.toByteArray());
//                        VoiceUtil.saveAsWav(out.toByteArray(), "voice.wav");
                        System.out.println("Recording stopped, total bytes captured: " + out.size());
                        store = out.toByteArray(); // Assign to class-level variable
                        // TODO: Capture and send the voice to the chatController from here
                    } finally {
                        try {
                            out.close();
                            out.flush();
                            line.close();
                            line.flush();
//                            Listener.sendVoiceMessage(out.toByteArray());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread captureThread = new Thread(runner);
            captureThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: ");
            e.printStackTrace();
        }
    }

    public static byte[] getAudioByteArray() {
        return store; // Access class-level variable
    }


}
