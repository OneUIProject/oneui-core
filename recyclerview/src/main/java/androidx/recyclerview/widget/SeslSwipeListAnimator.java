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

package androidx.recyclerview.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung SwipeListAnimator class.
 */
public class SeslSwipeListAnimator {
    private static final String TAG = "SeslSwipeListAnimator";

    private final int DIRECTION_LTR = 0;
    private final int DIRECTION_RTL = 1;

    private final int DEFAULT_DRAWABLE_PADDING = 10;
    private final int DEFAULT_TEXT_SIZE = 15;

    private final int DEFAULT_LEFT_COLOR = Color.parseColor("#6ebd52");
    private final int DEFAULT_RIGHT_COLOR = Color.parseColor("#56c0e5");
    private final int DEFAULT_TEXT_COLOR = Color.parseColor("#ffffff");

    private Paint mBgLeftToRight = null;
    private Paint mBgRightToLeft = null;
    private Context mContext;
    private BitmapDrawable mDrawSwipeBitmapDrawable = null;
    private RecyclerView mRecyclerView;
    private Bitmap mSwipeBitmap = null;
    private SwipeConfiguration mSwipeConfiguration;
    private Rect mSwipeRect = null;
    private TextPaint mTextPaint = null;

    public static class SwipeConfiguration {
        public int UNSET_VALUE = -1;

        public Drawable drawableLeftToRight;
        public Drawable drawableRightToLeft;
        public String textLeftToRight;
        public String textRightToLeft;

        public int colorLeftToRight = UNSET_VALUE;
        public int colorRightToLeft = UNSET_VALUE;
        public int drawablePadding = UNSET_VALUE;
        public int textSize = UNSET_VALUE;
        public int textColor = UNSET_VALUE;
    }

    public SeslSwipeListAnimator(RecyclerView recyclerView, Context context) {
        mContext = context;
        mRecyclerView = recyclerView;
    }

    public void setSwipeConfiguration(SwipeConfiguration swipeConfiguration) {
        mSwipeConfiguration = swipeConfiguration;

        if (swipeConfiguration.textLeftToRight == null) {
            mSwipeConfiguration.textLeftToRight = " ";
        }
        if (mSwipeConfiguration.textRightToLeft == null) {
            mSwipeConfiguration.textRightToLeft = " ";
        }

        if (mSwipeConfiguration.colorLeftToRight == mSwipeConfiguration.UNSET_VALUE) {
            mSwipeConfiguration.colorLeftToRight = DEFAULT_LEFT_COLOR;
        }
        if (mSwipeConfiguration.colorRightToLeft == mSwipeConfiguration.UNSET_VALUE) {
            mSwipeConfiguration.colorRightToLeft = DEFAULT_RIGHT_COLOR;
        }
        if (mSwipeConfiguration.textColor == mSwipeConfiguration.UNSET_VALUE) {
            mSwipeConfiguration.textColor = DEFAULT_TEXT_COLOR;
        }

        if (mSwipeConfiguration.textSize == mSwipeConfiguration.UNSET_VALUE) {
            mSwipeConfiguration.textSize = DEFAULT_TEXT_SIZE;
        }
        if (mSwipeConfiguration.drawablePadding == mSwipeConfiguration.UNSET_VALUE) {
            mSwipeConfiguration.drawablePadding = DEFAULT_DRAWABLE_PADDING;
        }

        mBgLeftToRight = initPaintWithAlphaAntiAliasing(mSwipeConfiguration.colorLeftToRight);
        mBgRightToLeft = initPaintWithAlphaAntiAliasing(mSwipeConfiguration.colorRightToLeft);

        mTextPaint = new TextPaint();
        mTextPaint.setColor(mSwipeConfiguration.textColor);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(convertDipToPixels(mContext, mSwipeConfiguration.textSize));
    }

    private Paint initPaintWithAlphaAntiAliasing(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        return paint;
    }

