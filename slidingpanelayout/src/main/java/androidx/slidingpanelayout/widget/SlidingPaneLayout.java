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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.appcompat.util.SeslMisc;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.Openable;
import androidx.customview.widget.ViewDragHelper;
import androidx.slidingpanelayout.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung SlidingPaneLayout class.
 */
@RequiresApi(23)
public class SlidingPaneLayout extends ViewGroup implements Openable {
    // Sesl
    private static final String TAG = SlidingPaneLayout.class.getSimpleName();

    private static final float SESL_EXTRA_AREA_SENSITIVITY = 0.1f;

    static final int PENDING_ACTION_NONE = 0;
    static final int PENDING_ACTION_COLLAPSED = 2;
    static final int PENDING_ACTION_COLLAPSED_LOCK = 258;
    static final int PENDING_ACTION_EXPANDED = 1;
    static final int PENDING_ACTION_EXPANDED_LOCK = 257;

    public static final int SESL_PENDING_NONE = PENDING_ACTION_NONE;
    public static final int SESL_PENDING_COLLAPSED = PENDING_ACTION_COLLAPSED;
    public static final int SESL_PENDING_COLLAPSED_LOCK = PENDING_ACTION_COLLAPSED_LOCK;
    public static final int SESL_PENDING_EXPANDED = PENDING_ACTION_EXPANDED;
    public static final int SESL_PENDING_EXPANDED_LOCK = PENDING_ACTION_EXPANDED_LOCK;

    public static final int SESL_STATE_CLOSE = 0;
    public static final int SESL_STATE_OPEN = 1;
    public static final int SESL_STATE_IDLE = 2;

    private int mDoubleCheckState = -1;
    private int mFixedPaneStartX = 0;
    private int mLastValidVelocity = 0;
    private int mMarginBottom = 0;
    private int mMarginTop = 0;
    private int mPendingAction;
    private int mPrevOrientation = Configuration.ORIENTATION_UNDEFINED;
    private int mPrevWindowVisibility = View.VISIBLE;
    private int mRoundedColor = Color.WHITE;
    private int mSlidingPaneDragArea = 0;
    private int mSmoothWidth = 0;
    private int mStartMargin = 0;
    private int mStartSlideX = 0;

    private float mDrawerWidthPercent;
    final float mSlideSlop = 0.15f;
    private float mStartOffset = 0;
    private float mPrevMotionX;

    private View mDrawerPanel;
    private View mResizeChild = null;
    private ArrayList<View> mResizeChildList = null;
    private SlidingPaneRoundedCorner mSlidingPaneRoundedCorner;
    private SeslSlidingState mSlidingState;
    private VelocityTracker mVelocityTracker = null;

    private boolean mDrawRoundedCorner;
    private boolean mIsAnimate;
    private boolean mIsLock = false;
    private boolean mIsNeedBlockDim = false;
    private boolean mIsNeedClose = false;
    private boolean mIsNeedOpen = false;
    private boolean mIsSinglePanel;
    boolean mIsSlideableViewTouched;
    private boolean mResizeOff;
    private boolean mSetCustomPendingAction = false;
    private boolean mSetResizeChild = false;
    // Sesl

    /**
     * Default size of the overhang for a pane in the open state.
     * At least this much of a sliding pane will remain visible.
     * This indicates that there is more content available and provides
     * a "physical" edge to grab to pull it closed.
     */
    private static final int DEFAULT_OVERHANG_SIZE = 32; // dp;

    /**
     * If no fade color is given by default it will fade to 80% gray.
     */
    private static final int DEFAULT_FADE_COLOR = 0xcccccccc;

    /**
     * The fade color used for the sliding panel. 0 = no fading.
     */
    private int mSliderFadeColor = DEFAULT_FADE_COLOR;

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /** Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage. */
    private static final String ACCESSIBILITY_CLASS_NAME =
            "androidx.slidingpanelayout.widget.SlidingPaneLayout";

    /**
     * The fade color used for the panel covered by the slider. 0 = no fading.
     */
    private int mCoveredFadeColor;

    /**
     * Drawable used to draw the shadow between panes by default.
     */
    private Drawable mShadowDrawableLeft;

    /**
     * Drawable used to draw the shadow between panes to support RTL (right to left language).
     */
    private Drawable mShadowDrawableRight;

    /**
     * The size of the overhang in pixels.
     * This is the minimum section of the sliding panel that will
     * be visible in the open state to allow for a closing drag.
     */
    private final int mOverhangSize;

    /**
     * True if a panel can slide with the current measurements
     */
    private boolean mCanSlide;

    /**
     * The child view that can slide, if any.
     */
    View mSlideableView;

    /**
     * How far the panel is offset from its closed position.
     * range [0, 1] where 0 = closed, 1 = open.
     */
    float mSlideOffset;

    /**
     * How far the non-sliding panel is parallaxed from its usual position when open.
     * range [0, 1]
     */
    private float mParallaxOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    int mSlideRange;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    boolean mIsUnableToDrag;

    /**
     * Distance in pixels to parallax the fixed pane by when fully closed
     */
    private int mParallaxBy;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private PanelSlideListener mPanelSlideListener;

    final ViewDragHelper mDragHelper;

    /**
     * Stores whether or not the pane was open the last time it was slideable.
     * If open/close operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    boolean mPreservedOpenState;
    private boolean mFirstLayout = true;

    private final Rect mTmpRect = new Rect();

    final ArrayList<DisableLayerRunnable> mPostedRunnables = new ArrayList<>();

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelSlideListener {
        /**
         * Called when a sliding pane's position changes.
         * @param panel The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        void onPanelSlide(@NonNull View panel, float slideOffset);
        /**
         * Called when a sliding pane becomes slid completely open. The pane may or may not
         * be interactive at this point depending on how much of the pane is visible.
         * @param panel The child view that was slid to an open position, revealing other panes
         */
        void onPanelOpened(@NonNull View panel);

