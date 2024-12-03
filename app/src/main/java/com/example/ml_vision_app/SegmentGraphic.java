package com.example.ml_vision_app;

import static android.content.ContentValues.TAG;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class SegmentGraphic extends GraphicOverlay.Graphic {
    private final Paint segmentLinePaint;
    private final int _numberOfSegments;
    private final float _yOffset;
    private final float _segmentSize;
    private final float _lineWidth;

    public SegmentGraphic(GraphicOverlay overlay, float yOffset, float imageWidth, float segmentSize, int numberOfSegments) {
        super(overlay);

        _numberOfSegments = numberOfSegments;
        _segmentSize = segmentSize;
        _yOffset = yOffset;
        _lineWidth = imageWidth / 3; // 1/3 of total width
        segmentLinePaint = new Paint();
        segmentLinePaint.setColor(Color.BLACK);
        segmentLinePaint.setStyle(Paint.Style.STROKE);
        segmentLinePaint.setStrokeWidth(5f);
        Log.d(TAG, "SegmentGraphic: initialized");
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < _numberOfSegments; i++) {
            canvas.drawLine(0, (i + 1) * _segmentSize + _yOffset, _lineWidth, (i + 1) * _segmentSize + _yOffset, segmentLinePaint);
        }
    }
}
