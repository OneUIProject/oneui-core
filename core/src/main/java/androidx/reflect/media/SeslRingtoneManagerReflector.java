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

import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung RingtoneManager utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslRingtoneManagerReflector {
    private static final Class<?> mClass = RingtoneManager.class;

    private SeslRingtoneManagerReflector() {
    }

    /**
     * Returns <b>RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS</b>.
     */
    public static String getField_EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS() {
        Object EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS");
            if (method != null) {
                EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = SeslBaseReflector.invoke(null, method);
            }
        } else {
            Field field = SeslBaseReflector.getField(mClass, "EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS");
            if (field != null) {
                EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = SeslBaseReflector.get(null, field);
            }
        }

        if (EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS instanceof String) {
            return (String) EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS;
        } else {
            return "android.intent.extra.ringtone.SHOW_DEFAULT";
        }
    }
}
