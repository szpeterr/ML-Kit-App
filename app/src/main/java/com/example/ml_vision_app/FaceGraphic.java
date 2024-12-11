package com.example.ml_vision_app;

import static com.google.android.material.internal.ViewUtils.getOverlay;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

public class FaceGraphic extends GraphicOverlay.Graphic {

    private final Face face;
    private float offsetX = 0;
    private float offsetY = 0;
    private float overlayWidth;
    private float overlayHeight;
    private final float inputImageWidth;
    private final float inputImageHeight;
    private final Paint facePositionPaint;
    private final Paint landmarkPaint;
    private final Paint boxPaint;

    private GraphicOverlay overlay;

    public FaceGraphic(GraphicOverlay overlay, Face face, float offsetX, float offsetY, float inputImageHeight, float inputImageWidth) {
        super(overlay);
        this.face = face;

        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.inputImageHeight = inputImageHeight;
        this.inputImageWidth = inputImageWidth;
        overlayWidth = overlay.getWidth();
        overlayHeight = overlay.getHeight();

        facePositionPaint = new Paint();
        facePositionPaint.setColor(Color.BLUE);
        facePositionPaint.setTextSize(36.0f);

        landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(4.0f);

        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5.0f);
    }

    @Override
    public void draw(Canvas canvas) {
        if (face == null) {
            return;
        }

        // Draw bounding box around the face
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
        float width = scale(face.getBoundingBox().width());
        float height = scale(face.getBoundingBox().height());

        float left = x - width / 2;
        float top = y - height / 2;
        float right = x + width / 2;
        float bottom = y + height / 2;

        canvas.drawRect(left, top, right, bottom, boxPaint);

        // Draw landmarks on the face
        for (FaceLandmark landmark : face.getAllLandmarks()) {
            PointF position = landmark.getPosition();
            if (position != null) {
                float landmarkX = translateX(position.x);
                float landmarkY = translateY(position.y);
                canvas.drawCircle(landmarkX, landmarkY, 10.0f, landmarkPaint);
            }
        }

        // Optionally, draw additional information such as face ID
        if (face.getTrackingId() != null) {
            canvas.drawText("ID: " + face.getTrackingId(), x, y - height / 2 - 10, facePositionPaint);
        }
    }

    // Scales the given value from image pixels to overlay pixels
    protected float scale(float imagePixel) {
        float overlayWidth = getOverlay().getWidth();
        float imageWidth = getOverlay().getImageWidth(); // Replace this with actual logic to get image width
        return imagePixel * overlayWidth / imageWidth;
    }
}


