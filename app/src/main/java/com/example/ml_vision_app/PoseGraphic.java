package com.example.ml_vision_app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

public class PoseGraphic extends GraphicOverlay.Graphic {

    private final Pose pose;
    private final Paint circlePaint;
    private final Paint linePaint;
    private final float offsetX;
    private final float offsetY;
    private static float rightIndexY;
    private final float inputImageWidth;
    private final float inputImageHeight;
    private float overlayWidth;
    private float overlayHeight;

    public PoseGraphic(GraphicOverlay overlay, Pose pose, float offsetX, float offsetY, float inputImageHeight, float inputImageWidth) {
        super(overlay);
        this.pose = pose;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.inputImageHeight = inputImageHeight;
        this.inputImageWidth = inputImageWidth;
        overlayWidth = overlay.getWidth();
        overlayHeight = overlay.getHeight();


        // Paint for drawing points
        circlePaint = new Paint();
        circlePaint.setColor(Color.RED);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setStrokeWidth(10f);

        // Paint for drawing lines
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
    }

    @Override
    public void draw(Canvas canvas) {

        // Draw keypoints as circles, applying the calibration offset to each point
        for (PoseLandmark landmark : pose.getAllPoseLandmarks()) {
            //float x = landmark.getPosition().x + offsetX;
            //float y = landmark.getPosition().y + offsetY;
            float x = (landmark.getPosition().x + offsetX) * (overlayWidth / inputImageWidth);
            float y = (landmark.getPosition().y + offsetY) * (overlayHeight / inputImageHeight);
            canvas.drawCircle(x, y, 8f, circlePaint);
            if (landmark.getLandmarkType() == PoseLandmark.RIGHT_INDEX) {
                rightIndexY = y;
            }
            //checkForSoundStart();
        }

        // Draw connections between keypoints, applying the offsets
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER);
        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP);
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW);
        drawLine(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST);
        drawLine(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW);
        drawLine(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST);
        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE);
        drawLine(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE);
        drawLine(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE);
        drawLine(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE);
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP);
        drawLine(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP);
    }

    private void drawLine(Canvas canvas, int startLandmark, int endLandmark) {
        // Check if both landmarks are present in the pose
        PoseLandmark start = pose.getPoseLandmark(startLandmark);
        PoseLandmark end = pose.getPoseLandmark(endLandmark);
        if (start != null && end != null) {
            // Draw a line between the two landmarks
            canvas.drawLine(
                    (start.getPosition().x + offsetX) * (overlayWidth / inputImageWidth),
                    (start.getPosition().y + offsetY) * (overlayHeight / inputImageHeight),
                    (end.getPosition().x + offsetX) * (overlayWidth / inputImageWidth),
                    (end.getPosition().y + offsetY) * (overlayHeight / inputImageHeight),
                    linePaint
            );
        }
    }

    public static float getRightIndexY() {
        return rightIndexY;
    }
}




