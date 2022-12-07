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

import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslPopupWindowReflector {
    private static final Class<?> mClass = PopupWindow.class;

    private SeslPopupWindowReflector() {
    }

    public static void setAllowScrollingAnchorParent(@NonNull PopupWindow popupWindow, boolean enabled) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClass, "setAllowScrollingAnchorParent", Boolean.TYPE);

        if (method != null) {
            SeslBaseReflector.invoke(popupWindow, method, enabled);
        }
    }
}
