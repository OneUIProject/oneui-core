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

package androidx.core.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Edge Effect class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslEdgeEffect extends EdgeEffect {
    private static final String TAG = "SeslEdgeEffect";

    private static final boolean DEBUG = false;

    private static final int[] ATTRS = {
            android.R.attr.colorEdgeEffect
    };

    private static final int APPEAR_TIME = 250;

    // Time it will take the effect to fully recede in ms
    private static final int RECEDE_TIME = 450;

    // Time it will take before a pulled glow begins receding in ms
    private static final int PULL_TIME = 167;

    private static final int KEEP_TIME = 0;

    private static final float MAX_GLOW_SCALE = 2.f;

    private static final float PULL_GLOW_BEGIN = 0.f;

    // Minimum velocity that will be absorbed
    private static final int MIN_VELOCITY = 100;
    // Maximum velocity, clamps at this value
    private static final int MAX_VELOCITY = 10000;

    private static final float EPSILON = 0.001f;

    private static final double ANGLE = Math.PI / 6;
    private static final float SIN = (float) Math.sin(ANGLE);
    private static final float COS = (float) Math.cos(ANGLE);
    private static final float RADIUS_FACTOR = 0.75f;

    private float MAX_SCALE = 1.0f;

    private static final float EDGE_MAX_ALPAH_DARK = 0.08f;
    private static final float EDGE_MAX_ALPAH_LIGHT = 0.05f;
    private static final float EDGE_PADDING_WITHOUT_TAB_IN_DIP = 5.0f;
    private static final float EDGE_PADDING_WITH_TAB_IN_DIP = 3.0f;
    private static final float EDGE_CONTROL_POINT_HEIGHT_WITHOUT_TAB_IN_DIP = 29.0f;
    private static final float EDGE_CONTROL_POINT_HEIGHT_WITH_TAB_IN_DIP = 19.0f;

    private static final float TAB_HEIGHT_IN_DIP = 85.0f;
    private static final float TAB_HEIGHT_BUFFER_IN_DIP = 5.0f;

    private static final int MSG_CALL_ONRELEASE = 1;

    private static float sMaxAlpha;

    private float mGlowAlpha;
    private float mGlowScaleY;

    private float mGlowAlphaStart;
    private float mGlowAlphaFinish;
    private float mGlowScaleYStart;
    private float mGlowScaleYFinish;

    private float mEdgeEffectMargin = 0.0f;
    private float mEdgePadding;
    private float mEdgeControlPointHeight;

    private final float mTabHeight;
    private final float mTabHeightBuffer;

    private float mTempDeltaDistance;
    private float mTempDisplacement;

    private long mStartTime;
    private float mDuration;

    private final DisplayMetrics mDisplayMetrics;
    private final Interpolator mInterpolator;

    private View mHostView;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PULL = 1;
    private static final int STATE_ABSORB = 2;
    private static final int STATE_RECEDE = 3;
    private static final int STATE_PULL_DECAY = 4;
    private static final int STATE_APPEAR = 5;
    private static final int STATE_KEEP = 6;

    private int mState = STATE_IDLE;

    private float mPullDistance;

    private final Rect mBounds = new Rect();
    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();
    private float mDisplacement = 0.5f;
    private float mTargetDisplacement = 0.5f;

    private boolean mCanVerticalScroll = true;
    private boolean mOnReleaseCalled = false;

    /**
     * Construct a new EdgeEffect with a theme appropriate for the provided context.
     * @param context Context used to provide theming and resource information for the EdgeEffect
     */
    public SeslEdgeEffect(Context context) {
        super(context);
        mPaint.setAntiAlias(true);
        final TypedArray a = context.getTheme().obtainStyledAttributes(ATTRS);
        final int themeColor = a.getColor(0, 0xff666666);
        a.recycle();
        mPaint.setColor(themeColor & 0xffffff);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        sMaxAlpha = isLightTheme(context) ? EDGE_MAX_ALPAH_LIGHT : EDGE_MAX_ALPAH_DARK;
        mInterpolator = new DecelerateInterpolator();
        mDisplayMetrics = context.getResources().getDisplayMetrics();
        mTabHeight = dipToPixels(TAB_HEIGHT_IN_DIP);
        mTabHeightBuffer = dipToPixels(TAB_HEIGHT_BUFFER_IN_DIP);
    }

    private boolean isLightTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        final boolean valid = context.getTheme().resolveAttribute(android.R.attr.isLightTheme,
                typedValue, true);
        return !valid || typedValue.data != 0;
    }

    private float dipToPixels(float dipValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dipValue, mDisplayMetrics);
    }

    private float calculateEdgeEffectMargin(int width) {
        final float margin = ((float) (width * 0.136d)) / 2;
        return margin;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setHostView(View hostView, boolean canVerticalScroll) {
        mHostView = hostView;
        mCanVerticalScroll = canVerticalScroll;
    }

    /**
     * Set the size of this edge effect in pixels.
     *
     * @param width Effect width in pixels
     * @param height Effect height in pixels
     */
    @Override
    public void setSize(int width, int height) {
        final float r = width * RADIUS_FACTOR / SIN;
        final float y = COS * r;
        final float h = r - y;

         if (width > mTabHeight + mTabHeightBuffer) {
             mEdgePadding = dipToPixels(EDGE_PADDING_WITHOUT_TAB_IN_DIP);
             mEdgeControlPointHeight = dipToPixels(EDGE_CONTROL_POINT_HEIGHT_WITHOUT_TAB_IN_DIP);
         } else {
             mEdgePadding = dipToPixels(EDGE_PADDING_WITH_TAB_IN_DIP);
             mEdgeControlPointHeight = dipToPixels(EDGE_CONTROL_POINT_HEIGHT_WITH_TAB_IN_DIP);
         }

         if (mCanVerticalScroll) {
             mEdgeEffectMargin = calculateEdgeEffectMargin(width);
         }

        mBounds.set(mBounds.left, mBounds.top, width, (int) Math.min(height, h));
    }

    /**
     * Reports if this EdgeEffect's animation is finished. If this method returns false
     * after a call to {@link #draw(Canvas)} the host widget should schedule another
     * drawing pass to continue the animation.
     *
     * @return true if animation is finished, false if drawing should continue on the next frame.
     */
    @Override
    public boolean isFinished() {
        return mState == STATE_IDLE;
    }

    /**
     * Immediately finish the current animation.
     * After this call {@link #isFinished()} will return true.
     */
    @Override
    public void finish() {
        mState = STATE_IDLE;
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} after this
     * and draw the results accordingly.
     *
     * <p>Views using EdgeEffect should favor {@link #onPull(float, float)} when the displacement
     * of the pull point is known.</p>
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     */
    @Override
    public void onPull(float deltaDistance) {
        onPull(deltaDistance, 0.5f);
    }

    private boolean isEdgeEffectRunning() {
        return mState == STATE_APPEAR || mState == STATE_KEEP
                || mState == STATE_RECEDE || mState == STATE_ABSORB;
    }

    public void onPullCallOnRelease(float deltaDistance, float displacement, int delayTime) {
        mTempDeltaDistance = deltaDistance;
        mTempDisplacement = displacement;

        if (delayTime == 0) {
            mOnReleaseCalled = true;
            onPull(deltaDistance, displacement);
            mHandler.sendEmptyMessageDelayed(MSG_CALL_ONRELEASE, 700);
        } else {
            mHandler.postDelayed(mForceCallOnRelease, delayTime);
        }
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} after this
     * and draw the results accordingly.
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @param displacement The displacement from the starting side of the effect of the point
     *                     initiating the pull. In the case of touch this is the finger position.
     *                     Values may be from 0-1.
     */
    @Override
    public void onPull(float deltaDistance, float displacement) {
        if (mPullDistance == 0) {
            mOnReleaseCalled = false;
            if (isEdgeEffectRunning()) {
                mPullDistance += deltaDistance;
            }
        }

        final long now = AnimationUtils.currentAnimationTimeMillis();
        mTargetDisplacement = displacement;
        if (mState == STATE_PULL_DECAY && now - mStartTime < mDuration) {
            return;
        }
        if (mState != STATE_PULL) {
            mGlowScaleY = Math.max(PULL_GLOW_BEGIN, mGlowScaleY);
        }

        if (isEdgeEffectRunning()) {
            return;
        }

        if (mPullDistance == 0 || mOnReleaseCalled) {
            if (mHostView != null) {
                final int feedbackConstant = SeslHapticFeedbackConstantsReflector
                        .semGetVibrationIndex(28);
                if (feedbackConstant != -1) {
                    mHostView.performHapticFeedback(feedbackConstant);
                }
            }

            mState = STATE_PULL;
            mStartTime = now;
            mDuration = PULL_TIME;
            mPullDistance += deltaDistance;
        }
    }

    /**
     * Call when the object is released after being pulled.
     * This will begin the "decay" phase of the effect. After calling this method
     * the host view should {@link android.view.View#invalidate()} and thereby
     * draw the results accordingly.
     */
    @Override
    public void onRelease() {
        mPullDistance = 0;
        mOnReleaseCalled = true;

        if (mState != STATE_PULL && mState != STATE_PULL_DECAY) {
            return;
        }

        mState = STATE_RECEDE;
        mGlowAlphaStart = mGlowAlpha;
        mGlowScaleYStart = mGlowScaleY;

        mGlowAlphaFinish = 0.f;
        mGlowScaleYFinish = 0.f;

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = RECEDE_TIME;
    }

    /**
     * Call when the effect absorbs an impact at the given velocity.
     * Used when a fling reaches the scroll boundary.
     *
     * <p>When using a {@link android.widget.Scroller} or {@link android.widget.OverScroller},
     * the method <code>getCurrVelocity</code> will provide a reasonable approximation
     * to use here.</p>
     *
     * @param velocity Velocity at impact in pixels per second.
     */
    @Override
    public void onAbsorb(int velocity) {
        if (isEdgeEffectRunning()) {
            return;
        }

        if (mHostView != null) {
            mHostView.performHapticFeedback(SeslHapticFeedbackConstantsReflector
                    .semGetVibrationIndex(28));
        }

        mOnReleaseCalled = true;
        mState = STATE_ABSORB;
        velocity = Math.min(Math.max(MIN_VELOCITY, Math.abs(velocity)), MAX_VELOCITY);

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = APPEAR_TIME;

        // The glow depends more on the velocity, and therefore starts out
        // nearly invisible.
        mGlowAlphaStart = 0;
        mGlowScaleYStart = 0;

        mGlowScaleYFinish = MAX_SCALE;
        // Alpha should change for the glow as well as size.
        mGlowAlphaFinish = sMaxAlpha;
        mTargetDisplacement = 0.5f;

        mHandler.sendEmptyMessageDelayed(MSG_CALL_ONRELEASE, 700);
    }

    /**
     * Set the color of this edge effect in argb.
     *
     * @param color Color in argb
     */
    @Override
    public void setColor(@ColorInt int color) {
        mPaint.setColor(color);
    }

    /**
     * Return the color of this edge effect in argb.
     * @return The color of this edge effect in argb
     */
    @Override
    @ColorInt
    public int getColor() {
        return mPaint.getColor();
    }

    /**
     * Draw into the provided canvas. Assumes that the canvas has been rotated
     * accordingly and the size has been set. The effect will be drawn the full
     * width of X=0 to X=width, beginning from Y=0 and extending to some factor <
     * 1.f of height.
     *
     * @param canvas Canvas to draw into
     * @return true if drawing should continue beyond this frame to continue the
     *         animation
     */
    public boolean draw(Canvas canvas) {
        update();

        final int count = canvas.save();

        final float centerX = mBounds.centerX();

        canvas.scale(1.f, Math.min(mGlowScaleY, 1.f), centerX, 0);

        final float displacement = Math.max(0, Math.min(mDisplacement, 1.f));
        float translateX = mBounds.width() * 0.2f;

        mPath.reset();
        mPath.moveTo(mEdgeEffectMargin, 0.0f);
        mPath.lineTo(mEdgeEffectMargin, 0.0f);
        mPath.cubicTo(centerX - translateX, mEdgeControlPointHeight,
                centerX + translateX, mEdgeControlPointHeight,
                mBounds.width() - mEdgeEffectMargin, 0.0f);
        mPath.lineTo(mBounds.width() - mEdgeEffectMargin, 0.0f);
        mPath.close();

        mPaint.setAlpha((int) (0xff * mGlowAlpha));
        canvas.drawPath(mPath, mPaint);
        canvas.restoreToCount(count);

        boolean oneLastFrame = false;
        if (mState == STATE_RECEDE && mGlowScaleY == 0) {
            mState = STATE_IDLE;
            oneLastFrame = true;
        }

        return mState != STATE_IDLE || oneLastFrame;
    }

    /**
     * Return the maximum height that the edge effect will be drawn at given the original
     * {@link #setSize(int, int) input size}.
     * @return The maximum height of the edge effect
     */
    @Override
    public int getMaxHeight() {
        return (int) (mBounds.height() * MAX_GLOW_SCALE + 0.5f);
    }

    private void update() {
        final long time = AnimationUtils.currentAnimationTimeMillis();
        final float t = Math.min((time - mStartTime) / mDuration, 1.f);

        final float interp = mInterpolator.getInterpolation(t);

        mGlowAlpha = mGlowAlphaStart + (mGlowAlphaFinish - mGlowAlphaStart) * interp;
        mGlowScaleY = mGlowScaleYStart + (mGlowScaleYFinish - mGlowScaleYStart) * interp;
        mDisplacement = (mDisplacement + mTargetDisplacement) / 2;

        if (t >= 1.f - EPSILON || mState == STATE_PULL) {
            switch (mState) {
                case STATE_ABSORB:
                    mState = STATE_KEEP;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = KEEP_TIME;

                    mGlowAlphaStart = sMaxAlpha;
                    mGlowAlphaFinish = sMaxAlpha;
                    mGlowScaleYStart = MAX_SCALE;
                    mGlowScaleYFinish = MAX_SCALE;
                    break;
                case STATE_PULL:
                    mState = STATE_APPEAR;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = APPEAR_TIME;

                    mGlowAlphaStart = 0.f;
                    mGlowScaleYStart = 0.f;
                    mGlowAlphaFinish = sMaxAlpha;
                    mGlowScaleYFinish = MAX_SCALE;
                    mGlowScaleY = 0.f;
                    mOnReleaseCalled = false;
                    break;
                case STATE_PULL_DECAY:
                    mState = STATE_RECEDE;
                    break;
                case STATE_RECEDE:
                    mState = STATE_IDLE;
                    break;
                case STATE_APPEAR:
                    mState = STATE_KEEP;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = KEEP_TIME;

                    mGlowAlphaStart = sMaxAlpha;
                    mGlowAlphaFinish = sMaxAlpha;
                    mGlowScaleYStart = MAX_SCALE;
                    mGlowScaleYFinish = MAX_SCALE;
                    break;
                case STATE_KEEP:
                    mState = STATE_RECEDE;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = RECEDE_TIME;

                    mGlowAlphaStart = mGlowAlpha;
                    mGlowScaleYStart = mGlowScaleY;
                    mGlowAlphaFinish = 0.0f;
                    mGlowScaleYFinish = 0.0f;
                    break;
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == MSG_CALL_ONRELEASE) {
                onRelease();
            }
        }
    };

    private Runnable mForceCallOnRelease = new Runnable() {
        @Override
        public void run() {
            mOnReleaseCalled = true;
            onPull(mTempDeltaDistance, mTempDisplacement);
            mHandler.sendEmptyMessageDelayed(MSG_CALL_ONRELEASE, 700);
        }
    };

}
