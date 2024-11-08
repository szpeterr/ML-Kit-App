package com.example.ml_vision_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InstrumentActivity extends AppCompatActivity {

    Button h1Button;
    Button backButton;
    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    private final int sampleRate = 44100; // Sample rate in Hz
    private final double frequency = 440; // Frequency in Hz (A4 note)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_instrument);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        h1Button = findViewById(R.id.hangszer1);
        backButton = findViewById(R.id.backButton);

        h1Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start playing the frequency
                        if (!isPlaying) {
                            playFrequency();
                        }
                        return true; // Indicate that the event was handled

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Stop playing the frequency
                        stopFrequency();
                        return true; // Indicate that the event was handled
                }
                return false;
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InstrumentActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void playFrequency() {
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        audioTrack.play();
        isPlaying = true;

        new Thread(() -> {
            short[] buffer = new short[bufferSize];
            double angle = 0;

            while (isPlaying) {
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

    private void stopFrequency() {
        isPlaying = false;
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFrequency(); // Ensure the audio track is stopped and released
    }
}