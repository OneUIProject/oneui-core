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

package androidx.reflect.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Paint utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslPaintReflector {
    private static final Class<?> mClass = Paint.class;

    private SeslPaintReflector() {
    }

    /**
     * Calls <b>Paint.getHCTStrokeWidth()</b>.
     */
    public static float getHCTStrokeWidth(@NonNull Paint paint) {
        Method method = SeslBaseReflector.getMethod(mClass, "getHCTStrokeWidth");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(paint, method);
            if (result instanceof Float) {
                return (Float) result;
            }
        }

        return 0.0f;
    }
}
