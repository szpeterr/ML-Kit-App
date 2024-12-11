package com.example.ml_vision_app;

import static android.content.ContentValues.TAG;

import static com.example.ml_vision_app.MainActivity.CALIBRATION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.billthefarmer.mididriver.MidiConstants;

//import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;


public class PoseDetectionActivity extends AppCompatActivity {

    private static final float SHRINK_PERCENT = 0.75f; //the scaled size of the segment
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private PoseDetector poseDetector;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    //private SoundPlayer soundPlayer;
    private float calibrationOffsetX = 0f;
    private float calibrationOffsetY = 0f;
    private float currentIndexFingerX = 0f;
    private float currentIndexFingerY = 0f;
    private float prevIndexFingerX = 0f;
    private float prevIndexFingerY = 0f;
    private long prevFrameTime = 0L;
    private float minDistance = 50.0f;
    private static float minSpeed = 50.0f;
    private float imageHeight;
    private float imageWidth;
    //private boolean canPlaySound = true;
    private int rawVelocity;
    private int convertedVelocity;
    private long lastSoundPlayedTime = 0L;
    private MidiHelper midiHelper;
    //int[] soundRes = {R.raw.a4, R.raw.b4, R.raw.c4, R.raw.d4, R.raw.e4, R.raw.f4, R.raw.g4};
    //Codes of notes and half notes
    static int[] soundCodes = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72};
    private static final int SEGNUM = soundCodes.length; // segment number
    private float segmentSize = 0.0f; // Size of the area accounted for one note. NEEDS AN OFFSET!
    private float inputImageHeight;
    private float inputImageWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose_detection);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        int instrumentId = sharedPreferences.getInt("instrumentId", 0);

        previewView = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        //private boolean isPlaying = false;

        ImageButton switchCameraButton = findViewById(R.id.switch_camera_button);


        //soundPlayer = new SoundPlayer(this);
        midiHelper = new MidiHelper();
        midiHelper.sendMidi(MidiConstants.PROGRAM_CHANGE, instrumentId);

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
    private void playNote(int id) {
        //Log.d(TAG, "playNote: sound volume (velocity) converted by converter is " + );
        //Log.d(TAG, "playNote: without convert it looks like this: " + velocity);
        Log.d(TAG, "playNote: the global velocity playNote will use is " + convertedVelocity);
        midiHelper.sendMidi(MidiConstants.NOTE_ON, soundCodes[id], convertedVelocity); // NOTE_ON, note, velocity 127
        long noteTimeMultiplier = 2L;
        new android.os.Handler().postDelayed(() -> midiHelper.sendMidi(MidiConstants.NOTE_OFF, soundCodes[id], 0), rawVelocity * noteTimeMultiplier); // NOTE_OFF
    }
//    private void drawNoteLabel(int id) {
//
//    }

    private void toggleCamera() {
        // Switch between front and back camera
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                ? CameraSelector.DEFAULT_BACK_CAMERA
                : CameraSelector.DEFAULT_FRONT_CAMERA;
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
    // Create buffer
    private Queue<Pose> frameBuffer = new LinkedList<>();
    private int frameCounter = 0;

    // Analyze each frame for pose detection
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            inputImageHeight = image.getHeight();
            inputImageWidth = image.getWidth();
            imageHeight = graphicOverlay.getHeight();
            imageWidth = graphicOverlay.getWidth();
            segmentSize = (imageHeight / SEGNUM) * SHRINK_PERCENT;

            // Process the image for pose detection
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {

                        // Buffer logic
                        frameCounter++;
                        if (frameCounter % 4 == 0) {
                            frameBuffer.offer(pose); // Add frame to buffer
                            if (frameBuffer.size() > 4) {
                                frameBuffer.poll(); // Remove oldest frame
                            }
                        }

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
                new PoseGraphic(graphicOverlay, pose, calibrationOffsetX, calibrationOffsetY, inputImageHeight, inputImageWidth));
        graphicOverlay.add(
                new SegmentGraphic(graphicOverlay, imageHeight * SHRINK_PERCENT, segmentSize, SEGNUM, inputImageHeight, this));
        graphicOverlay.invalidate(); // Redraw the overlay
        //Log.d(TAG, "drawPose: SegmentGraphics got added: " + graphicOverlay.getChildren().contains(segmentGraphic));
    }

