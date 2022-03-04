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

package androidx.reflect.hardware.input;

import android.hardware.input.InputManager;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung InputManager utility class.
 */
public class SeslInputManagerReflector {
    private static final Class<?> mClass = InputManager.class;

    /**
     * Returns instance of {@link InputManager}.
     */
    @Nullable
    private static Object getInstance() {
        Method method = SeslBaseReflector.getMethod(mClass, "getInstance");
        if (method != null) {
            return SeslBaseReflector.invoke(null, method);
        }

        return null;
    }

    /**
     * Changes the mouse pointer's icon shape into the specified <arg>iconId</arg>.
     */
    public static void setPointerIconType(int iconId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Object inputManager = getInstance();
            if (inputManager != null) {
                Method method;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_setPointerIconType", Integer.TYPE);
                } else {
                    method = SeslBaseReflector.getMethod(mClass, "setPointerIconType", Integer.TYPE);
                }

                if (method != null) {
                    SeslBaseReflector.invoke(inputManager, method, iconId);
                }
            }
        }
    }
}
