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

package androidx.reflect.lunarcalendar;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;
import androidx.reflect.SeslPathClassReflector;

import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslSolarLunarConverterReflector {
    private static final String mClassName = "com.samsung.android.calendar.secfeature.lunarcalendar.SolarLunarConverter";

    private SeslSolarLunarConverterReflector() {
    }

    public static void convertLunarToSolar(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter, int y, int m, int d, boolean isLeapMonth) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "convertLunarToSolar", Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE);
        if (method != null) {
            SeslBaseReflector.invoke(solarLunarConverter, method, y, m, d, isLeapMonth);
        }
    }

    public static void convertSolarToLunar(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter, int y, int m, int d) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "convertSolarToLunar", Integer.TYPE, Integer.TYPE, Integer.TYPE);
        if (method != null) {
            SeslBaseReflector.invoke(solarLunarConverter, method, y, m, d);
        }
    }

    public static int getWeekday(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter, int year, int month, int day) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getWeekday", Integer.TYPE, Integer.TYPE, Integer.TYPE);
        if (method != null) {
            Object invoke = SeslBaseReflector.invoke(solarLunarConverter, method, year, month, day);
            if (invoke instanceof Integer) {
                return (Integer) invoke;
            }
        }

        return 0;
    }

    public static int getDayLengthOf(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter, int year, int month, boolean isLeapMonth) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getDayLengthOf", Integer.TYPE, Integer.TYPE, Boolean.TYPE);
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarConverter, method, year, month, isLeapMonth);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 30;
    }

    public static int getYear(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getYear");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarConverter, method);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 2019;
    }

    public static int getMonth(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getMonth");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarConverter, method);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 10;
    }

    public static int getDay(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getDay");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarConverter, method);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 19;
    }

    public static boolean isLeapMonth(PathClassLoader pathClassLoader, @NonNull Object solarLunarConverter) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "isLeapMonth");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarConverter, method);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }
}
