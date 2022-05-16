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

package androidx.reflect;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.util.Log;

import androidx.annotation.RestrictTo;

import dalvik.system.PathClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Java Reflection utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslPathClassReflector {
    private static final String TAG = "SeslPathClassReflector";

    private SeslPathClassReflector() {
    }

    /**
     * Returns the {@link Class} object associated with the class or interface with the given
     * string name, using the given class loader.
     */
    public static Class<?> getClass(PathClassLoader pathClassLoader, String className) {
        try {
            return Class.forName(className, true, pathClassLoader);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Fail to get class", e);
            return null;
        }
    }

    /**
     * Returns a {@link Method} object that reflects the specified public member method of the
     * class or interface represented by the given string name, using the given class loader.
     */
    public static Method getMethod(PathClassLoader pathClassLoader, String className, String methodName, Class<?>... parameterTypes) {
        Class<?> cls = getClass(pathClassLoader, className);
        if (cls == null) {
            return null;
        }
        try {
            return cls.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, methodName + " NoSuchMethodException", e);
            return null;
        }
    }

    /**
     * Returns a {@link Field} object that reflects the specified public member field of the class or
     * interface represented by the given string name, using the given class loader.
     */
    public static Field getField(PathClassLoader pathClassLoader, String className, String fieldName) {
        Class<?> cls = getClass(pathClassLoader, className);
        if (cls == null) {
            return null;
        }
        try {
            return cls.getField(fieldName);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, fieldName + " NoSuchMethodException", e);
            return null;
        }
    }
}
