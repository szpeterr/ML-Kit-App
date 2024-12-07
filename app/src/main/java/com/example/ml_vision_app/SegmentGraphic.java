package com.example.ml_vision_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class SegmentGraphic extends GraphicOverlay.Graphic {
    private final Paint segmentLinePaint;
    private final Paint segmentTextPaint;
    private final int numberOfSegments;
    private final float yOffset;
    private final float segmentSize;
    //private final float imageWidth;
    private final float lineWidth;
    private final int textOffset = 0; //offset from the left edge of the screen
    static String[] soundNames = {"C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4",
            "A#4", "B4", "C5"}; // A "B" magyarban "H"
    private final float inputImageHeight;

    public SegmentGraphic(GraphicOverlay overlay, float yOffset, float segmentSize,
                          int numberOfSegments, float inputImageHeight, Context context) {
        super(overlay);

        this.numberOfSegments = numberOfSegments;
        this.segmentSize = segmentSize;
        this.yOffset = yOffset;
        this.inputImageHeight = inputImageHeight;
        lineWidth = (float) overlay.getWidth() / 3; // 1/3 of total width

        segmentLinePaint = new Paint();
        segmentLinePaint.setColor(Color.BLACK);
        segmentLinePaint.setStyle(Paint.Style.STROKE);
        segmentLinePaint.setStrokeWidth(5f);

        segmentTextPaint = new Paint();
        segmentTextPaint.setColor(Color.BLACK);
        segmentTextPaint.setTextSize(context.getResources().getDimension(R.dimen.text_size_medium));
        segmentTextPaint.setTextAlign(Paint.Align.LEFT);
        segmentTextPaint.setStyle(Paint.Style.FILL);
        segmentTextPaint.setStrokeWidth(5f);
        segmentTextPaint.setAntiAlias(true);

        //Log.d(TAG, "SegmentGraphic: initialized");
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < numberOfSegments; i++) {
            canvas.drawLine(0, (i + 1) * segmentSize, lineWidth, // + yOffset
                    (i + 1) * segmentSize, segmentLinePaint);
            canvas.drawText(soundNames[i], textOffset, (i + 1) * segmentSize, segmentTextPaint);
        }
    }
}
