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

    @Override
    public void draw(Canvas canvas) {
        // Draw keypoints as circles
        for (PoseLandmark landmark : pose.getAllPoseLandmarks()) {
            float x = landmark.getPosition().x;
            float y = landmark.getPosition().y;
            canvas.drawCircle(x, y, 8f, circlePaint);
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
}


