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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung SemSoundAssistantManager utility class.
 */
public class SeslSemSoundAssistantManagerReflector {
    private static String mClassName = "com.samsung.android.media.SemSoundAssistantManager";

    private SeslSemSoundAssistantManagerReflector() {
    }

    /**
     * Returns instance of <b>SemSoundAssistantManager</b>.
     */
    @SuppressLint("LongLogTag")
    @Nullable
    private static Object getInstance(@NonNull Context context) {
        Constructor<?> constructor = SeslBaseReflector.getConstructor(mClassName, Context.class);
        if (constructor == null) {
            return null;
        }

        try {
            return constructor.newInstance(context);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            Log.e("SeslSemSoundAssistantManagerReflector", "Failed to instantiate class");
            return null;
        }
    }

    /**
     * Sets quick audio path opening.
     */
    public static void setFastAudioOpenMode(@NonNull Context context, boolean mode) {
        Method method = SeslBaseReflector.getDeclaredMethod(mClassName, "setFastAudioOpenMode", Boolean.TYPE);
        Object semSoundAssistantManager = getInstance(context);
        if (method != null && semSoundAssistantManager != null) {
            SeslBaseReflector.invoke(semSoundAssistantManager, method, mode);
        }
    }
}
