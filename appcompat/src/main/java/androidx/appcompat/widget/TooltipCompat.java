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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung Tooltip class.
 */
public class TooltipCompat  {
    /**
     * Sets the tooltip text for the view.
     *
     * @param view the view on which to set the tooltip text
     * @param tooltipText the tooltip text
     */
    public static void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText) {
        TooltipCompatHandler.setTooltipText(view, tooltipText);
    }

    /**
     * Force the next Tooltip to be shown under its anchor view.
     */
    public static void seslSetNextTooltipForceBelow(boolean isBelow) {
        TooltipCompatHandler.seslSetTooltipForceBelow(isBelow);
    }

    /**
     * Force the next Tooltip to use ActionBar positioning.
     */
    public static void seslSetNextTooltipForceActionBarPosX(boolean isForceX) {
        TooltipCompatHandler.seslSetTooltipForceActionBarPosX(isForceX);
    }

    /**
     * Set a custom position where to show the next Tooltip.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    static void setTooltipPosition(int x, int y, int layoutDirection) {
        TooltipCompatHandler.seslSetTooltipPosition(x, y, layoutDirection);
    }

    /**
     * Set whether to show the next Tooltip.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void setTooltipNull(boolean tooltipNull) {
        TooltipCompatHandler.seslSetTooltipNull(tooltipNull);
    }

    private TooltipCompat() {
    }
}
