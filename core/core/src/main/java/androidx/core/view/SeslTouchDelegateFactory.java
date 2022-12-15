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

package androidx.core.view;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.view.SeslTouchTargetDelegate.ExtraInsets;
import androidx.core.view.SeslTouchTargetDelegate.InvalidDelegateViewException;

import java.util.ArrayList;
import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslTouchDelegateFactory {
    private interface Strategy {
        ExtraInsets getExtraInsets(Rect prevBounds, Rect bounds, Rect nextBounds);
    }

    public static TouchDelegate make(@NonNull LinearLayout linearLayout)
            throws InvalidDelegateViewException {
        return make(linearLayout, getChildren(linearLayout));
    }

    @NonNull
    public static TouchDelegate make(@NonNull LinearLayout linearLayout, List<View> targetList)
            throws InvalidDelegateViewException {
        SeslTouchTargetDelegate delegate = new SeslTouchTargetDelegate(linearLayout);

        if (targetList.size() == 0) {
            return delegate;
        }

        final int h = linearLayout.getHeight();
        final int w = linearLayout.getWidth();
        final Rect llBounds = new Rect(0, 0, w, h);

        ArrayList<Rect> targetsBounds = new ArrayList<>();
        for (View view : targetList) {
            targetsBounds.add(delegate.calculateViewBounds(view));
        }

        Strategy strategy;
        if (linearLayout.getOrientation() == LinearLayout.HORIZONTAL) {
            strategy = (prevBounds, bounds, nextBounds) -> {
                final int l = bounds.left - prevBounds.right;
                final int t = bounds.top - llBounds.top;
                final int r = Math.max(0, nextBounds.left - bounds.right) / 2;
                final int b = llBounds.bottom - bounds.bottom;

                return ExtraInsets.of(l, t, r, b);
            };
        } else {
            strategy = (prevBounds, bounds, nextBounds) -> {
                final int l = bounds.left - llBounds.left;
                final int t = bounds.top - prevBounds.bottom;
                final int r = llBounds.right - bounds.right;
                final int b = Math.max(0, nextBounds.top - bounds.bottom) / 2;

                return ExtraInsets.of(l, t, r, b);
            };
        }

        Rect lastVBounds = targetsBounds.get(targetsBounds.size() - 1);
        targetsBounds.add(new Rect(Math.max(0, w - lastVBounds.right) + w,
                Math.max(0, h - lastVBounds.bottom) + h, w, h));

        int i = 0;
        Rect rect = new Rect(0, 0, 0, 0);
        while (i < targetList.size()) {
            Rect r = targetsBounds.get(i);
            int i2 = i + 1;
            delegate.addTouchDelegate(targetList.get(i), strategy.getExtraInsets(rect, r, targetsBounds.get(i2)));
            rect = r;
            i = i2;
        }

        return delegate;
    }

    private static List<View> getChildren(ViewGroup viewGroup) {
        ArrayList<View> list = new ArrayList<>();
        final int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child != null) {
                list.add(child);
            }
        }
        return list;
    }
}