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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.animation.SeslAnimationUtils;
import androidx.appcompat.graphics.drawable.DrawableWrapper;
import androidx.appcompat.util.SeslMisc;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.Preconditions;
import androidx.core.view.ViewCompat;
import androidx.reflect.view.SeslViewReflector;
import androidx.reflect.widget.SeslHoverPopupWindowReflector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung AbsSeekBar class.
 */
public abstract class SeslAbsSeekBar extends SeslProgressBar {
    private static final String TAG = "SeslAbsSeekBar";

    private static final boolean IS_BASE_SDK_VERSION = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;

    static final float SCALE_FACTOR = 1000.0f;

    private static final int MUTE_VIB_TOTAL = 4;
    private static final int MUTE_VIB_DURATION = 500;
    private static final int MUTE_VIB_DISTANCE_LVL = 400;

    private static final int HOVER_DETECT_TIME = 200;
    private static final int HOVER_POPUP_WINDOW_GRAVITY_TOP_ABOVE = 12336;
    private static final int HOVER_POPUP_WINDOW_GRAVITY_CENTER_HORIZONTAL_ON_POINT = 513;

    private final Rect mTempRect = new Rect();

    private ValueAnimator mValueAnimator;
    private AnimatorSet mMuteAnimationSet;

    private ColorStateList mDefaultNormalProgressColor;
    private ColorStateList mDefaultSecondaryProgressColor;
    private ColorStateList mDefaultActivatedProgressColor;
    private ColorStateList mDefaultActivatedThumbColor;
    private ColorStateList mOverlapNormalProgressColor;
    private ColorStateList mOverlapActivatedProgressColor;
    private Drawable mOverlapBackground;
    private Drawable mSplitProgress;

    private Drawable mThumb;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbTintMode = false;

    private Drawable mTickMark;
    private ColorStateList mTickMarkTintList = null;
    private PorterDuff.Mode mTickMarkTintMode = null;
    private boolean mHasTickMarkTint = false;
    private boolean mHasTickMarkTintMode = false;

    private int mCurrentProgressLevel;
    private int mOverlapPoint = -1;
    private int mHoveringLevel = 0;

    private int mPreviousHoverPopupType = 0;

    private int mThumbRadius;
    private int mThumbPosX;
    private int mThumbOffset;
    private boolean mSplitTrack;

    private int mTrackMinWidth;
    private int mTrackMaxWidth;

    private Drawable mDivider;

    /**
     * On touch, this offset plus the scaled value from the position of the
     * touch will form the progress value. Usually 0.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    float mTouchProgressOffset;

    /**
     * Whether this is user seekable.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    boolean mIsUserSeekable = true;

    /**
     * On key presses (right or left), the amount to increment/decrement the
     * progress.
     */
    private int mKeyProgressIncrement = 1;

    private static final int NO_ALPHA = 0xFF;
    private float mDisabledAlpha;

    private int mScaledTouchSlop;
    private float mTouchDownX;
    private float mTouchDownY;
    private boolean mIsDragging;

    private boolean mAllowedSeekBarAnimation = false;
    private boolean mIsDraggingForSliding = false;
    private boolean mIsFirstSetProgress = false;
    private boolean mIsLightTheme;
    boolean mIsSeamless = false;
    private boolean mIsSetModeCalled = false;
    private boolean mIsTouchDisabled = false;
    private boolean mLargeFont = false;
    private boolean mSetDualColorMode = false;
    private boolean mUseMuteAnimation = false;

    private List<Rect> mUserGestureExclusionRects = Collections.emptyList();
    private final List<Rect> mGestureExclusionRects = new ArrayList<>();
    private final Rect mThumbRect = new Rect();

    public SeslAbsSeekBar(Context context) {
        super(context);
    }

