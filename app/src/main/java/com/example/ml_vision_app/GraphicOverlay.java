package com.example.ml_vision_app;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {

    private final List<Graphic> graphics = new ArrayList<>();
    private float offsetX = 0;
    private float offsetY = 0;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Method to set offsets for calibration
    public void setOffsets(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        invalidate(); // Redraw to apply offsets
    }

    // Getters for offsets (if needed by other classes)
    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    // Adds a new graphic to be drawn
    public void add(Graphic graphic) {
        graphics.add(graphic);
        invalidate();
    }

    // Clears all graphics
    public void clear() {
        graphics.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Graphic graphic : graphics) {
            graphic.draw(canvas);
        }
    }

    // Base class for a custom graphic that you’ll subclass for specific drawings
    public abstract static class Graphic {
        private final GraphicOverlay overlay;

        protected Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        // Translate x coordinate with scaling and offsets
        protected float translateX(float x) {
            return x * overlay.getWidth() + overlay.getOffsetX();
        }

        // Translate y coordinate with scaling and offsets
        protected float translateY(float y) {
            return y * overlay.getHeight() + overlay.getOffsetY();
        }

        // Abstract method to be implemented for drawing
        public abstract void draw(Canvas canvas);
    }
}


