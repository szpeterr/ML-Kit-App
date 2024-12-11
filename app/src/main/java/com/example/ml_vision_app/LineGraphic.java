package com.example.ml_vision_app;

import android.graphics.Canvas;
import android.graphics.Paint;

public class LineGraphic extends GraphicOverlay.Graphic {
    private final float start1X;
    private final float start1Y;
    private final float end1X;
    private final float end1Y;


    private final Paint linePaint;

    public LineGraphic(GraphicOverlay overlay, float start1X, float start1Y, float end1X, float end1Y, int color) {
        super(overlay);
        this.start1X = start1X;
        this.start1Y = start1Y;
        this.end1X = end1X;
        this.end1Y = end1Y;

        linePaint = new Paint();
        linePaint.setColor(color);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(8.0f); // Thickness of the line
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(translateX(-40), translateY(-40), translateX(40), translateY(40), linePaint);
        canvas.drawLine(translateX(-60), translateY(-60), translateX(40), translateY(0), linePaint);
    }
}


