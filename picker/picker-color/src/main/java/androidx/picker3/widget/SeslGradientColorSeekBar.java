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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.core.graphics.ColorUtils;
import androidx.picker.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

class SeslGradientColorSeekBar extends SeekBar {
    private static final String TAG = "SeslGradientColorSeekBar";
    private static final int SEEKBAR_MAX_VALUE = 100;
    private final Context mContext;
    private final Resources mResources;
    private int[] mColors = {Color.BLACK, Color.WHITE};
    private GradientDrawable mProgressDrawable;

    public SeslGradientColorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mProgressDrawable
                = (GradientDrawable) getContext().getDrawable(R.drawable.sesl_color_picker_gradient_seekbar_drawable);
        mContext = context;
        mResources = context.getResources();
    }

    void init(Integer color) {
        setMax(SEEKBAR_MAX_VALUE);

        if (color != null) {
            initColor(color);
        }

        initProgressDrawable();
        initThumb();
    }

    private void initColor(int color) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        final float value = hsv[2];
        hsv[2] = 1.0f;
        mColors[1] = Color.HSVToColor(hsv);

        setProgress(Math.round(value * getMax()));
    }

    void restoreColor(int color) {
        if (mProgressDrawable != null) {
            initColor(color);
            mProgressDrawable.setColors(mColors);
            setProgressDrawable(mProgressDrawable);
        }
    }

    void changeColorBase(int color) {
        if (mProgressDrawable != null) {
            color = ColorUtils.setAlphaComponent(color, 255);

            mColors[1] = color;
            mProgressDrawable.setColors(mColors);
            setProgressDrawable(mProgressDrawable);

            final float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);

            final float value = hsv[2];
            hsv[2] = 1.0f;
            mColors[1] = Color.HSVToColor(hsv);

            setProgress(Math.round(value * getMax()));
        }
    }

    private void initProgressDrawable() {
        setProgressDrawable(mProgressDrawable);
    }

    private void initThumb() {
        setThumb(getContext().getDrawable(R.drawable.sesl_color_picker_seekbar_cursor));
        setThumbOffset(0);
        setSplitTrack(false);
    }

    private static Drawable resizeDrawable(Context context, BitmapDrawable image,
                                           int width, int height) {
        if (image == null) {
            return null;
        }

        Bitmap bitmap = image.getBitmap();
        Matrix matrix = new Matrix();

        final float scaleWidth;
        final float scaleHeight;
        if (width > 0) {
            scaleWidth = width / bitmap.getWidth();
        } else {
            scaleWidth = 0.0f;
        }
        if (height > 0) {
            scaleHeight = height / bitmap.getHeight();
        } else {
            scaleHeight = 0.0f;
        }
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap
                = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return new BitmapDrawable(context.getResources(), resizedBitmap);
    }
}
