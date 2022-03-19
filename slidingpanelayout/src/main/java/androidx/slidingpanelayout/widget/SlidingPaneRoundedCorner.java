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

package androidx.slidingpanelayout.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.util.SeslMisc;
import androidx.core.view.ViewCompat;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RequiresApi(23)
public class SlidingPaneRoundedCorner {
    static final String TAG = "SeslPaneRoundedCorner";

    private static final int RADIUS = 15;

    private Context mContext;
    private Resources mRes;

    private Rect mRoundedCornerBounds = new Rect();
    private final Rect mTmpRect = new Rect();

    private Drawable mStartTopDrawable;
    private Drawable mStartBottomDrawable;
    private Drawable mEndTopDrawable;
    private Drawable mEndBottomDrawable;

    @ColorInt
    private int mStartTopDrawableColor;
    @ColorInt
    private int mStartBottomDrawableColor;

    private int mRoundedCornerMode;
    private int mRoundRadius = -1;

    private int mMarginTop = 0;
    private int mMarginBottom = 0;

    SlidingPaneRoundedCorner(Context context) {
        mContext = context;
        mRes = context.getResources();
        initRoundedCorner();
    }

    void setMarginTop(int marginTop) {
        mMarginTop = marginTop;
    }

    void setMarginBottom(int marginBottom) {
        mMarginBottom = marginBottom;
    }

    public void setRoundedCorners(int corners) {
        mRoundedCornerMode = corners;
        if (mStartTopDrawable == null || mStartBottomDrawable == null
                || mEndTopDrawable == null || mEndBottomDrawable == null) {
            initRoundedCorner();
        }
    }

    public int getRoundedCorners() {
        return mRoundedCornerMode;
    }

    public void setRoundedCornerColor(int corners, @ColorInt int color) {
        if (mStartTopDrawable == null || mStartBottomDrawable == null
                || mEndTopDrawable == null || mEndBottomDrawable == null) {
            initRoundedCorner();
        }

        PorterDuffColorFilter colorFilter
                = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        mStartTopDrawableColor = color;
        mStartTopDrawable.setColorFilter(colorFilter);
        mEndTopDrawable.setColorFilter(colorFilter);
        mEndBottomDrawable.setColorFilter(colorFilter);
        mStartBottomDrawableColor = color;
        mStartBottomDrawable.setColorFilter(colorFilter);
    }

    @RequiresApi(23)
    private void initRoundedCorner() {
        mRoundRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RADIUS,
                mRes.getDisplayMetrics());

        mStartTopDrawable = mRes.getDrawable(R.drawable.sesl_top_right_round, mContext.getTheme());
        mStartBottomDrawable = mRes.getDrawable(R.drawable.sesl_bottom_right_round, mContext.getTheme());
        mEndTopDrawable = mRes.getDrawable(R.drawable.sesl_top_left_round, mContext.getTheme());
        mEndBottomDrawable = mRes.getDrawable(R.drawable.sesl_bottom_left_round, mContext.getTheme());

        if (!SeslMisc.isLightTheme(mContext)) {
            final int color = mRes.getColor(R.color.sesl_round_and_bgcolor_dark, null);
            mStartBottomDrawableColor = color;
            mStartTopDrawableColor = color;
        } else {
            final int color = mRes.getColor(R.color.sesl_round_and_bgcolor_light, null);
            mStartBottomDrawableColor = color;
            mStartTopDrawableColor = color;
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public int getRoundedCornerRadius() {
        return mRoundRadius;
    }

    private void removeRoundedCorner(int corner) {
        if (corner == 0) {
            mStartTopDrawable = null;
            mStartBottomDrawable = null;
        } else {
            mEndTopDrawable = null;
            mEndBottomDrawable = null;
        }
    }

    public void drawRoundedCorner(Canvas canvas) {
        canvas.getClipBounds(mRoundedCornerBounds);
        drawRoundedCornerInternal(canvas);
    }

    public void drawRoundedCorner(View view, Canvas canvas) {
        if (isLayoutRtlSupport(view)) {
            mRoundedCornerMode = 1;
        } else {
            mRoundedCornerMode = 0;
        }

        final int x;
        final int y;
        if (view.getTranslationY() != 0) {
            x = Math.round(view.getX());
            y = Math.round(view.getY());
        } else {
            x = view.getLeft();
            y = view.getTop();
        }

        final int left = x;
        final int top = mMarginTop + y;
        final int right = view.getWidth() + x + mRoundRadius;
        final int bottom = y + view.getHeight() - mMarginBottom;

        canvas.getClipBounds(mTmpRect);
        mTmpRect.right = Math.max(mTmpRect.left, view.getRight() + mRoundRadius);
        canvas.clipRect(mTmpRect);

        mRoundedCornerBounds.set(left, top, right, bottom);
        drawRoundedCornerInternal(canvas);
    }

    private void drawRoundedCornerInternal(Canvas canvas) {
        final int left = mRoundedCornerBounds.left;
        final int right = mRoundedCornerBounds.right;
        final int top = mRoundedCornerBounds.top;
        final int bottom = mRoundedCornerBounds.bottom;

        if (mRoundedCornerMode == 0) {
            mStartTopDrawable.setBounds(left - mRoundRadius, top,
                    left, mRoundRadius + top);
            mStartTopDrawable.draw(canvas);

            mStartBottomDrawable.setBounds(left - mRoundRadius,
                    bottom - mRoundRadius, left, bottom);
            mStartBottomDrawable.draw(canvas);
        } else {
            mEndTopDrawable.setBounds(right - mRoundRadius, top,
                    right, mRoundRadius + top);
            mEndTopDrawable.draw(canvas);

            mEndBottomDrawable.setBounds(right - mRoundRadius,
                    bottom - mRoundRadius, right, bottom);
            mEndBottomDrawable.draw(canvas);
        }
    }

    private boolean isLayoutRtlSupport(View view) {
        return ViewCompat.getLayoutDirection(view)
                == ViewCompat.LAYOUT_DIRECTION_RTL;
    }
}
