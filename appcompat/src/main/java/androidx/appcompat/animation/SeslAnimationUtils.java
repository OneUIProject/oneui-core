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

package androidx.appcompat.animation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.RestrictTo;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Animation class with predefined interpolators.
 */
public class SeslAnimationUtils {
    @SuppressLint("NewApi")
    public static final Interpolator SINE_IN_OUT_70 =
            new PathInterpolator(0.33f, 0.0f, 0.3f, 1.0f);
    @SuppressLint("NewApi")
    public static final Interpolator SINE_IN_OUT_80 =
            new PathInterpolator(0.33f, 0.0f, 0.2f, 1.0f);
    @SuppressLint("NewApi")
    public static final Interpolator SINE_IN_OUT_90 =
            new PathInterpolator(0.33f, 0.0f, 0.1f, 1.0f);
    @SuppressLint("NewApi")
    public static final Interpolator SINE_OUT_80 =
            new PathInterpolator(0.17f, 0.17f, 0.2f, 1.0f);
    @SuppressLint("NewApi")
    public static final Interpolator SINE_OUT_70 =
            new PathInterpolator(0.17f, 0.17f, 0.3f, 1.0f);
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final Interpolator ELASTIC_40 =
            new SeslElasticInterpolator(1.0f, 0.8f);
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final Interpolator ELASTIC_50 =
            new SeslElasticInterpolator(1.0f, 0.7f);
}
