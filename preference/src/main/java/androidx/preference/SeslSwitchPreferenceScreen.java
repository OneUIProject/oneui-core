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

package androidx.preference;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import androidx.reflect.view.SeslViewReflector;
import androidx.reflect.widget.SeslHoverPopupWindowReflector;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslSwitchPreferenceScreen extends SwitchPreferenceCompat {
    private View.OnKeyListener mSwitchKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            int keyCode = keyEvent.getKeyCode();

            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!isChecked()) {
                        return false;
                    } else {
                        if (callChangeListener(false)) {
                            setChecked(false);
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (isChecked()) {
                        return false;
                    } else {
                        if (callChangeListener(true)) {
                            setChecked(true);
                        }
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }
    };

    public SeslSwitchPreferenceScreen(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes);
        final Configuration configuration = context.getResources().getConfiguration();

        String fragment = a.getString(R.styleable.Preference_android_fragment);
        if (fragment == null || fragment.equals("")) {
            Log.w("SwitchPreferenceScreen",
                    "SwitchPreferenceScreen should getfragment property. " +
                            "Fragment property does not exsit in SwitchPreferenceScreen");
        }

        if ((configuration.screenWidthDp > 320 || configuration.fontScale < FONT_SCALE_MEDIUM)
                && (configuration.screenWidthDp >= 411 || configuration.fontScale < FONT_SCALE_LARGE)) {
            setLayoutResource(R.layout.sesl_switch_preference_screen);
        } else {
            setLayoutResource(R.layout.sesl_switch_preference_screen_large);
        }

        setWidgetLayoutResource(R.layout.sesl_switch_preference_screen_widget_divider);
        a.recycle();
    }

    public SeslSwitchPreferenceScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslSwitchPreferenceScreen(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.switchPreferenceStyle);
    }

    public SeslSwitchPreferenceScreen(Context context) {
        this(context, null);
    }

    @Override
    void callClickListener() {
    }

    @Override
    protected void onClick() {
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setOnKeyListener(mSwitchKeyListener);

        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        View switchView = holder.findViewById(AndroidResources.ANDROID_R_SWITCH_WIDGET);

        if (titleView != null && switchView != null) {
            SeslViewReflector.semSetHoverPopupType(switchView,
                    SeslHoverPopupWindowReflector.getField_TYPE_NONE());
            switchView.setContentDescription(titleView.getText().toString());
        }

        if (switchView != null) {
            final Configuration configuration = getContext().getResources().getConfiguration();
            LayoutParams lp = switchView.getLayoutParams();
            if ((configuration.screenWidthDp > 320 || configuration.fontScale < FONT_SCALE_MEDIUM)
                    && (configuration.screenWidthDp >= 411 || configuration.fontScale < FONT_SCALE_LARGE)) {
                lp.height = LayoutParams.MATCH_PARENT;
            } else {
                lp.height = LayoutParams.WRAP_CONTENT;
            }
            switchView.setLayoutParams(lp);
        }
    }
}
