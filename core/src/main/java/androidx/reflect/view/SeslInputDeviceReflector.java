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
import android.view.InputDevice;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung InputDevice utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslInputDeviceReflector {
    private static final Class<?> mClass = InputDevice.class;

    private SeslInputDeviceReflector() {
    }

    /**
     * Sets the current pointer type in the given {@link InputDevice}.
     */
    public static void semSetPointerType(InputDevice inputDevice, int pointerType) {
        if (inputDevice != null) {
            Method method = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_setPointerType", Integer.TYPE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                method = SeslBaseReflector.getMethod(mClass, "semSetPointerType", Integer.TYPE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                method = SeslBaseReflector.getMethod(mClass, "setPointerType", Integer.TYPE);
            }

            if (method != null) {
                SeslBaseReflector.invoke(inputDevice, method, pointerType);
            }
        }
    }
}
