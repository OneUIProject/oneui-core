/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.appcompat.util;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ContentResolver;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Button shapes helper class.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslShowButtonShapesHelper {
    private ContentResolver mContentResolver;
    private View mView;
    private Drawable mBackgroundOn;
    private Drawable mBackgroundOff;

    public SeslShowButtonShapesHelper(@NonNull View view, @Nullable Drawable backgroundOn,
                                      @Nullable Drawable backgroundOff) {
        mView = view;
        mContentResolver = view.getContext().getContentResolver();
        mBackgroundOn = backgroundOn;
        mBackgroundOff = backgroundOff;
    }

    public void setBackgroundOff(@Nullable Drawable backgroundOff) {
        if (mBackgroundOn == null || mBackgroundOn != backgroundOff) {
            mBackgroundOff = backgroundOff;
        } else {
            Log.w("SeslSBBHelper", backgroundOff + "is same drawable with mBackgroundOn");
        }
    }

    public void setBackgroundOn(@Nullable Drawable backgroundOn) {
        mBackgroundOn = backgroundOn;
    }

    public void updateButtonBackground() {
        final boolean show = Settings.System.getInt(mContentResolver, "show_button_background", 0) == 1;
        ViewCompat.setBackground(mView, show ? mBackgroundOn : mBackgroundOff);
    }

    public void updateOverflowButtonBackground(Drawable backgroundOn) {
        mBackgroundOn = backgroundOn;
        updateButtonBackground();
    }
}
