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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.picker.R;
import androidx.reflect.feature.SeslCscFeatureReflector;
import androidx.reflect.lunarcalendar.SeslFeatureReflector;
import androidx.reflect.lunarcalendar.SeslLunarDateUtilsReflector;
import androidx.reflect.lunarcalendar.SeslSolarLunarConverterReflector;
import androidx.reflect.view.SeslViewReflector;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY)
public class SeslSimpleMonthView extends View {
    private static final String TAG = "SeslSimpleMonthView";

    private static final String TAG_CSCFEATURE_CALENDAR_SETCOLOROFDAYS = "CscFeature_Calendar_SetColorOfDays";
    private static final String DEFAULT_WEEK_DAY_STRING_FEATURE = "XXXXXXR";

    private static final int DEFAULT_MONTH_LINE = 6;
    private static final int DEFAULT_NUM_DAYS = 7;
    private static final int DEFAULT_WEEK_START = 1;

    private static final float DIVISOR_FOR_CIRCLE_POSITION_Y = 2.7f;

    private static final int SIZE_UNSPECIFIED = -1;

    private static final int LEAP_MONTH = 1;
    private static final float LEAP_MONTH_WEIGHT = 0.5f;

    private static final int MAX_MONTH_VIEW_ID = 42;

    private static final int MIN_HEIGHT = 10;
    private static final int YEAR_WEIGHT = 10000;
    private static final int MONTH_WEIGHT = 100;

    private Paint mAbnormalSelectedDayPaint;
    private final Calendar mCalendar = Calendar.getInstance();
    private Context mContext;
    private Paint mDayNumberPaint;
    private Paint mDayNumberSelectedPaint;
    private Paint mHcfEnabledDayNumberPaint;
    private Calendar mMaxDate = Calendar.getInstance();
    private Calendar mMinDate = Calendar.getInstance();
    private OnDayClickListener mOnDayClickListener;
    private OnDeactivatedDayClickListener mOnDeactivatedDayClickListener;
    private PathClassLoader mPathClassLoader = null;
    private Object mSolarLunarConverter;
    private Calendar mTempDate = Calendar.getInstance();
    private final MonthViewTouchHelper mTouchHelper;
    private int mWeekHeight;
    private int mWeekStart = DEFAULT_WEEK_START;
    private int mYear;

    private final int mAbnormalStartEndDateBackgroundAlpha;
    private int mCalendarWidth;
    private int[] mDayColorSet = new int[DEFAULT_NUM_DAYS];
    private int mDayNumberDisabledAlpha;
    private int mDayOfWeekStart = 0;
    private int mDaySelectedCircleSize;
    private int mDaySelectedCircleStroke;
    private int mEnabledDayEnd = 31;
    private int mEnabledDayStart = 1;
    private int mEndDay;
    private int mEndMonth;
    private int mEndYear;
    private int mIsLeapEndMonth;
    private int mIsLeapStartMonth;
    private int mLastAccessibilityFocusedView = View.NO_ID;
    private int mMiniDayNumberTextSize;
    private int mMode = SeslDatePicker.DATE_MODE_NONE;
    private int mMonth;
    private int mNormalTextColor;
    private int mNumCells = DEFAULT_NUM_DAYS;
    private int mNumDays = DEFAULT_NUM_DAYS;
    private int mPadding = 0;
    private final int mPrevNextMonthDayNumberAlpha;
    private int mSaturdayTextColor;
    private int mSelectedDay = -1;
    private int mSelectedDayColor;
    private int mSelectedDayNumberTextColor;
    private int mStartDay;
    private int mStartMonth;
    private int mStartYear;
    private int mSundayTextColor;

    private boolean mIsFirstMonth = false;
    private boolean mIsHcfEnabled = false;
    private boolean mIsLastMonth = false;
    private boolean mIsLeapMonth = false;
    private boolean mIsLunar = false;
    private boolean mIsNextMonthLeap = false;
    private boolean mIsPrevMonthLeap = false;
    private boolean mIsRTL;
    private boolean mLockAccessibilityDelegate;
    private boolean mLostAccessibilityFocus = false;

    interface OnDayClickListener {
        void onDayClick(SeslSimpleMonthView view, int year, int month, int day);
    }

    interface OnDeactivatedDayClickListener {
        void onDeactivatedDayClick(SeslSimpleMonthView view, int year, int month, int day,
                                   boolean isLeapMonth, boolean pageChanged);
    }

    SeslSimpleMonthView(Context context) {
        this(context, null);
    }

