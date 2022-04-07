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
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RestrictTo;
import androidx.picker.R;
import androidx.reflect.lunarcalendar.SeslFeatureReflector;
import androidx.reflect.lunarcalendar.SeslSolarLunarTablesReflector;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY)
public class SeslDatePickerSpinnerLayout extends LinearLayout {
    private static final int FORMAT_DDMMYYYY = 1;
    private static final int FORMAT_MMDDYYYY = 0;
    private static final int FORMAT_YYYYDDMM = 3;
    private static final int FORMAT_YYYYMMDD = 2;
    private static final int HUNGARIAN_MONTH_TEXT_SIZE_DIFF = 4;
    private static final int PICKER_DAY = 0;
    private static final int PICKER_MONTH = 1;
    private static final int PICKER_YEAR = 2;
    private static final boolean SESL_DEBUG = false;
    private static final String TAG = "SeslDatePickerSpinnerLayout";
    private Context mContext;
    private Calendar mCurrentDate;
    private Locale mCurrentLocale;
    private SeslDatePicker mDatePicker;
    private final View mDayPaddingView;
    private final SeslNumberPicker mDaySpinner;
    private final EditText mDaySpinnerInput;
    private boolean mIsEditTextMode;
    private boolean mIsLeapMonth = false;
    private boolean mIsLunar = false;
    private int mLunarCurrentDay;
    private int mLunarCurrentMonth;
    private int mLunarCurrentYear;
    private int mLunarTempDay;
    private int mLunarTempMonth;
    private int mLunarTempYear;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private final SeslNumberPicker mMonthSpinner;
    private final EditText mMonthSpinnerInput;
    private int mNumberOfMonths;
    private SeslDatePicker.OnEditTextModeChangedListener mOnEditTextModeChangedListener;
    private OnSpinnerDateChangedListener mOnSpinnerDateChangedListener;
    PathClassLoader mPathClassLoader = null;
    private EditText[] mPickerTexts = new EditText[3];
    private String[] mShortMonths;
    private Object mSolarLunarTables;
    private final LinearLayout mSpinners;
    private Calendar mTempDate;
    private Toast mToast;
    private String mToastText;
    private final View mYearPaddingView;
    private final SeslNumberPicker mYearSpinner;
    private final EditText mYearSpinnerInput;

