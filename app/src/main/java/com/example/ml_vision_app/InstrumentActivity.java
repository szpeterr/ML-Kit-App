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
    Button rockOrgan;

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
        rockOrgan = findViewById(R.id.rockOrgan);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("instrumentId", 0);
        editor.apply();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });
        pianoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInstrument(GeneralMidiConstants.ACOUSTIC_GRAND_PIANO);
                goBack();
            }
        });

        marimbaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInstrument(GeneralMidiConstants.MARIMBA);
                goBack();
            }
        });

        rockOrgan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInstrument(GeneralMidiConstants.ROCK_ORGAN);
                goBack();
            }
        });
    }

    private void saveInstrument(int newInstrumentId) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("instrumentId", newInstrumentId);
        editor.apply();
    }
    private void goBack() {
        Intent intent = new Intent(InstrumentActivity.this, MainActivity.class);
        startActivity(intent);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        stopFrequency(); // Ensure the audio track is stopped and released
//    }
}