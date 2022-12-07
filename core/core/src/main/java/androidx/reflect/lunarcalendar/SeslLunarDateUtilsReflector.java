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

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;
import androidx.reflect.SeslPathClassReflector;

import java.lang.reflect.Method;
import java.util.Calendar;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslLunarDateUtilsReflector {
    private static final String mClassName = "com.android.calendar.event.widget.datetimepicker.LunarDateUtils";

    private SeslLunarDateUtilsReflector() {
    }

    public static String buildLunarDateString(PathClassLoader pathClassLoader, Calendar calendar, Context context) {
        Method method = SeslPathClassReflector.getMethod(pathClassLoader, mClassName, "buildLunarDateString", Calendar.class, Context.class);
        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method, calendar, context);
            if (result instanceof String) {
                return (String) result;
            }
        }

        return null;
    }
}
