package com.example.ml_vision_app;

import static android.content.ContentValues.TAG;
import static com.example.ml_vision_app.SoundGenerator.isFrequencyPlaying;

import static com.example.ml_vision_app.MainActivity.CALIBRATION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.Map;
//import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class PoseDetectionActivity extends AppCompatActivity {

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    //private boolean isPlaying = false;
    private ImageButton switchCameraButton;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private SoundPlayer soundPlayer;
    private float calibrationOffsetX = 0f;
    private float calibrationOffsetY = 0f;
    private float sectorSize = 0.0f; // Size of the area accounted for one note. NEEDS AN OFFSET!
    int[] soundRes = {R.raw.a4, R.raw.b4, R.raw.c4, R.raw.d4, R.raw.e4, R.raw.f4, R.raw.g4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose_detection);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        switchCameraButton = findViewById(R.id.switch_camera_button);


        soundPlayer = new SoundPlayer(this);

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
            sectorSize = (float) image.getHeight() / soundPlayer.numberOfSounds;
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
    }

    private void checkFingerPositionAndPlaySound(Pose pose) {
        // Getting index finger positions
        PoseLandmark rightIndexFinger = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        PoseLandmark leftIndexFinger = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);

        if (rightIndexFinger != null) {
            //float rightX = rightIndexFinger.getPosition().x;
            float rightY = rightIndexFinger.getPosition().y;
            //float leftX = leftIndexFinger.getPosition().x;
            float leftY = leftIndexFinger.getPosition().y;
            boolean canPlaySound = true;
            float minSpeed = 5.0E-7f; //5*10^-7
            long lastSoundPlayedTime = 0L; // Variable to store the last sound played time
            long minSoundDelay = 500L;

            for (int i = 0; i < soundPlayer.numberOfSounds; i++) {
                if (rightY >= i * sectorSize && rightY < (i + 1) * sectorSize) {
                    Log.d(TAG, "checkFingerPositionAndPlaySound: " + "in zone " + i);
                    if (currentLeftFingerSpeed(pose) > minSpeed && canPlaySound) {
                        Log.d(TAG, "checkFingerPositionAndPlaySound: speed is " + currentLeftFingerSpeed(pose));
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastSoundPlayedTime >= minSoundDelay) {
                            soundPlayer.playPianoSound(soundRes[i]);
                            lastSoundPlayedTime = currentTime;
                            canPlaySound = false;
                            Log.d(TAG, "checkFingerPositionAndPlaySound: " + "sound played");
                        }
                    } else {
                        canPlaySound = true;
                    }
                }
            }

        }
    }

    private float currentLeftFingerSpeed(Pose pose) {
        PoseLandmark leftIndexFinger = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        float prevIndexFingerX = 0f;
        float prevIndexFingerY = 0f;
        float currentIndexFingerX = 0f;
        float currentIndexFingerY = 0f;

        // Previous frame timestamp
        long prevFrameTime = 0L;

        // ... in your pose detection callback ...

        // Get current position of the left index finger
        currentIndexFingerX = leftIndexFinger.getPosition().x;
        currentIndexFingerY = leftIndexFinger.getPosition().y;

        // Get current frame timestamp
        long currentFrameTime = System.currentTimeMillis();

        // Calculate time difference
        float deltaTime = (currentFrameTime - prevFrameTime) / 1000f; // Convert to seconds

        // Calculate displacement
        float displacementX = currentIndexFingerX - prevIndexFingerX;
        float displacementY = currentIndexFingerY - prevIndexFingerY;

        // Calculate speed
        float speed = (float) Math.sqrt(displacementX * displacementX + displacementY * displacementY) / deltaTime;

        // ... use the speed value ...

        // Update previous position and timestamp
        prevIndexFingerX = currentIndexFingerX;
        prevIndexFingerY = currentIndexFingerY;
        prevFrameTime = currentFrameTime;
        Log.d(TAG, "currentLeftFingerSpeed: " + speed);
        return speed;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseDetector != null) {
            poseDetector.close();
            soundPlayer.release();
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
