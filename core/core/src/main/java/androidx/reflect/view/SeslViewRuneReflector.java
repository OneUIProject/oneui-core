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

package androidx.reflect.view;

import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslViewRuneReflector {
    private static String mClassName = "com.samsung.android.rune.ViewRune";

    private SeslViewRuneReflector() {
    }

    public static boolean supportFoldableDualDisplay() {
        Method method = SeslBaseReflector.getMethod(mClassName, "hidden_supportFoldableDualDisplay");

        Object result = null;
        if (method != null) {
            result = SeslBaseReflector.invoke(mClassName, method);
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        return false;
    }

    public static boolean supportFoldableNoSubDisplay() {
        Method method = SeslBaseReflector.getMethod(mClassName, "hidden_supportFoldableNoSubDisplay");

        Object result = null;
        if (method != null) {
            result = SeslBaseReflector.invoke(mClassName, method);
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        return false;
    }

    public static boolean isEdgeEffectStretchType() {
        Method method = SeslBaseReflector.getMethod(mClassName, "hidden_isEdgeEffectStretchType");

        Object result = null;
        if (method != null) {
            result = SeslBaseReflector.invoke(mClassName, method);
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        return false;
    }
}
