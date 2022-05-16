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

package androidx.reflect.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.Resources;
import android.view.ViewDebug;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung ViewDebug utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslViewDebugReflector {
    private static final Class<?> mClass = ViewDebug.class;

    private SeslViewDebugReflector() {
    }

    /**
     * Gets the style attributes from the {@link Resources.Theme}. For debugging only.
     *
     * @param resources Resources to resolve attributes from.
     * @param theme Theme to dump.
     * @return a String array containing pairs of adjacent Theme attribute data: name followed by
     * its value.
     */
    public static String[] getStyleAttributesDump(Resources resources, Resources.Theme theme) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_getStyleAttributesDump", Resources.class, Resources.Theme.class);
        if (method != null) {
            method.setAccessible(true);
            Object result = SeslBaseReflector.invoke(null, method, resources, theme);
            if (result instanceof String[]) {
                return (String[]) result;
            }
        }

        return new String[0];
    }
}
