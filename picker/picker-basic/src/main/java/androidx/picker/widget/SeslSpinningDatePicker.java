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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.picker.R;
import androidx.reflect.feature.SeslCscFeatureReflector;
import androidx.reflect.feature.SeslFloatingFeatureReflector;
import androidx.reflect.lunarcalendar.SeslFeatureReflector;
import androidx.reflect.lunarcalendar.SeslSolarLunarConverterReflector;
import androidx.reflect.lunarcalendar.SeslSolarLunarTablesReflector;
import androidx.reflect.os.SeslSystemPropertiesReflector;
import androidx.reflect.view.SeslViewReflector;
import androidx.reflect.widget.SeslHoverPopupWindowReflector;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslSpinningDatePicker extends LinearLayout
        implements SeslSimpleMonthView.OnDayClickListener,
        View.OnClickListener,
        View.OnLongClickListener,
        SeslSimpleMonthView.OnDeactivatedDayClickListener {
    private static final String TAG = "SeslSpinningDatePicker";
    private static final boolean SESL_DEBUG = false;

    private static final String TAG_CSCFEATURE_CALENDAR_SETCOLOROFDAYS = "CscFeature_Calendar_SetColorOfDays";

    private static final int UAE_MCC = 424;
    private static final String UAE_SALES_CODE = "XSG";
    private static final String UAE_WEEK_DAY_STRING_FEATURE = "XXXXXBR";

    private static final int USE_LOCALE = 0;

    private static final int LAYOUT_MODE_DEFAULT = 0;
    private static final int LAYOUT_MODE_PHONE = 1;
    private static final int LAYOUT_MODE_MULTIPANE = 2;

    public static final int VIEW_TYPE_SPINNER = 0;
    public static final int VIEW_TYPE_CALENDAR = 1;

    public static final int DATE_MODE_NONE = 0;
    public static final int DATE_MODE_START = 1;
    public static final int DATE_MODE_END = 2;
    public static final int DATE_MODE_WEEK_SELECT = 3;

    private static final int DEFAULT_START_YEAR = 1902;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int DEFAULT_MONTH_PER_YEAR = 12;

    private static final int NOT_LEAP_MONTH = 0;
    private static final int LEAP_MONTH = 1;

    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;

    private static final int SIZE_UNSPECIFIED = -1;
    private static final float MAX_FONT_SCALE = 1.2f;

    private static final int MESSAGE_CALENDAR_HEADER_TEXT_VALUE_SET = 1000;
    private static final int MESSAGE_CALENDAR_HEADER_MONTH_BUTTON_SET = 1001;

    private RelativeLayout mCalendarHeader;
    private RelativeLayout mCalendarHeaderLayout;
    private TextView mCalendarHeaderText;
    private CalendarPagerAdapter mCalendarPagerAdapter;
    private LinearLayout mCalendarViewLayout;
    private ViewPager mCalendarViewPager;
    private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;
    private FrameLayout mContentFrame;
    private Context mContext;
    private Calendar mCurrentDate;
    private Locale mCurrentLocale;
    private RelativeLayout mCustomButtonLayout;
    private View mCustomButtonView;
    private SeslSpinningDatePickerSpinner mDatePickerSpinner;
    private LinearLayout mDateTimePickerLayout;
    private SimpleDateFormat mDayFormatter;
    private LinearLayout mDayOfTheWeekLayout;
    private DayOfTheWeekView mDayOfTheWeekView;
    private View mEmptySpaceLeft;
    private View mEmptySpaceRight;
    private Calendar mEndDate;
    private View mFirstBlankSpace;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private String mMonthViewColor = null;
    private ImageButton mNextButton;
    private OnDateChangedListener mOnDateChangedListener;
    private OnViewTypeChangedListener mOnViewTypeChangedListener;
    private static PackageManager mPackageManager;
    PathClassLoader mPathClassLoader = null;
    private LinearLayout mPickerView;
    private ImageButton mPrevButton;
    private View mSecondBlankSpace;
    private Object mSolarLunarConverter;
    private Object mSolarLunarTables;
    private Calendar mStartDate;
    private Calendar mTempDate;
    private Calendar mTempMinMaxDate;
    private ValidationCallback mValidationCallback;
    private ViewAnimator mViewAnimator;

    private int mBackgroundBorderlessResId = -1;
    private int mCalendarHeaderLayoutHeight;
    private int mCalendarViewMargin;
    private int mCalendarViewPagerHeight;
    private int mCalendarViewPagerWidth;
    private int mCurrentPosition;
    private int mCurrentViewType = -1;
    private int mDatePickerHeight;
    private int mDayOfTheWeekLayoutHeight;
    private int mDayOfTheWeekLayoutWidth;
    private int mDayOfWeekStart;
    private int mFirstBlankSpaceHeight;
    private int mFirstDayOfWeek = 0;
    private int mIsLeapEndMonth;
    private int mIsLeapStartMonth;
    private int mLayoutMode;
    private int mLunarCurrentDay;
    private int mLunarCurrentMonth;
    private int mLunarCurrentYear;
    private int mLunarEndDay;
    private int mLunarEndMonth;
    private int mLunarEndYear;
    private int mLunarStartDay;
    private int mLunarStartMonth;
    private int mLunarStartYear;
    private int mMeasureSpecHeight;
    private int mMode = DATE_MODE_NONE;
    private int mNumDays;
    private int mOldCalendarViewPagerWidth;
    private int mOldSelectedDay = -1;
    private int mPadding = 0;
    private int mPickerViewHeight;
    private int mPositionCount;
    private int mSecondBlankSpaceHeight;
    private int[] mTotalMonthCountWithLeap;
    private int mWeekStart;

    private boolean mIs24HourView;
    private boolean mIsCalledFromDeactivatedDayClick;
    private boolean mIsConfigurationChanged = false;
    private boolean mIsCustomButtonSeparate = false;
    private boolean mIsEnabled = true;
    private boolean mIsFarsiLanguage;
    private boolean mIsFirstMeasure = true;
    private boolean mIsFromSetLunar = false;
    private boolean mIsHeightSetForDialog;
    private boolean mIsInDialog;
    private boolean mIsLeapMonth = false;
    private boolean mIsLunar = false;
    private boolean mIsLunarSupported = false;
    private boolean mIsMarginRightShown;
    private boolean mIsRTL;
    private boolean mIsSimplifiedChinese;
    private boolean mIsTibetanLanguage;
    private boolean mIsWeekRangeSet;
    private boolean mLunarChanged = false;
    private boolean mSupportShortSpinnerHeight;

    private final View.OnFocusChangeListener mBtnFocusChangeListener
            = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                removeAllCallbacks();
            }
        }
    };

    private View.OnClickListener mCalendarHeaderClickListener
            = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setCurrentViewType(VIEW_TYPE_SPINNER);
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_CALENDAR_HEADER_TEXT_VALUE_SET:
                    if (mTempDate.get(Calendar.YEAR) <= getMaxYear()
                            && mTempDate.get(Calendar.YEAR) >= getMinYear()) {
                        String monthAndYearString = getMonthAndYearString(mTempDate);
                        mCalendarHeaderText.setText(monthAndYearString);
                        mCalendarHeaderText.setContentDescription(monthAndYearString
                                + ", "
                                + mContext.getString(mCurrentViewType == VIEW_TYPE_CALENDAR ?
                                R.string.sesl_date_picker_switch_to_wheel_description
                                : R.string.sesl_date_picker_switch_to_calendar_description));
                    }
                    break;
                case MESSAGE_CALENDAR_HEADER_MONTH_BUTTON_SET:
                    if (mCurrentViewType == VIEW_TYPE_SPINNER) {
                        setPrevButtonProperties(0.0f, false);
                        setNextButtonProperties(0.0f, false);
                        final int TYPE_NONE = SeslHoverPopupWindowReflector.getField_TYPE_NONE();
                        if (TYPE_NONE != -1) {
                            SeslViewReflector.semSetHoverPopupType(mPrevButton, TYPE_NONE);
                            SeslViewReflector.semSetHoverPopupType(mNextButton, TYPE_NONE);
                        }
                    } else {
                        final int TYPE_TOOLTIP = SeslHoverPopupWindowReflector.getField_TYPE_TOOLTIP();
                        if (TYPE_TOOLTIP != -1) {
                            SeslViewReflector.semSetHoverPopupType(mPrevButton, TYPE_TOOLTIP);
                            SeslViewReflector.semSetHoverPopupType(mNextButton, TYPE_TOOLTIP);
                        }
                        if (mCurrentPosition > 0 && mCurrentPosition < mPositionCount - 1) {
                            setPrevButtonProperties(1.0f, true);
                            setNextButtonProperties(1.0f, true);
                        } else if (mPositionCount == 1) {
                            setPrevButtonProperties(0.4f, false);
                            setNextButtonProperties(0.4f, false);
                            removeAllCallbacks();
                        } else if (mCurrentPosition == 0) {
                            setPrevButtonProperties(0.4f, false);
                            setNextButtonProperties(1.0f, true);
                            removeAllCallbacks();
                        } else if (mCurrentPosition == mPositionCount - 1) {
                            setPrevButtonProperties(1.0f, true);
                            setNextButtonProperties(0.4f, false);
                            removeAllCallbacks();
                        }
                    }
                    break;
            }
        }
    };

    private View.OnKeyListener mMonthBtnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (mIsRTL) {
                mIsConfigurationChanged = false;
            }
            if (event.getAction() == KeyEvent.ACTION_UP
                    || event.getAction() == 3 /*?*/) {
                removeAllCallbacks();
            }
            return false;
        }
    };

    private View.OnTouchListener mMonthBtnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                removeAllCallbacks();
            }
            return false;
        }
    };

    private SeslSpinningDatePickerSpinner.OnSpinnerDateClickListener mOnSpinnerDateClickListener
            = new SeslSpinningDatePickerSpinner.OnSpinnerDateClickListener() {
        @Override
        public void onSpinnerDateClicked(Calendar calendar, LunarDate lunarDate) {
            clearCalendar(mCurrentDate,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            if (lunarDate != null) {
                mLunarCurrentYear = lunarDate.year;
                mLunarCurrentMonth = lunarDate.month;
                mLunarCurrentDay = lunarDate.day;
                mIsLeapMonth = lunarDate.isLeapMonth;
            }
            setCurrentViewType(VIEW_TYPE_CALENDAR);
        }
    };

    public interface OnDateChangedListener {
        void onDateChanged(SeslSpinningDatePicker view, int year, int month, int day);
    }

    public interface OnViewTypeChangedListener {
        void onViewTypeChanged(SeslSpinningDatePicker view);
    }

    private interface ValidationCallback {
        void onValidationChanged(boolean valid);
    }

    private void setPrevButtonProperties(float alpha, boolean enabled) {
        mPrevButton.setAlpha(alpha);
        if (enabled) {
            mPrevButton.setBackgroundResource(mBackgroundBorderlessResId);
            mPrevButton.setEnabled(true);
            mPrevButton.setFocusable(true);
        } else {
            mPrevButton.setBackground(null);
            mPrevButton.setEnabled(false);
            mPrevButton.setFocusable(false);
        }
    }

    private void setNextButtonProperties(float alpha, boolean enabled) {
        mNextButton.setAlpha(alpha);
        if (enabled) {
            mNextButton.setBackgroundResource(mBackgroundBorderlessResId);
            mNextButton.setEnabled(true);
            mNextButton.setFocusable(true);
        } else {
            mNextButton.setBackground(null);
            mNextButton.setEnabled(false);
            mNextButton.setFocusable(false);
        }
    }

    public SeslSpinningDatePicker(Context context) {
        this(context, null);
    }

    public SeslSpinningDatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.datePickerStyle);
    }

    public SeslSpinningDatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslSpinningDatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        mCurrentLocale = Locale.getDefault();
        mIsRTL = isRTL();
        mIsTibetanLanguage = isTibetanLanguage();
        mIsFarsiLanguage = isFarsiLanguage();
        mIsSimplifiedChinese = isSimplifiedChinese();
        if (mIsSimplifiedChinese) {
            mDayFormatter = new SimpleDateFormat("EEEEE", mCurrentLocale);
        } else {
            mDayFormatter = new SimpleDateFormat("EEE", mCurrentLocale);
        }
        mMinDate = getCalendarForLocale(mMinDate, mCurrentLocale);
        mMaxDate = getCalendarForLocale(mMaxDate, mCurrentLocale);
        mTempMinMaxDate = getCalendarForLocale(mMaxDate, mCurrentLocale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, mCurrentLocale);
        mTempDate = getCalendarForLocale(mCurrentDate, mCurrentLocale);

        final Resources res = getResources();

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.DatePicker, defStyleAttr, defStyleRes);
        mMinDate.set(a.getInt(R.styleable.DatePicker_android_startYear, DEFAULT_START_YEAR),
                Calendar.JANUARY, 1);
        mMaxDate.set(a.getInt(R.styleable.DatePicker_android_endYear, DEFAULT_END_YEAR),
                Calendar.DECEMBER, 31);
        mLayoutMode = a.getInt(R.styleable.DatePicker_pickerLayoutMode, LAYOUT_MODE_DEFAULT);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        switch (mLayoutMode) {
            case LAYOUT_MODE_PHONE:
                inflater.inflate(Build.VERSION.SDK_INT >= 23 ?
                                R.layout.sesl_spinning_date_picker_phone : R.layout.sesl_spinning_date_picker_legacy_phone,
                        this, true);
                break;
            case LAYOUT_MODE_MULTIPANE:
                inflater.inflate(Build.VERSION.SDK_INT >= 23 ?
                                R.layout.sesl_spinning_date_picker_multipane : R.layout.sesl_spinning_date_picker_legacy_multipane,
                        this, true);
                break;
            case LAYOUT_MODE_DEFAULT:
            default:
                inflater.inflate(Build.VERSION.SDK_INT >= 23 ?
                                R.layout.sesl_spinning_date_picker : R.layout.sesl_spinning_date_picker_legacy,
                        this, true);
                break;
        }

        mCalendarViewLayout = (LinearLayout) inflater.inflate(
                R.layout.sesl_spinning_date_picker_calendar, null, false);

        int firstDayOfWeek = a.getInt(R.styleable.DatePicker_android_firstDayOfWeek, 0);
        if (firstDayOfWeek != 0) {
            setFirstDayOfWeek(firstDayOfWeek);
        }

        a.recycle();

        mMonthViewColor = getMonthViewColorStringForSpecific();

        TypedArray seslArray = mContext.obtainStyledAttributes(attrs, R.styleable.DatePicker,
                defStyleAttr, defStyleRes);
        mDayOfTheWeekView = new DayOfTheWeekView(mContext, seslArray);
        final int dayNumberTextColor = seslArray.getColor(R.styleable.DatePicker_dayNumberTextColor,
                res.getColor(R.color.sesl_date_picker_normal_day_number_text_color_light));
        final int btnTintColor = seslArray.getColor(R.styleable.DatePicker_buttonTintColor,
                res.getColor(R.color.sesl_date_picker_button_tint_color_light));
        seslArray.recycle();

        mCalendarPagerAdapter = new CalendarPagerAdapter();
        mCalendarViewPager = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar);
        mCalendarViewPager.setAdapter(mCalendarPagerAdapter);
        mCalendarViewPager.setOnPageChangeListener(new CalendarPageChangeListener());
        mCalendarViewPager.seslSetSupportedMouseWheelEvent(true);

        mPadding = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_padding);
        mCalendarHeader = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header);

        mCalendarHeaderText = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header_text);
        mCalendarHeaderText.setTextColor(dayNumberTextColor);

        mStartDate = getCalendarForLocale(mCurrentDate, mCurrentLocale);
        mEndDate = getCalendarForLocale(mCurrentDate, mCurrentLocale);

        mPickerView = findViewById(R.id.sesl_spinning_date_picker_view);
        mEmptySpaceLeft = findViewById(R.id.sesl_spinning_date_time_picker_empty_view_left);
        mEmptySpaceRight = findViewById(R.id.sesl_spinning_date_picker_margin_view_center);
        mIsMarginRightShown = false;

        mDatePickerSpinner = findViewById(R.id.sesl_spinning_date_picker_spinner_view);
        mDatePickerSpinner.setOnSpinnerDateClickListener(mOnSpinnerDateClickListener);
        mDatePickerSpinner.setMinValue(mMinDate);
        mDatePickerSpinner.setMaxValue(mMaxDate);
        mDatePickerSpinner.setValue(mCurrentDate);
        mDatePickerSpinner.setOnValueChangedListener(
                new SeslSpinningDatePickerSpinner.OnValueChangeListener() {
                    @Override
                    public void onValueChange(SeslSpinningDatePickerSpinner spinner,
                                              Calendar oldCalendar, Calendar newCalendar,
                                              boolean isLeapMonth, LunarDate lunarDate) {
                        mCurrentDate = (Calendar) newCalendar.clone();
                        
                        int year = newCalendar.get(Calendar.YEAR);
                        int month = newCalendar.get(Calendar.MONTH);
                        int day = newCalendar.get(Calendar.DAY_OF_MONTH);
                        if (lunarDate != null) {
                            year = lunarDate.year;
                            month = lunarDate.month;
                            day = lunarDate.day;
                        }
                        
                        if (mIsLunar) {
                            mLunarCurrentYear = year;
                            mLunarCurrentMonth = month;
                            mLunarCurrentDay = day;
                            mIsLeapMonth = isLeapMonth;
                        }
                        
                        switch (mMode) {
                            case DATE_MODE_START:
                                clearCalendar(mStartDate, year, month, day);
                                if (mIsLunar) {
                                    mLunarStartYear = year;
                                    mLunarStartMonth = month;
                                    mLunarStartDay = day;
                                    mIsLeapStartMonth = isLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                                }
                                break;
                            case DATE_MODE_END:
                                clearCalendar(mEndDate, year, month, day);
                                if (mIsLunar) {
                                    mLunarEndYear = year;
                                    mLunarEndMonth = month;
                                    mLunarEndDay = day;
                                    mIsLeapEndMonth = isLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                                }
                                break;
                            default:
                                clearCalendar(mStartDate, year, month, day);
                                clearCalendar(mEndDate, year, month, day);
                                if (mIsLunar) {
                                    mLunarStartYear = year;
                                    mLunarStartMonth = month;
                                    mLunarStartDay = day;
                                    mIsLeapStartMonth = isLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                                    mLunarEndYear = year;
                                    mLunarEndMonth = month;
                                    mLunarEndDay = day;
                                    mIsLeapEndMonth = isLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                                    mDatePickerSpinner.setLunar(mIsLunar, mIsLeapMonth);
                                }
                                break;
                        }

                        onValidationChanged(!mStartDate.after(mEndDate));
                        updateSimpleMonthView(false);
                        if (mMode == DATE_MODE_WEEK_SELECT && mIsWeekRangeSet) {
                            updateStartEndDateRange(getDayOffset(), year, month, day);
                        }
                        SeslSpinningDatePicker.this.onDateChanged();
                    }
                });

        mViewAnimator = findViewById(R.id.sesl_spinning_date_picker_view_animator);
        mViewAnimator.addView(mCalendarViewLayout, 1,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mCurrentViewType = VIEW_TYPE_SPINNER;
        mCalendarHeaderText.setOnClickListener(mCalendarHeaderClickListener);

        mDayOfTheWeekLayoutHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_day_height);
        checkMaxFontSize();
        mCalendarViewPagerWidth = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_width);
        mCalendarViewMargin = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_margin);
        mDayOfTheWeekLayoutWidth = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_width);

        mDayOfTheWeekLayout = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_day_of_the_week);
        mDayOfTheWeekLayout.addView(mDayOfTheWeekView);
        mDateTimePickerLayout = findViewById(R.id.sesl_spinning_date_time_picker_layout);
        mCalendarHeaderLayout = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header_layout);
        if (mIsRTL) {
            mPrevButton = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header_next_button);
            mNextButton = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header_prev_button);
            mPrevButton.setContentDescription(mContext.getString(R.string.sesl_date_picker_decrement_month));
            mNextButton.setContentDescription(mContext.getString(R.string.sesl_date_picker_increment_month));
        } else {
            mPrevButton = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header_prev_button);
            mNextButton = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_calendar_header_next_button);
        }
        mPrevButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPrevButton.setOnLongClickListener(this);
        mNextButton.setOnLongClickListener(this);
        mPrevButton.setOnTouchListener(mMonthBtnTouchListener);
        mNextButton.setOnTouchListener(mMonthBtnTouchListener);
        mPrevButton.setOnKeyListener(mMonthBtnKeyListener);
        mNextButton.setOnKeyListener(mMonthBtnKeyListener);
        mPrevButton.setOnFocusChangeListener(mBtnFocusChangeListener);
        mNextButton.setOnFocusChangeListener(mBtnFocusChangeListener);
        mPrevButton.setColorFilter(btnTintColor);
        mNextButton.setColorFilter(btnTintColor);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,
                outValue, true);
        mBackgroundBorderlessResId = outValue.resourceId;

        mCalendarHeaderLayoutHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_header_height);
        mCalendarViewPagerHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_height);
        mOldCalendarViewPagerWidth = mCalendarViewPagerWidth;
        mPickerViewHeight = res.getDimensionPixelOffset(R.dimen.sesl_spinning_date_picker_height);
        mCalendarHeaderText.setFocusable(true);

        mPrevButton.setNextFocusRightId(R.id.sesl_date_picker_calendar_header_text);
        mNextButton.setNextFocusLeftId(R.id.sesl_date_picker_calendar_header_text);
        mCalendarHeaderText.setNextFocusRightId(R.id.sesl_date_picker_calendar_header_next_button);
        mCalendarHeaderText.setNextFocusLeftId(R.id.sesl_date_picker_calendar_header_prev_button);

        mFirstBlankSpace = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_between_header_and_weekend);
        mFirstBlankSpaceHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_gap_between_header_and_weekend);
        mSecondBlankSpace = mCalendarViewLayout.findViewById(R.id.sesl_date_picker_between_weekend_and_calender);
        mSecondBlankSpaceHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_gap_between_weekend_and_calender);

        mDatePickerHeight = mCalendarHeaderLayoutHeight + mFirstBlankSpaceHeight
                + mDayOfTheWeekLayoutHeight + mSecondBlankSpaceHeight + mCalendarViewPagerHeight;

        updateSimpleMonthView(true);

        TypedValue outValue2 = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.windowIsFloating,
                outValue2, true);
        mIsInDialog = outValue2.data != 0;

        Activity activity = scanForActivity(mContext);
        if (activity != null && !mIsInDialog) {
            mContentFrame = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        } else if (activity == null) {
            Log.e(TAG, "Cannot get window of this context. context:" + mContext);
        }
    }

    public void setIs24HourView(@NonNull Boolean is24HourView) {
        if (is24HourView == null) {
            return;
        }

        mIs24HourView = is24HourView;
        mEmptySpaceLeft.setVisibility(is24HourView && !mIsMarginRightShown ?
                View.VISIBLE : View.GONE);
    }

    public void showMarginRight(@NonNull Boolean show) {
        mIsMarginRightShown = show;
        mEmptySpaceRight.setVisibility(show ? View.VISIBLE : View.GONE);
        mEmptySpaceLeft.setVisibility(mIs24HourView && !mIsMarginRightShown ?
                View.VISIBLE : View.GONE);
    }

    public void setViewAnimatorForCalendarView(ViewAnimator viewAnimator) {
        mViewAnimator.removeViewAt(1);
        mViewAnimator = viewAnimator;
        mViewAnimator.addView(mCalendarViewLayout, 1,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @RestrictTo(LIBRARY)
    void onValidationChanged(boolean valid) {
        if (mValidationCallback != null) {
            mValidationCallback.onValidationChanged(valid);
        }
    }

    public boolean getWrapSelectorWheel() {
        return mDatePickerSpinner.getWrapSelectorWheel();
    }

    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mDatePickerSpinner.setWrapSelectorWheel(wrapSelectorWheel);
    }

    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        Calendar newCalendar = Calendar.getInstance(locale);
        if (oldCalendar != null) {
            newCalendar.setTimeInMillis(oldCalendar.getTimeInMillis());
        }
        newCalendar.set(Calendar.HOUR_OF_DAY, 12);
        newCalendar.set(Calendar.MINUTE, 0);
        newCalendar.set(Calendar.SECOND, 0);
        newCalendar.set(Calendar.MILLISECOND, 0);
        return newCalendar;
    }

    public void setOnViewTypeChangedListener(OnViewTypeChangedListener listener) {
        mOnViewTypeChangedListener = listener;
    }

    public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener onDateChangedListener) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, monthOfYear);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        if (mIsLunar) {
            mLunarCurrentYear = year;
            mLunarCurrentMonth = monthOfYear;
            mLunarCurrentDay = dayOfMonth;
        }

        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate = getCalendarForLocale(mMinDate, mCurrentLocale);
        }
        if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate = getCalendarForLocale(mMaxDate, mCurrentLocale);
        }

        if (mIsLunar) {
            mDatePickerSpinner.updateDate(year, monthOfYear, dayOfMonth);
        } else {
            mDatePickerSpinner.setValue(mCurrentDate);
        }

        mOnDateChangedListener = onDateChangedListener;

        updateSimpleMonthView(false);
        onDateChanged();

        mDatePickerSpinner.setMinValue(mMinDate);
        mDatePickerSpinner.setMaxValue(mMaxDate);

        if (mCurrentViewType == VIEW_TYPE_SPINNER) {
            mCalendarViewLayout.setVisibility(GONE);
            mCalendarViewLayout.setEnabled(false);
        }

        clearCalendar(mStartDate, year, monthOfYear, dayOfMonth);
        clearCalendar(mEndDate, year, monthOfYear, dayOfMonth);

        if (mIsLunar) {
            mLunarStartYear = year;
            mLunarStartMonth = monthOfYear;
            mLunarStartDay = dayOfMonth;
            mLunarEndYear = year;
            mLunarEndMonth = monthOfYear;
            mLunarEndDay = dayOfMonth;
        }
    }

    private void clearCalendar(Calendar calendar, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    public void updateDate(int year, int month, int dayOfMonth) {
        mTempDate.set(Calendar.YEAR, year);
        mTempDate.set(Calendar.MONTH, month);
        mTempDate.set(Calendar.DAY_OF_MONTH, dayOfMonth > mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH) ?
                mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH) : dayOfMonth);
        mCurrentDate = getCalendarForLocale(mTempDate, mCurrentLocale);
        if (mIsLunar) {
            mLunarCurrentYear = year;
            mLunarCurrentMonth = month;
            mLunarCurrentDay = dayOfMonth;
        }

        switch (mMode) {
            case DATE_MODE_START:
                clearCalendar(mStartDate, year, month, dayOfMonth);
                if (mIsLunar) {
                    mLunarStartYear = year;
                    mLunarStartMonth = month;
                    mLunarStartDay = dayOfMonth;
                }
                break;
            case DATE_MODE_END:
                clearCalendar(mEndDate, year, month, dayOfMonth);
                if (mIsLunar) {
                    mLunarEndYear = year;
                    mLunarEndMonth = month;
                    mLunarEndDay = dayOfMonth;
                }
                break;
            default:
                clearCalendar(mEndDate, year, month, dayOfMonth);
                if (mIsLunar) {
                    mLunarEndYear = year;
                    mLunarEndMonth = month;
                    mLunarEndDay = dayOfMonth;
                }
                break;
        }

        updateSimpleMonthView(true);
        onDateChanged();

        SeslSimpleMonthView view = mCalendarPagerAdapter.views.get(mCurrentPosition);
        if (view != null) {
            final int enabledDayRangeStart = (getMinMonth() == month && getMinYear() == year) ?
                    getMinDay() : 1;
            final int enabledDayRangeEnd = (getMaxMonth() == month && getMaxYear() == year) ?
                    getMaxDay() : 31;
            if (mIsLunarSupported) {
                view.setLunar(mIsLunar, mIsLeapMonth, mPathClassLoader);
            }
            if (mMode == DATE_MODE_WEEK_SELECT && mIsWeekRangeSet) {
                updateStartEndDateRange(getDayOffset(), year, month, dayOfMonth);
            }

            int startYear, startMonth, startDay, endYear, endMonth, endDay;
            if (mIsLunar) {
                startYear = mLunarStartYear;
                startMonth = mLunarStartMonth;
                startDay = mLunarStartDay;
                endYear = mLunarEndYear;
                endMonth = mLunarEndMonth;
                endDay = mLunarEndDay;
            } else {
                startYear = mStartDate.get(Calendar.YEAR);
                startMonth = mStartDate.get(Calendar.MONTH);
                startDay = mStartDate.get(Calendar.DAY_OF_MONTH);
                endYear = mEndDate.get(Calendar.YEAR);
                endMonth = mEndDate.get(Calendar.MONTH);
                endDay = mEndDate.get(Calendar.DAY_OF_MONTH);
            }
            view.setMonthParams(dayOfMonth, month, year,
                    getFirstDayOfWeek(), enabledDayRangeStart, enabledDayRangeEnd,
                    mMinDate, mMaxDate, startYear, startMonth, startDay, mIsLeapStartMonth,
                    endYear, endMonth, endDay, mIsLeapEndMonth, mMode);
            view.invalidate();

            if (!mIsLunar) {
                final int prevPos = mCurrentPosition - 1;
                if (prevPos >= 0) {
                    SeslSimpleMonthView prevMonth = mCalendarPagerAdapter.views.get(prevPos);
                    if (prevMonth != null) {
                        prevMonth.setStartDate(mStartDate, mIsLeapStartMonth);
                        prevMonth.setEndDate(mEndDate, mIsLeapEndMonth);
                    }
                }

                final int nextPos = mCurrentPosition + 1;
                if (nextPos < mPositionCount) {
                    SeslSimpleMonthView nextMonth = mCalendarPagerAdapter.views.get(nextPos);
                    if (nextMonth != null) {
                        nextMonth.setStartDate(mStartDate, mIsLeapStartMonth);
                        nextMonth.setEndDate(mEndDate, mIsLeapEndMonth);
                    }
                }
            }
        }

        if (mDatePickerSpinner != null) {
            mDatePickerSpinner.updateDate(year, month, dayOfMonth);
        }
    }

    // TODO rework this method
    private void updateStartEndDateRange(int weekEnd, int year, int monthOfYear, int dayOfMonth) {
        clearCalendar(mStartDate, year, monthOfYear, (dayOfMonth - weekEnd) + 1);
        int i = 7 - weekEnd;
        clearCalendar(mEndDate, year, monthOfYear, dayOfMonth + i);
        if (mIsLunar) {
            Calendar lunarToSolarCalendar = convertLunarToSolar(getCalendarForLocale(null, mCurrentLocale), year, monthOfYear, dayOfMonth);
            Calendar calendar = (Calendar) lunarToSolarCalendar.clone();
            calendar.add(Calendar.DAY_OF_YEAR, (-weekEnd) + 1);
            LunarDate lunarDate = new LunarDate();
            convertSolarToLunar(calendar, lunarDate);
            mLunarStartYear = lunarDate.year;
            mLunarStartMonth = lunarDate.month;
            mLunarStartDay = lunarDate.day;
            mIsLeapStartMonth = NOT_LEAP_MONTH;
            lunarToSolarCalendar.add(Calendar.DAY_OF_YEAR, i);
            convertSolarToLunar(lunarToSolarCalendar, lunarDate);
            mLunarEndYear = lunarDate.year;
            mLunarEndMonth = lunarDate.month;
            mLunarEndDay = lunarDate.day;
            mIsLeapEndMonth = NOT_LEAP_MONTH;
        }
    }

    private Calendar convertLunarToSolar(Calendar calendar, int year, int monthOfYear, int dayOfMonth) {
        Calendar newCalendar = (Calendar) calendar.clone();
        SeslSolarLunarConverterReflector
                .convertLunarToSolar(mPathClassLoader, mSolarLunarConverter, year, monthOfYear, dayOfMonth, mIsLeapMonth);
        newCalendar.set(SeslSolarLunarConverterReflector.getYear(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getMonth(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getDay(mPathClassLoader, mSolarLunarConverter));
        return newCalendar;
    }

    private Calendar convertSolarToLunar(Calendar calendar, LunarDate lunarDate) {
        Calendar newCalendar = (Calendar) calendar.clone();
        SeslSolarLunarConverterReflector
                .convertSolarToLunar(mPathClassLoader, mSolarLunarConverter,
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        newCalendar.set(SeslSolarLunarConverterReflector.getYear(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getMonth(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getDay(mPathClassLoader, mSolarLunarConverter));
        if (lunarDate != null) {
            lunarDate.day = SeslSolarLunarConverterReflector.getDay(mPathClassLoader, mSolarLunarConverter);
            lunarDate.month = SeslSolarLunarConverterReflector.getMonth(mPathClassLoader, mSolarLunarConverter);
            lunarDate.year = SeslSolarLunarConverterReflector.getYear(mPathClassLoader, mSolarLunarConverter);
            lunarDate.isLeapMonth = SeslSolarLunarConverterReflector.isLeapMonth(mPathClassLoader, mSolarLunarConverter);
        }
        return newCalendar;
    }

    private void onDateChanged() {
        if (mOnDateChangedListener != null) {
            int year = mCurrentDate.get(Calendar.YEAR);
            int monthOfYear = mCurrentDate.get(Calendar.MONTH);
            int dayOfMonth = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            if (mIsLunar) {
                year = mLunarCurrentYear;
                monthOfYear = mLunarCurrentMonth;
                dayOfMonth = mLunarCurrentDay;
            }
            mOnDateChangedListener.onDateChanged(this, year, monthOfYear, dayOfMonth);
        }
    }

    public int getYear() {
        if (mIsLunar) {
            return mLunarCurrentYear;
        }
        return mCurrentDate.get(Calendar.YEAR);
    }

    public int getMonth() {
        if (mIsLunar) {
            return mLunarCurrentMonth;
        }
        return mCurrentDate.get(Calendar.MONTH);
    }

    public int getDayOfMonth() {
        if (mIsLunar) {
            return mLunarCurrentDay;
        }
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    public long getMinDate() {
        return mMinDate.getTimeInMillis();
    }

    public Calendar getMinDateCalendar() {
        return mMinDate;
    }

    public void setMinDate(long minDate) {
        mTempMinMaxDate.setTimeInMillis(minDate);
        mTempMinMaxDate.set(Calendar.HOUR_OF_DAY, 12);
        mTempMinMaxDate.set(Calendar.MINUTE, 0);
        mTempMinMaxDate.set(Calendar.SECOND, 0);
        mTempMinMaxDate.set(Calendar.MILLISECOND, 0);
        if (mTempMinMaxDate.get(Calendar.YEAR) != mMinDate.get(Calendar.YEAR)
                || mTempMinMaxDate.get(Calendar.DAY_OF_YEAR) == mMinDate.get(Calendar.DAY_OF_YEAR)) {
            if (mIsLunar) {
                setTotalMonthCountWithLeap();
            }
            if (mCurrentDate.before(mTempMinMaxDate)) {
                mCurrentDate.setTimeInMillis(minDate);
                mDatePickerSpinner.setValue(mCurrentDate);
                onDateChanged();
            }
            mMinDate.setTimeInMillis(minDate);
            mDatePickerSpinner.setMinValue(mMinDate);
            mCalendarPagerAdapter.notifyDataSetChanged();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateSimpleMonthView(false);
                }
            }, 10);
        }
    }

    public long getMaxDate() {
        return mMaxDate.getTimeInMillis();
    }

    public Calendar getMaxDateCalendar() {
        return mMaxDate;
    }

    public void setMaxDate(long maxDate) {
        mTempMinMaxDate.setTimeInMillis(maxDate);
        mTempMinMaxDate.set(Calendar.HOUR_OF_DAY, 12);
        mTempMinMaxDate.set(Calendar.MINUTE, 0);
        mTempMinMaxDate.set(Calendar.SECOND, 0);
        mTempMinMaxDate.set(Calendar.MILLISECOND, 0);
        if (mTempMinMaxDate.get(Calendar.YEAR) != mMaxDate.get(Calendar.YEAR)
                || mTempMinMaxDate.get(Calendar.DAY_OF_YEAR) == mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            if (mIsLunar) {
                setTotalMonthCountWithLeap();
            }
            if (mCurrentDate.after(mTempMinMaxDate)) {
                mCurrentDate.setTimeInMillis(maxDate);
                onDateChanged();
            }
            mMaxDate.setTimeInMillis(maxDate);
            mDatePickerSpinner.setMaxValue(mMaxDate);
            mCalendarPagerAdapter.notifyDataSetChanged();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateSimpleMonthView(false);
                }
            }, 10);
        }
    }

    public int getMinYear() {
        return mMinDate.get(Calendar.YEAR);
    }

    public int getMaxYear() {
        return mMaxDate.get(Calendar.YEAR);
    }

    public int getMinMonth() {
        return mMinDate.get(Calendar.MONTH);
    }

    public int getMaxMonth() {
        return mMaxDate.get(Calendar.MONTH);
    }

    public int getMinDay() {
        return mMinDate.get(Calendar.DAY_OF_MONTH);
    }

    public int getMaxDay() {
        return mMaxDate.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            mDatePickerSpinner.setEnabled(enabled);
            mIsEnabled = enabled;
        }
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(getFormattedCurrentDate());
        return true;
    }

    private String getFormattedCurrentDate() {
        return DateUtils.formatDateTime(mContext,
                mCurrentDate.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mIsRTL = isRTL();
        mIsFarsiLanguage = isFarsiLanguage();
        mIsTibetanLanguage = isTibetanLanguage();

        Locale newLocale;
        if (Build.VERSION.SDK_INT >= 24) {
            newLocale = newConfig.getLocales().get(USE_LOCALE);
        } else {
            newLocale = newConfig.locale;
        }
        if (!mCurrentLocale.equals(newLocale)) {
            mCurrentLocale = newLocale;
            mIsSimplifiedChinese = isSimplifiedChinese();
            if (mIsSimplifiedChinese) {
                mDayFormatter = new SimpleDateFormat("EEEEE", newLocale);
            } else {
                mDayFormatter = new SimpleDateFormat("EEE", newLocale);
            }
        }

        final Resources res = mContext.getResources();
        mDateTimePickerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mIsFirstMeasure = true;
        mCalendarHeaderLayoutHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_header_height);
        mCalendarViewPagerHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_height);
        mDayOfTheWeekLayoutHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_day_height);
        mFirstBlankSpaceHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_gap_between_header_and_weekend);
        mSecondBlankSpaceHeight = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_gap_between_weekend_and_calender);

        if (mSupportShortSpinnerHeight) {
            mPickerViewHeight = LayoutParams.WRAP_CONTENT;
        } else {
            mPickerViewHeight = res.getDimensionPixelOffset(mIsHeightSetForDialog ?
                    R.dimen.sesl_spinning_date_picker_height_dialog : R.dimen.sesl_spinning_date_picker_height);
        }

        mDatePickerHeight = mCalendarHeaderLayoutHeight + mFirstBlankSpaceHeight
                + mDayOfTheWeekLayoutHeight + mSecondBlankSpaceHeight + mCalendarViewPagerHeight;

        if (mIsRTL) {
            mIsConfigurationChanged = true;
        }

        checkMaxFontSize();
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < 1 || firstDayOfWeek > 7) {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }
        mFirstDayOfWeek = firstDayOfWeek;
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek != 0 ? mFirstDayOfWeek : mCurrentDate.getFirstDayOfWeek();
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        int year = mCurrentDate.get(Calendar.YEAR);
        int month = mCurrentDate.get(Calendar.MONTH);
        int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
        if (mIsLunar) {
            year = mLunarCurrentYear;
            month = mLunarCurrentMonth;
            day = mLunarCurrentDay;
        }
        return new SavedState(superState, year, month, day,
                mMinDate.getTimeInMillis(), mMaxDate.getTimeInMillis(), -1);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(((View.BaseSavedState) state).getSuperState());
        SavedState ss = (SavedState) state;
        mCurrentDate.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
        mDatePickerSpinner.setValue(mCurrentDate);
        if (mIsLunar) {
            mLunarCurrentYear = ss.getSelectedYear();
            mLunarCurrentMonth = ss.getSelectedMonth();
            mLunarCurrentDay = ss.getSelectedDay();
        }
        mMinDate.setTimeInMillis(ss.getMinDate());
        mMaxDate.setTimeInMillis(ss.getMaxDate());
    }

    public void onDayOfMonthSelected(int year, int month, int day) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, day);

        if (mIsLunar) {
            mLunarCurrentYear = year;
            mLunarCurrentMonth = month;
            mLunarCurrentDay = day;
        }

        Message msg = mHandler.obtainMessage();
        msg.what = MESSAGE_CALENDAR_HEADER_TEXT_VALUE_SET;
        mHandler.sendMessage(msg);

        switch (mMode) {
            case DATE_MODE_START:
                clearCalendar(mStartDate, year, month, day);
                if (mIsLunar) {
                    mLunarStartYear = year;
                    mLunarStartMonth = month;
                    mLunarStartDay = day;
                    mIsLeapStartMonth = mIsLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                }
                break;
            case DATE_MODE_END:
                clearCalendar(mEndDate, year, month, day);
                if (mIsLunar) {
                    mLunarEndYear = year;
                    mLunarEndMonth = month;
                    mLunarEndDay = day;
                    mIsLeapEndMonth = mIsLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                }
                break;
            case DATE_MODE_WEEK_SELECT:
                mIsWeekRangeSet = true;
                final int dayOfWeekStart = (day % 7 + mDayOfWeekStart - 1) % 7;
                final int weekEnd = dayOfWeekStart != 0 ? dayOfWeekStart : 7;
                updateStartEndDateRange(weekEnd, year, month, day);
                break;
            default:
                clearCalendar(mStartDate, year, month, day);
                clearCalendar(mEndDate, year, month, day);
                if (mIsLunar) {
                    mLunarStartYear = year;
                    mLunarStartMonth = month;
                    mLunarStartDay = day;
                    mIsLeapStartMonth = mIsLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                    mLunarEndYear = year;
                    mLunarEndMonth = month;
                    mLunarEndDay = day;
                    mIsLeapEndMonth = mIsLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                }
                break;
        }

        if (mMode != DATE_MODE_NONE) {
            onValidationChanged(!mStartDate.after(mEndDate));
        }

        mDatePickerSpinner.updateDate(year, month, day);
        onDateChanged();
    }

    public Calendar getSelectedDay() {
        return mCurrentDate;
    }

    public Calendar getStartDate() {
        return mStartDate;
    }

    public Calendar getEndDate() {
        return mEndDate;
    }

    private static class SavedState extends View.BaseSavedState {
        private final long mMaxDate;
        private final long mMinDate;
        private final int mSelectedDay;
        private final int mSelectedMonth;
        private final int mSelectedYear;

        private SavedState(Parcelable superState, int year, int month, int day, long minDate, long maxDate, int isLeapMonth) {
            super(superState);
            mSelectedYear = year;
            mSelectedMonth = month;
            mSelectedDay = day;
            mMinDate = minDate;
            mMaxDate = maxDate;
        }

        private SavedState(Parcel in) {
            super(in);
            mSelectedYear = in.readInt();
            mSelectedMonth = in.readInt();
            mSelectedDay = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYear);
            dest.writeInt(mSelectedMonth);
            dest.writeInt(mSelectedDay);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
        }

        public int getSelectedDay() {
            return mSelectedDay;
        }

        public int getSelectedMonth() {
            return mSelectedMonth;
        }

        public int getSelectedYear() {
            return mSelectedYear;
        }

        public long getMinDate() {
            return mMinDate;
        }

        public long getMaxDate() {
            return mMaxDate;
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void debugLog(String msg) {
        if (SESL_DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private boolean isRTL() {
        if ("ur".equals(mCurrentLocale.getLanguage())) {
            return false;
        }
        byte directionality = Character.getDirectionality(mCurrentLocale.getDisplayName(mCurrentLocale).charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    @Override
    public void onDayClick(SeslSimpleMonthView view, int year, int month, int day) {
        debugLog("onDayClick day : " + day);

        if (!mIsCalledFromDeactivatedDayClick) {
            mDayOfWeekStart = view.getDayOfWeekStart();
        }

        int currentYear = mCurrentDate.get(Calendar.YEAR);
        int currentMonth = mCurrentDate.get(Calendar.MONTH);
        if (mIsLunar) {
            currentYear = mLunarCurrentYear;
            currentMonth = mLunarCurrentMonth;
        }

        onDayOfMonthSelected(year, month, day);

        final int selectedPos = (month - getMinMonth()) + ((year - getMinYear()) * 12);
        final boolean isNotSamePos = mCurrentPosition != selectedPos;
        if (year != currentYear || month != currentMonth || day != mOldSelectedDay || mIsLunar || isNotSamePos) {
            mOldSelectedDay = day;
            mCalendarPagerAdapter.notifyDataSetChanged();
        }

        final int enabledDayRangeStart = (getMinMonth() == month && getMinYear() == year) ?
                getMinDay() : 1;
        final int enabledDayRangeEnd = (getMaxMonth() == month && getMaxYear() == year) ?
                getMaxDay() : 31;

        if (mIsLunarSupported) {
            view.setLunar(mIsLunar, mIsLeapMonth, mPathClassLoader);
        }

        int startYear, startMonth, startDay, endYear, endMonth, endDay;
        if (mIsLunar) {
            startYear = mLunarStartYear;
            startMonth = mLunarStartMonth;
            startDay = mLunarStartDay;
            endYear = mLunarEndYear;
            endMonth = mLunarEndMonth;
            endDay = mLunarEndDay;
        } else {
            startYear = mStartDate.get(Calendar.YEAR);
            startMonth = mStartDate.get(Calendar.MONTH);
            startDay = mStartDate.get(Calendar.DAY_OF_MONTH);
            endYear = mEndDate.get(Calendar.YEAR);
            endMonth = mEndDate.get(Calendar.MONTH);
            endDay = mEndDate.get(Calendar.DAY_OF_MONTH);
        }

        view.setMonthParams(day, month, year,
                getFirstDayOfWeek(), enabledDayRangeStart, enabledDayRangeEnd,
                mMinDate, mMaxDate, startYear, startMonth, startDay, mIsLeapStartMonth,
                endYear, endMonth, endDay, mIsLeapEndMonth, mMode);
        view.invalidate();
        setCurrentViewType(VIEW_TYPE_SPINNER);
        mIsCalledFromDeactivatedDayClick = false;
    }

    // TODO rework this method
    // kang
    @Override
    public void onDeactivatedDayClick(SeslSimpleMonthView var1, int var2, int var3, int var4, boolean var5, boolean var6) {
        /* var1 = view; var2 = year; var3 = month; var4 = selectedDay; var5 = isLeapMonth; var6 = isPrevMonth;  */
        byte var7 = 1;
        this.mIsCalledFromDeactivatedDayClick = true;
        int var9;
        SeslSimpleMonthView var11;
        if (this.mIsLunar) {
            var2 = this.mCurrentPosition;
            if (var6) {
                --var2;
            } else {
                ++var2;
            }

            LunarDate var8 = this.getLunarDateByPosition(var2);
            var9 = var8.year;
            var3 = var8.month;
            var5 = var8.isLeapMonth;
            this.mIsLeapMonth = var5;
            this.mDatePickerSpinner.setLunar(this.mIsLunar, var5);
            var2 = this.mCurrentPosition;
            if (var6) {
                --var2;
            } else {
                ++var2;
            }

            this.mCurrentPosition = var2;
            this.mCalendarViewPager.setCurrentItem(var2);
            var11 = (SeslSimpleMonthView)this.mCalendarPagerAdapter.views.get(this.mCurrentPosition);
            if (var11 == null) {
                var2 = var7;
            } else {
                var2 = var11.getDayOfWeekStart();
            }

            this.mDayOfWeekStart = var2;
            this.onDayClick(var1, var9, var3, var4);
        } else {
            int var10 = this.getMinYear();
            var9 = this.getMinMonth();
            var11 = (SeslSimpleMonthView)this.mCalendarPagerAdapter.views.get((var2 - var10) * 12 + (var3 - var9));
            if (var11 == null) {
                var10 = 1;
            } else {
                var10 = var11.getDayOfWeekStart();
            }

            this.mDayOfWeekStart = var10;
            this.onDayClick(var1, var2, var3, var4);
            this.updateSimpleMonthView(true);
        }
    }
    // kang

    // TODO rework this method
    // kang
    private int getDayOffset() {
        SeslSimpleMonthView seslSimpleMonthView = this.mCalendarPagerAdapter.views.get(this.mCurrentPosition);
        this.mDayOfWeekStart = seslSimpleMonthView == null ? 1 : seslSimpleMonthView.getDayOfWeekStart();
        int i = (((this.mCurrentDate.get(5) % 7) + this.mDayOfWeekStart) - 1) % 7;
        if (i == 0) {
            return 7;
        }
        return i;
    }
    // kang

    // TODO rework this method
    // kang
    private void updateSimpleMonthView(boolean var1) {
        /* var1 = animation */

        int var2 = this.mCurrentDate.get(2);
        int var3 = this.mCurrentDate.get(1);
        if (this.mIsLunar) {
            var3 = this.mLunarCurrentYear;
            var2 = this.mLunarCurrentMonth;
        }

        int var4 = var3;
        if (this.mLunarChanged) {
            var2 = this.mTempDate.get(2);
            var4 = this.mTempDate.get(1);
        }

        var3 = (var4 - this.getMinYear()) * 12 + (var2 - this.getMinMonth());
        if (this.mIsLunar) {
            label64: {
                if (var2 < this.getIndexOfleapMonthOfYear(var4)) {
                    var3 = var2;
                } else {
                    var3 = var2 + 1;
                }

                if (var4 == this.getMinYear()) {
                    var4 = -this.getMinMonth();
                } else {
                    var4 = this.getTotalMonthCountWithLeap(var4 - 1);
                }

                var4 += var3;
                int var5 = this.mMode;
                if ((var5 != 1 && var5 != 3 || var2 != this.mLunarStartMonth || this.mIsLeapStartMonth != 1) && (var5 != 2 && var5 != 3 || var2 != this.mLunarEndMonth || this.mIsLeapEndMonth != 1)) {
                    var3 = var4;
                    if (var5 != 0) {
                        break label64;
                    }

                    var3 = var4;
                    if (!this.mIsLeapMonth) {
                        break label64;
                    }
                }

                var3 = var4 + 1;
            }
        }

        this.mCurrentPosition = var3;
        this.mCalendarViewPager.setCurrentItem(var3, var1);
        Message var6 = this.mHandler.obtainMessage();
        var6.what = 1000;
        var6.obj = true;
        this.mHandler.sendMessage(var6);
        var6 = this.mHandler.obtainMessage();
        var6.what = 1001;
        this.mHandler.sendMessage(var6);
    }
    // kang

    private class CalendarPagerAdapter extends PagerAdapter {
        SparseArray<SeslSimpleMonthView> views = new SparseArray<>();

        public CalendarPagerAdapter() {
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public int getCount() {
            final int diffYear = getMaxYear() - getMinYear();
            mPositionCount = (getMaxMonth() - getMinMonth()) + 1 + (diffYear * 12);
            if (mIsLunar) {
                mPositionCount = getTotalMonthCountWithLeap(getMaxYear());
            }
            return mPositionCount;
        }

        @NonNull
        public Object instantiateItem(View pager, int position) {
            SeslSimpleMonthView v = new SeslSimpleMonthView(mContext);
            debugLog("instantiateItem : " + position);
            v.setClickable(true);
            v.setOnDayClickListener(SeslSpinningDatePicker.this);
            v.setOnDeactivatedDayClickListener(SeslSpinningDatePicker.this);
            v.setTextColor(mMonthViewColor);

            final int currentMonth = getMinMonth() + position;

            int year = (currentMonth / DEFAULT_MONTH_PER_YEAR) + getMinYear();
            int month;
            boolean isLeapMonth;
            if (mIsLunar) {
                LunarDate lunarDate = getLunarDateByPosition(position);
                year = lunarDate.year;
                month = lunarDate.month;
                isLeapMonth = lunarDate.isLeapMonth;
            } else {
                month = currentMonth % DEFAULT_MONTH_PER_YEAR;
                isLeapMonth = false;
            }

            int selectedDay = -1;
            if (mIsLunar) {
                if (mLunarCurrentYear == year && mLunarCurrentMonth == month) {
                    selectedDay = mLunarCurrentDay;
                }
            } else {
                if (mCurrentDate.get(Calendar.YEAR) == year
                        && mCurrentDate.get(Calendar.MONTH) == month) {
                    selectedDay = mCurrentDate.get(Calendar.DAY_OF_MONTH);
                }
            }

            if (mIsLunarSupported) {
                v.setLunar(mIsLunar, isLeapMonth, mPathClassLoader);
            }

            int startYear, startMonth, startDay, endYear, endMonth, endDay;
            if (mIsLunar) {
                startYear = mLunarStartYear;
                startMonth = mLunarStartMonth;
                startDay = mLunarStartDay;
                endYear = mLunarEndYear;
                endMonth = mLunarEndMonth;
                endDay = mLunarEndDay;
            } else {
                startYear = mStartDate.get(Calendar.YEAR);
                startMonth = mStartDate.get(Calendar.MONTH);
                startDay = mStartDate.get(Calendar.DAY_OF_MONTH);
                endYear = mEndDate.get(Calendar.YEAR);
                endMonth = mEndDate.get(Calendar.MONTH);
                endDay = mEndDate.get(Calendar.DAY_OF_MONTH);
            }
            v.setMonthParams(selectedDay, month, year,
                    getFirstDayOfWeek(), 1, 31,
                    mMinDate, mMaxDate, startYear, startMonth, startDay, mIsLeapStartMonth,
                    endYear, endMonth, endDay, mIsLeapEndMonth, mMode);

            if (position == 0) {
                v.setFirstMonth();
            }
            if (position == mPositionCount - 1) {
                v.setLastMonth();
            }

            if (mIsLunar) {
                if (position != 0 && getLunarDateByPosition(position - 1).isLeapMonth) {
                    v.setPrevMonthLeap();
                }
                if (position != mPositionCount - 1 && getLunarDateByPosition(position + 1).isLeapMonth) {
                    v.setNextMonthLeap();
                }
            }

            mNumDays = v.getNumDays();
            mWeekStart = v.getWeekStart();

            ((ViewPager) pager).addView(v, 0);
            views.put(position, v);
            return v;
        }

        @Override
        public void destroyItem(View pager, int position, Object view) {
            debugLog("destroyItem : " + position);
            ((ViewPager) pager).removeView((View) view);
            views.remove(position);
        }

        @Override
        public boolean isViewFromObject(View pager, Object obj) {
            return pager != null && pager.equals(obj);
        }

        @Override
        public void startUpdate(View view) {
            debugLog("startUpdate");
        }

        @Override
        public void finishUpdate(View view) {
            debugLog("finishUpdate");
        }
    }

    private class CalendarPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            if (mIsRTL) {
                mIsConfigurationChanged = false;
            }

            if (mIsFromSetLunar) {
                mIsFromSetLunar = false;
                return;
            }

            mCurrentPosition = position;

            final int currentMonth = getMinMonth() + position;

            int year = (currentMonth / DEFAULT_MONTH_PER_YEAR) + getMinYear();
            int month = currentMonth % 12;
            int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            if (mIsLunar) {
                LunarDate lunarDate = getLunarDateByPosition(position);
                year = lunarDate.year;
                month = lunarDate.month;
                day = mLunarCurrentDay;
                mIsLeapMonth = lunarDate.isLeapMonth;
                mDatePickerSpinner.setLunar(mIsLunar, mIsLeapMonth);
            }

            final boolean isYearChanged = year != mTempDate.get(Calendar.YEAR);

            mTempDate.set(Calendar.YEAR, year);
            mTempDate.set(Calendar.MONTH, month);
            mTempDate.set(Calendar.DAY_OF_MONTH, 1);
            if (day > mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                day = mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH);
            }
            mTempDate.set(Calendar.DAY_OF_MONTH, day);

            Message msg = mHandler.obtainMessage();
            msg.what = MESSAGE_CALENDAR_HEADER_TEXT_VALUE_SET;
            msg.obj = isYearChanged;
            mHandler.sendMessage(msg);
            Message msg1 = mHandler.obtainMessage();
            msg1.what = MESSAGE_CALENDAR_HEADER_MONTH_BUTTON_SET;
            mHandler.sendMessage(msg1);

            SparseArray<SeslSimpleMonthView> views = mCalendarPagerAdapter.views;
            if (views.get(position) != null) {
                views.get(position).clearAccessibilityFocus();
                views.get(position).setImportantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
            if (position != 0) {
                int previousPos = position - 1;
                if (views.get(previousPos) != null) {
                    views.get(previousPos).clearAccessibilityFocus();
                    views.get(previousPos).setImportantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
                }
            }
            if (position != mPositionCount - 1) {
                int nextPos = position + 1;
                if (views.get(nextPos) != null) {
                    views.get(nextPos).clearAccessibilityFocus();
                    views.get(nextPos).setImportantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
                }
            }
        }
    }

    private static Activity scanForActivity(Context cont) {
        if (cont instanceof Activity) {
            return (Activity) cont;
        }
        if (cont instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) cont).getBaseContext());
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasureSpecHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mCalendarViewPagerWidth);

        if (mIsFirstMeasure || mOldCalendarViewPagerWidth != mCalendarViewPagerWidth) {
            mIsFirstMeasure = false;

            mOldCalendarViewPagerWidth = mCalendarViewPagerWidth;

            if (mCustomButtonLayout != null) {
                mCustomButtonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, mCalendarHeaderLayoutHeight));
            }
            mCalendarHeaderLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, mCalendarHeaderLayoutHeight));
            mDayOfTheWeekLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    mDayOfTheWeekLayoutWidth, mDayOfTheWeekLayoutHeight));
            mDayOfTheWeekView.setLayoutParams(new LinearLayout.LayoutParams(
                    mDayOfTheWeekLayoutWidth, mDayOfTheWeekLayoutHeight));
            mCalendarViewPager.setLayoutParams(new LinearLayout.LayoutParams(
                    mCalendarViewPagerWidth, mCalendarViewPagerHeight));
            mPickerView.setLayoutParams(new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, mPickerViewHeight));

            if (mIsRTL && mIsConfigurationChanged) {
                mCalendarViewPager.seslSetConfigurationChanged(true);
            }

            mFirstBlankSpace.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, mFirstBlankSpaceHeight));
            mSecondBlankSpace.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, mSecondBlankSpaceHeight));
        }

        super.onMeasure(newWidthMeasureSpec, heightMeasureSpec);
    }

    private int makeMeasureSpec(int widthMeasureSpec, int heightMeasureSpec) {
        if (heightMeasureSpec == SIZE_UNSPECIFIED) {
            return widthMeasureSpec;
        }

        final int mode = View.MeasureSpec.getMode(widthMeasureSpec);

        final int size;
        int smallestScreenWidthDp = getResources().getConfiguration().smallestScreenWidthDp;
        if (mode == MeasureSpec.AT_MOST) {
            if (smallestScreenWidthDp >= 600) {
                size = getResources().getDimensionPixelSize(R.dimen.sesl_date_picker_dialog_min_width);
            } else {
                size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        (float) smallestScreenWidthDp, getResources().getDisplayMetrics()) + 0.5f);
            }
        } else {
            size = View.MeasureSpec.getSize(widthMeasureSpec);
        }

        switch (mode) {
            case MeasureSpec.AT_MOST:
                if (smallestScreenWidthDp < 600 || mLayoutMode != LAYOUT_MODE_DEFAULT) {
                    mCalendarViewPagerWidth = mViewAnimator.getMeasuredWidth() - (mCalendarViewMargin * 2);
                    mDayOfTheWeekLayoutWidth = mViewAnimator.getMeasuredWidth() - (mCalendarViewMargin * 2);
                } else {
                    mCalendarViewPagerWidth = size - (mCalendarViewMargin * 2);
                    mDayOfTheWeekLayoutWidth = size - (mCalendarViewMargin * 2);
                }
                return View.MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
            case MeasureSpec.UNSPECIFIED:
                return View.MeasureSpec.makeMeasureSpec(heightMeasureSpec, MeasureSpec.EXACTLY);
            case MeasureSpec.EXACTLY:
                if (smallestScreenWidthDp < 600 || mLayoutMode != LAYOUT_MODE_DEFAULT) {
                    mCalendarViewPagerWidth = mViewAnimator.getMeasuredWidth() - (mCalendarViewMargin * 2);
                    mDayOfTheWeekLayoutWidth = mViewAnimator.getMeasuredWidth() - (mCalendarViewMargin * 2);
                } else {
                    mCalendarViewPagerWidth = size - (mCalendarViewMargin * 2);
                    mDayOfTheWeekLayoutWidth = size - (mCalendarViewMargin * 2);
                }
                return widthMeasureSpec;
        }

        throw new IllegalArgumentException("Unknown measure mode: " + mode);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        calculateContentHeight();
    }

    private void calculateContentHeight() {
        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT || getMeasuredHeight() <= mDatePickerHeight) {
            int availableHeight = mMeasureSpecHeight;
            if (mContentFrame != null) {
                availableHeight = mContentFrame.getMeasuredHeight();
            }

            updateViewType(availableHeight);
        }
    }

    private void updateViewType(int height) {
        if (!mSupportShortSpinnerHeight && Build.VERSION.SDK_INT >= 24) {
            Activity activity = scanForActivity(mContext);
            if (activity != null && activity.isInMultiWindowMode()) {
                if (height < mDatePickerHeight) {
                    setCurrentViewType(VIEW_TYPE_SPINNER);
                    if (mDatePickerSpinner != null) {
                        mDatePickerSpinner.setOnSpinnerDateClickListener(null);
                    }
                } else {
                    if (mDatePickerSpinner != null) {
                        mDatePickerSpinner.setOnSpinnerDateClickListener(mOnSpinnerDateClickListener);
                    }
                }
            } else {
                if (mDatePickerSpinner != null && mDatePickerSpinner.getOnSpinnerDateClickListener() == null) {
                    mDatePickerSpinner.setOnSpinnerDateClickListener(mOnSpinnerDateClickListener);
                }
            }
        }
    }

    private String getMonthAndYearString(Calendar calendar) {
        if (mIsFarsiLanguage) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("LLLL y", mCurrentLocale);
            return simpleDateFormat.format(calendar.getTime());
        }
        if (mIsTibetanLanguage) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("y LLLL", Locale.getDefault());
            return simpleDateFormat.format(calendar.getTime());
        }

        StringBuilder stringBuilder = new StringBuilder(50);
        Formatter formatter = new Formatter(stringBuilder, mCurrentLocale);
        stringBuilder.setLength(0);

        final long millis = calendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), formatter, millis, millis,
                DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_SHOW_YEAR,
                Time.getCurrentTimezone()).toString();
    }

    private class DayOfTheWeekView extends View {
        private Paint mMonthDayLabelPaint;
        private int mNormalDayTextColor;
        private int mSaturdayTextColor;
        private int mSundayTextColor;
        private int[] mDayColorSet = new int[7];
        private String mDefaultWeekdayFeatureString = "XXXXXXR";
        private Calendar mDayLabelCalendar = Calendar.getInstance();
        private String mWeekdayFeatureString;

        public DayOfTheWeekView(Context context, TypedArray array) {
            super(context);

            final Resources res = context.getResources();

            final int monthDayLabelTextSize
                    = res.getDimensionPixelSize(R.dimen.sesl_date_picker_month_day_label_text_size);

            mNormalDayTextColor = array.getColor(R.styleable.DatePicker_dayTextColor,
                    res.getColor(R.color.sesl_date_picker_normal_text_color_light));
            mSundayTextColor = array.getColor(R.styleable.DatePicker_sundayTextColor,
                    res.getColor(R.color.sesl_date_picker_sunday_text_color_light));
            mSaturdayTextColor = ResourcesCompat.getColor(res,
                    R.color.sesl_date_picker_saturday_week_text_color_light, null);

            if (mMonthViewColor != null) {
                mWeekdayFeatureString = mMonthViewColor;
            } else {
                mWeekdayFeatureString = SeslCscFeatureReflector.getString(
                        TAG_CSCFEATURE_CALENDAR_SETCOLOROFDAYS, mDefaultWeekdayFeatureString);
            }

            mMonthDayLabelPaint = new Paint();
            mMonthDayLabelPaint.setAntiAlias(true);
            mMonthDayLabelPaint.setColor(mNormalDayTextColor);
            mMonthDayLabelPaint.setTextSize(monthDayLabelTextSize);
            mMonthDayLabelPaint.setTypeface(Typeface.create("sec-roboto-light", Typeface.NORMAL));
            mMonthDayLabelPaint.setTextAlign(Paint.Align.CENTER);
            mMonthDayLabelPaint.setStyle(Paint.Style.FILL);
            mMonthDayLabelPaint.setFakeBoldText(false);
        }

        // TODO rework this method
        // kang
        @Override
        protected void onDraw(Canvas var1) {
            super.onDraw(var1);
            if (mNumDays != 0) {
                int var2 = mDayOfTheWeekLayoutHeight * 2 / 3;
                int var3 = mDayOfTheWeekLayoutWidth / (mNumDays * 2);
                byte var4 = 0;
                int var5 = 0;

                while(true) {
                    int var6 = var4;
                    int var7;
                    if (var5 >= mNumDays) {
                        while(var6 < mNumDays) {
                            var7 = (mWeekStart + var6) % mNumDays;
                            this.mDayLabelCalendar.set(7, var7);
                            String var8 = mDayFormatter.format(this.mDayLabelCalendar.getTime()).toUpperCase();
                            int var9;
                            if (mIsRTL) {
                                var9 = ((mNumDays - 1 - var6) * 2 + 1) * var3;
                                var5 = mPadding;
                            } else {
                                var9 = (var6 * 2 + 1) * var3;
                                var5 = mPadding;
                            }

                            this.mMonthDayLabelPaint.setColor(this.mDayColorSet[var7]);
                            var1.drawText(var8, (float)(var9 + var5), (float)var2, this.mMonthDayLabelPaint);
                            ++var6;
                        }

                        return;
                    }

                    char var10 = this.mWeekdayFeatureString.charAt(var5);
                    var7 = (var5 + 2) % mNumDays;
                    if (var10 != 'B') {
                        if (var10 != 'R') {
                            this.mDayColorSet[var7] = this.mNormalDayTextColor;
                        } else {
                            this.mDayColorSet[var7] = this.mSundayTextColor;
                        }
                    } else {
                        this.mDayColorSet[var7] = this.mSaturdayTextColor;
                    }

                    ++var5;
                }
            }
        }
        // kang
    }

    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.sesl_date_picker_calendar_header_prev_button) {
            if (mIsRTL) {
                if (mCurrentPosition != mPositionCount - 1) {
                    mCalendarViewPager.setCurrentItem(mCurrentPosition + 1);
                }
            } else {
                if (mCurrentPosition != 0) {
                    mCalendarViewPager.setCurrentItem(mCurrentPosition - 1);
                }
            }
        } else if (viewId == R.id.sesl_date_picker_calendar_header_next_button) {
            if (mIsRTL) {
                if (mCurrentPosition != 0) {
                    mCalendarViewPager.setCurrentItem(mCurrentPosition - 1);
                }
            } else {
                if (mCurrentPosition != mPositionCount - 1) {
                    mCalendarViewPager.setCurrentItem(mCurrentPosition + 1);
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeAllCallbacks();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onLongClick(View view) {
        final int id = view.getId();
        if (id == R.id.sesl_date_picker_calendar_header_prev_button
                && mCurrentPosition != 0) {
            postChangeCurrentByOneFromLongPress(false, ViewConfiguration.getLongPressTimeout());
        } else if (id == R.id.sesl_date_picker_calendar_header_next_button
                && mCurrentPosition != mPositionCount - 1) {
            postChangeCurrentByOneFromLongPress(true, ViewConfiguration.getLongPressTimeout());
        }
        return false;
    }

    private void postChangeCurrentByOneFromLongPress(boolean increment, long delayMillis) {
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
        } else {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        mChangeCurrentByOneFromLongPressCommand.setStep(increment);
        postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis);
    }

    private void removeAllCallbacks() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCalendarViewPager.setCurrentItem(mCurrentPosition, false);
                }
            }, 200);
        }
    }

    private class ChangeCurrentByOneFromLongPressCommand implements Runnable {
        private boolean mIncrement;

        private void setStep(boolean increment) {
            mIncrement = increment;
        }

        @Override
        public void run() {
            if (mIncrement) {
                mCalendarViewPager.setCurrentItem(mCurrentPosition + 1);
            } else {
                mCalendarViewPager.setCurrentItem(mCurrentPosition - 1);
            }
            postDelayed(this, DEFAULT_LONG_PRESS_UPDATE_INTERVAL);
        }
    }

    public void setDateMode(int mode) {
        mMode = mode;
        mIsWeekRangeSet = false;

        switch (mode) {
            case DATE_MODE_START:
                int startYear = mIsLunar ? mLunarStartYear: mStartDate.get(Calendar.YEAR);
                int startMonth = mIsLunar ? mLunarStartMonth: mStartDate.get(Calendar.MONTH);
                int startDay = mIsLunar ? mLunarStartDay: mStartDate.get(Calendar.DAY_OF_MONTH);
                mDatePickerSpinner.updateDate(startYear, startMonth, startDay);
                break;
            case DATE_MODE_END:
                int endYear = mIsLunar ? mLunarEndYear: mEndDate.get(Calendar.YEAR);
                int endMonth = mIsLunar ? mLunarEndMonth: mEndDate.get(Calendar.MONTH);
                int endDay = mIsLunar ? mLunarEndDay: mEndDate.get(Calendar.DAY_OF_MONTH);
                mDatePickerSpinner.updateDate(endYear, endMonth, endDay);
                break;
        }

        if (mCurrentViewType == VIEW_TYPE_SPINNER) {
            mPickerView.setVisibility(VISIBLE);
            mPickerView.setEnabled(true);
        }

        SeslSimpleMonthView currentMonthView = mCalendarPagerAdapter.views.get(mCurrentPosition);
        if (currentMonthView != null) {
            int day, month, year;
            if (mIsLunar) {
                year = mLunarCurrentYear;
                month = mLunarCurrentMonth;
                day = mLunarCurrentDay;
            } else {
                year = mCurrentDate.get(Calendar.YEAR);
                month = mCurrentDate.get(Calendar.MONTH);
                day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            }

            int minDay = (getMinMonth() == month && getMinYear() == year) ? getMinDay() : 1;
            int maxDay = (getMaxMonth() == month && getMaxYear() == year) ? getMaxDay() : 31;

            int startYear, startMonth, startDay, endYear, endMonth, endDay;
            if (mIsLunar) {
                startYear = mLunarStartYear;
                startMonth = mLunarStartMonth;
                startDay = mLunarStartDay;
                endYear = mLunarEndYear;
                endMonth = mLunarEndMonth;
                endDay = mLunarEndDay;
            } else {
                startYear = mStartDate.get(Calendar.YEAR);
                startMonth = mStartDate.get(Calendar.MONTH);
                startDay = mStartDate.get(Calendar.DAY_OF_MONTH);
                endYear = mEndDate.get(Calendar.YEAR);
                endMonth = mEndDate.get(Calendar.MONTH);
                endDay = mEndDate.get(Calendar.DAY_OF_MONTH);
            }
            currentMonthView.setMonthParams(day, month, year,
                    getFirstDayOfWeek(), minDay, maxDay, mMinDate, mMaxDate,
                    startYear, startMonth, startDay, mIsLeapStartMonth,
                    endYear, endMonth, endDay, mIsLeapEndMonth, mMode);
            currentMonthView.invalidate();
        }

        if (mIsLunar) {
            updateSimpleMonthView(false);
        }

        mCalendarPagerAdapter.notifyDataSetChanged();
    }

    public int getDateMode() {
        return mMode;
    }

    private void checkMaxFontSize() {
        final float currentFontScale = mContext.getResources().getConfiguration().fontScale;
        final int calendarHeaderTextSize = getResources().getDimensionPixelOffset(
                R.dimen.sesl_date_picker_calendar_header_month_text_size);
        if (currentFontScale > MAX_FONT_SCALE) {
            mCalendarHeaderText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (float) Math.floor(Math.ceil(calendarHeaderTextSize / currentFontScale) * (double) 1.2f));
        }
    }

    public void setCurrentViewType(int type) {
        boolean typeChanged = false;

        switch (type) {
            case VIEW_TYPE_SPINNER:
                if (mCurrentViewType != type) {
                    switch (mMode) {
                        case DATE_MODE_START:
                            int startYear = mStartDate.get(Calendar.YEAR);
                            int startMonth = mStartDate.get(Calendar.MONTH);
                            int startDayOfMonth = mStartDate.get(Calendar.DAY_OF_MONTH);
                            if (mIsLunar) {
                                startYear = mLunarStartYear;
                                startMonth = mLunarStartMonth;
                                startDayOfMonth = mLunarStartDay;
                            }
                            mDatePickerSpinner.updateDate(startYear, startMonth, startDayOfMonth);
                            break;

                        case DATE_MODE_END:
                            int endYear = mEndDate.get(Calendar.YEAR);
                            int endMonth = mEndDate.get(Calendar.MONTH);
                            int endDayOfMonth = mEndDate.get(Calendar.DAY_OF_MONTH);
                            if (mIsLunar) {
                                endYear = mLunarEndYear;
                                endMonth = mLunarEndMonth;
                                endDayOfMonth = mLunarEndDay;
                            }
                            mDatePickerSpinner.updateDate(endYear, endMonth, endDayOfMonth);
                            break;

                        default:
                            int year = mCurrentDate.get(Calendar.YEAR);
                            int month = mCurrentDate.get(Calendar.MONTH);
                            int dayOfMonth = mCurrentDate.get(Calendar.DAY_OF_MONTH);
                            if (mIsLunar) {
                                year = mLunarCurrentYear;
                                month = mLunarCurrentMonth;
                                dayOfMonth = mLunarCurrentDay;
                            }
                            mDatePickerSpinner.updateDate(year, month, dayOfMonth);
                    }

                    mViewAnimator.setDisplayedChild(VIEW_TYPE_SPINNER);

                    mPickerView.setEnabled(true);
                    mPickerView.setVisibility(View.VISIBLE);
                    mCalendarViewLayout.setVisibility(View.GONE);

                    mCurrentViewType = type;

                    Message msg = mHandler.obtainMessage();
                    msg.what = MESSAGE_CALENDAR_HEADER_TEXT_VALUE_SET;
                    mHandler.sendMessage(msg);

                    typeChanged = true;
                }

                if (mOnViewTypeChangedListener != null && typeChanged) {
                    mOnViewTypeChangedListener.onViewTypeChanged(this);
                }

                Message msg = mHandler.obtainMessage();
                msg.what = MESSAGE_CALENDAR_HEADER_MONTH_BUTTON_SET;
                mHandler.sendMessage(msg);
                break;

            case VIEW_TYPE_CALENDAR:
                if (mCurrentViewType != type) {
                    mCalendarPagerAdapter.notifyDataSetChanged();

                    mViewAnimator.setDisplayedChild(1);

                    mPickerView.setEnabled(false);
                    mPickerView.setVisibility(View.GONE);
                    mCalendarViewLayout.setEnabled(true);
                    mCalendarViewLayout.setVisibility(View.VISIBLE);

                    mCurrentViewType = type;

                    Message msg1 = mHandler.obtainMessage();
                    msg1.what = MESSAGE_CALENDAR_HEADER_TEXT_VALUE_SET;
                    mHandler.sendMessage(msg1);

                    typeChanged = true;
                }

                if (mOnViewTypeChangedListener != null) {
                    mOnViewTypeChangedListener.onViewTypeChanged(this);
                }

                Message msg1 = mHandler.obtainMessage();
                msg1.what = MESSAGE_CALENDAR_HEADER_MONTH_BUTTON_SET;
                mHandler.sendMessage(msg1);
                break;
        }
    }

    public int getCurrentViewType() {
        return mCurrentViewType;
    }

    public void setLunarSupported(boolean supported, View switchButton) {
        mIsLunarSupported = supported;

        if (!supported) {
            mIsLunar = false;
            mIsLeapMonth = false;
            mDatePickerSpinner.setLunar(false, false);
            mCustomButtonView = null;
        } else {
            removeCustomViewFromParent();
            mCustomButtonView = switchButton;
            if (switchButton != null) {
                removeCustomViewFromParent();
                mCustomButtonView.setId(android.R.id.custom);
                RelativeLayout.LayoutParams buttonParams;
                ViewGroup.LayoutParams layoutParams = mCustomButtonView.getLayoutParams();
                if (layoutParams instanceof RelativeLayout.LayoutParams) {
                    buttonParams = (RelativeLayout.LayoutParams) layoutParams;
                } else {
                    if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                        buttonParams = new RelativeLayout.LayoutParams((ViewGroup.MarginLayoutParams) layoutParams);
                    } else if (layoutParams != null) {
                        buttonParams = new RelativeLayout.LayoutParams(layoutParams);
                    } else {
                        buttonParams = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                }
                buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);
                buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                addCustomButtonInHeader();
            }
        }

        if (mIsLunarSupported && mPathClassLoader == null) {
            mPackageManager = mContext.getApplicationContext().getPackageManager();
            mPathClassLoader = LunarUtils.getPathClassLoader(getContext());
            if (mPathClassLoader != null) {
                mSolarLunarConverter = SeslFeatureReflector.getSolarLunarConverter(mPathClassLoader);
                mSolarLunarTables = SeslFeatureReflector.getSolarLunarTables(mPathClassLoader);
            }
        }
    }

    public void setLunar(boolean isLunar, boolean isLeapMonth) {
        if (mIsLunarSupported && mIsLunar != isLunar) {
            mIsLunar = isLunar;
            mIsLeapMonth = isLeapMonth;
            mDatePickerSpinner.setLunar(isLunar, isLeapMonth);

            if (isLunar) {
                setTotalMonthCountWithLeap();
                if (mMode == DATE_MODE_NONE) {
                    mIsLeapStartMonth = isLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                    mIsLeapEndMonth = isLeapMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
                }
            }

            mIsFromSetLunar = true;
            mCalendarPagerAdapter.notifyDataSetChanged();
            mLunarChanged = true;
            updateSimpleMonthView(true);
            mLunarChanged = false;
        }
    }

    public boolean isLunar() {
        return mIsLunar;
    }

    public boolean isLeapMonth() {
        return mIsLeapMonth;
    }

    public void setSeparateLunarButton(boolean separate) {
        if (mIsCustomButtonSeparate != separate) {
            if (separate) {
                removeCustomButtonInHeader();
                addCustomButtonSeparateLayout();
            } else {
                removeCustomButtonSeparateLayout();
                addCustomButtonInHeader();
            }
            mIsCustomButtonSeparate = separate;
        }
    }

    private void removeCustomViewFromParent() {
        if (mCustomButtonView != null) {
            ViewParent parent = mCustomButtonView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mCustomButtonView);
            }
        }
    }

    private void addCustomButtonInHeader() {
        if (mCustomButtonView != null) {
            removeCustomViewFromParent();
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mCalendarHeader.getLayoutParams();
            lp.addRule(RelativeLayout.START_OF, mCustomButtonView.getId());
            lp.leftMargin = mContext.getResources().getDimensionPixelOffset(
                    R.dimen.sesl_date_picker_lunar_calendar_header_margin);
            ((RelativeLayout.LayoutParams) mPrevButton.getLayoutParams()).leftMargin = 0;
            ((RelativeLayout.LayoutParams) mNextButton.getLayoutParams()).rightMargin = 0;
            mCalendarHeaderLayout.addView(mCustomButtonView);
        }
    }

    private void removeCustomButtonInHeader() {
        final Resources res = mContext.getResources();
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mCalendarHeader.getLayoutParams();
        lp.removeRule(RelativeLayout.START_OF);
        lp.leftMargin = 0;
        ((RelativeLayout.LayoutParams) mPrevButton.getLayoutParams()).leftMargin
                = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_margin);
        ((RelativeLayout.LayoutParams) mNextButton.getLayoutParams()).rightMargin
                = res.getDimensionPixelOffset(R.dimen.sesl_date_picker_calendar_view_margin);
        removeCustomViewFromParent();
    }

    private void addCustomButtonSeparateLayout() {
        if (mCustomButtonView != null) {
            if (mCustomButtonLayout == null) {
                mCustomButtonLayout = new RelativeLayout(mContext);
                mCustomButtonLayout.setLayoutParams(
                        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, mCalendarHeaderLayoutHeight));
            } else {
                ((LinearLayout.LayoutParams) mCustomButtonLayout.getLayoutParams()).height = mCalendarHeaderLayoutHeight;
            }
            removeCustomViewFromParent();
            mCustomButtonLayout.addView(mCustomButtonView);
            mCalendarViewLayout.addView(mCustomButtonLayout, 0);
            mDatePickerHeight += mCalendarHeaderLayoutHeight;
        }
    }

    private void removeCustomButtonSeparateLayout() {
        removeCustomViewFromParent();
        mCalendarViewLayout.removeView(mCustomButtonLayout);
        mDatePickerHeight -= mCalendarHeaderLayoutHeight;
    }

    // TODO rework this method
    // kang
    private void setTotalMonthCountWithLeap() {
        if (this.mSolarLunarTables != null && this.mPathClassLoader != null) {
            int var1 = 0;
            this.mTotalMonthCountWithLeap = new int[this.getMaxYear() - this.getMinYear() + 1];

            for(int var2 = this.getMinYear(); var2 <= this.getMaxYear(); ++var2) {
                int var4;
                label41: {
                    int var3 = this.getMinYear();
                    var4 = 12;
                    if (var2 == var3) {
                        var4 = this.getMinMonth() + 1;
                        var3 = this.getIndexOfleapMonthOfYear(var2);
                        if (var3 <= 12 && var3 >= var4) {
                            var4 = 13 - var4;
                        } else {
                            var4 = 12 - var4;
                        }
                    } else {
                        if (var2 != this.getMaxYear()) {
                            if (this.getIndexOfleapMonthOfYear(var2) <= 12) {
                                var4 = 13;
                            }
                            break label41;
                        }

                        var3 = this.getMaxMonth() + 1;
                        int var5 = this.getIndexOfleapMonthOfYear(var2);
                        var4 = var3;
                        if (var5 > 12) {
                            break label41;
                        }

                        if (var3 < var5) {
                            var4 = var3;
                            break label41;
                        }

                        var4 = var3;
                    }

                    ++var4;
                }

                var1 += var4;
                this.mTotalMonthCountWithLeap[var2 - this.getMinYear()] = var1;
            }
        }
    }
    // kang

    private int getTotalMonthCountWithLeap(int year) {
        if (mTotalMonthCountWithLeap == null || year < getMinYear()) {
            return 0;
        }
        return mTotalMonthCountWithLeap[year - getMinYear()];
    }

    // TODO rework this method
    // kang
    private LunarDate getLunarDateByPosition(int var1) {
        /* var1 = position */
        
        LunarDate var2 = new LunarDate();
        int var3 = this.getMinYear();
        int var4 = this.getMinYear();

        boolean var8;
        while(true) {
            int var5 = this.getMaxYear();
            byte var6 = 0;
            boolean var7 = false;
            if (var4 > var5) {
                var8 = false;
                var1 = var6;
                var4 = var3;
                break;
            }

            if (var1 < this.getTotalMonthCountWithLeap(var4)) {
                if (var4 == this.getMinYear()) {
                    var3 = -this.getMinMonth();
                } else {
                    var3 = this.getTotalMonthCountWithLeap(var4 - 1);
                }

                int var10 = var1 - var3;
                var5 = this.getIndexOfleapMonthOfYear(var4);
                byte var9 = 12;
                if (var5 <= 12) {
                    var9 = 13;
                }

                if (var10 < var5) {
                    var1 = var10;
                } else {
                    var1 = var10 - 1;
                }

                var8 = var7;
                if (var9 == 13) {
                    var8 = var7;
                    if (var5 == var10) {
                        var8 = true;
                    }
                }
                break;
            }

            ++var4;
        }

        var2.set(var4, var1, 1, var8);
        return var2;
    }
    // kang

    private int getIndexOfleapMonthOfYear(int year) {
        if (mSolarLunarTables == null) {
            return 127;
        }

        final int startOfLunarYear = SeslSolarLunarTablesReflector
                .getField_START_OF_LUNAR_YEAR(mPathClassLoader, mSolarLunarTables);
        final int widthPerYear = SeslSolarLunarTablesReflector
                .getField_WIDTH_PER_YEAR(mPathClassLoader, mSolarLunarTables);
        final int indexOfLeapMonth = SeslSolarLunarTablesReflector
                .getField_INDEX_OF_LEAP_MONTH(mPathClassLoader, mSolarLunarTables);
        return SeslSolarLunarTablesReflector.getLunar(mPathClassLoader, mSolarLunarTables,
                ((year - startOfLunarYear) * widthPerYear) + indexOfLeapMonth);
    }

    @RestrictTo(LIBRARY)
    static class LunarDate {
        public int day;
        boolean isLeapMonth;
        public int month;
        public int year;

        LunarDate() {
            year = 1900;
            month = 1;
            day = 1;
            isLeapMonth = false;
        }

        LunarDate(int year, int month, int day, boolean isLeap) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.isLeapMonth = isLeap;
        }

        public void set(int year, int month, int day, boolean isLeap) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.isLeapMonth = isLeap;
        }
    }

    @RestrictTo(LIBRARY)
    static class LunarUtils {
        private static PathClassLoader mClassLoader;

        static PathClassLoader getPathClassLoader(Context context) {
            if (mClassLoader == null) {
                try {
                    ApplicationInfo appInfo = context.getPackageManager()
                            .getApplicationInfo(getCalendarPackageName(), PackageManager.GET_META_DATA);
                    if (appInfo == null) {
                        Log.e(TAG, "getPathClassLoader, appInfo is null");
                        return null;
                    }

                    String calendarPkgPath = appInfo.sourceDir;
                    if (calendarPkgPath == null || TextUtils.isEmpty(calendarPkgPath)) {
                        Log.e(TAG, "getPathClassLoader, calendar package source " +
                                "directory is null or empty");
                        return null;
                    }

                    mClassLoader = new PathClassLoader(calendarPkgPath, ClassLoader.getSystemClassLoader());
                } catch (PackageManager.NameNotFoundException unused) {
                    Log.e(TAG, "getPathClassLoader, calendar package name not found");
                    return null;
                }
            }

            return mClassLoader;
        }
    }

    @RestrictTo(LIBRARY)
    public static String getCalendarPackageName() {
        String packageName = SeslFloatingFeatureReflector
                .getString("SEC_FLOATING_FEATURE_CALENDAR_CONFIG_PACKAGE_NAME",
                        "com.android.calendar");
        if ("com.android.calendar".equals(packageName)) {
            return packageName;
        }

        try {
            mPackageManager.getPackageInfo(packageName, 0);
            return packageName;
        } catch (PackageManager.NameNotFoundException e) {
            return "com.android.calendar";
        }
    }

    public void setLunarStartDate(int year, int month, int day, boolean isLeapStartMonth) {
        mLunarStartYear = year;
        mLunarStartMonth = month;
        mLunarStartDay = day;
        mIsLeapStartMonth = isLeapStartMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
    }

    public int[] getLunarStartDate() {
        return new int[]{mLunarStartYear, mLunarStartMonth, mLunarStartDay, mIsLeapStartMonth};
    }

    public void setLunarEndDate(int year, int month, int day, boolean isLeapEndMonth) {
        mLunarEndYear = year;
        mLunarEndMonth = month;
        mLunarEndDay = day;
        mIsLeapEndMonth = isLeapEndMonth ? LEAP_MONTH : NOT_LEAP_MONTH;
    }

    public int[] getLunarEndDate() {
        return new int[]{mLunarEndYear, mLunarEndMonth, mLunarEndDay, mIsLeapEndMonth};
    }

    private boolean isFarsiLanguage() {
        return "fa".equals(mCurrentLocale.getLanguage());
    }

    private boolean isSimplifiedChinese() {
        return mCurrentLocale.getLanguage().equals(Locale.SIMPLIFIED_CHINESE.getLanguage())
                && mCurrentLocale.getCountry().equals(Locale.SIMPLIFIED_CHINESE.getCountry());
    }

    private boolean isTibetanLanguage() {
        Locale locale = Locale.getDefault();
        return locale != null && "bo".equals(locale.getLanguage());
    }

    @Override
    public void setGravity(int gravity) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mDatePickerSpinner.getLayoutParams();
        lp.gravity = gravity;
        mDatePickerSpinner.setLayoutParams(lp);
    }

    public void setMargin(int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mDatePickerSpinner.getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        mDatePickerSpinner.setLayoutParams(lp);
    }

    public void setHeightForDialog() {
        mIsHeightSetForDialog = true;
        mPickerViewHeight = getResources().getDimensionPixelOffset(R.dimen.sesl_spinning_date_picker_height_dialog);
        mPickerView.setLayoutParams(new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, mPickerViewHeight));
        mSupportShortSpinnerHeight = false;
    }

    public void setSupportShortSpinnerHeight() {
        mSupportShortSpinnerHeight = true;
        mPickerViewHeight = LayoutParams.WRAP_CONTENT;
        mPickerView.setLayoutParams(new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, mPickerViewHeight));
        mIsHeightSetForDialog = false;
    }

    private String getMonthViewColorStringForSpecific() {
        try {
            if ("wifi-only"
                    .equalsIgnoreCase(SeslSystemPropertiesReflector
                            .getStringProperties("ro.carrier"))) {
                String countryIsoCode = SeslSystemPropertiesReflector
                        .getStringProperties("persist.sys.selected_country_iso");
                if (TextUtils.isEmpty(countryIsoCode)
                        && UAE_SALES_CODE.equals(SeslSystemPropertiesReflector.getSalesCode())) {
                    return null;
                }
                if (TextUtils.isEmpty(countryIsoCode)) {
                    countryIsoCode = SeslSystemPropertiesReflector
                            .getStringProperties("ro.csc.countryiso_code");
                }
                if ("AE".equals(countryIsoCode)) {
                    return UAE_WEEK_DAY_STRING_FEATURE;
                }
            } else if (UAE_SALES_CODE.equals(SeslSystemPropertiesReflector.getSalesCode())) {
                TelephonyManager manager
                        = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                final String simOperator = manager.getSimOperator();
                if (simOperator != null && simOperator.length() > 3
                        && Integer.parseInt(simOperator.substring(0, 3)) == UAE_MCC) {
                    return UAE_WEEK_DAY_STRING_FEATURE;
                }
            }
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "msg : " + e.getMessage());
        }

        return null;
    }
}
