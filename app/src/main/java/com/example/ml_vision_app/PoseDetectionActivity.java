package com.example.ml_vision_app;

import static com.example.ml_vision_app.SoundGenerator.isFrequencyPlaying;

import static com.example.ml_vision_app.MainActivity.CALIBRATION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageButton;
import android.widget.Toast;

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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;

import android.util.Size;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.concurrent.ExecutionException;


public class PoseDetectionActivity extends AppCompatActivity {

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    //private boolean isPlaying = false;
    private ImageButton switchCameraButton;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private float calibrationOffsetX = 0f;
    private float calibrationOffsetY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose_detection);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        switchCameraButton = findViewById(R.id.switch_camera_button);

        // Initialize PoseDetector with STREAM_MODE
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        switchCameraButton.setOnClickListener(v -> toggleCamera());

        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }
    }

    private void toggleCamera() {
        // Switch between front and back camera
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera(); // Restart camera with the new selector
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, createPreview(), createImageAnalysis());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Create camera preview
    private Preview createPreview() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        return preview;
    }

    // Create image analysis for pose detection
    private ImageAnalysis createImageAnalysis() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                //.setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);
        return imageAnalysis;
    }

    // Analyze each frame for pose detection
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // Process the image for pose detection
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        drawPose(pose); // Draw the skeleton overlay
                        checkFingerPositionAndPlaySound(pose); // Check finger position and play sound
                    })
                    .addOnFailureListener(e -> e.printStackTrace())
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    // Draw the pose on the overlay
    private void drawPose(Pose pose) {
        graphicOverlay.clear();
        graphicOverlay.add(
                new PoseGraphic(graphicOverlay, pose, calibrationOffsetX, calibrationOffsetY));
        graphicOverlay.invalidate(); // Redraw the overlay
        checkForFrequencyStart();
    }

    private void checkFingerPositionAndPlaySound(Pose pose) {
        // Assuming you want to track the right index finger
        PoseLandmark indexFinger = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);

        if (indexFinger != null) {
            float x = indexFinger.getPosition().x;
            float y = indexFinger.getPosition().y;

            if (y < 300) { // Example condition: play sound when finger's y-position is above 300
                if (!SoundGenerator.isFrequencyPlaying) {
                    SoundGenerator.playFrequency();
                }
            } else {
                if (SoundGenerator.isFrequencyPlaying) {
                    SoundGenerator.stopFrequency();
                }
            }
        }
    }

    private void checkForFrequencyStart() {
        int playThreshold = 1000;
        if (PoseGraphic.getRightIndexY() > playThreshold) {
            if (!isFrequencyPlaying) {
                SoundGenerator.playFrequency();
                isFrequencyPlaying = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseDetector != null) {
            poseDetector.close();
            SoundGenerator.stopFrequency();
        }
    }

    // Handle camera permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CALIBRATION_REQUEST_CODE && resultCode == RESULT_OK) {
            calibrationOffsetX = data.getFloatExtra("calibrationOffsetX", 0f);
            calibrationOffsetY = data.getFloatExtra("calibrationOffsetY", 0f);
            graphicOverlay.setOffsets(calibrationOffsetX, calibrationOffsetY);
        }
    }
}
