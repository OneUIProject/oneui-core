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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.util.SeslMisc;
import androidx.core.content.res.ResourcesCompat;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung DropDownItemTextView class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslDropDownItemTextView extends SeslCheckedTextView {
    private static final String TAG = SeslDropDownItemTextView.class.getSimpleName();

    public SeslDropDownItemTextView(Context context) {
        this(context, null);
    }

    public SeslDropDownItemTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public SeslDropDownItemTextView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        setTypeface(Typeface.create("sec-roboto-light", checked ? Typeface.BOLD : Typeface.NORMAL));

        if (checked) {
            Context context = getContext();
            if (context != null && getCurrentTextColor() == Color.MAGENTA) {
                Log.w(TAG, "text color reload!");

                ColorStateList textColor = ResourcesCompat.getColorStateList(context.getResources(),
                        SeslMisc.isLightTheme(context) ? R.color.sesl_spinner_dropdown_text_color_light : R.color.sesl_spinner_dropdown_text_color_dark,
                        context.getTheme());
                if (textColor != null) {
                    setTextColor(textColor);
                } else {
                    Log.w(TAG, "Didn't set SeslDropDownItemTextView text color!!");
                }
            }
        }
    }
}
