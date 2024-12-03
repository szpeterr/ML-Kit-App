package com.example.ml_vision_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
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
    private Button captureButton;
    private float[] maxPoint = new float[2]; // X, Y for max point
    private float[] minPoint = new float[2]; // X, Y for min point
    private boolean isMaxCaptured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        captureButton = findViewById(R.id.capture_button);

        // Initialize pose detector
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        captureButton.setOnClickListener(v -> toggleCaptureMode());

        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }
    }

    private void toggleCaptureMode() {
        if (isMaxCaptured) {
            // Min capture mode
            captureButton.setText("Capture Min Point");
        } else {
            // Max capture mode
            captureButton.setText("Capture Max Point");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, createPreview(), createImageAnalysis());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Preview createPreview() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        return preview;
    }

    private ImageAnalysis createImageAnalysis() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);
        return imageAnalysis;
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        @android.annotation.SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            poseDetector.process(image)
                    .addOnSuccessListener(this::handlePose)
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private void handlePose(Pose pose) {
        PoseLandmark cueHand = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST); // Cue hand
        PoseLandmark targetFinger = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX); // Target hand index finger

        if (cueHand != null && targetFinger != null) {
            // Check if cue hand is raised
            if (cueHand.getPosition().y < graphicOverlay.getHeight() / 4) { // Cue position threshold
                if (isMaxCaptured) {
                    // Capture Min Point
                    minPoint[0] = targetFinger.getPosition().x;
                    minPoint[1] = targetFinger.getPosition().y;
                    finishCalibration();
                } else {
                    // Capture Max Point
                    maxPoint[0] = targetFinger.getPosition().x;
                    maxPoint[1] = targetFinger.getPosition().y;
                    isMaxCaptured = true;
                    toggleCaptureMode();
                }
            }
        }
    }

    private void finishCalibration() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("maxPoint", maxPoint);
        resultIntent.putExtra("minPoint", minPoint);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseDetector != null) {
            poseDetector.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}
