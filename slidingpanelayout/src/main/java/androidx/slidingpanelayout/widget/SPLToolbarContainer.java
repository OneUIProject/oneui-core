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

package androidx.slidingpanelayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ViewStubCompat;
import androidx.slidingpanelayout.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SPLToolbarContainer extends FrameLayout {
    protected static final String TAG = SPLToolbarContainer.class.getSimpleName();

    @Nullable
    private ViewStubCompat mViewStubCompat;

    public SPLToolbarContainer(@NonNull Context context) {
        this(context, null);
    }

    public SPLToolbarContainer(@NonNull Context context,
                               @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SPLToolbarContainer(@NonNull Context context,
                               @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext()
                .obtainStyledAttributes(R.styleable.AppCompatTheme);
        if (!a.getBoolean(R.styleable.AppCompatTheme_windowActionModeOverlay, false)) {
            LayoutInflater.from(context).inflate(R.layout.sesl_spl_action_mode_view_stub, this, true);
            mViewStubCompat = findViewById(R.id.action_mode_bar_stub);
        }
        a.recycle();

        setWillNotDraw(false);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        if (mViewStubCompat != null) {
            mViewStubCompat.bringToFront();
            mViewStubCompat.invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mViewStubCompat != null) {
            mViewStubCompat.bringToFront();
            mViewStubCompat.invalidate();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
