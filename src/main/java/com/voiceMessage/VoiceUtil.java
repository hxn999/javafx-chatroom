package com.voiceMessage;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


public class VoiceUtil {
    public static void setRecording(boolean flag){
        isRecording = flag;
    }

    public static boolean isRecording() {
        return isRecording;
    }

    protected static boolean isRecording = false;
    static ByteArrayOutputStream out;
    /**
     * Defines an audio format
     */
    static AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
        return format;
    }
    public static void saveAsWav(byte[] audio, String filename) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(audio);
            AudioFormat format = getAudioFormat();
            AudioInputStream ais = new AudioInputStream(bais, format, audio.length / format.getFrameSize());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
            System.out.println("Saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
