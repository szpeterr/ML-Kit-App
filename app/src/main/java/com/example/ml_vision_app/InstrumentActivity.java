package com.example.ml_vision_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.billthefarmer.mididriver.MidiConstants;
import org.billthefarmer.mididriver.GeneralMidiConstants;

public class InstrumentActivity extends AppCompatActivity {

    Button pianoButton;
    Button marimbaButton;
    Button backButton;
    //public static int activeInstrument;

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

        pianoButton = findViewById(R.id.Piano);
        marimbaButton = findViewById(R.id.Marimba);
        backButton = findViewById(R.id.backButton);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("instrumentId", 0);
        editor.apply();

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
        pianoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInstrument(GeneralMidiConstants.ACOUSTIC_GRAND_PIANO);
            }
        });

        marimbaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInstrument(GeneralMidiConstants.MARIMBA);
            }
        });
    }

    private void saveInstrument(int newInstrumentId) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("instrumentId", newInstrumentId);
        editor.apply();
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        stopFrequency(); // Ensure the audio track is stopped and released
//    }
}