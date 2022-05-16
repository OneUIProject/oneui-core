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

package androidx.reflect.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.view.HapticFeedbackConstants;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung HapticFeedbackConstants utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslHapticFeedbackConstantsReflector {
    private static final Class<?> mClass = HapticFeedbackConstants.class;

    private SeslHapticFeedbackConstantsReflector() {
    }

    /**
     * Returns a <b>feedbackConstant</b> by the given <arg>index</arg>
     */
    public static int semGetVibrationIndex(int index) {
        Method method = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semGetVibrationIndex", Integer.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            method = SeslBaseReflector.getMethod(mClass, "semGetVibrationIndex", Integer.TYPE);
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method, index);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return -1;
    }
}
