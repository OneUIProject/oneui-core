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

package androidx.reflect.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung ApplicationPackageManager utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslApplicationPackageManagerReflector {
    private static String mClassName = "android.app.ApplicationPackageManager";

    private SeslApplicationPackageManagerReflector() {
    }

    /**
     * Retrieve the icon associated with the given <arg>packageName</arg>.
     */
    public static Drawable semGetApplicationIconForIconTray(@NonNull Object instance, String packageName, int mode) {
        Method method = SeslBaseReflector.getMethod(mClassName, "semGetApplicationIconForIconTray", String.class, Integer.TYPE);

        if (method != null) {
            Object result = SeslBaseReflector.invoke(instance, method, packageName, mode);
            if (result instanceof Drawable) {
                return (Drawable) result;
            }
        }

        return null;
    }

    /**
     * Retrieve the icon associated with the given {@link ComponentName}.
     */
    public static Drawable semGetActivityIconForIconTray(@NonNull Object instance, ComponentName activityName, int mode) {
        Method method = SeslBaseReflector.getMethod(mClassName, "semGetActivityIconForIconTray", ComponentName.class, Integer.TYPE);

        if (method != null) {
            Object result = SeslBaseReflector.invoke(instance, method, activityName, mode);
            if (result instanceof Drawable) {
                return (Drawable) result;
            }
        }

        return null;
    }
}
