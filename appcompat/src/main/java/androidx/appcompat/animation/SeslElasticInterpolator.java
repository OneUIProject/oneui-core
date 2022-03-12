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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.view.animation.Interpolator;

import androidx.annotation.RestrictTo;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung custom interpolator that simulates an elastic behavior.
 */
@RestrictTo(LIBRARY_GROUP)
class SeslElasticInterpolator implements Interpolator {
    private float mAmplitude;
    private float mPeriod;

    SeslElasticInterpolator(float amplitude, float period) {
        mAmplitude = amplitude;
        mPeriod = period;
    }

    public void setAmplitude(float value) {
        mAmplitude = value;
    }

    public void setPeriod(float value) {
        mPeriod = value;
    }

    public float getAmplitude() {
        return mAmplitude;
    }

    public float getPeriod() {
        return mPeriod;
    }

    @Override
    public float getInterpolation(float t) {
        return out(t, mAmplitude, mPeriod);
    }

    private float out(float t, float a, float p) {
        if (t == 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }

        if (p == 0.0f) {
            p = 0.3f;
        }

        float s;
        if (a == 0.0f || a < 1.0f) {
            s = p / 4.0f;
            a = 1.0f;
        } else {
            s = (float) (p / (2 * Math.PI) * Math.asin(1.0f / a));
        }

        return (float) (a * Math.pow(2.0d, (-10.0f * t)) * Math.sin((t - s) * (2 * Math.PI)) / p + 1.0d);
    }
}
