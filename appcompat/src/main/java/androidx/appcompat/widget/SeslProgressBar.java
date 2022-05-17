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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.RemoteViews.RemoteView;

import androidx.annotation.IntDef;
import androidx.annotation.InterpolatorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.math.MathUtils;
import androidx.core.util.Pools.SynchronizedPool;
import androidx.core.view.ViewCompat;
import androidx.reflect.graphics.drawable.SeslStateListDrawableReflector;
import androidx.reflect.view.SeslViewReflector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung ProgressBar class.
 */
@RemoteView
public class SeslProgressBar extends View {

    private static final int MAX_LEVEL = 10000;

    private static final int TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200;

    /** Interpolator used for smooth progress animations. */
    private static final DecelerateInterpolator PROGRESS_ANIM_INTERPOLATOR =
            new DecelerateInterpolator();

    /** Duration of smooth progress animations. */
    private static final int PROGRESS_ANIM_DURATION = 80;

    /** Samsung ProgressBar modes */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({MODE_STANDARD,
            MODE_WARNING,
            MODE_DUAL_COLOR,
            MODE_VERTICAL,
            MODE_SPLIT,
            MODE_EXPAND,
            MODE_EXPAND_VERTICAL,
            MODE_CIRCLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekBarMode { }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected static final int MODE_STANDARD = 0;
    public static final int MODE_WARNING = 1;
    public static final int MODE_DUAL_COLOR = 2;
    public static final int MODE_VERTICAL = 3;
    public static final int MODE_SPLIT = 4;
    public static final int MODE_EXPAND = 5;
    public static final int MODE_EXPAND_VERTICAL = 6;
    public static final int MODE_CIRCLE = 7;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int mMinWidth;
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int mMaxWidth;
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int mMinHeight;
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int mMaxHeight;

    private int mRoundStrokeWidth;
    private int mCirclePadding;

    private int mProgress;
    private int mSecondaryProgress;
    private int mMin;
    private boolean mMinInitialized;
    private int mMax;
    private boolean mMaxInitialized;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected int mCurrentMode = MODE_STANDARD;

    private int mBehavior;
    // Better to define a Drawable that implements Animatable if you want to modify animation
    // characteristics programatically.
    private int mDuration;
    private boolean mIndeterminate;
    private boolean mOnlyIndeterminate;
    private Transformation mTransformation;
    private AlphaAnimation mAnimation;
    private boolean mHasAnimation;

    private Drawable mIndeterminateDrawable;
    private Drawable mProgressDrawable;
    private boolean mUseHorizontalProgress = false;

    private Drawable mIndeterminateHorizontalXsmall;
    private Drawable mIndeterminateHorizontalSmall;
    private Drawable mIndeterminateHorizontalMedium;
    private Drawable mIndeterminateHorizontalLarge;
    private Drawable mIndeterminateHorizontalXlarge;
    /**
     * Please use {@link #getCurrentDrawable()}, {@link #setProgressDrawable(Drawable)},
     * {@link #setIndeterminateDrawable(Drawable)} and their tiled versions instead of
     * accessing this directly.
     */
    private Drawable mCurrentDrawable;
    private ProgressTintInfo mProgressTintInfo;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected float mDensity;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int mSampleWidth = 0;
    private boolean mNoInvalidate;
    private Interpolator mInterpolator;
    private RefreshProgressRunnable mRefreshProgressRunnable;
    private long mUiThreadId;
    private boolean mShouldStartAnimationDrawable;

    private CircleAnimationCallback mCircleAnimationCallback;

    private boolean mInDrawing;
    private boolean mAttached;
    private boolean mRefreshIsPosted;

    /** Value used to track progress animation, in the range [0...1]. */
    private float mVisualProgress;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    boolean mMirrorForRtl = false;

    private boolean mAggregatedIsVisible;

    private AccessibilityEventSender mAccessibilityEventSender;

    private final ArrayList<RefreshData> mRefreshData = new ArrayList<RefreshData>();

    /**
     * Create a new progress bar with range 0...100 and initial progress of 0.
     * @param context the application environment
     */
    public SeslProgressBar(Context context) {
        this(context, null);
    }

    public SeslProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.progressBarStyle);
    }

