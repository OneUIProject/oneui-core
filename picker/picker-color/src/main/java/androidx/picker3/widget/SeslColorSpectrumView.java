/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.picker3.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import androidx.picker.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

class SeslColorSpectrumView extends View {
    private static final String TAG = "SeslColorSpectrumView";

    private static final float STROKE_WIDTH = 2.0f;
    private static final int ROUNDED_CORNER_RADIUS = 4;

    private Drawable cursorDrawable;
    private final Context mContext;
    private Paint mCursorPaint;
    private Paint mHuePaint;
    private SpectrumColorChangedListener mListener;
    private final Resources mResources;
    private Paint mSaturationPaint;
    private Rect mSpectrumRect;
    private Paint mStrokePaint;

    private float mCursorPosX;
    private float mCursorPosY;

    private int ROUNDED_CORNER_RADIUS_IN_Px = 0;
    private final int mCursorPaintSize;
    private final int mCursorStrokeSize;

    private final int[] HUE_COLORS = {
            -65281, -16776961, -16711681, -16711936, -256, -65536
    };

    interface SpectrumColorChangedListener {
        void onSpectrumColorChanged(float hue, float saturation);
    }

    void setOnSpectrumColorChangedListener(SpectrumColorChangedListener listener) {
        mListener = listener;
    }

    public SeslColorSpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResources = context.getResources();

        mSpectrumRect = new Rect(0, 0,
                mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_oneui_3_color_swatch_view_width),
                mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_oneui_3_color_spectrum_view_height));
        mCursorPaintSize = mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_spectrum_cursor_paint_size);
        mCursorStrokeSize = mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_spectrum_cursor_paint_size)
                + (mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_spectrum_cursor_out_stroke_size) * (int) STROKE_WIDTH);
        ROUNDED_CORNER_RADIUS_IN_Px = dpToPx(ROUNDED_CORNER_RADIUS);

        init();
    }

    private void init() {
        mCursorPaint = new Paint();

        mStrokePaint = new Paint();
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(mResources.getColor(R.color.sesl_color_picker_stroke_color_spectrumview));
        mStrokePaint.setStrokeWidth(STROKE_WIDTH);

        cursorDrawable = mResources.getDrawable(R.drawable.sesl_color_picker_gradient_wheel_cursor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float distance
                = (float) Math.sqrt(Math.pow(event.getX(), 2.0d) + Math.pow(event.getY(), 2.0d));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                playSoundEffect(SoundEffectConstants.CLICK);
                break;
            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
        }

        float posX = event.getX();
        float posY = event.getY();

        if (posX > mSpectrumRect.width()) {
            posX = mSpectrumRect.width();
        }
        if (posY > mSpectrumRect.height()) {
            posY = mSpectrumRect.height();
        }
        if (posX < 0.0f) {
            posX = 0.0f;
        }
        if (posY <= 7.0f) {
            posY = 7.0f;
        }

        mCursorPosX = posX;
        mCursorPosY = posY;

        final float hue = ((posX - mSpectrumRect.left) / mSpectrumRect.width()) * 300.0f;
        final float saturation = (mCursorPosY - mSpectrumRect.top) / mSpectrumRect.height();

        final float[] hsv = new float[3];
        hsv[0] = hue >= 0 ? hue : 0;
        hsv[1] = saturation;

        if (mListener != null) {
            mListener.onSpectrumColorChanged(hsv[0], hsv[1]);
        } else {
            Log.d(TAG, "Listener is not set.");
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mSpectrumRect = canvas.getClipBounds();

        mHuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHuePaint.setShader(
                new LinearGradient((float) mSpectrumRect.right,
                        (float) mSpectrumRect.top,
                        (float) mSpectrumRect.left,
                        (float) mSpectrumRect.top,
                        HUE_COLORS,
                        null,
                        Shader.TileMode.CLAMP)
        );
        mHuePaint.setStyle(Paint.Style.FILL);

        mSaturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSaturationPaint.setShader(
                new LinearGradient((float) mSpectrumRect.left,
                        (float) mSpectrumRect.top,
                        (float) mSpectrumRect.left,
                        (float) mSpectrumRect.bottom,
                        Color.WHITE,
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP)
        );

        canvas.drawRoundRect(mSpectrumRect.left,
                mSpectrumRect.top,
                mSpectrumRect.right,
                mSpectrumRect.bottom,
                ROUNDED_CORNER_RADIUS_IN_Px,
                ROUNDED_CORNER_RADIUS_IN_Px,
                mHuePaint);
        canvas.drawRoundRect(mSpectrumRect.left,
                mSpectrumRect.top,
                mSpectrumRect.right,
                mSpectrumRect.bottom,
                ROUNDED_CORNER_RADIUS_IN_Px,
                ROUNDED_CORNER_RADIUS_IN_Px,
                mSaturationPaint);
        canvas.drawRoundRect(mSpectrumRect.left,
                mSpectrumRect.top,
                mSpectrumRect.right,
                mSpectrumRect.bottom,
                ROUNDED_CORNER_RADIUS_IN_Px,
                ROUNDED_CORNER_RADIUS_IN_Px,
                mStrokePaint);

        if (mCursorPosX < mSpectrumRect.left) {
            mCursorPosX = mSpectrumRect.left;
        }
        if (mCursorPosY <= mSpectrumRect.top + 7) {
            mCursorPosY = mSpectrumRect.top + 7;
        }
        if (mCursorPosX > mSpectrumRect.right) {
            mCursorPosX = mSpectrumRect.right;
        }
        if (mCursorPosY > mSpectrumRect.bottom) {
            mCursorPosY = mSpectrumRect.bottom;
        }

        canvas.drawCircle(mCursorPosX, mCursorPosY, mCursorPaintSize / STROKE_WIDTH, mCursorPaint);

        cursorDrawable.setBounds(((int) mCursorPosX) - (mCursorPaintSize / 2),
                ((int) mCursorPosY) - (mCursorPaintSize / 2),
                ((int) mCursorPosX) + (mCursorPaintSize / 2),
                ((int) mCursorPosY) + (mCursorPaintSize / 2));
        cursorDrawable.draw(canvas);

        setDrawingCacheEnabled(true);
    }

    void setColor(int color) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        updateCursorPosition(color, hsv);
    }

    public void updateCursorPosition(int color, float[] hsv) {
        if (mSpectrumRect != null) {
            mCursorPosX = mSpectrumRect.left + ((mSpectrumRect.width() * hsv[0]) / 300.0f);
            mCursorPosY = mSpectrumRect.top + (mSpectrumRect.height() * hsv[1]);
            Log.d(TAG, "updateCursorPosition() " +
                    "HSV[" + hsv[0] + ", " + hsv[1] + ", " + hsv[1]
                    + "] mCursorPosX=" + mCursorPosX + " mCursorPosY=" + mCursorPosY);
        }
        invalidate();
    }

    void updateCursorColor(int color) {
        Log.i(TAG, "updateCursorColor color " + color);
        mCursorPaint.setColor(color);
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
