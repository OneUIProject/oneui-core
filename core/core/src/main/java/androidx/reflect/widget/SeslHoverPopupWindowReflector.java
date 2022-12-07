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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

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
public class SeslHoverPopupWindowReflector {
    private static String mClassName;

    private SeslHoverPopupWindowReflector() {
    }

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mClassName = "com.samsung.android.widget.SemHoverPopupWindow";
        } else {
            mClassName = "android.widget.HoverPopupWindow";
        }
    }

    public static int getField_TYPE_NONE() {
        Object TYPE_NONE = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_TYPE_NONE");
            if (method != null) {
                TYPE_NONE = SeslBaseReflector.invoke(null, method);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "TYPE_NONE");
            if (field != null) {
                TYPE_NONE = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_NONE instanceof Integer) {
            return (Integer) TYPE_NONE;
        }

        return 0;
    }

    public static int getField_TYPE_TOOLTIP() {
        Object TYPE_TOOLTIP = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_TYPE_TOOLTIP");
            if (method != null) {
                TYPE_TOOLTIP = SeslBaseReflector.invoke(null, method);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "TYPE_TOOLTIP");
            if (field != null) {
                TYPE_TOOLTIP = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_TOOLTIP instanceof Integer) {
            return (Integer) TYPE_TOOLTIP;
        }

        return 1;
    }

    public static int getField_TYPE_USER_CUSTOM() {
        Object TYPE_USER_CUSTOM = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_TYPE_USER_CUSTOM");
            if (method != null) {
                TYPE_USER_CUSTOM = SeslBaseReflector.invoke(null, method);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClassName, "TYPE_USER_CUSTOM");
            if (field != null) {
                TYPE_USER_CUSTOM = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_USER_CUSTOM instanceof Integer) {
            return (Integer) TYPE_USER_CUSTOM;
        }

        return 3;
    }

    public static void setGravity(@NonNull Object hoverPopupWindow, int gravity) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_setGravity", Integer.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClassName, "setGravity", Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClassName, "setPopupGravity", Integer.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(hoverPopupWindow, method, gravity);
        }
    }

    public static void setOffset(@NonNull Object hoverPopupWindow, int x, int y) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_setOffset", Integer.TYPE, Integer.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClassName, "setOffset", Integer.TYPE, Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClassName, "setPopupPosOffset", Integer.TYPE, Integer.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(hoverPopupWindow, method, x, y);
        }
    }

    public static void setHoverDetectTime(@NonNull Object hoverPopupWindow, int ms) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_setHoverDetectTime", Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClassName, "setHoverDetectTime", Integer.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(hoverPopupWindow, method, ms);
        }
    }

    public static void setHoveringPoint(@NonNull Object hoverPopupWindow, int x, int y) {
        Method method = SeslBaseReflector.getMethod(mClassName, "setHoveringPoint", Integer.TYPE, Integer.TYPE);
        if (method != null) {
            SeslBaseReflector.invoke(hoverPopupWindow, method, x, y);
        }
    }

    public static void update(@NonNull Object hoverPopupWindow) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getMethod(mClassName, "hidden_update");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClassName, "update");
        } else {
            method = SeslBaseReflector.getMethod(mClassName, "updateHoverPopup");
        }

        if (method != null) {
            SeslBaseReflector.invoke(hoverPopupWindow, method);
        }
    }
}
