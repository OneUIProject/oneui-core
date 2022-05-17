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
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.picker.util.SeslAnimationListener;
import androidx.reflect.view.SeslViewReflector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY)
class SeslSpinningDatePickerSpinner extends LinearLayout {
    private static final DateFormatter mDateFormatter = new DateFormatter();
    private DatePickerDelegate mDelegate;
    private boolean mIsLeapMonth;
    private boolean mIsLunar;

    static class DateFormatter implements Formatter {
        final Object[] mArgs = new Object[1];
        Locale mCurrentLocale;
        SimpleDateFormat mFmt;

        DateFormatter() {
            init(Locale.getDefault());
        }

        private void init(Locale locale) {
            mFmt = createFormatter(locale);
            mCurrentLocale = locale;
        }

        @Override
        public String format(Calendar calendar) {
            Locale locale = Locale.getDefault();
            if (!mCurrentLocale.equals(locale)) {
                init(locale);
            }
            mArgs[0] = calendar;
            return mFmt.format(calendar.getTime());
        }

        public String formatForAccessibility(Calendar calendar, Context context) {
            return DateUtils.formatDateTime(context, calendar.getTimeInMillis(), 26);
        }

        public String format(Calendar calendar, Context context) {
            mArgs[0] = calendar;
            return DateUtils.formatDateTime(context, calendar.getTimeInMillis(), 524314);
        }

        private SimpleDateFormat createFormatter(Locale locale) {
            if (isSimplifiedChinese(locale)) {
                return new SimpleDateFormat("EEEEE, MMM dd", locale);
            }
            if (isRTL(locale)) {
                return new SimpleDateFormat("EEEEE, MMM dd", locale);
            }
            return new SimpleDateFormat("EEE, MMM dd", locale);
        }

        @Override
        public Calendar parse(String value) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            try {
                calendar.setTime(mFmt.parse(value));
                return calendar;
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }

        private boolean isRTL(Locale locale) {
            byte directionality = Character.getDirectionality(locale.getDisplayName(locale).charAt(0));
            return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
        }

        private boolean isSimplifiedChinese(Locale locale) {
            return locale.getLanguage().equals(Locale.SIMPLIFIED_CHINESE.getLanguage())
                    && locale.getCountry().equals(Locale.SIMPLIFIED_CHINESE.getCountry());
        }
    }

    interface OnScrollListener {
        public static final int SCROLL_STATE_IDLE = 0;
        public static final int SCROLL_STATE_TOUCH_SCROLL = 1;
        public static final int SCROLL_STATE_FLING = 2;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({SCROLL_STATE_IDLE, SCROLL_STATE_TOUCH_SCROLL, SCROLL_STATE_FLING})
        public @interface ScrollState {
        }

        void onScrollStateChange(SeslSpinningDatePickerSpinner view, @ScrollState int scrollState);
    }

    interface OnSpinnerDateClickListener {
        void onSpinnerDateClicked(Calendar calendar, SeslSpinningDatePicker.LunarDate lunarDate);
    }

    interface OnValueChangeListener {
        void onValueChange(SeslSpinningDatePickerSpinner spinner,
                           Calendar oldCalendar, Calendar newCalendar,
                           boolean isLeapMonth, SeslSpinningDatePicker.LunarDate lunarDate);
    }

    interface Formatter {
        String format(Calendar calendar);

        Calendar parse(String value);
    }

    static Formatter getDateFormatter() {
        return mDateFormatter;
    }

    public SeslSpinningDatePickerSpinner(Context context) {
        this(context, null);
    }

    public SeslSpinningDatePickerSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeslSpinningDatePickerSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslSpinningDatePickerSpinner(Context context, AttributeSet attrs, int defStyleAttr,
                                         int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mDelegate = new SeslSpinningDatePickerSpinnerDelegate(this,
                context, attrs, defStyleAttr, defStyleRes);
    }

    void setPickerContentDescription(String name) {
        mDelegate.setPickerContentDescription(name);
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
        return mDelegate.dispatchHoverEvent(event);
    }

    public void setSkipValuesOnLongPressEnabled(boolean enabled) {
        mDelegate.setSkipValuesOnLongPressEnabled(enabled);
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

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mDelegate.setOnScrollListener(onScrollListener);
    }

    public void setOnSpinnerDateClickListener(OnSpinnerDateClickListener onSpinnerDateClickListener) {
        mDelegate.setOnSpinnerDateClickListener(onSpinnerDateClickListener);
    }

    public OnSpinnerDateClickListener getOnSpinnerDateClickListener() {
        return mDelegate.getOnSpinnerDateClickListener();
    }

    void updateDate(int year, int month, int dayOfMonth) {
        Calendar calendar = (Calendar) getValue().clone();
        calendar.set(year, month, dayOfMonth);
        if (mIsLunar) {
            calendar = mDelegate.convertLunarToSolar(calendar, year, month, dayOfMonth);
        }
        setValue(calendar);
    }

