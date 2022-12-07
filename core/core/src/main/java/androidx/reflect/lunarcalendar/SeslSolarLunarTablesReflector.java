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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslSolarLunarTablesReflector {
    private static final String mClassName = "com.samsung.android.calendar.secfeature.lunarcalendar.SolarLunarTables";

    private SeslSolarLunarTablesReflector() {
    }

    public static int getDayLengthOf(@NonNull PathClassLoader pathClassLoader, @NonNull Object solarLunarTables, int year, int month, boolean leap) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getDayLengthOf", Integer.TYPE, Integer.TYPE, Boolean.TYPE);
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarTables, method, year, month, leap);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 29;
    }

    public static boolean isLeapMonth(@NonNull PathClassLoader pathClassLoader, @NonNull Object solarLunarTables, int year, int month) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "isLeapMonth", Integer.TYPE, Integer.TYPE);
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarTables, method, year, month);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    public static byte getLunar(@NonNull PathClassLoader pathClassLoader, @NonNull Object solarLunarTables, int index) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "getLunar", Integer.TYPE);
        if (method != null) {
            Object result = SeslBaseReflector.invoke(solarLunarTables, method, index);
            if (result instanceof Byte) {
                return (Byte) result;
            }
        }

        return Byte.MAX_VALUE;
    }

    public static int getField_START_OF_LUNAR_YEAR(@NonNull PathClassLoader pathClassLoader, @NonNull Object solarLunarTables) {
        Field field = SeslPathClassReflector.getField(pathClassLoader, mClassName, "START_OF_LUNAR_YEAR");
        if (field != null) {
            Object START_OF_LUNAR_YEAR = SeslBaseReflector.get(solarLunarTables, field);
            if (START_OF_LUNAR_YEAR instanceof Integer) {
                return (Integer) START_OF_LUNAR_YEAR;
            }
        }

        return 1881;
    }

    public static int getField_WIDTH_PER_YEAR(@NonNull PathClassLoader pathClassLoader, @NonNull Object solarLunarTables) {
        Field field = SeslPathClassReflector.getField(pathClassLoader, mClassName, "WIDTH_PER_YEAR");
        if (field != null) {
            Object WIDTH_PER_YEAR = SeslBaseReflector.get(solarLunarTables, field);
            if (WIDTH_PER_YEAR instanceof Integer) {
                return (Integer) WIDTH_PER_YEAR;
            }
        }

        return 14;
    }

    public static int getField_INDEX_OF_LEAP_MONTH(@NonNull PathClassLoader pathClassLoader, @NonNull Object solarLunarTables) {
        Field field = SeslPathClassReflector.getField(pathClassLoader, mClassName, "INDEX_OF_LEAP_MONTH");
        if (field != null) {
            Object INDEX_OF_LEAP_MONTH = SeslBaseReflector.get(solarLunarTables, field);
            if (INDEX_OF_LEAP_MONTH instanceof Integer) {
                return (Integer) INDEX_OF_LEAP_MONTH;
            }
        }

        return 13;
    }
}
