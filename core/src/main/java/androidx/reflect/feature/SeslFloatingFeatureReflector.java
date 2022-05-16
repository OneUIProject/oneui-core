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

package androidx.reflect.feature;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung FloatingFeature utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslFloatingFeatureReflector {
    private static final String mClassName;

    private SeslFloatingFeatureReflector() {
    }

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mClassName = "com.samsung.sesl.feature.SemFloatingFeature";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mClassName = "com.samsung.android.feature.SemFloatingFeature";
        } else {
            mClassName = "com.samsung.android.feature.FloatingFeature";
        }
    }

    /**
     * Returns instance of <b>SemFloatingFeature</b>.
     */
    private static Object getInstance() {
        Method method = SeslBaseReflector.getMethod(mClassName, "getInstance");
        if (method == null) {
            return null;
        }

        Object result = SeslBaseReflector.invoke(null, method);
        if (result.getClass().getName().equals(mClassName)) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Gets the value of the given Floating Feature <arg>tag</arg>.
     */
    public static String getString(String tag, String defaultValue) {
        Object result = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_getString", String.class, String.class);
            result = SeslBaseReflector.invoke(null, method, tag, defaultValue);
        } else {
            Object semFloatingFeature = getInstance();
            if (semFloatingFeature != null) {
                Method method = SeslBaseReflector.getMethod(mClassName, "getString", String.class, String.class);
                result = SeslBaseReflector.invoke(semFloatingFeature, method, tag, defaultValue);
            }
        }

        if (result instanceof String) {
            return (String) result;
        } else {
            return defaultValue;
        }
    }
}
