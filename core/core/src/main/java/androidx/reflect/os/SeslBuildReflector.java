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

package androidx.reflect.os;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslBuildReflector {

    private SeslBuildReflector() {
    }

    public static class SeslVersionReflector {
        private static final Class<?> mClass = Build.VERSION.class;

        private SeslVersionReflector() {
        }

        public static int getField_SEM_PLATFORM_INT() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Field field = SeslBaseReflector.getDeclaredField(mClass, "SEM_PLATFORM_INT");
                if (field != null) {
                    if (SeslBaseReflector.get(null, field) instanceof Integer) {
                        return (Integer) SeslBaseReflector.get(null, field);
                    }
                }
            }

            return -1;
        }
    }

}
