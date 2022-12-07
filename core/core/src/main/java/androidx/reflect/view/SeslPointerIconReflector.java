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

package androidx.reflect.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslPointerIconReflector {
    protected static String mClassName = "android.view.PointerIcon";

    private SeslPointerIconReflector() {
    }

    public static int getField_SEM_TYPE_STYLUS_DEFAULT() {
        Object TYPE_STYLUS_DEFAULT = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_DEFAULT");
            if (method != null) {
                TYPE_STYLUS_DEFAULT = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_DEFAULT";
            } else {
                fieldName = "HOVERING_SPENICON_DEFAULT";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_DEFAULT = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_DEFAULT instanceof Integer) {
            return (Integer) TYPE_STYLUS_DEFAULT;
        }

        return 1;
    }

    public static int getField_SEM_TYPE_STYLUS_SCROLL_UP() {
        Object TYPE_STYLUS_SCROLL_UP = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_SCROLL_UP");
            if (method != null) {
                TYPE_STYLUS_SCROLL_UP = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_SCROLL_UP";
            } else {
                fieldName = "HOVERING_SCROLLICON_POINTER_01";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_SCROLL_UP = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_SCROLL_UP instanceof Integer) {
            return (Integer) TYPE_STYLUS_SCROLL_UP;
        }

        return 11;
    }

    public static int getField_SEM_TYPE_STYLUS_SCROLL_DOWN() {
        Object TYPE_STYLUS_SCROLL_DOWN = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_SCROLL_DOWN");
            if (method != null) {
                TYPE_STYLUS_SCROLL_DOWN = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_SCROLL_DOWN";
            } else {
                fieldName = "HOVERING_SCROLLICON_POINTER_05";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_SCROLL_DOWN = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_SCROLL_DOWN instanceof Integer) {
            return (Integer) TYPE_STYLUS_SCROLL_DOWN;
        }

        return 15;
    }

    public static int getField_SEM_TYPE_STYLUS_SCROLL_LEFT() {
        Object TYPE_STYLUS_SCROLL_LEFT = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_SCROLL_LEFT");
            if (method != null) {
                TYPE_STYLUS_SCROLL_LEFT = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_SCROLL_LEFT";
            } else {
                fieldName = "HOVERING_SCROLLICON_POINTER_07";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_SCROLL_LEFT = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_SCROLL_LEFT instanceof Integer) {
            return (Integer) TYPE_STYLUS_SCROLL_LEFT;
        }

        return 17;
    }

    public static int getField_SEM_TYPE_STYLUS_SCROLL_RIGHT() {
        Object TYPE_STYLUS_SCROLL_RIGHT = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_SCROLL_RIGHT");
            if (method != null) {
                TYPE_STYLUS_SCROLL_RIGHT = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_SCROLL_RIGHT";
            } else {
                fieldName = "HOVERING_SCROLLICON_POINTER_03";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_SCROLL_RIGHT = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_SCROLL_RIGHT instanceof Integer) {
            return (Integer) TYPE_STYLUS_SCROLL_RIGHT;
        }

        return 13;
    }

    public static int getField_SEM_TYPE_STYLUS_PEN_SELECT() {
        Object TYPE_STYLUS_PEN_SELECT = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_PEN_SELECT");
            if (method != null) {
                TYPE_STYLUS_PEN_SELECT = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_PEN_SELECT";
            } else {
                fieldName = "HOVERING_PENSELECT_POINTER_01";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_PEN_SELECT = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_PEN_SELECT instanceof Integer) {
            return (Integer) TYPE_STYLUS_PEN_SELECT;
        }

        return 21;
    }

    public static int getField_SEM_TYPE_STYLUS_MORE() {
        Object TYPE_STYLUS_MORE = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "hidden_SEM_TYPE_STYLUS_MORE");
            if (method != null) {
                TYPE_STYLUS_MORE = SeslBaseReflector.invoke(null, method);
            }
        } else {
            String fieldName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fieldName = "SEM_TYPE_STYLUS_MORE";
            } else {
                fieldName = "HOVERING_SPENICON_MORE";
            }

            Field field = SeslBaseReflector.getField(mClassName, fieldName);
            if (field != null) {
                TYPE_STYLUS_MORE = SeslBaseReflector.get(null, field);
            }
        }

        if (TYPE_STYLUS_MORE instanceof Integer) {
            return (Integer) TYPE_STYLUS_MORE;
        }

        return 20010;
    }
}