    public SeslProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mUiThreadId = Thread.currentThread().getId();
        initProgressBar();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ProgressBar, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.ProgressBar,
                    attrs, a, defStyleAttr, defStyleRes);
        }

        mNoInvalidate = true;

        final Drawable progressDrawable = a.getDrawable(R.styleable.ProgressBar_android_progressDrawable);
        if (progressDrawable != null) {
            // Calling setProgressDrawable can set mMaxHeight, so make sure the
            // corresponding XML attribute for mMaxHeight is read after calling
            // this method.
            if (needsTileify(progressDrawable)) {
                setProgressDrawableTiled(progressDrawable);
            } else {
                setProgressDrawable(progressDrawable);
            }
        }

        mDuration = a.getInt(R.styleable.ProgressBar_android_indeterminateDuration, mDuration);

        mMinWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minWidth, mMinWidth);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxWidth, mMaxWidth);
        mMinHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_minHeight, mMinHeight);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.ProgressBar_android_maxHeight, mMaxHeight);

        mBehavior = a.getInt(R.styleable.ProgressBar_android_indeterminateBehavior, mBehavior);

        final int resID = a.getResourceId(
                R.styleable.ProgressBar_android_interpolator,
                android.R.anim.linear_interpolator); // default to linear interpolator
        if (resID > 0) {
            setInterpolator(context, resID);
        }

        setMin(a.getInt(R.styleable.ProgressBar_android_min, mMin));
        setMax(a.getInt(R.styleable.ProgressBar_android_max, mMax));

        setProgress(a.getInt(R.styleable.ProgressBar_android_progress, mProgress));

        setSecondaryProgress(a.getInt(
                R.styleable.ProgressBar_android_secondaryProgress, mSecondaryProgress));

        final Drawable indeterminateDrawable = a.getDrawable(
                R.styleable.ProgressBar_android_indeterminateDrawable);
        if (indeterminateDrawable != null) {
            if (needsTileify(indeterminateDrawable)) {
                setIndeterminateDrawableTiled(indeterminateDrawable);
            } else {
                setIndeterminateDrawable(indeterminateDrawable);
            }
        }

        mOnlyIndeterminate = a.getBoolean(
                R.styleable.ProgressBar_android_indeterminateOnly, mOnlyIndeterminate);

        mNoInvalidate = false;

        setIndeterminate(mOnlyIndeterminate || a.getBoolean(
                R.styleable.ProgressBar_android_indeterminate, mIndeterminate));

        mMirrorForRtl = a.getBoolean(R.styleable.ProgressBar_android_mirrorForRtl, mMirrorForRtl);

        if (a.hasValue(R.styleable.ProgressBar_android_progressTintMode)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressTintMode = DrawableUtils.parseTintMode(a.getInt(
                    R.styleable.ProgressBar_android_progressTintMode, -1), null);
            mProgressTintInfo.mHasProgressTintMode = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_progressTint)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressTintList = a.getColorStateList(
                    R.styleable.ProgressBar_android_progressTint);
            mProgressTintInfo.mHasProgressTint = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_progressBackgroundTintMode)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressBackgroundTintMode = DrawableUtils.parseTintMode(a.getInt(
                    R.styleable.ProgressBar_android_progressBackgroundTintMode, -1), null);
            mProgressTintInfo.mHasProgressBackgroundTintMode = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_progressBackgroundTint)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressBackgroundTintList = a.getColorStateList(
                    R.styleable.ProgressBar_android_progressBackgroundTint);
            mProgressTintInfo.mHasProgressBackgroundTint = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_secondaryProgressTintMode)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mSecondaryProgressTintMode = DrawableUtils.parseTintMode(
                    a.getInt(R.styleable.ProgressBar_android_secondaryProgressTintMode, -1), null);
            mProgressTintInfo.mHasSecondaryProgressTintMode = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_secondaryProgressTint)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mSecondaryProgressTintList = a.getColorStateList(
                    R.styleable.ProgressBar_android_secondaryProgressTint);
            mProgressTintInfo.mHasSecondaryProgressTint = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_indeterminateTintMode)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mIndeterminateTintMode = DrawableUtils.parseTintMode(a.getInt(
                    R.styleable.ProgressBar_android_indeterminateTintMode, -1), null);
            mProgressTintInfo.mHasIndeterminateTintMode = true;
        }

        if (a.hasValue(R.styleable.ProgressBar_android_indeterminateTint)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mIndeterminateTintList = a.getColorStateList(
                    R.styleable.ProgressBar_android_indeterminateTint);
            mProgressTintInfo.mHasIndeterminateTint = true;
        }

        mUseHorizontalProgress = a.getBoolean(R.styleable.ProgressBar_useHorizontalProgress, mUseHorizontalProgress);

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, R.style.Base_V7_Theme_AppCompat_Light);
        mIndeterminateHorizontalXsmall = getResources().getDrawable(R.drawable.sesl_progress_bar_indeterminate_xsmall_transition,
                contextThemeWrapper.getTheme());
        mIndeterminateHorizontalSmall = getResources().getDrawable(R.drawable.sesl_progress_bar_indeterminate_small_transition,
                contextThemeWrapper.getTheme());
        mIndeterminateHorizontalMedium = getResources().getDrawable(R.drawable.sesl_progress_bar_indeterminate_medium_transition,
                contextThemeWrapper.getTheme());
        mIndeterminateHorizontalLarge = getResources().getDrawable(R.drawable.sesl_progress_bar_indeterminate_large_transition,
                contextThemeWrapper.getTheme());
        mIndeterminateHorizontalXlarge = getResources().getDrawable(R.drawable.sesl_progress_bar_indeterminate_xlarge_transition,
                contextThemeWrapper.getTheme());

        a.recycle();

        applyProgressTints();
        applyIndeterminateTint();

        // If not explicitly specified this view is important for accessibility.
        if (ViewCompat.getImportantForAccessibility(this)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        mDensity = context.getResources().getDisplayMetrics().density;
        mCircleAnimationCallback = new CircleAnimationCallback(this);
    }

    /**
     * Sets the minimum width the progress bar can have.
     * @param minWidth the minimum width to be set, in pixels
     * @attr ref android.R.styleable#ProgressBar_minWidth
     */
    public void setMinWidth(@Px int minWidth) {
        mMinWidth = minWidth;
        requestLayout();
    }

    /**
     * @return the minimum width the progress bar can have, in pixels
     */
    @Px public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * Sets the maximum width the progress bar can have.
     * @param maxWidth the maximum width to be set, in pixels
     * @attr ref android.R.styleable#ProgressBar_maxWidth
     */
    public void setMaxWidth(@Px int maxWidth) {
        mMaxWidth = maxWidth;
        requestLayout();
    }

    /**
     * @return the maximum width the progress bar can have, in pixels
     */
    @Px public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * Sets the minimum height the progress bar can have.
     * @param minHeight the minimum height to be set, in pixels
     * @attr ref android.R.styleable#ProgressBar_minHeight
     */
    public void setMinHeight(@Px int minHeight) {
        mMinHeight = minHeight;
        requestLayout();
    }

    /**
     * @return the minimum height the progress bar can have, in pixels
     */
    @Px public int getMinHeight() {
        return mMinHeight;
    }

    /**
     * Sets the maximum height the progress bar can have.
     * @param maxHeight the maximum height to be set, in pixels
     * @attr ref android.R.styleable#ProgressBar_maxHeight
     */
    public void setMaxHeight(@Px int maxHeight) {
        mMaxHeight = maxHeight;
        requestLayout();
    }

    /**
     * @return the maximum height the progress bar can have, in pixels
     */
    @Px public int getMaxHeight() {
        return mMaxHeight;
    }

    /**
     * Returns {@code true} if the target drawable needs to be tileified.
     *
     * @param dr the drawable to check
     * @return {@code true} if the target drawable needs to be tileified,
     *         {@code false} otherwise
     */
    private static boolean needsTileify(Drawable dr) {
        if (dr instanceof LayerDrawable) {
            final LayerDrawable orig = (LayerDrawable) dr;
            final int N = orig.getNumberOfLayers();
            for (int i = 0; i < N; i++) {
                if (needsTileify(orig.getDrawable(i))) {
                    return true;
                }
            }
            return false;
        }

        if (dr instanceof StateListDrawable) {
            final StateListDrawable in = (StateListDrawable) dr;
            final int N = StateListDrawableCompat.getStateCount(in);
            for (int i = 0; i < N; i++) {
                Drawable d = StateListDrawableCompat.getStateDrawable(in, i);
                if (d != null && needsTileify(d)) {
                    return true;
                }
            }
            return false;
        }

        // If there's a bitmap that's not wrapped with a ClipDrawable or
        // ScaleDrawable, we'll need to wrap it and apply tiling.
        if (dr instanceof BitmapDrawable) {
            return true;
        }

        return false;
    }

    /**
     * Converts a drawable to a tiled version of itself. It will recursively
     * traverse layer and state list drawables.
     */
    private Drawable tileify(Drawable drawable, boolean clip) {
        // TODO: This is a terrible idea that potentially destroys any drawable
        // that extends any of these classes. We *really* need to remove this.

        if (drawable instanceof LayerDrawable) {
            final LayerDrawable orig = (LayerDrawable) drawable;
            final int N = orig.getNumberOfLayers();
            final Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                final int id = orig.getId(i);
                outDrawables[i] = tileify(orig.getDrawable(i),
                        (id == android.R.id.progress || id == android.R.id.secondaryProgress));
            }

            final LayerDrawable clone = new LayerDrawable(outDrawables);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < N; i++) {
                    clone.setId(i, orig.getId(i));
                    clone.setLayerGravity(i, orig.getLayerGravity(i));
                    clone.setLayerWidth(i, orig.getLayerWidth(i));
                    clone.setLayerHeight(i, orig.getLayerHeight(i));
                    clone.setLayerInsetLeft(i, orig.getLayerInsetLeft(i));
                    clone.setLayerInsetRight(i, orig.getLayerInsetRight(i));
                    clone.setLayerInsetTop(i, orig.getLayerInsetTop(i));
                    clone.setLayerInsetBottom(i, orig.getLayerInsetBottom(i));
                    clone.setLayerInsetStart(i, orig.getLayerInsetStart(i));
                    clone.setLayerInsetEnd(i, orig.getLayerInsetEnd(i));
                }
            }

            return clone;
        }

        if (drawable instanceof StateListDrawable) {
            final StateListDrawable in = (StateListDrawable) drawable;
            final StateListDrawable out = new StateListDrawable();
            final int N = StateListDrawableCompat.getStateCount(in);
            for (int i = 0; i < N; i++) {
                final int[] set = StateListDrawableCompat.getStateSet(in, i);
                Drawable d = StateListDrawableCompat.getStateDrawable(in, i);
                if (d != null) {
                    out.addState(set, tileify(d, clip));
                }
            }

            return out;
        }

        if (drawable instanceof BitmapDrawable) {
            final Drawable.ConstantState cs = drawable.getConstantState();
            final BitmapDrawable clone = (BitmapDrawable) cs.newDrawable(getResources());
            clone.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);

            if (mSampleWidth <= 0) {
                mSampleWidth = clone.getIntrinsicWidth();
            }

            if (clip) {
                return new ClipDrawable(clone, Gravity.LEFT, ClipDrawable.HORIZONTAL);
            } else {
                return clone;
            }
        }

        return drawable;
    }

    /**
     * Convert a AnimationDrawable for use as a barberpole animation.
     * Each frame of the animation is wrapped in a ClipDrawable and
     * given a tiling BitmapShader.
     */
    private Drawable tileifyIndeterminate(Drawable drawable) {
        if (drawable instanceof AnimationDrawable) {
            AnimationDrawable background = (AnimationDrawable) drawable;
            final int N = background.getNumberOfFrames();
            AnimationDrawable newBg = new AnimationDrawable();
            newBg.setOneShot(background.isOneShot());

            for (int i = 0; i < N; i++) {
                Drawable frame = tileify(background.getFrame(i), true);
                frame.setLevel(10000);
                newBg.addFrame(frame, background.getDuration(i));
            }
            newBg.setLevel(10000);
            drawable = newBg;
        }
        return drawable;
    }

    /**
     * <p>
     * Initialize the progress bar's default values:
     * </p>
     * <ul>
     * <li>progress = 0</li>
     * <li>max = 100</li>
     * <li>animation duration = 4000 ms</li>
     * <li>indeterminate = false</li>
     * <li>behavior = repeat</li>
     * </ul>
     */
    private void initProgressBar() {
        mMin = 0;
        mMax = 100;
        mProgress = 0;
        mSecondaryProgress = 0;
        mIndeterminate = false;
        mOnlyIndeterminate = false;
        mDuration = 4000;
        mBehavior = AlphaAnimation.RESTART;
        mMinWidth = 24;
        mMaxWidth = 48;
        mMinHeight = 24;
        mMaxHeight = 48;
    }

    /**
     * <p>Indicate whether this progress bar is in indeterminate mode.</p>
     *
     * @return true if the progress bar is in indeterminate mode
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized boolean isIndeterminate() {
        return mIndeterminate;
    }

    /**
     * <p>Change the indeterminate mode for this progress bar. In indeterminate
     * mode, the progress is ignored and the progress bar shows an infinite
     * animation instead.</p>
     *
     * If this progress bar's style only supports indeterminate mode (such as the circular
     * progress bars), then this will be ignored.
     *
     * @param indeterminate true to enable the indeterminate mode
     */
    public synchronized void setIndeterminate(boolean indeterminate) {
        if ((!mOnlyIndeterminate || !mIndeterminate) && indeterminate != mIndeterminate) {
            mIndeterminate = indeterminate;

            if (indeterminate) {
                // swap between indeterminate and regular backgrounds
                swapCurrentDrawable(mIndeterminateDrawable);
                startAnimation();
            } else {
                swapCurrentDrawable(mProgressDrawable);
                stopAnimation();
            }
        }
    }

    private void swapCurrentDrawable(Drawable newDrawable) {
        final Drawable oldDrawable = mCurrentDrawable;
        mCurrentDrawable = newDrawable;

        if (oldDrawable != mCurrentDrawable) {
            if (oldDrawable != null) {
                oldDrawable.setVisible(false, false);
            }
            if (mCurrentDrawable != null) {
                mCurrentDrawable.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
            }
        }
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * indeterminate mode.</p>
     *
     * @return a {@link android.graphics.drawable.Drawable} instance
     *
     * @see #setIndeterminateDrawable(android.graphics.drawable.Drawable)
     * @see #setIndeterminate(boolean)
     */
    public Drawable getIndeterminateDrawable() {
        return mIndeterminateDrawable;
    }

    /**
     * Define the drawable used to draw the progress bar in indeterminate mode.
     *
     * @param d the new drawable
     * @attr ref android.R.styleable#ProgressBar_indeterminateDrawable
     * @see #getIndeterminateDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setIndeterminateDrawable(Drawable d) {
        if (mIndeterminateDrawable != d) {
            if (mIndeterminateDrawable != null) {
                if (mUseHorizontalProgress) {
                    stopAnimation();
                }
                mIndeterminateDrawable.setCallback(null);
                unscheduleDrawable(mIndeterminateDrawable);
            }

            mIndeterminateDrawable = d;

            if (d != null) {
                d.setCallback(this);
                DrawableCompat.setLayoutDirection(d,
                        ViewCompat.getLayoutDirection(this));
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }
                applyIndeterminateTint();
            }

            if (mIndeterminate) {
                if (mUseHorizontalProgress) {
                    startAnimation();
                }
                swapCurrentDrawable(d);
                postInvalidate();
            }
        }
    }

    /**
     * Applies a tint to the indeterminate drawable. Does not modify the
     * current tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setIndeterminateDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and
     * tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#ProgressBar_indeterminateTint
     * @see #getIndeterminateTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setIndeterminateTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mIndeterminateTintList = tint;
        mProgressTintInfo.mHasIndeterminateTint = true;

        applyIndeterminateTint();
    }

    /**
     * @return the tint applied to the indeterminate drawable
     * @attr ref android.R.styleable#ProgressBar_indeterminateTint
     * @see #setIndeterminateTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getIndeterminateTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mIndeterminateTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setIndeterminateTintList(ColorStateList)} to the indeterminate
     * drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @attr ref android.R.styleable#ProgressBar_indeterminateTintMode
     * @see #setIndeterminateTintList(ColorStateList)
     * @see Drawable#setTintMode(PorterDuff.Mode)
     *
     */
    public void setIndeterminateTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mIndeterminateTintMode = tintMode;
        mProgressTintInfo.mHasIndeterminateTintMode = true;

        applyIndeterminateTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the indeterminate
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the indeterminate
     *         drawable
     * @attr ref android.R.styleable#ProgressBar_indeterminateTintMode
     * @see #setIndeterminateTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getIndeterminateTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mIndeterminateTintMode : null;
    }

    private void applyIndeterminateTint() {
        if (mIndeterminateDrawable != null && mProgressTintInfo != null) {
            final ProgressTintInfo tintInfo = mProgressTintInfo;
            if (tintInfo.mHasIndeterminateTint || tintInfo.mHasIndeterminateTintMode) {
                mIndeterminateDrawable = mIndeterminateDrawable.mutate();

                if (tintInfo.mHasIndeterminateTint) {
                    DrawableCompat.setTintList(mIndeterminateDrawable,
                            tintInfo.mIndeterminateTintList);
                }

                if (tintInfo.mHasIndeterminateTintMode) {
                    DrawableCompat.setTintMode(mIndeterminateDrawable,
                            tintInfo.mIndeterminateTintMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (mIndeterminateDrawable.isStateful()) {
                    mIndeterminateDrawable.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Define the tileable drawable used to draw the progress bar in
     * indeterminate mode.
     * <p>
     * If the drawable is a BitmapDrawable or contains BitmapDrawables, a
     * tiled copy will be generated for display as a progress bar.
     *
     * @param d the new drawable
     * @see #getIndeterminateDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setIndeterminateDrawableTiled(Drawable d) {
        if (d != null) {
            d = tileifyIndeterminate(d);
        }

        setIndeterminateDrawable(d);
    }

    /**
     * <p>Get the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @return a {@link android.graphics.drawable.Drawable} instance
     *
     * @see #setProgressDrawable(android.graphics.drawable.Drawable)
     * @see #setIndeterminate(boolean)
     */
    public Drawable getProgressDrawable() {
        return mProgressDrawable;
    }

    /**
     * Define the drawable used to draw the progress bar in progress mode.
     *
     * @param d the new drawable
     * @see #getProgressDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setProgressDrawable(Drawable d) {
        if (mProgressDrawable != d) {
            if (mProgressDrawable != null) {
                mProgressDrawable.setCallback(null);
                unscheduleDrawable(mProgressDrawable);
            }

            mProgressDrawable = d;

            if (d != null) {
                d.setCallback(this);
                DrawableCompat.setLayoutDirection(d,
                        ViewCompat.getLayoutDirection(this));
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }

                // Make sure the ProgressBar is always tall enough
                if (mCurrentMode == MODE_VERTICAL || mCurrentMode == MODE_EXPAND_VERTICAL) {
                    int drawableWidth = d.getMinimumWidth();
                    if (mMaxWidth < drawableWidth) {
                        mMaxWidth = drawableWidth;
                        requestLayout();
                    }
                } else {
                    int drawableHeight = d.getMinimumHeight();
                    if (mMaxHeight < drawableHeight) {
                        mMaxHeight = drawableHeight;
                        requestLayout();
                    }
                }

                applyProgressTints();
            }

            if (!mIndeterminate) {
                swapCurrentDrawable(d);
                postInvalidate();
            }

            updateDrawableBounds(getWidth(), getHeight());
            updateDrawableState();

            doRefreshProgress(android.R.id.progress,
                    mProgress, false, false, false);
            doRefreshProgress(android.R.id.secondaryProgress,
                    mSecondaryProgress, false, false, false);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean getMirrorForRtl() {
        return mMirrorForRtl;
    }

    /**
     * Applies the progress tints in order of increasing specificity.
     */
    private void applyProgressTints() {
        if (mProgressDrawable != null && mProgressTintInfo != null) {
            applyPrimaryProgressTint();
            applyProgressBackgroundTint();
            applySecondaryProgressTint();
        }
    }

    /**
     * Should only be called if we've already verified that mProgressDrawable
     * and mProgressTintInfo are non-null.
     */
    private void applyPrimaryProgressTint() {
        if (mProgressTintInfo.mHasProgressTint
                || mProgressTintInfo.mHasProgressTintMode) {
            final Drawable target = getTintTarget(android.R.id.progress, true);
            if (target != null) {
                if (mProgressTintInfo.mHasProgressTint) {
                    DrawableCompat.setTintList(target, mProgressTintInfo.mProgressTintList);
                }
                if (mProgressTintInfo.mHasProgressTintMode) {
                    DrawableCompat.setTintMode(target, mProgressTintInfo.mProgressTintMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Should only be called if we've already verified that mProgressDrawable
     * and mProgressTintInfo are non-null.
     */
    private void applyProgressBackgroundTint() {
        if (mProgressTintInfo.mHasProgressBackgroundTint
                || mProgressTintInfo.mHasProgressBackgroundTintMode) {
            final Drawable target = getTintTarget(android.R.id.background, false);
            if (target != null) {
                if (mProgressTintInfo.mHasProgressBackgroundTint) {
                    DrawableCompat.setTintList(target, mProgressTintInfo.mProgressBackgroundTintList);
                }
                if (mProgressTintInfo.mHasProgressBackgroundTintMode) {
                    DrawableCompat.setTintMode(target, mProgressTintInfo.mProgressBackgroundTintMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Should only be called if we've already verified that mProgressDrawable
     * and mProgressTintInfo are non-null.
     */
    private void applySecondaryProgressTint() {
        if (mProgressTintInfo.mHasSecondaryProgressTint
                || mProgressTintInfo.mHasSecondaryProgressTintMode) {
            final Drawable target = getTintTarget(android.R.id.secondaryProgress, false);
            if (target != null) {
                if (mProgressTintInfo.mHasSecondaryProgressTint) {
                    DrawableCompat.setTintList(target, mProgressTintInfo.mSecondaryProgressTintList);
                }
                if (mProgressTintInfo.mHasSecondaryProgressTintMode) {
                    DrawableCompat.setTintMode(target, mProgressTintInfo.mSecondaryProgressTintMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * Applies a tint to the progress indicator, if one exists, or to the
     * entire progress drawable otherwise. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * The progress indicator should be specified as a layer with
     * id {@link android.R.id#progress} in a {@link LayerDrawable}
     * used as the progress drawable.
     * <p>
     * Subsequent calls to {@link #setProgressDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and
     * tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#ProgressBar_progressTint
     * @see #getProgressTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setProgressTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressTintList = tint;
        mProgressTintInfo.mHasProgressTint = true;

        if (mProgressDrawable != null) {
            applyPrimaryProgressTint();
        }
    }

    /**
     * Returns the tint applied to the progress drawable, if specified.
     *
     * @return the tint applied to the progress drawable
     * @attr ref android.R.styleable#ProgressBar_progressTint
     * @see #setProgressTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getProgressTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setProgressTintList(ColorStateList)}} to the progress
     * indicator. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @attr ref android.R.styleable#ProgressBar_progressTintMode
     * @see #getProgressTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setProgressTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressTintMode = tintMode;
        mProgressTintInfo.mHasProgressTintMode = true;

        if (mProgressDrawable != null) {
            applyPrimaryProgressTint();
        }
    }

    /**
     * Returns the blending mode used to apply the tint to the progress
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the progress
     *         drawable
     * @attr ref android.R.styleable#ProgressBar_progressTintMode
     * @see #setProgressTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getProgressTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressTintMode : null;
    }

    /**
     * Applies a tint to the progress background, if one exists. Does not
     * modify the current tint mode, which is
     * {@link PorterDuff.Mode#SRC_ATOP} by default.
     * <p>
     * The progress background must be specified as a layer with
     * id {@link android.R.id#background} in a {@link LayerDrawable}
     * used as the progress drawable.
     * <p>
     * Subsequent calls to {@link #setProgressDrawable(Drawable)} where the
     * drawable contains a progress background will automatically mutate the
     * drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#ProgressBar_progressBackgroundTint
     * @see #getProgressBackgroundTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setProgressBackgroundTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBackgroundTintList = tint;
        mProgressTintInfo.mHasProgressBackgroundTint = true;

        if (mProgressDrawable != null) {
            applyProgressBackgroundTint();
        }
    }

    /**
     * Returns the tint applied to the progress background, if specified.
     *
     * @return the tint applied to the progress background
     * @attr ref android.R.styleable#ProgressBar_progressBackgroundTint
     * @see #setProgressBackgroundTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getProgressBackgroundTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBackgroundTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setProgressBackgroundTintList(ColorStateList)}} to the progress
     * background. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @attr ref android.R.styleable#ProgressBar_progressBackgroundTintMode
     * @see #setProgressBackgroundTintList(ColorStateList)
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setProgressBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBackgroundTintMode = tintMode;
        mProgressTintInfo.mHasProgressBackgroundTintMode = true;

        if (mProgressDrawable != null) {
            applyProgressBackgroundTint();
        }
    }

    /**
     * @return the blending mode used to apply the tint to the progress
     *         background
     * @attr ref android.R.styleable#ProgressBar_progressBackgroundTintMode
     * @see #setProgressBackgroundTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getProgressBackgroundTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBackgroundTintMode : null;
    }

    /**
     * Applies a tint to the secondary progress indicator, if one exists.
     * Does not modify the current tint mode, which is
     * {@link PorterDuff.Mode#SRC_ATOP} by default.
     * <p>
     * The secondary progress indicator must be specified as a layer with
     * id {@link android.R.id#secondaryProgress} in a {@link LayerDrawable}
     * used as the progress drawable.
     * <p>
     * Subsequent calls to {@link #setProgressDrawable(Drawable)} where the
     * drawable contains a secondary progress indicator will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @attr ref android.R.styleable#ProgressBar_secondaryProgressTint
     * @see #getSecondaryProgressTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setSecondaryProgressTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mSecondaryProgressTintList = tint;
        mProgressTintInfo.mHasSecondaryProgressTint = true;

        if (mProgressDrawable != null) {
            applySecondaryProgressTint();
        }
    }

    /**
     * Returns the tint applied to the secondary progress drawable, if
     * specified.
     *
     * @return the tint applied to the secondary progress drawable
     * @attr ref android.R.styleable#ProgressBar_secondaryProgressTint
     * @see #setSecondaryProgressTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getSecondaryProgressTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mSecondaryProgressTintList : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setSecondaryProgressTintList(ColorStateList)}} to the secondary
     * progress indicator. The default mode is
     * {@link PorterDuff.Mode#SRC_ATOP}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @attr ref android.R.styleable#ProgressBar_secondaryProgressTintMode
     * @see #setSecondaryProgressTintList(ColorStateList)
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setSecondaryProgressTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mSecondaryProgressTintMode = tintMode;
        mProgressTintInfo.mHasSecondaryProgressTintMode = true;

        if (mProgressDrawable != null) {
            applySecondaryProgressTint();
        }
    }

    /**
     * Returns the blending mode used to apply the tint to the secondary
     * progress drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the secondary
     *         progress drawable
     * @attr ref android.R.styleable#ProgressBar_secondaryProgressTintMode
     * @see #setSecondaryProgressTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getSecondaryProgressTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mSecondaryProgressTintMode : null;
    }

    /**
     * Returns the drawable to which a tint or tint mode should be applied.
     *
     * @param layerId id of the layer to modify
     * @param shouldFallback whether the base drawable should be returned
     *                       if the id does not exist
     * @return the drawable to modify
     */
    @Nullable
    private Drawable getTintTarget(int layerId, boolean shouldFallback) {
        Drawable layer = null;

        final Drawable d = mProgressDrawable;
        if (d != null) {
            mProgressDrawable = d.mutate();

            if (d instanceof LayerDrawable) {
                layer = ((LayerDrawable) d).findDrawableByLayerId(layerId);
            }

            if (shouldFallback && layer == null) {
                layer = d;
            }
        }

        return layer;
    }

    /**
     * Define the tileable drawable used to draw the progress bar in
     * progress mode.
     * <p>
     * If the drawable is a BitmapDrawable or contains BitmapDrawables, a
     * tiled copy will be generated for display as a progress bar.
     *
     * @param d the new drawable
     * @see #getProgressDrawable()
     * @see #setIndeterminate(boolean)
     */
    public void setProgressDrawableTiled(Drawable d) {
        if (d != null) {
            d = tileify(d, false);
        }

        setProgressDrawable(d);
    }

    /**
     * Returns the drawable currently used to draw the progress bar. This will be
     * either {@link #getProgressDrawable()} or {@link #getIndeterminateDrawable()}
     * depending on whether the progress bar is in determinate or indeterminate mode.
     *
     * @return the drawable currently used to draw the progress bar
     */
    @Nullable
    public Drawable getCurrentDrawable() {
        return mCurrentDrawable;
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mProgressDrawable || who == mIndeterminateDrawable
                || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mProgressDrawable != null) mProgressDrawable.jumpToCurrentState();
        if (mIndeterminateDrawable != null) mIndeterminateDrawable.jumpToCurrentState();
    }

    /**
     * @hide
     */
    // @Override : hidden method
    public void onResolveDrawables(int layoutDirection) {
        final Drawable d = mCurrentDrawable;
        layoutDirection = ViewCompat.getLayoutDirection(this);
        if (d != null) {
            DrawableCompat.setLayoutDirection(d, layoutDirection);
        }
        if (mIndeterminateDrawable != null) {
            DrawableCompat.setLayoutDirection(mIndeterminateDrawable, layoutDirection);
        }
        if (mProgressDrawable != null) {
            DrawableCompat.setLayoutDirection(mProgressDrawable, layoutDirection);
        }
    }

    @Override
    public void postInvalidate() {
        if (!mNoInvalidate) {
            super.postInvalidate();
        }
    }

    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (SeslProgressBar.this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.id, rd.progress, rd.fromUser, true, rd.animate);
                    rd.recycle();
                }
                mRefreshData.clear();
                mRefreshIsPosted = false;
            }
        }
    }

    private static class RefreshData {
        private static final int POOL_MAX = 24;
        private static final SynchronizedPool<RefreshData> sPool =
                new SynchronizedPool<RefreshData>(POOL_MAX);

        public int id;
        public int progress;
        public boolean fromUser;
        public boolean animate;

        public static RefreshData obtain(int id, int progress, boolean fromUser, boolean animate) {
            RefreshData rd = sPool.acquire();
            if (rd == null) {
                rd = new RefreshData();
            }
            rd.id = id;
            rd.progress = progress;
            rd.fromUser = fromUser;
            rd.animate = animate;
            return rd;
        }

        public void recycle() {
            sPool.release(this);
        }
    }

    private synchronized void doRefreshProgress(int id, int progress, boolean fromUser,
            boolean callBackToApp, boolean animate) {
        int range = mMax - mMin;
        final float scale = range > 0 ? (progress - mMin) / (float) range : 0;
        final boolean isPrimary = id == android.R.id.progress;

        Drawable drawable = mCurrentDrawable;
        if (drawable != null) {
            final int level = (int) (scale * MAX_LEVEL);

            if (drawable instanceof LayerDrawable) {
                Drawable layer = ((LayerDrawable) drawable).findDrawableByLayerId(id);
                if (layer != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && canResolveLayoutDirection()) {
                    DrawableCompat.setLayoutDirection(layer, ViewCompat.getLayoutDirection(this));
                }
                if (layer != null) {
                    drawable = layer;
                }
                drawable.setLevel(level);
            } else if (drawable instanceof StateListDrawable) {
                final int numStates = StateListDrawableCompat.getStateCount((StateListDrawable) drawable);
                for (int i = 0; i < numStates; i++) {
                    Drawable stateD = StateListDrawableCompat.getStateDrawable((StateListDrawable) drawable, i);
                    if (stateD != null && stateD instanceof LayerDrawable) {
                        Drawable layer = ((LayerDrawable) stateD).findDrawableByLayerId(i);
                        if (layer != null && Build.VERSION.SDK_INT > 19 && canResolveLayoutDirection()) {
                            DrawableCompat.setLayoutDirection(layer, ViewCompat.getLayoutDirection(this));
                        }
                        if (layer == null) {
                            layer = drawable;
                        }
                        layer.setLevel(level);
                    } else {
                        return;
                    }
                }
            } else {
                drawable.setLevel(level);
            }
        } else {
            invalidate();
        }

        if (isPrimary && animate) {
            final ObjectAnimator animator = ObjectAnimator.ofFloat(this, VISUAL_PROGRESS, scale);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                animator.setAutoCancel(true);
            }
            animator.setDuration(PROGRESS_ANIM_DURATION);
            animator.setInterpolator(PROGRESS_ANIM_INTERPOLATOR);
            animator.start();
        } else {
            setVisualProgress(id, scale);
        }

        if (isPrimary && callBackToApp) {
            onProgressRefresh(scale, fromUser, progress);
        }
    }

    void onProgressRefresh(float scale, boolean fromUser, int progress) {
        if (((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled()) {
            scheduleAccessibilityEventSender();
        }
        if (mSecondaryProgress > mProgress && !fromUser) {
            refreshProgress(android.R.id.secondaryProgress, mSecondaryProgress, false, false);
        }
    }

    /**
     * Sets the visual state of a progress indicator.
     *
     * @param id the identifier of the progress indicator
     * @param progress the visual progress in the range [0...1]
     */
    private void setVisualProgress(int id, float progress) {
        mVisualProgress = progress;

        Drawable d = mCurrentDrawable;

        if (d instanceof LayerDrawable) {
            d = ((LayerDrawable) d).findDrawableByLayerId(id);
            if (d == null) {
                // If we can't find the requested layer, fall back to setting
                // the level of the entire drawable. This will break if
                // progress is set on multiple elements, but the theme-default
                // drawable will always have all layer IDs present.
                d = mCurrentDrawable;
            }
        }

        if (d != null) {
            final int level = (int) (progress * MAX_LEVEL);
            d.setLevel(level);
        } else {
            invalidate();
        }

        onVisualProgressChanged(id, progress);
    }

    /**
     * Called when the visual state of a progress indicator changes.
     *
     * @param id the identifier of the progress indicator
     * @param progress the visual progress in the range [0...1]
     */
    void onVisualProgressChanged(int id, float progress) {
        // Stub method.
    }

    private synchronized void refreshProgress(int id, int progress, boolean fromUser,
            boolean animate) {
        if (mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(id, progress, fromUser, true, animate);
        } else {
            if (mRefreshProgressRunnable == null) {
                mRefreshProgressRunnable = new RefreshProgressRunnable();
            }

            final RefreshData rd = RefreshData.obtain(id, progress, fromUser, animate);
            mRefreshData.add(rd);
            if (mAttached && !mRefreshIsPosted) {
                post(mRefreshProgressRunnable);
                mRefreshIsPosted = true;
            }
        }
    }

    /**
     * Sets the current progress to the specified value. Does not do anything
     * if the progress bar is in indeterminate mode.
     * <p>
     * This method will immediately update the visual position of the progress
     * indicator. To animate the visual position to the target value, use
     * {@link #setProgress(int, boolean)}}.
     *
     * @param progress the new progress, between {@link #getMin()} and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getProgress()
     * @see #incrementProgressBy(int)
     */
    public synchronized void setProgress(int progress) {
        setProgressInternal(progress, false, false);
    }

    /**
     * Sets the current progress to the specified value, optionally animating
     * the visual position between the current and target values.
     * <p>
     * Animation does not affect the result of {@link #getProgress()}, which
     * will return the target value immediately after this method is called.
     *
     * @param progress the new progress value, between {@link #getMin()} and {@link #getMax()}
     * @param animate {@code true} to animate between the current and target
     *                values or {@code false} to not animate
     */
    public void setProgress(int progress, boolean animate) {
        setProgressInternal(progress, false, animate);
    }

    synchronized boolean setProgressInternal(int progress, boolean fromUser, boolean animate) {
        if (mIndeterminate) {
            // Not applicable.
            return false;
        }

        progress = MathUtils.constrain(progress, mMin, mMax);

        if (progress == mProgress) {
            // No change from current.
            return false;
        }

        mProgress = progress;
        if (mCurrentMode == MODE_CIRCLE) {
            if (getProgressDrawable() instanceof LayerDrawable) {
                Drawable d = ((LayerDrawable) getProgressDrawable())
                        .findDrawableByLayerId(android.R.id.progress);
                if (d != null && d instanceof CirCleProgressDrawable) {
                    ((CirCleProgressDrawable) d).setProgress(progress, animate);
                }
            }
        }
        refreshProgress(android.R.id.progress, mProgress, fromUser, animate);
        return true;
    }

    /**
     * <p>
     * Set the current secondary progress to the specified value. Does not do
     * anything if the progress bar is in indeterminate mode.
     * </p>
     *
     * @param secondaryProgress the new secondary progress, between {@link #getMin()} and
     * {@link #getMax()}
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getSecondaryProgress()
     * @see #incrementSecondaryProgressBy(int)
     */
    public synchronized void setSecondaryProgress(int secondaryProgress) {
        if (mIndeterminate) {
            return;
        }

        if (secondaryProgress < mMin) {
            secondaryProgress = mMin;
        }

        if (secondaryProgress > mMax) {
            secondaryProgress = mMax;
        }

        if (secondaryProgress != mSecondaryProgress) {
            mSecondaryProgress = secondaryProgress;
            refreshProgress(android.R.id.secondaryProgress, mSecondaryProgress, false, false);
        }
    }

    /**
     * <p>Get the progress bar's current level of progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current progress, between {@link #getMin()} and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getProgress() {
        return mIndeterminate ? 0 : mProgress;
    }

    /**
     * <p>Get the progress bar's current level of secondary progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current secondary progress, between {@link #getMin()} and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setSecondaryProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getSecondaryProgress() {
        return mIndeterminate ? 0 : mSecondaryProgress;
    }

    /**
     * <p>Return the lower limit of this progress bar's range.</p>
     *
     * @return a positive integer
     *
     * @see #setMin(int)
     * @see #getProgress()
     * @see #getSecondaryProgress()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getMin() {
        return mMin;
    }

    /**
     * <p>Return the upper limit of this progress bar's range.</p>
     *
     * @return a positive integer
     *
     * @see #setMax(int)
     * @see #getProgress()
     * @see #getSecondaryProgress()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getMax() {
        return mMax;
    }

    /**
     * <p>Set the lower range of the progress bar to <tt>min</tt>.</p>
     *
     * @param min the lower range of this progress bar
     *
     * @see #getMin()
     * @see #setProgress(int)
     * @see #setSecondaryProgress(int)
     */
    public synchronized void setMin(int min) {
        if (mMaxInitialized) {
            if (min > mMax) {
                min = mMax;
            }
        }
        mMinInitialized = true;
        if (mMaxInitialized && min != mMin) {
            mMin = min;
            postInvalidate();

            if (mProgress < min) {
                mProgress = min;
            }
            refreshProgress(android.R.id.progress, mProgress, false, false);
        } else {
            mMin = min;
        }
    }

    /**
     * <p>Set the upper range of the progress bar <tt>max</tt>.</p>
     *
     * @param max the upper range of this progress bar
     *
     * @see #getMax()
     * @see #setProgress(int)
     * @see #setSecondaryProgress(int)
     */
    public synchronized void setMax(int max) {
        if (mMinInitialized) {
            if (max < mMin) {
                max = mMin;
            }
        }
        mMaxInitialized = true;
        if (mMinInitialized && max != mMax) {
            mMax = max;
            postInvalidate();

            if (mProgress > max) {
                mProgress = max;
            }
            refreshProgress(android.R.id.progress, mProgress, false, false);
        } else {
            mMax = max;
        }
    }

    /**
     * <p>Increase the progress bar's progress by the specified amount.</p>
     *
     * @param diff the amount by which the progress must be increased
     *
     * @see #setProgress(int)
     */
    public synchronized final void incrementProgressBy(int diff) {
        setProgress(mProgress + diff);
    }

    /**
     * <p>Increase the progress bar's secondary progress by the specified amount.</p>
     *
     * @param diff the amount by which the secondary progress must be increased
     *
     * @see #setSecondaryProgress(int)
     */
    public synchronized final void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(mSecondaryProgress + diff);
    }

    /**
     * <p>Start the indeterminate progress animation.</p>
     */
    private void startAnimation() {
        if (getVisibility() != VISIBLE) {
            return;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M || getWindowVisibility() == VISIBLE) {
            if (mIndeterminateDrawable instanceof Animatable) {
                mShouldStartAnimationDrawable = true;
                mHasAnimation = false;
                if (mIndeterminateDrawable instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) mIndeterminateDrawable)
                            .registerAnimationCallback(mCircleAnimationCallback);
                }
            } else {
                mHasAnimation = true;

                if (mInterpolator == null) {
                    mInterpolator = new LinearInterpolator();
                }

                if (mTransformation == null) {
                    mTransformation = new Transformation();
                } else {
                    mTransformation.clear();
                }

                if (mAnimation == null) {
                    mAnimation = new AlphaAnimation(0.0f, 1.0f);
                } else {
                    mAnimation.reset();
                }

                mAnimation.setRepeatMode(mBehavior);
                mAnimation.setRepeatCount(Animation.INFINITE);
                mAnimation.setDuration(mDuration);
                mAnimation.setInterpolator(mInterpolator);
                mAnimation.setStartTime(Animation.START_ON_FIRST_FRAME);
            }
            postInvalidate();
        }
    }

    /**
     * <p>Stop the indeterminate progress animation.</p>
     */
    private void stopAnimation() {
        mHasAnimation = false;
        if (mIndeterminateDrawable instanceof Animatable) {
            ((Animatable) mIndeterminateDrawable).stop();
            if (mIndeterminateDrawable instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) mIndeterminateDrawable)
                        .unregisterAnimationCallback(mCircleAnimationCallback);
            }
            mShouldStartAnimationDrawable = false;
        }
        postInvalidate();
    }

    /**
     * Sets the acceleration curve for the indeterminate animation.
     *
     * <p>The interpolator is loaded as a resource from the specified context. Defaults to a linear
     * interpolation.
     *
     * <p>The interpolator only affects the indeterminate animation if the
     * {@link #setIndeterminateDrawable(Drawable) supplied indeterminate drawable} does not
     * implement {@link Animatable}.
     *
     * <p>This call must be made before the indeterminate animation starts for it to have an affect.
     *
     * @param context The application environment
     * @param resID The resource identifier of the interpolator to load
     * @attr ref android.R.styleable#ProgressBar_interpolator
     * @see #setInterpolator(Interpolator)
     * @see #getInterpolator()
     */
    public void setInterpolator(Context context, @InterpolatorRes int resID) {
        setInterpolator(AnimationUtils.loadInterpolator(context, resID));
    }

    /**
     * Sets the acceleration curve for the indeterminate animation.
     * Defaults to a linear interpolation.
     *
     * <p>The interpolator only affects the indeterminate animation if the
     * {@link #setIndeterminateDrawable(Drawable) supplied indeterminate drawable} does not
     * implement {@link Animatable}.
     *
     * <p>This call must be made before the indeterminate animation starts for it to have
     * an affect.
     *
     * @param interpolator The interpolator which defines the acceleration curve
     * @attr ref android.R.styleable#ProgressBar_interpolator
     * @see #setInterpolator(Context, int)
     * @see #getInterpolator()
     */
    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    /**
     * Gets the acceleration curve type for the indeterminate animation.
     *
     * @return the {@link Interpolator} associated to this animation
     * @attr ref android.R.styleable#ProgressBar_interpolator
     * @see #setInterpolator(Context, int)
     * @see #setInterpolator(Interpolator)
     */
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        if (isVisible != mAggregatedIsVisible) {
            mAggregatedIsVisible = isVisible;

            if (mIndeterminate) {
                // let's be nice with the UI thread
                if (isVisible) {
                    startAnimation();
                } else {
                    stopAnimation();
                }
            }

            if (mCurrentDrawable != null) {
                mCurrentDrawable.setVisible(isVisible, false);
            }
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        if (!mInDrawing) {
            if (verifyDrawable(dr)) {
                final Rect dirty = dr.getBounds();
                final int scrollX = getScrollX() + getPaddingLeft();
                final int scrollY = getScrollY() + getPaddingTop();

                invalidate(dirty.left + scrollX, dirty.top + scrollY,
                        dirty.right + scrollX, dirty.bottom + scrollY);
            } else {
                super.invalidateDrawable(dr);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(w, h);
    }

    protected void updateDrawableBounds(int w, int h) {
        // onDraw will translate the canvas so we draw starting at 0,0.
        // Subtract out padding for the purposes of the calculations below.
        w -= getPaddingRight() + getPaddingLeft();
        h -= getPaddingTop() + getPaddingBottom();

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;

        if (mIndeterminateDrawable != null) {
            // Aspect ratio logic does not apply to AnimationDrawables
            if (mOnlyIndeterminate && !(mIndeterminateDrawable instanceof AnimationDrawable)) {
                // Maintain aspect ratio. Certain kinds of animated drawables
                // get very confused otherwise.
                final int intrinsicWidth = mIndeterminateDrawable.getIntrinsicWidth();
                final int intrinsicHeight = mIndeterminateDrawable.getIntrinsicHeight();
                final float intrinsicAspect = (float) intrinsicWidth / intrinsicHeight;
                final float boundAspect = (float) w / h;
                if (Math.abs(intrinsicAspect - boundAspect) < 1.0E-7d) {
                    if (boundAspect > intrinsicAspect) {
                        // New width is larger. Make it smaller to match height.
                        final int width = (int) (h * intrinsicAspect);
                        left = (w - width) / 2;
                        right = left + width;
                    } else {
                        // New height is larger. Make it smaller to match width.
                        final int height = (int) (w * (1 / intrinsicAspect));
                        top = (h - height) / 2;
                        bottom = top + height;
                    }
                }
            }
            if (mMirrorForRtl && ViewUtils.isLayoutRtl(this)) {
                int tempLeft = left;
                left = w - right;
                right = w - tempLeft;
            }
            mIndeterminateDrawable.setBounds(left, top, right, bottom);
        }

        if (mProgressDrawable != null) {
            mProgressDrawable.setBounds(0, 0, right, bottom);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTrack(canvas);
    }

    /**
     * Draws the progress bar track.
     */
    void drawTrack(Canvas canvas) {
        final Drawable d = mCurrentDrawable;
        if (d != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            final int saveCount = canvas.save();

            if (mCurrentMode != MODE_VERTICAL && mMirrorForRtl && ViewUtils.isLayoutRtl(this)) {
                canvas.translate(getWidth() - getPaddingRight(), getPaddingTop());
                canvas.scale(-1.0f, 1.0f);
            } else {
                canvas.translate(getPaddingLeft(), getPaddingTop());
            }

            final long time = getDrawingTime();
            if (mHasAnimation) {
                mAnimation.getTransformation(time, mTransformation);
                final float scale = mTransformation.getAlpha();
                try {
                    mInDrawing = true;
                    d.setLevel((int) (scale * MAX_LEVEL));
                } finally {
                    mInDrawing = false;
                }
                ViewCompat.postInvalidateOnAnimation(this);
            }

            d.draw(canvas);
            canvas.restoreToCount(saveCount);

            if (mShouldStartAnimationDrawable && d instanceof Animatable) {
                ((Animatable) d).start();
                mShouldStartAnimationDrawable = false;
            }
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = 0;
        int dh = 0;

        final Drawable d = mCurrentDrawable;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
        }

        updateDrawableState();

        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        initCirCleStrokeWidth(measuredWidth - getPaddingLeft() - getPaddingRight());
        if (mUseHorizontalProgress && mIndeterminate) {
            seslSetIndeterminateProgressDrawable(measuredWidth - getPaddingLeft() - getPaddingRight());
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable progressDrawable = mProgressDrawable;
        if (progressDrawable != null && progressDrawable.isStateful()) {
            changed |= progressDrawable.setState(state);
        }

        final Drawable indeterminateDrawable = mIndeterminateDrawable;
        if (indeterminateDrawable != null && indeterminateDrawable.isStateful()) {
            changed |= indeterminateDrawable.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mProgressDrawable != null) {
            DrawableCompat.setHotspot(mProgressDrawable, x, y);
        }

        if (mIndeterminateDrawable != null) {
            DrawableCompat.setHotspot(mIndeterminateDrawable, x, y);
        }
    }

    static class SavedState extends BaseSavedState {
        int progress;
        int secondaryProgress;

        /**
         * Constructor called from {@link SeslProgressBar#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            secondaryProgress = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(secondaryProgress);
        }

        public static final @NonNull Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.progress = mProgress;
        ss.secondaryProgress = mSecondaryProgress;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setProgress(ss.progress);
        setSecondaryProgress(ss.secondaryProgress);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIndeterminate) {
            startAnimation();
        }
        if (mRefreshData != null) {
            synchronized (this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.id, rd.progress, rd.fromUser, true, rd.animate);
                    rd.recycle();
                }
                mRefreshData.clear();
            }
        }
        mAttached = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mIndeterminate) {
            stopAnimation();
        } else {
            mCircleAnimationCallback = null;
        }
        if (mRefreshProgressRunnable != null) {
            removeCallbacks(mRefreshProgressRunnable);
            mRefreshIsPosted = false;
        }
        if (mAccessibilityEventSender != null) {
            removeCallbacks(mAccessibilityEventSender);
        }
        // This should come after stopAnimation(), otherwise an invalidate message remains in the
        // queue, which can prevent the entire view hierarchy from being GC'ed during a rotation
        super.onDetachedFromWindow();
        mAttached = false;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.ProgressBar.class.getName();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setItemCount(mMax - mMin);
        event.setCurrentItemIndex(mProgress);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isIndeterminate()) {
            AccessibilityNodeInfo.RangeInfo rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
                    AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, getMin(), getMax(),
                    getProgress());
            info.setRangeInfo(rangeInfo);
        }
    }

    private void scheduleAccessibilityEventSender() {
        if (mAccessibilityEventSender == null) {
            mAccessibilityEventSender = new AccessibilityEventSender();
        } else {
            removeCallbacks(mAccessibilityEventSender);
        }
        postDelayed(mAccessibilityEventSender, TIMEOUT_SEND_ACCESSIBILITY_EVENT);
    }

    /**
     * Returns whether the ProgressBar is animating or not. This is essentially the same
     * as whether the ProgressBar is {@link #isIndeterminate() indeterminate} and visible,
     * as indeterminate ProgressBars are always animating, and non-indeterminate
     * ProgressBars are not animating.
     *
     * @return true if the ProgressBar is animating, false otherwise.
     */
    public boolean isAnimating() {
        return isIndeterminate() && getWindowVisibility() == VISIBLE && isShown();
    }

    private class AccessibilityEventSender implements Runnable {
        @Override
        public void run() {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

    private static class ProgressTintInfo {
        ColorStateList mIndeterminateTintList;
        PorterDuff.Mode mIndeterminateTintMode;
        boolean mHasIndeterminateTint;
        boolean mHasIndeterminateTintMode;

        ColorStateList mProgressTintList;
        PorterDuff.Mode mProgressTintMode;
        boolean mHasProgressTint;
        boolean mHasProgressTintMode;

        ColorStateList mProgressBackgroundTintList;
        PorterDuff.Mode mProgressBackgroundTintMode;
        boolean mHasProgressBackgroundTint;
        boolean mHasProgressBackgroundTintMode;

        ColorStateList mSecondaryProgressTintList;
        PorterDuff.Mode mSecondaryProgressTintMode;
        boolean mHasSecondaryProgressTint;
        boolean mHasSecondaryProgressTintMode;
    }

    public void setMode(@SeekBarMode int mode) {
        mCurrentMode = mode;

        Drawable progressDrawable = null;
        switch (mode) {
            case MODE_VERTICAL:
                progressDrawable = ContextCompat.getDrawable(getContext(),
                        R.drawable.sesl_scrubber_progress_vertical);
                break;
            case MODE_SPLIT:
                progressDrawable = ContextCompat.getDrawable(getContext(),
                        R.drawable.sesl_split_seekbar_background_progress);
                break;
            case MODE_EXPAND:
                break;
            case MODE_CIRCLE:
                initializeRoundCicleMode();
                break;
        }

        if (progressDrawable != null) {
            setProgressDrawableTiled(progressDrawable);
        }
    }

    protected void onSlidingRefresh(int level) {
        Drawable d = mCurrentDrawable;
        if (d != null) {
            Drawable progressDrawable = null;
            if (d instanceof LayerDrawable) {
                progressDrawable = ((LayerDrawable) d)
                        .findDrawableByLayerId(android.R.id.progress);
            }
            if (progressDrawable != null) {
                progressDrawable.setLevel(level);
            }
        }
    }

    @Override
    public int getPaddingLeft() {
        return SeslViewReflector.getField_mPaddingLeft(this);
    }

    @Override
    public int getPaddingRight() {
        return SeslViewReflector.getField_mPaddingRight(this);
    }

    private void initCirCleStrokeWidth(int size) {
        if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_size_small) == size) {
            mRoundStrokeWidth = getResources().getDimensionPixelSize(R.dimen.sesl_progress_circle_size_small_width);
            mCirclePadding = getResources().getDimensionPixelOffset(R.dimen.sesl_progress_circle_size_small_padding);
        } else if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_size_small_title) == size) {
            mRoundStrokeWidth = getResources().getDimensionPixelSize(R.dimen.sesl_progress_circle_size_small_title_width);
            mCirclePadding = getResources().getDimensionPixelOffset(R.dimen.sesl_progress_circle_size_small_title_padding);
        } else if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_size_large) == size) {
            mRoundStrokeWidth = getResources().getDimensionPixelSize(R.dimen.sesl_progress_circle_size_large_width);
            mCirclePadding = getResources().getDimensionPixelOffset(R.dimen.sesl_progress_circle_size_large_padding);
        } else if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_size_xlarge) == size) {
            mRoundStrokeWidth = getResources().getDimensionPixelSize(R.dimen.sesl_progress_circle_size_xlarge_width);
            mCirclePadding = getResources().getDimensionPixelOffset(R.dimen.sesl_progress_circle_size_xlarge_padding);
        } else {
            mRoundStrokeWidth = (getResources().getDimensionPixelSize(R.dimen.sesl_progress_circle_size_small_width) * size)
                    / getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_size_small);
            mCirclePadding = (size * getResources().getDimensionPixelOffset(R.dimen.sesl_progress_circle_size_small_padding))
                    / getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_size_small);
        }
    }

    private void seslSetIndeterminateProgressDrawable(int size) {
        if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_indeterminate_xsmall) >= size) {
            setIndeterminateDrawable(mIndeterminateHorizontalXsmall);
        } else if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_indeterminate_small) >= size) {
            setIndeterminateDrawable(mIndeterminateHorizontalSmall);
        } else if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_indeterminate_medium) >= size) {
            setIndeterminateDrawable(mIndeterminateHorizontalMedium);
        } else if (getResources().getDimensionPixelSize(R.dimen.sesl_progress_bar_indeterminate_large) >= size) {
            setIndeterminateDrawable(mIndeterminateHorizontalLarge);
        } else {
            setIndeterminateDrawable(mIndeterminateHorizontalXlarge);
        }
    }

    private static class StateListDrawableCompat {
        private static final boolean IS_BASE_SDK_VERSION = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;

        static int getStateCount(StateListDrawable drawable) {
            if (!IS_BASE_SDK_VERSION) {
                return 0;
            }
            SeslStateListDrawableReflector.getStateCount(drawable);
            return 0;
        }

        static Drawable getStateDrawable(StateListDrawable drawable, int index) {
            if (IS_BASE_SDK_VERSION) {
                return SeslStateListDrawableReflector.getStateDrawable(drawable, index);
            }
            return null;
        }

        static int[] getStateSet(StateListDrawable drawable, int index) {
            if (IS_BASE_SDK_VERSION) {
                return SeslStateListDrawableReflector.getStateSet(drawable, index);
            }
            return null;
        }
    }

    private static class CircleAnimationCallback extends Animatable2.AnimationCallback {
        final Handler mHandler = new Handler(Looper.getMainLooper());
        private WeakReference<SeslProgressBar> mProgressBar;

        public CircleAnimationCallback(SeslProgressBar progressBar) {
            mProgressBar = new WeakReference<>(progressBar);
        }

        @Override
        public void onAnimationEnd(Drawable drawable) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SeslProgressBar progressBar = (SeslProgressBar) mProgressBar.get();
                    if (progressBar != null) {
                        ((AnimatedVectorDrawable) progressBar.mIndeterminateDrawable).start();
                    }
                }
            });
        }
    }

    private ColorStateList colorToColorStateList(int color) {
        int[][] EMPTY = {new int[0]};
        return new ColorStateList(EMPTY, new int[]{color});
    }

    private void initializeRoundCicleMode() {
        mOnlyIndeterminate = false;
        setIndeterminate(false);

        CirCleProgressDrawable background
                = new CirCleProgressDrawable(true,
                colorToColorStateList(getResources().getColor(R.color.sesl_progress_control_color_background)));
        CirCleProgressDrawable primaryProgress
                = new CirCleProgressDrawable(false,
                colorToColorStateList(getResources().getColor(R.color.sesl_progress_control_color_activated_light)));
        Drawable[] drawables = {background, primaryProgress};

        LayerDrawable layer = new LayerDrawable(drawables);
        layer.setPaddingMode(LayerDrawable.PADDING_MODE_STACK);
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.progress);
        setProgressDrawable(layer);
    }

    private class CirCleProgressDrawable extends Drawable {
        int mColor;
        ColorStateList mColorStateList;
        private boolean mIsBackground;
        private final Paint mPaint;
        int mAlpha = 255;
        private RectF mArcRect = new RectF();
        private final ProgressState mState = new ProgressState();
        private final IntProperty<CirCleProgressDrawable> VISUAL_CIRCLE_PROGRESS =
                new IntProperty<CirCleProgressDrawable>("visual_progress") {
            public void setValue(CirCleProgressDrawable d, int value) {
                d.mProgress = value;
                invalidateSelf();
            }

            public Integer get(CirCleProgressDrawable d) {
                return Integer.valueOf(d.mProgress);
            }
        };
        public int mProgress = 0;

        public CirCleProgressDrawable(boolean isBackground, ColorStateList colorStateList) {
            mPaint = new Paint();
            mIsBackground = isBackground;
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mColorStateList = colorStateList;
            mColor = colorStateList.getDefaultColor();
            mPaint.setColor(mColor);
        }

        @Override
        public void draw(Canvas canvas) {
            mPaint.setStrokeWidth(mRoundStrokeWidth);

            final int prevAlpha = mPaint.getAlpha();
            mPaint.setAlpha(modulateAlpha(prevAlpha, mAlpha));
            mPaint.setAntiAlias(true);
            mArcRect.set((((float) mRoundStrokeWidth) / 2.0f) + ((float) mCirclePadding),
                    (((float) mRoundStrokeWidth) / 2.0f) + ((float) mCirclePadding),
                    (((float) SeslProgressBar.this.getWidth()) - (((float) mRoundStrokeWidth) / 2.0f)) - ((float) mCirclePadding),
                    (((float) SeslProgressBar.this.getWidth()) - (((float) mRoundStrokeWidth) / 2.0f)) - ((float) mCirclePadding));

            final int range = mMax - mMin;
            final float scale = range > 0 ? ((float) (mProgress - mMin)) / ((float) range) : 0.0f;
            canvas.save();
            if (mIsBackground) {
                canvas.drawArc(mArcRect, 270.0f, 360.0f, false, mPaint);
            } else {
                canvas.drawArc(mArcRect, 270.0f, scale * 360.0f, false, mPaint);
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

        public void setProgress(int progress, boolean animate) {
            if (animate) {
                ObjectAnimator animator = ObjectAnimator.ofInt(this, VISUAL_CIRCLE_PROGRESS, progress);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    animator.setAutoCancel(true);
                }
                animator.setDuration(PROGRESS_ANIM_DURATION);
                animator.setInterpolator(PROGRESS_ANIM_INTERPOLATOR);
                animator.start();
            } else {
                mProgress = progress;
                SeslProgressBar.this.invalidate();
            }
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
                mColor = tint.getDefaultColor();
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

            return changed;
        }

        @Override
        public Drawable.ConstantState getConstantState() {
            return mState;
        }

        private class ProgressState extends Drawable.ConstantState {
            @Override
            public int getChangingConfigurations() {
                return 0;
            }

            @Override
            public Drawable newDrawable() {
                return CirCleProgressDrawable.this;
            }
        }
    }

    /**
     * Property wrapper around the visual state of the {@code progress} functionality
     * handled by the {@link SeslProgressBar#setProgress(int, boolean)} method. This does
     * not correspond directly to the actual progress -- only the visual state.
     */
    private final FloatProperty<SeslProgressBar> VISUAL_PROGRESS =
            new FloatProperty<SeslProgressBar>("visual_progress") {
                @Override
                public void setValue(SeslProgressBar object, float value) {
                    object.setVisualProgress(android.R.id.progress, value);
                    object.mVisualProgress = value;
                }

                @Override
                public Float get(SeslProgressBar object) {
                    return object.mVisualProgress;
                }
            };
}
