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

package androidx.reflect.graphics.drawable;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslStateListDrawableReflector {
    private static final Class<?> mClass = StateListDrawable.class;

    private SeslStateListDrawableReflector() {
    }

    public static int getStateCount(@NonNull StateListDrawable drawable) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_getStateCount");
        } else {
            method = SeslBaseReflector.getMethod(mClass, "getStateCount");
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(drawable, method);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 0;
    }

    public static Drawable getStateDrawable(@NonNull StateListDrawable drawable, int index) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_getStateDrawable", Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "getStateDrawable", Integer.TYPE);
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(drawable, method, index);
            if (result instanceof Drawable) {
                return (Drawable) result;
            }
        }

        return null;
    }

    public static int[] getStateSet(@NonNull StateListDrawable drawable, int index) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_getStateSet", Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "getStateSet", Integer.TYPE);
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(drawable, method, index);
            if (result instanceof int[]) {
                return (int[]) result;
            }
        }

        return new int[0];
    }
}
