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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.util.SeslMisc;
import androidx.picker.R;
import androidx.picker.widget.SeslDatePicker;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslDatePickerDialog extends AlertDialog
        implements DialogInterface.OnClickListener, SeslDatePicker.OnDateChangedListener {

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";

    private final SeslDatePicker mDatePicker;
    private final OnDateSetListener mDateSetListener;
    private InputMethodManager mImm;

    private final View.OnFocusChangeListener mBtnFocusChangeListener
            = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (mDatePicker.isEditTextMode() && hasFocus) {
                mDatePicker.setEditTextMode(false);
            }
        }
    };

    private final SeslDatePicker.ValidationCallback mValidationCallback
            = new SeslDatePicker.ValidationCallback() {
        @Override
        public void onValidationChanged(boolean valid) {
            Button positive = getButton(BUTTON_POSITIVE);
            if (positive != null) {
                positive.setEnabled(valid);
            }
        }
    };

    public interface OnDateSetListener {
        void onDateSet(SeslDatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    public SeslDatePickerDialog(@NonNull Context context,
                            @Nullable OnDateSetListener callBack,
                            int year,
                            int monthOfYear,
                            int dayOfMonth) {
        this(context, 0, callBack, year, monthOfYear, dayOfMonth);
    }

    public SeslDatePickerDialog(@NonNull Context context,
                            @StyleRes int theme,
                            @Nullable OnDateSetListener callBack,
                            int year,
                            int monthOfYear,
                            int dayOfMonth) {
        super(context, resolveDialogTheme(context, theme));

        Context themeContext = getContext();

        LayoutInflater inflater = LayoutInflater.from(themeContext);
        View view = inflater.inflate(R.layout.sesl_date_picker_dialog, null);
        setView(view);
        setButton(BUTTON_POSITIVE, themeContext.getString(R.string.sesl_picker_done), this);
        setButton(BUTTON_NEGATIVE, themeContext.getString(R.string.sesl_picker_cancel), this);
        mDatePicker = (SeslDatePicker) view.findViewById(R.id.sesl_datePicker);

        mDatePicker.init(year, monthOfYear, dayOfMonth, this);
        mDatePicker.setValidationCallback(mValidationCallback);
        mDatePicker.setDialogWindow(getWindow());
        mDatePicker.setDialogPaddingVertical(view.getPaddingTop() + view.getPaddingBottom());

        mDateSetListener = callBack;
        mImm = (InputMethodManager) themeContext.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    static int resolveDialogTheme(@NonNull Context context, @StyleRes int resid) {
        if (resid == 0) {
            return SeslMisc.isLightTheme(context) ?
                    R.style.Theme_AppCompat_Light_PickerDialog_DatePicker
                    : R.style.Theme_AppCompat_PickerDialog_DatePicker;
        }
        return resid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getButton(BUTTON_POSITIVE).setOnFocusChangeListener(mBtnFocusChangeListener);
        getButton(BUTTON_NEGATIVE).setOnFocusChangeListener(mBtnFocusChangeListener);
    }

    @Override
    public void onDateChanged(SeslDatePicker view, int year, int month, int day) {
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        if (mImm != null) {
            mImm.hideSoftInputFromWindow(
                    getWindow().getDecorView().getWindowToken(), 0);
        }

        switch (which) {
            case BUTTON_NEGATIVE:
                cancel();
                break;
            case BUTTON_POSITIVE:
                if (mDateSetListener != null) {
                    mDatePicker.clearFocus();
                    mDateSetListener.onDateSet(mDatePicker,
                            mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
                }
                break;
        }
    }

    @NonNull
    public SeslDatePicker getDatePicker() {
        return mDatePicker;
    }

    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(YEAR, mDatePicker.getYear());
        state.putInt(MONTH, mDatePicker.getMonth());
        state.putInt(DAY, mDatePicker.getDayOfMonth());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int year = savedInstanceState.getInt(YEAR);
        final int month = savedInstanceState.getInt(MONTH);
        final int day = savedInstanceState.getInt(DAY);
        mDatePicker.init(year, month, day, this);
    }
}
