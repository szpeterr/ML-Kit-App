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

    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    private Button captureButton;
    private float[] maxPoint = new float[2]; // X, Y for max point
    private float[] minPoint = new float[2]; // X, Y for min point
    private boolean isMaxCaptured = false;
    private boolean isMinCaptured = false;

    private float segmentSize = 0.0f; // Size of the area accounted for one note. NEEDS AN OFFSET!
    private float inputImageHeight;
    private float inputImageWidth;
    private ImageButton switchCameraButton;

    private float imageHeight, imageWidth;

    private boolean calibrationCompleted = false;
    private boolean isCalibrating = false; // Indicates if calibration is in progress
    private float upperLineY; // Y-coordinate for the top of the middle zone
    private float lowerLineY; // Y-coordinate for the bottom of the middle zone
    private static final float SHRINK_PERCENT = 0.75f; //the scaled size of the segment


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        captureButton = findViewById(R.id.capture_button);
        switchCameraButton = findViewById(R.id.switch_camera_button);

        // Initialize pose detector
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        captureButton.setOnClickListener(v -> toggleCaptureMode());
        switchCameraButton.setOnClickListener(v -> toggleCamera());

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

    private void toggleCamera() {
        // Toggle between front and back cameras
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                ? CameraSelector.DEFAULT_BACK_CAMERA
                : CameraSelector.DEFAULT_FRONT_CAMERA;

        // Restart the camera with the updated selector
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll(); // Unbind the current camera
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

    @OptIn(markerClass = ExperimentalGetImage.class)
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
            inputImageHeight = image.getHeight();
            inputImageWidth = image.getWidth();
            imageHeight = graphicOverlay.getHeight();
            imageWidth = graphicOverlay.getWidth();

            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        drawPose(pose); // Draw the skeleton overlay
                    })
                    .addOnFailureListener(e -> e.printStackTrace())
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private void drawPose(Pose pose) {
        graphicOverlay.clear();
        graphicOverlay.add(
                new PoseGraphic(graphicOverlay, pose, offsetX, offsetY, inputImageHeight, inputImageWidth));
        graphicOverlay.invalidate(); // Redraw the overlay
        //Log.d(TAG, "drawPose: SegmentGraphics got added: " + graphicOverlay.getChildren().contains(segmentGraphic));
    }

    private void handlePose(Pose pose) {
        if (calibrationCompleted) return;

        PoseLandmark rightHand = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
        PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);

        graphicOverlay.clear();

        // Calculate scaling factors
        float scaleY = graphicOverlay.getHeight() / inputImageHeight;
        float scaleX = graphicOverlay.getWidth() / inputImageWidth;

        // Calculate line positions
        upperLineY = inputImageHeight * 0.4f * scaleY; // Top of the middle zone
        lowerLineY = inputImageHeight * 0.6f * scaleY; // Bottom of the middle zone

        // Draw the middle zone lines
        LineGraphic upperLine = new LineGraphic(graphicOverlay, 0, upperLineY, graphicOverlay.getWidth(), upperLineY, 0xFF00FF00); // Green
        LineGraphic lowerLine = new LineGraphic(graphicOverlay, 0, lowerLineY, graphicOverlay.getWidth(), lowerLineY, 0xFF00FF00); // Green
        graphicOverlay.add(upperLine);
        graphicOverlay.add(lowerLine);

        // Draw max and min points if captured
        if (isMaxCaptured) {
            LineGraphic maxLine = new LineGraphic(graphicOverlay, 0, maxPoint[1] * scaleY, graphicOverlay.getWidth(), maxPoint[1] * scaleY, 0xFFFF0000); // Red
            graphicOverlay.add(maxLine);
        }
        if (calibrationCompleted) {
            LineGraphic minLine = new LineGraphic(graphicOverlay, 0, minPoint[1] * scaleY, graphicOverlay.getWidth(), minPoint[1] * scaleY, 0xFF0000FF); // Blue
            graphicOverlay.add(minLine);
        }

        graphicOverlay.invalidate();

        if (rightHand == null || rightThumb == null || rightIndex == null) return;

        // Detect if hand is in the middle zone
        float handY = rightHand.getPosition().y * scaleY;
        boolean handInZone = handY > upperLineY && handY < lowerLineY;

        // Detect if hand forms a fist
        float thumbToIndexDistance = Math.abs(rightThumb.getPosition().x - rightIndex.getPosition().x) * scaleX;
        boolean isFist = thumbToIndexDistance < 20; // Adjust threshold as needed

        if (handInZone && isFist && !isCalibrating) {
            // Start calibration
            isCalibrating = true;
        }

        if (isCalibrating) {
            // Check if the fist rises above the top line
            if (handY < upperLineY && !isMaxCaptured) {
                maxPoint[0] = rightHand.getPosition().x;
                maxPoint[1] = rightHand.getPosition().y;
                isMaxCaptured = true;
            }

            // Check if the fist drops below the bottom line
            if (handY > lowerLineY && isMaxCaptured && !calibrationCompleted) {
                minPoint[0] = rightHand.getPosition().x;
                minPoint[1] = rightHand.getPosition().y;
                calibrationCompleted = true;

                // Calibration complete, send results
                finishCalibration();
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
