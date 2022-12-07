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
import android.widget.AbsListView;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslAbsListViewReflector {
    private static final Class<?> mClass = AbsListView.class;

    private SeslAbsListViewReflector() {
    }

    public static EdgeEffect getField_mEdgeGlowTop(@NonNull AbsListView listView) {
        Object edgeGlowTop = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_mEdgeGlowTop");
            if (method != null) {
                edgeGlowTop = SeslBaseReflector.invoke(listView, method);
            }
        } else {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "mEdgeGlowTop");
            if (field != null) {
                edgeGlowTop = SeslBaseReflector.get(listView, field);
            }
        }

        if (edgeGlowTop instanceof EdgeEffect) {
            return (EdgeEffect) edgeGlowTop;
        }

        return null;
    }

    public static void setField_mEdgeGlowTop(@NonNull AbsListView listView, EdgeEffect edgeEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_mEdgeGlowTop", EdgeEffect.class);
            if (method != null) {
                SeslBaseReflector.invoke(listView, method, edgeEffect);
            }
        } else {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "mEdgeGlowTop");
            if (field != null) {
                SeslBaseReflector.set(listView, field, edgeEffect);
            }
        }
    }

    public static void setField_mEdgeGlowBottom(@NonNull AbsListView listView, EdgeEffect edgeEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_mEdgeGlowBottom", EdgeEffect.class);
            if (method != null) {
                SeslBaseReflector.invoke(listView, method, edgeEffect);
            }
        } else {
            Field field = SeslBaseReflector.getDeclaredField(mClass, "mEdgeGlowBottom");
            if (field != null) {
                SeslBaseReflector.set(listView, field, edgeEffect);
            }
        }
    }
}
