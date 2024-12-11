package com.example.ml_vision_app;

import android.graphics.Canvas;
import android.graphics.Paint;

public class LineGraphic extends GraphicOverlay.Graphic {
    private final float startX;
    private final float startY;
    private final float endX;
    private final float endY;
    private final Paint linePaint;

    public LineGraphic(GraphicOverlay overlay, float startX, float startY, float endX, float endY, int color) {
        super(overlay);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;

        linePaint = new Paint();
        linePaint.setColor(color);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(8.0f); // Thickness of the line
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(translateX(startX), translateY(startY), translateX(endX), translateY(endY), linePaint);
    }
}


