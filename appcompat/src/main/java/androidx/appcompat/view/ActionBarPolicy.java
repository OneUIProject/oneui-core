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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import androidx.annotation.RestrictTo;
import androidx.appcompat.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Allows components to query for various configuration policy decisions about how the action bar
 * should lay out and behave on the current device.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ActionBarPolicy {
    private static final float MENU_WIDTH_LIMIT_FACTOR = 0.7f;

    private Context mContext;

    public static ActionBarPolicy get(Context context) {
        return new ActionBarPolicy(context);
    }

    private ActionBarPolicy(Context context) {
        mContext = context;
    }

    /**
     * Returns the maximum number of action buttons that should be permitted within an action
     * bar/action mode. This will be used to determine how many showAsAction="ifRoom" items can fit.
     * "always" items can override this.
     */
    public int getMaxActionButtons() {
        final Configuration configuration = mContext.getResources().getConfiguration();
        final int widthDp = configuration.screenWidthDp;
        final int heightDp = configuration.screenHeightDp;
        final int smallest = configuration.smallestScreenWidthDp;

        if (smallest > 600 || widthDp > 600 || (widthDp > 960 && heightDp > 720)
                || (widthDp > 720 && heightDp > 960)) {
            // For values-w600dp, values-sw600dp and values-xlarge.
            return 5;
        } else if (widthDp >= 500 || (widthDp > 640 && heightDp > 480)
                || (widthDp > 480 && heightDp > 640)) {
            // For values-w500dp and values-large.
            return 4;
        } else if (widthDp >= 360) {
            // For values-w360dp.
            return 3;
        } else {
            return 2;
        }
    }

    public boolean showsOverflowMenuButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return true;
        } else {
            return !ViewConfiguration.get(mContext).hasPermanentMenuKey();
        }
    }

    public int getEmbeddedMenuWidthLimit() {
        return (int) (mContext.getResources().getDisplayMetrics().widthPixels
                * MENU_WIDTH_LIMIT_FACTOR);
    }

    public boolean hasEmbeddedTabs() {
        return false;
    }

    public int getTabContainerHeight() {
        TypedArray a = mContext.obtainStyledAttributes(null, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);
        int height = a.getLayoutDimension(R.styleable.ActionBar_height, 0);
        a.recycle();
        return height;
    }

    public boolean enableHomeButtonByDefault() {
        return false;
    }

    public int getStackedTabMaxWidth() {
        return 0;
    }

    public boolean hasNavigationBar() {
        return !ViewConfiguration.get(mContext).hasPermanentMenuKey()
                && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
    }
}
