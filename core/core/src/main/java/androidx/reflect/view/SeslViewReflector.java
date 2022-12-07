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

import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.PointerIcon;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslViewReflector {
    private static final String TAG = "SeslViewReflector";
    private static final Class<?> mClass = View.class;

    private SeslViewReflector() {
    }

    public static void setField_mPaddingLeft(@NonNull View view, int value) {
        Field field = SeslBaseReflector.getDeclaredField(mClass, "mPaddingLeft");
        if (field != null) {
            SeslBaseReflector.set(view, field, value);
        }
    }

    public static void setField_mPaddingRight(@NonNull View view, int value) {
        Field field = SeslBaseReflector.getDeclaredField(mClass, "mPaddingRight");
        if (field != null) {
            SeslBaseReflector.set(view, field, value);
        }
    }

    public static int getField_mPaddingLeft(@NonNull View view) {
        Field field = SeslBaseReflector.getDeclaredField(mClass, "mPaddingLeft");
        if (field != null) {
            Object paddingLeft = SeslBaseReflector.get(view, field);
            if (paddingLeft instanceof Integer) {
                return (Integer) paddingLeft;
            }
        }

        return 0;
    }

    public static int getField_mPaddingRight(@NonNull View view) {
        Field field = SeslBaseReflector.getDeclaredField(mClass, "mPaddingRight");
        if (field != null) {
            Object paddingLeft = SeslBaseReflector.get(view, field);
            if (paddingLeft instanceof Integer) {
                return (Integer) paddingLeft;
            }
        }

        return 0;
    }

    public static boolean isInScrollingContainer(@NonNull View view) {
        Method method;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            method = SeslBaseReflector.getMethod(mClass, "isInScrollingContainer");
        } else {
            method = SeslBaseReflector.getMethod(mClass, "hidden_isInScrollingContainer");
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(view, method);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    public static void clearAccessibilityFocus(@NonNull View view) {
        Method method = SeslBaseReflector.getMethod(mClass, "clearAccessibilityFocus");
        if (method != null) {
            SeslBaseReflector.invoke(view, method);
        }
    }

    public static boolean requestAccessibilityFocus(@NonNull View view) {
        Method method = SeslBaseReflector.getMethod(mClass, "requestAccessibilityFocus");
        if (method != null) {
            Object result = SeslBaseReflector.invoke(view, method);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    public static void notifyViewAccessibilityStateChangedIfNeeded(@NonNull View view, int changeType) {
        Method method;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            method = SeslBaseReflector.getMethod(mClass, "notifyViewAccessibilityStateChangedIfNeeded", Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "hidden_notifyViewAccessibilityStateChangedIfNeeded", Integer.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(view, method, changeType);
        }
    }

    public static boolean isVisibleToUser(@NonNull View view) {
        return isVisibleToUser(view, null);
    }

    public static boolean isVisibleToUser(@NonNull View view, Rect boundInView) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClass, "isVisibleToUser", Rect.class);
        if (method != null) {
            Object result = SeslBaseReflector.invoke(view, method, boundInView);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    public static int semGetHoverPopupType(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Method method = SeslBaseReflector.getMethod(mClass, "semGetHoverPopupType");
            if (method != null) {
                Object result = SeslBaseReflector.invoke(view, method);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            }
        } else {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "mHoverPopupType");
            if (field != null) {
                Object hoverPopupType = SeslBaseReflector.get(view, field);
                if (hoverPopupType instanceof Integer) {
                    return (Integer) hoverPopupType;
                }
            }
        }

        return 0;
    }

    public static void semSetHoverPopupType(@NonNull View view, int type) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetHoverPopupType", Integer.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semSetHoverPopupType", Integer.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "setHoverPopupType", Integer.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(view, method, type);
        }
    }

    public static void semSetDirectPenInputEnabled(@NonNull View view, boolean enabled) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetDirectPenInputEnabled", Boolean.TYPE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semSetDirectPenInputEnabled", Boolean.TYPE);
        } else {
            method = SeslBaseReflector.getMethod(mClass, "setWritingBuddyEnabled", Boolean.TYPE);
        }

        if (method != null) {
            SeslBaseReflector.invoke(view, method, enabled);
        }
    }

    public static boolean isHoveringUIEnabled(@NonNull View view) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClass, "isHoveringUIEnabled");
        if (method != null) {
            Object invoke = SeslBaseReflector.invoke(view, method);
            if (invoke instanceof Boolean) {
                return (Boolean) invoke;
            }
        }

        return false;
    }

    public static void semSetBlurInfo(@NonNull View view, @Nullable Object blurInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetBlurInfo", Class.forName("android.view.SemBlurInfo"));
                if (method != null) {
                    SeslBaseReflector.invoke(view, method, blurInfo);
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "semSetBlurInfo ClassNotFoundException", e);
            }
        }
    }

    public static void semSetPointerIcon(@NonNull View view, int toolType, PointerIcon pointerIcon) {
        Method method = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semSetPointerIcon", Integer.TYPE, PointerIcon.class);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getMethod(mClass, "semSetPointerIcon", Integer.TYPE, PointerIcon.class);
        }

        if (method != null) {
            SeslBaseReflector.invoke(view, method, toolType, pointerIcon);
        }
    }

    public static boolean isHighContrastTextEnabled(@NonNull View view) {
        String methodName;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            methodName = "semIsHighContrastTextEnabled";
        } else {
            methodName = "isHighContrastTextEnabled";
        }

        Method method = SeslBaseReflector.getMethod(mClass, methodName);
        if (method != null) {
            Object invoke = SeslBaseReflector.invoke(view, method);
            if (invoke instanceof Boolean) {
                return (Boolean) invoke;
            }
        }

        return false;
    }

    public static Object semGetHoverPopup(@NonNull View view, boolean createIfNotExist) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_semGetHoverPopup", Boolean.TYPE);
            if (method != null) {
                return SeslBaseReflector.invoke(view, method, createIfNotExist);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Method method = SeslBaseReflector.getMethod(mClass, "semGetHoverPopup", Boolean.TYPE);
            if (method != null) {
                return SeslBaseReflector.invoke(view, method, createIfNotExist);
            }
        } else {
            Method method = SeslBaseReflector.getMethod(mClass, "getHoverPopupWindow");
            if (method != null) {
                return SeslBaseReflector.invoke(view, method);
            }
        }

        return null;
    }

    public static void resolvePadding(@NonNull View view) {
        Method method;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "resolvePadding");
        } else {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_resolvePadding");
        }

        if (method != null) {
            SeslBaseReflector.invoke(view, method);
        }
    }

    public static void resetPaddingToInitialValues(@NonNull View view) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClass, "resetPaddingToInitialValues");
        if (method != null) {
            SeslBaseReflector.invoke(view, method);
        }
    }

    public static void getWindowDisplayFrame(@NonNull View view, @NonNull Rect outRect) {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "getWindowDisplayFrame", Rect.class);
        } else {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "getWindowVisibleDisplayFrame", Rect.class);
        }

        if (method != null) {
            SeslBaseReflector.invoke(view, method, outRect);
        }
    }
}
