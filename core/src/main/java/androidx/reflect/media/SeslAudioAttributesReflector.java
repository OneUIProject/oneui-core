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

package androidx.reflect.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.media.AudioAttributes;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung AudioAttributes utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslAudioAttributesReflector {
    @RequiresApi(21)
    private static final Class<?> mClass = AudioAttributes.class;

    private SeslAudioAttributesReflector() {
    }

    /**
     * Returns <b>AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY</b>.
     */
    public static int getField_FLAG_BYPASS_INTERRUPTION_POLICY() {
        Object FLAG_BYPASS_INTERRUPTION_POLICY = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_FLAG_BYPASS_INTERRUPTION_POLICY");
            if (method != null) {
                FLAG_BYPASS_INTERRUPTION_POLICY = SeslBaseReflector.invoke(null, method);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Field field = SeslBaseReflector.getField(mClass, "FLAG_BYPASS_INTERRUPTION_POLICY");
            if (field != null) {
                FLAG_BYPASS_INTERRUPTION_POLICY = SeslBaseReflector.get(null, field);
            }
        }

        if (FLAG_BYPASS_INTERRUPTION_POLICY instanceof Integer) {
            return (Integer) FLAG_BYPASS_INTERRUPTION_POLICY;
        } else {
            return 1;
        }
    }
}
