package com.example.ml_vision_app;

import static android.content.ContentValues.TAG;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.mlkit.vision.pose.Pose;

public class SoundGenerator {
    private static AudioTrack audioTrack;
    public static boolean isFrequencyPlaying = false;
    private static final int sampleRate = 44100; // Sample rate in Hz
    private static final double baseFrequency = 440; // Frequency in Hz (A4 note)
    private static double frequency = baseFrequency;
    private static Thread soundThread;
    public static void playFrequency() {
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
        isFrequencyPlaying = true;

        soundThread = new Thread(() -> {
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
        });
        soundThread.start();
    }
    public void observeLifecycle(LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                    stopFrequency();
                }
            }
        });
    }
    public static void stopFrequency() {
        isFrequencyPlaying = false;
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        if (soundThread.isAlive()) {
            soundThread.interrupt();
            try {
                soundThread.join();
            } catch (InterruptedException e) {
                // Handle interruption
                Log.e(TAG, "stopFrequency: ", e);
            }
        }
    }

    public static double getFrequency() {
        return frequency;
    }
    public static void setFrequency(double newFrequency) {
        frequency = newFrequency;
    }
}
