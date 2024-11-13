package com.example.ml_vision_app;
import static android.content.ContentValues.TAG;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.Collections;

public class PoseGraphic extends GraphicOverlay.Graphic {
    private final Pose pose;
    private final Paint circlePaint;
    private final Paint linePaint;
    private static float leftIndexY;
    private static float rightIndexY;
    private static float leftIndexX;
    private static float rightIndexX;

    public PoseGraphic(GraphicOverlay overlay, Pose pose) {
        super(overlay);
        this.pose = pose;

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

    public static float getLeftIndexX() {
        return leftIndexX;
    }

    public static float getRightIndexX() {
        return rightIndexX;
    }

    @Override
    public void draw(Canvas canvas) {
        // Draw keypoints as circles
        for (PoseLandmark landmark : pose.getAllPoseLandmarks()) {
            float x = landmark.getPosition().x;
            float y = landmark.getPosition().y;
            Log.d(TAG, "Landmark coor: " + x + " " + y);
            canvas.drawCircle(x, y, 8f, circlePaint);
            if (landmark.getLandmarkType() == PoseLandmark.RIGHT_INDEX) rightIndexY = y;
            if (landmark.getLandmarkType() == PoseLandmark.LEFT_INDEX) leftIndexY = y;
            if (landmark.getLandmarkType() == PoseLandmark.RIGHT_INDEX) rightIndexX = x;
            if (landmark.getLandmarkType() == PoseLandmark.LEFT_INDEX) leftIndexX = x;

            // Calculate difference in Y coordinates
            //float fingerYDistance = Math.abs(leftIndexY - rightIndexY);

        }

        // Draw connections between keypoints
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER);
        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP);
        // You can add more lines based on the body part connections
    }

    private void drawLine(Canvas canvas, int startLandmark, int endLandmark) {
        PoseLandmark start = pose.getPoseLandmark(startLandmark);
        PoseLandmark end = pose.getPoseLandmark(endLandmark);
        if (start != null && end != null) {
            canvas.drawLine(start.getPosition().x, start.getPosition().y,
                    end.getPosition().x, end.getPosition().y, linePaint);
        }
    }

    public static float getRightIndexY() {
        return rightIndexY;
    }
    public static float getLeftIndexY() {
        return leftIndexY;
    }
}


