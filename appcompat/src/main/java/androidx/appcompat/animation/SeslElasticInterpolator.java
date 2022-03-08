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

import android.view.animation.Interpolator;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung custom interpolator that simulates an elastic behavior.
 *
 * @hide
 */
class SeslElasticInterpolator implements Interpolator {
    private float mAmplitude;
    private float mPeriod;

    SeslElasticInterpolator(float amplitude, float period) {
        mAmplitude = amplitude;
        mPeriod = period;
    }

    public void setAmplitude(float amplitude) {
        mAmplitude = amplitude;
    }

    public void setPeriod(float period) {
        mPeriod = period;
    }

    public float getAmplitude() {
        return mAmplitude;
    }

    public float getPeriod() {
        return mPeriod;
    }

    @Override
    public float getInterpolation(float input) {
        return out(input, mAmplitude, mPeriod);
    }

    private float out(float input, float amplitude, float period) {
        if (input == 0.0f) {
            return 0.0f;
        }
        if (input >= 1.0f) {
            return 1.0f;
        }

        if (period == 0.0f) {
            period = 0.3f;
        }

        double s;
        if (amplitude == 0.0f || amplitude < 1.0f) {
            s = period / 4.0f;
            amplitude = 1.0f;
        } else {
            s = period / (2 * Math.PI) * Math.asin(1.0f / amplitude);
        }

        return (float) (amplitude * Math.pow(2.0d, (-10.0f * input)) * Math.sin((input - s) * (2 * Math.PI)) / period + 1.0d);
    }
}