    public SeslAbsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeslAbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslAbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AppCompatSeekBar, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.AppCompatSeekBar, attrs, a, defStyleAttr,
                    defStyleRes);
        }

        final Resources res = context.getResources();

        final Drawable thumb = a.getDrawable(R.styleable.AppCompatSeekBar_android_thumb);
        setThumb(thumb);

        if (a.hasValue(R.styleable.AppCompatSeekBar_android_thumbTintMode)) {
            mThumbTintMode = DrawableUtils.parseTintMode(a.getInt(
                    R.styleable.AppCompatSeekBar_android_thumbTintMode, -1), mThumbTintMode);
            mHasThumbTintMode = true;
        }

        if (a.hasValue(R.styleable.AppCompatSeekBar_android_thumbTint)) {
            mThumbTintList = a.getColorStateList(R.styleable.AppCompatSeekBar_android_thumbTint);
            mHasThumbTint = true;
        }

        final Drawable tickMark = a.getDrawable(R.styleable.AppCompatSeekBar_tickMark);
        setTickMark(tickMark);

        if (a.hasValue(R.styleable.AppCompatSeekBar_tickMarkTintMode)) {
            mTickMarkTintMode = DrawableUtils.parseTintMode(a.getInt(
                    R.styleable.AppCompatSeekBar_tickMarkTintMode, -1), mTickMarkTintMode);
            mHasTickMarkTintMode = true;
        }

        if (a.hasValue(R.styleable.AppCompatSeekBar_tickMarkTint)) {
            mTickMarkTintList = a.getColorStateList(R.styleable.AppCompatSeekBar_tickMarkTint);
            mHasTickMarkTint = true;
        }

        mSplitTrack = a.getBoolean(R.styleable.AppCompatSeekBar_android_splitTrack, false);

        mTrackMinWidth = a.getDimensionPixelSize(R.styleable.AppCompatSeekBar_seslTrackMinWidth,
                Math.round(res.getDimension(R.dimen.sesl_seekbar_track_height)));
        mTrackMaxWidth = a.getDimensionPixelSize(R.styleable.AppCompatSeekBar_seslTrackMaxWidth,
                Math.round(res.getDimension(R.dimen.sesl_seekbar_track_height_expand)));
        mThumbRadius = a.getDimensionPixelSize(R.styleable.AppCompatSeekBar_seslThumbRadius,
                Math.round(res.getDimension(R.dimen.sesl_seekbar_thumb_radius)));

        // Guess thumb offset if thumb != null, but allow layout to override.
        final int thumbOffset = a.getDimensionPixelOffset(
                R.styleable.AppCompatSeekBar_android_thumbOffset, getThumbOffset());
        setThumbOffset(thumbOffset);

        if (a.hasValue(R.styleable.AppCompatSeekBar_seslSeekBarMode)) {
            mCurrentMode = a.getInt(R.styleable.AppCompatSeekBar_seslSeekBarMode, MODE_STANDARD);
        }

        final boolean useDisabledAlpha = a.getBoolean(R.styleable.AppCompatSeekBar_useDisabledAlpha, true);
        a.recycle();

        if (useDisabledAlpha) {
            final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AppCompatTheme, 0, 0);
            mDisabledAlpha = ta.getFloat(R.styleable.AppCompatTheme_android_disabledAlpha, 0.5f);
            ta.recycle();
        } else {
            mDisabledAlpha = 1.0f;
        }

        applyThumbTint();
        applyTickMarkTint();

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mIsLightTheme = SeslMisc.isLightTheme(context);

        mDefaultNormalProgressColor = colorToColorStateList(res.getColor(R.color.sesl_seekbar_control_color_default));
        mDefaultSecondaryProgressColor = colorToColorStateList(res.getColor(R.color.sesl_seekbar_control_color_secondary));
        mDefaultActivatedProgressColor = colorToColorStateList(res.getColor(R.color.sesl_seekbar_control_color_activated));
        mOverlapNormalProgressColor = colorToColorStateList(res.getColor(mIsLightTheme ?
                R.color.sesl_seekbar_overlap_color_default_light : R.color.sesl_seekbar_overlap_color_default_dark));
        mOverlapActivatedProgressColor = colorToColorStateList(res.getColor(mIsLightTheme ?
                R.color.sesl_seekbar_overlap_color_activated_light : R.color.sesl_seekbar_overlap_color_activated_dark));

        mDefaultActivatedThumbColor = getThumbTintList();
        if (mDefaultActivatedThumbColor == null) {
            final int[][] states = {new int[]{android.R.attr.state_enabled},
                    new int[]{-android.R.attr.state_enabled}};
            final int[] colors = new int[2];
            colors[0] = res.getColor(R.color.sesl_thumb_control_color_activated);
            colors[1] = res.getColor(mIsLightTheme ?
                    R.color.sesl_seekbar_disable_color_activated_light : R.color.sesl_seekbar_disable_color_activated_dark);
            mDefaultActivatedThumbColor = new ColorStateList(states, colors);
        }

        mAllowedSeekBarAnimation = res.getBoolean(R.bool.sesl_seekbar_sliding_animation);
        if (mAllowedSeekBarAnimation) {
            initMuteAnimation();
        }

        if (mCurrentMode != MODE_STANDARD) {
            setMode(mCurrentMode);
        }
    }

    /**
     * Sets the thumb that will be drawn at the end of the progress meter within the SeekBar.
     * <p>
     * If the thumb is a valid drawable (i.e. not null), half its width will be
     * used as the new thumb offset (@see #setThumbOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     */
    public void setThumb(Drawable thumb) {
        final boolean needUpdate;
        // This way, calling setThumb again with the same bitmap will result in
        // it recalcuating mThumbOffset (if for example it the bounds of the
        // drawable changed)
        if (mThumb != null && thumb != mThumb) {
            mThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (thumb != null) {
            thumb.setCallback(this);
            if (canResolveLayoutDirection()) {
                DrawableCompat.setLayoutDirection(thumb, ViewCompat.getLayoutDirection(this));
            }

            // Assuming the thumb drawable is symmetric, set the thumb offset
            // such that the thumb will hang halfway off either edge of the
            // progress bar.
            if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
                mThumbOffset = thumb.getIntrinsicHeight() / 2;
            } else {
                mThumbOffset = thumb.getIntrinsicWidth() / 2;
            }

            // If we're updating get the new states
            if (needUpdate &&
                    (thumb.getIntrinsicWidth() != mThumb.getIntrinsicWidth()
                        || thumb.getIntrinsicHeight() != mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }

        mThumb = thumb;

        applyThumbTint();
        invalidate();

        if (needUpdate) {
            updateThumbAndTrackPos(getWidth(), getHeight());
            if (thumb != null && thumb.isStateful()) {
                // Note that if the states are different this won't work.
                // For now, let's consider that an app bug.
                int[] state = getDrawableState();
                thumb.setState(state);
            }
        }
    }

    /**
     * Return the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     *
     * @return The current thumb drawable
     */
    public Drawable getThumb() {
        return mThumb;
    }

    /**
     * Applies a tint to the thumb drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setThumb(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#SeekBar_thumbTint
     * @see #getThumbTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setThumbTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
        mDefaultActivatedThumbColor = tint;
    }

    /**
     * Returns the tint applied to the thumb drawable, if specified.
     *
     * @return the tint applied to the thumb drawable
     * @attr ref android.R.styleable#SeekBar_thumbTint
     * @see #setThumbTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)}} to the thumb drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @attr ref android.R.styleable#SeekBar_thumbTintMode
     * @see #getThumbTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setThumbTintMode(@Nullable PorterDuff.Mode tintMode) {
        mThumbTintMode = tintMode;
        mHasThumbTintMode = true;
        applyThumbTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the thumb drawable,
     * if specified.
     *
     * @return the blending mode used to apply the tint to the thumb drawable
     * @attr ref android.R.styleable#SeekBar_thumbTintMode
     * @see #setThumbTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getThumbTintMode() {
        return mThumbTintMode;
    }

    private void applyThumbTint() {
        if (mThumb != null && (mHasThumbTint || mHasThumbTintMode)) {
            mThumb = mThumb.mutate();

            if (mHasThumbTint) {
                DrawableCompat.setTintList(mThumb, mThumbTintList);
            }

            if (mHasThumbTintMode) {
                DrawableCompat.setTintMode(mThumb, mThumbTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mThumb.isStateful()) {
                mThumb.setState(getDrawableState());
            }
        }
    }

    /**
     * @see #setThumbOffset(int)
     */
    public int getThumbOffset() {
        return mThumbOffset;
    }

    /**
     * Sets the thumb offset that allows the thumb to extend out of the range of
     * the track.
     *
     * @param thumbOffset The offset amount in pixels.
     */
    public void setThumbOffset(int thumbOffset) {
        mThumbOffset = thumbOffset;
        invalidate();
    }

    /**
     * Specifies whether the track should be split by the thumb. When true,
     * the thumb's optical bounds will be clipped out of the track drawable,
     * then the thumb will be drawn into the resulting gap.
     *
     * @param splitTrack Whether the track should be split by the thumb
     */
    public void setSplitTrack(boolean splitTrack) {
        mSplitTrack = splitTrack;
        invalidate();
    }

    /**
     * Returns whether the track should be split by the thumb.
     */
    public boolean getSplitTrack() {
        return mSplitTrack;
    }

    /**
     * Sets the drawable displayed at each progress position, e.g. at each
     * possible thumb position.
     *
     * @param tickMark the drawable to display at each progress position
     */
    public void setTickMark(Drawable tickMark) {
        if (mTickMark != null) {
            mTickMark.setCallback(null);
        }

        mTickMark = tickMark;

        if (tickMark != null) {
            tickMark.setCallback(this);
            DrawableCompat.setLayoutDirection(tickMark, ViewCompat.getLayoutDirection(this));
            if (tickMark.isStateful()) {
                tickMark.setState(getDrawableState());
            }
            applyTickMarkTint();
        }

        invalidate();
    }

    /**
     * @return the drawable displayed at each progress position
     */
    public Drawable getTickMark() {
        return mTickMark;
    }

    /**
     * Applies a tint to the tick mark drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setTickMark(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#SeekBar_tickMarkTint
     * @see #getTickMarkTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setTickMarkTintList(@Nullable ColorStateList tint) {
        mTickMarkTintList = tint;
        mHasTickMarkTint = true;

        applyTickMarkTint();
    }

    /**
     * Returns the tint applied to the tick mark drawable, if specified.
     *
     * @return the tint applied to the tick mark drawable
     * @attr ref android.R.styleable#SeekBar_tickMarkTint
     * @see #setTickMarkTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getTickMarkTintList() {
        return mTickMarkTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTickMarkTintList(ColorStateList)}} to the tick mark drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @attr ref android.R.styleable#SeekBar_tickMarkTintMode
     * @see #getTickMarkTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setTickMarkTintMode(@Nullable PorterDuff.Mode tintMode) {
        mTickMarkTintMode = tintMode;
        mHasTickMarkTintMode = true;

        applyTickMarkTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the tick mark drawable,
     * if specified.
     *
     * @return the blending mode used to apply the tint to the tick mark drawable
     * @attr ref android.R.styleable#SeekBar_tickMarkTintMode
     * @see #setTickMarkTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getTickMarkTintMode() {
        return mTickMarkTintMode;
    }

    private void applyTickMarkTint() {
        if (mTickMark != null && (mHasTickMarkTint || mHasTickMarkTintMode)) {
            mTickMark = mTickMark.mutate();

            if (mHasTickMarkTint) {
                DrawableCompat.setTintList(mTickMark, mTickMarkTintList);
            }

            if (mHasTickMarkTintMode) {
                DrawableCompat.setTintMode(mTickMark, mTickMarkTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTickMark.isStateful()) {
                mTickMark.setState(getDrawableState());
            }
        }
    }

    /**
     * Sets the amount of progress changed via the arrow keys.
     *
     * @param increment The amount to increment or decrement when the user
     *            presses the arrow keys.
     */
    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = increment < 0 ? -increment : increment;
    }

    /**
     * Returns the amount of progress changed via the arrow keys.
     * <p>
     * By default, this will be a value that is derived from the progress range.
     *
     * @return The amount to increment or decrement when the user presses the
     *         arrow keys. This will be positive.
     */
    public int getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    @Override
    public synchronized void setMin(int min) {
        if (mIsSeamless) {
            min = Math.round(min * SCALE_FACTOR);
        }

        super.setMin(min);
        int range = getMax() - getMin();

        if ((mKeyProgressIncrement == 0) || (range / mKeyProgressIncrement > 20)) {

            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) range / 20)));
        }
    }

    @Override
    public synchronized void setMax(int max) {
        if (mIsSeamless) {
            max = Math.round(max * SCALE_FACTOR);
        }

        super.setMax(max);
        mIsFirstSetProgress = true;
        int range = getMax() - getMin();

        if ((mKeyProgressIncrement == 0) || (range / mKeyProgressIncrement > 20)) {
            // It will take the user too long to change this via keys, change it
            // to something more reasonable
            setKeyProgressIncrement(Math.max(1, Math.round((float) range / 20)));
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mThumb || who == mTickMark || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumb != null) {
            mThumb.jumpToCurrentState();
        }

        if (mTickMark != null) {
            mTickMark.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null && mDisabledAlpha < 1.0f) {
            final int alpha = isEnabled() ? NO_ALPHA : (int) (NO_ALPHA * mDisabledAlpha);
            progressDrawable.setAlpha(alpha);
            if (mOverlapBackground != null) {
                mOverlapBackground.setAlpha(alpha);
            }
        }

        if (mThumb != null && mHasThumbTint) {
            if (!isEnabled()) {
                DrawableCompat.setTintList(mThumb, null);
            } else {
                DrawableCompat.setTintList(mThumb, mDefaultActivatedThumbColor);
                updateDualColorMode();
            }
        }
        if (mSetDualColorMode && progressDrawable != null && progressDrawable.isStateful() && mOverlapBackground != null) {
            mOverlapBackground.setState(getDrawableState());
        }

        final Drawable thumb = mThumb;
        if (thumb != null && thumb.isStateful()
                && thumb.setState(getDrawableState())) {
            invalidateDrawable(thumb);
        }

        final Drawable tickMark = mTickMark;
        if (tickMark != null && tickMark.isStateful()
                && tickMark.setState(getDrawableState())) {
            invalidateDrawable(tickMark);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mThumb != null) {
            DrawableCompat.setHotspot(mThumb, x, y);
        }
    }

    @Override
    void onVisualProgressChanged(int id, float scale) {
        super.onVisualProgressChanged(id, scale);

        if (id == android.R.id.progress) {
            final Drawable thumb = mThumb;
            if (thumb != null) {
                setThumbPos(getWidth(), thumb, scale, Integer.MIN_VALUE);

                // Since we draw translated, the drawable's bounds that it signals
                // for invalidation won't be the actual bounds we want invalidated,
                // so just invalidate this whole view.
                invalidate();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        updateThumbAndTrackPos(w, h);
    }

    private void updateThumbAndTrackPos(int w, int h) {
        if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
            updateThumbAndTrackPosInVertical(w, h);
            return;
        }

        final int paddedHeight = h - getPaddingTop() - getPaddingBottom();
        final Drawable track = getCurrentDrawable();
        final Drawable thumb = mThumb;

        // The max height does not incorporate padding, whereas the height
        // parameter does.
        final int trackHeight = Math.min(mMaxHeight, paddedHeight);
        final int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbHeight > trackHeight) {
            final int offsetHeight = (paddedHeight - thumbHeight) / 2;
            trackOffset = offsetHeight + (thumbHeight - trackHeight) / 2;
            thumbOffset = offsetHeight;
        } else {
            final int offsetHeight = (paddedHeight - trackHeight) / 2;
            trackOffset = offsetHeight;
            thumbOffset = offsetHeight + (trackHeight - thumbHeight) / 2;
        }

        if (track != null) {
            final int trackWidth = w - getPaddingRight() - getPaddingLeft();
            track.setBounds(0, trackOffset, trackWidth, trackOffset + trackHeight);
        }

        if (thumb != null) {
            setThumbPos(w, thumb, getScale(), thumbOffset);
        }

        updateSplitProgress();
    }

    private void updateThumbAndTrackPosInVertical(int w, int h) {
        final int paddedWidth = w - getPaddingLeft() - getPaddingRight();
        final Drawable track = getCurrentDrawable();
        final Drawable thumb = mThumb;

        // The max width does not incorporate padding, whereas the width
        // parameter does.
        final int trackWidth = Math.min(mMaxWidth, paddedWidth);
        final int thumbWidth = thumb == null ? 0 : thumb.getIntrinsicWidth();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbWidth > trackWidth) {
            final int offsetHeight = (paddedWidth - thumbWidth) / 2;
            trackOffset = offsetHeight + (thumbWidth - trackWidth) / 2;
            thumbOffset = offsetHeight;
        } else {
            final int offsetHeight = (paddedWidth - trackWidth) / 2;
            trackOffset = offsetHeight;
            thumbOffset = offsetHeight + (trackWidth - thumbWidth) / 2;
        }

        if (track != null) {
            final int trackHeight = h - getPaddingBottom() - getPaddingTop();
            track.setBounds(trackOffset, 0, paddedWidth - trackOffset, trackHeight);
        }

        if (thumb != null) {
            setThumbPosInVertical(h, thumb, getScale(), thumbOffset);
        }
    }

    private float getScale() {
        int min = getMin();
        int max = getMax();
        int range = max - min;
        return range > 0 ? (getProgress() - min) / (float) range : 0;
    }

    /**
     * Updates the thumb drawable bounds.
     *
     * @param w Width of the view, including padding
     * @param thumb Drawable used for the thumb
     * @param scale Current progress between 0 and 1
     * @param offset Vertical offset for centering. If set to
     *            {@link Integer#MIN_VALUE}, the current offset will be used.
     */
    private void setThumbPos(int w, Drawable thumb, float scale, int offset) {
        if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
            setThumbPosInVertical(getHeight(), thumb, scale, offset);
            return;
        }

        int available = w - getPaddingLeft() - getPaddingRight();
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        final int thumbPos = (int) (scale * available + 0.5f);

        final int top, bottom;
        if (offset == Integer.MIN_VALUE) {
            final Rect oldBounds = thumb.getBounds();
            top = oldBounds.top;
            bottom = oldBounds.bottom;
        } else {
            top = offset;
            bottom = offset + thumbHeight;
        }

        final int left = (ViewUtils.isLayoutRtl(this) && mMirrorForRtl) ? available - thumbPos : thumbPos;
        final int right = left + thumbWidth;

        final Drawable background = getBackground();
        if (background != null) {
            final int offsetX = getPaddingLeft() - mThumbOffset;
            final int offsetY = getPaddingTop();
            DrawableCompat.setHotspotBounds(background, left + offsetX, top + offsetY,
                    right + offsetX, bottom + offsetY);
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, top, right, bottom);
        updateGestureExclusionRects();

        mThumbPosX = (left + getPaddingLeft()) - (getPaddingLeft() - (thumbWidth / 2));
        updateSplitProgress();
    }

    /**
     * Updates the thumb drawable bounds.
     *
     * @param h Width of the view, including padding
     * @param thumb Drawable used for the thumb
     * @param scale Current progress between 0 and 1
     * @param offset Vertical offset for centering. If set to
     *            {@link Integer#MIN_VALUE}, the current offset will be used.
     */
    private void setThumbPosInVertical(int h, Drawable thumb, float scale, int offset) {
        int available = h - getPaddingTop() - getPaddingBottom();
        final int thumbWidth = thumb.getIntrinsicHeight();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbHeight;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        final int thumbPos = (int) (scale * available + 0.5f);

        final int left, right;
        if (offset == Integer.MIN_VALUE) {
            final Rect oldBounds = thumb.getBounds();
            left = oldBounds.left;
            right = oldBounds.right;
        } else {
            left = offset;
            right = offset + thumbWidth;
        }

        final int top = available - thumbPos;
        final int bottom = top + thumbHeight;

        final Drawable background = getBackground();
        if (background != null) {
            final int offsetX = getPaddingLeft();
            final int offsetY = getPaddingTop() - mThumbOffset;
            DrawableCompat.setHotspotBounds(background, left + offsetX, top + offsetY,
                    right + offsetX, bottom + offsetY);
        }

        // Canvas will be translated, so 0,0 is where we start drawing
        thumb.setBounds(left, top, right, bottom);

        mThumbPosX = top + (thumbWidth / 2) + getPaddingLeft();
    }

    @Override
    public void setSystemGestureExclusionRects(@NonNull List<Rect> rects) {
        Preconditions.checkNotNull(rects, "rects must not be null");
        mUserGestureExclusionRects = rects;
        updateGestureExclusionRects();
    }

    private void updateGestureExclusionRects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final Drawable thumb = mThumb;
            if (thumb == null) {
                super.setSystemGestureExclusionRects(mUserGestureExclusionRects);
                return;
            }
            mGestureExclusionRects.clear();
            thumb.copyBounds(mThumbRect);
            mGestureExclusionRects.add(mThumbRect);
            mGestureExclusionRects.addAll(mUserGestureExclusionRects);
            super.setSystemGestureExclusionRects(mGestureExclusionRects);
        }
    }

    /**
     * @hide
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void onResolveDrawables(int layoutDirection) {
        super.onResolveDrawables(layoutDirection);

        if (mThumb != null) {
            DrawableCompat.setLayoutDirection(mThumb, layoutDirection);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (supportIsHoveringUIEnabled()) {
            final int hoverPopupType = getHoverPopupType();
            if (isHoverPopupTypeUserCustom(hoverPopupType) && mPreviousHoverPopupType != hoverPopupType) {
                mPreviousHoverPopupType = hoverPopupType;
                setHoverPopupGravity(HOVER_POPUP_WINDOW_GRAVITY_TOP_ABOVE
                        | HOVER_POPUP_WINDOW_GRAVITY_CENTER_HORIZONTAL_ON_POINT);
                setHoverPopupOffset(0, getMeasuredHeight() / 2);
                setHoverPopupDetectTime();
            }
        }
        if (mCurrentMode == MODE_SPLIT) {
            mSplitProgress.draw(canvas);
            mDivider.draw(canvas);
        }
        if (!mIsTouchDisabled) {
            drawThumb(canvas);
        }
    }

    @Override
    void drawTrack(Canvas canvas) {
        final Drawable thumbDrawable = mThumb;
        if (thumbDrawable != null && mSplitTrack) {
            final Rect insets = DrawableUtils.getOpticalBounds(thumbDrawable);
            final Rect tempRect = mTempRect;
            thumbDrawable.copyBounds(tempRect);
            tempRect.offset(getPaddingLeft() - mThumbOffset, getPaddingTop());
            tempRect.left += insets.left;
            tempRect.right -= insets.right;

            final int saveCount = canvas.save();
            canvas.clipRect(tempRect, Op.DIFFERENCE);
            super.drawTrack(canvas);
            drawTickMarks(canvas);
            canvas.restoreToCount(saveCount);
        } else {
            super.drawTrack(canvas);
            drawTickMarks(canvas);
        }

        if (!checkInvalidatedDualColorMode()) {
            canvas.save();
            if (mMirrorForRtl && ViewUtils.isLayoutRtl(this)) {
                canvas.translate(getWidth() - getPaddingRight(), getPaddingTop());
                canvas.scale(-1.0f, 1.0f);
            } else {
                canvas.translate(getPaddingLeft(), getPaddingTop());
            }

            final Rect bounds = mOverlapBackground.getBounds();
            final Rect tempRect = mTempRect;
            mOverlapBackground.copyBounds(tempRect);

            final int maxProgress;
            final int curProgress;
            if (mIsSeamless) {
                curProgress = Math.max(super.getProgress(), (int) (((float) mOverlapPoint) * SCALE_FACTOR));
                maxProgress = super.getMax();
            } else {
                curProgress = Math.max(getProgress(), mOverlapPoint);
                maxProgress = getMax();
            }
            if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
                tempRect.bottom = (int) (((float) bounds.bottom) - (((float) bounds.height())
                        * (((float) curProgress) / ((float) maxProgress))));
            } else {
                tempRect.left = (int) (((float) bounds.left) + (((float) bounds.width())
                        * (((float) curProgress) / ((float) maxProgress))));
            }
            canvas.clipRect(tempRect);
            if (mDefaultNormalProgressColor.getDefaultColor() != mOverlapNormalProgressColor.getDefaultColor()) {
                mOverlapBackground.draw(canvas);
            }
            canvas.restore();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected void drawTickMarks(Canvas canvas) {
        if (mTickMark != null) {
            final int count = getMax() - getMin();
            if (count > 1) {
                final int w = mTickMark.getIntrinsicWidth();
                final int h = mTickMark.getIntrinsicHeight();
                final int halfW = w >= 0 ? w / 2 : 1;
                final int halfH = h >= 0 ? h / 2 : 1;
                mTickMark.setBounds(-halfW, -halfH, halfW, halfH);

                final float spacing = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) count;
                final int saveCount = canvas.save();
                canvas.translate(getPaddingLeft(), getHeight() / 2f);
                for (int i = 0; i <= count; i++) {
                    mTickMark.draw(canvas);
                    canvas.translate(spacing, 0);
                }
                canvas.restoreToCount(saveCount);
            }
        }
    }

    /**
     * Draw the thumb.
     */
    void drawThumb(Canvas canvas) {
        if (mThumb != null) {
            final int saveCount = canvas.save();
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
                canvas.translate(getPaddingLeft(), getPaddingTop() - mThumbOffset);
            } else {
                canvas.translate(getPaddingLeft() - mThumbOffset, getPaddingTop());
            }
            mThumb.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getCurrentDrawable();

        int thumbHeight = mThumb == null ? 0 : mThumb.getIntrinsicHeight();
        int dw = 0;
        int dh = 0;
        if (d != null) {
            if (mCurrentMode != MODE_VERTICAL && mCurrentMode != MODE_EXPAND_VERTICAL) {
                dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
                dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
                dh = Math.max(thumbHeight, dh);
            } else {
                dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicWidth()));
                dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicHeight()));
                dw = Math.max(thumbHeight, dw);
            }
        }
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || mIsTouchDisabled || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsDraggingForSliding = false;
                if (mCurrentMode == MODE_EXPAND || mCurrentMode == MODE_EXPAND_VERTICAL
                        || supportIsInScrollingContainer()) {
                    mTouchDownX = event.getX();
                    mTouchDownY = event.getY();
                } else {
                    startDrag(event);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                mIsDraggingForSliding = true;
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    final float y = event.getY();
                    if (mCurrentMode != MODE_VERTICAL && mCurrentMode != MODE_EXPAND_VERTICAL
                            && Math.abs(x - mTouchDownX) > (float) mScaledTouchSlop
                            || (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL)
                            && Math.abs(y - mTouchDownY) > (float) mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDraggingForSliding) {
                    mIsDraggingForSliding = false;
                }
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                mIsDraggingForSliding = false;
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);

        if (mThumb != null) {
            // This may be within the padding region.
            invalidate(mThumb.getBounds());
        }

        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    private void setHotspot(float x, float y) {
        final Drawable bg = getBackground();
        if (bg != null) {
            DrawableCompat.setHotspot(bg, x, y);
        }
    }

    // TODO rework this method
    /* JADX WARN: Removed duplicated region for block: B:24:0x0078  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x009c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    //kang
    private void trackTouchEvent(MotionEvent event) {
        if (this.mCurrentMode != 3 && this.mCurrentMode != 6) {
            int var2;
            int var3;
            int var4;
            float var6;
            float var7;
            label55: {
                label63: {
                    var2 = Math.round(event.getX());
                    var3 = Math.round(event.getY());
                    var4 = this.getWidth();
                    int var5 = var4 - this.getPaddingLeft() - this.getPaddingRight();
                    if (ViewUtils.isLayoutRtl(this) && this.mMirrorForRtl) {
                        if (var2 > var4 - this.getPaddingRight()) {
                            break label63;
                        }

                        if (var2 >= this.getPaddingLeft()) {
                            var6 = (float)(var5 - var2 + this.getPaddingLeft()) / (float)var5;
                            var7 = this.mTouchProgressOffset;
                            break label55;
                        }
                    } else {
                        if (var2 < this.getPaddingLeft()) {
                            break label63;
                        }

                        if (var2 <= var4 - this.getPaddingRight()) {
                            var6 = (float)(var2 - this.getPaddingLeft()) / (float)var5;
                            var7 = this.mTouchProgressOffset;
                            break label55;
                        }
                    }

                    var7 = 0.0F;
                    var6 = 1.0F;
                    break label55;
                }

                var6 = 0.0F;
                var7 = var6;
            }

            float var8;
            float var9;
            float var10;
            float var11;
            if (this.mIsSeamless) {
                var8 = (float)(super.getMax() - super.getMin());
                var9 = 1.0F / var8;
                var10 = var6;
                if (var6 > 0.0F) {
                    var10 = var6;
                    if (var6 < 1.0F) {
                        var11 = var6 % var9;
                        var10 = var6;
                        if (var11 > var9 / 2.0F) {
                            var10 = var6 + (var9 - var11);
                        }
                    }
                }

                var6 = var10 * var8;
                var4 = super.getMin();
            } else {
                var8 = (float)(this.getMax() - this.getMin());
                var11 = 1.0F / var8;
                var10 = var6;
                if (var6 > 0.0F) {
                    var10 = var6;
                    if (var6 < 1.0F) {
                        var9 = var6 % var11;
                        var10 = var6;
                        if (var9 > var11 / 2.0F) {
                            var10 = var6 + (var11 - var9);
                        }
                    }
                }

                var6 = var10 * var8;
                var4 = this.getMin();
            }

            var10 = (float)var4;
            this.setHotspot((float)var2, (float)var3);
            this.setProgressInternal(Math.round(var7 + var6 + var10), true, false);
        } else {
            this.trackTouchEventInVertical(event);
        }
    }

    // TODO rework this method
    private void trackTouchEventInVertical(MotionEvent event) {
        int var2 = this.getHeight();
        int var3 = this.getPaddingTop();
        int var4 = this.getPaddingBottom();
        int var5 = Math.round(event.getX());
        int var6 = var2 - Math.round(event.getY());
        float var7;
        float var8;
        if (var6 < this.getPaddingBottom()) {
            var7 = 0.0F;
            var8 = var7;
        } else if (var6 > var2 - this.getPaddingTop()) {
            var8 = 0.0F;
            var7 = 1.0F;
        } else {
            var7 = (float)(var6 - this.getPaddingBottom()) / (float)(var2 - var3 - var4);
            var8 = this.mTouchProgressOffset;
        }

        float var9;
        float var10;
        float var11;
        float var12;
        if (this.mIsSeamless) {
            var9 = (float)(super.getMax() - super.getMin());
            var10 = 1.0F / var9;
            var11 = var7;
            if (var7 > 0.0F) {
                var11 = var7;
                if (var7 < 1.0F) {
                    var12 = var7 % var10;
                    var11 = var7;
                    if (var12 > var10 / 2.0F) {
                        var11 = var7 + (var10 - var12);
                    }
                }
            }

            var7 = var11 * var9;
            var3 = super.getMin();
        } else {
            var9 = (float)(this.getMax() - this.getMin());
            var10 = 1.0F / var9;
            var11 = var7;
            if (var7 > 0.0F) {
                var11 = var7;
                if (var7 < 1.0F) {
                    var12 = var7 % var10;
                    var11 = var7;
                    if (var12 > var10 / 2.0F) {
                        var11 = var7 + (var10 - var12);
                    }
                }
            }

            var7 = var11 * var9;
            var3 = this.getMin();
        }

        var11 = (float)var3;
        this.setHotspot((float)var5, (float)var6);
        this.setProgressInternal(Math.round(var8 + var7 + var11), true, false);
    }
    //kang

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
        }
    }

    /**
     * This is called when the user either releases their touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
        if (mIsSeamless && isPressed()) {
            mValueAnimator = ValueAnimator.ofInt(super.getProgress(),
                    (int) (Math.round(super.getProgress() / SCALE_FACTOR) * SCALE_FACTOR));
            mValueAnimator.setDuration(300);
            mValueAnimator.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_90);
            mValueAnimator.start();
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    callSuperSetProgress((Integer) animation.getAnimatedValue());
                }
            });
        } else if (mIsSeamless) {
            setProgress(Math.round(super.getProgress() / SCALE_FACTOR));
        }
    }

    private void callSuperSetProgress(int progress) {
        super.setProgress(progress);
    }

    /**
     * Called when the user changes the seekbar's progress by using a key event.
     */
    void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            int increment = mKeyProgressIncrement;
            if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_MINUS:
                        increment = -increment;
                        // fallthrough
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_PLUS:
                    case KeyEvent.KEYCODE_EQUALS:
                        increment = ViewUtils.isLayoutRtl(this) ? -increment : increment;

                        final int newProgress = mIsSeamless ?
                                Math.round((getProgress() + increment) * SCALE_FACTOR)
                                : getProgress() + increment;

                        if (setProgressInternal(newProgress, true, true)) {
                            onKeyChange();
                            return true;
                        }
                        break;
                }
            } else {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_MINUS:
                        increment = -increment;
                        // fallthrough
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_PLUS:
                    case KeyEvent.KEYCODE_EQUALS:
                        increment = ViewUtils.isLayoutRtl(this) ? -increment : increment;

                        final int newProgress = mIsSeamless ?
                                Math.round((getProgress() + increment) * SCALE_FACTOR)
                                : getProgress() + increment;

                        if (setProgressInternal(newProgress, true, true)) {
                            onKeyChange();
                            return true;
                        }
                        break;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        Log.d(TAG, "Stack:", new Throwable("stack dump"));
        return android.widget.AbsSeekBar.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        if (isEnabled()) {
            final int progress = getProgress();
            if (progress > getMin()) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
            }
            if (progress < getMax()) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            }
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }

        if (!isEnabled()) {
            return false;
        }

        switch (action) {
            case android.R.id.accessibilityActionSetProgress: {
                if (!canUserSetProgress()) {
                    return false;
                }
                if (arguments == null || !arguments.containsKey(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)) {
                    return false;
                }
                float value = arguments.getFloat(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE);
                if (mIsSeamless) {
                    value = Math.round(value * SCALE_FACTOR);
                }
                return setProgressInternal((int) value, true, true);
            }
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                if (!canUserSetProgress()) {
                    return false;
                }
                int range = getMax() - getMin();
                int increment = Math.max(1, Math.round((float) range / 20));
                if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                    increment = -increment;
                }

                // Let progress bar handle clamping values.
                final int newProgress = mIsSeamless ?
                        Math.round((getProgress() + increment) * SCALE_FACTOR)
                        : getProgress() + increment;
                if (setProgressInternal(newProgress, true, true)) {
                    onKeyChange();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * @return whether user can change progress on the view
     */
    boolean canUserSetProgress() {
        return !isIndeterminate() && isEnabled();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        final Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(getWidth(), thumb, getScale(), Integer.MIN_VALUE);

            // Since we draw translated, the drawable's bounds that it signals
            // for invalidation won't be the actual bounds we want invalidated,
            // so just invalidate this whole view.
            invalidate();
        }
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser, int progress) {
        final int targetLevel = (int) (10000 * scale);

        final boolean isMuteAnimationNeeded = mUseMuteAnimation && !mIsFirstSetProgress && !mIsDraggingForSliding;
        if (!isMuteAnimationNeeded || mCurrentProgressLevel == 0 || targetLevel != 0) {
            cancelMuteAnimation();
            mIsFirstSetProgress = false;
            mCurrentProgressLevel = targetLevel;

            super.onProgressRefresh(scale, fromUser, progress);

            Drawable thumb = mThumb;
            if (thumb != null) {
                setThumbPos(getWidth(), thumb, scale, Integer.MIN_VALUE);
                invalidate();
            }
        } else {
            startMuteAnimation();
        }
    }

    public void setThumbTintColor(int color) {
        ColorStateList mOverlapColor = colorToColorStateList(color);
        if (!mOverlapColor.equals(mDefaultActivatedThumbColor)) {
            mDefaultActivatedThumbColor = mOverlapColor;
        }
    }

    private void updateSplitProgress() {
        if (mCurrentMode == MODE_SPLIT) {
            Drawable d = mSplitProgress;
            Rect base = getCurrentDrawable().getBounds();
            if (d != null) {
                if (!mMirrorForRtl || !ViewUtils.isLayoutRtl(this)) {
                    d.setBounds(getPaddingLeft(), base.top, mThumbPosX, base.bottom);
                } else {
                    d.setBounds(mThumbPosX, base.top, getWidth() - getPaddingRight(), base.bottom);
                }
            }

            final int w = getWidth();
            final int h = getHeight();
            if (mDivider != null) {
                mDivider.setBounds((int) ((w / 2.0f) - ((mDensity * 4.0f) / 2.0f)),
                        (int) ((h / 2.0f) - ((mDensity * 22.0f) / 2.0f)),
                        (int) ((w / 2.0f) + ((mDensity * 4.0f) / 2.0f)),
                        (int) ((h / 2.0f) + ((mDensity * 22.0f) / 2.0f)));
            }
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean setProgressInternal(int progress, boolean fromUser, boolean animate) {
        boolean superRet = super.setProgressInternal(progress, fromUser, animate);
        updateWarningMode(progress);
        updateDualColorMode();
        return superRet;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onStartTrackingHover(int hoverLevel, int posX, int posY) {
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onStopTrackingHover() {
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onHoverChanged(int hoverLevel, int posX, int posY) {
    }

    private void trackHoverEvent(int posX) {
        final float scale;
        final int width = getWidth();
        final int available = width - getPaddingLeft() - getPaddingRight();
        float hoverLevel = 0.0f;
        if (posX < getPaddingLeft()) {
            scale = 0.0f;
        } else if (posX > width - getPaddingRight()) {
            scale = 1.0f;
        } else {
            scale = (posX - getPaddingLeft()) / available;
            hoverLevel = mTouchProgressOffset;
        }
        final int max = getMax();
        mHoveringLevel = (int) (hoverLevel + (max * scale));
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean onHoverEvent(MotionEvent event) {
        if (supportIsHoveringUIEnabled()) {
            final int action = event.getAction();
            final int x = (int) event.getX();
            final int y = (int) event.getY();

            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    trackHoverEvent(x);
                    onStartTrackingHover(mHoveringLevel, x, y);
                    break;
                case MotionEvent.ACTION_HOVER_MOVE:
                    trackHoverEvent(x);
                    onHoverChanged(mHoveringLevel, x, y);
                    if (isHoverPopupTypeUserCustom(getHoverPopupType())) {
                        setHoveringPoint((int) event.getRawX(), (int) event.getRawY());
                        updateHoverPopup();
                    }
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    onStopTrackingHover();
                    break;
            }
        }

        return super.onHoverEvent(event);
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setProgressDrawable(Drawable d) {
        super.setProgressDrawable(d);
    }

    public Rect getThumbBounds() {
        if (mThumb != null) {
            return mThumb.getBounds();
        }
        return null;
    }

    public int getThumbHeight() {
        return mThumb.getIntrinsicHeight();
    }

    @Override
    public void setMode(int mode) {
        if (mCurrentMode != mode || !mIsSetModeCalled) {
            super.setMode(mode);

            switch (mode) {
                case MODE_STANDARD:
                    setProgressTintList(mDefaultActivatedProgressColor);
                    setThumbTintList(mDefaultActivatedThumbColor);
                    break;
                case MODE_WARNING:
                    updateWarningMode(getProgress());
                    break;
                case MODE_VERTICAL:
                    setThumb(getContext().getResources().getDrawable(mIsLightTheme ?
                            R.drawable.sesl_scrubber_control_anim_light
                            : R.drawable.sesl_scrubber_control_anim_dark));
                    break;
                case MODE_SPLIT:
                    mSplitProgress = getContext().getResources()
                            .getDrawable(R.drawable.sesl_split_seekbar_primary_progress);
                    mDivider = getContext().getResources()
                            .getDrawable(R.drawable.sesl_split_seekbar_vertical_bar);
                    updateSplitProgress();
                    break;
                case MODE_EXPAND:
                    initializeExpandMode();
                    break;
                case MODE_EXPAND_VERTICAL:
                    initializeExpandVerticalMode();
                    break;
            }

            invalidate();
            mIsSetModeCalled = true;
        } else {
            Log.w(TAG, "Seekbar mode is already set. Do not call this method redundant");
        }
    }

    private void initializeExpandMode() {
        SliderDrawable background =
                new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mDefaultNormalProgressColor);
        SliderDrawable secondaryProgress =
                new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mDefaultSecondaryProgressColor);
        SliderDrawable primaryProgress =
                new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mDefaultActivatedProgressColor);
        Drawable thumbDrawable =
                new DrawableWrapper(new ThumbDrawable(mThumbRadius, mDefaultActivatedThumbColor, false));

        Drawable[] drawables = {background,
                new ClipDrawable(secondaryProgress, Gravity.CENTER_VERTICAL | Gravity.LEFT, ClipDrawable.HORIZONTAL),
                new ClipDrawable(primaryProgress, Gravity.CENTER_VERTICAL | Gravity.LEFT, ClipDrawable.HORIZONTAL)};
        LayerDrawable layer = new LayerDrawable(drawables);
        layer.setPaddingMode(LayerDrawable.PADDING_MODE_STACK);
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.secondaryProgress);
        layer.setId(2, android.R.id.progress);

        setProgressDrawable(layer);
        setThumb(thumbDrawable);
        setBackgroundResource(R.drawable.sesl_seekbar_background_borderless_expand);

        if (getMaxHeight() > mTrackMaxWidth) {
            setMaxHeight(mTrackMaxWidth);
        }
    }

    private void initializeExpandVerticalMode() {
        SliderDrawable background =
                new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mDefaultNormalProgressColor, true);
        SliderDrawable secondaryProgress =
                new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mDefaultSecondaryProgressColor, true);
        SliderDrawable primaryProgress =
                new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mDefaultActivatedProgressColor, true);
        Drawable thumbDrawable =
                new DrawableWrapper(new ThumbDrawable(mThumbRadius, mDefaultActivatedThumbColor, true));

        Drawable[] drawables = {background,
                new ClipDrawable(secondaryProgress, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, ClipDrawable.VERTICAL),
                new ClipDrawable(primaryProgress, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, ClipDrawable.VERTICAL)};
        LayerDrawable layer = new LayerDrawable(drawables);
        layer.setPaddingMode(LayerDrawable.PADDING_MODE_STACK);
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.secondaryProgress);
        layer.setId(2, android.R.id.progress);

        setProgressDrawable(layer);
        setThumb(thumbDrawable);
        setBackgroundResource(R.drawable.sesl_seekbar_background_borderless_expand);

        if (getMaxWidth() > mTrackMaxWidth) {
            setMaxWidth(mTrackMaxWidth);
        }
    }

    @Deprecated
    public void setOverlapBackgroundForDualColor(int color) {
        ColorStateList mOverlapColor = colorToColorStateList(color);
        if (!mOverlapColor.equals(mOverlapNormalProgressColor)) {
            mOverlapNormalProgressColor = mOverlapColor;
        }
        mOverlapActivatedProgressColor = mOverlapNormalProgressColor;
        mLargeFont = true;
    }

    public void setOverlapPointForDualColor(int value) {
        if (value < getMax()) {
            mSetDualColorMode = true;
            mOverlapPoint = value;
            if (value == -1) {
                setProgressTintList(mDefaultActivatedProgressColor);
                setThumbTintList(mDefaultActivatedThumbColor);
            } else {
                if (mOverlapBackground == null) {
                    initDualOverlapDrawable();
                }
                updateDualColorMode();
            }

            invalidate();
        }
    }

    private void updateDualColorMode() {
        if (!checkInvalidatedDualColorMode()) {
            DrawableCompat.setTintList(mOverlapBackground, mOverlapNormalProgressColor);
            if (!mLargeFont) {
                final boolean setOverlapColor = mIsSeamless ?
                        super.getProgress() > mOverlapPoint * SCALE_FACTOR
                        : getProgress() > mOverlapPoint;

                if (setOverlapColor) {
                    setProgressOverlapTintList(mOverlapActivatedProgressColor);
                    setThumbOverlapTintList(mOverlapActivatedProgressColor);
                } else {
                    setProgressTintList(mDefaultActivatedProgressColor);
                    setThumbTintList(mDefaultActivatedThumbColor);
                }
            }
            updateBoundsForDualColor();
        }
    }

    private void updateBoundsForDualColor() {
        if (getCurrentDrawable() != null && !checkInvalidatedDualColorMode()) {
            Rect base = getCurrentDrawable().getBounds();
            mOverlapBackground.setBounds(base);
        }
    }

    public void setDualModeOverlapColor(int bgColor, int fgColor) {
        ColorStateList mOverlapBackgroundColor = colorToColorStateList(bgColor);
        ColorStateList mOverlapForegroundColor = colorToColorStateList(fgColor);
        if (!mOverlapBackgroundColor.equals(mOverlapNormalProgressColor)) {
            mOverlapNormalProgressColor = mOverlapBackgroundColor;
        }
        if (!mOverlapForegroundColor.equals(mOverlapActivatedProgressColor)) {
            mOverlapActivatedProgressColor = mOverlapForegroundColor;
        }

        updateDualColorMode();
        invalidate();
    }

    private boolean checkInvalidatedDualColorMode() {
        return mOverlapPoint == -1 || mOverlapBackground == null;
    }

    private void initDualOverlapDrawable() {
        if (mCurrentMode == MODE_EXPAND) {
            mOverlapBackground = new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mOverlapNormalProgressColor);
        } else if (mCurrentMode == MODE_EXPAND_VERTICAL) {
            mOverlapBackground = new SliderDrawable(mTrackMinWidth, mTrackMaxWidth, mOverlapNormalProgressColor, true);
        } else if (getProgressDrawable() != null && getProgressDrawable().getConstantState() != null) {
            mOverlapBackground = getProgressDrawable().getConstantState().newDrawable().mutate();
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected void updateDrawableBounds(int w, int h) {
        super.updateDrawableBounds(w, h);
        updateThumbAndTrackPos(w, h);
        updateBoundsForDualColor();
    }

    private ColorStateList colorToColorStateList(int color) {
        int[][] EMPTY = {new int[0]};
        return new ColorStateList(EMPTY, new int[]{color});
    }

    private void updateWarningMode(int progress) {
        if (mCurrentMode == MODE_WARNING) {
            final boolean isMax = progress == getMax();
            if (isMax) {
                setProgressOverlapTintList(mOverlapActivatedProgressColor);
                setThumbOverlapTintList(mOverlapActivatedProgressColor);
            } else {
                setProgressTintList(mDefaultActivatedProgressColor);
                setThumbTintList(mDefaultActivatedThumbColor);
            }
        }
    }

    private void initMuteAnimation() {
        mMuteAnimationSet = new AnimatorSet();
        List<Animator> list = new ArrayList();

        int distance = MUTE_VIB_DISTANCE_LVL;
        for (int i = 0; i < 8; i++) {
            final boolean isGoingDirection = i % 2 == 0;

            ValueAnimator progressZeroAnimation = isGoingDirection ?
                    ValueAnimator.ofInt(0, distance) : ValueAnimator.ofInt(distance, 0);
            progressZeroAnimation.setDuration(62);
            progressZeroAnimation.setInterpolator(new LinearInterpolator());
            progressZeroAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentProgressLevel = (Integer) animation.getAnimatedValue();
                    onSlidingRefresh(mCurrentProgressLevel);
                }
            });

            list.add(progressZeroAnimation);

            if (isGoingDirection) {
                distance = (int) (distance * 0.6d);
            }
        }

        mMuteAnimationSet.playSequentially(list);
    }

    private void cancelMuteAnimation() {
        if (mMuteAnimationSet != null && mMuteAnimationSet.isRunning()) {
            mMuteAnimationSet.cancel();
        }
    }

    private void startMuteAnimation() {
        cancelMuteAnimation();
        if (mMuteAnimationSet != null) {
            mMuteAnimationSet.start();
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected void onSlidingRefresh(int level) {
        super.onSlidingRefresh(level);

        final float scale = level / 10000.0f;
        Drawable thumb = mThumb;
        if (thumb != null) {
            setThumbPos(getWidth(), thumb, scale, Integer.MIN_VALUE);
            invalidate();
        }
    }

    private void setThumbOverlapTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;
        applyThumbTint();
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setProgressTintList(@Nullable ColorStateList tint) {
        super.setProgressTintList(tint);
        mDefaultActivatedProgressColor = tint;
    }

    private void setProgressOverlapTintList(@Nullable ColorStateList tint) {
        super.setProgressTintList(tint);
    }

    private boolean supportIsHoveringUIEnabled() {
        return IS_BASE_SDK_VERSION && SeslViewReflector.isHoveringUIEnabled(this);
    }

    private void setHoverPopupGravity(int gravity) {
        if (IS_BASE_SDK_VERSION) {
            Object hoverPopupWindow = SeslViewReflector.semGetHoverPopup(this, true);
            SeslHoverPopupWindowReflector.setGravity(hoverPopupWindow, gravity);
        }
    }

    private void setHoverPopupOffset(int x, int y) {
        if (IS_BASE_SDK_VERSION) {
            Object hoverPopupWindow = SeslViewReflector.semGetHoverPopup(this, true);
            SeslHoverPopupWindowReflector.setOffset(hoverPopupWindow, x, y);
        }
    }

    private void setHoverPopupDetectTime() {
        if (IS_BASE_SDK_VERSION) {
            Object hoverPopupWindow = SeslViewReflector.semGetHoverPopup(this, true);
            SeslHoverPopupWindowReflector.setHoverDetectTime(hoverPopupWindow, HOVER_DETECT_TIME);
        }
    }

    private void setHoveringPoint(int x, int y) {
        if (IS_BASE_SDK_VERSION) {
            SeslHoverPopupWindowReflector.setHoveringPoint(this, x, y);
        }
    }

    public void updateHoverPopup() {
        if (IS_BASE_SDK_VERSION) {
            Object hoverPopupWindow = SeslViewReflector.semGetHoverPopup(this, true);
            SeslHoverPopupWindowReflector.update(hoverPopupWindow);
        }
    }

    private boolean isHoverPopupTypeUserCustom(int type) {
        return IS_BASE_SDK_VERSION
                && type == SeslHoverPopupWindowReflector.getField_TYPE_USER_CUSTOM();
    }

    private int getHoverPopupType() {
        if (IS_BASE_SDK_VERSION) {
            return SeslViewReflector.semGetHoverPopupType(this);
        }
        return 0;
    }

    private boolean supportIsInScrollingContainer() {
        return SeslViewReflector.isInScrollingContainer(this);
    }

    @Override
    public synchronized int getProgress() {
        if (mIsSeamless) {
            return Math.round(super.getProgress() / SCALE_FACTOR);
        } else  {
            return super.getProgress();
        }
    }

    @Override
    public synchronized int getMin() {
        if (mIsSeamless) {
            return Math.round(super.getMin() / SCALE_FACTOR);
        } else  {
            return super.getMin();
        }
    }

    @Override
    public synchronized int getMax() {
        if (mIsSeamless) {
            return Math.round(super.getMax() / SCALE_FACTOR);
        } else  {
            return super.getMax();
        }
    }

    @Override
    public synchronized void setProgress(int progress) {
        if (mIsSeamless) {
            progress = Math.round(progress * SCALE_FACTOR);
        }
        super.setProgress(progress);
    }

    @Override
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        if (mIsSeamless) {
            secondaryProgress = Math.round(secondaryProgress * SCALE_FACTOR);
        }
        super.setSecondaryProgress(secondaryProgress);
    }

    public void setSeamless(boolean seamless) {
        if (mIsSeamless != seamless) {
            mIsSeamless = seamless;
            if (seamless) {
                super.setMax(Math.round(super.getMax() * SCALE_FACTOR));
                super.setMin(Math.round(super.getMin() * SCALE_FACTOR));
                super.setProgress(Math.round(super.getProgress() * SCALE_FACTOR));
                super.setSecondaryProgress(Math.round(super.getSecondaryProgress() * SCALE_FACTOR));
            } else {
                super.setProgress(Math.round(super.getProgress() / SCALE_FACTOR));
                super.setSecondaryProgress(Math.round(super.getSecondaryProgress() / SCALE_FACTOR));
                super.setMax(Math.round(super.getMax() / SCALE_FACTOR));
                super.setMin(Math.round(super.getMin() / SCALE_FACTOR));
            }
        }
    }


    private class SliderDrawable extends Drawable {
        private final int ANIMATION_DURATION = 250;

        private final SliderState mState = new SliderState();
        ColorStateList mColorStateList;
        private final Paint mPaint = new Paint();
        ValueAnimator mPressedAnimator;
        ValueAnimator mReleasedAnimator;

        private boolean mIsStateChanged = false;
        private boolean mIsVertical;

        int mAlpha = 0xff;
        int mColor;

        private final float mSliderMaxWidth;
        private final float mSliderMinWidth;
        private float mRadius;

        public SliderDrawable(float minWidth, float maxWidth, ColorStateList color) {
            this(minWidth, maxWidth, color, false);
        }

        public SliderDrawable(float minWidth, float maxWidth, ColorStateList color, boolean isVertical) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setAntiAlias(true);

            mColorStateList = color;
            mColor = color.getDefaultColor();
            mPaint.setColor(mColor);

            mPaint.setStrokeWidth(minWidth);
            mSliderMinWidth = minWidth;
            mSliderMaxWidth = maxWidth;
            mRadius = minWidth / 2.0f;

            mIsVertical = isVertical;

            initAnimator();
        }

        private void initAnimator() {
            float tempTrackMinWidth = mSliderMinWidth;
            float tempTrackMaxWidth = mSliderMaxWidth;

            mPressedAnimator = ValueAnimator.ofFloat(tempTrackMinWidth, tempTrackMaxWidth);
            mPressedAnimator.setDuration(ANIMATION_DURATION);
            mPressedAnimator.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_80);
            mPressedAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    final float value = (Float) valueAnimator.getAnimatedValue();
                    invalidateTrack(value);
                }
            });

            mReleasedAnimator = ValueAnimator.ofFloat(tempTrackMaxWidth, tempTrackMinWidth);
            mReleasedAnimator.setDuration(ANIMATION_DURATION);
            mReleasedAnimator.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_80);
            mReleasedAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    final float value = (Float) valueAnimator.getAnimatedValue();
                    invalidateTrack(value);
                }
            });
        }

        @Override
        public void draw(Canvas canvas) {
            final int prevAlpha = mPaint.getAlpha();
            mPaint.setAlpha(modulateAlpha(prevAlpha, mAlpha));

            canvas.save();
            if (!mIsVertical) {
                final float width = SeslAbsSeekBar.this.getWidth() - SeslAbsSeekBar.this.getPaddingLeft()
                        - SeslAbsSeekBar.this.getPaddingRight() - mRadius;
                canvas.drawLine(mRadius, SeslAbsSeekBar.this.getHeight() / 2.0f, width,
                        SeslAbsSeekBar.this.getHeight() / 2.0f, mPaint);
            } else {
                final float height = SeslAbsSeekBar.this.getHeight() - SeslAbsSeekBar.this.getPaddingTop()
                        - SeslAbsSeekBar.this.getPaddingBottom() - mRadius;
                canvas.drawLine(SeslAbsSeekBar.this.getWidth() / 2.0f, height,
                        SeslAbsSeekBar.this.getWidth() / 2.0f, mRadius, mPaint);
            }
            canvas.restore();

            mPaint.setAlpha(prevAlpha);
        }

        private int modulateAlpha(int paintAlpha, int alpha) {
            int scale = alpha + (alpha >>> 7);
            return (paintAlpha * scale) >>> 8;
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            Paint p = mPaint;
            if (p.getXfermode() != null) {
                return PixelFormat.TRANSLUCENT;
            }
            final int alpha = p.getAlpha();
            if (alpha == 0) {
                return PixelFormat.TRANSPARENT;
            }
            if (alpha == 255) {
                return PixelFormat.OPAQUE;
            }
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setTintList(ColorStateList tint) {
            super.setTintList(tint);
            if (tint != null) {
                mColorStateList = tint;
                mColor = mColorStateList.getDefaultColor();
                mPaint.setColor(mColor);
                invalidateSelf();
            }
        }

        @Override
        protected boolean onStateChange(int[] stateSet) {
            final boolean changed = super.onStateChange(stateSet);

            final int color = mColorStateList.getColorForState(stateSet, mColor);
            if (mColor != color) {
                mColor = color;
                mPaint.setColor(color);
                invalidateSelf();
            }

            boolean enabled = false;
            boolean pressed = false;
            for (int i : stateSet) {
                if (i == android.R.attr.state_enabled) {
                    enabled = true;
                } else if (i == android.R.attr.state_pressed) {
                    pressed = true;
                }
            }

            startSliderAnimation(enabled && pressed);

            return changed;
        }

        @Override
        public int getIntrinsicWidth() {
            return (int) mSliderMaxWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return (int) mSliderMaxWidth;
        }

        public void setStrokeWidth(float height) {
            mPaint.setStrokeWidth(height);
            mRadius = height / 2.0f;
        }

        private void startSliderAnimation(boolean isChanged) {
            if (mIsStateChanged != isChanged) {
                if (isChanged) {
                    startPressedAnimation();
                } else {
                    startReleasedAnimation();
                }
                mIsStateChanged = isChanged;
            }
        }

        private void startPressedAnimation() {
            if (!mPressedAnimator.isRunning()) {
                if (mReleasedAnimator.isRunning()) {
                    mReleasedAnimator.cancel();
                }
                mPressedAnimator.setFloatValues(mSliderMinWidth, mSliderMaxWidth);
                mPressedAnimator.start();
            }
        }

        private void startReleasedAnimation() {
            if (!mReleasedAnimator.isRunning()) {
                if (mPressedAnimator.isRunning()) {
                    mPressedAnimator.cancel();
                }
                mReleasedAnimator.setFloatValues(mSliderMaxWidth, mSliderMinWidth);
                mReleasedAnimator.start();
            }
        }

        void invalidateTrack(float value) {
            setStrokeWidth(value);
            invalidateSelf();
        }

        @Override
        public Drawable.ConstantState getConstantState() {
            return mState;
        }

        private class SliderState extends Drawable.ConstantState {
            @Override
            public Drawable newDrawable() {
                return SliderDrawable.this;
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        }
    }

    private class ThumbDrawable extends Drawable {
        private final int PRESSED_DURATION = 100;
        private final int RELEASED_DURATION = 300;

        private ColorStateList mColorStateList;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator mThumbPressed;
        private ValueAnimator mThumbReleased;

        private boolean mIsVertical = false;
        private boolean mIsStateChanged = false;

        private int mAlpha = 0xff;
        int mColor;
        private final int mRadius;
        private int mRadiusForAni;

        public ThumbDrawable(int radius, ColorStateList color, boolean isVertical) {
            mRadiusForAni = radius;
            mRadius = radius;
            mColorStateList = color;
            mColor = color.getDefaultColor();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mColor);
            mIsVertical = isVertical;
            initAnimation();
        }

        void initAnimation() {
            mThumbPressed = ValueAnimator.ofFloat(mRadius, 0.0f);
            mThumbPressed.setDuration(PRESSED_DURATION);
            mThumbPressed.setInterpolator(new LinearInterpolator());
            mThumbPressed.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float value = (Float) animation.getAnimatedValue();
                    setRadius((int) value);
                    invalidateSelf();
                }
            });

            mThumbReleased = ValueAnimator.ofFloat(0.0f, mRadius);
            mThumbReleased.setDuration(RELEASED_DURATION);
            mThumbReleased.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_90);
            mThumbReleased.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float value = (Float) animation.getAnimatedValue();
                    setRadius((int) value);
                    invalidateSelf();
                }
            });
        }

        @Override
        public void draw(Canvas canvas) {
            final int prevAlpha = mPaint.getAlpha();
            mPaint.setAlpha(modulateAlpha(prevAlpha, mAlpha));

            canvas.save();
            if (!mIsVertical) {
                canvas.drawCircle(SeslAbsSeekBar.this.mThumbPosX,
                        SeslAbsSeekBar.this.getHeight() / 2.0f, mRadiusForAni, mPaint);
            } else {
                canvas.drawCircle(SeslAbsSeekBar.this.getWidth() / 2.0f,
                        SeslAbsSeekBar.this.mThumbPosX, mRadiusForAni, mPaint);
            }
            canvas.restore();

            mPaint.setAlpha(prevAlpha);
        }

        @Override
        public int getIntrinsicWidth() {
            return mRadius * 2;
        }

        @Override
        public int getIntrinsicHeight() {
            return mRadius * 2;
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        public void setTintList(ColorStateList tint) {
            super.setTintList(tint);
            if (tint != null) {
                mColorStateList = tint;
                mColor = tint.getColorForState(SeslAbsSeekBar.this.getDrawableState(), mColor);
                mPaint.setColor(mColor);
                invalidateSelf();
            }
        }

        @Override
        protected boolean onStateChange(int[] stateSet) {
            final boolean changed = super.onStateChange(stateSet);

            final int color = mColorStateList.getColorForState(stateSet, mColor);
            if (mColor != color) {
                mColor = color;
                mPaint.setColor(color);
                invalidateSelf();
            }

            boolean enabled = false;
            boolean pressed = false;
            for (int i : stateSet) {
                if (i == android.R.attr.state_enabled) {
                    enabled = true;
                } else if (i == android.R.attr.state_pressed) {
                    pressed = true;
                }
            }

            startThumbAnimation(enabled && pressed);

            return changed;
        }

        private void startThumbAnimation(boolean isChanged) {
            if (mIsStateChanged != isChanged) {
                if (isChanged) {
                    startPressedAnimation();
                } else {
                    startReleasedAnimation();
                }
                mIsStateChanged = isChanged;
            }
        }

        private void startPressedAnimation() {
            if (!mThumbPressed.isRunning()) {
                if (mThumbReleased.isRunning()) {
                    mThumbReleased.cancel();
                }
                mThumbPressed.start();
            }
        }

        private void startReleasedAnimation() {
            if (!mThumbReleased.isRunning()) {
                if (mThumbPressed.isRunning()) {
                    mThumbPressed.cancel();
                }
                mThumbReleased.start();
            }
        }

        private void setRadius(int radius) {
            mRadiusForAni = radius;
        }

        @Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }

        private int modulateAlpha(int paintAlpha, int alpha) {
            int scale = alpha + (alpha >>> 7);
            return (paintAlpha * scale) >>> 8;
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            Paint p = mPaint;
            if (p.getXfermode() != null) {
                return PixelFormat.TRANSLUCENT;
            }
            final int alpha = p.getAlpha();
            if (alpha == 0) {
                return PixelFormat.TRANSPARENT;
            }
            if (alpha == 255) {
                return PixelFormat.OPAQUE;
            }
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Override
    public int getPaddingLeft() {
        final boolean isInVerticalMode = mCurrentMode == MODE_VERTICAL
                || mCurrentMode == MODE_EXPAND_VERTICAL;
        return isInVerticalMode ? super.getPaddingTop()
                : super.getPaddingLeft();
    }

    @Override
    public int getPaddingTop() {
        final boolean isInVerticalMode = mCurrentMode == MODE_VERTICAL
                || mCurrentMode == MODE_EXPAND_VERTICAL;
        return isInVerticalMode ? super.getPaddingLeft()
                : super.getPaddingTop();
    }

    @Override
    public int getPaddingRight() {
        final boolean isInVerticalMode = mCurrentMode == MODE_VERTICAL
                || mCurrentMode == MODE_EXPAND_VERTICAL;
        return isInVerticalMode ? super.getPaddingBottom()
                : super.getPaddingRight();
    }

    @Override
    public int getPaddingBottom() {
        final boolean isInVerticalMode = mCurrentMode == MODE_VERTICAL
                || mCurrentMode == MODE_EXPAND_VERTICAL;
        return isInVerticalMode ? super.getPaddingRight()
                : super.getPaddingBottom();
    }
}
