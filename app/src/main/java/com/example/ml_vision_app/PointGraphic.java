package com.example.ml_vision_app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class PointGraphic extends GraphicOverlay.Graphic {

    private final float pointX;
    private final float pointY;
    private final String label;
    private final Paint circlePaint;
    private final Paint textPaint;

    public PointGraphic(GraphicOverlay overlay, float x, float y, String label) {
        super(overlay);

        this.pointX = x;
        this.pointY = y;
        this.label = label;

        // Paint for drawing the point
        circlePaint = new Paint();
        circlePaint.setColor(Color.RED);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);

        // Paint for drawing the label
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas) {
        // Translate point coordinates to overlay coordinates
        float canvasX = translateX(pointX);
        float canvasY = translateY(pointY);

        // Draw the point as a circle
        canvas.drawCircle(canvasX, canvasY, 10, circlePaint);

        // Draw the label next to the point
        canvas.drawText(label, canvasX + 15, canvasY, textPaint);
    }
}

