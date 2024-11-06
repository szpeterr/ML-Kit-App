package com.example.ml_vision_app;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {

    private final List<Graphic> graphics = new ArrayList<>();

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Adds a new graphic to be drawn
    public void add(Graphic graphic) {
        graphics.add(graphic);
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

        public abstract void draw(Canvas canvas);
    }
}