    private TextView.OnEditorActionListener mEditorActionListener
            = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateInputState();
                setEditTextMode(false);
            }
            return false;
        }
    };

    private SeslNumberPicker.OnEditTextModeChangedListener mModeChangeListener
            = new SeslNumberPicker.OnEditTextModeChangedListener() {
        @Override
        public void onEditTextModeChanged(SeslNumberPicker numberPicker, boolean edit) {
            setEditTextMode(edit);
            updateModeState(edit);
        }
    };

    public interface OnSpinnerDateChangedListener {
        void onDateChanged(SeslDatePickerSpinnerLayout view, int year, int monthOfYear, int dayOfMonth);
    }

    public SeslDatePickerSpinnerLayout(Context context) {
        this(context, null);
    }

    public SeslDatePickerSpinnerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.datePickerStyle);
    }

    public SeslDatePickerSpinnerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    // TODO rework this method
    public SeslDatePickerSpinnerLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.sesl_date_picker_spinner, this, true);

        mCurrentLocale = Locale.getDefault();
        setCurrentLocale(mCurrentLocale);

        // kang
        SeslNumberPicker.OnValueChangeListener onChangeListener = new SeslNumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(SeslNumberPicker var1, int var2, int var3) {
                mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
                if (mIsLunar) {
                    mLunarTempYear = mLunarCurrentYear;
                    mLunarTempMonth = mLunarCurrentMonth;
                    mLunarTempDay = mLunarCurrentDay;
                }

                int var5;
                boolean var6;
                boolean var7;
                label96: {
                    Calendar var9;
                    if (var1.equals(mDaySpinner)) {
                        var5 = mTempDate.getActualMaximum(5);
                        if (mIsLunar) {
                            var5 = getLunarMaxDayOfMonth(mTempDate.get(1), mTempDate.get(2), mIsLeapMonth);
                        }

                        if (var2 == var5 && var3 == 1 || var2 == 1 && var3 == var5) {
                            mTempDate.set(5, var3);
                            if (mIsLunar) {
                                mLunarTempDay = var3;
                            }
                        } else {
                            var9 = mTempDate;
                            var2 = var3 - var2;
                            var9.add(5, var2);
                            if (mIsLunar) {
                                mLunarTempDay += var2;
                            }
                        }

                        var6 = false;
                    } else {
                        if (var1.equals(mMonthSpinner)) {
                            if (var2 == 11 && var3 == 0 || var2 == 0 && var3 == 11) {
                                mTempDate.set(2, var3);
                                if (mIsLunar) {
                                    mLunarTempMonth = var3;
                                }
                            } else {
                                var9 = mTempDate;
                                var2 = var3 - var2;
                                var9.add(2, var2);
                                if (mIsLunar) {
                                    mLunarTempMonth += var2;
                                }
                            }

                            var6 = false;
                            var7 = true;
                            break label96;
                        }

                        if (!var1.equals(mYearSpinner)) {
                            throw new IllegalArgumentException();
                        }

                        var9 = mTempDate;
                        var2 = var3 - var2;
                        var9.add(1, var2);
                        if (mIsLunar) {
                            mLunarTempYear += var2;
                        }

                        var6 = true;
                    }

                    var7 = var6;
                }

                if (mIsLunar) {
                    var2 = getLunarMaxDayOfMonth(mLunarTempYear, mLunarTempMonth, mIsLeapMonth);
                    if (mLunarTempDay > var2) {
                        mLunarTempDay = var2;
                    }

                    if (mIsLeapMonth) {
                        mIsLeapMonth = SeslSolarLunarTablesReflector.isLeapMonth(mPathClassLoader, mSolarLunarTables, mLunarTempYear, mLunarTempMonth);
                    }
                }

                var5 = mTempDate.get(1);
                var2 = mTempDate.get(2);
                var3 = mTempDate.get(5);
                if (mIsLunar) {
                    var5 = mLunarTempYear;
                    var2 = mLunarTempMonth;
                    var3 = mLunarTempDay;
                }

                setDate(var5, var2, var3);
                if (var6 || var7) {
                    updateSpinners(false, false, var6, var7);
                }

                notifyDateChanged();
            }
        };
        // kang

        mSpinners = findViewById(R.id.sesl_date_picker_pickers);
        mDayPaddingView = findViewById(R.id.sesl_date_picker_spinner_day_padding);
        mYearPaddingView = findViewById(R.id.sesl_date_picker_spinner_year_padding);

        mDaySpinner = findViewById(R.id.sesl_date_picker_spinner_day);
        mDaySpinnerInput = mDaySpinner.findViewById(R.id.numberpicker_input);
        mDaySpinner.setFormatter(SeslNumberPicker.getTwoDigitFormatter());
        mDaySpinner.setOnValueChangedListener(onChangeListener);
        mDaySpinner.setOnEditTextModeChangedListener(mModeChangeListener);
        mDaySpinner.setMaxInputLength(2);
        mDaySpinner.setYearDateTimeInputMode();

        mMonthSpinner = findViewById(R.id.sesl_date_picker_spinner_month);
        mMonthSpinnerInput = mMonthSpinner.findViewById(R.id.numberpicker_input);
        if (usingNumericMonths()) {
            mMonthSpinner.setMinValue(1);
            mMonthSpinner.setMaxValue(12);
            mMonthSpinner.setYearDateTimeInputMode();
            mMonthSpinner.setMaxInputLength(2);
        } else {
            mMonthSpinner.setMinValue(0);
            mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
            mMonthSpinner.setFormatter(null);
            mMonthSpinner.setDisplayedValues(mShortMonths);
            mMonthSpinnerInput.setInputType(EditorInfo.TYPE_CLASS_TEXT);
            mMonthSpinner.setMonthInputMode();
        }
        mMonthSpinner.setOnValueChangedListener(onChangeListener);
        mMonthSpinner.setOnEditTextModeChangedListener(mModeChangeListener);

        mYearSpinner = findViewById(R.id.sesl_date_picker_spinner_year);
        mYearSpinnerInput = mYearSpinner.findViewById(R.id.numberpicker_input);
        mYearSpinner.setOnValueChangedListener(onChangeListener);
        mYearSpinner.setOnEditTextModeChangedListener(mModeChangeListener);
        mYearSpinner.setMaxInputLength(4);
        mYearSpinner.setYearDateTimeInputMode();

        Typeface datePickerTypeface = Typeface.create("sec-roboto-light", Typeface.BOLD);
        mDaySpinner.setTextTypeface(datePickerTypeface);
        mMonthSpinner.setTextTypeface(datePickerTypeface);
        mYearSpinner.setTextTypeface(datePickerTypeface);

        final Resources res = context.getResources();

        final int numberTextSize
                = res.getInteger(R.integer.sesl_date_picker_spinner_number_text_size);
        final int numberTextSizeWithUnit
                = res.getInteger(R.integer.sesl_date_picker_spinner_number_text_size_with_unit);
        int textSize = numberTextSize;

        mToastText = res.getString(R.string.sesl_number_picker_invalid_value_entered);

        mDaySpinner.setTextSize(numberTextSize);
        mYearSpinner.setTextSize(numberTextSize);
        String language = mCurrentLocale.getLanguage();
        if ("my".equals(language) || "ml".equals(language) || "bn".equals(language) || "ar".equals(language) || "fa".equals(language)) {
            textSize = res.getInteger(R.integer.sesl_date_picker_spinner_long_month_text_size);
        } else if ("ga".equals(language)) {
            textSize = res.getInteger(R.integer.sesl_date_picker_spinner_long_month_text_size) - 1;
        } else if ("hu".equals(language)) {
            textSize -= HUNGARIAN_MONTH_TEXT_SIZE_DIFF;
        }
        if (usingNumericMonths()) {
            mMonthSpinner.setTextSize(textSize);
        } else {
            mMonthSpinner.setTextSize(textSize);
        }
        if ("ko".equals(language) || "zh".equals(language) || "ja".equals(language)) {
            mDaySpinner.setTextSize(numberTextSizeWithUnit);
            mMonthSpinner.setTextSize(numberTextSizeWithUnit);
            mYearSpinner.setTextSize(numberTextSizeWithUnit);
            mDaySpinner.setDateUnit(SeslNumberPicker.MODE_UNIT_DAY);
            mMonthSpinner.setDateUnit(SeslNumberPicker.MODE_UNIT_MONTH);
            mYearSpinner.setDateUnit(SeslNumberPicker.MODE_UNIT_YEAR);
        }

        mDaySpinner.setPickerContentDescription(
                context.getResources().getString(R.string.sesl_date_picker_day));
        mMonthSpinner.setPickerContentDescription(
                context.getResources().getString(R.string.sesl_date_picker_month));
        mYearSpinner.setPickerContentDescription(
                context.getResources().getString(R.string.sesl_date_picker_year));

        mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        init(mCurrentDate.get(Calendar.YEAR),
                mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH));

        reorderSpinners();
    }

    void init(int year, int monthOfYear, int dayOfMonth) {
        setDate(year, monthOfYear, dayOfMonth);
        updateSpinners(true, true, true, true);
    }

    void updateDate(int year, int month, int dayOfMonth) {
        if (isNewDate(year, month, dayOfMonth)) {
            setDate(year, month, dayOfMonth);
            updateSpinners(true, true, true, true);
        }
    }

    int getYear() {
        if (mIsLunar) {
            return mLunarCurrentYear;
        }
        return mCurrentDate.get(Calendar.YEAR);
    }

    int getMonth() {
        if (mIsLunar) {
            return mLunarCurrentMonth;
        }
        return mCurrentDate.get(Calendar.MONTH);
    }

    int getDayOfMonth() {
        if (mIsLunar) {
            return mLunarCurrentDay;
        }
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    void setMinDate(long minDate) {
        mMinDate.setTimeInMillis(minDate);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        }
        updateSpinners(true, true, true, true);
    }

    Calendar getMinDate() {
        return mMinDate;
    }

    void setMaxDate(long maxDate) {
        mMaxDate.setTimeInMillis(maxDate);
        if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
        updateSpinners(true, true, true, true);
    }

    Calendar getMaxDate() {
        return mMaxDate;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mDaySpinner.setEnabled(enabled);
        mMonthSpinner.setEnabled(enabled);
        mYearSpinner.setEnabled(enabled);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCurrentLocale(newConfig.locale);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        String selectedDateUtterance = DateUtils.formatDateTime(mContext,
                mCurrentDate.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
        event.getText().add(selectedDateUtterance);
    }

    protected void setCurrentLocale(Locale locale) {
        mTempDate = getCalendarForLocale(mTempDate, locale);
        mMinDate = getCalendarForLocale(mMinDate, locale);
        mMaxDate = getCalendarForLocale(mMaxDate, locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale);

        mNumberOfMonths = mTempDate.getActualMaximum(Calendar.MONTH) + 1;
        mShortMonths = new DateFormatSymbols().getShortMonths();

        for (int i = 0; i < mShortMonths.length; i++) {
            mShortMonths[i] = mShortMonths[i].toUpperCase();
        }

        if (usingNumericMonths()) {
            mShortMonths = new String[mNumberOfMonths];
            for (int i = 0; i < mNumberOfMonths; i++) {
                mShortMonths[i] = String.format("%d", i + 1);
            }
        }
    }

    private boolean usingNumericMonths() {
        return Character.isDigit(mShortMonths[0].charAt(0));
    }

    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    private void reorderSpinners() {
        mSpinners.removeAllViews();
        char[] order = DateFormat.getDateFormatOrder(mContext);
        final int spinnerCount = order.length;
        for (int i = 0; i < spinnerCount; i++) {
            switch (order[i]) {
                case 'M':
                    mSpinners.addView(mMonthSpinner);
                    setImeOptions(mMonthSpinner, spinnerCount, i);
                    break;
                case 'd':
                    mSpinners.addView(mDaySpinner);
                    setImeOptions(mDaySpinner, spinnerCount, i);
                    break;
                case 'y':
                    mSpinners.addView(mYearSpinner);
                    setImeOptions(mYearSpinner, spinnerCount, i);
                    break;
                default:
                    throw new IllegalArgumentException(Arrays.toString(order));
            }
        }

        if (order[0] == 'y') {
            mSpinners.addView(mYearPaddingView, 0);
            mSpinners.addView(mDayPaddingView);
        } else {
            mSpinners.addView(mDayPaddingView, 0);
            mSpinners.addView(mYearPaddingView);
        }

        switch (order[0]) {
            case 'M':
                setTextWatcher(FORMAT_MMDDYYYY);
                break;
            case 'd':
                setTextWatcher(FORMAT_DDMMYYYY);
                break;
            case 'y':
                if (order[1]  == 'd') {
                    setTextWatcher(FORMAT_YYYYDDMM);
                } else {
                    setTextWatcher(FORMAT_YYYYMMDD);
                }
                break;
        }
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != month
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != dayOfMonth;
    }

    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(year, month, dayOfMonth);

        if (mIsLunar) {
            mLunarCurrentYear = year;
            mLunarCurrentMonth = month;
            mLunarCurrentDay = dayOfMonth;
        }

        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
    }

    // TODO rework this method
    // kang
    private void updateSpinners(boolean var1, boolean var2, boolean var3, boolean var4) {
        /* var1 = set; var2 = yearChanged; var3 = monthChanged; var4 = dayChanged; */
        if (var2) {
            this.mYearSpinner.setMinValue(this.mMinDate.get(1));
            this.mYearSpinner.setMaxValue(this.mMaxDate.get(1));
            this.mYearSpinner.setWrapSelectorWheel(false);
        }

        int var5;
        int var6;
        int var7;
        int var8;
        if (var3) {
            var5 = this.mMinDate.get(1);
            var6 = this.mMaxDate.get(1);
            var7 = 11;
            if (var6 - var5 == 0) {
                var5 = this.mMinDate.get(2);
                var7 = this.mMaxDate.get(2);
            } else {
                var5 = this.mCurrentDate.get(1);
                if (this.mIsLunar) {
                    var5 = this.mLunarCurrentYear;
                }

                if (var5 == this.mMinDate.get(1)) {
                    var5 = this.mMinDate.get(2);
                    var7 = 11;
                } else {
                    if (var5 == this.mMaxDate.get(1)) {
                        var7 = this.mMaxDate.get(2);
                    }

                    var5 = 0;
                }
            }

            var8 = var7;
            var6 = var5;
            if (this.usingNumericMonths()) {
                var6 = var5 + 1;
                var8 = var7 + 1;
            }

            this.mMonthSpinner.setDisplayedValues((String[])null);
            this.mMonthSpinner.setMinValue(var6);
            this.mMonthSpinner.setMaxValue(var8);
            if (!this.usingNumericMonths()) {
                String[] var9 = (String[])Arrays.copyOfRange(this.mShortMonths, this.mMonthSpinner.getMinValue(), this.mMonthSpinner.getMaxValue() + 1);
                this.mMonthSpinner.setDisplayedValues(var9);
            }
        }

        if (var4) {
            var8 = this.mMinDate.get(1);
            var5 = this.mMaxDate.get(1);
            var7 = this.mMinDate.get(2);
            var6 = this.mMaxDate.get(2);
            if (var5 - var8 == 0 && var6 - var7 == 0) {
                var5 = this.mMinDate.get(5);
                var7 = this.mMaxDate.get(5);
            } else {
                var6 = this.mCurrentDate.get(1);
                var5 = this.mCurrentDate.get(2);
                if (this.mIsLunar) {
                    var6 = this.mLunarCurrentYear;
                    var5 = this.mLunarCurrentMonth;
                }

                if (var6 == this.mMinDate.get(1) && var5 == this.mMinDate.get(2)) {
                    var7 = this.mMinDate.get(5);
                    var8 = this.mCurrentDate.getActualMaximum(5);
                    if (this.mIsLunar) {
                        var6 = this.getLunarMaxDayOfMonth(var6, var5, this.mIsLeapMonth);
                        var5 = var7;
                        var7 = var6;
                    } else {
                        var5 = var7;
                        var7 = var8;
                    }
                } else {
                    label121: {
                        label122: {
                            if (var6 == this.mMaxDate.get(1) && var5 == this.mMaxDate.get(2)) {
                                var8 = this.mMaxDate.get(5);
                                var7 = var8;
                                if (this.mIsLunar) {
                                    var7 = Math.min(var8, this.getLunarMaxDayOfMonth(var6, var5, this.mIsLeapMonth));
                                    break label122;
                                }
                            } else {
                                var7 = this.mCurrentDate.getActualMaximum(5);
                                if (this.mIsLunar) {
                                    var7 = this.getLunarMaxDayOfMonth(var6, var5, this.mIsLeapMonth);
                                    break label122;
                                }
                            }

                            var5 = 1;
                            break label121;
                        }

                        var5 = 1;
                    }
                }
            }

            this.mDaySpinner.setMinValue(var5);
            this.mDaySpinner.setMaxValue(var7);
        }

        if (var1) {
            this.mYearSpinner.setValue(this.mCurrentDate.get(1));
            var7 = this.mCurrentDate.get(2);
            if (this.mIsLunar) {
                var7 = this.mLunarCurrentMonth;
            }

            if (this.usingNumericMonths()) {
                this.mMonthSpinner.setValue(var7 + 1);
            } else {
                this.mMonthSpinner.setValue(var7);
            }

            var7 = this.mCurrentDate.get(5);
            if (this.mIsLunar) {
                var7 = this.mLunarCurrentDay;
            }

            this.mDaySpinner.setValue(var7);
            if (this.usingNumericMonths()) {
                this.mMonthSpinnerInput.setRawInputType(2);
            }

            if (this.mIsEditTextMode) {
                EditText[] var11 = this.mPickerTexts;
                if (var11 != null) {
                    var5 = var11.length;

                    for(var7 = 0; var7 < var5; ++var7) {
                        EditText var10 = var11[var7];
                        if (var10.hasFocus()) {
                            var10.setSelection(0, 0);
                            var10.selectAll();
                            break;
                        }
                    }
                }
            }

        }
    }
    // kang

    private void notifyDateChanged() {
        if (mOnSpinnerDateChangedListener != null) {
            mOnSpinnerDateChangedListener.onDateChanged(this,
                    getYear(), getMonth(), getDayOfMonth());
        }
    }

    private void setImeOptions(SeslNumberPicker spinner, int spinnerCount, int spinnerIndex) {
        final int imeOptions;
        if (spinnerIndex < spinnerCount - 1) {
            imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_NEXT;
        } else {
            imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_DONE;
        }

        TextView input = (TextView) spinner.findViewById(R.id.numberpicker_input);
        input.setImeOptions(imeOptions);
    }

    void updateInputState() {
        InputMethodManager inputMethodManager
                = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager == null) {
            return;
        }

        if (inputMethodManager.isActive(mYearSpinnerInput)) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            mYearSpinnerInput.clearFocus();
        } else if (inputMethodManager.isActive(mMonthSpinnerInput)) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            mMonthSpinnerInput.clearFocus();
        } else if (inputMethodManager.isActive(mDaySpinnerInput)) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            mDaySpinnerInput.clearFocus();
        }
    }

    private void setTextWatcher(int format) {
        seslLog("setTextWatcher() usingNumericMonths  : " + usingNumericMonths()
                + "format  : " + format);

        int yIdx = -1;
        int mIdx = -1;
        int dIdx = -1;
        switch (format) {
            case FORMAT_MMDDYYYY:
                mIdx = 0;
                dIdx = 1;
                yIdx = 2;
                break;
            case FORMAT_DDMMYYYY:
                dIdx = 0;
                mIdx = 1;
                yIdx = 2;
                break;
            case FORMAT_YYYYMMDD:
                yIdx = 0;
                mIdx = 1;
                dIdx = 2;
                break;
            case FORMAT_YYYYDDMM:
                yIdx = 0;
                dIdx = 1;
                mIdx = 2;
                break;
        }

        mPickerTexts[yIdx] = mYearSpinner.getEditText();
        mPickerTexts[mIdx] = mMonthSpinner.getEditText();
        mPickerTexts[dIdx] = mDaySpinner.getEditText();

        mPickerTexts[yIdx].addTextChangedListener(new SeslTextWatcher(4, yIdx, false));
        if (usingNumericMonths()) {
            mPickerTexts[mIdx].addTextChangedListener(new SeslTextWatcher(2, mIdx, true));
        } else {
            mPickerTexts[mIdx].addTextChangedListener(new SeslTextWatcher(3, mIdx, true));
        }
        mPickerTexts[dIdx].addTextChangedListener(new SeslTextWatcher(2, dIdx, false));

        if (format != 3 || usingNumericMonths()) {
            mPickerTexts[mPickerTexts.length - 1].setOnEditorActionListener(mEditorActionListener);
        }

        mPickerTexts[yIdx].setOnKeyListener(new SeslKeyListener());
        mPickerTexts[mIdx].setOnKeyListener(new SeslKeyListener());
        mPickerTexts[dIdx].setOnKeyListener(new SeslKeyListener());
    }

    private class SeslKeyListener implements View.OnKeyListener {
        public SeslKeyListener() {
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            seslLog(event.toString());

            if (event.getAction() != KeyEvent.ACTION_UP) {
                return false;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    Configuration config = getResources().getConfiguration();
                    if (config.keyboard == Configuration.KEYBOARD_12KEY) {
                        /*return !mContext.getPackageManager().hasSystemFeature("com.sec.feature.dual_lcd")
                                || !mContext.getPackageManager().hasSystemFeature("com.sec.feature.folder_type")
                                || config.keyboard != 2;*/
                    }
                    return false;
                case KeyEvent.KEYCODE_TAB:
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (isEditTextMode()) {
                        if ((((EditText) v).getImeOptions() & EditorInfo.IME_ACTION_NEXT)
                                == EditorInfo.IME_ACTION_NEXT) {
                            View next = FocusFinder.getInstance().findNextFocus(mDatePicker, v, View.FOCUS_FORWARD);
                            if (next == null) {
                                return true;
                            }
                            next.requestFocus();
                        } else if ((((EditText) v).getImeOptions() & EditorInfo.IME_ACTION_DONE)
                                == EditorInfo.IME_ACTION_DONE) {
                            updateInputState();
                            setEditTextMode(false);
                        }
                    }
                    return true;
            }

            return false;
        }
    }

    private void seslLog(String msg) {
        if (SESL_DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private class SeslTextWatcher implements TextWatcher {
        private final int INVALID_POSITION_ID = -1;
        private final int LAST_POSITION_ID = 2;
        private int mChangedLen = 0;
        private int mCheck;
        private int mId;
        private boolean mIsMonth;
        private int mMaxLen;
        private int mNext;
        private String mPrevText;

        private SeslTextWatcher(int maxLen, int id, boolean month) {
            mMaxLen = maxLen;
            mId = id;
            mIsMonth = month;
            mCheck = id - 1;
            if (mCheck < 0) {
                mCheck = LAST_POSITION_ID;
            }
            mNext = id + 1 <= LAST_POSITION_ID ?
                    id + 1 : INVALID_POSITION_ID;
        }

        @Override
        public void afterTextChanged(Editable view) {
            seslLog("[" + mId + "] afterTextChanged: " + view.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            seslLog("[" + mId + "] beforeTextChanged: " + ((Object) s)
                    + ", " + start + ", " + count + ", " + after);
            mPrevText = s.toString();
            mChangedLen = after;
        }

        // TODO rework this method
        // kang
        @Override
        public void onTextChanged(CharSequence var1, int var2, int var3, int var4) {
            /* var1 = s; var2 = start; var3 = before; var4 = count; */

            seslLog("[" + this.mId + "] onTextChanged: " + this.mPrevText + " -> " + var1);
            var2 = var1.length();
            String var5 = var1.toString();
            String var10 = (String)mPickerTexts[this.mId].getTag();
            if (var10 == null || !"onClick".equals(var10) && !"onLongClick".equals(var10)) {
                if (mPickerTexts[this.mId].isFocused()) {
                    boolean var6 = this.mIsMonth;
                    var10 = "";
                    if (var6) {
                        if (usingNumericMonths() && this.mChangedLen == 1) {
                            seslLog("[" + this.mId + "] Samsung Keypad Num Month");
                            var4 = mMonthSpinner.getMinValue();
                            var3 = Integer.parseInt(var5);
                            if (var2 == this.mMaxLen) {
                                if (var3 < var4) {
                                    if (Character.getNumericValue(var5.charAt(0)) < 2) {
                                        this.showInvalidValueEnteredToast(Character.toString(var5.charAt(0)), 1);
                                    } else {
                                        this.showInvalidValueEnteredToast("", 0);
                                    }

                                    return;
                                }

                                this.changeFocus();
                            } else if (var2 > 0) {
                                if (var4 >= 10 && "0".equals(var5)) {
                                    this.showInvalidValueEnteredToast("", 0);
                                    return;
                                }

                                if (!"1".equals(var5) && !"0".equals(var5)) {
                                    if (var3 < var4) {
                                        this.showInvalidValueEnteredToast("", 0);
                                        return;
                                    }

                                    this.changeFocus();
                                }
                            }
                        } else if (!this.isNumericStr(this.mPrevText)) {
                            if (var2 >= this.mMaxLen) {
                                if (this.isMeaLanguage()) {
                                    if (TextUtils.isEmpty(this.mPrevText) && this.isMonthStr(var5)) {
                                        this.changeFocus();
                                    }
                                } else {
                                    this.changeFocus();
                                }
                            } else if ((this.isSwaLanguage() || this.isFarsiLanguage()) && var2 > 0 && !this.isNumericStr(var5)) {
                                this.changeFocus();
                            }
                        }
                    } else if (this.mChangedLen == 1) {
                        if (this.mMaxLen < 3) {
                            var3 = mDaySpinner.getMinValue();
                            var4 = Integer.parseInt(var5);
                            if (this.mPrevText.length() < var2 && var2 == this.mMaxLen) {
                                if (var4 < var3) {
                                    if (Character.getNumericValue(var5.charAt(0)) < 4) {
                                        this.showInvalidValueEnteredToast(Character.toString(var5.charAt(0)), 1);
                                    } else {
                                        this.showInvalidValueEnteredToast("", 0);
                                    }

                                    return;
                                }

                                this.changeFocus();
                            } else {
                                if (var3 >= 10 && var4 == 0 || var3 >= 20 && (var4 == 0 || var4 == 1) || var3 >= 30 && (var4 == 0 || var4 == 1 || var4 == 2)) {
                                    this.showInvalidValueEnteredToast("", 0);
                                    return;
                                }

                                if (var4 > 3) {
                                    if (var4 < var3) {
                                        this.showInvalidValueEnteredToast("", 0);
                                        return;
                                    }

                                    this.changeFocus();
                                }

                                if (usingNumericMonths()) {
                                    var2 = mMonthSpinner.getValue() - 1;
                                } else {
                                    var2 = mMonthSpinner.getValue();
                                }

                                if (!mIsLunar && var2 == 1 && var4 == 3) {
                                    if (var4 < var3) {
                                        this.showInvalidValueEnteredToast("", 0);
                                        return;
                                    }

                                    this.changeFocus();
                                }
                            }
                        } else {
                            var4 = mYearSpinner.getMinValue();
                            int var7 = mYearSpinner.getMaxValue();
                            var3 = Integer.parseInt(var5);
                            if (this.mPrevText.length() < var2 && var2 == this.mMaxLen) {
                                if (var3 < var4 || var3 > var7) {
                                    this.showInvalidValueEnteredToast(var5.substring(0, 3), 3);
                                    return;
                                }

                                if (usingNumericMonths()) {
                                    var2 = mMonthSpinner.getValue() - 1;
                                } else {
                                    var2 = mMonthSpinner.getValue();
                                }

                                mTempDate.clear();
                                mTempDate.set(var3, var2, mDaySpinner.getValue());
                                Calendar var11 = Calendar.getInstance();
                                var11.clear();
                                var11.set(mMinDate.get(1), mMinDate.get(2), mMinDate.get(5));
                                if (mTempDate.before(var11) || mTempDate.after(mMaxDate)) {
                                    this.showInvalidValueEnteredToast(var5.substring(0, 3), 3);
                                    return;
                                }

                                this.changeFocus();
                            } else {
                                int var8 = var2 - 1;
                                int var9 = (int)(1000.0D / Math.pow(10.0D, (double)var8));
                                if (var2 != 1) {
                                    var10 = var5.substring(0, var8);
                                }

                                if (var3 < var4 / var9 || var3 > var7 / var9) {
                                    this.showInvalidValueEnteredToast(var10, var8);
                                }
                            }
                        }
                    }

                }
            } else {
                seslLog("[" + this.mId + "] TAG exists: " + var10);
            }
        }
        // kang

        private void showInvalidValueEnteredToast(String setValue, int selection) {
            mPickerTexts[mId].setText(setValue);
            if (selection != 0) {
                mPickerTexts[mId].setSelection(selection);
            }
            if (mToast == null) {
                mToast = Toast.makeText(mContext, mToastText, Toast.LENGTH_SHORT);
                View view = LayoutInflater.from(mContext).inflate(R.layout.sesl_custom_toast_layout, null);
                ((TextView) view.findViewById(R.id.message)).setText(mToastText);
                mToast.setView(view);
            }
            mToast.show();
        }

        private boolean isSwaLanguage() {
            String language = mCurrentLocale.getLanguage();
            return "hi".equals(language) || "ta".equals(language) || "ml".equals(language)
                    || "te".equals(language) || "or".equals(language) || "ne".equals(language)
                    || "as".equals(language) || "bn".equals(language) || "gu".equals(language)
                    || "si".equals(language) || "pa".equals(language) || "kn".equals(language)
                    || "mr".equals(language);
        }

        private boolean isMeaLanguage() {
            String language = mCurrentLocale.getLanguage();
            return "ar".equals(language) || "fa".equals(language) || "ur".equals(language);
        }

        private boolean isFarsiLanguage() {
            String language = mCurrentLocale.getLanguage();
            return "fa".equals(language);
        }

        private boolean isNumericStr(String s) {
            return !TextUtils.isEmpty(s) && Character.isDigit(s.charAt(0));
        }

        private boolean isMonthStr(String s) {
            for (int i = 0; i < mNumberOfMonths; i++) {
                if (s.equals(mShortMonths[i])) {
                    return true;
                }
            }
            return false;
        }

        private void changeFocus() {
            AccessibilityManager manager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager == null || !manager.isTouchExplorationEnabled()) {
                seslLog("[" + mId + "] changeFocus() mNext : " + mNext + ", mCheck : " + mCheck);
                if (mNext >= 0) {
                    if (!mPickerTexts[mCheck].isFocused()) {
                        mPickerTexts[mNext].requestFocus();
                    }
                    if (mPickerTexts[mId].isFocused()) {
                        mPickerTexts[mId].clearFocus();
                    }
                }
            }
        }
    }

    void setOnSpinnerDateChangedListener(SeslDatePicker picker, OnSpinnerDateChangedListener listener) {
        if (mDatePicker == null) {
            mDatePicker = picker;
        }
        mOnSpinnerDateChangedListener = listener;
    }

    private void updateModeState(boolean mode) {
        if (mIsEditTextMode != mode && !mode) {
            if (mDaySpinner.isEditTextMode()) {
                mDaySpinner.setEditTextMode(false);
            }
            if (mMonthSpinner.isEditTextMode()) {
                mMonthSpinner.setEditTextMode(false);
            }
            if (mYearSpinner.isEditTextMode()) {
                mYearSpinner.setEditTextMode(false);
            }
        }
    }

    void setEditTextMode(boolean editTextMode) {
        if (mIsEditTextMode != editTextMode) {
            mIsEditTextMode = editTextMode;

            InputMethodManager inputMethodManager
                    = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            mDaySpinner.setEditTextMode(editTextMode);
            mMonthSpinner.setEditTextMode(editTextMode);
            mYearSpinner.setEditTextMode(editTextMode);
            if (inputMethodManager != null) {
                if (!mIsEditTextMode) {
                    inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
                } else {
                    inputMethodManager.showSoftInput(mDaySpinner, 0);
                }
            }

            if (mOnEditTextModeChangedListener != null) {
                mOnEditTextModeChangedListener.onEditTextModeChanged(mDatePicker, editTextMode);
            }
        }
    }

    boolean isEditTextMode() {
        return mIsEditTextMode;
    }

    void setOnEditTextModeChangedListener(SeslDatePicker picker,
                                          SeslDatePicker.OnEditTextModeChangedListener onEditModeChangedListener) {
        if (mDatePicker == null) {
            mDatePicker = picker;
        }
        mOnEditTextModeChangedListener = onEditModeChangedListener;
    }

    EditText getEditText(int picker) {
        switch (picker) {
            case PICKER_DAY:
                return mDaySpinner.getEditText();
            case PICKER_MONTH:
                return mMonthSpinner.getEditText();
            case PICKER_YEAR:
                return mYearSpinner.getEditText();
            default:
                return mDaySpinner.getEditText();
        }
    }

    SeslNumberPicker getNumberPicker(int picker) {
        switch (picker) {
            case PICKER_DAY:
                return mDaySpinner;
            case PICKER_MONTH:
                return mMonthSpinner;
            case PICKER_YEAR:
                return mYearSpinner;
            default:
                return mDaySpinner;
        }
    }

    void setLunar(boolean isLunar, boolean isLeapMonth, PathClassLoader pathClassLoader) {
        mIsLunar = isLunar;
        mIsLeapMonth = isLeapMonth;
        if (isLunar && mPathClassLoader == null) {
            mPathClassLoader = pathClassLoader;
            mSolarLunarTables = SeslFeatureReflector.getSolarLunarTables(pathClassLoader);
        }
        updateSpinners(false, true, true, true);
    }

    void setIsLeapMonth(boolean isLeapMonth) {
        mIsLeapMonth = isLeapMonth;
    }

    private int getLunarMaxDayOfMonth(int year, int month, boolean isLeapMonth) {
        if (mSolarLunarTables == null) {
            return 0;
        }
        return SeslSolarLunarTablesReflector
                .getDayLengthOf(mPathClassLoader, mSolarLunarTables, year, month, isLeapMonth);
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (mDaySpinner != null) {
            mDaySpinner.requestLayout();
        }
        if (mYearSpinner != null) {
            mYearSpinner.requestLayout();
        }
        if (mMonthSpinner != null) {
            mMonthSpinner.requestLayout();
        }
    }
}
