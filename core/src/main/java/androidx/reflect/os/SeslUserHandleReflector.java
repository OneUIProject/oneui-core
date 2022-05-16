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

package androidx.reflect.os;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.RestrictTo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung UserHandle utility class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslUserHandleReflector {
    private static final Class<?> mClass = UserHandle.class;

    private SeslUserHandleReflector() {
    }

    /**
     * Returns the user id of the current process.
     */
    public static int myUserId() {
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_myUserId");
        } else {
            method = SeslBaseReflector.getMethod(mClass, "myUserId");
        }

        if (method != null) {
            Object result = SeslBaseReflector.invoke(null, method);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        }

        return 0;
    }
}
