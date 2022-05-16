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

package androidx.reflect.content.res;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung CompatibilityInfo utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslCompatibilityInfoReflector {
    private static final String mClassName = "android.content.res.CompatibilityInfo";

    private SeslCompatibilityInfoReflector() {
    }

    /**
     * Returns <b>CompatibilityInfo.applicationScale</b> in the given {@link Resources}.
     */
    public static float getField_applicationScale(@NonNull Resources resources) {
        Object compatibilityInfo = SeslResourcesReflector.getCompatibilityInfo(resources);
        if (compatibilityInfo != null) {
            Field field = SeslBaseReflector.getField(mClassName, "applicationScale");
            if (field != null) {
                Object applicationScale = SeslBaseReflector.get(compatibilityInfo, field);
                if (applicationScale instanceof Integer) {
                    return (float) (Integer) applicationScale;
                }
            }
        }

        return 1.0f;
    }
}