        /**
         * Called when a sliding pane becomes slid completely closed. The pane is now guaranteed
         * to be interactive. It may now obscure other views in the layout.
         * @param panel The child view that was slid to a closed position
         */
        void onPanelClosed(@NonNull View panel);
    }

    /**
     * No-op stubs for {@link PanelSlideListener}. If you only want to implement a subset
     * of the listener methods you can extend this instead of implement the full interface.
     */
    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
        }
        @Override
        public void onPanelOpened(View panel) {
        }
        @Override
        public void onPanelClosed(View panel) {
        }
    }

    public static class SeslSlidingState {
        private int mCurrentState = SESL_STATE_IDLE;

        SeslSlidingState() { }

        void onStateChanged(int state) {
            mCurrentState = state;
        }

        public int getState() {
            return mCurrentState;
        }
    }

    public SlidingPaneLayout(@NonNull Context context) {
        this(context, null);
    }

    public SlidingPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("ResourceAsColor")
    public SlidingPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final float density = context.getResources().getDisplayMetrics().density;
        mOverhangSize = (int) (DEFAULT_OVERHANG_SIZE * density + 0.5f);

        setWillNotDraw(false);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingPaneLayout, defStyle, 0);
        mIsSinglePanel = a.getBoolean(R.styleable.SlidingPaneLayout_seslIsSinglePanel, false);
        mDrawRoundedCorner = a.getBoolean(R.styleable.SlidingPaneLayout_seslDrawRoundedCorner, true);
        mRoundedColor = a.getColor(R.styleable.SlidingPaneLayout_seslDrawRoundedCornerColor,
                SeslMisc.isLightTheme(context) ? getResources().getColor(R.color.sesl_sliding_pane_background_light, null)
                        : getResources().getColor(R.color.sesl_sliding_pane_background_dark, null));
        mResizeOff = a.getBoolean(R.styleable.SlidingPaneLayout_seslResizeOff, false);
        mMarginTop = a.getDimensionPixelSize(R.styleable.SlidingPaneLayout_seslDrawerMarginTop, 0);
        mMarginBottom = a.getDimensionPixelSize(R.styleable.SlidingPaneLayout_seslDrawerMarginBottom, 0);
        a.recycle();

        mDragHelper = ViewDragHelper.seslCreate(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * density);
        mDragHelper.seslSetUpdateOffsetLR(mResizeOff);

        if (mDrawRoundedCorner) {
            mSlidingPaneRoundedCorner = new SlidingPaneRoundedCorner(context);
            mSlidingPaneRoundedCorner.setRoundedCorners(0);
            mSlidingPaneRoundedCorner.setMarginTop(mMarginTop);
            mSlidingPaneRoundedCorner.setMarginBottom(mMarginBottom);
        }

        final boolean isDefaultOpen = getResources().getBoolean(R.dimen.sesl_sliding_layout_default_open);

        mSlidingPaneDragArea = getResources().getDimensionPixelSize(R.dimen.sesl_sliding_pane_contents_drag_width_default);
        mPendingAction = !isDefaultOpen ? SESL_PENDING_COLLAPSED : SESL_PENDING_EXPANDED;
        mPrevOrientation = getResources().getConfiguration().orientation;
        mSlidingState = new SeslSlidingState();
    }

    /**
     * Set a distance to parallax the lower pane by when the upper pane is in its
     * fully closed state. The lower pane will scroll between this position and
     * its fully open state.
     *
     * @param parallaxBy Distance to parallax by in pixels
     */
    public void setParallaxDistance(@Px int parallaxBy) {
        mParallaxBy = parallaxBy;
        requestLayout();
    }

    /**
     * @return The distance the lower pane will parallax by when the upper pane is fully closed.
     *
     * @see #setParallaxDistance(int)
     */
    @Px
    public int getParallaxDistance() {
        return mParallaxBy;
    }

    /**
     * Set the color used to fade the sliding pane out when it is slid most of the way offscreen.
     *
     * @param color An ARGB-packed color value
     */
    public void setSliderFadeColor(@ColorInt int color) {
        mSliderFadeColor = color;
    }

    /**
     * @return The ARGB-packed color value used to fade the sliding pane
     */
    @ColorInt
    public int getSliderFadeColor() {
        return mSliderFadeColor;
    }

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane
     * will become fully covered in the closed state.
     *
     * @param color An ARGB-packed color value
     */
    public void setCoveredFadeColor(@ColorInt int color) {
        mCoveredFadeColor = color;
    }

    /**
     * @return The ARGB-packed color value used to fade the fixed pane
     */
    @ColorInt
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    public void setPanelSlideListener(@Nullable PanelSlideListener listener) {
        mPanelSlideListener = listener;
    }

    void dispatchOnPanelSlide(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelSlide(panel, mSlideOffset);
        }
        if (!mResizeOff) {
            resizeSlideableView(mSlideOffset);
        }
    }

    public void seslSetDrawerMarginTop(int marginTop) {
        if (mMarginTop != marginTop) {
            mMarginTop = marginTop;
            if (mSlidingPaneRoundedCorner != null) {
                mSlidingPaneRoundedCorner.setMarginTop(marginTop);
            }
            requestLayout();
        }
    }

    public void seslSetDrawerMarginBottom(int marginBottom) {
        if (mMarginBottom != marginBottom) {
            mMarginBottom = marginBottom;
            if (mSlidingPaneRoundedCorner != null) {
                mSlidingPaneRoundedCorner.setMarginBottom(marginBottom);
            }
            requestLayout();
        }
    }

    public void seslSetResizeOff(boolean resizeOff) {
        mResizeOff = resizeOff;
        if (mDragHelper != null) {
            mDragHelper.seslSetUpdateOffsetLR(resizeOff);
        }
    }

    public boolean seslGetReiszeOff() {
        return mResizeOff;
    }

    // TODO rework this method
    // kang
    void resizeSlideableView(float slideOffset) {
        int width = (getWidth() - getPaddingLeft()) - getPaddingRight();
        View view = this.mSlideableView;
        if (view instanceof ViewGroup) {
            int paddingStart = view.getPaddingStart() + this.mSlideableView.getPaddingEnd();
            ViewGroup viewGroup = (ViewGroup) this.mSlideableView;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = viewGroup.getChildAt(i);
                ViewGroup.LayoutParams layoutParams = childAt.getLayoutParams();
                if (layoutParams != null) {
                    int paddingStart2 = (((width - this.mStartSlideX) - ((int) (this.mSlideRange * slideOffset))) - paddingStart)
                            - (childAt.getPaddingStart() + childAt.getPaddingEnd());
                    int min = Math.min(paddingStart2, (int) (width * ResourcesCompat.getFloat(getResources(),
                            R.dimen.sesl_sliding_pane_contents_width_percent)));
                    ViewGroup.LayoutParams layoutParams2 = null;
                    if (this.mSetResizeChild) {
                        ArrayList<View> arrayList = this.mResizeChildList;
                        if (arrayList == null) {
                            View view2 = this.mResizeChild;
                            if (view2 != null) {
                                layoutParams2 = view2.getLayoutParams();
                            }
                            if (layoutParams2 != null) {
                                layoutParams2.width = min;
                            }
                        } else {
                            Iterator<View> it = arrayList.iterator();
                            while (it.hasNext()) {
                                View next = it.next();
                                if (next != null) {
                                    next.getLayoutParams().width = min;
                                } else {
                                    return;
                                }
                            }
                        }
                    } else if (this.mIsSinglePanel && !isToolbar(childAt)) {
                        if (childAt instanceof CoordinatorLayout) {
                            findResizeChild(childAt);
                            View view3 = this.mResizeChild;
                            if (view3 != null) {
                                layoutParams2 = view3.getLayoutParams();
                            }
                            if (layoutParams2 != null) {
                                layoutParams2.width = min;
                            }
                        } else {
                            paddingStart2 = min;
                        }
                    }
                    layoutParams.width = paddingStart2;
                    childAt.requestLayout();
                }
            }
        }
    }
    // kang

    public void seslSetResizeChild(View child) {
        mSetResizeChild = true;
        mResizeChild = child;
        mResizeChildList = null;
    }

    public void seslSetResizeChild(ArrayList<View> children) {
        mSetResizeChild = true;
        if (mResizeChildList == null) {
            mResizeChildList = new ArrayList<>();
        }
        mResizeChildList = children;
    }

    private void findResizeChild(View coordinator) {
        if (!mSetResizeChild
                && (coordinator instanceof ViewGroup)) {
            ViewGroup vg = (ViewGroup) coordinator;
            if (vg.getChildCount() == 2) {
                mResizeChild = vg.getChildAt(1);
            }
        }
    }

    private boolean isToolbar(View child) {
        return (child instanceof Toolbar)
                || (child instanceof android.widget.Toolbar)
                || (child instanceof SPLToolbarContainer);
    }

    void dispatchOnPanelOpened(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelOpened(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelClosed(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelClosed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewsVisibility(View panel) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final int startBound = isLayoutRtl ? (getWidth() - getPaddingRight()) : getPaddingLeft();
        final int endBound = isLayoutRtl ? getPaddingLeft() : (getWidth() - getPaddingRight());
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (panel != null && viewIsOpaque(panel)) {
            left = panel.getLeft();
            right = panel.getRight();
            top = panel.getTop();
            bottom = panel.getBottom();
        } else {
            left = right = top = bottom = 0;
        }

        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);

            if (child == panel) {
                // There are still more children above the panel but they won't be affected.
                break;
            } else if (child.getVisibility() == GONE) {
                continue;
            }

            final int clampedChildLeft = Math.max(
                    (isLayoutRtl ? endBound : startBound), child.getLeft());
            final int clampedChildTop = Math.max(topBound, child.getTop());
            final int clampedChildRight = Math.min(
                    (isLayoutRtl ? startBound : endBound), child.getRight());
            final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
            final int vis;
            if (clampedChildLeft >= left && clampedChildTop >= top
                    && clampedChildRight <= right && clampedChildBottom <= bottom) {
                vis = INVISIBLE;
            } else {
                vis = VISIBLE;
            }
            child.setVisibility(vis);
        }
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    @SuppressWarnings("deprecation")
    // Remove suppression once b/120984816 is addressed.
    private static boolean viewIsOpaque(View v) {
        if (v.isOpaque()) {
            return true;
        }

        // View#isOpaque didn't take all valid opaque scrollbar modes into account
        // before API 18 (JB-MR2). On newer devices rely solely on isOpaque above and return false
        // here. On older devices, check the view's background drawable directly as a fallback.
        if (Build.VERSION.SDK_INT >= 18) {
            return false;
        }

        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;

        for (int i = 0, count = mPostedRunnables.size(); i < count; i++) {
            final DisableLayerRunnable dlr = mPostedRunnables.get(i);
            dlr.run();
        }
        mPostedRunnables.clear();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!mSetCustomPendingAction) {
            if (!isOpen()
                    || (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
                            && mPrevOrientation == Configuration.ORIENTATION_LANDSCAPE)) {
                mPendingAction = SESL_PENDING_COLLAPSED;
            } else {
                mPendingAction = SESL_PENDING_EXPANDED;
            }
        }

        if (mIsLock) {
            if (isOpen()) {
                mPendingAction = SESL_PENDING_EXPANDED;
            } else {
                mPendingAction = SESL_PENDING_COLLAPSED;
            }
        }

        mPrevOrientation = newConfig.orientation;
        mDrawerWidthPercent = ResourcesCompat.getFloat(getResources(), R.dimen.sesl_sliding_pane_drawer_width_percent);
        setDrawerPaneWidth();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        if ((mPrevWindowVisibility == View.GONE
                || mPrevWindowVisibility == View.INVISIBLE) && visibility == View.VISIBLE) {
            if (isOpen()) {
                mPendingAction = SESL_PENDING_EXPANDED;
            } else {
                mPendingAction = SESL_PENDING_COLLAPSED;
            }
        }

        if (mPrevWindowVisibility != visibility) {
            mPrevWindowVisibility = visibility;
        }
    }

    // TODO rework this method
    // kang
    @Override
    protected void onMeasure(int var1, int var2) {
        int var3 = MeasureSpec.getMode(var1);
        int var4 = MeasureSpec.getSize(var1);
        int var5 = MeasureSpec.getMode(var2);
        int var6 = MeasureSpec.getSize(var2);
        int var7;
        if (var3 != 1073741824) {
            if (!this.isInEditMode()) {
                throw new
                        IllegalStateException("Width must have an exact value or MATCH_PARENT");
            }

            if (var3 == -2147483648) {
                var2 = var4;
                var7 = var5;
                var1 = var6;
            } else {
                var2 = var4;
                var7 = var5;
                var1 = var6;
                if (var3 == 0) {
                    var2 = 300;
                    var7 = var5;
                    var1 = var6;
                }
            }
        } else {
            var2 = var4;
            var7 = var5;
            var1 = var6;
            if (var5 == 0) {
                if (!this.isInEditMode()) {
                    throw new IllegalStateException("Height must not be UNSPECIFIED");
                }

                var2 = var4;
                var7 = var5;
                var1 = var6;
                if (var5 == 0) {
                    var1 = 300;
                    var7 = -2147483648;
                    var2 = var4;
                }
            }
        }

        if (var7 != -2147483648) {
            if (var7 != 1073741824) {
                var1 = 0;
            } else {
                var1 = var1 - this.getPaddingTop() - this.getPaddingBottom();
            }

            var6 = var1;
        } else {
            var6 = var1 - this.getPaddingTop() - this.getPaddingBottom();
            var1 = 0;
        }

        int var8 = var2 - this.getPaddingLeft() - this.getPaddingRight();
        int var9 = this.getChildCount();
        if (var9 > 2) {
            Log.e(TAG, "onMeasure: More than two child views are not supported.");
        }

        this.mSlideableView = null;
        this.mDrawerPanel = null;
        int var10 = 0;
        var3 = var10;
        int var11 = var8;
        float var12 = 0.0F;

        float var15;
        int var16;
        for(var4 = var1; var10 < var9; var12 = var15) {
            View var13 = this.getChildAt(var10);
            androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams var14
                    = (androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams)
                    var13.getLayoutParams();
            if (var13.getVisibility() == GONE) {
                var14.dimWhenOffset = false;
                var1 = var4;
                var15 = var12;
            } else {
                label196: {
                    var15 = var12;
                    if (var14.weight > 0.0F) {
                        var12 += var14.weight;
                        var15 = var12;
                        if (var14.width == 0) {
                            var1 = var4;
                            var15 = var12;
                            break label196;
                        }
                    }

                    var1 = var14.leftMargin + var14.rightMargin;
                    if (var14.width == -2) {
                        if (var14.slideable) {
                            var1 = MeasureSpec.makeMeasureSpec(var8 - var1, MeasureSpec.AT_MOST);
                        } else {
                            var12 = ResourcesCompat.getFloat(this.getResources(),
                                    R.dimen.sesl_sliding_pane_drawer_width_percent);
                            this.mDrawerWidthPercent = var12;
                            var14.width = (int)((float)var2 * var12);
                            var1 = MeasureSpec.makeMeasureSpec(var14.width, MeasureSpec.EXACTLY);
                        }
                    } else if (var14.width == -1) {
                        var1 = MeasureSpec.makeMeasureSpec(var8 - var1, MeasureSpec.EXACTLY);
                    } else {
                        var1 = MeasureSpec.makeMeasureSpec(var14.width, MeasureSpec.EXACTLY);
                    }

                    if (var14.height == -2) {
                        var5 = MeasureSpec.makeMeasureSpec(var6, MeasureSpec.AT_MOST);
                    } else if (var14.height == -1) {
                        var5 = MeasureSpec.makeMeasureSpec(var6, MeasureSpec.EXACTLY);
                    } else {
                        var5 = MeasureSpec.makeMeasureSpec(var14.height, MeasureSpec.EXACTLY);
                    }

                    var13.measure(var1, var5);
                    var5 = var13.getMeasuredWidth();
                    var16 = var13.getMeasuredHeight();
                    var1 = var4;
                    if (var7 == -2147483648) {
                        var1 = var4;
                        if (var16 > var4) {
                            var1 = Math.min(var16, var6);
                        }
                    }

                    var11 -= var5;
                    boolean var17;
                    if (var11 <= 0) {
                        var17 = true;
                    } else {
                        var17 = false;
                    }

                    var14.slideable = (boolean)var17;
                    var3 |= var17 ? 1 : 0;
                    if (var14.slideable) {
                        this.mSlideableView = var13;
                    } else {
                        this.mDrawerPanel = var13;
                    }
                }
            }

            ++var10;
            var4 = var1;
        }

        if (var3 != 0 || var12 > 0.0F) {
            var7 = var8 - this.mOverhangSize;

            for(var5 = 0; var5 < var9; ++var5) {
                View var21 = this.getChildAt(var5);
                if (var21.getVisibility() != GONE) {
                    androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams var20
                            = (androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams)
                            var21.getLayoutParams();
                    if (var21.getVisibility() != GONE) {
                        boolean var19;
                        if (var20.width == 0 && var20.weight > 0.0F) {
                            var19 = true;
                        } else {
                            var19 = false;
                        }

                        if (var19) {
                            var10 = 0;
                        } else {
                            var10 = var21.getMeasuredWidth();
                        }

                        if (var3 != 0 && var21 != this.mSlideableView) {
                            if (var20.width < 0 && (var10 > var7 || var20.weight > 0.0F)) {
                                if (var19) {
                                    if (var20.height == -2) {
                                        var1 = MeasureSpec.makeMeasureSpec(var6, MeasureSpec.AT_MOST);
                                    } else if (var20.height == -1) {
                                        var1 = MeasureSpec.makeMeasureSpec(var6, MeasureSpec.EXACTLY);
                                    } else {
                                        var1 = MeasureSpec.makeMeasureSpec(var20.height, MeasureSpec.EXACTLY);
                                    }
                                } else {
                                    var1 = MeasureSpec.makeMeasureSpec(var21.getMeasuredHeight(), MeasureSpec.EXACTLY);
                                }

                                var21.measure(MeasureSpec.makeMeasureSpec(var7, MeasureSpec.EXACTLY), var1);
                            }
                        } else if (var20.weight > 0.0F) {
                            if (var20.width == 0) {
                                if (var20.height == -2) {
                                    var1 = MeasureSpec.makeMeasureSpec(var6, MeasureSpec.AT_MOST);
                                } else if (var20.height == -1) {
                                    var1 = MeasureSpec.makeMeasureSpec(var6, MeasureSpec.EXACTLY);
                                } else {
                                    var1 = MeasureSpec.makeMeasureSpec(var20.height, MeasureSpec.EXACTLY);
                                }
                            } else {
                                var1 = MeasureSpec.makeMeasureSpec(var21.getMeasuredHeight(), MeasureSpec.EXACTLY);
                            }

                            if (var3 != 0) {
                                var16 = var8 - (var20.leftMargin + var20.rightMargin);
                                int var18 = MeasureSpec.makeMeasureSpec(var16, MeasureSpec.EXACTLY);
                                if (var10 != var16) {
                                    var21.measure(var18, var1);
                                }
                            } else {
                                var16 = Math.max(0, var11);
                                var21.measure(MeasureSpec.makeMeasureSpec(var10 + (int)(var20.weight
                                        * (float)var16 / var12), MeasureSpec.EXACTLY), var1);
                            }
                        }
                    }
                }
            }
        }

        var1 = this.getWindowWidth();
        if (var1 > 0) {
            var2 = var1;
        }

        this.setMeasuredDimension(var2, var4 + this.getPaddingTop() + this.getPaddingBottom());
        this.mCanSlide = var3 == 1;
        if (this.mDragHelper.getViewDragState() != 0 && var3 == 0) {
            this.mDragHelper.abort();
        }

    }
    // kang

    // TODO rework this method
    // kang
    @Override
    protected void onLayout(boolean var1, int var2, int var3, int var4, int var5) {
        boolean var6 = this.isLayoutRtlSupport();
        if (var6) {
            this.mDragHelper.setEdgeTrackingEnabled(2);
        } else {
            this.mDragHelper.setEdgeTrackingEnabled(1);
        }

        int var7 = var4 - var2;
        if (var6) {
            var2 = this.getPaddingRight();
        } else {
            var2 = this.getPaddingLeft();
        }

        if (var6) {
            var5 = this.getPaddingLeft();
        } else {
            var5 = this.getPaddingRight();
        }

        int var8 = this.getPaddingTop();
        int var9 = this.getChildCount();
        if (this.mFirstLayout) {
            float var10;
            if (!this.mCanSlide || !this.mPreservedOpenState && this.mPendingAction != 1) {
                var10 = 0.0F;
            } else {
                var10 = 1.0F;
            }

            this.mSlideOffset = var10;
        }

        var3 = var2;

        for(int var11 = 0; var11 < var9; ++var11) {
            View var12 = this.getChildAt(var11);
            if (var12.getVisibility() != GONE) {
                androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams var13;
                int var14;
                int var15;
                int var16;
                label148: {
                    var13 = (androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams)var12.getLayoutParams();
                    var14 = var12.getMeasuredWidth();
                    if (var13.slideable) {
                        var15 = var13.leftMargin;
                        var4 = var13.rightMargin;
                        var16 = var7 - var5;
                        var15 = Math.min(var2, var16 - this.mOverhangSize) - var3 - (var15 + var4);
                        if (var6) {
                            var4 = var13.rightMargin;
                        } else {
                            var4 = var13.leftMargin;
                        }

                        this.mStartMargin = var4;
                        this.mSlideRange = var15;
                        if (var3 + var4 + var15 + var14 / 2 > var16) {
                            var1 = true;
                        } else {
                            var1 = false;
                        }

                        var13.dimWhenOffset = var1;
                        var16 = (int)((float)var15 * this.mSlideOffset);
                        var4 = var3 + var4 + var16;
                        this.mSlideOffset = (float)var16 / (float)this.mSlideRange;
                    } else {
                        if (this.mCanSlide) {
                            var3 = this.mParallaxBy;
                            if (var3 != 0) {
                                var3 = (int)((1.0F - this.mSlideOffset) * (float)var3);
                                var4 = var2;
                                break label148;
                            }
                        }

                        var4 = var2;
                    }

                    var3 = 0;
                }

                if (var6) {
                    label139: {
                        var15 = var7 - var4 + var3;
                        if (this.mResizeOff) {
                            if (var13.slideable) {
                                var3 = var15 - (var7 - this.mStartMargin);
                                break label139;
                            }
                        } else if (var13.slideable) {
                            var3 = -this.getLeft();
                            break label139;
                        }

                        var3 = var15 - var14;
                    }

                    this.mFixedPaneStartX = 0;
                    var16 = var3;
                    var3 = var15;
                } else {
                    label133: {
                        var16 = var4 - var3;
                        if (this.mResizeOff) {
                            if (var13.slideable) {
                                var3 = var7 - this.mStartMargin + var16;
                                break label133;
                            }
                        } else if (var13.slideable) {
                            var3 = this.getRight();
                            break label133;
                        }

                        var3 = var16 + var14;
                    }

                    this.mFixedPaneStartX = var13.leftMargin;
                }

                if (var6) {
                    var15 = var13.rightMargin;
                } else {
                    var15 = var13.leftMargin;
                }

                this.mStartSlideX = var15;
                var12.layout(var16, var8, var3, var12.getMeasuredHeight() + var8);
                var2 += var12.getWidth();
                var3 = var4;
            }
        }

        if (this.mFirstLayout) {
            if (this.mCanSlide) {
                if (this.mParallaxBy != 0) {
                    this.parallaxOtherViews(this.mSlideOffset);
                }

                if (((androidx.slidingpanelayout.widget.SlidingPaneLayout.LayoutParams)
                        this.mSlideableView.getLayoutParams()).dimWhenOffset) {
                    this.dimChildView(this.mSlideableView, this.mSlideOffset, this.mSliderFadeColor);
                }
            } else {
                for(var2 = 0; var2 < var9; ++var2) {
                    this.dimChildView(this.getChildAt(var2), 0.0F, this.mSliderFadeColor);
                }
            }

            this.updateObscuredViewsVisibility(this.mSlideableView);
        }

        this.mFirstLayout = false;
        var2 = this.mPendingAction;
        if (var2 == 1) {
            if (this.mIsLock) {
                this.resizeSlideableView(1.0F);
            }

            this.openPane(0, false);
            this.mPendingAction = 0;
        } else if (var2 == 2) {
            if (this.mIsLock) {
                this.resizeSlideableView(0.0F);
            }

            this.closePane(0, false);
            this.mPendingAction = 0;
        } else if (var2 == 257) {
            this.mIsLock = false;
            this.openPane(0, false);
            this.mIsLock = true;
            this.mPendingAction = 0;
        } else if (var2 == 258) {
            this.mIsLock = false;
            this.closePane(0, false);
            this.mIsLock = true;
            this.mPendingAction = 0;
        }

        this.updateSlidingState();
        var2 = this.mDoubleCheckState;
        if (var2 != -1) {
            if (var2 == 1) {
                this.openPane(0, true);
            } else if (var2 == 0) {
                this.closePane(0, true);
            }

            this.mDoubleCheckState = -1;
        }

    }
    // kang

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (w != oldw) {
            mFirstLayout = true;
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (!isInTouchMode() && !mCanSlide) {
            mPreservedOpenState = child == mSlideableView;
        }
    }

    // TODO rework this method
    // kang
    @Override
    public boolean onInterceptTouchEvent(MotionEvent var1) {
        int var2 = var1.getActionMasked();
        if (!this.mCanSlide || this.mIsLock || this.mIsUnableToDrag && var2 != 0) {
            this.mDragHelper.cancel();
            return super.onInterceptTouchEvent(var1);
        } else {
            boolean var3;
            boolean var11;
            label129: {
                var3 = false;
                float var4;
                float var5;
                int var7;
                if (var2 != 0) {
                    label140: {
                        if (var2 != 1) {
                            if (var2 == 2) {
                                var4 = var1.getX();
                                var5 = var1.getY();
                                float var6 = Math.abs(var4 - this.mInitialMotionX);
                                Math.abs(var5 - this.mInitialMotionY);
                                var7 = this.mDragHelper.getTouchSlop();
                                float var8 = this.mPrevMotionX;
                                var5 = var4 - var8;
                                if (var8 != var4) {
                                    this.mPrevMotionX = var4;
                                }

                                if (!this.mIsUnableToDrag && var6 > (float)var7) {
                                    if (this.isLayoutRtlSupport()) {
                                        if (this.mResizeOff) {
                                            var5 = (float)(this.mSlideableView.getRight() - this.getWindowWidth() + this.mStartMargin);
                                        }
                                    } else {
                                        var5 = Math.max((float)this.mSlideableView.getLeft() + var5, (float)this.mStartMargin);
                                    }

                                    this.onPanelDragged((int)var5);
                                    return true;
                                }
                                break label140;
                            }

                            if (var2 != 3) {
                                break label140;
                            }
                        }

                        if (Math.abs(this.mStartOffset - this.mSlideOffset) < 0.1F) {
                            return false;
                        }

                        this.mSmoothWidth = this.mSlideableView.getWidth();
                        this.mDoubleCheckState = -1;
                        var5 = this.mStartOffset;
                        if (var5 == 0.0F) {
                            if (this.mSlideOffset > 0.15F) {
                                this.mIsNeedOpen = true;
                                this.mIsNeedClose = false;
                            } else {
                                this.mIsNeedOpen = false;
                                this.mIsNeedClose = true;
                            }
                        }

                        if (var5 == 1.0F) {
                            if (this.mSlideOffset < var5 - 0.15F) {
                                this.mIsNeedOpen = false;
                                this.mIsNeedClose = true;
                            } else {
                                this.mIsNeedOpen = true;
                                this.mIsNeedClose = false;
                            }
                        }

                        if (!this.mIsAnimate) {
                            var5 = this.mSlideOffset;
                            if (var5 != 0.0F && var5 != 1.0F) {
                                if (this.mIsNeedOpen) {
                                    this.mDoubleCheckState = 1;
                                    this.openPane(0, true);
                                } else if (this.mIsNeedClose) {
                                    this.mDoubleCheckState = 0;
                                    this.closePane(0, true);
                                } else if (var5 > 0.5F) {
                                    this.mDoubleCheckState = 1;
                                    this.openPane(0, true);
                                } else {
                                    this.mDoubleCheckState = 0;
                                    this.closePane(0, true);
                                }

                                return true;
                            }
                        }
                    }
                } else {
                    this.mStartOffset = this.mSlideOffset;
                    this.mIsNeedOpen = false;
                    this.mIsNeedClose = false;
                    this.mIsUnableToDrag = false;
                    var4 = var1.getX();
                    var5 = var1.getY();
                    this.mInitialMotionX = var4;
                    this.mInitialMotionY = var5;
                    this.mSmoothWidth = 0;
                    this.mPrevMotionX = var4;
                    if (this.isLayoutRtlSupport()) {
                        var7 = this.mSlideableView.getRight();
                    } else {
                        var7 = this.mSlideableView.getLeft();
                    }

                    if (this.isLayoutRtlSupport()) {
                        if (var4 < (float)(var7 - this.mSlidingPaneDragArea) || this.mIsLock) {
                            this.mDragHelper.cancel();
                            this.mIsUnableToDrag = true;
                        }
                    } else if (var4 > (float)(var7 + this.mSlidingPaneDragArea) || this.mIsLock) {
                        this.mDragHelper.cancel();
                        this.mIsUnableToDrag = true;
                    }

                    boolean var9 = this.mDragHelper.isViewUnder(this.mSlideableView, (int)var4, (int)var5);
                    this.mIsSlideableViewTouched = var9;
                    if (var9 && this.isDimmed(this.mSlideableView)) {
                        var11 = true;
                        break label129;
                    }
                }

                var11 = false;
            }

            if (!this.mCanSlide && var2 == 0 && this.getChildCount() > 1) {
                View var10 = this.getChildAt(1);
                if (var10 != null) {
                    this.mPreservedOpenState = this.mDragHelper.isViewUnder(var10, (int)var1.getX(), (int)var1.getY()) ^ true;
                }
            }

            if (var2 != 3 && var2 != 1) {
                if (this.mDragHelper.shouldInterceptTouchEvent(var1) || var11) {
                    var3 = true;
                }

                return var3;
            } else {
                this.mDragHelper.cancel();
                return false;
            }
        }
    }
    // kang

    private void setVelocityTracker(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        mVelocityTracker.addMovement(ev);
    }

    // TODO rework this method
    // kang
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int right;
        int i;
        if (!this.mCanSlide || this.mIsLock) {
            return super.onTouchEvent(ev);
        }
        this.mDragHelper.processTouchEvent(ev);
        setVelocityTracker(ev);
        int actionMasked = ev.getActionMasked();
        if (actionMasked != 0) {
            float f = 1.0f;
            if (actionMasked == 1) {
                VelocityTracker velocityTracker = this.mVelocityTracker;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                if (isDimmed(this.mSlideableView)) {
                    float x = ev.getX();
                    float y = ev.getY();
                    float f2 = x - this.mInitialMotionX;
                    float f3 = y - this.mInitialMotionY;
                    int touchSlop = this.mDragHelper.getTouchSlop();
                    if ((f2 * f2) + (f3 * f3) < touchSlop * touchSlop
                            && this.mDragHelper.isViewUnder(this.mSlideableView, (int) x, (int) y)) {
                        closePane(0, true);
                    }
                }
                this.mSmoothWidth = this.mSlideableView.getWidth();
                this.mDoubleCheckState = -1;
                float f4 = this.mStartOffset;
                if (f4 == 0.0f) {
                    if (this.mSlideOffset > 0.15f) {
                        this.mIsNeedOpen = true;
                        this.mIsNeedClose = false;
                    } else {
                        this.mIsNeedOpen = false;
                        this.mIsNeedClose = true;
                    }
                }
                if (f4 == 1.0f) {
                    if (this.mSlideOffset < f4 - 0.15f) {
                        this.mIsNeedOpen = false;
                        this.mIsNeedClose = true;
                    } else {
                        this.mIsNeedOpen = true;
                        this.mIsNeedClose = false;
                    }
                }
                float f5 = this.mSlideOffset;
                if (!(f5 == 0.0f || f5 == 1.0f)) {
                    if (this.mIsNeedOpen) {
                        this.mDoubleCheckState = 1;
                        openPane(0, true);
                    } else if (this.mIsNeedClose) {
                        this.mDoubleCheckState = 0;
                        closePane(0, true);
                    } else if (f5 > 0.5f) {
                        this.mDoubleCheckState = 1;
                        openPane(0, true);
                    } else {
                        this.mDoubleCheckState = 0;
                        closePane(0, true);
                    }
                }
            } else if (actionMasked == 2) {
                float x2 = ev.getX();
                float y2 = ev.getY();
                float abs = Math.abs(x2 - this.mInitialMotionX);
                float f6 = this.mPrevMotionX;
                float f7 = x2 - f6;
                if (f6 != x2) {
                    this.mPrevMotionX = x2;
                }
                Math.abs(y2 - this.mInitialMotionY);
                int touchSlop2 = this.mDragHelper.getTouchSlop();
                if (!this.mIsUnableToDrag && abs > touchSlop2) {
                    if (this.mIsSlideableViewTouched) {
                        if (!isLayoutRtlSupport()) {
                            int right2 = isLayoutRtlSupport() ? this.mSlideableView.getRight()
                                    : this.mSlideableView.getLeft();
                            int width = this.mSlideableView.getWidth();
                            if (right2 == 0) {
                                right2 = 1;
                            }
                            int i2 = width / right2;
                            float left = this.mSlideableView.getLeft();
                            if (i2 == 0) {
                                i2 = 1;
                            }
                            f7 = Math.max(left + (f7 / i2), this.mStartMargin);
                            if (this.mResizeOff) {
                                this.mSlideableView.setLeft((int) Math.max(this.mStartMargin, f7));
                                this.mSlideableView.setRight((this.mSlideableView.getLeft()
                                        + getWindowWidth()) - this.mStartMargin);
                            }
                        } else if (this.mResizeOff) {
                            right = this.mSlideableView.getRight() - getWindowWidth();
                            i = this.mStartMargin;
                            f7 = right + i;
                        }
                        onPanelDragged((int) f7);
                    } else {
                        if (isLayoutRtlSupport()) {
                            float max = Math.max(Math.min(this.mSlideableView.getRight() + f7,
                                    getWidth() - this.mStartMargin), (getWidth() - this.mStartMargin)
                                    - this.mSlideRange);
                            if (this.mResizeOff) {
                                this.mSlideableView.setRight((int) max);
                                this.mSlideableView.setLeft((this.mSlideableView.getRight()
                                        - getWindowWidth()) + this.mStartMargin);
                                right = this.mSlideableView.getRight() - getWindowWidth();
                                i = this.mStartMargin;
                                f7 = right + i;
                            }
                        } else {
                            int i3 = this.mStartMargin;
                            int i4 = this.mSlideRange;
                            float f8 = (i3 + i4) / (i4 == 0 ? 1.0f : i4);
                            this.mVelocityTracker.computeCurrentVelocity(1000, 2.0f);
                            if (this.mVelocityTracker.getXVelocity() > 0.0f) {
                                f8 *= this.mVelocityTracker.getXVelocity();
                            }
                            float left2 = this.mSlideableView.getLeft();
                            if (f8 != 0.0f) {
                                f = f8;
                            }
                            f7 = Math.min(left2 + (f7 / f), this.mStartMargin + this.mSlideRange);
                            if (this.mResizeOff) {
                                this.mSlideableView.setLeft((int) Math.max(this.mStartMargin, f7));
                                this.mSlideableView.setRight((this.mSlideableView.getLeft()
                                        + getWindowWidth()) - this.mStartMargin);
                            }
                        }
                        onPanelDragged((int) f7);
                    }
                }
            }
        } else {
            this.mStartOffset = this.mSlideOffset;
            this.mIsNeedOpen = false;
            this.mIsNeedClose = false;
            float x3 = ev.getX();
            float y3 = ev.getY();
            this.mInitialMotionX = x3;
            this.mInitialMotionY = y3;
            this.mPrevMotionX = x3;
            this.mSmoothWidth = 0;
        }
        return true;
    }
    // kang

    private boolean closePane(int initialVelocity, boolean isAnim) {
        if (mIsAnimate) {
            return true;
        }
        if (mSlideableView == null || mIsLock) {
            return false;
        }

        if (!isAnim) {
            final int left = isLayoutRtlSupport() ? mSlideRange : mStartMargin;

            onPanelDragged(left);

            if (mResizeOff) {
                resizeSlideableView(0);
                if (isLayoutRtlSupport()) {
                    mSlideableView.setRight(getWindowWidth() - mStartMargin);
                    mSlideableView.setLeft((mSlideableView.getRight() - getWindowWidth())
                            + mStartMargin);
                } else {
                    mSlideableView.setLeft(left);
                }
            } else {
                resizeSlideableView(0);
            }

            mPreservedOpenState = false;
            return true;
        } else if (mFirstLayout || smoothSlideTo(0.f, initialVelocity)) {
            mPreservedOpenState = false;
            return true;
        }
        return false;
    }

    private boolean openPane(int initialVelocity, boolean isAnim) {
        if (mIsAnimate) {
            return true;
        }
        if (mSlideableView == null || mIsLock) {
            return false;
        }

        if (!isAnim) {
            final int left = mFixedPaneStartX
                    + (isLayoutRtlSupport() ? -mSlideRange : mSlideRange);

            onPanelDragged(left);

            if (mResizeOff) {
                resizeSlideableView(0);
                if (isLayoutRtlSupport()) {
                    mSlideableView.setRight((getWindowWidth() - mStartMargin) - mSlideRange);
                    mSlideableView.setLeft(mSlideableView.getRight() - (getWindowWidth() - mStartMargin));
                } else {
                    mSlideableView.setLeft(left);
                    mSlideableView.setRight((left + getWindowWidth()) - mStartMargin);
                }
            } else {
                resizeSlideableView(1);
            }

            mPreservedOpenState = true;
            return true;
        } else if (mFirstLayout || smoothSlideTo(1.f, initialVelocity)) {
            mPreservedOpenState = true;
            return true;
        }
        return false;
    }

    /**
     * @deprecated Renamed to {@link #openPane()} - this method is going away soon!
     */
    @Deprecated
    public void smoothSlideOpen() {
        openPane();
    }

    /**
     * Open the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    @Override
    public void open() {
        mLastValidVelocity = 0;
        openPane();
    }

    /**
     * Open the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now open/in the process of opening
     */
    public boolean openPane() {
        mIsNeedOpen = true;
        mIsNeedClose = false;
        return openPane(0, !shouldSkipScroll());
    }

    /**
     * @deprecated Renamed to {@link #closePane()} - this method is going away soon!
     */
    @Deprecated
    public void smoothSlideClosed() {
        closePane();
    }

    /**
     * Close the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    @Override
    public void close() {
        mLastValidVelocity = 0;
        closePane();
    }

    /**
     * Close the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now closed/in the process of closing
     */
    public boolean closePane() {
        mIsNeedOpen = false;
        mIsNeedClose = true;
        return closePane(0, !shouldSkipScroll());
    }

    /**
     * Check if the layout is completely open. It can be open either because the slider
     * itself is open revealing the left pane, or if all content fits without sliding.
     *
     * @return true if sliding panels are completely open
     */
    @Override
    public boolean isOpen() {
        return !mCanSlide || mSlideOffset == 1;
    }

    /**
     * @return true if content in this layout can be slid open and closed
     * @deprecated Renamed to {@link #isSlideable()} - this method is going away soon!
     */
    @Deprecated
    public boolean canSlide() {
        return mCanSlide;
    }

    /**
     * Check if the content in this layout cannot fully fit side by side and therefore
     * the content pane can be slid back and forth.
     *
     * @return true if content in this layout can be slid open and closed
     */
    public boolean isSlideable() {
        return mCanSlide;
    }

    // TODO rework this method
    // kang
    void onPanelDragged(int newLeft) {
        if (!mIsLock) {
            if (this.mSlideableView == null) {
                this.mSlideOffset = 0.0f;
                return;
            }
            boolean isLayoutRtlSupport = isLayoutRtlSupport();
            LayoutParams layoutParams = (LayoutParams) this.mSlideableView.getLayoutParams();
            int paddingRight = (isLayoutRtlSupport
                    ? getPaddingRight() : getPaddingLeft())
                    + (isLayoutRtlSupport ? ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin :
                    ((ViewGroup.MarginLayoutParams) layoutParams).leftMargin);
            int width = this.mSlideableView.getWidth();
            if (isLayoutRtlSupport && this.mResizeOff) {
                width = getWidth() - paddingRight;
            } else if (this.mIsNeedClose) {
                width = Math.max((getWidth() - this.mSlideRange) - paddingRight, this.mSmoothWidth);
            } else if (this.mIsNeedOpen) {
                int width2 = getWidth() - paddingRight;
                int i2 = this.mSmoothWidth;
                if (i2 == 0) {
                    i2 = getWidth() - paddingRight;
                }
                width = Math.min(width2, i2);
            }
            if (isLayoutRtlSupport) {
                newLeft = (getWidth() - newLeft) - width;
            }
            float f = newLeft - paddingRight;
            int i3 = this.mSlideRange;
            if (i3 == 0) {
                i3 = 1;
            }
            float f2 = f / i3;
            this.mSlideOffset = f2;
            float f3 = 1.0f;
            if (f2 <= 1.0f) {
                f3 = Math.max(f2, 0.0f);
            }
            this.mSlideOffset = f3;
            VelocityTracker velocityTracker = this.mVelocityTracker;
            if (!(velocityTracker == null || velocityTracker.getXVelocity() == 0.0f)) {
                this.mLastValidVelocity = (int) this.mVelocityTracker.getXVelocity();
            }
            updateSlidingState();
            if (this.mParallaxBy != 0) {
                parallaxOtherViews(this.mSlideOffset);
            }
            if (layoutParams.dimWhenOffset) {
                dimChildView(this.mSlideableView, this.mSlideOffset, this.mSliderFadeColor);
            }
            dispatchOnPanelSlide(this.mSlideableView);
        }
    }
    // kang

    @SuppressWarnings("deprecation")
    private void dimChildView(View v, float mag, int fadeColor) {
        if (!mIsNeedBlockDim) {
            final LayoutParams lp = (LayoutParams) v.getLayoutParams();

            if (mag > 0 && fadeColor != 0) {
                final int baseAlpha = (fadeColor & 0xff000000) >>> 24;
                int imag = (int) (baseAlpha * mag);
                int color = imag << 24 | (fadeColor & 0xffffff);
                if (lp.dimPaint == null) {
                    lp.dimPaint = new Paint();
                }
                lp.dimPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(
                        color, PorterDuff.Mode.SRC_OVER));
                if (v.getLayerType() != View.LAYER_TYPE_HARDWARE) {
                    v.setLayerType(View.LAYER_TYPE_HARDWARE, lp.dimPaint);
                }
                invalidateChildRegion(v);
            } else if (v.getLayerType() != View.LAYER_TYPE_NONE) {
                if (lp.dimPaint != null) {
                    lp.dimPaint.setColorFilter(null);
                }
                final DisableLayerRunnable dlr = new DisableLayerRunnable(v);
                mPostedRunnables.add(dlr);
                ViewCompat.postOnAnimation(this, dlr);
            }
        }
    }

    private void updateSlidingState() {
        if (mSlidingState != null && mSlideableView != null) {
            if (mSlideOffset == 0) {
                if (mSlidingState.getState() != SESL_STATE_CLOSE) {
                    mSlidingState.onStateChanged(SESL_STATE_CLOSE);
                    dispatchOnPanelClosed(mSlideableView);
                }
            } else if (mSlideOffset == 1) {
                if (mSlidingState.getState() != SESL_STATE_OPEN) {
                    mSlidingState.onStateChanged(SESL_STATE_OPEN);
                    dispatchOnPanelOpened(mSlideableView);
                }
            } else if (mSlidingState.getState() != SESL_STATE_IDLE) {
                mSlidingState.onStateChanged(SESL_STATE_IDLE);
            }
        }
    }

    public void seslSetBlockDimWhenOffset(boolean isDim) {
        mIsNeedBlockDim = isDim;
    }

    public void seslSetPendingAction(int pendingAction) {
        if (pendingAction == SESL_PENDING_NONE
                || pendingAction == SESL_PENDING_COLLAPSED
                || pendingAction == SESL_PENDING_EXPANDED
                || pendingAction == SESL_PENDING_EXPANDED_LOCK
                || pendingAction == SESL_PENDING_COLLAPSED_LOCK) {
            mSetCustomPendingAction = true;
            mPendingAction = pendingAction;
        } else {
            mSetCustomPendingAction = false;
            Log.e(TAG, "pendingAction value is wrong ==> Your pending action value is ["
                    + pendingAction + "] / Now set pendingAction value as default");
        }
    }

    public SeslSlidingState seslGetSlidingState() {
        return mSlidingState;
    }

    public void seslSetLock(boolean needLock) {
        mIsLock = needLock;
    }

    public boolean seslGetLock() {
        return mIsLock;
    }

    public void seslSetSlidingPaneDragArea(int dragWidth) {
        mSlidingPaneDragArea = dragWidth;
    }

    public int seslGetSlidingPaneDragArea() {
        return mSlidingPaneDragArea;
    }

    public void seslOpenPane(boolean isAnim) {
        mLastValidVelocity = 0;
        mIsNeedOpen = true;
        mIsNeedClose = false;
        openPane(0, isAnim);
    }

    public void seslClosePane(boolean isAnim) {
        mLastValidVelocity = 0;
        mIsNeedOpen = false;
        mIsNeedClose = true;
        closePane(0, isAnim);
    }

    public int seslGetSlideRange() {
        return mSlideRange;
    }

    public void seslSetRoundedCornerColor(int color) {
        mRoundedColor = color;
    }

    private void setDrawerPaneWidth() {
        if (mDrawerPanel == null) {
            Log.e(TAG, "mDrawerPanel is null");
            return;
        }

        ViewGroup.LayoutParams lp = mDrawerPanel.getLayoutParams();
        lp.width = (int) (getWindowWidth() * mDrawerWidthPercent);
        mDrawerPanel.setLayoutParams(lp);
    }

    private int getWindowWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mDrawRoundedCorner && mSlideableView != null) {
            mSlidingPaneRoundedCorner.setRoundedCornerColor(0, mRoundedColor);
            mSlidingPaneRoundedCorner.drawRoundedCorner(mSlideableView, canvas);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        boolean result;
        final int save = canvas.save();

        if (mCanSlide && !lp.slideable && mSlideableView != null) {
            // Clip against the slider; no sense drawing what will immediately be covered.
            canvas.getClipBounds(mTmpRect);
            if (isLayoutRtlSupport()) {
                mTmpRect.left = Math.max(mTmpRect.left, mSlideableView.getRight());
            } else {
                mTmpRect.right = Math.min(mTmpRect.right, mSlideableView.getLeft());
            }
            canvas.clipRect(mTmpRect);
        }

        result = super.drawChild(canvas, child, drawingTime);

        canvas.restoreToCount(save);

        return result;
    }

    private Method mGetDisplayList;
    private Field mRecreateDisplayList;
    private boolean mDisplayListReflectionLoaded;

    void invalidateChildRegion(View v) {
        if (Build.VERSION.SDK_INT >= 17) {
            ViewCompat.setLayerPaint(v, ((LayoutParams) v.getLayoutParams()).dimPaint);
            return;
        }

        if (Build.VERSION.SDK_INT >= 16) {
            // Private API hacks! Nasty! Bad!
            //
            // In Jellybean, some optimizations in the hardware UI renderer
            // prevent a changed Paint on a View using a hardware layer from having
            // the intended effect. This twiddles some internal bits on the view to force
            // it to recreate the display list.
            if (!mDisplayListReflectionLoaded) {
                try {
                    mGetDisplayList = View.class.getDeclaredMethod("getDisplayList",
                            (Class<?>[]) null);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "Couldn't fetch getDisplayList method; dimming won't work right.",
                            e);
                }
                try {
                    mRecreateDisplayList = View.class.getDeclaredField("mRecreateDisplayList");
                    mRecreateDisplayList.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "Couldn't fetch mRecreateDisplayList field; dimming will be slow.",
                            e);
                }
                mDisplayListReflectionLoaded = true;
            }
            if (mGetDisplayList == null || mRecreateDisplayList == null) {
                // Slow path. REALLY slow path. Let's hope we don't get here.
                v.invalidate();
                return;
            }

            try {
                mRecreateDisplayList.setBoolean(v, true);
                mGetDisplayList.invoke(v, (Object[]) null);
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing display list state", e);
            }
        }

        ViewCompat.postInvalidateOnAnimation(this, v.getLeft(), v.getTop(), v.getRight(),
                v.getBottom());
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        mIsAnimate = false;

        if (!mCanSlide) {
            // Nothing to do.
            return false;
        }

        final boolean isLayoutRtl = isLayoutRtlSupport();
        final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

        int x;
        if (isLayoutRtl) {
            int startBound = getPaddingRight() + lp.rightMargin;
            int childWidth = mSlideableView.getWidth();
            int width = 0;
            if (!mIsNeedClose) {
                if (mIsNeedOpen) {
                    width = getWidth();
                }
                x = (int) (getWidth() - (startBound + slideOffset * mSlideRange + childWidth));
            } else if (mResizeOff) {
                width = getWidth();
            } else {
                width = getWidth() - mSlideRange;
            }
            childWidth = width - startBound;
            x = (int) (getWidth() - (startBound + slideOffset * mSlideRange + childWidth));
        } else {
            int startBound = getPaddingLeft() + lp.leftMargin;
            x = (int) (startBound + slideOffset * mSlideRange);
        }

        if (mDragHelper.smoothSlideViewTo(mSlideableView, x, mSlideableView.getTop())) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            mIsAnimate = true;
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            if (!mCanSlide) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * @deprecated Renamed to {@link #setShadowDrawableLeft(Drawable d)} to support LTR (left to
     * right language) and {@link #setShadowDrawableRight(Drawable d)} to support RTL (right to left
     * language) during opening/closing.
     *
     * @param d drawable to use as a shadow
     */
    @Deprecated
    public void setShadowDrawable(Drawable d) {
        setShadowDrawableLeft(d);
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane
     * during opening/closing.
     *
     * @param d drawable to use as a shadow
     */
    public void setShadowDrawableLeft(@Nullable Drawable d) {
        mShadowDrawableLeft = d;
    }

    /**
     * Set a drawable to use as a shadow cast by the left pane onto the right pane
     * during opening/closing to support right to left language.
     *
     * @param d drawable to use as a shadow
     */
    public void setShadowDrawableRight(@Nullable Drawable d) {
        mShadowDrawableRight = d;
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane
     * during opening/closing.
     *
     * @param resId Resource ID of a drawable to use
     * @deprecated Renamed to {@link #setShadowResourceLeft(int)} to support LTR (left to
     * right language) and {@link #setShadowResourceRight(int)} to support RTL (right to left
     * language) during opening/closing.
     */
    @Deprecated
    public void setShadowResource(@DrawableRes int resId) {
        setShadowDrawableLeft(getResources().getDrawable(resId));
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane
     * during opening/closing.
     *
     * @param resId Resource ID of a drawable to use
     */
    public void setShadowResourceLeft(int resId) {
        setShadowDrawableLeft(ContextCompat.getDrawable(getContext(), resId));
    }

    /**
     * Set a drawable to use as a shadow cast by the left pane onto the right pane
     * during opening/closing to support right to left language.
     *
     * @param resId Resource ID of a drawable to use
     */
    public void setShadowResourceRight(int resId) {
        setShadowDrawableRight(ContextCompat.getDrawable(getContext(), resId));
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
        final boolean isLayoutRtl = isLayoutRtlSupport();
        Drawable shadowDrawable;
        if (isLayoutRtl) {
            shadowDrawable = mShadowDrawableRight;
        } else {
            shadowDrawable = mShadowDrawableLeft;
        }

        final View shadowView = getChildCount() > 1 ? getChildAt(1) : null;
        if (shadowView == null || shadowDrawable == null) {
            // No need to draw a shadow if we don't have one.
            return;
        }

        final int top = shadowView.getTop();
        final int bottom = shadowView.getBottom();

        final int shadowWidth = shadowDrawable.getIntrinsicWidth();
        final int left;
        final int right;
        if (isLayoutRtlSupport()) {
            left = shadowView.getRight();
            right = left + shadowWidth;
        } else {
            right = shadowView.getLeft();
            left = right - shadowWidth;
        }

        shadowDrawable.setBounds(left, top, right, bottom);
        shadowDrawable.draw(c);
    }

    private void parallaxOtherViews(float slideOffset) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final LayoutParams slideLp = (LayoutParams) mSlideableView.getLayoutParams();
        final boolean dimViews = slideLp.dimWhenOffset
                && (isLayoutRtl ? slideLp.rightMargin : slideLp.leftMargin) <= 0;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View v = getChildAt(i);
            if (v == mSlideableView) continue;

            final int oldOffset = (int) ((1 - mParallaxOffset) * mParallaxBy);
            mParallaxOffset = slideOffset;
            final int newOffset = (int) ((1 - slideOffset) * mParallaxBy);
            final int dx = oldOffset - newOffset;

            v.offsetLeftAndRight(isLayoutRtl ? -dx : dx);

            if (dimViews) {
                dimChildView(v, isLayoutRtl ? mParallaxOffset - 1
                        : 1 - mParallaxOffset, mCoveredFadeColor);
            }
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx Delta scrolled in pixels
     * @param x X coordinate of the active touch point
     * @param y Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }

        return checkV && v.canScrollHorizontally((isLayoutRtlSupport() ? dx : -dx));
    }

    boolean isDimmed(View child) {
        if (child == null) {
            return false;
        }
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return mCanSlide && lp.dimWhenOffset && mSlideOffset > 0;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.isOpen = isSlideable() ? isOpen() : mPreservedOpenState;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.isOpen) {
            openPane();
        } else {
            closePane();
        }
        mPreservedOpenState = ss.isOpen;
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        DragHelperCallback() {
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mIsUnableToDrag) {
                return false;
            }

            return ((LayoutParams) child.getLayoutParams()).slideable;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mIsAnimate = false;
                if (mSlideOffset == 0) {
                    updateObscuredViewsVisibility(mSlideableView);
                    dispatchOnPanelClosed(mSlideableView);
                    mPreservedOpenState = false;
                } else {
                    dispatchOnPanelOpened(mSlideableView);
                    mPreservedOpenState = true;
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            // Make all child views visible in preparation for sliding things around
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (mStartOffset != 0 || mLastValidVelocity <= 0 || mSlideOffset <= 0.2f) {
                if (mStartOffset == 1 && mLastValidVelocity < 0 && mSlideOffset < 0.8f && dx > 0) {
                    return;
                }
            } else if (dx < 0) {
                return;
            }
            onPanelDragged(left);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final LayoutParams lp = (LayoutParams) releasedChild.getLayoutParams();

            int left;
            if (isLayoutRtlSupport()) {
                int startToRight =  getPaddingRight() + lp.rightMargin;
                if (xvel < 0 || (xvel == 0 && mSlideOffset > 0.5f)) {
                    startToRight += mSlideRange;
                }
                int childWidth = mSlideableView.getWidth();
                left = getWidth() - startToRight - childWidth;
            } else {
                left = getPaddingLeft() + lp.leftMargin;
                if (xvel > 0 || (xvel == 0 && mSlideOffset > 0.5f)) {
                    left += mSlideRange;
                }
            }
            mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

            final int newLeft;
            if (isLayoutRtlSupport()) {
                int startBound = getWidth()
                        - (getPaddingRight() + lp.rightMargin + mSlideableView.getWidth());
                int endBound =  startBound - mSlideRange;
                newLeft = Math.max(Math.min(left, startBound), endBound);
            } else {
                int startBound = getPaddingLeft() + lp.leftMargin;
                int endBound = startBound + mSlideRange;
                newLeft = Math.min(Math.max(left, startBound), endBound);
            }
            return newLeft;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // Make sure we never move views vertically.
            // This could happen if the child has less height than its parent.
            return child.getTop();
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            mDragHelper.captureChildView(mSlideableView, pointerId);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[] {
            android.R.attr.layout_weight
        };

        /**
         * The weighted proportion of how much of the leftover space
         * this child should consume after measurement.
         */
        public float weight = 0;

        /**
         * True if this pane is the slideable pane in the layout.
         */
        boolean slideable;

        /**
         * True if this view should be drawn dimmed
         * when it's been offset from its default position.
         */
        boolean dimWhenOffset;

        Paint dimPaint;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@NonNull android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.weight = source.weight;
        }

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            this.weight = a.getFloat(0, 0);
            a.recycle();
        }

    }

    static class SavedState extends AbsSavedState {
        boolean isOpen;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            isOpen = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isOpen ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            final AccessibilityNodeInfoCompat superNode = AccessibilityNodeInfoCompat.obtain(info);
            super.onInitializeAccessibilityNodeInfo(host, superNode);
            copyNodeInfoNoChildren(info, superNode);
            superNode.recycle();

            info.setClassName(ACCESSIBILITY_CLASS_NAME);
            info.setSource(host);

            final ViewParent parent = ViewCompat.getParentForAccessibility(host);
            if (parent instanceof View) {
                info.setParent((View) parent);
            }

            // This is a best-approximation of addChildrenForAccessibility()
            // that accounts for filtering.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (!filter(child) && (child.getVisibility() == View.VISIBLE)) {
                    // Force importance to "yes" since we can't read the value.
                    ViewCompat.setImportantForAccessibility(
                            child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    info.addChild(child);
                }
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);

            event.setClassName(ACCESSIBILITY_CLASS_NAME);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                AccessibilityEvent event) {
            if (mSlideOffset != 0 || mStartMargin >= getResources().getDimensionPixelSize(
                    R.dimen.sesl_sliding_pane_contents_drag_width_default)) {
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            } else if (filterDrawerChild(child)) {
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
            if (!filter(child)) {
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
            return false;
        }

        private boolean filterDrawerChild(View child) {
            if (child == mDrawerPanel) {
                return true;
            }

            if (mDrawerPanel instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) mDrawerPanel;
                final int childCount = vg.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    if (child == vg.getChildAt(i)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean filter(View child) {
            return isDimmed(child);
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                AccessibilityNodeInfoCompat src) {
            final Rect rect = mTmpRect;

            src.getBoundsInScreen(rect);
            dest.setBoundsInScreen(rect);

            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());

            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());

            dest.addAction(src.getActions());

            dest.setMovementGranularities(src.getMovementGranularities());
        }
    }

    private class DisableLayerRunnable implements Runnable {
        final View mChildView;

        DisableLayerRunnable(View childView) {
            mChildView = childView;
        }

        @Override
        public void run() {
            if (mChildView.getParent() == SlidingPaneLayout.this) {
                mChildView.setLayerType(View.LAYER_TYPE_NONE, null);
                invalidateChildRegion(mChildView);
            }
            mPostedRunnables.remove(this);
        }
    }

    boolean isLayoutRtlSupport() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    private boolean shouldSkipScroll() {
        return Settings.System.getInt(getContext().getContentResolver(),
                "remove_animations", 0) == 1;
    }
}
