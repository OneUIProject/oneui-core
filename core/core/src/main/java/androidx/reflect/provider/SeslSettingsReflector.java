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

package androidx.reflect.provider;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslSettingsReflector {

    private SeslSettingsReflector() {
    }

    public static class SeslSystemReflector {
        private static final Class<?> mClass = Settings.System.class;

        private SeslSystemReflector() {
        }

        public static String getField_SEM_PEN_HOVERING() {
            Object PEN_HOVERING = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_SEM_PEN_HOVERING");
                if (method != null) {
                    PEN_HOVERING = SeslBaseReflector.invoke(null, method);
                }
            } else {
                String fieldName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fieldName = "SEM_PEN_HOVERING";
                } else {
                    fieldName = "PEN_HOVERING";
                }

                Field field = SeslBaseReflector.getField(mClass, fieldName);
                if (field != null) {
                    PEN_HOVERING = SeslBaseReflector.get(null, field);
                }
            }

            if (PEN_HOVERING instanceof String) {
                return (String) PEN_HOVERING;
            } else {
                return "pen_hovering";
            }
        }

        public static String getField_SEM_ACCESSIBILITY_REDUCE_TRANSPARENCY() {
            Object result = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_SEM_ACCESSIBILITY_REDUCE_TRANSPARENCY");
                if (method != null) {
                    result = SeslBaseReflector.invoke(null, method);
                }
            }

            if (result instanceof String) {
                return (String) result;
            } else {
                return "not_supported";
            }
        }
    }

}
