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

package androidx.indexscroll.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroupOverlay;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.animation.SeslAnimationUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.indexscroll.R;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslIndexScrollView extends FrameLayout {
    public static final int GRAVITY_INDEX_BAR_LEFT = 0;
    public static final int GRAVITY_INDEX_BAR_RIGHT = 1;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({GRAVITY_INDEX_BAR_LEFT, GRAVITY_INDEX_BAR_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GravityIndexBar {
    }

    private static final float OUT_OF_BOUNDARY = -9999.0f;

    private static final String GROUP_CHAR = "\ud83d\udc65︎";

    private Context mContext;

    private SeslAbsIndexer mIndexer;
    private final IndexerObserver mIndexerObserver = new IndexerObserver();

    IndexScroll mIndexScroll;
    private IndexScrollPreview mIndexScrollPreview;
    private IndexScrollTouchHelper mTouchHelper;
    private ViewGroupOverlay mViewGroupOverlay;

    private OnIndexBarEventListener mOnIndexBarEventListener = null;

    private int mIndexBarGravity = GRAVITY_INDEX_BAR_RIGHT;

    private Typeface mGroupIconFont;
    private Typeface mSECRobotoLightRegularFont;

    private String mCurrentIndex;

    private boolean mHasOverlayChild = false;
    private boolean mIsSimpleIndexScroll = false;
    private boolean mRegisteredDataSetObserver = false;

    boolean mNeedToHandleA11yEvent = false;
    int mA11yTargetIndex = -1;
    float mA11yDownPosY = -1f;

    private long mStartTouchDown = 0;
    private float mTouchY = OUT_OF_BOUNDARY;

    private final Runnable mPreviewDelayRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIndexScrollPreview != null) {
                mIndexScrollPreview.fadeOutAnimation();
            }
        }
    };

    public interface OnIndexBarEventListener {
        void onIndexChanged(int sectionIndex);

        void onPressed(float v);

        void onReleased(float v);
    }

    public SeslIndexScrollView(Context context) {
        super(context);
        mContext = context;
        mCurrentIndex = null;
        init();
    }

    public SeslIndexScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mIndexBarGravity = GRAVITY_INDEX_BAR_RIGHT;
        init();
    }

    private void init() {
        mViewGroupOverlay = getOverlay();

        if (mIndexScrollPreview == null) {
            mIndexScrollPreview = new IndexScrollPreview(mContext);
            mIndexScrollPreview.setLayout(0, 0, getWidth(), getHeight());
            mViewGroupOverlay.add(mIndexScrollPreview);
        }

        mTouchHelper = new IndexScrollTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);

        mHasOverlayChild = true;

        mIndexScroll = new IndexScroll(mContext, getHeight(), getWidth(), mIndexBarGravity);
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (isTalkBackIsRunning() && mIndexScroll.mRecyclerView != null) {
            mNeedToHandleA11yEvent = mTouchHelper.dispatchHoverEvent(event)
                                        || super.dispatchHoverEvent(event);
            if (!mNeedToHandleA11yEvent) {
                mA11yDownPosY = -1f;
                mA11yTargetIndex = -1;
            }
            return mNeedToHandleA11yEvent;
        } else  {
            return false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mTouchHelper.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean onFocusChanged,
                                  int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(onFocusChanged, direction, previouslyFocusedRect);
        mTouchHelper.onFocusChanged(onFocusChanged, direction, previouslyFocusedRect);
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mIndexScroll != null) {
            mIndexScroll.setDimensions(getWidth(), getHeight());
            if (mCurrentIndex != null
                    && mCurrentIndex.length() != 0 && mIndexScrollPreview != null) {
                mIndexScrollPreview.setLayout(0, 0, getWidth(), getHeight());
                mIndexScrollPreview.invalidate();
            }
            if (mIndexScroll != null
                    && mIndexScroll.isAlphabetInit()) {
                mIndexScroll.draw(canvas);
            }
        }
    }

    public void setIndexer(SeslArrayIndexer indexer) {
        if (indexer != null) {
            setAbsIndexer(indexer);
        } else {
            throw new IllegalArgumentException("SeslIndexView.setIndexer(indexer) " +
                    ": indexer=null.");
        }
    }

    public void setIndexer(SeslCursorIndexer indexer) {
        if (indexer != null) {
            if (indexer.isInitialized()) {
                setAbsIndexer(indexer);
            } else {
                throw new IllegalArgumentException("The indexer was not initialized " +
                        "before setIndexer api call. It is necessary to check if the items " +
                        "being applied to the indexer is normal.");
            }
        } else {
            throw new IllegalArgumentException("SeslIndexView.setIndexer(indexer) " +
                    ": indexer=null.");
        }
    }

    private void setAbsIndexer(SeslAbsIndexer indexer) {
        if (mIndexer != null && mRegisteredDataSetObserver) {
            mRegisteredDataSetObserver = false;
            mIndexer.unregisterDataSetObserver(mIndexerObserver);
        }

        mIsSimpleIndexScroll = false;

        mIndexer = indexer;
        mRegisteredDataSetObserver = true;
        mIndexer.registerDataSetObserver(mIndexerObserver);

        if (mIndexScroll.mScrollThumbBgDrawable != null) {
            mIndexScroll.mScrollThumbBgDrawable
                    .setColorFilter(mIndexScroll.mThumbColor, PorterDuff.Mode.MULTIPLY);
        }

        mIndexer.cacheIndexInfo();
        mIndexScroll.setAlphabetArray(mIndexer.getAlphabetArray());
    }

    public void setSimpleIndexScroll(String[] indexBarChar, int width) {
        if (indexBarChar != null) {
            mIsSimpleIndexScroll = true;

            setSimpleIndexWidth((int) mContext.getResources()
                    .getDimension(R.dimen.sesl_indexbar_simple_index_width));

            if (width != 0) {
                setSimpleIndexWidth(width);
            }

            if (mIndexScroll.mScrollThumbBgDrawable != null) {
                mIndexScroll.mScrollThumbBgDrawable
                        .setColorFilter(mIndexScroll.mThumbColor, PorterDuff.Mode.MULTIPLY);
            }

            mIndexScroll.setAlphabetArray(indexBarChar);
        } else {
            throw new IllegalArgumentException("SeslIndexView.setSimpleIndexScroll(indexBarChar) ");
        }
    }

    private void setSimpleIndexWidth(int width) {
        if (mIndexScroll != null) {
            mIndexScroll.setSimpleIndexScrollWidth(width);
        }
    }

    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (mIndexScroll.mRecyclerView != recyclerView
                && recyclerView != null) {
            if (mIndexScroll.mRecyclerView != null) {
                mIndexScroll.mRecyclerView
                        .removeOnScrollListener(mIndexScroll.mScrollListener);
            }

            mIndexScroll.mRecyclerView = recyclerView;
            mIndexScroll.mLayout = mIndexScroll.mRecyclerView.getLayoutManager();

            mIndexScroll.mRecyclerView.addOnScrollListener(mIndexScroll.mScrollListener);
            mIndexScroll.mCurItemPosition = RecyclerView.NO_POSITION;

            enableScrollThumb(true);

            mTouchHelper.updateId(recyclerView.getId());
        }
    }

    public void enableScrollThumb(boolean enabled) {
        if (mIndexScroll != null) {
            mIndexScroll.mEnableScrollThumb = enabled;
            if (!enabled) {
                mIndexScroll.changeThumbAlpha(0);
            }
        }
    }

    public void setIndexBarTextMode(boolean textMode) {
        if (mIndexScroll != null) {
            mIndexScroll.mEnableTextMode = textMode;

            if (textMode) {
                mIndexScroll.mBgDrawableDefault
                        = getResources().getDrawable(R.drawable.sesl_index_bar_textmode_bg,
                        mContext.getTheme());
                mIndexScroll.mBgRectWidth
                        = (int) getResources().getDimension(R.dimen.sesl_indexbar_textmode_width);
                mIndexScroll.mScrollThumbBgDrawable
                        = getResources().getDrawable(R.drawable.sesl_index_bar_textmode_thumb_shape,
                        mContext.getTheme());
            } else {
                mIndexScroll.mBgDrawableDefault
                        = getResources().getDrawable(R.drawable.sesl_index_bar_bg,
                        mContext.getTheme());
                mIndexScroll.mBgRectWidth
                        = (int) getResources().getDimension(R.dimen.sesl_indexbar_width);
                mIndexScroll.mScrollThumbBgDrawable
                        = getResources().getDrawable(R.drawable.sesl_index_bar_thumb_shape,
                        mContext.getTheme());
            }

            mIndexScroll.mScrollThumbBgDrawable
                    .setColorFilter(mIndexScroll.mThumbColor, PorterDuff.Mode.MULTIPLY);
            mIndexScroll.mBgDrawableDefault
                    .setColorFilter(mIndexScroll.mBgTintColor, PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mHasOverlayChild) {
            mViewGroupOverlay.remove(mIndexScrollPreview);
            mHasOverlayChild = false;
        }

        if (mIndexer != null && mRegisteredDataSetObserver) {
            mRegisteredDataSetObserver = false;
            mIndexer.unregisterDataSetObserver(mIndexerObserver);
        }

        if (mPreviewDelayRunnable != null) {
            removeCallbacks(mPreviewDelayRunnable);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mHasOverlayChild) {
            mViewGroupOverlay.add(mIndexScrollPreview);
            mHasOverlayChild = true;
        }

        if (mIndexer != null && !mRegisteredDataSetObserver) {
            mRegisteredDataSetObserver = true;
            mIndexer.registerDataSetObserver(mIndexerObserver);
        }
    }

    public void setIndexBarBackgroundDrawable(Drawable bgDrawable) {
        mIndexScroll.mBgDrawableDefault = bgDrawable;
    }

    public void setIndexBarBackgroundColor(int bgColor) {
        mIndexScroll.mBgDrawableDefault
                .setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
    }

    public void setIndexBarTextColor(int textColor) {
        mIndexScroll.mTextColorDimmed = textColor;
    }

    public void setIndexBarPressedTextColor(int pressedTextColor) {
        mIndexScroll.mScrollThumbBgDrawable
                .setColorFilter(pressedTextColor, PorterDuff.Mode.MULTIPLY);
        mIndexScroll.mThumbColor = pressedTextColor;
    }

    public void setEffectTextColor(int effectTextColor) {
        mIndexScrollPreview.setTextColor(effectTextColor);
    }

    public void setEffectBackgroundColor(int effectBackgroundColor) {
        mIndexScrollPreview
                .setBackgroundColor(mIndexScroll.getColorWithAlpha(effectBackgroundColor, 0.8f));
    }

    public void setIndexBarGravity(@GravityIndexBar int gravity) {
        mIndexBarGravity = gravity;
        mIndexScroll.setPosition(gravity);
    }

    private int getListViewPosition(String indexPath) {
        if (indexPath != null && mIndexer != null) {
            return mIndexer.getCachingValue(mIndexScroll.getSelectedIndex());
        } else {
            return -1;
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable drawable) {
        return (mIndexScroll != null && mIndexScroll.getIndexScrollThumb() == drawable)
                || super.verifyDrawable(drawable);
    }

    public void setIndexScrollMargin(int topMargin, int bottomMargin) {
        if (mIndexScroll != null) {
            mIndexScroll.setIndexScrollBgMargin(topMargin, bottomMargin);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        if (mNeedToHandleA11yEvent) {
            return handleA11yEvent(ev);
        } else {
            return handleMotionEvent(ev);
        }
    }

    private boolean handleA11yEvent(MotionEvent ev) {
        final int action = ev.getAction();

        if (mIndexer == null) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mA11yDownPosY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                final float posY = ev.getY();
                if (posY != mA11yDownPosY) {
                    if (mA11yTargetIndex == -1) {
                        mA11yTargetIndex
                                = mIndexer.getIndexByPosition(mIndexScroll.findFirstChildPosition());
                    }

                    if (mA11yDownPosY - posY > 0.0f
                            && mA11yTargetIndex != mIndexScroll.mAlphabetSize - 1) {
                        mA11yTargetIndex++;
                    } else if (mA11yDownPosY - posY < 0.0f
                            && mA11yTargetIndex != 0) {
                        mA11yTargetIndex--;
                    }

                    setContentDescription(mIndexScroll.mAlphabetArray[mA11yTargetIndex]
                            + ", " + getResources().getString(R.string.sesl_index_selected));

                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
                    notifyIndexChange(mIndexer.getCachingValue(mA11yTargetIndex));
                }
                break;
        }

        return true;
    }

    private boolean handleMotionEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final float y = ev.getY();
        final float x = ev.getX();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mCurrentIndex = mIndexScroll.getIndexByPosition((int) x, (int) y, true);
                mStartTouchDown = System.currentTimeMillis();

                if (mCurrentIndex == null) {
                    return false;
                }

                if (mIndexScroll.isAlphabetInit()
                        && mCurrentIndex != null && mCurrentIndex.length() != 0) {
                    mIndexScroll.setEffectText(mCurrentIndex);
                    mIndexScroll.drawEffect(y);
                    mIndexScrollPreview.setLayout(0, 0, getWidth(), getHeight());
                    mIndexScrollPreview.invalidate();
                    mTouchY = y;
                    mIndexScroll.changeThumbAlpha(255);
                }

                final int position;
                if (!mIsSimpleIndexScroll) {
                    position = getListViewPosition(mCurrentIndex);
                } else {
                    position = mIndexScroll.getSelectedIndex();
                }
                if (position != RecyclerView.NO_POSITION) {
                    notifyIndexChange(position);
                }
            }
            break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentIndex = null;
                        mIndexScroll.resetSelectedIndex();
                        mIndexScrollPreview.close();
                        mIndexScroll.changeThumbAlpha(0);
                        invalidate();
                        if (mOnIndexBarEventListener != null) {
                            mOnIndexBarEventListener.onReleased(y);
                        }
                    }
                }, 30);
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                if (mCurrentIndex == null || !mIndexScrollPreview.mIsOpen) {
                    return false;
                }

                final int position;
                final String calculatedIndexStr = mIndexScroll.getIndexByPosition((int) x, (int) y, false);

                if (mCurrentIndex != null && calculatedIndexStr == null
                        && !mIsSimpleIndexScroll) {
                    mCurrentIndex = mIndexScroll.getIndexByPosition((int) x, (int) y, false);

                    position = getListViewPosition(mCurrentIndex);
                    if (position != RecyclerView.NO_POSITION) {
                        notifyIndexChange(position);
                    }
                } else if (mCurrentIndex == null || calculatedIndexStr == null
                        || calculatedIndexStr.length() >= mCurrentIndex.length()) {
                    mCurrentIndex = mIndexScroll.getIndexByPosition((int) x, (int) y, false);
                    if (mIndexScroll.isAlphabetInit()
                            && mCurrentIndex != null && mCurrentIndex.length() != 0) {
                        mIndexScroll.setEffectText(mCurrentIndex);
                        mIndexScroll.drawEffect(y);
                        mTouchY = y;
                    }

                    if (!mIsSimpleIndexScroll) {
                        position = getListViewPosition(mCurrentIndex);
                    } else {
                        position = mIndexScroll.getSelectedIndex();
                    }
                    if (position != RecyclerView.NO_POSITION) {
                        notifyIndexChange(position);
                    }
                } else {
                    mCurrentIndex = mIndexScroll.getIndexByPosition((int) x, (int) y, false);

                    if (!mIsSimpleIndexScroll) {
                        position = getListViewPosition(mCurrentIndex);
                    } else {
                        position = mIndexScroll.getSelectedIndex();
                    }
                    if (position != RecyclerView.NO_POSITION) {
                        notifyIndexChange(position);
                    }
                }
            }
            break;

            default:
                return false;
        }

        invalidate();
        return true;
    }

    private boolean isTalkBackIsRunning() {
        AccessibilityManager accessibilityManager
                = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        String enabledServices = Settings.Secure.getString(getContext().getContentResolver(), "enabled_accessibility_services");

        if (accessibilityManager != null && accessibilityManager.isEnabled() && enabledServices != null) {
            return enabledServices.matches("(?i).*com.samsung.accessibility/com.samsung.android.app.talkback.TalkBackService.*")
                    || enabledServices.matches("(?i).*com.samsung.android.accessibility.talkback/com.samsung.android.marvin.talkback.TalkBackService.*")
                    || enabledServices.matches("(?i).*com.google.android.marvin.talkback.TalkBackService.*")
                    || enabledServices.matches("(?i).*com.samsung.accessibility/com.samsung.accessibility.universalswitch.UniversalSwitchService.*");
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyIndexChange(int pos) {
        if (mOnIndexBarEventListener != null) {
            mOnIndexBarEventListener.onIndexChanged(pos);
        }
    }

    public void setOnIndexBarEventListener(OnIndexBarEventListener iOnIndexBarEventListener) {
        mOnIndexBarEventListener = iOnIndexBarEventListener;
    }

    class IndexerObserver extends DataSetObserver {
        private final long INDEX_UPDATE_DELAY = 200;
        boolean mDataInvalid = false;

        @Override
        public void onChanged() {
            super.onChanged();
            notifyDataSetChange();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            notifyDataSetChange();
        }

        public boolean hasIndexerDataValid() {
            return !mDataInvalid;
        }

        private void notifyDataSetChange() {
            this.mDataInvalid = true;
            removeCallbacks(mUpdateIndex);
            postDelayed(mUpdateIndex, INDEX_UPDATE_DELAY);
        }

        Runnable mUpdateIndex = new Runnable() {
            @Override
            public void run() {
                mDataInvalid = false;
            }
        };
    }

    class IndexScroll {
        public static final int GRAVITY_INDEX_BAR_LEFT = 0;
        public static final int GRAVITY_INDEX_BAR_RIGHT = 1;
        public static final int NO_SELECTED_INDEX = -1;
        private Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

        private String[] mAlphabetArray = null;
        private Drawable mBgDrawableDefault = null;
        private Rect mBgRect;
        private String mBigText;
        private Context mContext;
        IndexBarAttributeValues mIndexBarTextAttrs;
        private RecyclerView.LayoutManager mLayout;
        private RecyclerView mRecyclerView;
        private Drawable mScrollThumbBgDrawable = null;
        private Rect mScrollThumbBgRect;
        private String mSmallText;
        private Rect mTextBounds = new Rect();
        private Paint mTextPaint;
        private ValueAnimator mThumbFadeAnimator;
        private ValueAnimator mThumbPosAnimator;

        private boolean mBgRectParamsSet = false;
        private boolean mEnableScrollThumb;
        boolean mEnableTextMode;
        private boolean mIsAlphabetInit = false;

        private float mContentMinHeight;
        private float mDotRadius;
        private float mIndexScrollPreviewRadius;
        private float mItemHeight;
        private float mPreviewLimitY;
        private float mSeparatorHeight;

        private int mAdditionalSpace;
        private int mAlphabetSize;
        private int mBgRectWidth;
        private int mBgTintColor;
        private int mContentPadding;
        private int mCurItemPosition = RecyclerView.NO_POSITION;
        private int mCurThumbAlpha = 255;
        private int mHeight;
        private int mItemWidth;
        private int mItemWidthGap;
        private int mPosition = 0;
        private int mScreenHeight;
        private int mScrollBottom;
        private int mScrollBottomMargin;
        private int mScrollThumbAdditionalHeight;
        private int mScrollThumbBgRectHeight;
        private int mScrollThumbBgRectHorizontalPadding;
        private int mScrollThumbBgRectVerticalPadding;
        private int mScrollTop = 0;
        private int mScrollTopMargin;
        private int mSelectedIndex = NO_SELECTED_INDEX;
        private int mTargetThumbAlpha = 255;
        private int mTextColorDimmed;
        private int mTextSize;
        private int mThumbColor = Color.TRANSPARENT;
        private int mWidth;
        private int mWidthShift = 0;

        private final Runnable mFadeOutRunnable = new Runnable() {
            @Override
            public void run() {
                playThumbFadeAnimator(0);
            }
        };

        private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (mEnableScrollThumb && newState == RecyclerView.SCROLL_STATE_IDLE
                        && mCurrentIndex == null) {
                    removeCallbacks(mFadeOutRunnable);
                    postDelayed(mFadeOutRunnable, 500);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mEnableScrollThumb) {
                    final int position = findFirstChildPosition();
                    if (mCurrentIndex == null && mCurItemPosition != position) {
                        if (mTargetThumbAlpha != 255 && dy != 0) {
                            playThumbFadeAnimator(255);
                        }

                        mCurItemPosition = position;

                        if (mIndexer != null) {
                            int index = mIndexer.getIndexByPosition(position);
                            if (position + mRecyclerView.getChildCount() == mRecyclerView.getAdapter().getItemCount()) {
                                index = mIndexer.getAlphabetArray().length - 1;
                            }

                            if (dy != 0) {
                                playThumbPosAnimator(mTouchY,
                                        (float) (mBgRect.top + mScrollThumbBgRectVerticalPadding)
                                                + (((float) index / (float) (mIndexer.getAlphabetArray().length - 1)) * (float) mBgRect.height()));
                            } else {
                                mTouchY = (float) (mBgRect.top + mScrollThumbBgRectVerticalPadding)
                                        + (((float) index / (float) (mIndexer.getAlphabetArray().length - 1)) * (float) mBgRect.height());
                            }
                        }
                    }
                }
            }
        };

        class IndexBarAttributeValues {
            String[] alphabetArray;
            int count = 0;
            float height = 0.0f;
            float separatorHeight = 0.0f;
        }

        public IndexScroll(Context context, int height, int width) {
            mHeight = height;
            mWidth = width;
            mContext = context;
            init();
        }

        public IndexScroll(Context context, int height, int width,
                           int position) {
            mHeight = height;
            mWidth = width;
            mPosition = position;
            mContext = context;
            init();
        }

        public boolean isAlphabetInit() {
            return mIsAlphabetInit;
        }

        public int getPosition() {
            return mPosition;
        }

        public int getSelectedIndex() {
            return mSelectedIndex;
        }

        public int getHeight() {
            return mHeight;
        }

        public void setSimpleIndexScrollWidth(int itemWidth) {
            if (itemWidth > 0) {
                mItemWidth = itemWidth;
                mBgRectWidth = itemWidth;
                allocateBgRectangle();
            }
        }

        public void setIndexScrollBgMargin(int topMargin, int bottomMargin) {
            mScrollTopMargin = topMargin;
            mScrollBottomMargin = bottomMargin;
            invalidate();
        }

        public void setPosition(int position) {
            mPosition = position;
            setBgRectParams();
        }

        public void setDimensions(int width, int height) {
            if (mIsAlphabetInit) {
                mWidth = width;
                mHeight = height
                        - (mScrollTop + mScrollBottom + mScrollTopMargin + mScrollBottomMargin);
                mScreenHeight = height;
                mItemHeight = mHeight / mAlphabetSize;
                mSeparatorHeight = Math.max(mItemHeight, mContentMinHeight);
                setBgRectParams();

                if (mIndexBarTextAttrs != null) {
                    mIndexBarTextAttrs.separatorHeight = mContentMinHeight;
                    manageIndexScrollHeight();
                }
            }
        }

        private void init() {
            final Resources rsrc = mContext.getResources();

            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);

            if (mSECRobotoLightRegularFont == null) {
                mSECRobotoLightRegularFont = Typeface.create(mContext
                        .getString(androidx.appcompat.R.string.sesl_font_family_regular),
                        Typeface.NORMAL);
            }
            if (mGroupIconFont == null) {
                mGroupIconFont = Typeface.createFromAsset(mContext.getAssets(),
                        "sesl_indexscroll_group_font.ttf");
            }
            mTextPaint.setTypeface(mSECRobotoLightRegularFont);

            mScrollTopMargin = 0;
            mScrollBottomMargin = 0;
            mItemWidth = 1;
            mItemWidthGap = 1;

            mBgRectWidth = (int) rsrc.getDimension(R.dimen.sesl_indexbar_width);
            mTextSize = (int) rsrc.getDimension(R.dimen.sesl_indexbar_text_size);
            mScrollTop = (int) rsrc.getDimension(R.dimen.sesl_indexbar_margin_top);
            mScrollBottom = (int) rsrc.getDimension(R.dimen.sesl_indexbar_margin_bottom);
            mWidthShift = (int) rsrc.getDimension(R.dimen.sesl_indexbar_margin_horizontal);
            mContentPadding = (int) rsrc.getDimension(R.dimen.sesl_indexbar_content_padding);
            mContentMinHeight = rsrc.getDimension(R.dimen.sesl_indexbar_content_min_height);
            mDotRadius = rsrc.getDimension(R.dimen.sesl_indexbar_dot_radius);
            mAdditionalSpace = (int) rsrc.getDimension(R.dimen.sesl_indexbar_additional_touch_boundary);
            mIndexScrollPreviewRadius = rsrc.getDimension(R.dimen.sesl_index_scroll_preview_radius);
            mPreviewLimitY = rsrc.getDimension(R.dimen.sesl_index_scroll_preview_ypos_limit);

            TypedValue outValue = new TypedValue();
            final Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.colorPrimary, outValue, true);
            final int colorPrimary = outValue.resourceId != 0
                    ? ResourcesCompat.getColor(rsrc, outValue.resourceId, null) : outValue.data;

            mIndexBarTextAttrs = new IndexBarAttributeValues();

            mScrollThumbBgRectVerticalPadding = (int) rsrc.getDimension(R.dimen.sesl_indexbar_thumb_vertical_padding);
            mScrollThumbBgRectHorizontalPadding = (int) rsrc.getDimension(R.dimen.sesl_indexbar_thumb_horizontal_padding);
            mScrollThumbAdditionalHeight = (int) rsrc.getDimension(R.dimen.sesl_indexbar_thumb_additional_height);

            mScrollThumbBgDrawable = rsrc.getDrawable(R.drawable.sesl_index_bar_thumb_shape,
                    mContext.getTheme());
            mScrollThumbBgDrawable.setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);

            mThumbColor = colorPrimary;

            mContext.getTheme().resolveAttribute(R.attr.isLightTheme, outValue, true);
            if (outValue.data != 0) {
                mTextColorDimmed = ResourcesCompat.getColor(rsrc, R.color.sesl_index_bar_text_color_light, theme);
                mBgTintColor = ResourcesCompat.getColor(rsrc, R.color.sesl_index_bar_background_tint_color_light, theme);
                mIndexScrollPreview.setBackgroundColor(getColorWithAlpha(colorPrimary, 0.8f));
            } else {
                mTextColorDimmed = ResourcesCompat.getColor(rsrc, R.color.sesl_index_bar_text_color_dark, theme);
                mBgTintColor = ResourcesCompat.getColor(rsrc, R.color.sesl_index_bar_background_tint_color_dark, theme);
                mIndexScrollPreview.setBackgroundColor(getColorWithAlpha(colorPrimary, 0.75f));
            }

            mBgDrawableDefault = rsrc.getDrawable(R.drawable.sesl_index_bar_bg, theme);
            mBgDrawableDefault.setColorFilter(mBgTintColor, PorterDuff.Mode.MULTIPLY);

            mEnableTextMode = false;
            mEnableScrollThumb = false;
            setBgRectParams();
        }

        private int getColorWithAlpha(int color, float ratio) {
            int newColor = 0;
            int alpha = Math.round(Color.alpha(color) * ratio);
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            newColor = Color.argb(alpha, r, g, b);
            return newColor;
        }

        public void setAlphabetArray(String[] alphabetArray) {
            if (alphabetArray != null) {
                mAlphabetArray = alphabetArray;
                mAlphabetSize = alphabetArray.length;
                mItemHeight = mHeight / mAlphabetSize;
                mSeparatorHeight = Math.max(mItemHeight, mContentMinHeight);
                mIsAlphabetInit = true;
            }
        }

        private void adjustSeparatorHeight() {
            mIndexBarTextAttrs.separatorHeight = mHeight / mIndexBarTextAttrs.count;
            if (mIndexBarTextAttrs.separatorHeight < mContentMinHeight) {
                mIndexBarTextAttrs.separatorHeight = mContentMinHeight;
            }
            mIndexBarTextAttrs.height = mHeight;
        }

        private void manageIndexScrollHeight() {
            if (mIsAlphabetInit) {
                mIndexBarTextAttrs.count = mAlphabetSize;
                mIndexBarTextAttrs.alphabetArray = new String[mIndexBarTextAttrs.count];
                mIndexBarTextAttrs.height = mIndexBarTextAttrs.count * mContentMinHeight;
                setIndexBarTextOptimized(mIndexBarTextAttrs);
            }
        }

        // TODO rework this method
        // kang
        private void setIndexBarTextOptimized(IndexBarAttributeValues var1) {
            this.adjustSeparatorHeight();
            int var2 = var1.count;
            byte var3 = 0;
            int var4 = var2;

            int var5;
            for(var5 = 0; (float)this.mHeight < var1.separatorHeight * (float)var4; ++var5) {
                --var4;
            }

            if (this.mEnableTextMode) {
                float var6 = (float)var2 / ((float)var5 + 1.0F);
                var2 = 0;

                for(var5 = var3; var5 < var4; ++var5) {
                    while(var5 != 0) {
                        int var7 = var2 + 1;
                        if (var5 + var2 != Math.round((float)var7 * var6)) {
                            break;
                        }

                        var2 = var7;
                    }

                    var1.alphabetArray[var5] = this.mAlphabetArray[var5 + var2];
                }
            }

            var1.count = var4;
            this.adjustSeparatorHeight();
        }
        // kang

        public String getIndexByPosition(int x, int y, boolean pressed) {
            if (mBgRect == null || !mIsAlphabetInit) {
                return "";
            }

            if (pressed) {
                if ((mPosition == 0 && x < mBgRect.left - mAdditionalSpace)
                        || (mPosition == 1 && x > mBgRect.right + mAdditionalSpace)) {
                    return "";
                }
            }

            if (pressed) {
                if (x < mBgRect.left - mAdditionalSpace || x > mBgRect.right + mAdditionalSpace) {
                    if (mPosition == 0
                            && x >= mWidthShift + mItemWidth + mItemWidthGap) {
                        return null;
                    }

                    if (mPosition == 1
                            && x <= (mWidth - mWidthShift) - (mItemWidth + mItemWidthGap)) {
                        return null;
                    }

                    if (!isInSelectedIndexRect(y)) {
                        return getIndexByY(y);
                    }

                    return (mAlphabetArray == null
                            || mSelectedIndex < 0 || mSelectedIndex >= mAlphabetSize)
                                ? "" : mAlphabetArray[mSelectedIndex];
                }
            } else {
                if (isInSelectedIndexRect(y)) {
                    return (mAlphabetArray == null
                            || mSelectedIndex < 0 || mSelectedIndex >= mAlphabetSize)
                                ? "" : getIndexByY(y);
                }
            }

            return getIndexByY(y);
        }

        private int getIndex(int y) {
            int index;

            if (y < mScrollTop + mScrollTopMargin + mIndexBarTextAttrs.height) {
                final float indexTouchBoundary = mIndexBarTextAttrs.height / mAlphabetSize;
                index = (int) ((y - mScrollTop - mScrollTopMargin)
                        / indexTouchBoundary);
            } else {
                index = mAlphabetSize - 1;
            }

            if (index < 0) {
                return 0;
            }

            return index >= mAlphabetSize
                    ? mAlphabetSize - 1 : index;
        }

        private String getIndexByY(int y) {
            if (y > mBgRect.top - mAdditionalSpace
                    && y < mBgRect.bottom + mAdditionalSpace) {
                if (y < mBgRect.top) {
                    mSelectedIndex = 0;
                } else if (y > mBgRect.bottom) {
                    mSelectedIndex = mAlphabetSize - 1;
                } else {
                    mSelectedIndex = getIndex(y);
                    if (mSelectedIndex == mAlphabetSize) {
                        mSelectedIndex = mSelectedIndex - 1;
                    }
                }

                if (mSelectedIndex == mAlphabetSize
                        || mSelectedIndex == mAlphabetSize + 1) {
                    mSelectedIndex = mAlphabetSize - 1;
                }

                if (mAlphabetArray != null
                        && mSelectedIndex > -1 && mSelectedIndex <= mAlphabetSize) {
                    return mAlphabetArray[mSelectedIndex];
                }
            }

            return "";
        }

        private boolean isInSelectedIndexRect(int y) {
            if (mSelectedIndex == -1 || mSelectedIndex >= this.mAlphabetSize) {
                return false;
            }

            return y >= ((int) (((float) (mScrollTop + mScrollTopMargin))
                    + (((float) mSelectedIndex) * mSeparatorHeight)))
                        && y <= ((int) (((float) (mScrollTop + mScrollTopMargin))
                           + (mSeparatorHeight * ((float) (mSelectedIndex + 1)))));
        }

        public void resetSelectedIndex() {
            mSelectedIndex = NO_SELECTED_INDEX;
        }

        public void draw(Canvas canvas) {
            if (mIsAlphabetInit) {
                drawScroll(canvas);
            }
        }

        public void drawScroll(Canvas canvas) {
            drawBgRectangle(canvas);
            drawAlphabetCharacters(canvas);

            if ((mSelectedIndex < 0 || mSelectedIndex >= mAlphabetSize)
                    && mIndexScrollPreview != null) {
                mIndexScrollPreview.close();
            }
        }

        public Drawable getIndexScrollThumb() {
            return mScrollThumbBgDrawable;
        }

        public void setEffectText(String effectText) {
            mBigText = effectText;
        }

        public void drawEffect(float effectPositionY) {
            if (mSelectedIndex != NO_SELECTED_INDEX) {
                mSmallText = mAlphabetArray[mSelectedIndex];
                mTextPaint.getTextBounds(mSmallText, 0, mSmallText.length(), mTextBounds);

                final float bottomDrawY;
                final float topDrawY;
                if (mScreenHeight
                        <= ((2.0f * mIndexScrollPreviewRadius)
                            + mPreviewLimitY + mScrollTopMargin + mScrollBottomMargin)) {
                    topDrawY = mScrollTop + mScrollTopMargin + (mIndexBarTextAttrs.separatorHeight * 0.5f);
                    bottomDrawY = mScrollTop + mScrollTopMargin - mScrollBottomMargin
                            + mIndexBarTextAttrs.height - (mIndexBarTextAttrs.separatorHeight * 0.5f);
                } else {
                    topDrawY = mScrollTopMargin + mPreviewLimitY + mIndexScrollPreviewRadius;
                    bottomDrawY = mScreenHeight - mScrollBottomMargin - mPreviewLimitY - mIndexScrollPreviewRadius;
                }

                if (effectPositionY <= topDrawY || effectPositionY >= bottomDrawY) {
                    effectPositionY = effectPositionY <= topDrawY
                            ? topDrawY : effectPositionY >= bottomDrawY ? bottomDrawY : OUT_OF_BOUNDARY;
                }
                if (effectPositionY != OUT_OF_BOUNDARY) {
                    mIndexScrollPreview.open(effectPositionY, mBigText);
                    if (mOnIndexBarEventListener != null) {
                        mOnIndexBarEventListener.onPressed(effectPositionY);
                    }
                }
            }
        }

        private void allocateBgRectangle() {
            int left;
            int right;
            if (mPosition == GRAVITY_INDEX_BAR_RIGHT) {
                right = mWidth - mWidthShift;
                left = right - mBgRectWidth;
            } else {
                left = mWidthShift;
                right = mBgRectWidth + left;
            }

            if (mBgRect == null) {
                final int top
                        = mScrollTop + mScrollTopMargin - mContentPadding;
                final int bottom
                        = mHeight + mScrollTop + mScrollTopMargin + mContentPadding;
                mBgRect = new Rect(left, top, right, bottom);
            } else {
                final int top
                        = mScrollTop + mScrollTopMargin - mContentPadding;
                final int bottom
                        = mHeight + mScrollTop + mScrollTopMargin + mContentPadding;
                mBgRect.set(left, top, right, bottom);
            }

            if (mEnableTextMode) {
                mScrollThumbBgRectHeight = ((int) (mContentMinHeight * 3.0f)) + mScrollThumbAdditionalHeight;
                left += mScrollThumbBgRectHorizontalPadding;
                right -= mScrollThumbBgRectHorizontalPadding;
            } else {
                mScrollThumbBgRectHeight = ((int) (mContentMinHeight * 2.0f)) + mScrollThumbAdditionalHeight;
            }

            int top = (int) (mTouchY - (mScrollThumbBgRectHeight / 2));
            int bottom = (int) (mTouchY + (mScrollThumbBgRectHeight / 2));
            if ((top < mBgRect.top + mScrollThumbBgRectVerticalPadding
                    && bottom > mBgRect.bottom - mScrollThumbBgRectVerticalPadding)
                        || mScrollThumbBgRectHeight >= (mBgRect.bottom - mBgRect.top)
                           - (mScrollThumbBgRectVerticalPadding * 2)) {
                top = mBgRect.top + mScrollThumbBgRectVerticalPadding;
                bottom = mBgRect.bottom - mScrollThumbBgRectVerticalPadding;
            } else if (top < mBgRect.top + mScrollThumbBgRectVerticalPadding) {
                top = mBgRect.top + mScrollThumbBgRectVerticalPadding;
                bottom = mScrollThumbBgRectHeight + top;
            } else if (bottom > mBgRect.bottom - mScrollThumbBgRectVerticalPadding) {
                bottom = mBgRect.bottom - mScrollThumbBgRectVerticalPadding;
                top = bottom - mScrollThumbBgRectHeight;
            }

            if (mScrollThumbBgRect == null) {
                mScrollThumbBgRect = new Rect(left, top, right, bottom);
            } else {
                mScrollThumbBgRect.set(left, top, right, bottom);
            }
        }

        private void drawBgRectangle(Canvas canvas) {
            if (!mBgRectParamsSet) {
                setBgRectParams();
                mBgRectParamsSet = true;
            }
            mBgDrawableDefault.draw(canvas);
            if (mTouchY != OUT_OF_BOUNDARY) {
                mScrollThumbBgDrawable.draw(canvas);
            }
        }

        private void setBgRectParams() {
            allocateBgRectangle();
            mBgDrawableDefault.setBounds(mBgRect);
            mScrollThumbBgDrawable.setBounds(mScrollThumbBgRect);
        }

        private void drawAlphabetCharacters(Canvas canvas) {
            mTextPaint.setColor(mTextColorDimmed);
            mTextPaint.setTextSize(mTextSize);
            if (mAlphabetArray != null && mIndexBarTextAttrs.count != 0) {
                for (int index = 0; index < mIndexBarTextAttrs.count; index++) {
                    if (mEnableTextMode) {
                        String text = mIndexBarTextAttrs.alphabetArray[index];
                        if (text.equals(SeslIndexScrollView.GROUP_CHAR)) {
                            Paint charPaint = new Paint();
                            charPaint.set(mTextPaint);
                            charPaint.setTypeface(mGroupIconFont);
                            charPaint.getTextBounds(text, 0, text.length(), mTextBounds);

                            final float width = charPaint.measureText(text);
                            final float textPosX = mBgRect.centerX() - (width * 0.5f);
                            final float textPosY = (mIndexBarTextAttrs.separatorHeight * index)
                                    + ((mIndexBarTextAttrs.separatorHeight * 0.5f)
                                    - (mTextBounds.top * 0.5f)) + mScrollTop + mScrollTopMargin;
                            canvas.drawText(text, textPosX, textPosY, charPaint);
                        } else {
                            mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);

                            final float width = mTextPaint.measureText(text);
                            final float textPosX = mBgRect.centerX() - (width * 0.5f);
                            final float textPosY = (mIndexBarTextAttrs.separatorHeight * index)
                                    + ((mIndexBarTextAttrs.separatorHeight * 0.5f)
                                    - (mTextBounds.top * 0.5f)) + mScrollTop + mScrollTopMargin;
                            canvas.drawText(text, textPosX, textPosY, mTextPaint);
                        }
                    } else {
                        final float circleX = mBgRect.centerX();
                        final float circleY = (mIndexBarTextAttrs.separatorHeight * index)
                                + (mIndexBarTextAttrs.separatorHeight * 0.5f)
                                + mScrollTop + mScrollTopMargin;
                        canvas.drawCircle(circleX, circleY, mDotRadius, mTextPaint);
                    }
                }
            }
        }

        private void changeThumbAlpha(int alpha) {
            mCurThumbAlpha = alpha;
            mTargetThumbAlpha = alpha;

            removeCallbacks(mFadeOutRunnable);
            if (mThumbFadeAnimator != null) {
                mThumbFadeAnimator.cancel();
            }
            mScrollThumbBgDrawable.setAlpha(alpha);
        }

        private void playThumbFadeAnimator(int targetAlpha) {
            if (targetAlpha != mCurThumbAlpha) {
                if (mThumbFadeAnimator != null) {
                    mThumbFadeAnimator.cancel();
                }
                mTargetThumbAlpha = targetAlpha;

                mThumbFadeAnimator = ValueAnimator.ofInt(mCurThumbAlpha, mTargetThumbAlpha);
                mThumbFadeAnimator.setDuration(150);
                mThumbFadeAnimator.setInterpolator(LINEAR_INTERPOLATOR);
                mThumbFadeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mCurThumbAlpha = (Integer) animation.getAnimatedValue();
                        mScrollThumbBgDrawable.setAlpha(mCurThumbAlpha);
                        invalidate();
                    }
                });
                mThumbFadeAnimator.start();
            }
        }

        private void playThumbPosAnimator(float startY, float endY) {
            if (mThumbPosAnimator != null) {
                mThumbPosAnimator.cancel();
            }

            mThumbPosAnimator = ValueAnimator.ofFloat(startY, endY);
            mThumbPosAnimator.setDuration(300);
            mThumbPosAnimator.setInterpolator(SeslAnimationUtils.SINE_OUT_70);
            mThumbPosAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    mTouchY = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mThumbPosAnimator.start();
        }

        private int findFirstChildPosition() {
            final int position;
            if (mLayout instanceof LinearLayoutManager) {
                LinearLayoutManager llm = (LinearLayoutManager) mLayout;
                position = llm.findFirstVisibleItemPosition();
            } else if (mLayout instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager) mLayout;
                position = sglm.findFirstVisibleItemPositions(null)
                        [mLayout.getLayoutDirection() == LAYOUT_DIRECTION_RTL ? sglm.getSpanCount() - 1 : 0];
            } else {
                position = 0;
            }

            if (position == RecyclerView.NO_POSITION) {
                return 0;
            }

            return position;
        }
    }

    class IndexScrollPreview extends View {
        private static final int FASTSCROLL_VIBRATE_INDEX = 26;

        private String mPreviewText;
        private Paint mShapePaint;
        private Rect mTextBounds;
        private Paint mTextPaint;

        private boolean mIsOpen = false;

        private float mPreviewCenterMargin;
        private float mPreviewCenterX;
        private float mPreviewCenterY;
        private float mPreviewRadius;

        private int mTextSize;
        private int mTextWidthLimit;
        private int mVibrateIndex;

        public IndexScrollPreview(Context context) {
            super(context);
            init(context);
        }

        private void init(Context context) {
            final Resources rsrc = context.getResources();

            mShapePaint = new Paint();
            mShapePaint.setStyle(Paint.Style.FILL);
            mShapePaint.setAntiAlias(true);

            mTextSize
                    = (int) rsrc.getDimension(R.dimen.sesl_index_scroll_preview_text_size);
            mTextWidthLimit
                    = (int) rsrc.getDimension(R.dimen.sesl_index_scroll_preview_text_width_limit);

            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTypeface(mSECRobotoLightRegularFont);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setTextSize(mTextSize);
            mTextPaint.setColor(
                    ResourcesCompat.getColor(rsrc, R.color.sesl_index_scroll_preview_text_color_light, null));

            mTextBounds = new Rect();

            mPreviewRadius = rsrc.getDimension(R.dimen.sesl_index_scroll_preview_radius);
            mPreviewCenterMargin = rsrc.getDimension(R.dimen.sesl_index_scroll_preview_margin_center);

            mIsOpen = false;
            mVibrateIndex = SeslHapticFeedbackConstantsReflector
                    .semGetVibrationIndex(FASTSCROLL_VIBRATE_INDEX);
        }

        public void setLayout(int l, int t, int r, int b) {
            layout(l, t, r, b);
            if (mIndexBarGravity == GRAVITY_INDEX_BAR_LEFT) {
                mPreviewCenterX = mPreviewCenterMargin;
            } else {
                mPreviewCenterX = r - mPreviewCenterMargin;
            }
        }

        @Override
        public void setBackgroundColor(int bgColor) {
            mShapePaint.setColor(bgColor);
        }

        public void setTextColor(int txtColor) {
            mTextPaint.setColor(txtColor);
        }

        public void open(float y, String text) {
            int textSize = mTextSize;
            mPreviewCenterY = y;

            if (!mIsOpen || !mPreviewText.equals(text)) {
                performHapticFeedback(mVibrateIndex);
            }

            mPreviewText = text;
            mTextPaint.setTextSize(textSize);

            while (mTextPaint.measureText(text) > mTextWidthLimit) {
                textSize--;
                mTextPaint.setTextSize(textSize);
            }

            if (!mIsOpen) {
                startAnimation();
                mIsOpen = true;
            }
        }

        public void close() {
            final long gap = System.currentTimeMillis() - mStartTouchDown;
            removeCallbacks(mPreviewDelayRunnable);
            if (gap <= 100) {
                postDelayed(mPreviewDelayRunnable, 100);
            } else {
                fadeOutAnimation();
            }
        }

        private void fadeOutAnimation() {
            if (mIsOpen) {
                startAnimation();
                mIsOpen = false;
            }
        }

        public void startAnimation() {
            ObjectAnimator anim = !mIsOpen
                    ? ObjectAnimator.ofFloat(mIndexScrollPreview, "alpha", 0.0f, 1.0f)
                    : ObjectAnimator.ofFloat(mIndexScrollPreview, "alpha", 1.0f, 0.0f);
            anim.setDuration(167);

            AnimatorSet set = new AnimatorSet();
            set.play(anim);
            set.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mIsOpen) {
                canvas.drawCircle(mPreviewCenterX, mPreviewCenterY, mPreviewRadius, mShapePaint);
                if (mPreviewText.equals(GROUP_CHAR)) {
                    Paint charPaint = new Paint();
                    charPaint.set(mTextPaint);
                    charPaint.setTypeface(mGroupIconFont);
                    charPaint.getTextBounds(mPreviewText, 0,
                            mPreviewText.length() - 1, mTextBounds);

                    final float textY = mPreviewCenterY - ((charPaint.descent() + charPaint.ascent()) / 2.0f);
                    canvas.drawText(mPreviewText, mPreviewCenterX, textY, charPaint);
                } else {
                    mTextPaint.getTextBounds(mPreviewText, 0,
                            mPreviewText.length() - 1, mTextBounds);

                    final float textY = mPreviewCenterY - ((mTextPaint.descent() + mTextPaint.ascent()) / 2.0f);
                    canvas.drawText(mPreviewText, mPreviewCenterX, textY, mTextPaint);
                }
            }
        }
    }

    private class IndexScrollTouchHelper extends ExploreByTouchHelper {
        private int mId = Integer.MIN_VALUE;

        public IndexScrollTouchHelper(View host) {
            super(host);
        }

        private void updateId(int id) {
            mId = id;
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            if (mIndexScroll.mRecyclerView != null
                    && mIndexScroll.mBgRect.contains((int) x, (int) y)) {
                return mId;
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            if (mIndexScroll.mRecyclerView != null && mId != Integer.MIN_VALUE) {
                virtualViewIds.add(mId);
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            if (mId == virtualViewId) {
                final Resources res = getResources();

                StringBuilder contentDescription
                        = new StringBuilder(res.getString(R.string.sesl_index_section));
                contentDescription.append(", ");
                contentDescription.append(res.getString(R.string.sesl_index_scrollbar));
                contentDescription.append(", ");
                contentDescription.append(res.getString(R.string.sesl_index_assistant_text));

                node.setContentDescription(contentDescription);
                node.setBoundsInParent(mIndexScroll.mBgRect);
                node.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, @Nullable Bundle arguments) {
            return false;
        }

        @Override
        public void seslNotifyPerformAction(int virtualViewId, int action, Bundle arguments) {
            if (mId == virtualViewId && mId != Integer.MIN_VALUE) {
                if (action == AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS) {
                    mNeedToHandleA11yEvent = true;
                } else if (action == AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
                    mNeedToHandleA11yEvent = false;
                }
            }
        }
    }
}
