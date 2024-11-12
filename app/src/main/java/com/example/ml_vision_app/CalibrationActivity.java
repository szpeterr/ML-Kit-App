package com.example.ml_vision_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

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
import com.google.common.util.concurrent.MoreExecutors;
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
    private Button completeCalibrationButton;
    private ImageButton switchCameraButton;
    private float calibrationOffsetX = 0f;
    private float calibrationOffsetY = 0f;
    private boolean calibrationComplete = false;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA; // Start with back camera

    private float calculateOffsetX(Pose pose) {
        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        float expectedCenterX = graphicOverlay.getWidth() / 2f; // Center X of overlay
        return nose != null ? (expectedCenterX - nose.getPosition().x) : 0f;
    }

    private float calculateOffsetY(Pose pose) {
        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        float expectedCenterY = graphicOverlay.getHeight() / 2f; // Center Y of overlay
        return nose != null ? (expectedCenterY - nose.getPosition().y) : 0f;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        completeCalibrationButton = findViewById(R.id.complete_calibration_button);
        switchCameraButton = findViewById(R.id.switch_camera_button);

        // Setup PoseDetector for calibration
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        completeCalibrationButton.setOnClickListener(v -> finishCalibration());
        switchCameraButton.setOnClickListener(v -> toggleCamera());

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }
    }

    private void toggleCamera() {
        // Switch between front and back camera
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera(); // Restart camera with the new selector
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll(); // Unbind previous use cases
                cameraProvider.bindToLifecycle(this, cameraSelector, createPreview(), createImageAnalysis());
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
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        calibratePose(pose);   // Run calibration logic
                        drawPose(pose);        // Draw the skeleton overlay
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private void drawPose(Pose pose) {
        graphicOverlay.clear();
        graphicOverlay.add(
                new PoseGraphic(graphicOverlay, pose, graphicOverlay.getOffsetX(), graphicOverlay.getOffsetY()));
        graphicOverlay.invalidate(); // Redraw the overlay
    }

    private void calibratePose(Pose pose) {
        if (isStarPose(pose)) {
            calibrationOffsetX = calculateOffsetX(pose);
            calibrationOffsetY = calculateOffsetY(pose);

            // Apply offsets to GraphicOverlay
            graphicOverlay.setOffsets(calibrationOffsetX, calibrationOffsetY);
            calibrationComplete = true;
            completeCalibrationButton.setVisibility(View.VISIBLE);
        }
    }


    private boolean isStarPose(Pose pose) {
        return true; // Placeholder
    }


    private void finishCalibration() {
        if (calibrationComplete) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("calibrationOffsetX", calibrationOffsetX);
            resultIntent.putExtra("calibrationOffsetY", calibrationOffsetY);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
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
