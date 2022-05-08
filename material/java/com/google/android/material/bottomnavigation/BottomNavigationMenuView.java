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

package com.google.android.material.bottomnavigation;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Resources;
import androidx.core.view.ViewCompat;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.google.android.material.navigation.NavigationBarItemView;
import com.google.android.material.navigation.NavigationBarMenuView;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/** @hide For internal use only. */
@RestrictTo(LIBRARY_GROUP)
public class BottomNavigationMenuView extends NavigationBarMenuView {
  private final int inactiveItemMaxWidth;
  private final int inactiveItemMinWidth;
  private int activeItemMaxWidth;
  private final int activeItemMinWidth;
  private int itemHeight;

  private boolean itemHorizontalTranslationEnabled;
  private int[] tempChildWidths;

  private boolean mHasIcon;
  private float mWidthPercent;

  public BottomNavigationMenuView(@NonNull Context context) {
    super(context);

    FrameLayout.LayoutParams params =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.gravity = Gravity.CENTER;
    setLayoutParams(params);

    final Resources res = getResources();

    TypedValue outValue = new TypedValue();
    res.getValue(R.dimen.sesl_bottom_navigation_width_proportion, outValue, true);
    mWidthPercent = outValue.getFloat();

    inactiveItemMaxWidth =
        res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_item_max_width);
    inactiveItemMinWidth =
        res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_item_min_width);
    activeItemMaxWidth =
        (int) (getResources().getDisplayMetrics().widthPixels * mWidthPercent);
    activeItemMinWidth =
        res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_active_item_min_width);
    itemHeight = res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_mode_height);

    tempChildWidths = new int[BottomNavigationView.MAX_ITEM_COUNT];

    mUseItemPool = false;
  }

  // TODO rework this method
  // kang
  @Override
  protected void onMeasure(int var1, int var2) {
    /* var1 = widthMeasureSpec; var2 = heightMeasureSpec; */
    DisplayMetrics var3 = this.getResources().getDisplayMetrics();
    if ((float)MeasureSpec.getSize(var1) / var3.density < 590.0F) {
      this.mWidthPercent = 1.0F;
    } else {
      this.mWidthPercent = 0.75F;
    }

    this.activeItemMaxWidth = (int)((float)this.getResources().getDisplayMetrics().widthPixels * this.mWidthPercent);
    int var4 = (int)((float)MeasureSpec.getSize(var1) * this.mWidthPercent);
    this.getMenu();
    this.getVisibleItemCount();
    int var5 = 0;

    for(var1 = var5; var5 < this.getChildCount(); var1 = var2) {
      var2 = var1;
      if (this.getChildAt(var5).getVisibility() == 0) {
        var2 = var1 + 1;
      }

      ++var5;
    }

    int var6 = this.getChildCount();
    this.tempChildWidths = new int[var6];
    boolean var7;
    if (this.getViewType() != 3) {
      var7 = true;
    } else {
      var7 = false;
    }

    this.mHasIcon = var7;
    Resources var10 = this.getResources();
    if (this.mHasIcon) {
      var2 = R.dimen.sesl_bottom_navigation_icon_mode_height;
    } else {
      var2 = R.dimen.sesl_bottom_navigation_text_mode_height;
    }

    var2 = var10.getDimensionPixelSize(var2);
    this.itemHeight = var2;
    int var8 = MeasureSpec.makeMeasureSpec(var2, 1073741824);
    int var9;
    int[] var11;
    View var12;
    int var10002;
    if (this.isShifting(this.getLabelVisibilityMode(), var6) && this.isItemHorizontalTranslationEnabled()) {
      var12 = this.getChildAt(this.getSelectedItemPosition());
      var2 = this.activeItemMinWidth;
      var1 = var2;
      if (var12.getVisibility() != 8) {
        var12.measure(MeasureSpec.makeMeasureSpec(this.activeItemMaxWidth, -2147483648), var8);
        var1 = Math.max(var2, var12.getMeasuredWidth());
      }

      byte var13;
      if (var12.getVisibility() != 8) {
        var13 = 1;
      } else {
        var13 = 0;
      }

      var2 = var6 - var13;
      var9 = Math.min(var4 - this.inactiveItemMinWidth * var2, Math.min(var1, this.activeItemMaxWidth));
      var5 = var4 - var9;
      if (var2 == 0) {
        var1 = 1;
      } else {
        var1 = var2;
      }

      var4 = Math.min(var5 / var1, this.inactiveItemMaxWidth);
      var1 = var5 - var2 * var4;

      for(var2 = 0; var2 < var6; var1 = var5) {
        if (this.getChildAt(var2).getVisibility() != 8) {
          var11 = this.tempChildWidths;
          if (var2 == this.getSelectedItemPosition()) {
            var5 = var9;
          } else {
            var5 = var4;
          }

          var11[var2] = var5;
          var5 = var1;
          if (var1 > 0) {
            var11 = this.tempChildWidths;
            var10002 = var11[var2]++;
            var5 = var1 - 1;
          }
        } else {
          this.tempChildWidths[var2] = 0;
          var5 = var1;
        }

        ++var2;
      }
    } else {
      if (var1 == 0) {
        var2 = 1;
      } else {
        var2 = var1;
      }

      var2 = var4 / var2;
      if (var1 != 2) {
        var2 = Math.min(var2, this.activeItemMaxWidth);
      }

      var1 = var4 - var1 * var2;

      for(var5 = 0; var5 < var6; var1 = var9) {
        if (this.getChildAt(var5).getVisibility() != 8) {
          var11 = this.tempChildWidths;
          var11[var5] = var2;
          var9 = var1;
          if (var1 > 0) {
            var10002 = var11[var5]++;
            var9 = var1 - 1;
          }
        } else {
          this.tempChildWidths[var5] = 0;
          var9 = var1;
        }

        ++var5;
      }
    }

    var1 = 0;

    for(var5 = var1; var1 < var6; var5 = var2) {
      var12 = this.getChildAt(var1);
      var2 = var5;
      if (var12 != null) {
        if (var12.getVisibility() == 8) {
          var2 = var5;
        } else {
          var12.measure(MeasureSpec.makeMeasureSpec(this.tempChildWidths[var1], 1073741824), var8);
          var12.getLayoutParams().width = var12.getMeasuredWidth();
          var2 = var5 + var12.getMeasuredWidth();
        }
      }

      ++var1;
    }

    this.setMeasuredDimension(View.resolveSizeAndState(var5, MeasureSpec.makeMeasureSpec(var5, 1073741824), 0), View.resolveSizeAndState(this.itemHeight, var8, 0));
  }
  // kang

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    final int count = getChildCount();
    final int width = right - left;
    final int height = bottom - top;
    final int padding;
    if (!mHasIcon) {
      padding = 0;
    } else if (getViewVisibleItemCount() == 5) {
      padding = getResources()
              .getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_mode_min_padding_horizontal);
    } else {
      padding = getResources()
              .getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_mode_padding_horizontal);
    }
    int used = 0;
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == GONE) {
        continue;
      }
      if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) {
        child.layout(width - used - child.getMeasuredWidth() + padding, 0, width - used - padding, height);
      } else {
        child.layout(used + padding, 0, child.getMeasuredWidth() + used - padding, height);
      }
      used += child.getMeasuredWidth();
    }
    updateBadgeIfNeeded();
  }

  /**
   * Sets whether the menu items horizontally translate on selection when the combined item widths
   * fill the screen.
   *
   * @param itemHorizontalTranslationEnabled whether the menu items horizontally translate on
   *     selection
   * @see #isItemHorizontalTranslationEnabled()
   */
  public void setItemHorizontalTranslationEnabled(boolean itemHorizontalTranslationEnabled) {
    this.itemHorizontalTranslationEnabled = itemHorizontalTranslationEnabled;
  }

  /**
   * Returns whether the menu items horizontally translate on selection when the combined item
   * widths fill the screen.
   *
   * @return whether the menu items horizontally translate on selection
   * @see #setItemHorizontalTranslationEnabled(boolean)
   */
  public boolean isItemHorizontalTranslationEnabled() {
    return itemHorizontalTranslationEnabled;
  }

  @Override
  @NonNull
  protected NavigationBarItemView createNavigationBarItemView(@NonNull Context context) {
    return new BottomNavigationItemView(context);
  }
}
