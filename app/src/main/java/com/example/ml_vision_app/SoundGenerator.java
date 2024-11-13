package com.example.ml_vision_app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundGenerator {
    private static AudioTrack audioTrack;
    public static boolean isFrequencyPlaying = false;
    private static final int sampleRate = 44100; // Sample rate in Hz
    private static final double frequency = 440; // Frequency in Hz (A4 note)
    public static void playFrequency() {
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        audioTrack.play();
        isFrequencyPlaying = true;

        new Thread(() -> {
            short[] buffer = new short[bufferSize];
            double angle = 0;

            while (isFrequencyPlaying) {
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = (short) (Math.sin(angle) * Short.MAX_VALUE);
                    angle += 2.0 * Math.PI * frequency / sampleRate;
                    if (angle >= 2.0 * Math.PI) {
                        angle -= 2.0 * Math.PI;
                    }
                }
                audioTrack.write(buffer, 0, buffer.length);
            }
        }).start();
    }

    public static void stopFrequency() {
        isFrequencyPlaying = false;
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}
