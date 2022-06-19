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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.ViewCompat;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * A popup window displaying a text message aligned to a specified view.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class TooltipPopup {
    private static final String TAG = "SESL_TooltipPopup";

    private final Context mContext;

    private final View mContentView;
    private final TextView mMessageView;

    private final WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams();
    private final Rect mTmpDisplayFrame = new Rect();
    private final int[] mTmpAnchorPos = new int[2];
    private final int[] mTmpAppPos = new int[2];

    private int mNavigationBarHeight = 0;

    TooltipPopup(@NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.popupTheme, typedValue, false);
        if (typedValue.data != 0) {
            mContext = new ContextThemeWrapper(context, typedValue.data);
        } else {
            mContext = context;
        }

        mContentView = LayoutInflater.from(mContext).inflate(R.layout.sesl_tooltip, null);
        mMessageView = (TextView) mContentView.findViewById(R.id.message);
        mContentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        hide();
                        return true;
                    case MotionEvent.ACTION_OUTSIDE:
                        hide();
                        return false;
                }

                return false;
            }
        });

        mLayoutParams.setTitle(getClass().getSimpleName());
        mLayoutParams.packageName = mContext.getPackageName();
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL;
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.format = PixelFormat.TRANSLUCENT;
        mLayoutParams.windowAnimations = R.style.Animation_AppCompat_Tooltip;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
    }

    void show(View anchorView, int anchorX, int anchorY, boolean fromTouch,
            CharSequence tooltipText) {
        if (isShowing()) {
            hide();
        }

        mMessageView.setText(tooltipText);

        computePosition(anchorView, anchorX, anchorY, fromTouch, mLayoutParams, false, false);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mContentView, mLayoutParams);
    }

    void show(View anchorView, int anchorX, int anchorY, boolean fromTouch,
              CharSequence tooltipText, boolean isForceBelow, boolean isForceActionBarX) {
        if (isShowing()) {
            hide();
        }

        mMessageView.setText(tooltipText);

        computePosition(anchorView, anchorX, anchorY, fromTouch, mLayoutParams, isForceBelow, isForceActionBarX);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mContentView, mLayoutParams);
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void showActionItemTooltip(int x, int y, int layoutDirection, CharSequence tooltipText) {
        if (isShowing()) {
            hide();
        }

        mMessageView.setText(tooltipText);

        mLayoutParams.x = x;
        mLayoutParams.y = y;

        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            mLayoutParams.gravity = Gravity.END;
        } else {
            mLayoutParams.gravity = Gravity.START;
        }

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mContentView, mLayoutParams);
    }

    void hide() {
        if (!isShowing()) {
            return;
        }

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.removeView(mContentView);
    }

    boolean isShowing() {
        return mContentView.getParent() != null;
    }

    private boolean checkNaviBarForLandscape() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point size = new Point();
        display.getRealSize(size);

        final int rotation = display.getRotation();
        final int navigationBarHeight = (int) mContext.getResources().getDimension(R.dimen.sesl_navigation_bar_height);

        if (rotation == Surface.ROTATION_90 && mTmpDisplayFrame.right + navigationBarHeight >= size.x) {
            setNavigationBarHeight(size.x - mTmpDisplayFrame.right);
            return true;
        } else if (rotation == Surface.ROTATION_270 && mTmpDisplayFrame.left <= navigationBarHeight) {
            setNavigationBarHeight(mTmpDisplayFrame.left);
            return true;
        }

        return false;
    }

    private void setNavigationBarHeight(int height) {
        mNavigationBarHeight = height;
    }

    private int getNavigationBarHeight() {
        return mNavigationBarHeight;
    }

    private void computePosition(View anchorView, int anchorX, int anchorY, boolean fromTouch,
            WindowManager.LayoutParams outParams, boolean isForceBelow, boolean isForceActionBarX) {
        outParams.token = anchorView.getApplicationWindowToken();

        final int offsetX = anchorView.getWidth() / 2;  // Center on the view horizontally.

        outParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;

        final View appView = getAppRootView(anchorView);
        if (appView == null) {
            Log.e(TAG, "Cannot find app view");
            return;
        }
        appView.getWindowVisibleDisplayFrame(mTmpDisplayFrame);
        if (mTmpDisplayFrame.left < 0 && mTmpDisplayFrame.top < 0) {
            // No meaningful display frame, the anchor view is probably in a subpanel
            // (such as a popup window). Use the screen frame as a reasonable approximation.
            final Resources res = mContext.getResources();
            final int statusBarHeight;
            int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId != 0) {
                statusBarHeight = res.getDimensionPixelSize(resourceId);
            } else {
                statusBarHeight = 0;
            }
            final DisplayMetrics metrics = res.getDisplayMetrics();
            mTmpDisplayFrame.set(0, statusBarHeight, metrics.widthPixels, metrics.heightPixels);
        }

        int[] displayFrame = new int[2];
        appView.getLocationOnScreen(displayFrame);
        Rect rect = new Rect(displayFrame[0], displayFrame[1], displayFrame[0] + appView.getWidth(), displayFrame[1] + appView.getHeight());
        mTmpDisplayFrame.left = rect.left;
        mTmpDisplayFrame.right = rect.right;

        appView.getLocationOnScreen(mTmpAppPos);

        anchorView.getLocationOnScreen(mTmpAnchorPos);
        Log.i(TAG, "computePosition - displayFrame left : " + mTmpDisplayFrame.left);
        Log.i(TAG, "computePosition - displayFrame right : " + mTmpDisplayFrame.right);
        Log.i(TAG, "computePosition - displayFrame top : " + mTmpDisplayFrame.top);
        Log.i(TAG, "computePosition - displayFrame bottom : " + mTmpDisplayFrame.bottom);
        Log.i(TAG, "computePosition - anchorView locationOnScreen x: " + mTmpAnchorPos[0]);
        Log.i(TAG, "computePosition - anchorView locationOnScreen y : " + mTmpAnchorPos[1]);
        Log.i(TAG, "computePosition - appView locationOnScreen x : " + mTmpAppPos[0]);
        Log.i(TAG, "computePosition - appView locationOnScreen y : " + mTmpAppPos[1]);
        mTmpAnchorPos[0] -= mTmpAppPos[0];
        mTmpAnchorPos[1] -= mTmpAppPos[1];
        // mTmpAnchorPos is now relative to the main app window.

        outParams.x = mTmpAnchorPos[0] + offsetX - mTmpDisplayFrame.width() / 2;

        final int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mContentView.measure(spec, spec);
        final int tooltipWidth = mContentView.getMeasuredWidth();
        final int tooltipHeight = mContentView.getMeasuredHeight();

        final int tooltipRightMargin = mContext.getResources().getDimensionPixelOffset(R.dimen.sesl_hover_tooltip_popup_right_margin);
        final int tooltipAreaMargin = mContext.getResources().getDimensionPixelOffset(R.dimen.sesl_hover_tooltip_popup_area_margin);

        final int yAbove = mTmpAnchorPos[1] - tooltipHeight;
        final int yBelow = mTmpAnchorPos[1] + anchorView.getHeight();
        if (fromTouch) {
            if (ViewCompat.getLayoutDirection(anchorView) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                outParams.x = (((mTmpAnchorPos[0] + anchorView.getWidth()) - (mTmpDisplayFrame.width() / 2)) - (tooltipWidth / 2)) - tooltipRightMargin;
                if (outParams.x < ((-mTmpDisplayFrame.width()) / 2) + (tooltipWidth / 2)) {
                    outParams.x = ((-mTmpDisplayFrame.width()) / 2) + (tooltipWidth / 2) + tooltipRightMargin;
                }
                outParams.x = adjustTooltipPosition(outParams.x, tooltipWidth, tooltipRightMargin);
            } else {
                outParams.x = ((mTmpAnchorPos[0] + offsetX) - (mTmpDisplayFrame.width() / 2)) + (tooltipWidth / 2) + tooltipRightMargin;
                outParams.x = adjustTooltipPosition(outParams.x, tooltipWidth, tooltipRightMargin);
            }
            if (yBelow + offsetX > mTmpDisplayFrame.height()) {
                outParams.y = yAbove;
            } else {
                outParams.y = yBelow;
            }
        } else {
            outParams.x = (mTmpAnchorPos[0] + offsetX) - (mTmpDisplayFrame.width() / 2);
            if (outParams.x < ((-mTmpDisplayFrame.width()) / 2) + (tooltipWidth / 2)) {
                outParams.x = ((-mTmpDisplayFrame.width()) / 2) + (tooltipWidth / 2) + tooltipAreaMargin;
            }
            outParams.x = adjustTooltipPosition(outParams.x, tooltipWidth, tooltipRightMargin);
            if (yAbove >= 0) {
                outParams.y = yAbove;
            } else {
                outParams.y = yBelow;
            }
        }
        if (isForceBelow) {
            outParams.y = mTmpAnchorPos[1] + anchorView.getHeight();
        }
        if (isForceActionBarX) {
            if (ViewCompat.getLayoutDirection(anchorView) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                outParams.x = (((mTmpAnchorPos[0] + anchorView.getWidth()) - (mTmpDisplayFrame.width() / 2)) - (tooltipWidth / 2)) - tooltipRightMargin;
                if (outParams.x < ((-mTmpDisplayFrame.width()) / 2) + (tooltipWidth / 2)) {
                    outParams.x = ((-mTmpDisplayFrame.width()) / 2) + (tooltipWidth / 2) + tooltipAreaMargin;
                }
                outParams.x = adjustTooltipPosition(outParams.x, tooltipWidth, tooltipRightMargin);
            } else {
                outParams.x = (((mTmpAnchorPos[0] + offsetX) - (mTmpDisplayFrame.width() / 2)) + (tooltipWidth / 2)) - tooltipRightMargin;
                outParams.x = adjustTooltipPosition(outParams.x, tooltipWidth, tooltipRightMargin);
            }
            if (tooltipHeight + yBelow <= mTmpDisplayFrame.height()) {
                outParams.y = yBelow;
            } else {
                outParams.y = yAbove;
            }
        }
    }

    private int adjustTooltipPosition(int posX, int tooltipWidth, int tooltipHorizontalPadding) {
        int fixedX;

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        final int rotation = display.getRotation();

        if (checkNaviBarForLandscape()) {
            if (rotation == Surface.ROTATION_90) {
                fixedX = (((mTmpDisplayFrame.width() - tooltipWidth) - getNavigationBarHeight()) / 2) - tooltipHorizontalPadding;
                if (posX <= fixedX) {
                    return posX;
                }
            } else if (rotation == Surface.ROTATION_270) {
                if (posX <= 0) {
                    fixedX = ((tooltipWidth - mTmpDisplayFrame.width()) / 2) + tooltipHorizontalPadding;
                    if (posX > fixedX) {
                        return posX;
                    }
                    return fixedX + tooltipHorizontalPadding;
                } else {
                    fixedX = ((mTmpDisplayFrame.width() - tooltipWidth) / 2) + tooltipHorizontalPadding;
                    if (posX <= fixedX) {
                        return posX;
                    }
                }
            } else {
                return posX;
            }
        } else if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            if (posX <= 0) {
                fixedX = ((tooltipWidth - mTmpDisplayFrame.width()) / 2) + tooltipHorizontalPadding;
                if (posX >= fixedX) {
                    return posX;
                }
                return fixedX + tooltipHorizontalPadding;
            } else {
                fixedX = ((mTmpDisplayFrame.width() - tooltipWidth) / 2) + tooltipHorizontalPadding;
                if (posX <= fixedX) {
                    return posX;
                }
            }
        } else {
            return posX;
        }

        return fixedX - tooltipHorizontalPadding;
    }

    private static View getAppRootView(View anchorView) {
        View rootView = anchorView.getRootView();
        ViewGroup.LayoutParams lp = rootView.getLayoutParams();
        if (lp instanceof WindowManager.LayoutParams
                && (((WindowManager.LayoutParams) lp).type
                    == WindowManager.LayoutParams.TYPE_APPLICATION)) {
            // This covers regular app windows and Dialog windows.
            return rootView;
        }
        // For non-application window types (such as popup windows) try to find the main app window
        // through the context.
        Context context = anchorView.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return ((Activity) context).getWindow().getDecorView();
            } else {
                context = ((ContextWrapper) context).getBaseContext();
            }
        }
        // Main app window not found, fall back to the anchor's root view. There is no guarantee
        // that the tooltip position will be computed correctly.
        return rootView;
    }
}
