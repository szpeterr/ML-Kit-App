package com.example.ml_vision_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    MaterialCardView cardFaceDetection;
    MaterialCardView cardPoseDetection;
    MaterialCardView cardMusic;
    //MaterialCardView cardSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        cardFaceDetection = findViewById(R.id.cardFaceDetection);
        cardPoseDetection = findViewById(R.id.cardPoseDetection);
        cardMusic = findViewById(R.id.cardMusic);

        cardFaceDetection.setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, FaceDetectionActivity.class);
            startActivity(myIntent);
        });
        cardPoseDetection.setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, PoseDetectionActivity.class);
            startActivity(myIntent);
        });
        cardMusic.setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, InstrumentActivity.class);
            startActivity(myIntent);
        });
    }
}