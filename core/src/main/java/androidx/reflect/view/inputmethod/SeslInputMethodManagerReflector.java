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

package androidx.reflect.view.inputmethod;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung InputMethodManager utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslInputMethodManagerReflector {
    private static final Class<?> mClass = InputMethodManager.class;

    private SeslInputMethodManagerReflector() {
    }

    /**
     * Returns current accessory keyboard state in the given {@link InputMethodManager}.
     */
    public static int isAccessoryKeyboardState(InputMethodManager imm) {
        Method method = SeslBaseReflector.getMethod(mClass, "isAccessoryKeyboardState");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(imm, method);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 0;
    }

    /**
     * Get whether the soft keyboard is shown in the given {@link InputMethodManager}.
     */
    public static boolean isInputMethodShown(InputMethodManager imm) {
        Method method = SeslBaseReflector.getMethod(mClass, "semIsInputMethodShown");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(imm, method);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }
}
