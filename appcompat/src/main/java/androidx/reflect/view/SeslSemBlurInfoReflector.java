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

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;
import android.view.View;

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
 * Samsung SemBlurInfo utility class, to be used only for devices with OneUI 4 onwards.
 */
public class SeslSemBlurInfoReflector {
    private static final String TAG = "SeslSemBlurInfoReflector";
    private static final String mBuilderClass = "android.view.SemBlurInfo$Builder";

    /**
     * Creates a <b>SemBlurInfo.Builder</b> instance with the given <arg>blurMode</arg>.
     */
    @SuppressLint("LongLogTag")
    @Nullable
    public static Object semCreateBlurBuilder(int blurMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Constructor<?> constructor = SeslBaseReflector.getConstructor(mBuilderClass, Integer.TYPE);
            if (constructor != null) {
                try {
                    return constructor.newInstance(blurMode);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "semCreateBlurBuilder IllegalAccessException", e);
                } catch (InstantiationException e) {
                    Log.e(TAG, "semCreateBlurBuilder InstantiationException", e);
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "semCreateBlurBuilder InvocationTargetException", e);
                }
            }
        }

        return null;
    }

    /**
     * Sets background blur <arg>radius</arg> in the given <arg>builder</arg>.
     */
    @NonNull
    public static Object semSetBuilderBlurRadius(@NonNull Object builder, int radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Method method = SeslBaseReflector.getDeclaredMethod(mBuilderClass, "hidden_setRadius", Integer.TYPE);
            if (method != null) {
                method.setAccessible(true);
                SeslBaseReflector.invoke(builder, method, radius);
            }
        }

        return builder;
    }

    /**
     * Sets background blur <arg>color</arg> in the given <arg>builder</arg>.
     */
    @NonNull
    public static Object semSetBuilderBlurBackgroundColor(@NonNull Object builder, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Method method = SeslBaseReflector.getDeclaredMethod(mBuilderClass, "hidden_setBackgroundColor", Integer.TYPE);
            if (method != null) {
                method.setAccessible(true);
                SeslBaseReflector.invoke(builder, method, color);
            }
        }

        return builder;
    }

    /**
     * Sets background blur <arg>cornerRadius</arg> in the given <arg>builder</arg>.
     */
    @NonNull
    public static Object semSetBuilderBlurBackgroundCornerRadius(@NonNull Object builder, float cornerRadius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Method method = SeslBaseReflector.getDeclaredMethod(mBuilderClass, "hidden_setBackgroundCornerRadius", Float.TYPE);
            if (method != null) {
                method.setAccessible(true);
                SeslBaseReflector.invoke(builder, method, cornerRadius);
            }
        }

        return builder;
    }

    /**
     * Sets a <b>SemBlurInfo.Builder</b> instance in the given <arg>view</arg>.
     */
    public static void semBuildSetBlurInfo(@NonNull Object builder, @NonNull View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Method method = SeslBaseReflector.getDeclaredMethod(mBuilderClass, "hidden_build");
            if (method != null) {
                method.setAccessible(true);
                SeslViewReflector.semSetBlurInfo(view, SeslBaseReflector.invoke(builder, method));
            }
        }
    }
}
