package com.example.ml_vision_app;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.camera.core.CameraSelector;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {

    private final List<Graphic> graphics = new ArrayList<>();
    private float offsetX = 0;
    private float offsetY = 0;

    // Dimensions of the camera preview image
    private int imageWidth = 0;
    private int imageHeight = 0;

    // Scaling factors
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private CameraSelector cameraSelector;

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

    // Method to set the dimensions of the camera preview and calculate scale factors
    public void setCameraInfo(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        if (imageWidth > 0 && imageHeight > 0) {
            scaleX = (float) getWidth() / imageWidth;
            scaleY = (float) getHeight() / imageHeight;
        }

        invalidate();
    }

    // Getters for the camera preview dimensions
    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
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

    public void setCameraSelector(CameraSelector cameraSelector) {
        this.cameraSelector = cameraSelector;
    }

    // Base class for a custom graphic that you’ll subclass for specific drawings
    public abstract static class Graphic {
        private final GraphicOverlay overlay;

        protected Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        // Scale and translate x coordinate
        protected float translateX(float x) {
            if (overlay.getCameraSelector() == CameraSelector.DEFAULT_FRONT_CAMERA) {
                // Flip horizontally for the front camera
                return overlay.getWidth() - (x * overlay.getWidth()) + overlay.getOffsetX();
            } else {
                return x * overlay.getWidth() + overlay.getOffsetX();
            }
        }

        // Scale and translate y coordinate
        protected float translateY(float y) {
            return y * overlay.scaleY + overlay.getOffsetY();
        }

        // Get the overlay object (optional)
        public GraphicOverlay getOverlay() {
            return overlay;
        }

        // Abstract method to be implemented for drawing
        public abstract void draw(Canvas canvas);
    }

    private CameraSelector getCameraSelector() {
        return cameraSelector;
    }
}



