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

import android.content.Context;
import android.util.AttributeSet;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung SwitchCompat used in {@link SeslSwitchBar}.
 */
public class SeslToggleSwitch extends SwitchCompat {

    private SeslToggleSwitch.OnBeforeCheckedChangeListener mOnBeforeListener;

    public interface OnBeforeCheckedChangeListener {
        boolean onBeforeCheckedChanged(SeslToggleSwitch toggleSwitch, boolean checked);
    }

    public SeslToggleSwitch(Context context) {
        super(context);
    }

    public SeslToggleSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeslToggleSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnBeforeCheckedChangeListener(OnBeforeCheckedChangeListener listener) {
        mOnBeforeListener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mOnBeforeListener != null
                && mOnBeforeListener.onBeforeCheckedChanged(this, checked)) {
            return;
        }
        super.setChecked(checked);
    }

    public void setCheckedInternal(boolean checked) {
        super.setChecked(checked);
    }
}
