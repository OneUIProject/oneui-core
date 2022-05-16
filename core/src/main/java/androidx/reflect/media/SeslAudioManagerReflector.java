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

import android.media.AudioManager;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Field;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung AudioManager utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslAudioManagerReflector {
    private static final Class<?> mClass = AudioManager.class;

    private SeslAudioManagerReflector() {
    }

    /**
     * Returns <b>AudioManager.SOUND_TIME_PICKER_SCROLL</b>.
     */
    public static int getField_SOUND_TIME_PICKER_SCROLL() {
        Field SOUND_TIME_PICKER_SCROLL = SeslBaseReflector.getField(mClass, "SOUND_TIME_PICKER_SCROLL");
        if (SOUND_TIME_PICKER_SCROLL != null) {
            Object obj = SeslBaseReflector.get(null, SOUND_TIME_PICKER_SCROLL);
            if (obj instanceof Integer) {
                return (Integer) obj;
            }
        }

        return 0;
    }

    /**
     * Returns <b>AudioManager.SOUND_TIME_PICKER_FAST</b>.
     */
    public static int getField_SOUND_TIME_PICKER_SCROLL_FAST() {
        Field SOUND_TIME_PICKER_FAST = SeslBaseReflector.getField(mClass, "SOUND_TIME_PICKER_FAST");
        if (SOUND_TIME_PICKER_FAST != null) {
            Object obj = SeslBaseReflector.get(null, SOUND_TIME_PICKER_FAST);
            if (obj instanceof Integer) {
                return (Integer) obj;
            }
        }

        return 0;
    }

    /**
     * Returns <b>AudioManager.SOUND_TIME_PICKER_SLOW</b>.
     */
    public static int getField_SOUND_TIME_PICKER_SCROLL_SLOW() {
        Field SOUND_TIME_PICKER_SLOW = SeslBaseReflector.getField(mClass, "SOUND_TIME_PICKER_SLOW");
        if (SOUND_TIME_PICKER_SLOW != null) {
            Object obj = SeslBaseReflector.get(null, SOUND_TIME_PICKER_SLOW);
            if (obj instanceof Integer) {
                return (Integer) obj;
            }
        }

        return 0;
    }
}
