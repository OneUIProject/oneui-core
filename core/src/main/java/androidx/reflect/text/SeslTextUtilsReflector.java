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

package androidx.reflect.text;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung TextUtils utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslTextUtilsReflector {
    private static final Class<?> mClass = TextUtils.class;

    private SeslTextUtilsReflector() {
    }

    /**
     * Calls <b>TextUtils.semGetPrefixCharForSpan(TextPaint, CharSequence, char[])</b>.
     */
    public static char[] semGetPrefixCharForSpan(TextPaint paint, CharSequence text, char[] prefix) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semGetPrefixCharForSpan", TextPaint.class, CharSequence.class, char[].class);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semGetPrefixCharForSpan", TextPaint.class, CharSequence.class, char[].class);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "getPrefixCharForIndian", TextPaint.class, CharSequence.class, char[].class);
        }

        if (method == null) {
            return new char[0];
        }

        Object result = SeslBaseReflector.invoke(null, method, paint, text, prefix);
        if (result instanceof char[]) {
            return (char[]) result;
        }

        return null;
    }
}