//    public static int convertFloatToInt(float input) {
//        int intValue = (int) input;
//        return Math.min(intValue, 127);
//    }
public static int convertFloatToInt(float input) {
    //int intValue = (int) (input - minSpeed);
    int intValue = (int) input;
    Log.d(TAG, "convertFloatToInt: wunderbar reduced value is " + intValue);
    return Math.min(intValue, 127);
}
    private void checkFingerPositionAndPlaySound(Pose pose) {
        Log.d(TAG, "checkFingerPositionAndPlaySound: CHECK STARTS HERE");
        //float minSpeed = 3.2E-7f; // E = 10^-7
        float minSpeed = 50.0f;
        long minSoundDelay = 600L;
        // Getting index finger positions
        PoseLandmark rightIndexFinger = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        if (rightIndexFinger == null) {
            Log.d(TAG, "checkFingerPositionAndPlaySound: yepp, the finger is indeed null!");
            return;
        }
        Log.d(TAG, "checkFingerPositionAndPlaySound: at least the finger is not null");
        float scaledNoteFingerY = graphicOverlay.getHeight() / inputImageHeight * rightIndexFinger.getPosition().y; //RIGHT finger by default
        //THIS IS THE ONLY WORKING speed so far without convert
        float speed = currentFingerSpeed(pose); //LEFT finger by default
        Log.d(TAG, "checkFingerPositionAndPlaySound: speed before convert is " + speed);
        convertedVelocity = convertFloatToInt(speed);
        Log.d(TAG, "checkFingerPositionAndPlaySound: just to make sure. Velocity is now: " + convertedVelocity);
        //Log.d(TAG, "checkFingerPositionAndPlaySound: the smart unrounded value is " + speed);
        //Log.d(TAG, "checkFingerPositionAndPlaySound: the GPT version of the converter gives this wonderful number: " + convertFloatToInt(speed));
        //Log.d(TAG, "checkFingerPositionAndPlaySound: the stupid rounded number is no other than " + Math.round(currentFingerSpeed(pose)));

        // reset the sound when speed falls back to normal
        if (Math.abs(speed) < minSpeed) {
            //canPlaySound = true;
            Log.d(TAG, "checkFingerPositionAndPlaySound: " + "speed from the returner is " + speed);
            Log.d(TAG, "checkFingerPositionAndPlaySound: sound reset");
            return; // skip this cycle
        }
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < SEGNUM; i++) {
            // if finger is inside segment with offset applied
            Log.d(TAG, "checkFingerPositionAndPlaySound: i is " + i);
            //+ SHRINK_PERCENT * imageHeight
            //+ SHRINK_PERCENT * imageHeight
            if ((scaledNoteFingerY >= (i * segmentSize )) && (scaledNoteFingerY < ((i + 1) * segmentSize ))) {
                Log.d(TAG, "checkFingerPositionAndPlaySound: in zone " + i);
                if (currentTime - lastSoundPlayedTime >= minSoundDelay) { //canPlaySound &&
                    //int velocity = convertFloatToInt(currentFingerSpeed(pose));
                    //Log.d(TAG, "checkFingerPositionAndPlaySound: " + "velocity before passing is " + velocity);
//                    if (velocity > 127) velocity = 127;
                    playNote(SEGNUM - (i + 1)); // Play note for zone
                    lastSoundPlayedTime = currentTime;
                    //canPlaySound = false;
                    Log.d(TAG, "checkFingerPositionAndPlaySound: YOU SHOULD HAVE HEARD A SOUND");
                }
            }
        }
    }

    private float currentFingerSpeed(Pose pose) {
        //PoseLandmark leftIndexFinger = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        PoseLandmark rightIndexFinger = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        if (rightIndexFinger == null) {
            Log.d(TAG, "currentFingerSpeed: " + "yes, finger is indeed null!");
            return 0;
        }

        // Buffer logic starts -
        if (frameBuffer.size() < 4) {
            return 0; // Not enough frames in buffer
        }

        Pose latestFrame = frameBuffer.peek(); // Latest frame
        Pose thirdToLastFrame = (Pose) frameBuffer.toArray()[frameBuffer.size() - 4]; // 3rd-to-last frame
        // Buffer logic ends -

        currentIndexFingerX = rightIndexFinger.getPosition().x;
        currentIndexFingerY = rightIndexFinger.getPosition().y;

        // Get current frame timestamp
        long currentFrameTime = System.currentTimeMillis();

        // Calculate time difference
        float deltaTime = (currentFrameTime - prevFrameTime) / 1000f; // Convert to seconds

        // If this is the first frame, deltaTime will be invalid
        if (prevFrameTime == 0 || deltaTime == 0) {
            prevIndexFingerX = currentIndexFingerX;
            prevIndexFingerY = currentIndexFingerY;
            prevFrameTime = currentFrameTime;
            Log.d(TAG, "currentFingerSpeed: first frame!");
            return 0;
        }

        // Calculate displacement
        float displacementX = currentIndexFingerX - prevIndexFingerX;
        float displacementY = currentIndexFingerY - prevIndexFingerY;

        // Calculate speed
        float distance = (float) Math.sqrt(displacementX * displacementX + displacementY * displacementY);
        float speed = distance / deltaTime;

        // Update previous position and timestamp
        //if (deltaTime % 4 == 0) {
            prevIndexFingerX = currentIndexFingerX;
            prevIndexFingerY = currentIndexFingerY;
            prevFrameTime = currentFrameTime;
        //}

        //Log.d(TAG, "currentLeftFingerSpeed: " + "speed is " + speed);
        //Log.d(TAG, "currentLeftFingerSpeed: " + "moved distance is " + displacementX);
        //if (distance > minDistance) return speed;
        //return 0;
        //if (distance < minDistance) return 0;
        //return speed;
        rawVelocity = Math.round(distance);
        return distance; // finger travel distance between two frames
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseDetector != null) {
            poseDetector.close();
            //soundPlayer.release();
        }
        if (midiHelper != null) {
            midiHelper.stopMidi();
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
