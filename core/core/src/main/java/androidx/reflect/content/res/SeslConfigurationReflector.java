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

package androidx.reflect.content.res;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslConfigurationReflector {
    private static final Class<?> mClass = Configuration.class;

    private SeslConfigurationReflector() {
    }

    public static boolean isDexEnabled(@NonNull Configuration configuration) {
        return getField_semDesktopModeEnabled(configuration) == getField_SEM_DESKTOP_MODE_ENABLED();
    }

    private static int getField_semDesktopModeEnabled(@NonNull Configuration configuration) {
        Object semDesktopModeEnabled = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semDesktopModeEnabled");
            if (method != null) {
                semDesktopModeEnabled = SeslBaseReflector.invoke(configuration, method);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "semDesktopModeEnabled");
            if (field != null) {
                semDesktopModeEnabled = SeslBaseReflector.get(configuration, field);
            }
        }

        if (semDesktopModeEnabled instanceof Integer) {
            return (Integer) semDesktopModeEnabled;
        }

        return -1;
    }

    private static int getField_SEM_DESKTOP_MODE_ENABLED() {
        Object SEM_DESKTOP_MODE_ENABLED = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_SEM_DESKTOP_MODE_ENABLED");
            if (method != null) {
                SEM_DESKTOP_MODE_ENABLED = SeslBaseReflector.invoke(null, method);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "SEM_DESKTOP_MODE_ENABLED");
            if (field != null) {
                SEM_DESKTOP_MODE_ENABLED = SeslBaseReflector.get(null, field);
            }
        }

        if (SEM_DESKTOP_MODE_ENABLED instanceof Integer) {
            return (Integer) SEM_DESKTOP_MODE_ENABLED;
        }

        return 0;
    }
}
