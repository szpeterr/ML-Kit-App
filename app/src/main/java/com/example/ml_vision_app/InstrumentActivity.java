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

//        h1Button.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        // Start playing the frequency
//                        if (!isPlaying) {
//                            playFrequency();
//                        }
//                        return true; // Indicate that the event was handled
//
//                    case MotionEvent.ACTION_UP:
//                    case MotionEvent.ACTION_CANCEL:
//                        // Stop playing the frequency
//                        stopFrequency();
//                        return true; // Indicate that the event was handled
//                }
//                return false;
//            }
//        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InstrumentActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        stopFrequency(); // Ensure the audio track is stopped and released
//    }
}