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

package androidx.reflect.lunarcalendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.reflect.SeslBaseReflector;
import androidx.reflect.SeslPathClassReflector;

import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Calendar Feature utility class.
 */
public class SeslFeatureReflector {
    private static final String mClassName = "com.android.calendar.Feature";

    /**
     * Returns an instance of <b>SolarLunarConverter</b> in the given {@link PathClassLoader}.
     */
    @Nullable
    public static Object getSolarLunarConverter(@NonNull PathClassLoader pathClassLoader) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getSolarLunarConverter");
        if (method != null) {
            return SeslBaseReflector.invoke(null, method);
        }

        return null;
    }

    /**
     * Returns an instance of <b>SolarLunarTables</b> in the given {@link PathClassLoader}.
     */
    @Nullable
    public static Object getSolarLunarTables(@NonNull PathClassLoader pathClassLoader) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getSolarLunarTables");
        if (method != null) {
            return SeslBaseReflector.invoke(null, method);
        }

        return null;
    }
}
