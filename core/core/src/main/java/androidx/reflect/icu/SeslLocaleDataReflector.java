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

package androidx.reflect.icu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormatSymbols;
import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslLocaleDataReflector {
    private static String mClassName = "libcore.icu.LocaleData";
    private static String mDateFormatSymbolsClass = "android.icu.text.DateFormatSymbols";
    private static String mSemClassName = "com.samsung.sesl.icu.SemLocaleData";
    private static String mSemDateFormatSymbolsClass = "com.samsung.sesl.icu.SemDateFormatSymbols";

    private SeslLocaleDataReflector() {
    }

    public static Object get(@NonNull Locale locale) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mSemClassName, "get", Locale.class);
        } else {
            method = SeslBaseReflector.getMethod(mClassName, "get", Locale.class);
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method, locale);
            if (result.getClass().getName().equals(mClassName)) {
                return result;
            }
        }

        return null;
    }

    public static char getField_zeroDigit(@NonNull Object localeData) {
        Object zeroDigit = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mSemClassName, "getZeroDigit", SeslBaseReflector.getClass(mClassName));
            if (method != null) {
                zeroDigit = SeslBaseReflector.invoke(null, method, localeData);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "zeroDigit");
            if (field != null) {
                zeroDigit = SeslBaseReflector.get(localeData, field);
            }
        }

        if (zeroDigit instanceof Character) {
            return (Character) zeroDigit;
        }

        return '0';
    }

    public static String[] getField_amPm(@NonNull Object localeData) {
        Object amPm = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mSemClassName, "getAmPm", SeslBaseReflector.getClass(mClassName));
            if (method != null) {
                amPm = SeslBaseReflector.invoke(null, method, localeData);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "amPm");
            if (field != null) {
                amPm = SeslBaseReflector.get(localeData, field);
            }
        }

        if (amPm instanceof String[]) {
            return (String[]) amPm;
        }

        Log.e("SeslLocaleDataReflector", "amPm failed. Use DateFormatSymbols for ampm");
        return new DateFormatSymbols().getAmPmStrings();
    }

    public static String getField_narrowAm(@NonNull Object localeData) {
        Object narrowAm = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mSemClassName, "getNarrowAm", SeslBaseReflector.getClass(mClassName));
            if (method != null) {
                narrowAm = SeslBaseReflector.invoke(null, method, localeData);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "narrowAm");
            if (field != null) {
                narrowAm = SeslBaseReflector.get(localeData, field);
            }
        }

        if (narrowAm instanceof String) {
            return (String) narrowAm;
        }

        return "Am";
    }

    public static String getField_narrowPm(@NonNull Object localeData) {
        Object narrowPm = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mSemClassName, "getNarrowPm", SeslBaseReflector.getClass(mClassName));
            if (method != null) {
                narrowPm = SeslBaseReflector.invoke(null, method, localeData);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "narrowPm");
            if (field != null) {
                narrowPm = SeslBaseReflector.get(localeData, field);
            }
        }

        if (narrowPm instanceof String) {
            return (String) narrowPm;
        }

        return "Pm";
    }

    public static String[] getAmpmNarrowStrings(@NonNull Object dateFormatSymbols) {
        Method method = SeslBaseReflector.getDeclaredMethod(mSemDateFormatSymbolsClass, "getAmpmNarrowStrings", SeslBaseReflector.getClass(mDateFormatSymbolsClass));
        Object result = null;

        if (method != null) {
            result = SeslBaseReflector.invoke(null, method, dateFormatSymbols);
        }

        if (result instanceof String[]) {
            return (String[]) result;
        } else {
            Log.e("SeslLocaleDataReflector", "amPm narrow strings failed. Use getAmPmStrings for ampm");
            return new DateFormatSymbols().getAmPmStrings();
        }
    }
}
