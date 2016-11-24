package edu.stanford.ee368.cameratranslation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by francischen on 11/23/16.
 */

public class OcrGraphicPlain extends GraphicOverlay.Graphic {
    private int mId;

    private static final int TEXT_COLOR = Color.GREEN;

    private static Paint sTextPaint;
    private final String mText;

    OcrGraphicPlain(GraphicOverlay overlay, String text){
        super(overlay);

        mText = text;


        if (sTextPaint == null) {
            sTextPaint = new Paint();
            sTextPaint.setColor(TEXT_COLOR);
            sTextPaint.setTextSize(200.0f);
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public boolean contains(float x, float y) {
        return true;
    }
    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (mText == null) {
            return;
        }

        canvas.drawText(mText, 200, 600, sTextPaint);
    }
}
