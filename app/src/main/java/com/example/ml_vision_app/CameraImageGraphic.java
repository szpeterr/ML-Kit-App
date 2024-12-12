package com.example.ml_vision_app;
import android.graphics.Bitmap;
import android.graphics.Canvas;

/** Draw camera image to background. */
public class CameraImageGraphic extends GraphicOverlay {

    private final Bitmap bitmap;

    public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap) {
        super(overlay);
        this.bitmap = bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
    }
}
