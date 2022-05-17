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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.view.ViewCompat;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ActionBarContextView extends AbsActionBarView {
    private static final String TAG = "ActionBarContextView";

    private static float MAX_FONT_SCALE = 1.2f;

    private CharSequence mTitle;
    private CharSequence mSubtitle;

    private View mClose;
    private View mCloseButton;
    private View mCustomView;
    private LinearLayout mTitleLayout;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private int mTitleStyleRes;
    private int mSubtitleStyleRes;
    private boolean mTitleOptional;
    private int mCloseItemLayout;

    private boolean mIsActionModeAccessibilityOn;
    private boolean mCheckActionModeOn;

    public ActionBarContextView(@NonNull Context context) {
        this(context, null);
    }

    public ActionBarContextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.actionModeStyle);
    }

    public ActionBarContextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.ActionMode, defStyle, 0);
        ViewCompat.setBackground(this, a.getDrawable(R.styleable.ActionMode_background));
        mTitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_titleTextStyle, 0);
        mSubtitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_subtitleTextStyle, 0);

        mContentHeight = a.getLayoutDimension(
                R.styleable.ActionMode_height, 0);

        mCloseItemLayout = a.getResourceId(
                R.styleable.ActionMode_closeItemLayout,
                R.layout.sesl_action_mode_close_item);

        a.recycle();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionMode,
                android.R.attr.actionModeStyle, 0);

        final int height = a.getDimensionPixelSize(R.styleable.ActionMode_height, -1);
        if (height >= 0) {
            setContentHeight(height);
        }

        setPadding(0,
                getResources().getDimensionPixelSize(R.dimen.sesl_action_bar_top_padding),
                0,
                0);

        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final int top = getResources().getDimensionPixelSize(R.dimen.sesl_action_bar_top_padding);
        setPadding(0, top, 0, 0);

        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionMode,
                android.R.attr.actionModeStyle, 0);
        final int height = a.getDimensionPixelSize(R.styleable.ActionMode_height, -1);
        a.recycle();

        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = height + top;
        setLayoutParams(lp);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.hideOverflowMenu();
            mActionMenuPresenter.hideSubMenus();
        }
    }

    @Override
    public void setContentHeight(int height) {
        mContentHeight = height;
    }

    public void setCustomView(View view) {
        if (mCustomView != null) {
            removeView(mCustomView);
        }
        mCustomView = view;
        if (view != null && mTitleLayout != null) {
            removeView(mTitleLayout);
            mTitleLayout = null;
        }
        if (view != null) {
            addView(view);
        }
        requestLayout();
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        initTitle();
    }

    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        initTitle();
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    private void initTitle() {
        if (mTitleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.sesl_action_bar_title_item, this);
            mTitleLayout = (LinearLayout) getChildAt(getChildCount() - 1);
            mTitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_title);
            mSubtitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_subtitle);
            if (mTitleStyleRes != 0) {
                mTitleView.setTextAppearance(getContext(), mTitleStyleRes);
            }
            if (mSubtitleStyleRes != 0) {
                mSubtitleView.setTextAppearance(getContext(), mSubtitleStyleRes);
            }
        }

        mTitleView.setText(mTitle);
        mSubtitleView.setText(mSubtitle);

        final boolean hasTitle = !TextUtils.isEmpty(mTitle);
        final boolean hasSubtitle = !TextUtils.isEmpty(mSubtitle);
        mSubtitleView.setVisibility(hasSubtitle ? VISIBLE : GONE);
        mTitleLayout.setVisibility(hasTitle || hasSubtitle ? VISIBLE : GONE);
        if (mTitleLayout.getParent() == null) {
            addView(mTitleLayout);
        }
    }

    public void initForMode(final ActionMode mode) {
        mCheckActionModeOn = true;

        if (mClose == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mClose = inflater.inflate(mCloseItemLayout, this, false);
            addView(mClose);
        } else if (mClose.getParent() == null) {
            addView(mClose);
        }

        mCloseButton = mClose.findViewById(R.id.action_mode_close_button);
        mCloseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mode.finish();
            }
        });

        final MenuBuilder menu = (MenuBuilder) mode.getMenu();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
        mActionMenuPresenter = new ActionMenuPresenter(getContext());
        mActionMenuPresenter.setReserveOverflow(true);

        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        menu.addMenuPresenter(mActionMenuPresenter, mPopupContext);
        mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
        ViewCompat.setBackground(mMenuView, null);
        addView(mMenuView, layoutParams);
    }

    public void closeMode() {
        if (mClose == null) {
            killMode();
            return;
        }
    }

    public void killMode() {
        removeAllViews();
        mCustomView = null;
        mMenuView = null;
        mActionMenuPresenter = null;
        if (mCloseButton != null) {
            mCloseButton.setOnClickListener(null);
        }
    }

    @Override
    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        // Used by custom views if they don't supply layout params. Everything else
        // added to an ActionBarContextView should have them already.
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_width=\"match_parent\" (or fill_parent)");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

        final int contentWidth = MeasureSpec.getSize(widthMeasureSpec);

        final int topPadding =
                getResources().getDimensionPixelSize(R.dimen.sesl_action_bar_top_padding);

        int maxHeight = mContentHeight > 0 ?
                mContentHeight + topPadding : MeasureSpec.getSize(heightMeasureSpec);

        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        int availableWidth = contentWidth - getPaddingLeft() - getPaddingRight();
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

        if (mClose != null && mClose.getVisibility() == View.VISIBLE) {
            availableWidth = measureChildView(mClose, availableWidth, childSpecHeight, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
            availableWidth -= lp.leftMargin + lp.rightMargin;
        }

        if (mMenuView != null && mMenuView.getParent() == this) {
            availableWidth = measureChildView(mMenuView, availableWidth,
                    childSpecHeight, 0);
        }

        if (mTitleLayout != null && mCustomView == null) {
            final Context context = getContext();
            if (mTitleView != null) {
                TypedArray a = context.obtainStyledAttributes(mTitleStyleRes,
                        R.styleable.TextAppearance);
                TypedValue value = a.peekValue(R.styleable.TextAppearance_android_textSize);
                a.recycle();

                final float textSize = TypedValue.complexToFloat(value.data);
                if (TextUtils.isEmpty(mSubtitle)) {
                    mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                            textSize
                                    * Math.min(context.getResources().getConfiguration().fontScale,
                                    MAX_FONT_SCALE));
                } else {
                    mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
                }
            }
            if (mClose == null || mClose.getVisibility() == View.GONE) {
                final int contentInsetStart =
                        (int) context.getResources().getDimension(R.dimen.sesl_toolbar_content_inset);
                boolean isRtl =
                        ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR;
                if (mTitleView != null && mTitleView.getVisibility() == View.VISIBLE) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTitleView.getLayoutParams();
                    if (isRtl) {
                        lp.leftMargin = contentInsetStart;
                    } else {
                        lp.rightMargin = contentInsetStart;
                    }
                    mTitleView.setLayoutParams(lp);
                }
                if (mSubtitleView != null && mSubtitleView.getVisibility() == View.VISIBLE) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mSubtitleView.getLayoutParams();
                    if (isRtl) {
                        lp.leftMargin = contentInsetStart;
                    } else {
                        lp.rightMargin = contentInsetStart;
                    }
                    mSubtitleView.setLayoutParams(lp);
                }
            }
            if (mTitleOptional) {
                final int titleWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                mTitleLayout.measure(titleWidthSpec, childSpecHeight);
                final int titleWidth = mTitleLayout.getMeasuredWidth();
                final boolean titleFits = titleWidth <= availableWidth;
                if (titleFits) {
                    availableWidth -= titleWidth;
                }
                mTitleLayout.setVisibility(titleFits ? VISIBLE : GONE);
            } else {
                availableWidth = measureChildView(mTitleLayout, availableWidth, childSpecHeight, 0);
            }
        }

        if (mCustomView != null) {
            ViewGroup.LayoutParams lp = mCustomView.getLayoutParams();
            final int customWidthMode = lp.width != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            final int customWidth = lp.width >= 0 ?
                    Math.min(lp.width, availableWidth) : availableWidth;
            final int customHeightMode = lp.height != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            final int customHeight = lp.height >= 0 ?
                    Math.min(lp.height, height) : height;
            mCustomView.measure(MeasureSpec.makeMeasureSpec(customWidth, customWidthMode),
                    MeasureSpec.makeMeasureSpec(customHeight, customHeightMode));
        }

        if (mContentHeight <= 0) {
            int measuredHeight = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View v = getChildAt(i);
                int paddedViewHeight = v.getMeasuredHeight() + verticalPadding;
                if (paddedViewHeight > measuredHeight) {
                    measuredHeight = paddedViewHeight;
                }
            }
            setMeasuredDimension(contentWidth, measuredHeight);
        } else {
            setMeasuredDimension(contentWidth, maxHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        int x = isLayoutRtl ? r - l - getPaddingRight() : getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();

        if (mClose != null && mClose.getVisibility() != GONE) {
            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
            final int startMargin = (isLayoutRtl ? lp.rightMargin : lp.leftMargin);
            final int endMargin = (isLayoutRtl ? lp.leftMargin : lp.rightMargin);
            x = next(x, startMargin, isLayoutRtl);
            x += positionChild(mClose, x, y, contentHeight, isLayoutRtl);
            x = next(x, endMargin, isLayoutRtl);
        }

        if (mTitleLayout != null && mCustomView == null && mTitleLayout.getVisibility() != GONE) {
            x += positionChild(mTitleLayout, x, y, contentHeight, isLayoutRtl);
        }

        if (mCustomView != null) {
            x += positionChild(mCustomView, x, y, contentHeight, isLayoutRtl);
        }

        x = isLayoutRtl ? getPaddingLeft() : r - l - getPaddingRight();

        if (mMenuView != null) {
            x += positionChild(mMenuView, x, y, contentHeight, !isLayoutRtl);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Action mode started
            Log.d(TAG, "onInitializeAccessibilityEvent Check ActionMode :"
                    + mCheckActionModeOn);
            if (mCheckActionModeOn) {
                mIsActionModeAccessibilityOn = true;
                mCheckActionModeOn = false;
            } else {
                mIsActionModeAccessibilityOn = false;
            }
            Log.d(TAG, "onInitializeAccessibilityEvent mIsActionModeAccessibilityOn :"
                    + mIsActionModeAccessibilityOn);
            event.setSource(this);
            event.setClassName(getClass().getName());
            event.setPackageName(getContext().getPackageName());
            event.setContentDescription(mTitle);
        } else {
            super.onInitializeAccessibilityEvent(event);
        }
    }

    public boolean getIsActionModeAccessibilityOn() {
        return mIsActionModeAccessibilityOn;
    }

    public void setTitleOptional(boolean titleOptional) {
        if (titleOptional != mTitleOptional) {
            requestLayout();
        }
        mTitleOptional = titleOptional;
    }

    public boolean isTitleOptional() {
        return mTitleOptional;
    }
}
