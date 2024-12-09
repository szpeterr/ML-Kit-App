package com.example.ml_vision_app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class WaistLineGraphic extends GraphicOverlay.Graphic {

    private final float lineStartX;
    private final float lineEndX;
    private final float lineY;
    private final Paint paint;

    public WaistLineGraphic(GraphicOverlay overlay, float startX, float endX, float y) {
        super(overlay);

        this.lineStartX = translateX(startX);
        this.lineEndX = translateX(endX);
        this.lineY = translateY(y);

        // Paint for drawing the waistline
        paint = new Paint();
        paint.setColor(Color.YELLOW); // Color for the waistline
        paint.setStrokeWidth(5); // Line thickness
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        // Draw the waistline on the canvas
        canvas.drawLine(lineStartX, lineY, lineEndX, lineY, paint);
    }
}

