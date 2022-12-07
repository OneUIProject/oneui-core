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

package androidx.reflect.widget;

import android.os.Build;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslTextViewReflector {
    private static final Class<?> mClass = TextView.class;

    private SeslTextViewReflector() {
    }

    public static void semSetActionModeMenuItemEnabled(@NonNull TextView textView, int menuId, boolean enabled) {
        Method method;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetActionModeMenuItemEnabled", Integer.TYPE, Boolean.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semSetActionModeMenuItemEnabled", Integer.TYPE, Boolean.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "setNewActionPopupMenu", Integer.TYPE, Boolean.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(textView, method, menuId, enabled);
        }
    }

    public static int getField_SEM_AUTOFILL_ID() {
        Object SEM_AUTOFILL_ID = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_SEM_AUTOFILL_ID");
            if (method != null) {
                SEM_AUTOFILL_ID = SeslBaseReflector.invoke(null, method);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "SEM_AUTOFILL_ID");
            if (field != null) {
                SEM_AUTOFILL_ID = SeslBaseReflector.get(null, field);
            }
        }

        if (SEM_AUTOFILL_ID instanceof Integer) {
            return (Integer) SEM_AUTOFILL_ID;
        }

        return 0;
    }

    public static boolean getField_mSingleLine(@NonNull TextView textView) {
        Field field = SeslBaseReflector.getDeclaredField(mClass, "mSingleLine");
        if (field != null) {
            Object singleLine = SeslBaseReflector.get(textView, field);
            if (singleLine instanceof Boolean) {
                return (Boolean) singleLine;
            }
        }

        return false;
    }

    public static boolean semIsTextSelectionProgressing() {
        Method method = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semIsTextSelectionProgressing");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semIsTextSelectionProgressing");
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    public static boolean semIsTextViewHovered() {
        Method method = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semIsTextViewHovered");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semIsTextViewHovered");
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    public static void semSetButtonShapeEnabled(@NonNull TextView textView, boolean enabled) {
        Method method = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetButtonShapeEnabled", Boolean.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            method = SeslBaseReflector.getMethod(mClass, "semSetButtonShapeEnabled", Boolean.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(textView, method, enabled);
        }
    }

    public static void semSetButtonShapeEnabled(@NonNull TextView textView, boolean enabled, int textColor) {
        Method method = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetButtonShapeEnabled", Boolean.TYPE, Integer.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            method = SeslBaseReflector.getMethod(mClass, "semSetButtonShapeEnabled", Boolean.TYPE, Integer.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(textView, method, enabled, textColor);
        }
    }
}
