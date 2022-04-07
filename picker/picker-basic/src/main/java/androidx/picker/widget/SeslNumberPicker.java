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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.picker.util.SeslAnimationListener;
import androidx.reflect.view.SeslViewReflector;
import androidx.reflect.view.accessibility.SeslAccessibilityManagerReflector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslNumberPicker extends LinearLayout {
    static final int MODE_UNIT_DAY = 997;
    static final int MODE_UNIT_MONTH = 998;
    static final int MODE_UNIT_NONE = -1;
    static final int MODE_UNIT_YEAR = 999;
    private static final TwoDigitFormatter sTwoDigitFormatter = new TwoDigitFormatter();
    private NumberPickerDelegate mDelegate;

    private static class TwoDigitFormatter implements SeslNumberPicker.Formatter {
        final StringBuilder mBuilder = new StringBuilder();

        char mZeroDigit;
        java.util.Formatter mFmt;

        final Object[] mArgs = new Object[1];

        TwoDigitFormatter() {
            final Locale locale = Locale.getDefault();
            init(locale);
        }

        private void init(Locale locale) {
            mFmt = createFormatter(locale);
            mZeroDigit = getZeroDigit(locale);
        }

        public String format(int value) {
            final Locale currentLocale = Locale.getDefault();
            if (mZeroDigit != getZeroDigit(currentLocale)) {
                init(currentLocale);
            }
            mArgs[0] = value;
            synchronized (mBuilder) {
                mBuilder.delete(0, mBuilder.length());
                mFmt.format("%02d", mArgs);
            }
            return mFmt.toString();
        }

        private static char getZeroDigit(Locale locale) {
            return DecimalFormatSymbols.getInstance(locale).getZeroDigit();
        }

        private java.util.Formatter createFormatter(Locale locale) {
            return new java.util.Formatter(mBuilder, locale);
        }
    }

    public interface OnEditTextModeChangedListener {
        void onEditTextModeChanged(SeslNumberPicker numberPicker, boolean isEditTextMode);
    }

    @RestrictTo(LIBRARY)
    interface OnScrollListener {
        public static final int SCROLL_STATE_IDLE = 0;
        public static final int SCROLL_STATE_TOUCH_SCROLL = 1;
        public static final int SCROLL_STATE_FLING = 2;

        @RestrictTo(LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({SCROLL_STATE_IDLE, SCROLL_STATE_TOUCH_SCROLL, SCROLL_STATE_FLING})
        public @interface ScrollState { }

        void onScrollStateChange(SeslNumberPicker view, @ScrollState int scrollState);
    }

    public interface OnValueChangeListener {
        void onValueChange(SeslNumberPicker spinner, int oldVal, int newVal);
    }

    public interface Formatter {
        String format(int value);
    }

    @RestrictTo(LIBRARY)
    static Formatter getTwoDigitFormatter() {
        return sTwoDigitFormatter;
    }

    public SeslNumberPicker(Context context) {
        this(context, null);
    }

    public SeslNumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeslNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslNumberPicker(Context context, AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mDelegate = new SeslNumberPickerSpinnerDelegate(this,
                context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPickerContentDescription(String name) {
        mDelegate.setPickerContentDescription(name);
    }

    @RestrictTo(LIBRARY)
    void setAmPm() {
        mDelegate.setAmPm();
    }

    public void setEditTextModeEnabled(boolean enabled) {
        mDelegate.setEditTextModeEnabled(enabled);
    }

    public boolean isEditTextModeEnabled() {
        return mDelegate.isEditTextModeEnabled();
    }

    public void setEditTextMode(boolean isEditTextMode) {
        mDelegate.setEditTextMode(isEditTextMode);
    }

    public boolean isEditTextMode() {
        return mDelegate.isEditTextMode();
    }

    @RestrictTo(LIBRARY)
    public void setCustomIntervalValue(int interval) {
        mDelegate.setCustomIntervalValue(interval);
    }

    public boolean setCustomInterval(int interval) {
        return mDelegate.setCustomInterval(interval);
    }

    public void applyWheelCustomInterval(boolean enabled) {
        mDelegate.applyWheelCustomInterval(enabled);
    }

    @RestrictTo(LIBRARY)
    public boolean isChangedDefaultInterval() {
        return mDelegate.isChangedDefaultInterval();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        mDelegate.onWindowVisibilityChanged(visibility);
        super.onWindowVisibilityChanged(visibility);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mDelegate.onLayout(changed, left, top, right, bottom);
    }

    @SuppressLint("WrongCall")
    void superOnMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @RestrictTo(LIBRARY)
    void setMeasuredDimensionWrapper(int measuredWidth, int measuredHeight) {
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mDelegate.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (mDelegate.dispatchKeyEventPreIme(event)) {
            return true;
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mDelegate.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mDelegate.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDelegate.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mDelegate.dispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mDelegate.onGenericMotionEvent(event)) {
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        mDelegate.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mDelegate.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        mDelegate.dispatchTrackballEvent(event);
        return super.dispatchTrackballEvent(event);
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (mDelegate.isEditTextModeNotAmPm()) {
            return super.dispatchHoverEvent(event);
        }
        return mDelegate.dispatchHoverEvent(event);
    }

    public void setSkipValuesOnLongPressEnabled(boolean enabled) {
    }

    @Override
    public void computeScroll() {
        mDelegate.computeScroll();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mDelegate.setEnabled(enabled);
    }

    @Override
    public void scrollBy(int x, int y) {
        mDelegate.scrollBy(x, y);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mDelegate.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mDelegate.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return mDelegate.computeVerticalScrollExtent();
    }

    public void setOnValueChangedListener(OnValueChangeListener onValueChangeListener) {
        mDelegate.setOnValueChangedListener(onValueChangeListener);
    }

    public void setOnLongPressUpdateInterval(long interval) {
    }

    @RestrictTo(LIBRARY)
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mDelegate.setOnScrollListener(onScrollListener);
    }

    public void setOnEditTextModeChangedListener(
            OnEditTextModeChangedListener onEditTextModeChangedListener) {
        mDelegate.setOnEditTextModeChangedListener(onEditTextModeChangedListener);
    }

    public void setFormatter(Formatter formatter) {
        mDelegate.setFormatter(formatter);
    }

    public void setValue(int value) {
        mDelegate.setValue(value);
    }

    @Override
    public boolean performClick() {
        if (mDelegate.isEditTextModeNotAmPm()) {
            return super.performClick();
        }

        if (super.performClick()) {
            return true;
        }

        mDelegate.performClick();
        return true;
    }

    @RestrictTo(LIBRARY)
    void performClick(boolean toIncrement) {
        mDelegate.performClick(toIncrement);
    }

    @Override
    public boolean performLongClick() {
        if (super.performLongClick()) {
            return true;
        }

        mDelegate.performLongClick();
        return true;
    }

    public boolean getWrapSelectorWheel() {
        return mDelegate.getWrapSelectorWheel();
    }

    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mDelegate.setWrapSelectorWheel(wrapSelectorWheel);
    }

    public int getValue() {
        return mDelegate.getValue();
    }

    public int getMinValue() {
        return mDelegate.getMinValue();
    }

    public void setMinValue(int minValue) {
        mDelegate.setMinValue(minValue);
    }

    public int getMaxValue() {
        return mDelegate.getMaxValue();
    }

    public void setMaxValue(int maxValue) {
        mDelegate.setMaxValue(maxValue);
    }

    public String[] getDisplayedValues() {
        return mDelegate.getDisplayedValues();
    }

    public void setDisplayedValues(String[] displayedValues) {
        mDelegate.setDisplayedValues(displayedValues);
    }

    public void setErrorToastMessage(String msg) {
        mDelegate.setErrorToastMessage(msg);
    }

    public void setTextSize(float size) {
        mDelegate.setTextSize(size);
    }

    public void setSubTextSize(float size) {
        mDelegate.setSubTextSize(size);
    }

    public void setTextTypeface(Typeface typeface) {
        mDelegate.setTextTypeface(typeface);
    }

    public int getPaintFlags() {
        return mDelegate.getPaintFlags();
    }

    public void setPaintFlags(int flags) {
        mDelegate.setPaintFlags(flags);
    }

    public void startAnimation(int delayTime, SeslAnimationListener listener) {
        mDelegate.startAnimation(delayTime, listener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDelegate.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDelegate.onAttachedToWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDelegate.isEditTextModeNotAmPm()) {
            super.onDraw(canvas);
        } else {
            mDelegate.onDraw(canvas);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        mDelegate.onInitializeAccessibilityEvent(event);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        mDelegate.onPopulateAccessibilityEvent(event);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mDelegate.isEditTextModeNotAmPm()) {
            return super.getAccessibilityNodeProvider();
        }
        return mDelegate.getAccessibilityNodeProvider();
    }

    @RestrictTo(LIBRARY)
    void setMaxInputLength(int limit) {
        mDelegate.setMaxInputLength(limit);
    }

    public EditText getEditText() {
        return mDelegate.getEditText();
    }

    public void setMonthInputMode() {
        mDelegate.setMonthInputMode();
    }

    public void setYearDateTimeInputMode() {
        mDelegate.setYearDateTimeInputMode();
    }

    int[] getEnableStateSet() {
        return ENABLED_STATE_SET;
    }

    boolean isVisibleToUserWrapper() {
        return SeslViewReflector.isVisibleToUser(this);
    }

    boolean isVisibleToUserWrapper(Rect boundInView) {
        return SeslViewReflector.isVisibleToUser(this, boundInView);
    }

    @RestrictTo(LIBRARY)
    static class CustomEditText extends EditText {
        private int mAdjustEditTextPosition;
        private String mPickerContentDescription = "";

        public CustomEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setPickerContentDescription(String name) {
            mPickerContentDescription = name;
        }

        public void setEditTextPosition(int pixel) {
            mAdjustEditTextPosition = pixel;
        }

        @Override
        public void onEditorAction(int actionCode) {
            super.onEditorAction(actionCode);
            if (actionCode == EditorInfo.IME_ACTION_DONE) {
                clearFocus();
            }
        }

        @Override
        public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
            final int sizeBeforeSuper = event.getText().size();
            super.onPopulateAccessibilityEvent(event);
            final int sizeAfterSuper = event.getText().size();
            if (sizeAfterSuper > sizeBeforeSuper) {
                event.getText().remove(sizeAfterSuper - 1);
            }

            Editable text = getText();
            if (!TextUtils.isEmpty(text)) {
                event.getText().add(text);
            }
            event.setContentDescription(mPickerContentDescription);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            AccessibilityManager accessibilityManager
                    = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (SeslAccessibilityManagerReflector
                    .isScreenReaderEnabled(accessibilityManager, true)) {
                info.setText(getText());
                AccessibilityNodeInfoCompat.wrap(info).setTooltipText(mPickerContentDescription);
            } else {
                info.setText(getTextForAccessibility());
            }
        }

        private CharSequence getTextForAccessibility() {
            Editable text = getText();

            if (mPickerContentDescription.equals("")) {
                return text;
            }
            if (!TextUtils.isEmpty(text)) {
                return text.toString() + ", " + mPickerContentDescription;
            }

            return ", " + mPickerContentDescription;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.translate(0.0f, (float) mAdjustEditTextPosition);
            super.onDraw(canvas);
        }
    }

    void setDateUnit(int unit) {
        mDelegate.setDateUnit(unit);
    }

    @RestrictTo(LIBRARY)
    interface NumberPickerDelegate {
        void applyWheelCustomInterval(boolean enabled);

        void computeScroll();

        int computeVerticalScrollExtent();

        int computeVerticalScrollOffset();

        int computeVerticalScrollRange();

        boolean dispatchHoverEvent(MotionEvent event);

        boolean dispatchKeyEvent(KeyEvent event);

        boolean dispatchKeyEventPreIme(KeyEvent event);

        boolean dispatchTouchEvent(MotionEvent event);

        void dispatchTrackballEvent(MotionEvent event);

        AccessibilityNodeProvider getAccessibilityNodeProvider();

        String[] getDisplayedValues();

        EditText getEditText();

        int getMaxHeight();

        int getMaxValue();

        int getMaxWidth();

        int getMinHeight();

        int getMinValue();

        int getMinWidth();

        int getPaintFlags();

        int getValue();

        boolean getWrapSelectorWheel();

        boolean isChangedDefaultInterval();

        boolean isEditTextMode();

        boolean isEditTextModeEnabled();

        boolean isEditTextModeNotAmPm();

        void onAttachedToWindow();

        void onConfigurationChanged(Configuration newConfig);

        void onDetachedFromWindow();

        void onDraw(Canvas canvas);

        void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect);

        boolean onGenericMotionEvent(MotionEvent event);

        void onInitializeAccessibilityEvent(AccessibilityEvent event);

        boolean onInterceptTouchEvent(MotionEvent event);

        void onLayout(boolean changed, int left, int top, int right, int bottom);

        void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

        void onPopulateAccessibilityEvent(AccessibilityEvent event);

        boolean onTouchEvent(MotionEvent event);

        void onWindowFocusChanged(boolean hasWindowFocus);

        void onWindowVisibilityChanged(int visibility);

        void performClick();

        void performClick(boolean toIncrement);

        void performLongClick();

        void scrollBy(int x, int y);

        void setAmPm();

        boolean setCustomInterval(int interval);

        void setCustomIntervalValue(int interval);

        void setDateUnit(int unit);

        void setDisplayedValues(String[] displayedValues);

        void setEditTextMode(boolean isEditTextMode);

        void setEditTextModeEnabled(boolean enabled);

        void setEnabled(boolean enabled);

        void setErrorToastMessage(String msg);

        void setFormatter(Formatter formatter);

        void setImeOptions(int imeOptions);

        void setMaxInputLength(int limit);

        void setMaxValue(int maxValue);

        void setMinValue(int minValue);

        void setMonthInputMode();

        void setOnEditTextModeChangedListener(
                OnEditTextModeChangedListener onEditTextModeChangedListener);

        void setOnScrollListener(OnScrollListener onScrollListener);

        void setOnValueChangedListener(OnValueChangeListener onValueChangedListener);

        void setPaintFlags(int flags);

        void setPickerContentDescription(String name);

        void setSubTextSize(float size);

        void setTextSize(float size);

        void setTextTypeface(Typeface typeface);

        void setValue(int value);

        void setWrapSelectorWheel(boolean wrapSelectorWheel);

        void setYearDateTimeInputMode();

        void startAnimation(int delayTime, SeslAnimationListener listener);
    }

    @RestrictTo(LIBRARY)
    static abstract class AbsNumberPickerDelegate implements NumberPickerDelegate {
        protected Context mContext;
        SeslNumberPicker mDelegator;

        AbsNumberPickerDelegate(SeslNumberPicker numberPicker, Context context) {
            mDelegator = numberPicker;
            mContext = context;
        }
    }
}
