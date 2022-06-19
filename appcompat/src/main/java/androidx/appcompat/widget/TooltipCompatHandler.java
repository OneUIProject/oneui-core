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

import static android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewConfigurationCompat;
import androidx.reflect.hardware.input.SeslInputManagerReflector;
import androidx.reflect.provider.SeslSettingsReflector;
import androidx.reflect.view.SeslPointerIconReflector;
import androidx.reflect.view.SeslViewReflector;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung TooltipCompatHandler class.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class TooltipCompatHandler implements View.OnLongClickListener, View.OnHoverListener,
        View.OnAttachStateChangeListener {
    private static final String TAG = "TooltipCompatHandler";

    private static final long LONG_CLICK_HIDE_TIMEOUT_MS = 2500;
    private static final long HOVER_HIDE_TIMEOUT_MS = 15000;
    private static final long HOVER_HIDE_TIMEOUT_SHORT_MS = 3000;

    private final View mAnchor;
    private final CharSequence mTooltipText;
    private final int mHoverSlop;

    private final Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            show(false /* not from touch*/);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private int mAnchorX;
    private int mAnchorY;

    private static int sLayoutDirection;

    private static int sPosX;
    private static int sPosY;

    private TooltipPopup mPopup;
    private boolean mFromTouch;

    // The handler currently scheduled to show a tooltip, triggered by a hover
    // (there can be only one).
    private static TooltipCompatHandler sPendingHandler;

    // The handler currently showing a tooltip (there can be only one).
    private static TooltipCompatHandler sActiveHandler = null;

    private static boolean sIsCustomTooltipPosition = false;
    private static boolean sIsForceActionBarX = false;
    private static boolean sIsForceBelow = false;
    private static boolean sIsTooltipNull = false;
    private boolean mIsSPenPointChanged = false;
    private boolean mIsShowRunnablePostDelayed = false;
    private int mLastHoverEvent = -1;
    private boolean mInitialWindowFocus = false;
    private boolean mIsForceExitDelay = false;

    /**
     * Set the tooltip text for the view.
     *
     * @param view        view to set the tooltip on
     * @param tooltipText the tooltip text
     */
    public static void setTooltipText(View view, CharSequence tooltipText) {
        if (view == null) {
            Log.i(TAG, "view is null");
            return;
        }
        sIsForceActionBarX = false;
        // The code below is not attempting to update the tooltip text
        // for a pending or currently active tooltip, because it may lead
        // to updating the wrong tooltip in in some rare cases (e.g. when
        // action menu item views are recycled). Instead, the tooltip is
        // canceled/hidden. This might still be the wrong tooltip,
        // but hiding a wrong tooltip is less disruptive UX.
        if (sPendingHandler != null && sPendingHandler.mAnchor == view) {
            setPendingHandler(null);
        }
        if (TextUtils.isEmpty(tooltipText)) {
            if (sActiveHandler != null && sActiveHandler.mAnchor == view) {
                sActiveHandler.hide();
            }
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
            view.setOnHoverListener(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Context context = view.getContext();
                if (!view.isEnabled() && context != null)  {
                    SeslViewReflector.semSetPointerIcon(view,
                            MotionEvent.TOOL_TYPE_STYLUS,
                            PointerIcon.getSystemIcon(context,
                                    SeslPointerIconReflector.getField_SEM_TYPE_STYLUS_DEFAULT()));
                }
            }
        } else {
            new TooltipCompatHandler(view, tooltipText);
        }
    }

    private TooltipCompatHandler(View anchor, CharSequence tooltipText) {
        mAnchor = anchor;
        mTooltipText = tooltipText;
        mHoverSlop = ViewConfigurationCompat.getScaledHoverSlop(
                ViewConfiguration.get(mAnchor.getContext()));
        clearAnchorPos();

        mAnchor.setOnLongClickListener(this);
        mAnchor.setOnHoverListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        mAnchorX = v.getWidth() / 2;
        mAnchorY = v.getHeight() / 2;
        show(true /* from touch */);
        return true;
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
        if (mPopup != null && mFromTouch) {
            return false;
        }
        if (mAnchor == null) {
            Log.i(TAG, "TooltipCompat Anchor view is null");
            return false;
        }
        if (!event.isFromSource(InputDevice.SOURCE_STYLUS) || isSPenHoveringSettingsEnabled()) {
            AccessibilityManager manager = (AccessibilityManager)
                    mAnchor.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager.isEnabled() && manager.isTouchExplorationEnabled()) {
                return false;
            }
            final int action = event.getAction();
            mLastHoverEvent = action;
            switch (action) {
                case MotionEvent.ACTION_HOVER_MOVE:
                    if (mAnchor.isEnabled() && mPopup == null) {
                        mAnchorX = (int) event.getX();
                        mAnchorY = (int) event.getY();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            showPenPointEffect(event, true);
                        }
                        if (!mIsShowRunnablePostDelayed || mIsForceExitDelay) {
                            setPendingHandler(this);
                            mIsForceExitDelay = false;
                            mIsShowRunnablePostDelayed = true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_HOVER_ENTER:
                    mInitialWindowFocus = mAnchor.hasWindowFocus();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Context context = v.getContext();
                        if (mAnchor.isEnabled() && mPopup == null && context != null) {
                            SeslViewReflector.semSetPointerIcon(v,
                                    MotionEvent.TOOL_TYPE_STYLUS,
                                    PointerIcon.getSystemIcon(context,
                                            SeslPointerIconReflector.getField_SEM_TYPE_STYLUS_MORE()));
                        }
                   }
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    Log.i(TAG, "MotionEvent.ACTION_HOVER_EXIT : hide SeslTooltipPopup");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Context context = v.getContext();
                        if (mAnchor.isEnabled() && mPopup != null && context != null) {
                            SeslViewReflector.semSetPointerIcon(v,
                                    MotionEvent.TOOL_TYPE_STYLUS,
                                    PointerIcon.getSystemIcon(context,
                                            SeslPointerIconReflector.getField_SEM_TYPE_STYLUS_DEFAULT()));
                        }
                    } else {
                        showPenPointEffect(event, false);
                    }
                    if (mPopup != null && mPopup.isShowing() && Math.abs(event.getX() - mAnchorX) < 4.0f &&
                            Math.abs(event.getY() - mAnchorY) < 4.0f) {
                        mIsForceExitDelay = true;
                        mAnchor.removeCallbacks(mHideRunnable);
                        mAnchor.postDelayed(mHideRunnable, LONG_CLICK_HIDE_TIMEOUT_MS);
                    } else {
                        hide();
                    }
                    break;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Context context = v.getContext();
                if (mAnchor.isEnabled() && mPopup != null && context != null) {
                    SeslViewReflector.semSetPointerIcon(v,
                            MotionEvent.TOOL_TYPE_STYLUS,
                            PointerIcon.getSystemIcon(context,
                                    SeslPointerIconReflector.getField_SEM_TYPE_STYLUS_DEFAULT()));
                }
            }
        }

        return false;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        // no-op.
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        hide();
    }

    void show(boolean fromTouch) {
        if (!ViewCompat.isAttachedToWindow(mAnchor)) {
            return;
        }
        setPendingHandler(null);
        if (sActiveHandler != null) {
            sActiveHandler.hide();
        }
        sActiveHandler = this;

        mFromTouch = fromTouch;
        mPopup = new TooltipPopup(mAnchor.getContext());
        if (sIsCustomTooltipPosition) {
            sIsForceBelow = false;
            sIsForceActionBarX = false;
            if (sIsTooltipNull && !fromTouch) {
                return;
            }
            mPopup.showActionItemTooltip(sPosX, sPosY, sLayoutDirection, mTooltipText);
            sIsCustomTooltipPosition = false;
        } else {
            if (sIsTooltipNull) {
                return;
            }
            if (sIsForceBelow || sIsForceActionBarX) {
                mPopup.show(mAnchor, mAnchorX, mAnchorY, mFromTouch, mTooltipText, sIsForceBelow, sIsForceActionBarX);
                sIsForceBelow = false;
                sIsForceActionBarX = false;
            } else {
                mPopup.show(mAnchor, mAnchorX, mAnchorY, mFromTouch, mTooltipText);
            }
        }
        // Only listen for attach state change while the popup is being shown.
        mAnchor.addOnAttachStateChangeListener(this);

        final long timeout;
        if (mFromTouch) {
            timeout = LONG_CLICK_HIDE_TIMEOUT_MS;
        } else if ((ViewCompat.getWindowSystemUiVisibility(mAnchor)
                & SYSTEM_UI_FLAG_LOW_PROFILE) == SYSTEM_UI_FLAG_LOW_PROFILE) {
            timeout = HOVER_HIDE_TIMEOUT_SHORT_MS - ViewConfiguration.getLongPressTimeout();
        } else {
            timeout = HOVER_HIDE_TIMEOUT_MS - ViewConfiguration.getLongPressTimeout();
        }
        mAnchor.removeCallbacks(mHideRunnable);
        mAnchor.postDelayed(mHideRunnable, timeout);
        if (mLastHoverEvent == MotionEvent.ACTION_HOVER_MOVE && !mAnchor.hasWindowFocus() && mInitialWindowFocus != mAnchor.hasWindowFocus()) {
            hide();
        }
    }

    void hide() {
        if (sActiveHandler == this) {
            sActiveHandler = null;
            if (mPopup != null) {
                mPopup.hide();
                mPopup = null;
                clearAnchorPos();
                mAnchor.removeOnAttachStateChangeListener(this);
            } else {
                Log.e(TAG, "sActiveHandler.mPopup == null");
            }
        }
        mIsShowRunnablePostDelayed = false;
        if (sPendingHandler == this) {
            setPendingHandler(null);
        }
        mAnchor.removeCallbacks(mHideRunnable);
        sPosX = 0;
        sPosY = 0;
        sIsTooltipNull = false;
        sIsCustomTooltipPosition = false;
    }

    private static void setPendingHandler(TooltipCompatHandler handler) {
        if (sPendingHandler != null) {
            sPendingHandler.cancelPendingShow();
        }
        sPendingHandler = handler;
        if (sPendingHandler != null) {
            sPendingHandler.scheduleShow();
        }
    }

    private void scheduleShow() {
        mAnchor.postDelayed(mShowRunnable, ViewConfiguration.getLongPressTimeout());
    }

    private void cancelPendingShow() {
        mAnchor.removeCallbacks(mShowRunnable);
    }

    /**
     * Update the anchor position if it significantly (that is by at least mHoverSlope)
     * different from the previously stored position. Ignoring insignificant changes
     * filters out the jitter which is typical for such input sources as stylus.
     *
     * @return True if the position has been updated.
     */
    private boolean updateAnchorPos(MotionEvent event) {
        final int newAnchorX = (int) event.getX();
        final int newAnchorY = (int) event.getY();
        if (Math.abs(newAnchorX - mAnchorX) <= mHoverSlop
                && Math.abs(newAnchorY - mAnchorY) <= mHoverSlop) {
            return false;
        }
        mAnchorX = newAnchorX;
        mAnchorY = newAnchorY;
        return true;
    }

    /**
     *  Clear the anchor position to ensure that the next change is considered significant.
     */
    private void clearAnchorPos() {
        mAnchorX = Integer.MAX_VALUE;
        mAnchorY = Integer.MAX_VALUE;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void seslSetTooltipPosition(int x, int y, int layoutDirection) {
        sLayoutDirection = layoutDirection;
        sPosX = x;
        sPosY = y;
        sIsCustomTooltipPosition = true;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void seslSetTooltipNull(boolean tooltipNull) {
        sIsTooltipNull = tooltipNull;
    }

    public static void seslSetTooltipForceBelow(boolean isbelow) {
        sIsForceBelow = isbelow;
    }

    public static void seslSetTooltipForceActionBarPosX(boolean isForceX) {
        sIsForceActionBarX = isForceX;
    }

    private void showPenPointEffect(MotionEvent event, boolean show) {
        if (event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return;
        }
        if (show) {
            SeslInputManagerReflector.setPointerIconType(SeslPointerIconReflector.getField_SEM_TYPE_STYLUS_MORE());
            mIsSPenPointChanged = true;
        } else if (mIsSPenPointChanged) {
            SeslInputManagerReflector.setPointerIconType(SeslPointerIconReflector.getField_SEM_TYPE_STYLUS_DEFAULT());
            mIsSPenPointChanged = false;
        }
    }

    boolean isSPenHoveringSettingsEnabled() {
        return Settings.System.getInt(mAnchor.getContext().getContentResolver(),
                SeslSettingsReflector.SeslSystemReflector.getField_SEM_PEN_HOVERING(), 0) == 1;
    }
}
