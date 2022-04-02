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

package androidx.picker.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.math.MathUtils;
import androidx.picker.util.SeslAnimationListener;

import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslTimePicker extends FrameLayout {
    public static final int PICKER_AMPM = 2;
    public static final int PICKER_DIVIDER = 3;
    public static final int PICKER_HOUR = 0;
    public static final int PICKER_MINUTE = 1;
    private TimePickerDelegate mDelegate;

    public interface OnEditTextModeChangedListener {
        void onEditTextModeChanged(SeslTimePicker view, boolean isEditTextMode);
    }

    public interface OnTimeChangedListener {
        void onTimeChanged(SeslTimePicker view, int hourOfDay, int minute);
    }

    public SeslTimePicker(Context context) {
        this(context, null);
    }

    public SeslTimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.timePickerStyle);
    }

    public SeslTimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslTimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mDelegate = new SeslTimePickerSpinnerDelegate(this,
                context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOnEditTextModeChangedListener(
            OnEditTextModeChangedListener onEditTextModeChangedListener) {
        mDelegate.setOnEditTextModeChangedListener(onEditTextModeChangedListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.AT_MOST) {
            widthMeasureSpec = MeasureSpec
                    .makeMeasureSpec(mDelegate.getDefaultWidth(), MeasureSpec.EXACTLY);
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            heightMeasureSpec = MeasureSpec
                    .makeMeasureSpec(mDelegate.getDefaultHeight(), MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setEditTextMode(boolean editTextMode) {
        mDelegate.setEditTextMode(editTextMode);
    }

    public boolean isEditTextMode() {
        return mDelegate.isEditTextMode();
    }

    public void setHour(@IntRange(from = 0, to = 23) int hour) {
        mDelegate.setHour(MathUtils.constrain(hour, 0, 23));
    }

    public int getHour() {
        return mDelegate.getHour();
    }

    public void setMinute(@IntRange(from = 0, to = 59) int minute) {
        mDelegate.setMinute(MathUtils.constrain(minute, 0, 59));
    }

    public int getMinute() {
        return mDelegate.getMinute();
    }

    public void setIs24HourView(@NonNull Boolean is24HourView) {
        if (is24HourView == null) {
            return;
        }

        mDelegate.setIs24Hour(is24HourView);
    }

    public void showMarginLeft(@NonNull Boolean show) {
        mDelegate.showMarginLeft(show);
    }

    @RestrictTo(LIBRARY)
    public boolean is24HourView() {
        return mDelegate.is24Hour();
    }

    public void set5MinuteInterval() {
        set5MinuteInterval(true);
    }

    public void set5MinuteInterval(boolean interval) {
        mDelegate.set5MinuteInterval(interval);
    }

    public void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener) {
        mDelegate.setOnTimeChangedListener(onTimeChangedListener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mDelegate.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mDelegate.isEnabled();
    }

    @Override
    public int getBaseline() {
        return mDelegate.getBaseline();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return mDelegate.onSaveInstanceState(superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState ss = (BaseSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mDelegate.onRestoreInstanceState(ss);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return mDelegate.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        mDelegate.onPopulateAccessibilityEvent(event);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        mDelegate.onInitializeAccessibilityEvent(event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        mDelegate.onInitializeAccessibilityNodeInfo(info);
    }

    @RestrictTo(LIBRARY)
    public void setLocale(Locale locale) {
        mDelegate.setCurrentLocale(locale);
    }

    public void startAnimation(int delayTime, SeslAnimationListener listener) {
        mDelegate.startAnimation(delayTime, listener);
    }

    public EditText getEditText(int picker) {
        return mDelegate.getEditText(picker);
    }

    public SeslNumberPicker getNumberPicker(int picker) {
        return mDelegate.getNumberPicker(picker);
    }

    public void setNumberPickerTextSize(int picker, float size) {
        mDelegate.setNumberPickerTextSize(picker, size);
    }

    public void setNumberPickerTextTypeface(int picker, Typeface typeface) {
        mDelegate.setNumberPickerTextTypeface(picker, typeface);
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (mDelegate != null) {
            mDelegate.requestLayout();
        }
    }

    private interface TimePickerDelegate {
        boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);

        int getBaseline();

        int getDefaultHeight();

        int getDefaultWidth();

        EditText getEditText(int picker);

        int getHour();

        int getMinute();

        SeslNumberPicker getNumberPicker(int picker);

        boolean is24Hour();

        boolean isEditTextMode();

        boolean isEnabled();

        void onConfigurationChanged(Configuration newConfig);

        void onInitializeAccessibilityEvent(AccessibilityEvent event);

        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info);

        void onPopulateAccessibilityEvent(AccessibilityEvent event);

        void onRestoreInstanceState(Parcelable state);

        Parcelable onSaveInstanceState(Parcelable superState);

        void requestLayout();

        void set5MinuteInterval(boolean interval);

        void setCurrentLocale(Locale locale);

        void setEditTextMode(boolean editTextMode);

        void setEnabled(boolean enabled);

        void setHour(@IntRange(from = 0, to = 23) int hour);

        void setIs24Hour(boolean is24Hour);

        void setMinute(@IntRange(from = 0, to = 59) int minute);

        void setNumberPickerTextSize(int picker, float size);

        void setNumberPickerTextTypeface(int picker, Typeface typeface);

        void setOnEditTextModeChangedListener(OnEditTextModeChangedListener onEditTextModeChangedListener);

        void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener);

        void showMarginLeft(boolean show);

        void startAnimation(int delayTime, SeslAnimationListener listener);
    }

    @RestrictTo(LIBRARY)
    static abstract class AbsTimePickerDelegate implements TimePickerDelegate {
        protected Context mContext;
        Locale mCurrentLocale;
        SeslTimePicker mDelegator;
        OnEditTextModeChangedListener mOnEditTextModeChangedListener;
        OnTimeChangedListener mOnTimeChangedListener;

        AbsTimePickerDelegate(@NonNull SeslTimePicker delegator,
                                        @NonNull Context context) {
            mDelegator = delegator;
            mContext = context;
            setCurrentLocale(Locale.getDefault());
        }

        @Override
        public void setCurrentLocale(Locale locale) {
            if (!locale.equals(mCurrentLocale)) {
                mCurrentLocale = locale;
            }
        }
    }
}
