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

package androidx.appcompat.view;

import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung custom Insets Callback class.
 */
@RequiresApi(30)
public class SeslTranslateViewInsetsCallback extends WindowInsetsAnimation.Callback {
    private View mView;
    private int mDeferInsetTypes;
    private int mPersistentInsetTypes;

    public SeslTranslateViewInsetsCallback(View view, int persistentInsetTypes,
                                           int deferInsetTypes) {
        this(view, persistentInsetTypes, deferInsetTypes, 0);
    }

    public SeslTranslateViewInsetsCallback(View view, int persistentInsetTypes,
                                           int deferInsetTypes, int dispatchMode) {
        super(dispatchMode);
        mView = view;
        mPersistentInsetTypes = persistentInsetTypes;
        mDeferInsetTypes = deferInsetTypes;
    }

    @Override
    @NonNull
    public WindowInsets onProgress(@NonNull WindowInsets insets,
                                   @NonNull List<WindowInsetsAnimation> runningAnimations) {
        Insets max = Insets.max(Insets.subtract(insets.getInsets(mDeferInsetTypes),
                insets.getInsets(mPersistentInsetTypes)), Insets.NONE);
        mView.setTranslationY(max.top - max.bottom);
        return insets;
    }

    @Override
    public void onEnd(@NonNull WindowInsetsAnimation animation) {
        mView.setTranslationY(0.0f);
    }
}
