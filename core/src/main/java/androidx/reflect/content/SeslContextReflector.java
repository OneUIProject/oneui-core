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

package androidx.reflect.content;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Context utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslContextReflector {
    private static final Class<?> mClass = Context.class;

    private SeslContextReflector() {
    }

    /**
     * Return a new {@link Context} object for the given <arg>packageName</arg> and {@link UserHandle}.
     */
    @RequiresApi(21)
    public static Context createPackageContextAsUser(@NonNull Context context, String packageName, int flags, UserHandle user) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_createPackageContextAsUser", String.class, Integer.TYPE, UserHandle.class);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "createPackageContextAsUser", String.class, Integer.TYPE, UserHandle.class);
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(context, method, packageName, flags, user);
            if (result instanceof Context) {
                return (Context) result;
            }
        }

        return null;
    }

    /**
     * Return the Theme object associated with the given {@link Context}.
     */
    public static String[] getTheme(@NonNull Context context) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_getTheme");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(context, method);
            if (result instanceof String[]) {
                return (String[]) result;
            }
        }

        return null;
    }
}
