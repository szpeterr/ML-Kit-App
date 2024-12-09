package com.example.ml_vision_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
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

    private float offsetX;
    private float offsetY;
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    private Button captureButton;
    private float[] maxPoint = new float[2]; // X, Y for max point
    private float[] minPoint = new float[2]; // X, Y for min point
    private boolean isMaxCaptured = false;

    private float segmentSize = 0.0f; // Size of the area accounted for one note. NEEDS AN OFFSET!
    private float inputImageHeight;
    private float inputImageWidth;

    private boolean calibrationCompleted = false;

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

    @OptIn(markerClass = ExperimentalGetImage.class)
    private ImageAnalysis createImageAnalysis() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);
        return imageAnalysis;
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            inputImageWidth = mediaImage.getWidth();
            inputImageHeight = mediaImage.getHeight();

            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> handlePose(pose))
                    .addOnFailureListener(e -> e.printStackTrace())
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private void handlePose(Pose pose) {
        if (calibrationCompleted) return;

        PoseLandmark rightHand = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST); // Cue hand
        PoseLandmark leftHand = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST); // Target hand

        graphicOverlay.clear();

        if (rightHand != null && leftHand != null) {
            float cueY = rightHand.getPosition().y;
            float leftHandX = leftHand.getPosition().x;
            float leftHandY = leftHand.getPosition().y;

            // Draw the waistline for cue
            float waistLineY = inputImageHeight * 0.5f; // Example position around waist
            WaistLineGraphic waistlineGraphic = new WaistLineGraphic(graphicOverlay, 0, inputImageWidth, waistLineY);
            graphicOverlay.add(waistlineGraphic);
            graphicOverlay.invalidate();

            // If right hand is below the waistline, process the points
            if (cueY > waistLineY) {
                if (!isMaxCaptured) {
                    maxPoint[0] = leftHandX;
                    maxPoint[1] = leftHandY;
                    isMaxCaptured = true;

                    // Update UI to indicate max point capture
                    captureButton.setText("Capture Min Point");
                } else if (leftHandY < maxPoint[1]) { // Ensure min is not greater than max
                    minPoint[0] = leftHandX;
                    minPoint[1] = leftHandY;
                    calibrationCompleted = true;

                    // Send the results back and stop pose processing
                    finishCalibration();

                    // Update UI to indicate min point capture and stop cue function
                    captureButton.setText("Calibration Completed");
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
