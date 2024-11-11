package com.example.ml_vision_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;


import java.util.concurrent.ExecutionException;

public class CalibrationActivity extends AppCompatActivity {
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    private float calibrationOffsetX = 0;
    private float calibrationOffsetY = 0;
    private boolean isCalibrated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);

        // Set up a button to complete calibration and return to MainActivity
        Button completeCalibrationButton = findViewById(R.id.complete_calibration_button);
        completeCalibrationButton.setOnClickListener(v -> completeCalibration());

        // Set up pose detection
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        startCamera();
    }

    private void startCamera() {
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        cameraProvider.unbindAll();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            Bitmap bitmap = previewView.getBitmap();
            if (bitmap != null) {
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                poseDetector.process(image)
                        .addOnSuccessListener(this::calculateOffsets)
                        .addOnFailureListener(Throwable::printStackTrace)
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        });

        cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
    }

    private void calculateOffsets(Pose pose) {
        if (!isCalibrated) {
            // Get star pose landmarks (example uses shoulders and hips)
            PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
            PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
            PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);

            if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
                // Calculate the average position to align the skeleton around the center
                float centerX = (leftShoulder.getPosition().x + rightShoulder.getPosition().x +
                        leftHip.getPosition().x + rightHip.getPosition().x) / 4;
                float centerY = (leftShoulder.getPosition().y + rightShoulder.getPosition().y +
                        leftHip.getPosition().y + rightHip.getPosition().y) / 4;

                // Calculate offsets (assuming the screen's center as the target alignment)
                calibrationOffsetX = previewView.getWidth() / 2 - centerX;
                calibrationOffsetY = previewView.getHeight() / 2 - centerY;

                Toast.makeText(this, "Calibration Complete!", Toast.LENGTH_SHORT).show();
                isCalibrated = true;  // Mark as calibrated to avoid re-calibration
            }
        }

        // Draw the current pose with calculated offsets
        graphicOverlay.clear();
        graphicOverlay.add(new PoseGraphic(graphicOverlay, pose, calibrationOffsetX, calibrationOffsetY));
        graphicOverlay.invalidate();
    }

    private void completeCalibration() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("calibrationOffsetX", calibrationOffsetX);
        resultIntent.putExtra("calibrationOffsetY", calibrationOffsetY);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        poseDetector.close();
    }
}
