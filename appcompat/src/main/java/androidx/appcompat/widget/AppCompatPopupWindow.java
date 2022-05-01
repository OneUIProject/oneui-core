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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.appcompat.R;
import androidx.appcompat.view.ActionBarPolicy;
import androidx.core.widget.PopupWindowCompat;
import androidx.reflect.view.SeslViewReflector;
import androidx.reflect.widget.SeslPopupWindowReflector;

import java.lang.reflect.Field;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Compat PopupWindow class.
 */
class AppCompatPopupWindow extends PopupWindow {

    private static final boolean COMPAT_OVERLAP_ANCHOR = Build.VERSION.SDK_INT < 21;

    private static final int[] ONEUI_BLUR_POPUP_BACKGROUND_RES = new int[] {
            R.drawable.sesl_menu_popup_background,
            R.drawable.sesl_menu_popup_background_dark
    };

    private Context mContext;

    private boolean mHasNavigationBar;
    private int mNavigationBarHeight;
    private boolean mIsReplacedPoupBackground;
    private boolean mOverlapAnchor;

    private final Rect mTempRect = new Rect();

    public AppCompatPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    public AppCompatPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.PopupWindow, defStyleAttr, defStyleRes);
        if (a.hasValue(R.styleable.PopupWindow_overlapAnchor)) {
            setSupportOverlapAnchor(a.getBoolean(R.styleable.PopupWindow_overlapAnchor, false));
        }

        mContext = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Transition enterTransition = getTransition(a.getResourceId(R.styleable.PopupWindow_popupEnterTransition, 0));
            final Transition exitTransition = getTransition(a.getResourceId(R.styleable.PopupWindow_popupExitTransition, 0));
            setEnterTransition(enterTransition);
            setExitTransition(exitTransition);
        }

        final int popupBackgroundResId = a.getResourceId(R.styleable.PopupWindow_android_popupBackground, -1);

        boolean isOneUIBlurBackground = false;
        for (int popupBgResIds : ONEUI_BLUR_POPUP_BACKGROUND_RES) {
            if (popupBgResIds == popupBackgroundResId) {
                isOneUIBlurBackground = true;
            }
        }

        // We re-set this for tinting purposes
        setBackgroundDrawable(a.getDrawable(R.styleable.PopupWindow_android_popupBackground));

        mIsReplacedPoupBackground = !isOneUIBlurBackground;

        a.recycle();

        mHasNavigationBar = ActionBarPolicy.get(context).hasNavigationBar();
        mNavigationBarHeight = mContext.getResources().getDimensionPixelSize(R.dimen.sesl_navigation_bar_height);
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        mIsReplacedPoupBackground = true;
        super.setBackgroundDrawable(background);
    }

    private Transition getTransition(int resId) {
        if (resId == 0 || resId == android.R.transition.no_transition) {
            return null;
        }

        TransitionInflater inflater = TransitionInflater.from(mContext);
        Transition transition = inflater.inflateTransition(resId);
        if (transition == null) {
            return null;
        }

        boolean isEmpty = (transition instanceof TransitionSet) && ((TransitionSet) transition).getTransitionCount() == 0;
        if (!isEmpty) {
            return transition;
        } else {
            return null;
        }
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        if (COMPAT_OVERLAP_ANCHOR && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.showAsDropDown(anchor, xoff, yoff);
        fixRoundedCorners();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        if (COMPAT_OVERLAP_ANCHOR && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.showAsDropDown(anchor, xoff, yoff, gravity);
        fixRoundedCorners();
    }

    @Override
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        if (COMPAT_OVERLAP_ANCHOR && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.update(anchor, xoff, yoff, width, height);
    }

    private void setSupportOverlapAnchor(boolean overlapAnchor) {
        if (COMPAT_OVERLAP_ANCHOR) {
            mOverlapAnchor = overlapAnchor;
        } else {
            PopupWindowCompat.setOverlapAnchor(this, overlapAnchor);
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean getSupportOverlapAnchor() {
        return PopupWindowCompat.getOverlapAnchor(this);
    }

    @Override
    public int getMaxAvailableHeight(@NonNull View anchor, int yOffset, boolean ignoreBottomDecorations) {
        final Rect displayFrame = new Rect();
        if (ignoreBottomDecorations) {
            SeslViewReflector.getWindowDisplayFrame(anchor, displayFrame);
            if (mHasNavigationBar
                    && mContext.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                displayFrame.bottom -= mNavigationBarHeight;
            }
        } else {
            anchor.getWindowVisibleDisplayFrame(displayFrame);
        }

        final int[] anchorPos = new int[2];
        anchor.getLocationOnScreen(anchorPos);

        int bottomEdge = displayFrame.bottom;
        final int distanceToBottom;
        final int distanceToTop = (anchorPos[1] - displayFrame.top) + yOffset;
        if (getSupportOverlapAnchor()) {
            distanceToBottom = bottomEdge - anchorPos[1] - yOffset;
        } else {
            distanceToBottom = bottomEdge - (anchorPos[1] + anchor.getHeight()) - yOffset;
        }

        // anchorPos[1] is distance from anchor to top of screen
        int returnedHeight = Math.max(distanceToBottom, distanceToTop);
        if (getBackground() != null) {
            getBackground().getPadding(mTempRect);
            return returnedHeight - (mTempRect.top + mTempRect.bottom);
        }

        return returnedHeight;
    }

    public void seslSetAllowScrollingAnchorParent(boolean enabled) {
        SeslPopupWindowReflector.setAllowScrollingAnchorParent(this, enabled);
    }

    boolean seslIsAvailableBlurBackground() {
        return !mIsReplacedPoupBackground;
    }

    /**
     * Adjust popup selector corners in non-Samsung Basic Interaction devices.
     */
    private void fixRoundedCorners() {
        // Check if we're using the correct bg
        if (!mIsReplacedPoupBackground) {
            try {
                // get PopupWindow mBackgroundView field
                Field field = getClass().getSuperclass().getDeclaredField("mBackgroundView");
                field.setAccessible(true);
                Object bg = field.get(this);
                // if we got it, fix those corners
                if (bg instanceof View) {
                    ((View) bg).setClipToOutline(true);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.e("ACPW", "fixRoundedCorners: " + e);
            }
        }
    }
}
