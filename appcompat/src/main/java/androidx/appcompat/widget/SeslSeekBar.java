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
import android.os.Build;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RestrictTo;
import androidx.appcompat.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung SeekBar class.
 */
public class SeslSeekBar extends SeslAbsSeekBar {

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range min..max where min
         *                 and max were set by {@link SeslProgressBar#setMin(int)} and
         *                 {@link SeslProgressBar#setMax(int)}, respectively. (The default values for
         *                 min is 0 and max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(SeslSeekBar seekBar, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStartTrackingTouch(SeslSeekBar seekBar);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStopTrackingTouch(SeslSeekBar seekBar);
    }

    /**
     * Samsung SeekBar callback for hover events.
     */
    public interface OnSeekBarHoverListener {
        void onHoverChanged(SeslSeekBar seekBar, int hoverLevel, boolean fromUser);

        void onStartTrackingHover(SeslSeekBar seekBar, int hoverLevel);

        void onStopTrackingHover(SeslSeekBar seekBar);
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private OnSeekBarHoverListener mOnSeekBarHoverListener;

    private int mOldValue;

    public SeslSeekBar(Context context) {
        this(context, null);
    }

    public SeslSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);
    }

    public SeslSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser, int progress) {
        super.onProgressRefresh(scale, fromUser, progress);
        if (mIsSeamless) {
            progress = Math.round(progress / SeslAbsSeekBar.SCALE_FACTOR);
            if (mOldValue != progress) {
                mOldValue = progress;
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
                }
            }
        } else {
            if (mOnSeekBarChangeListener != null) {
                mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
            }
        }
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     *
     * @param l The seek bar notification listener
     *
     * @see SeslSeekBar.OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    @Override
    void onStartTrackingTouch() {
        super.onStartTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    @Override
    void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.SeekBar.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && canUserSetProgress()) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        }
    }

    public void setOnSeekBarHoverListener(OnSeekBarHoverListener listener) {
        mOnSeekBarHoverListener = listener;
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onStartTrackingHover(int hoverLevel, int posX, int posY) {
        if (mOnSeekBarHoverListener != null) {
            mOnSeekBarHoverListener.onStartTrackingHover(this, hoverLevel);
        }
        super.onStartTrackingHover(hoverLevel, posX, posY);
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onStopTrackingHover() {
        if (mOnSeekBarHoverListener != null) {
            mOnSeekBarHoverListener.onStopTrackingHover(this);
        }
        super.onStopTrackingHover();
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onHoverChanged(int hoverLevel, int posX, int posY) {
        if (mOnSeekBarHoverListener != null) {
            mOnSeekBarHoverListener.onHoverChanged(this, hoverLevel, true);
        }
        super.onHoverChanged(hoverLevel, posX, posY);
    }
}
