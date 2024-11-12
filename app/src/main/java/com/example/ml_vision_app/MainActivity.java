package com.example.ml_vision_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    public ImageButton FaceDet;
    public ImageButton PoseDet;
    public ImageButton MusicDet;
    public ImageButton Calibration;

    public static final int CALIBRATION_REQUEST_CODE = 1001;
    private float calibrationOffsetX = 0;
    private float calibrationOffsetY = 0;
    // Start CalibrationActivity
    private void startCalibration() {
        Intent calibrationIntent = new Intent(MainActivity.this, CalibrationActivity.class);
        startActivityForResult(calibrationIntent, CALIBRATION_REQUEST_CODE);
    }

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

        FaceDet = findViewById(R.id.FaceDetection);
        PoseDet = findViewById(R.id.PoseDetection);
        Calibration = findViewById(R.id.Calibration);

        FaceDet.setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, FaceDetectionActivity.class);
            startActivity(myIntent);
        });


        PoseDet.setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, PoseDetectionActivity.class);
            startActivity(myIntent);
        });

        Calibration.setOnClickListener(v -> startCalibration());

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CALIBRATION_REQUEST_CODE && resultCode == RESULT_OK) {
            calibrationOffsetX = data.getFloatExtra("calibrationOffsetX", 0);
            calibrationOffsetY = data.getFloatExtra("calibrationOffsetY", 0);
        }
    }
}