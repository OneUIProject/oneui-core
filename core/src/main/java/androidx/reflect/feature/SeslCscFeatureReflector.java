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
 * Samsung CscFeature utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslCscFeatureReflector {
    private static String mClassName;

    private SeslCscFeatureReflector() {
    }

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mClassName = "com.samsung.sesl.feature.SemCscFeature";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mClassName = "com.samsung.android.feature.SemCscFeature";
        } else {
            mClassName = "com.sec.android.app.CscFeature";
        }
    }

    /**
     * Returns instance of <b>SemCscFeature</b>.
     */
    private static Object getInstance() {
        Method method = SeslBaseReflector.getMethod(mClassName, "getInstance");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method);
            if (result.getClass().getName().equals(mClassName)) {
                return result;
            }
        }

        return null;
    }

    /**
     * Gets the value of the given CSC Feature <arg>tag</arg>.
     */
    public static String getString(String tag, String defaultValue) {
        Object result = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_getString", String.class, String.class);
            result = SeslBaseReflector.invoke(null, method, tag, defaultValue);
        } else {
            Object semCscFeature = getInstance();
            if (semCscFeature != null) {
                Method method = SeslBaseReflector.getMethod(mClassName, "getString", String.class, String.class);
                result = SeslBaseReflector.invoke(semCscFeature, method, tag, defaultValue);
            }
        }

        if (result instanceof String) {
            return (String) result;
        } else {
            return defaultValue;
        }
    }
}
