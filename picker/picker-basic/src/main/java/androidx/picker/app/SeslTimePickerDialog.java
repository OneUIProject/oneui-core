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

package androidx.picker.app;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.util.SeslMisc;
import androidx.picker.R;
import androidx.picker.util.SeslAnimationListener;
import androidx.picker.widget.SeslTimePicker;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslTimePickerDialog extends AlertDialog
        implements DialogInterface.OnClickListener, SeslTimePicker.OnTimeChangedListener {

    public interface OnTimeSetListener {
        void onTimeSet(SeslTimePicker view, int hourOfDay, int minute);
    }

    private static final String IS_24_HOUR = "is24hour";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";

    private InputMethodManager mImm;
    private final SeslTimePicker mTimePicker;
    private final OnTimeSetListener mTimeSetListener;

    private final boolean mIs24HourView;
    private boolean mIsStartAnimation;

    private final int mInitialHourOfDay;
    private final int mInitialMinute;

    private final View.OnFocusChangeListener mBtnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (mTimePicker.isEditTextMode() && hasFocus) {
                mTimePicker.setEditTextMode(false);
            }
        }
    };

    public SeslTimePickerDialog(Context context,
                            OnTimeSetListener callBack,
                            int hourOfDay, int minute, boolean is24HourView) {
        this(context, 0, callBack, hourOfDay, minute, is24HourView);
    }

    static int resolveDialogTheme(Context context, int resid) {
        if (resid != 0) {
            return resid;
        }
        return SeslMisc.isLightTheme(context) ?
                R.style.Theme_AppCompat_Light_PickerDialog_TimePicker
                : R.style.Theme_AppCompat_PickerDialog_TimePicker;
    }

    public SeslTimePickerDialog(Context context,
                            int theme,
                            OnTimeSetListener callBack,
                            int hourOfDay, int minute, boolean is24HourView) {
        super(context, resolveDialogTheme(context, theme));
        mTimeSetListener = callBack;
        mInitialHourOfDay = hourOfDay;
        mInitialMinute = minute;
        mIs24HourView = is24HourView;

        Context themeContext = getContext();

        LayoutInflater inflater = LayoutInflater.from(themeContext);
        View view = inflater.inflate(R.layout.sesl_time_picker_spinner_dialog, null);
        setView(view);
        setButton(BUTTON_POSITIVE, themeContext.getString(R.string.sesl_picker_done), this);
        setButton(BUTTON_NEGATIVE, themeContext.getString(R.string.sesl_picker_cancel), this);
        mTimePicker = (SeslTimePicker) view.findViewById(R.id.timePicker);

        mTimePicker.setIs24HourView(mIs24HourView);
        mTimePicker.setHour(mInitialHourOfDay);
        mTimePicker.setMinute(mInitialMinute);
        mTimePicker.setOnTimeChangedListener(this);

        setTitle(R.string.sesl_time_picker_set_title);

        mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getButton(BUTTON_POSITIVE).setOnFocusChangeListener(mBtnFocusChangeListener);
        getButton(BUTTON_NEGATIVE).setOnFocusChangeListener(mBtnFocusChangeListener);

        mIsStartAnimation = true;
        mTimePicker.startAnimation(283, new SeslAnimationListener() {
            @Override
            public void onAnimationEnd() {
                mIsStartAnimation = false;
            }
        });
    }

    @Override
    public void onTimeChanged(SeslTimePicker view, int hourOfDay, int minute) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_NEGATIVE:
                if (mImm != null) {
                    mImm.hideSoftInputFromWindow(
                            getWindow().getDecorView().getWindowToken(), 0);
                }
                cancel();
                break;
            case BUTTON_POSITIVE:
                if (!mIsStartAnimation) {
                    if (mTimeSetListener != null) {
                        mTimePicker.clearFocus();
                        mTimeSetListener.onTimeSet(mTimePicker,
                                mTimePicker.getHour(), mTimePicker.getMinute());
                    }
                    if (mImm != null) {
                        mImm.hideSoftInputFromWindow(
                                getWindow().getDecorView().getWindowToken(), 0);
                    }
                    dismiss();
                }
                break;
        }
    }

    public void updateTime(int hourOfDay, int minutOfHour) {
        mTimePicker.setHour(hourOfDay);
        mTimePicker.setMinute(minutOfHour);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(HOUR, mTimePicker.getHour());
        state.putInt(MINUTE, mTimePicker.getMinute());
        state.putBoolean(IS_24_HOUR, mTimePicker.is24HourView());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int hour = savedInstanceState.getInt(HOUR);
        final int minute = savedInstanceState.getInt(MINUTE);
        mTimePicker.setIs24HourView(savedInstanceState.getBoolean(IS_24_HOUR));
        mTimePicker.setHour(hour);
        mTimePicker.setMinute(minute);
    }
}
