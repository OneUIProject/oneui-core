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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.picker.R;
import androidx.picker.widget.SeslColorPicker;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslColorPickerDialog extends AlertDialog
        implements DialogInterface.OnClickListener {
    private static final String TAG = "SeslColorPickerDialog";
    private final SeslColorPicker mColorPicker;
    private Integer mCurrentColor = null;
    private final OnColorSetListener mOnColorSetListener;

    public interface OnColorSetListener {
        void onColorSet(int color);
    }

    public SeslColorPickerDialog(Context context, OnColorSetListener listener) {
        super(context, resolveDialogTheme(context));
        // Ensure we are using the correctly themed context rather than the context that was passed in.
        context = getContext();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.sesl_color_picker_dialog, null);
        setView(view);

        setButton(BUTTON_POSITIVE, context.getString(R.string.sesl_picker_done), this);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.sesl_picker_cancel), this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mOnColorSetListener = listener;
        mColorPicker = view.findViewById(R.id.sesl_color_picker_content_view);
    }

    public SeslColorPickerDialog(Context context, OnColorSetListener listener,
                                 int currentColor) {
        this(context, listener);
        mColorPicker.getRecentColorInfo().setCurrentColor(currentColor);
        mCurrentColor = currentColor;
        mColorPicker.updateRecentColorLayout();
    }

    public SeslColorPickerDialog(Context context, OnColorSetListener listener,
                                 int[] recentlyUsedColors) {
        this(context, listener);
        mColorPicker.getRecentColorInfo().initRecentColorInfo(recentlyUsedColors);
        mColorPicker.updateRecentColorLayout();
    }

    public SeslColorPickerDialog(Context context, OnColorSetListener onColorSetListener,
                                 int currentColor, int[] recentlyUsedColors) {
        this(context, onColorSetListener);
        mColorPicker.getRecentColorInfo().initRecentColorInfo(recentlyUsedColors);
        mColorPicker.getRecentColorInfo().setCurrentColor(currentColor);
        mCurrentColor = currentColor;
        mColorPicker.updateRecentColorLayout();
    }

    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        switch (whichButton) {
            case BUTTON_NEGATIVE:
            default:
                return;
            case BUTTON_POSITIVE:
                mColorPicker.saveSelectedColor();
                if (mOnColorSetListener != null) {
                    if (mColorPicker.isUserInputValid() || mCurrentColor == null) {
                        mOnColorSetListener.onColorSet(mColorPicker.getRecentColorInfo()
                                .getSelectedColor());
                    } else {
                        mOnColorSetListener.onColorSet(mCurrentColor);
                    }
                }
        }
    }

    public SeslColorPicker getColorPicker() {
        return mColorPicker;
    }

    public void setNewColor(Integer newColor) {
        mColorPicker.getRecentColorInfo().setNewColor(newColor);
        mColorPicker.updateRecentColorLayout();
    }

    public void setTransparencyControlEnabled(boolean enabled) {
        mColorPicker.setOpacityBarEnabled(enabled);
    }

    private static int resolveDialogTheme(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.isLightTheme, outValue, true);
        return outValue.data != 0
                ? R.style.ThemeOverlay_AppCompat_Light_Dialog : R.style.ThemeOverlay_AppCompat_Dialog;
    }
}