    private int convertDipToPixels(Context context, int dip) {
        final float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dip * density);
    }

    public void doMoveAction(Canvas c, View viewForeground, float deltaX,
                             boolean isCurrentlyActive) {
        Log.i(TAG, "doMoveAction: viewForeground = " + viewForeground +
                " deltaX = " + deltaX + ", isCurrentlyActive = " + isCurrentlyActive);

        if (deltaX != 0f || isCurrentlyActive) {
            Log.i(TAG, "doMoveAction: #1 drawRectToBitmapCanvas");

            drawRectToBitmapCanvas(viewForeground, deltaX, deltaX / viewForeground.getWidth());
            viewForeground.setTranslationX(deltaX);
            viewForeground.setAlpha(1f - (Math.abs(deltaX) / viewForeground.getWidth()));

            mDrawSwipeBitmapDrawable = getBitmapDrawableToSwipeBitmap();
            if (mDrawSwipeBitmapDrawable != null) {
                mRecyclerView.invalidate(mDrawSwipeBitmapDrawable.getBounds());
                Log.i(TAG, "doMoveAction: draw");
                mDrawSwipeBitmapDrawable.draw(c);
            }
        } else {
            Log.i(TAG, "doMoveAction: #2 reutrn");
            clearSwipeAnimation(viewForeground);
        }
    }

    private int calculateTopOfList(View view) {
        final int top = view.getTop();
        View parent = (View) view.getParent();
        return (parent != null && !(parent instanceof RecyclerView)) ?
                top + calculateTopOfList(parent) : top;
    }

    // TODO rework this method
    // kang
    private Canvas drawRectToBitmapCanvas(View var1, float var2, float var3) {
        /* var1 = view; var2 = deltaX; var3 = swipeProgress; */
        int[] var4 = new int[2];
        this.mRecyclerView.getLocationInWindow(var4);
        int[] var5 = new int[2];
        var1.setTranslationX(0.0F);
        var1.getLocationInWindow(var5);
        var5[0] -= var4[0];
        int var6 = this.calculateTopOfList(var1);
        int var7 = var1.getWidth();
        int var8 = var1.getHeight();
        this.mSwipeRect = new Rect(var5[0] + var1.getPaddingLeft(), var6, var5[0] + var1.getWidth() - var1.getPaddingRight(), var6 + var8);
        if (this.mSwipeBitmap == null) {
            this.mSwipeBitmap = Bitmap.createBitmap(var7, var8, Bitmap.Config.ARGB_8888);
        }

        Canvas var19 = new Canvas(this.mSwipeBitmap);
        var19.drawColor(0, PorterDuff.Mode.CLEAR);
        float var9 = Math.abs(var2);
        float var10 = var9 / (float)var1.getWidth() * 255.0F;
        int var11;
        int var12;
        androidx.recyclerview.widget.SeslSwipeListAnimator.SwipeConfiguration var15;
        Rect var17;
        StringBuilder var18;
        Rect var23;
        if (var3 > 0.0F) {
            Drawable var20 = this.mSwipeConfiguration.drawableLeftToRight;
            if (var20 != null) {
                var17 = var20.getBounds();
                var6 = var17.width();
                var11 = var17.height();
                var18 = new StringBuilder();
                var18.append("#1 draw LtoR, d = ");
                var18.append(var20);
                var18.append(", d.getBounds()=");
                var18.append(var20.getBounds());
                Log.i("SeslSwipeListAnimator", var18.toString());
                var12 = this.mSwipeConfiguration.drawablePadding;
                var17 = new Rect(var12, 0, var6 + var12, var11);
                var17.offset(0, (var8 - var11) / 2);
            } else {
                Log.i("SeslSwipeListAnimator", "#2 draw LtoR, d = null");
                var17 = new Rect(0, 0, 0, 0);
            }

            var6 = (int)var2;
            Rect var13 = new Rect(0, 0, var6, var8);
            Paint var14 = this.mBgLeftToRight;
            var15 = this.mSwipeConfiguration;
            this.drawRectInto(var19, var13, var17, var20, var14, 255, var15.textLeftToRight, (float)var15.textSize, 0, var7);
            var23 = new Rect(var6, 0, var7, var8);
            Paint var21 = this.mBgLeftToRight;
            var8 = (int)var10;
            var15 = this.mSwipeConfiguration;
            this.drawRectInto(var19, var23, var17, var20, var21, var8, var15.textLeftToRight, (float)var15.textSize, 0, var7);
        } else if (var3 < 0.0F) {
            Drawable var22 = this.mSwipeConfiguration.drawableRightToLeft;
            if (var22 != null) {
                var17 = var22.getBounds();
                var12 = var17.width();
                var11 = var17.height();
                var6 = var7 - this.mSwipeConfiguration.drawablePadding;
                var18 = new StringBuilder();
                var18.append("#3 draw RtoL, d = ");
                var18.append(var22);
                var18.append(", d.getBounds()=");
                var18.append(var22.getBounds());
                Log.i("SeslSwipeListAnimator", var18.toString());
                var17 = new Rect(var6 - var12, 0, var6, var11);
                var17.offset(0, (var8 - var11) / 2);
            } else {
                Log.i("SeslSwipeListAnimator", "#4 draw RtoL, d = null");
                var17 = new Rect(var7, 0, var7, 0);
            }

            var6 = var7 - (int)var9;
            var23 = new Rect(var6, 0, var7, var8);
            Paint var16 = this.mBgRightToLeft;
            var15 = this.mSwipeConfiguration;
            this.drawRectInto(var19, var23, var17, var22, var16, 255, var15.textRightToLeft, (float)var15.textSize, 1, var7);
            var23 = new Rect(0, 0, var6, var8);
            var16 = this.mBgRightToLeft;
            var8 = (int)var10;
            var15 = this.mSwipeConfiguration;
            this.drawRectInto(var19, var23, var17, var22, var16, var8, var15.textRightToLeft, (float)var15.textSize, 1, var7);
        }

        return var19;
    }
    // kang

    // TODO rework this method
    // kang
    private void drawRectInto(Canvas var1, Rect var2, Rect var3, Drawable var4, Paint var5, int var6, String var7, float var8, int var9, int var10) {
        var1.save();
        var5.setAlpha(var6);
        this.mTextPaint.setAlpha(var6);
        var1.clipRect(var2);
        var1.drawRect(var2, var5);
        if (var4 != null) {
            var4.setBounds(var3);
            var4.draw(var1);
        }

        this.drawSwipeText(var1, this.mTextPaint, var7, var9, var3, var10);
        var1.restore();
    }
    // kang

    // TODO rework this method
    // kang
    private void drawSwipeText(Canvas var1, TextPaint var2, String var3, int var4, Rect var5, int var6) {
        Rect var7 = new Rect();
        var2.setTextAlign(Paint.Align.LEFT);
        var2.getTextBounds(var3, 0, var3.length(), var7);
        Paint.FontMetrics var8 = var2.getFontMetrics();
        float var9 = Math.abs(var8.top - var8.bottom);
        float var10 = (float)var1.getHeight() / 2.0F;
        float var11 = var9 / 2.0F;
        float var12 = var8.bottom;
        if (var4 == 0) {
            var9 = (float)(var5.right + this.mSwipeConfiguration.drawablePadding);
        } else {
            var4 = var5.left;
            int var13 = this.mSwipeConfiguration.drawablePadding;
            float var14 = (float)(var4 - var13 - var7.right);
            var9 = var14;
            if (var14 < (float)var13) {
                var9 = (float)var13;
            }
        }

        var1.drawText(TextUtils.ellipsize(var3, var2, (float)(var6 - var5.width() - this.mSwipeConfiguration.drawablePadding * 2), TextUtils.TruncateAt.END).toString(), var9, var10 + var11 - var12, var2);
    }
    // kang

    private BitmapDrawable getBitmapDrawableToSwipeBitmap() {
        if (mSwipeBitmap == null) {
            return null;
        }

        BitmapDrawable d = new BitmapDrawable(mRecyclerView.getResources(), mSwipeBitmap);
        d.setBounds(mSwipeRect);
        return d;
    }

    private void drawTextToCenter(Canvas canvas, Paint paint, String text) {
        final int height = canvas.getHeight();
        final int width = canvas.getWidth();

        Rect rect = new Rect();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), rect);

        final float x = ((width / 2f) - (rect.width() / 2f)) - rect.left;
        final float y = ((height / 2f) + (rect.height() / 2f)) - rect.bottom;
        canvas.drawText(text, x, y, paint);
    }

    public void clearSwipeAnimation(View view) {
        Log.i(TAG, "clearSwipeAnimation: view = " + view +
                " mDrawSwipeBitmapDrawable = " + mDrawSwipeBitmapDrawable);

        if (mDrawSwipeBitmapDrawable != null) {
            mDrawSwipeBitmapDrawable.getBitmap().recycle();
            mDrawSwipeBitmapDrawable = null;
            mSwipeBitmap = null;
        }

        if (view != null) {
            Log.i(TAG, "clearSwipeAnimation: view.getTranslationX() = "
                    + view.getTranslationX());
            if (view.getTranslationX() != 0f) {
                Log.i(TAG, "clearSwipeAnimation: **** set view.setTranslationX(0f) ****");
                view.setTranslationX(0f);
            }
        }
    }

    public void onSwiped(View view) {
        Log.i(TAG, "onSwiped");
        clearSwipeAnimation(view);
        view.setTranslationX(0f);
        view.setAlpha(1f);
    }
}