    SeslSimpleMonthView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.datePickerStyle);
    }

    SeslSimpleMonthView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mContext = context;
        mIsRTL = isRTL();

        final Resources res = context.getResources();

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryDark, outValue, true);
        if (outValue.resourceId != 0) {
            mSelectedDayColor = res.getColor(outValue.resourceId);
        } else {
            mSelectedDayColor = outValue.data;
        }
        mSundayTextColor = res.getColor(R.color.sesl_date_picker_sunday_number_text_color_light);
        mSaturdayTextColor = res.getColor(R.color.sesl_date_picker_saturday_text_color_light);

        TypedArray array = mContext
                .obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyle, 0);
        mNormalTextColor = array.getColor(R.styleable.DatePicker_dayNumberTextColor,
                res.getColor(R.color.sesl_date_picker_normal_day_number_text_color_light));
        mSelectedDayNumberTextColor = array.getColor(R.styleable.DatePicker_selectedDayNumberTextColor,
                res.getColor(R.color.sesl_date_picker_selected_day_number_text_color_light));
        mDayNumberDisabledAlpha = array.getInteger(R.styleable.DatePicker_dayNumberDisabledAlpha,
                res.getInteger(R.integer.sesl_day_number_disabled_alpha_light));
        array.recycle();

        mWeekHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_week_height);
        mDaySelectedCircleSize = res.getDimensionPixelSize(R.dimen.sesl_date_picker_selected_day_circle_radius);
        mDaySelectedCircleStroke = res.getDimensionPixelSize(R.dimen.sesl_date_picker_selected_day_circle_stroke);
        mMiniDayNumberTextSize = res.getDimensionPixelSize(R.dimen.sesl_date_picker_day_number_text_size);
        mCalendarWidth = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_width);
        mPadding = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_padding);

        mTouchHelper = new MonthViewTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        setImportantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        mLockAccessibilityDelegate = true;

        final String packageName = Settings.System.getString(mContext.getContentResolver(),
                "current_sec_active_themepackage");
        if (packageName != null) {
            mDayNumberDisabledAlpha = res.getInteger(R.integer.sesl_day_number_theme_disabled_alpha);
        }
        mPrevNextMonthDayNumberAlpha = res.getInteger(R.integer.sesl_day_number_theme_disabled_alpha);
        mAbnormalStartEndDateBackgroundAlpha = res
                .getInteger(R.integer.sesl_date_picker_abnormal_start_end_date_background_alpha);

        initView();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mIsRTL = isRTL();
        mTouchHelper.invalidateRoot();

        Resources res = mContext.getResources();
        mWeekHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_week_height);
        mDaySelectedCircleSize = res.getDimensionPixelSize(R.dimen.sesl_date_picker_selected_day_circle_radius);
        mMiniDayNumberTextSize = res.getDimensionPixelSize(R.dimen.sesl_date_picker_day_number_text_size);

        initView();
    }

    void setTextColor(String weekDayFeatureString) {
        if (weekDayFeatureString == null) {
            weekDayFeatureString = SeslCscFeatureReflector.getString(TAG_CSCFEATURE_CALENDAR_SETCOLOROFDAYS,
                    DEFAULT_WEEK_DAY_STRING_FEATURE);
        }
        for (int i = 0; i < mNumDays; i++) {
            final char parsedColor = weekDayFeatureString.charAt(i);
            final int index = (i + 2) % mNumDays;
            if (parsedColor == 'R') {
                mDayColorSet[index] = mSundayTextColor;
            } else if (parsedColor == 'B') {
                mDayColorSet[index] = mSaturdayTextColor;
            } else {
                mDayColorSet[index] = mNormalTextColor;
            }
        }
    }

    @Override
    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        if (!mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate);
        }
    }

    void setOnDayClickListener(OnDayClickListener listener) {
        mOnDayClickListener = listener;
    }

    void setOnDeactivatedDayClickListener(OnDeactivatedDayClickListener listener) {
        mOnDeactivatedDayClickListener = listener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return mTouchHelper.dispatchHoverEvent(event)
                || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            final int day = getDayFromLocation(event.getX(), event.getY());
            if (mIsFirstMonth && day < mEnabledDayStart
                    || mIsLastMonth && day > mEnabledDayEnd) {
                return true;
            }

            if (day > 0) {
                if (day > mNumCells) {
                    if (mIsLunar) {
                        int nextYear = mYear;
                        int nextMonth = mMonth + (mIsNextMonthLeap ? 0 : LEAP_MONTH);
                        if (nextMonth > Calendar.DECEMBER) {
                            nextYear++;
                            nextMonth = Calendar.JANUARY;
                        }
                        onDeactivatedDayClick(nextYear,
                                nextMonth, day - mNumCells,
                                false);
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        calendar.clear();
                        calendar.set(mYear, mMonth, mNumCells);
                        calendar.add(Calendar.DAY_OF_MONTH, day - mNumCells);
                        onDeactivatedDayClick(calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                false);
                    }
                } else {
                    onDayClick(mYear, mMonth, day);
                }
            } else {
                if (mIsLunar) {
                    int prevYear = mYear;
                    int prevMonth = mMonth - (mIsLeapMonth ? 0 : LEAP_MONTH);
                    if (prevMonth < 0) {
                        prevYear--;
                        prevMonth = Calendar.DECEMBER;
                    }
                    onDeactivatedDayClick(prevYear, prevMonth,
                            getDaysInMonthLunar(prevMonth, prevYear, mIsPrevMonthLeap) + day,
                            true);
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.clear();
                    calendar.set(mYear, mMonth, 1);
                    calendar.add(Calendar.DAY_OF_MONTH, day - 1);
                    onDeactivatedDayClick(calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                            true);
                }
            }
        }
        return true;
    }

    private void initView() {
        mDayNumberSelectedPaint = new Paint();
        mDayNumberSelectedPaint.setAntiAlias(true);
        mDayNumberSelectedPaint.setColor(mSelectedDayColor);
        mDayNumberSelectedPaint.setTextAlign(Paint.Align.CENTER);
        mDayNumberSelectedPaint.setStrokeWidth(mDaySelectedCircleStroke);
        mDayNumberSelectedPaint.setFakeBoldText(true);
        mDayNumberSelectedPaint.setStyle(Paint.Style.FILL);

        mAbnormalSelectedDayPaint = new Paint(mDayNumberSelectedPaint);
        mAbnormalSelectedDayPaint.setColor(mNormalTextColor);
        mAbnormalSelectedDayPaint.setAlpha(mAbnormalStartEndDateBackgroundAlpha);

        mDayNumberPaint = new Paint();
        mDayNumberPaint.setAntiAlias(true);
        mDayNumberPaint.setTextSize((float) mMiniDayNumberTextSize);
        mDayNumberPaint.setTypeface(Typeface.create("sec-roboto-light", Typeface.NORMAL));
        mDayNumberPaint.setTextAlign(Paint.Align.CENTER);
        mDayNumberPaint.setStyle(Paint.Style.FILL);
        mDayNumberPaint.setFakeBoldText(false);

        mHcfEnabledDayNumberPaint = new Paint(mDayNumberPaint);
        mHcfEnabledDayNumberPaint.setTypeface(Typeface.create("sec-roboto-light", Typeface.BOLD));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawDays(canvas);
    }

    void setMonthParams(int selectedDay, int month, int year, int weekStart,
                               int enabledDayStart, int enabledDayEnd,
                               Calendar minDate, Calendar maxDate,
                               int startYear, int startMonth, int startDay,
                               int isLeapStartMonth, int endYear, int endMonth, int endDay,
                               int isLeapEndMonth, int mode) {
        mMode = mode;

        if (mWeekHeight < MIN_HEIGHT) {
            mWeekHeight = MIN_HEIGHT;
        }
        mSelectedDay = selectedDay;
        if (isValidMonth(month)) {
            mMonth = month;
        }
        mYear = year;

        mCalendar.clear();
        mCalendar.set(Calendar.MONTH, mMonth);
        mCalendar.set(Calendar.YEAR, mYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);

        mMinDate = minDate;
        mMaxDate = maxDate;

        if (mIsLunar) {
            if (mSolarLunarConverter != null) {
                SeslSolarLunarConverterReflector
                        .convertLunarToSolar(mPathClassLoader,
                                mSolarLunarConverter, mYear, mMonth, 1, mIsLeapMonth);
                final int lunarYear = SeslSolarLunarConverterReflector
                        .getYear(mPathClassLoader, mSolarLunarConverter);
                final int lunarMonth = SeslSolarLunarConverterReflector.
                        getMonth(mPathClassLoader, mSolarLunarConverter);
                final int lunarDay = SeslSolarLunarConverterReflector
                        .getDay(mPathClassLoader, mSolarLunarConverter);
                mDayOfWeekStart = SeslSolarLunarConverterReflector
                        .getWeekday(mPathClassLoader, mSolarLunarConverter, lunarYear,
                                lunarMonth, lunarDay) + 1;
                mNumCells = getDaysInMonthLunar(mMonth, mYear, mIsLeapMonth);
            }
        } else {
            mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK);
            mNumCells = getDaysInMonth(mMonth, mYear);
        }

        if (isValidDayOfWeek(weekStart)) {
            mWeekStart = weekStart;
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        if (mMonth == minDate.get(Calendar.MONTH) && mYear == minDate.get(Calendar.YEAR)) {
            enabledDayStart = minDate.get(Calendar.DAY_OF_MONTH);
        }
        if (mMonth == maxDate.get(Calendar.MONTH) && mYear == maxDate.get(Calendar.YEAR)) {
            enabledDayEnd = maxDate.get(Calendar.DAY_OF_MONTH);
        }

        if (enabledDayStart > 0 && enabledDayEnd < 32) {
            mEnabledDayStart = enabledDayStart;
        }
        if (enabledDayEnd > 0 && enabledDayEnd < 32
                && enabledDayEnd >= enabledDayStart) {
            mEnabledDayEnd = enabledDayEnd;
        }

        mTouchHelper.invalidateRoot();

        mStartYear = startYear;
        mStartMonth = startMonth;
        mStartDay = startDay;
        mIsLeapStartMonth = isLeapStartMonth;
        mEndYear = endYear;
        mEndMonth = endMonth;
        mEndDay = endDay;
        mIsLeapEndMonth = isLeapEndMonth;
    }

    private int getDaysInMonthLunar(int month, int year, boolean isLeapMonth) {
        final int solarDay = getDaysInMonth(month, year);
        if (mSolarLunarConverter != null) {
            return SeslSolarLunarConverterReflector
                    .getDayLengthOf(mPathClassLoader, mSolarLunarConverter, year, month, isLeapMonth);
        } else  {
            Log.e(TAG, "getDaysInMonthLunar, mSolarLunarConverter is null");
            return solarDay;
        }
    }

    private static int getDaysInMonth(int month, int year) {
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.MARCH:
            case Calendar.MAY:
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.OCTOBER:
            case Calendar.DECEMBER:
                return 31;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            case Calendar.FEBRUARY:
                if (year % 4 == 0) {
                    return (year % 100 != 0 || year % 400 == 0) ? 29 : 28;
                }
                return 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mCalendarWidth);
        super.onMeasure(newWidthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!mLostAccessibilityFocus && mLastAccessibilityFocusedView == View.NO_ID
                && mSelectedDay != -1) {
            mTouchHelper.sendEventForVirtualView(mSelectedDay + findDayOffset(),
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        } else if (!mLostAccessibilityFocus && mLastAccessibilityFocusedView != View.NO_ID) {
            mTouchHelper.sendEventForVirtualView(mLastAccessibilityFocusedView + findDayOffset(),
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        }

        if (changed) {
            mTouchHelper.invalidateRoot();
        }

        super.onLayout(changed, left, top, right, bottom);
    }

    private int makeMeasureSpec(int widthMeasureSpec, int heightMeasureSpec) {
        if (heightMeasureSpec == ViewGroup.LayoutParams.MATCH_PARENT) {
            return widthMeasureSpec;
        }

        final int size = MeasureSpec.getSize(widthMeasureSpec);
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        switch (mode) {
            case MeasureSpec.AT_MOST:
                mCalendarWidth = Math.min(size, heightMeasureSpec);
                return MeasureSpec.makeMeasureSpec(mCalendarWidth, MeasureSpec.EXACTLY);
            case MeasureSpec.UNSPECIFIED:
                return MeasureSpec.makeMeasureSpec(heightMeasureSpec, MeasureSpec.EXACTLY);
            case MeasureSpec.EXACTLY:
                mCalendarWidth = size;
                return widthMeasureSpec;
        }

        throw new IllegalArgumentException("Unknown measure mode: " + mode);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mTouchHelper.invalidateRoot();
    }

    // TODO rework this method
    // kang
    private void drawDays(Canvas var1) {
        /* var1 = canvas */
        int var2 = this.mWeekHeight * 2 / 3;
        int var3 = this.mCalendarWidth / (this.mNumDays * 2);
        int var4 = this.findDayOffset();
        float var5 = (float)this.mMiniDayNumberTextSize / 2.7F;
        int var6 = this.mStartYear;
        float var7 = (float)this.mStartMonth;
        int var8 = this.mStartDay;
        int var9 = this.mEndYear;
        float var10 = (float)this.mEndMonth;
        int var11 = this.mEndDay;
        boolean var12 = this.mIsLunar;
        float var13 = var7;
        if (var12) {
            var13 = var7;
            if (this.mIsLeapStartMonth == 1) {
                var13 = var7 + 0.5F;
            }
        }

        float var14 = var13;
        var7 = var10;
        if (var12) {
            var7 = var10;
            if (this.mIsLeapEndMonth == 1) {
                var7 = var10 + 0.5F;
            }
        }

        int var15 = this.mYear;
        var13 = (float)this.mMonth;
        var10 = var13;
        if (var12) {
            var10 = var13;
            if (this.mIsLeapMonth) {
                var10 = var13 + 0.5F;
            }
        }

        int var16 = var6 * 10000 + (int)(var14 * 100.0F);
        int var17 = var9 * 10000 + (int)(var7 * 100.0F);
        int var18 = var15 * 10000 + (int)(100.0F * var10);
        boolean var20;
        if (this.mMode != 0) {
            boolean var19;
            if (var16 + var8 > var17 + var11) {
                var19 = true;
            } else {
                var19 = false;
            }

            var20 = var19;
        } else {
            var20 = false;
        }

        int var31;
        label404: {
            if (!var20) {
                label413: {
                    label410: {
                        if (var6 == var9 && var14 == var7 && var15 == var6 && var10 == var14) {
                            var17 = var11;
                        } else {
                            if (var16 < var18 && var18 < var17 && (var15 != var9 || var10 != var7)) {
                                var17 = this.mNumCells + 1;
                                break label410;
                            }

                            if (var15 != var6 || var10 != var14) {
                                if (var15 != var9 || var10 != var7) {
                                    break label413;
                                }

                                var17 = var11;
                                break label410;
                            }

                            var17 = this.mNumCells + 1;
                        }

                        var31 = var8;
                        break label404;
                    }

                    var31 = 0;
                    break label404;
                }
            }

            var31 = -1;
            var17 = -1;
        }

        this.mIsHcfEnabled = this.isHighContrastFontEnabled();
        var18 = 0;
        byte var21 = 1;
        var16 = var4;
        var13 = var5;
        int var22 = var11;
        var11 = var2;

        Paint var25;
        Paint var26;
        for(var2 = var21; var2 <= this.mNumCells; ++var2) {
            int var23;
            int var33;
            if (this.mIsRTL) {
                var33 = ((this.mNumDays - 1 - var16) * 2 + 1) * var3;
                var23 = this.mPadding;
            } else {
                var33 = (var16 * 2 + 1) * var3;
                var23 = this.mPadding;
            }

            int var24 = var33 + var23;
            var33 = this.mWeekStart;
            var23 = this.mNumDays;
            this.mDayNumberPaint.setColor(this.mDayColorSet[(var16 + var33) % var23]);
            if (var2 < this.mEnabledDayStart || var2 > this.mEnabledDayEnd) {
                this.mDayNumberPaint.setAlpha(this.mDayNumberDisabledAlpha);
            }

            var25 = this.mDayNumberPaint;
            var26 = var25;
            if (this.mIsHcfEnabled) {
                var26 = var25;
                if (var25.getAlpha() != this.mDayNumberDisabledAlpha) {
                    this.mHcfEnabledDayNumberPaint.setColor(this.mDayNumberPaint.getColor());
                    var26 = this.mHcfEnabledDayNumberPaint;
                }
            }

            if (var20) {
                label380: {
                    label322: {
                        label381: {
                            if (var6 == var15 && var14 == var10 && var8 == var2) {
                                var33 = this.mMode;
                                if (var33 == 2 || var33 == 3) {
                                    break label381;
                                }
                            }

                            if (var9 != var15 || var7 != var10 || var22 != var2) {
                                break label322;
                            }

                            var33 = this.mMode;
                            if (var33 != 1 && var33 != 3) {
                                break label322;
                            }
                        }

                        var1.drawCircle((float)var24, (float)var11 - var13, (float)this.mDaySelectedCircleSize, this.mDayNumberSelectedPaint);
                        var26.setColor(this.mSelectedDayNumberTextColor);
                    }

                    label382: {
                        if (var9 == var15 && var7 == var10 && var22 == var2) {
                            var33 = this.mMode;
                            if (var33 == 2 || var33 == 3) {
                                break label382;
                            }
                        }

                        if (var6 != var15 || var14 != var10 || var8 != var2) {
                            break label380;
                        }

                        var33 = this.mMode;
                        if (var33 != 1 && var33 != 3) {
                            break label380;
                        }
                    }

                    var1.drawCircle((float)var24, (float)var11 - var13, (float)this.mDaySelectedCircleSize, this.mAbnormalSelectedDayPaint);
                }
            } else {
                float var27;
                float var28;
                if (var31 < var2 && var2 < var17) {
                    var27 = (float)(var24 - var3);
                    var28 = (float)var11;
                    var33 = this.mDaySelectedCircleSize;
                    var28 = var28 - var13 - (float)var33;
                    var1.drawRect(var27, var28, var27 + (float)(var3 * 2), var28 + (float)(var33 * 2), this.mDayNumberSelectedPaint);
                    var26.setColor(this.mSelectedDayNumberTextColor);
                }

                var33 = var31;
                if (var31 != -1 && var31 == var17 && var2 == var31) {
                    var1.drawCircle((float)var24, (float)var11 - var13, (float)this.mDaySelectedCircleSize, this.mDayNumberSelectedPaint);
                    var26.setColor(this.mSelectedDayNumberTextColor);
                    var31 = var31;
                } else if (var17 == var2) {
                    var27 = (float)var11 - var13;
                    if (this.mIsRTL) {
                        var5 = (float)var24;
                    } else {
                        var5 = (float)(var24 - var3);
                    }

                    var31 = this.mDaySelectedCircleSize;
                    var28 = var27 - (float)var31;
                    var1.drawRect(var5, var28, (float)var3 + var5, var28 + (float)(var31 * 2), this.mDayNumberSelectedPaint);
                    var1.drawCircle((float)var24, var27, (float)this.mDaySelectedCircleSize, this.mDayNumberSelectedPaint);
                    var26.setColor(this.mSelectedDayNumberTextColor);
                    var31 = var33;
                } else {
                    var31 = var31;
                    if (var33 == var2) {
                        var27 = (float)var11 - var13;
                        if (this.mIsRTL) {
                            var5 = (float)(var24 - var3);
                        } else {
                            var5 = (float)var24;
                        }

                        var31 = this.mDaySelectedCircleSize;
                        var28 = var27 - (float)var31;
                        var1.drawRect(var5, var28, (float)var3 + var5, var28 + (float)(var31 * 2), this.mDayNumberSelectedPaint);
                        var1.drawCircle((float)var24, var27, (float)this.mDaySelectedCircleSize, this.mDayNumberSelectedPaint);
                        var26.setColor(this.mSelectedDayNumberTextColor);
                        var31 = var33;
                    }
                }
            }

            if (this.mMode == 0 && var2 == var17) {
                var26.setColor(this.mSelectedDayNumberTextColor);
            }

            var1.drawText(String.format("%d", var2), (float)var24, (float)var11, var26);
            ++var16;
            if (var16 == this.mNumDays) {
                var11 += this.mWeekHeight;
                ++var18;
                var16 = 0;
            }
        }

        var2 = var11;
        var11 = var31;
        var31 = var31;
        int var32;
        if (!this.mIsLastMonth) {
            byte var34 = 1;
            var32 = var16;
            var16 = var2;
            var2 = var34;

            while(true) {
                var31 = var11;
                if (var18 == 6) {
                    break;
                }

                if (this.mIsRTL) {
                    var31 = ((this.mNumDays - 1 - var32) * 2 + 1) * var3 + this.mPadding;
                } else {
                    var31 = (var32 * 2 + 1) * var3 + this.mPadding;
                }

                var8 = this.mWeekStart;
                var9 = this.mNumDays;
                this.mDayNumberPaint.setColor(this.mDayColorSet[(var32 + var8) % var9]);
                this.mDayNumberPaint.setAlpha(this.mPrevNextMonthDayNumberAlpha);
                if (this.mMode != 0 && var17 == this.mNumCells + 1) {
                    if (var2 >= this.mEndDay && this.isNextMonthEndMonth()) {
                        if (var2 == this.mEndDay) {
                            var10 = (float)var16 - var13;
                            if (this.mIsRTL) {
                                var7 = (float)var31;
                            } else {
                                var7 = (float)(var31 - var3);
                            }

                            var8 = this.mDaySelectedCircleSize;
                            var14 = var10 - (float)var8;
                            var1.drawRect(var7, var14, (float)var3 + var7, var14 + (float)(var8 * 2), this.mDayNumberSelectedPaint);
                            var1.drawCircle((float)var31, var10, (float)this.mDaySelectedCircleSize, this.mDayNumberSelectedPaint);
                        }
                    } else {
                        var7 = (float)(var31 - var3);
                        var10 = (float)var16;
                        var8 = this.mDaySelectedCircleSize;
                        var10 = var10 - var13 - (float)var8;
                        var1.drawRect(var7, var10, var7 + (float)(var3 * 2), var10 + (float)(var8 * 2), this.mDayNumberSelectedPaint);
                    }
                }

                if (!this.mIsLunar) {
                    var22 = this.mMonth + 1;
                    var6 = this.mYear;
                    var9 = var22;
                    var8 = var6;
                    if (var22 > 11) {
                        var8 = var6 + 1;
                        var9 = 0;
                    }

                    this.mTempDate.clear();
                    this.mTempDate.set(var8, var9, var2);
                    if (this.mTempDate.after(this.mMaxDate)) {
                        this.mDayNumberPaint.setAlpha(this.mDayNumberDisabledAlpha);
                    }
                }

                var25 = this.mDayNumberPaint;
                var26 = var25;
                if (this.mIsHcfEnabled) {
                    var26 = var25;
                    if (var25.getAlpha() != this.mDayNumberDisabledAlpha) {
                        this.mHcfEnabledDayNumberPaint.setColor(this.mDayNumberPaint.getColor());
                        var26 = this.mHcfEnabledDayNumberPaint;
                    }
                }

                if (this.mMode != 0 && var17 == this.mNumCells + 1 && (var2 <= this.mEndDay || !this.isNextMonthEndMonth())) {
                    var26.setColor(this.mSelectedDayNumberTextColor);
                }

                var1.drawText(String.format("%d", var2), (float)var31, (float)var16, var26);
                var31 = var32 + 1;
                if (var31 == this.mNumDays) {
                    var16 += this.mWeekHeight;
                    ++var18;
                    var31 = 0;
                }

                ++var2;
                var32 = var31;
            }
        }

        if (var4 > 0 && !this.mIsFirstMonth) {
            Calendar var35 = Calendar.getInstance();
            var35.clear();
            var35.set(this.mYear, this.mMonth, 1);
            var35.add(5, -var4);
            var11 = var35.get(5);
            if (this.mIsLunar) {
                var18 = this.mYear;
                var2 = this.mMonth - (mIsLeapMonth ? 0 : 1);
                var17 = var18;
                var11 = var2;
                if (var2 < 0) {
                    var17 = var18 - 1;
                    var11 = 11;
                }

                var11 = this.getDaysInMonthLunar(var11, var17, this.mIsPrevMonthLeap) - var4 + 1;
            }

            var17 = var11;

            for(var11 = 0; var11 < var4; ++var11) {
                if (this.mIsRTL) {
                    var2 = ((this.mNumDays - 1 - var11) * 2 + 1) * var3;
                    var18 = this.mPadding;
                } else {
                    var2 = (var11 * 2 + 1) * var3;
                    var18 = this.mPadding;
                }

                var9 = var2 + var18;
                var8 = this.mWeekHeight * 2 / 3;
                var18 = this.mWeekStart;
                var2 = this.mNumDays;
                this.mDayNumberPaint.setColor(this.mDayColorSet[(var18 + var11) % var2]);
                this.mDayNumberPaint.setAlpha(this.mPrevNextMonthDayNumberAlpha);
                if (this.mMode != 0 && var31 == 0) {
                    if (var17 <= this.mStartDay && this.isPrevMonthStartMonth()) {
                        if (var17 == this.mStartDay) {
                            var10 = (float)var8 - var13;
                            if (this.mIsRTL) {
                                var7 = (float)(var9 - var3);
                            } else {
                                var7 = (float)var9;
                            }

                            var2 = this.mDaySelectedCircleSize;
                            var14 = var10 - (float)var2;
                            var1.drawRect(var7, var14, (float)var3 + var7, var14 + (float)(var2 * 2), this.mDayNumberSelectedPaint);
                            var1.drawCircle((float)var9, var10, (float)this.mDaySelectedCircleSize, this.mDayNumberSelectedPaint);
                        }
                    } else {
                        var7 = (float)(var9 - var3);
                        var10 = (float)var8;
                        var2 = this.mDaySelectedCircleSize;
                        var10 = var10 - var13 - (float)var2;
                        var1.drawRect(var7, var10, var7 + (float)(var3 * 2), var10 + (float)(var2 * 2), this.mDayNumberSelectedPaint);
                    }
                }

                if (!this.mIsLunar) {
                    var32 = this.mMonth - 1;
                    var16 = this.mYear;
                    var18 = var32;
                    var2 = var16;
                    if (var32 < 0) {
                        var2 = var16 - 1;
                        var18 = 11;
                    }

                    this.mTempDate.clear();
                    this.mTempDate.set(var2, var18, var17);
                    var35 = Calendar.getInstance();
                    var35.clear();
                    var35.set(this.mMinDate.get(1), this.mMinDate.get(2), this.mMinDate.get(5));
                    if (this.mTempDate.before(this.mMinDate)) {
                        this.mDayNumberPaint.setAlpha(this.mDayNumberDisabledAlpha);
                    }
                }

                var25 = this.mDayNumberPaint;
                var26 = var25;
                if (this.mIsHcfEnabled) {
                    var26 = var25;
                    if (var25.getAlpha() != this.mDayNumberDisabledAlpha) {
                        this.mHcfEnabledDayNumberPaint.setColor(this.mDayNumberPaint.getColor());
                        var26 = this.mHcfEnabledDayNumberPaint;
                    }
                }

                if (this.mMode != 0 && var31 == 0 && (var17 >= this.mStartDay || !this.isPrevMonthStartMonth())) {
                    var26.setColor(this.mSelectedDayNumberTextColor);
                }

                var1.drawText(String.format("%d", var17), (float)var9, (float)var8, var26);
                ++var17;
            }
        }

    }
    // kang

    private boolean isPrevMonthStartMonth() {
        if (mIsLunar) {
            float month = mMonth;
            float startMonth = mStartMonth;
            if (mIsLeapMonth) {
                month += LEAP_MONTH_WEIGHT;
            }
            if (mIsLeapStartMonth == 1) {
                startMonth += LEAP_MONTH_WEIGHT;
            }

            float monthDiff = month - startMonth;
            if (mYear != mStartYear || (monthDiff >= 1.0f
                    && (monthDiff != 1.0f || mIsPrevMonthLeap))) {
                if (mYear != mStartYear + 1) {
                    return false;
                }
                if (monthDiff + 12.0f >= 1.0f
                        && (monthDiff + 12.0f != 1.0f || mIsPrevMonthLeap)) {
                    return false;
                }
            }
            return true;
        } else {
            return (mYear == mStartYear && mMonth == mStartMonth + 1)
                    || (mYear == mStartYear + 1 && mMonth == Calendar.JANUARY
                        && mStartMonth == Calendar.DECEMBER);
        }
    }

    private boolean isNextMonthEndMonth() {
        if (mIsLunar) {
            float month = mMonth;
            float endMonth = mEndMonth;
            if (mIsLeapMonth) {
                month += LEAP_MONTH_WEIGHT;
            }
            if (mIsLeapEndMonth == 1) {
                endMonth += LEAP_MONTH_WEIGHT;
            }

            float monthDiff = endMonth - month;
            if (mYear != mEndYear || (monthDiff >= 1.0f
                    && (monthDiff != 1.0f || mIsNextMonthLeap))) {
                if (mYear != mEndYear - 1) {
                    return false;
                }
                if (monthDiff + 12.0f >= 1.0f
                        && (monthDiff + 12.0f != 1.0f || mIsNextMonthLeap)) {
                    return false;
                }
            }
            return true;
        } else {
            return (mYear == mEndYear && mMonth == mEndMonth - 1)
                    || (mYear == mEndYear - 1 && mMonth == Calendar.DECEMBER
                        && mEndMonth == Calendar.JANUARY);
        }
    }

    private static boolean isValidDayOfWeek(int day) {
        return day >= Calendar.SUNDAY && day <= Calendar.SATURDAY;
    }

    private static boolean isValidMonth(int month) {
        return month >= Calendar.JANUARY && month <= Calendar.DECEMBER;
    }

    private int findDayOffset() {
        return (mDayOfWeekStart < mWeekStart ?
                mDayOfWeekStart + mNumDays : mDayOfWeekStart) - mWeekStart;
    }

    private int getDayFromLocation(float x, float y) {
        final int dayStart = mPadding;
        if (mIsRTL) {
            x = mCalendarWidth - x;
        }
        if (x < dayStart) {
            return -1;
        }
        if (x > mPadding + mCalendarWidth) {
            return -1;
        }

        final int row = ((int) y) / mWeekHeight;
        final int column = (int) (((x - dayStart) * mNumDays) / mCalendarWidth);
        final int day = (column - findDayOffset()) + 1;
        return day + (mNumDays * row);
    }

    private void onDayClick(int year, int month, int day) {
        if (mOnDayClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            mOnDayClickListener.onDayClick(this, year, month, day);
        }
        mTouchHelper.sendEventForVirtualView(day + findDayOffset(),
                AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    private void onDeactivatedDayClick(int year, int month, int day, boolean isPrevMonth) {
        if (!mIsLunar) {
            mTempDate.clear();
            mTempDate.set(year, month, day);
            if (isPrevMonth) {
                Calendar minDate = Calendar.getInstance();
                minDate.clear();
                minDate.set(mMinDate.get(Calendar.YEAR),
                        mMinDate.get(Calendar.MONTH),
                        mMinDate.get(Calendar.DAY_OF_MONTH));
                if (mTempDate.before(minDate)) {
                    return;
                }
            } else if (mTempDate.after(mMaxDate)) {
                return;
            }
        }
        if (mOnDeactivatedDayClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            mOnDeactivatedDayClickListener.onDeactivatedDayClick(this,
                    year, month, day, mIsLeapMonth, isPrevMonth);
        }
        mTouchHelper.sendEventForVirtualView(day, AccessibilityEvent.TYPE_VIEW_CLICKED);
    }

    void clearAccessibilityFocus() {
        mTouchHelper.clearFocusedVirtualView();
    }

    private class MonthViewTouchHelper extends ExploreByTouchHelper {
        private final Rect mTempRect = new Rect();
        private final Calendar mTempCalendar = Calendar.getInstance();

        public MonthViewTouchHelper(View host) {
            super(host);
        }

        public void setFocusedVirtualView(int virtualViewId) {
            getAccessibilityNodeProvider(SeslSimpleMonthView.this)
                    .performAction(virtualViewId,
                            AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
        }

        public void clearFocusedVirtualView() {
            final int focusedVirtualView = getFocusedVirtualView();
            if (focusedVirtualView != ExploreByTouchHelper.INVALID_ID) {
                getAccessibilityNodeProvider(SeslSimpleMonthView.this)
                        .performAction(focusedVirtualView,
                                AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
            }
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            final int day = getDayFromLocation(x, y);
            if (mIsFirstMonth && day < mEnabledDayStart) {
                return ExploreByTouchHelper.INVALID_ID;
            }
            if (!mIsLastMonth || day <= mEnabledDayEnd) {
                return day + findDayOffset();
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            final int dayOffset = findDayOffset();

            for (int viewId = 1; viewId <= MAX_MONTH_VIEW_ID; viewId++) {
                final int day = viewId - dayOffset;
                if ((!mIsFirstMonth || day >= mEnabledDayStart)
                        && (!mIsLastMonth || day <= mEnabledDayEnd)) {
                    virtualViewIds.add(viewId);
                }
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            virtualViewId -= findDayOffset();
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                mLastAccessibilityFocusedView = virtualViewId;
                mLostAccessibilityFocus = false;
            }
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED) {
                mLastAccessibilityFocusedView = View.NO_ID;
                mLostAccessibilityFocus = true;
            }
            event.setContentDescription(getItemDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            virtualViewId -= findDayOffset();
            getItemBounds(virtualViewId, mTempRect);
            node.setContentDescription(getItemDescription(virtualViewId));
            node.setBoundsInParent(mTempRect);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (mSelectedDay != View.NO_ID && virtualViewId == mSelectedDay) {
                node.addAction(AccessibilityNodeInfo.ACTION_SELECT);
                node.setClickable(true);
                node.setCheckable(true);
                node.setChecked(true);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (action != AccessibilityNodeInfo.ACTION_CLICK) {
                return false;
            }

            virtualViewId -= findDayOffset();
            if ((mIsFirstMonth && virtualViewId < mEnabledDayStart)
                    || (mIsLastMonth && virtualViewId > mEnabledDayEnd)) {
                return true;
            }

            if (virtualViewId <= 0) {
                if (mIsLunar) {
                    final int month = mMonth - (mIsLeapMonth ? 0 : 1);
                    if (month < 0) {
                        final int prevMonthLastDay = getDaysInMonthLunar(Calendar.DECEMBER, mYear - 1, mIsLeapMonth);
                        onDeactivatedDayClick(mYear - 1, month, prevMonthLastDay + virtualViewId, true);
                    } else {
                        final int prevMonthLastDay = getDaysInMonthLunar(month, mYear, mIsLeapMonth);
                        onDeactivatedDayClick(mYear, month, prevMonthLastDay + virtualViewId, true);
                    }
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.clear();
                    calendar.set(mYear, mMonth, 1);
                    calendar.add(Calendar.DAY_OF_MONTH, virtualViewId - 1);
                    onDeactivatedDayClick(calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            true);
                }
            } else if (virtualViewId <= mNumCells) {
                onDayClick(mYear, mMonth, virtualViewId);
            } else if (mIsLunar) {
                final int month = mMonth + 1;
                if (month > Calendar.DECEMBER) {
                    onDeactivatedDayClick(mYear + 1, 0, virtualViewId - mNumCells, false);
                } else {
                    onDeactivatedDayClick(mYear, month, virtualViewId - mNumCells, false);
                }
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.clear();
                calendar.set(mYear, mMonth, mNumCells);
                calendar.add(Calendar.DAY_OF_MONTH, virtualViewId - mNumCells);
                onDeactivatedDayClick(calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                        false);
            }

            return true;
        }

        private void getItemBounds(int day, Rect rect) {
            final int offsetX = mPadding;
            final int offsetY = (int) (mContext.getResources()
                    .getDisplayMetrics().density * (-1.0f));

            final int cellHeight = mWeekHeight;
            final int cellWidth = mCalendarWidth / mNumDays;

            final int index = (day - 1) + findDayOffset();
            final int row = index / mNumDays;
            final int column = index % mNumDays;

            final int x = offsetX + (column * cellWidth);
            final int y = offsetY + (row * cellHeight);
            rect.set(x, y, x + cellWidth, y + cellHeight);
        }

        private CharSequence getItemDescription(int day) {
            mTempCalendar.set(mYear, mMonth, day);

            String date = DateUtils.formatDateTime(mContext, mTempCalendar.getTimeInMillis(), 22);
            if (!mIsLunar || mPathClassLoader == null) {
                return date;
            }

            int year = mYear;
            int month = mMonth;
            boolean isLeapMonth = mIsLeapMonth;
            if (day <= 0) {
                month = mMonth - (!mIsLeapMonth ? 1 : 0);
                isLeapMonth = mIsPrevMonthLeap;
                if (month < 0) {
                    year--;
                    month = 11;
                }
                day += getDaysInMonthLunar(month, year, isLeapMonth);
            } else if (day > mNumCells) {
                month = mMonth + (!mIsNextMonthLeap ? 1 : 0);
                isLeapMonth = mIsNextMonthLeap;
                if (month > 11) {
                    year++;
                    month = 0;
                }
                day -= mNumCells;
            }

            SeslSolarLunarConverterReflector.convertLunarToSolar(mPathClassLoader,
                    mSolarLunarConverter, year, month, day, isLeapMonth);
            final int lunarYear = SeslSolarLunarConverterReflector
                    .getYear(mPathClassLoader, mSolarLunarConverter);
            final int lunarMonth = SeslSolarLunarConverterReflector
                    .getMonth(mPathClassLoader, mSolarLunarConverter);
            final int lunarDay = SeslSolarLunarConverterReflector
                    .getDay(mPathClassLoader, mSolarLunarConverter);

            Calendar calendar = Calendar.getInstance();
            calendar.set(lunarYear, lunarMonth, lunarDay);

            return SeslLunarDateUtilsReflector
                    .buildLunarDateString(mPathClassLoader, calendar, getContext());
        }
    }

    int getWeekStart() {
        return mWeekStart;
    }

    int getDayOfWeekStart() {
        return mDayOfWeekStart - (mWeekStart - 1);
    }

    int getNumDays() {
        return mNumDays;
    }

    void setStartDate(Calendar startDate, int isLeapMonth) {
        mStartYear = startDate.get(Calendar.YEAR);
        mStartMonth = startDate.get(Calendar.MONTH);
        mStartDay = startDate.get(Calendar.DAY_OF_MONTH);
        mIsLeapStartMonth = isLeapMonth;
    }

    void setEndDate(Calendar endDate, int isLeapMonth) {
        mEndYear = endDate.get(Calendar.YEAR);
        mEndMonth = endDate.get(Calendar.MONTH);
        mEndDay = endDate.get(Calendar.DAY_OF_MONTH);
        mIsLeapEndMonth = isLeapMonth;
    }

    private boolean isRTL() {
        Locale defLocale = Locale.getDefault();

        if ("ur".equals(defLocale.getLanguage())) {
            return false;
        }

        final byte defDirectionality = Character
                .getDirectionality(defLocale.getDisplayName(defLocale).charAt(0));
        return defDirectionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || defDirectionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    void setLunar(boolean isLunar, boolean isLeapMonth, PathClassLoader pathClassLoader) {
        mIsLunar = isLunar;
        mIsLeapMonth = isLeapMonth;
        if (isLunar && mSolarLunarConverter == null) {
            mPathClassLoader = pathClassLoader;
            mSolarLunarConverter = SeslFeatureReflector.getSolarLunarConverter(pathClassLoader);
        }
    }

    void setFirstMonth() {
        mIsFirstMonth = true;
    }

    void setLastMonth() {
        mIsLastMonth = true;
    }

    void setPrevMonthLeap() {
        mIsPrevMonthLeap = true;
    }

    void setNextMonthLeap() {
        mIsNextMonthLeap = true;
    }

    private boolean isHighContrastFontEnabled() {
        return SeslViewReflector.isHighContrastTextEnabled(this);
    }
}
