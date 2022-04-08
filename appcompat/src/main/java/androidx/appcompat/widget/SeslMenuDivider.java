/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.R;
import androidx.appcompat.util.SeslMisc;
import androidx.core.content.res.ResourcesCompat;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung MenuDivider class.
 */
public class SeslMenuDivider extends ImageView {
    private static final float CIRCLE_INTERVAL = 3.0f;
    private static final float DIAMETER_SIZE = 1.5f;

    private Paint mPaint;

    private final int mDiameter;
    private final int mInterval;

    public SeslMenuDivider(Context context) {
        this(context, null);
    }

    public SeslMenuDivider(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeslMenuDivider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mDiameter = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DIAMETER_SIZE, displayMetrics);
        mInterval = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CIRCLE_INTERVAL, displayMetrics);

        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(ResourcesCompat.getColor(context.getResources(), SeslMisc.isLightTheme(context) ?
                R.color.sesl_popup_menu_divider_color_light : R.color.sesl_popup_menu_divider_color_dark, null));
    }

    // TODO rework this method
    // kang
    @Override
    protected void onDraw(Canvas canvas) {
        int i;
        int i2;
        super.onDraw(canvas);
        int width = (getWidth() - getPaddingStart()) - getPaddingEnd();
        int height = getHeight();
        int i3 = this.mDiameter;
        int i4 = ((width - i3) / (this.mInterval + i3)) + 1;
        int i5 = i4 - 1;
        int paddingStart = ((int) ((i3 / 2.0f) + 0.5f)) + getPaddingStart();
        int i6 = this.mDiameter;
        int i7 = (width - i6) - ((this.mInterval + i6) * i5);
        if (i6 % 2 != 0) {
            i7--;
        }
        if (i5 > 0) {
            i = i7 / i5;
            i2 = i7 % i5;
        } else {
            i2 = 0;
            i = 0;
        }
        int i8 = 0;
        for (int i9 = 0; i9 < i4; i9++) {
            canvas.drawCircle(paddingStart + i8, height / 2, this.mDiameter / 2.0f, this.mPaint);
            i8 += this.mDiameter + this.mInterval + i;
            if (i9 < i2) {
                i8++;
            }
        }
    }
    // kang
}
