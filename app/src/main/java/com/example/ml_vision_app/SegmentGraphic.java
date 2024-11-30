package com.example.ml_vision_app;

import static android.content.ContentValues.TAG;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class SegmentGraphic extends GraphicOverlay.Graphic {
    private final Paint segmentLinePaint;
    private int _numberOfSegments;
    private float _yOffset;
    private float _segmentSize;
    private float _imageWidth;

    public SegmentGraphic(GraphicOverlay overlay, float yOffset, float imageWidth, float segmentSize, int numberOfSegments) {
        super(overlay);

        _numberOfSegments = numberOfSegments;
        _segmentSize = segmentSize;
        _yOffset = yOffset;
        segmentLinePaint = new Paint();
        segmentLinePaint.setColor(Color.BLACK);
        segmentLinePaint.setStyle(Paint.Style.STROKE);
        segmentLinePaint.setStrokeWidth(5f);

    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < _numberOfSegments; i++) {
            canvas.drawLine(0, (i + 1) * _segmentSize + _yOffset, _imageWidth, (i + 1) * _segmentSize + _yOffset, segmentLinePaint);
        }
    }
}
