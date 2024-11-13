package com.example.ml_vision_app;

import static android.content.ContentValues.TAG;
import static com.example.ml_vision_app.SoundGenerator.isFrequencyPlaying;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;

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

import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.concurrent.ExecutionException;


public class PoseDetectionActivity extends AppCompatActivity {

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    //private SoundGenerator soundGenerator;
    private int frameWidth;
    private int frameHeight;
    private BroadcastReceiver activityDestroyedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SoundGenerator.stopFrequency();
        }
    };
    //private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose_detection);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        //soundGenerator = new SoundGenerator();

        // Set up PoseDetector with STREAM_MODE
        PoseDetectorOptionsBase options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }

        previewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                frameWidth = right - left;
                frameHeight = bottom - top;

            }
        });
    }

    // Start the camera and bind to lifecycle
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, createPreview(), createImageAnalysis());
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
                .setTargetResolution(new Size(1280, 720)) // Set preferred resolution
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Keep only the latest frame
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
                    .addOnSuccessListener(pose -> drawPose(pose))
                    .addOnFailureListener(e -> e.printStackTrace())
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    // Draw the pose on the overlay
    private void drawPose(Pose pose) {
        graphicOverlay.clear();
        graphicOverlay.add(new PoseGraphic(graphicOverlay, pose));
        graphicOverlay.invalidate(); // Redraw the overlay
        // Calculate the difference in Y coordinates
        float fingerYDistance = Math.abs(PoseGraphic.getLeftIndexY() - PoseGraphic.getRightIndexY());
        Log.d(TAG, "playFrequency: " + (PoseGraphic.getLeftIndexY() - PoseGraphic.getRightIndexY()));

        // Apply threshold and adjust frequency
        if (fingerYDistance > 100) {
            double frequencyOffset = fingerYDistance * 0.1f;
            double newFrequency = SoundGenerator.getFrequency() + frequencyOffset;

            // Apply frequency limits
            double minFrequency = 200f; // Example minimum frequency
            double maxFrequency = 800f; // Example maximum frequency
            //newFrequency = Math.max(minFrequency, Math.min(maxFrequency, newFrequency));
            if (newFrequency < minFrequency) {
                newFrequency = minFrequency;
            } else if (newFrequency > maxFrequency) {
                newFrequency = maxFrequency;
            } else {
                SoundGenerator.setFrequency(newFrequency);
            }
        }
        checkForFrequencyStart();
    }

    private void checkForFrequencyStart() {
        //int playThreshold = 1000;
        if ((PoseGraphic.getRightIndexY() > PoseGraphic.getLeftIndexY()) &&
                !isLandmarkOutOfBounds(PoseGraphic.getRightIndexX(), PoseGraphic.getRightIndexY())) {
            if (!isLandmarkOutOfBounds(PoseGraphic.getLeftIndexX(), PoseGraphic.getLeftIndexY())) {
                if (!isFrequencyPlaying) {
                    SoundGenerator.playFrequency();
                    isFrequencyPlaying = true;
                }
            }
        }
    }

    private boolean isLandmarkOutOfBounds(float x, float y) {
        return x < 0 || x >= frameWidth || y < 0 || y >= frameHeight;
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

}