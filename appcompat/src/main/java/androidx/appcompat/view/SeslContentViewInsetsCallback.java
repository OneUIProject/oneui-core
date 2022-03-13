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

package androidx.appcompat.view;

import android.graphics.Insets;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.R;

import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung custom Insets Callback class.
 */
@RequiresApi(30)
public class SeslContentViewInsetsCallback extends WindowInsetsAnimation.Callback
        implements View.OnApplyWindowInsetsListener {
    private static final String TAG = "SeslCVInsetsCallback";

    private View mView;

    private WindowInsets mLastWindowInsets;

    private int mPersistentInsetTypes;
    private int mDeferInsetTypes;

    private boolean mIsDeferInsets = false;
    private boolean mWindowInsetsAnimationStarted = false;

    private final Handler handler = new Handler();
    private final Runnable mHandleInsetsAnimationCanceled = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "WindowInsetsAnimation could have been cancelled");
            if (!mWindowInsetsAnimationStarted && mIsDeferInsets) {
                Log.i(TAG, "Start to restore insets state");
                mIsDeferInsets = false;
                if (mView != null && mLastWindowInsets != null) {
                    mView.dispatchApplyWindowInsets(mLastWindowInsets);
                }
            }
        }
    };

    public SeslContentViewInsetsCallback(int persistentInsetTypes,
                                         int deferInsetTypes) {
        super(DISPATCH_MODE_CONTINUE_ON_SUBTREE);
        mPersistentInsetTypes = persistentInsetTypes;
        mDeferInsetTypes = deferInsetTypes;
    }


    @Override
    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
        mView = v;
        mLastWindowInsets = insets;

        final int typeMask;
        if (mIsDeferInsets) {
            typeMask = mPersistentInsetTypes;
        } else {
            typeMask = mPersistentInsetTypes | mDeferInsetTypes;
        }

        Insets typeInsets = insets.getInsets(typeMask);

        Log.i(TAG, "onApplyWindowInsets, typeInsets = " + typeInsets
                + ", mIsDeferInsets = " + mIsDeferInsets);

        mView.setPadding(typeInsets.left, typeInsets.top,
                typeInsets.right, typeInsets.bottom);

        View actionModeBar = mView.findViewById(R.id.action_mode_bar);
        if (actionModeBar != null) {
            MarginLayoutParams lp = (MarginLayoutParams) actionModeBar.getLayoutParams();
            if (mView.getPaddingTop() != 0) {
                lp.topMargin = 0;
            }
            if (mView.getPaddingRight() != 0) {
                lp.rightMargin = 0;
            }
            if (mView.getPaddingLeft() != 0) {
                lp.leftMargin = 0;
            }
        }

        return WindowInsets.CONSUMED;
    }

    @Override
    public void onPrepare(@NonNull WindowInsetsAnimation animation) {
        if ((animation.getTypeMask() & mDeferInsetTypes) != 0) {
            Log.i(TAG, "onPrepare");
            mIsDeferInsets = true;
            handler.postDelayed(mHandleInsetsAnimationCanceled, 100);
        }
    }

    @Override
    public WindowInsetsAnimation.Bounds onStart(@NonNull WindowInsetsAnimation animation,
                                                @NonNull WindowInsetsAnimation.Bounds bounds) {
        if ((animation.getTypeMask() & mDeferInsetTypes) != 0) {
            Log.i(TAG, "onStart");
            handler.removeCallbacks(mHandleInsetsAnimationCanceled);
            mWindowInsetsAnimationStarted = true;
        }
        return bounds;
    }

    @Override
    public WindowInsets onProgress(@NonNull WindowInsets insets,
                                   @NonNull List<WindowInsetsAnimation> runningAnimations) {
        return insets;
    }

    @Override
    public void onEnd(@NonNull WindowInsetsAnimation animation) {
        if ((animation.getTypeMask() & mDeferInsetTypes) != 0) {
            Log.i(TAG, "onEnd");
            mIsDeferInsets = false;
            mWindowInsetsAnimationStarted = false;
            if (mView != null && mLastWindowInsets != null) {
                mView.dispatchApplyWindowInsets(mLastWindowInsets);
            }
        }
    }
}
