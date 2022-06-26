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
import android.os.Build;
import android.os.Build.VERSION;

import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.TintTypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.SeslTouchTargetDelegate;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.navigation.NavigationBarMenuView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.shape.MaterialShapeDrawable;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung BottomNavigationView class.
 */
public class BottomNavigationView extends NavigationBarView {
  static final int MAX_ITEM_COUNT = 5;
  private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListenerForTD;

  public BottomNavigationView(@NonNull Context context) {
    this(context, null);
  }

  public BottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.bottomNavigationStyle);
  }

  public BottomNavigationView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, R.style.Widget_Design_BottomNavigationView);
  }

  public BottomNavigationView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    // Ensure we are using the correctly themed context rather than the context that was passed in.
    context = getContext();

    /* Custom attributes */
    TintTypedArray attributes =
        ThemeEnforcement.obtainTintedStyledAttributes(
            context, attrs, R.styleable.BottomNavigationView, defStyleAttr, defStyleRes);

    setItemHorizontalTranslationEnabled(
        attributes.getBoolean(
            R.styleable.BottomNavigationView_itemHorizontalTranslationEnabled, true));

    attributes.recycle();

    if (shouldDrawCompatibilityTopDivider()) {
      addCompatibilityTopDivider(context);
    }

    if (Build.VERSION.SDK_INT >= 21) {
      MenuView menuView = getMenuView();
      if (menuView instanceof NavigationBarMenuView) {
        if (((NavigationBarMenuView) menuView).getViewType() == NavigationBarView.SESL_TYPE_LABEL_ONLY) {
          final int padding
                  = getResources().getDimensionPixelSize(R.dimen.sesl_navigation_bar_text_mode_padding_horizontal);
          setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
        }
      }
    }
  }

  /**
   * Sets whether the menu items horizontally translate on selection when the combined item widths
   * fill up the screen.
   *
   * @param itemHorizontalTranslationEnabled whether the items horizontally translate on selection
   * @see #isItemHorizontalTranslationEnabled()
   */
  public void setItemHorizontalTranslationEnabled(boolean itemHorizontalTranslationEnabled) {
    BottomNavigationMenuView menuView = (BottomNavigationMenuView) getMenuView();
    if (menuView.isItemHorizontalTranslationEnabled() != itemHorizontalTranslationEnabled) {
      menuView.setItemHorizontalTranslationEnabled(itemHorizontalTranslationEnabled);
      getPresenter().updateMenuView(false);
    }
  }

  /**
   * Returns whether the items horizontally translate on selection when the item widths fill up the
   * screen.
   *
   * @return whether the menu items horizontally translate on selection
   * @see #setItemHorizontalTranslationEnabled(boolean)
   */
  public boolean isItemHorizontalTranslationEnabled() {
    return ((BottomNavigationMenuView) getMenuView()).isItemHorizontalTranslationEnabled();
  }

  @Override
  public int getMaxItemCount() {
    return MAX_ITEM_COUNT;
  }

  /** @hide */
  @RestrictTo(LIBRARY_GROUP)
  @Override
  @NonNull
  protected NavigationBarMenuView createNavigationBarMenuView(@NonNull Context context) {
    return new BottomNavigationMenuView(context);
  }

  /**
   * Returns true a divider must be added in place of shadows to maintain compatibility in pre-21
   * legacy backgrounds.
   */
  private boolean shouldDrawCompatibilityTopDivider() {
    return VERSION.SDK_INT < 21 && !(getBackground() instanceof MaterialShapeDrawable);
  }

  /**
   * Adds a divider in place of shadows to maintain compatibility in pre-21 legacy backgrounds. If a
   * pre-21 background has been updated to a MaterialShapeDrawable, MaterialShapeDrawable will draw
   * shadows instead.
   */
  private void addCompatibilityTopDivider(@NonNull Context context) {
    View divider = new View(context);
    divider.setBackgroundColor(
        ContextCompat.getColor(context, R.color.sesl_bottom_navigation_shadow_color));
    FrameLayout.LayoutParams dividerParams =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_shadow_height));
    divider.setLayoutParams(dividerParams);
    addView(divider);
  }

  /**
   * Set a listener that will be notified when a bottom navigation item is selected. This listener
   * will also be notified when the currently selected item is reselected, unless an {@link
   * OnNavigationItemReselectedListener} has also been set.
   *
   * @param listener The listener to notify
   * @see #setOnNavigationItemReselectedListener(OnNavigationItemReselectedListener)
   */
  @Deprecated
  public void setOnNavigationItemSelectedListener(
      @Nullable OnNavigationItemSelectedListener listener) {
    setOnItemSelectedListener(listener);
  }

  /**
   * Set a listener that will be notified when the currently selected bottom navigation item is
   * reselected. This does not require an {@link OnNavigationItemSelectedListener} to be set.
   *
   * @param listener The listener to notify
   * @see #setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener)
   */
  @Deprecated
  public void setOnNavigationItemReselectedListener(
      @Nullable OnNavigationItemReselectedListener listener) {
    setOnItemReselectedListener(listener);
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == VISIBLE) {
      seslSetTouchDelegateForBottomBar();
    } else {
      seslRemoveListenerForTouchDelegate();
    }
  }

  @Override
  public void seslSetGroupDividerEnabled(boolean enabled) {
    super.seslSetGroupDividerEnabled(enabled);
  }

  private void seslSetTouchDelegateForBottomBar() {
    final ViewTreeObserver vto = getViewTreeObserver();
    if (vto != null && mOnGlobalLayoutListenerForTD != null) {
      mOnGlobalLayoutListenerForTD = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          final BottomNavigationView bottomNaviView = BottomNavigationView.this;
          if (bottomNaviView != null) {
            bottomNaviView.post(new Runnable() {
              // TODO rework this method
              // kang
              @Override
              public void run() {
                SeslTouchTargetDelegate var1 = new SeslTouchTargetDelegate(bottomNaviView);
                int var2 = bottomNaviView.getChildCount();
                int var3 = 0;
                int var4 = 0;

                View var5;
                while(true) {
                  if (var4 >= var2) {
                    var5 = null;
                    break;
                  }

                  var5 = bottomNaviView.getChildAt(var4);
                  if (var5 instanceof BottomNavigationMenuView) {
                    break;
                  }

                  ++var4;
                }

                var4 = var3;
                if (var5 != null) {
                  var4 = var3;
                  if (var5.getVisibility() == VISIBLE) {
                    ViewGroup var6 = (ViewGroup)var5;
                    int var7 = var6.getChildCount();
                    var3 = 0;

                    for(var4 = var3; var3 < var7; ++var3) {
                      var5 = var6.getChildAt(var3);
                      if (var5.getVisibility() == VISIBLE) {
                        var4 = var5.getMeasuredHeight() / 2;
                        if (var3 == 0) {
                          var2 = var4;
                        } else {
                          var2 = 0;
                        }

                        int var8;
                        if (var3 == var7 - 1) {
                          var8 = var4;
                        } else {
                          var8 = 0;
                        }

                        var1.addTouchDelegate(var5, SeslTouchTargetDelegate.ExtraInsets.of(var2, var4, var8, var4));
                        var4 = 1;
                      }
                    }
                  }
                }

                if (var4 != 0) {
                  bottomNaviView.setTouchDelegate(var1);
                }

              }
              // kang
            });
          }
        }
      };;
      vto.addOnGlobalLayoutListener(mOnGlobalLayoutListenerForTD);
    }
  }

  private void seslRemoveListenerForTouchDelegate() {
    if (mOnGlobalLayoutListenerForTD != null) {
      getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListenerForTD);
      mOnGlobalLayoutListenerForTD = null;
    }
  }

  /** Listener for handling selection events on bottom navigation items. */
  @Deprecated
  public interface OnNavigationItemSelectedListener extends OnItemSelectedListener {}

  /** Listener for handling reselection events on bottom navigation items. */
  @Deprecated
  public interface OnNavigationItemReselectedListener extends OnItemReselectedListener {}
}