    void setFormatter(Formatter formatter) {
        mDelegate.setFormatter(formatter);
    }

    void setValue(Calendar value) {
        mDelegate.setValue(value);
    }

    @Override
    public boolean performClick() {
        if (super.performClick()) {
            return true;
        }

        mDelegate.performClick();
        return true;
    }

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

    boolean getWrapSelectorWheel() {
        return mDelegate.getWrapSelectorWheel();
    }

    void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mDelegate.setWrapSelectorWheel(wrapSelectorWheel);
    }

    void setOnLongPressUpdateInterval(long interval) {
        mDelegate.setOnLongPressUpdateInterval(interval);
    }

    Calendar getValue() {
        Calendar calendar = (Calendar) mDelegate.getValue().clone();
        return mIsLunar ?
                mDelegate.convertSolarToLunar(calendar, null) : calendar;
    }

    Calendar getMinValue() {
        return mDelegate.getMinValue();
    }

    void setMinValue(Calendar minValue) {
        mDelegate.setMinValue(minValue);
    }

    void setLunar(boolean isLunar, boolean isLeapMonth) {
        mIsLunar = isLunar;
        mIsLeapMonth = isLeapMonth;
        mDelegate.setLunar(isLunar, isLeapMonth);
    }

    Calendar getMaxValue() {
        return mDelegate.getMaxValue();
    }

    void setMaxValue(Calendar maxValue) {
        mDelegate.setMaxValue(maxValue);
    }

    void setTextSize(float size) {
        mDelegate.setTextSize(size);
    }

    void setSubTextSize(float size) {
        mDelegate.setSubTextSize(size);
    }

    void setTextTypeface(Typeface typeface) {
        mDelegate.setTextTypeface(typeface);
    }

    int getPaintFlags() {
        return mDelegate.getPaintFlags();
    }

    void setPaintFlags(int flags) {
        mDelegate.setPaintFlags(flags);
    }

    void startAnimation(int delayTime, SeslAnimationListener listener) {
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
        mDelegate.onDraw(canvas);
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
        return mDelegate.getAccessibilityNodeProvider();
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

    static class CustomEditText extends EditText {
        private int mAdjustEditTextPosition;

        public CustomEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        void setEditTextPosition(int pixel) {
            mAdjustEditTextPosition = pixel;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.translate(0.0f, mAdjustEditTextPosition);
            super.onDraw(canvas);
        }
    }

    interface DatePickerDelegate {
        void computeScroll();

        int computeVerticalScrollExtent();

        int computeVerticalScrollOffset();

        int computeVerticalScrollRange();

        Calendar convertLunarToSolar(Calendar calendar, int year, int monthOfYear, int dayOfMonth);

        Calendar convertSolarToLunar(Calendar calendar, SeslSpinningDatePicker.LunarDate lunarDate);

        boolean dispatchHoverEvent(MotionEvent event);

        boolean dispatchKeyEvent(KeyEvent event);

        boolean dispatchKeyEventPreIme(KeyEvent event);

        boolean dispatchTouchEvent(MotionEvent event);

        void dispatchTrackballEvent(MotionEvent event);

        AccessibilityNodeProvider getAccessibilityNodeProvider();

        int getMaxHeight();

        Calendar getMaxValue();

        int getMaxWidth();

        int getMinHeight();

        Calendar getMinValue();

        int getMinWidth();

        OnSpinnerDateClickListener getOnSpinnerDateClickListener();

        int getPaintFlags();

        Calendar getValue();

        boolean getWrapSelectorWheel();

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

        void setEnabled(boolean enabled);

        void setFormatter(Formatter formatter);

        void setLunar(boolean isLunar, boolean isLeapMonth);

        void setMaxValue(Calendar maxValue);

        void setMinValue(Calendar minValue);

        void setOnLongPressUpdateInterval(long interval);

        void setOnScrollListener(OnScrollListener onScrollListener);

        void setOnSpinnerDateClickListener(OnSpinnerDateClickListener onSpinnerDateClickListener);

        void setOnValueChangedListener(OnValueChangeListener onValueChangeListener);

        void setPaintFlags(int flags);

        void setPickerContentDescription(String name);

        void setSkipValuesOnLongPressEnabled(boolean enabled);

        void setSubTextSize(float size);

        void setTextSize(float size);

        void setTextTypeface(Typeface typeface);

        void setValue(Calendar value);

        void setWrapSelectorWheel(boolean wrapSelectorWheel);

        void startAnimation(int delayTime, SeslAnimationListener listener);
    }

    static abstract class AbsDatePickerDelegate implements DatePickerDelegate {
        Context mContext;
        SeslSpinningDatePickerSpinner mDelegator;

        AbsDatePickerDelegate(SeslSpinningDatePickerSpinner spinningDatePickerSpinner, Context context) {
            mDelegator = spinningDatePickerSpinner;
            mContext = context;
        }
    }
}
