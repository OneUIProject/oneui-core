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

package com.google.android.material.navigation;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static java.lang.Math.max;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.PointerIconCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.TooltipCompat;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Provides a view that will be used to render destination items inside a {@link
 * NavigationBarMenuView}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class NavigationBarItemView extends FrameLayout implements MenuView.ItemView {
  // Sesl
  private String TAG = NavigationBarItemView.class.getSimpleName();

  private static final float MAX_FONT_SCALE = 1.3f;

  static final int BADGE_TYPE_OVERFLOW = 0;
  static final int BADGE_TYPE_DOT = 1;
  static final int BADGE_TYPE_N = 2;

  private SpannableStringBuilder mLabelImgSpan;

  private int mBadgeType = BADGE_TYPE_DOT;
  private int mLargeLabelAppearance;
  private int mSmallLabelAppearance;
  private int mViewType;

  private boolean mIsBadgeNumberless;
  // Sesl

  private static final int INVALID_ITEM_POSITION = -1;
  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

  private int defaultMargin;
  private float shiftAmount;
  private float scaleUpFactor;
  private float scaleDownFactor;

  private int labelVisibilityMode;
  private boolean isShifting;

  private ImageView icon;
  private final ViewGroup labelGroup;
  private final TextView smallLabel;
  private final TextView largeLabel;
  private int itemPosition = INVALID_ITEM_POSITION;

  @Nullable private MenuItemImpl itemData;

  @Nullable private ColorStateList iconTint;
  @Nullable private Drawable originalIconDrawable;
  @Nullable private Drawable wrappedIconDrawable;

  @Nullable private BadgeDrawable badgeDrawable;

  public NavigationBarItemView(@NonNull Context context) {
    this(context, null, NavigationBarView.SESL_TYPE_ICON_LABEL);
  }

  public NavigationBarItemView(@NonNull Context context, int defStyleAttr) {
    this(context, null, defStyleAttr);
  }

  public NavigationBarItemView(@NonNull Context context,
                               @Nullable AttributeSet attrs, int viewType) {
    this(context, attrs, 0, viewType);
  }

  public NavigationBarItemView(@NonNull Context context,
                               @Nullable AttributeSet attrs, int defStyleAttr, int viewType) {
    super(context, attrs, defStyleAttr);

    mViewType = viewType;

    LayoutInflater.from(context).inflate(getItemLayoutResId(), this, true);
    icon = findViewById(R.id.navigation_bar_item_icon_view);
    labelGroup = findViewById(R.id.navigation_bar_item_labels_group);
    smallLabel = findViewById(R.id.navigation_bar_item_small_label_view);
    largeLabel = findViewById(R.id.navigation_bar_item_large_label_view);

    setBackgroundResource(getItemBackgroundResId());

    defaultMargin = getResources().getDimensionPixelSize(getItemDefaultMarginResId());

    // Save the original bottom padding from the label group so it can be animated to and from
    // during label visibility changes.
    labelGroup.setTag(R.id.mtrl_view_tag_bottom_padding, labelGroup.getPaddingBottom());

    // The labels used aren't always visible, so they are unreliable for accessibility. Instead,
    // the content description of the NavigationBarItemView should be used for accessibility.
    ViewCompat.setImportantForAccessibility(smallLabel, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    ViewCompat.setImportantForAccessibility(largeLabel, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    setFocusable(true);
    calculateTextScaleFactors(smallLabel.getTextSize(), largeLabel.getTextSize());

    // TODO(b/138148581): Support displaying a badge on label-only bottom navigation views.
    if (icon != null) {
      icon.addOnLayoutChangeListener(
          new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                View v,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
              if (icon.getVisibility() == VISIBLE) {
                tryUpdateBadgeBounds(icon);
              }
            }
          });
    }

    ViewCompat.setAccessibilityDelegate(this, null);
  }

  @RestrictTo(LIBRARY)
  public int getViewType() {
    return mViewType;
  }

  @Override
  protected int getSuggestedMinimumWidth() {
    LayoutParams labelGroupParams = (LayoutParams) labelGroup.getLayoutParams();
    int labelWidth =
        labelGroupParams.leftMargin + labelGroup.getMeasuredWidth() + labelGroupParams.rightMargin;

    return max(getSuggestedIconWidth(), labelWidth);
  }

  @Override
  protected int getSuggestedMinimumHeight() {
    LayoutParams labelGroupParams = (LayoutParams) labelGroup.getLayoutParams();
    return getSuggestedIconHeight()
        + labelGroupParams.topMargin
        + labelGroup.getMeasuredHeight()
        + labelGroupParams.bottomMargin;
  }

  @Override
  public void initialize(@NonNull MenuItemImpl itemData, int menuType) {
    this.itemData = itemData;
    setCheckable(itemData.isCheckable());
    setChecked(itemData.isChecked());
    setEnabled(itemData.isEnabled());
    setIcon(itemData.getIcon());
    setTitle(itemData.getTitle());
    setId(itemData.getItemId());
    if (!TextUtils.isEmpty(itemData.getContentDescription())) {
      setContentDescription(itemData.getContentDescription());
    }

    CharSequence tooltipText = itemData.getTooltipText();
    TooltipCompat.setTooltipText(this, tooltipText);

    String badgeText = itemData.getBadgeText();
    int badgeType = BADGE_TYPE_DOT;
    if (badgeText != null && !badgeText.equals("") && !badgeText.isEmpty()) {
      badgeType = itemData.getItemId() == R.id.bottom_overflow ?
              BADGE_TYPE_OVERFLOW : BADGE_TYPE_N;
    }
    setBadgeType(badgeType);
  }

  void setBadgeType(int type) {
    mBadgeType = type;
  }

  int getBadgeType() {
    return mBadgeType;
  }

  TextView getLabel() {
    return smallLabel != null ? smallLabel : largeLabel;
  }

  public void setItemPosition(int position) {
    itemPosition = position;
  }

  public int getItemPosition() {
    return itemPosition;
  }

  public void setShifting(boolean shifting) {
    if (isShifting != shifting) {
      isShifting = shifting;

      boolean initialized = itemData != null;
      if (initialized) {
        setChecked(itemData.isChecked());
      }
    }
  }

  public void setLabelVisibilityMode(@NavigationBarView.LabelVisibility int mode) {
    if (labelVisibilityMode != mode) {
      labelVisibilityMode = mode;

      boolean initialized = itemData != null;
      if (initialized) {
        setChecked(itemData.isChecked());
      }
    }
  }

  @Override
  @Nullable
  public MenuItemImpl getItemData() {
    return itemData;
  }

  @Override
  public void setTitle(@Nullable CharSequence title) {
    smallLabel.setText(title);
    largeLabel.setText(title);
    if (TextUtils.isEmpty(title)) {
      smallLabel.setVisibility(View.GONE);
      largeLabel.setVisibility(View.GONE);
    }
    if (itemData == null || TextUtils.isEmpty(itemData.getContentDescription())) {
      setContentDescription(title);
    }

    CharSequence tooltipText =
        itemData == null ? null : itemData.getTooltipText();
    TooltipCompat.setTooltipText(this, tooltipText);
  }

  void setLabelImageSpan(SpannableStringBuilder span) {
    mLabelImgSpan = span;
    smallLabel.setText(span);
    largeLabel.setText(span);
  }

  SpannableStringBuilder getLabelImageSpan() {
    return mLabelImgSpan;
  }

  @Override
  public void setCheckable(boolean checkable) {
    refreshDrawableState();
  }

  @Override
  public void setChecked(boolean checked) {
    largeLabel.setPivotX(largeLabel.getWidth() / 2);
    largeLabel.setPivotY(largeLabel.getBaseline());
    smallLabel.setPivotX(smallLabel.getWidth() / 2);
    smallLabel.setPivotY(smallLabel.getBaseline());

    if (getViewType() != NavigationBarView.SESL_TYPE_LABEL_ONLY) {
      defaultMargin = getResources().getDimensionPixelSize(R.dimen.sesl_navigation_bar_icon_inset);
    }

    switch (labelVisibilityMode) {
      case NavigationBarView.LABEL_VISIBILITY_AUTO:
        if (isShifting) {
          if (checked) {
            setViewLayoutParams(icon, defaultMargin, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            setViewScaleValues(largeLabel, 1f, 1f, VISIBLE);
          } else {
            setViewLayoutParams(icon, defaultMargin, Gravity.CENTER);
            setViewScaleValues(largeLabel, 0.5f, 0.5f, INVISIBLE);
          }
          smallLabel.setVisibility(INVISIBLE);
        } else {
          if (checked) {
            setViewLayoutParams(
                icon, (int) (defaultMargin + shiftAmount), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            setViewScaleValues(largeLabel, 1f, 1f, INVISIBLE);
            setViewScaleValues(smallLabel, scaleUpFactor, scaleUpFactor, VISIBLE);
          } else {
            setViewLayoutParams(icon, defaultMargin, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            setViewScaleValues(largeLabel, scaleDownFactor, scaleDownFactor, INVISIBLE);
            setViewScaleValues(smallLabel, 1f, 1f, VISIBLE);
          }
        }
        break;

      case NavigationBarView.LABEL_VISIBILITY_SELECTED:
        if (checked) {
          setViewLayoutParams(icon, defaultMargin, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
          setViewScaleValues(largeLabel, 1f, 1f, VISIBLE);
        } else {
          setViewLayoutParams(icon, defaultMargin, Gravity.CENTER);
          setViewScaleValues(largeLabel, 0.5f, 0.5f, INVISIBLE);
        }
        smallLabel.setVisibility(INVISIBLE);
        break;

      case NavigationBarView.LABEL_VISIBILITY_LABELED:
        if (checked) {
          setViewLayoutParams(
              icon, (int) (defaultMargin + shiftAmount), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
          setViewScaleValues(largeLabel, 1f, 1f, VISIBLE);
          setViewScaleValues(smallLabel, scaleUpFactor, scaleUpFactor, INVISIBLE);
        } else {
          setViewLayoutParams(icon, defaultMargin, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
          setViewScaleValues(largeLabel, scaleDownFactor, scaleDownFactor, INVISIBLE);
          setViewScaleValues(smallLabel, 1f, 1f, VISIBLE);
        }
        break;

      case NavigationBarView.LABEL_VISIBILITY_UNLABELED:
        setViewLayoutParams(icon, defaultMargin, Gravity.CENTER);
        largeLabel.setVisibility(GONE);
        smallLabel.setVisibility(GONE);
        break;

      default:
        break;
    }

    refreshDrawableState();
  }

  void updateLabelGroupTopMargin(int topMargin) {
    if (labelGroup != null) {
      defaultMargin = getResources()
              .getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_inset);

      ViewGroup.MarginLayoutParams lp
              = (ViewGroup.MarginLayoutParams) labelGroup.getLayoutParams();
      if (lp != null) {
        lp.topMargin = topMargin + defaultMargin;
        labelGroup.setLayoutParams(lp);
      }
    }
  }

  void setBadgeNumberless(boolean numberless) {
    mIsBadgeNumberless = numberless;
  }

  // TODO rework this method
  // kang
  @Override
  public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo var1) {
    /* var1 = info */
    super.onInitializeAccessibilityNodeInfo(var1);
    if (this.itemData != null) {
      BadgeDrawable var2 = this.badgeDrawable;
      if (var2 != null && var2.isVisible()) {
        CharSequence var6 = this.itemData.getTitle();
        if (!TextUtils.isEmpty(this.itemData.getContentDescription())) {
          var6 = this.itemData.getContentDescription();
        }

        var1.setContentDescription(var6 + ", " + this.badgeDrawable.getContentDescription());
      }
    }

    TextView var3 = (TextView)this.findViewById(R.id.notifications_badge);
    if (this.itemData != null && var3 != null && var3.getVisibility() == 0 && var3.getWidth() > 0) {
      CharSequence var4 = this.itemData.getTitle();
      String var7 = var4.toString();
      if (TextUtils.isEmpty(this.itemData.getContentDescription())) {
        int var5 = this.mBadgeType;
        if (var5 != 0) {
          if (var5 != 1) {
            if (var5 == 2) {
              var7 = var3.getText().toString();
              if (this.isNumericValue(var7)) {
                var5 = Integer.parseInt(var7);
                var7 = var4 + " , " + this.getResources().getQuantityString(R.plurals.mtrl_badge_content_description, var5, new Object[]{var5});
              } else if (this.mIsBadgeNumberless) {
                var7 = var4 + " , " + this.getResources().getString(R.string.mtrl_exceed_max_badge_number_content_description, new Object[]{999});
              } else {
                var7 = var4 + " , " + this.getResources().getString(R.string.sesl_material_badge_description);
              }
            }
          } else {
            var7 = var4 + " , " + this.getResources().getString(R.string.mtrl_badge_numberless_content_description);
          }
        } else {
          var7 = var4 + " , " + this.getResources().getString(R.string.sesl_material_badge_description);
        }
      } else {
        var7 = this.itemData.getContentDescription().toString();
      }

      var1.setContentDescription(var7);
    }

    AccessibilityNodeInfoCompat var8 = AccessibilityNodeInfoCompat.wrap(var1);
    var8.setCollectionItemInfo(CollectionItemInfoCompat.obtain(0, 1, this.getItemVisiblePosition(), 1, false, this.isSelected()));
    if (this.isSelected()) {
      var8.setClickable(false);
      var8.removeAction(AccessibilityActionCompat.ACTION_CLICK);
    }

    var1.setClassName(Button.class.getName());
  }
  // kang

  private boolean isNumericValue(String value) {
    if (value == null) {
      return false;
    }
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Iterate through all the preceding bottom navigating items to determine this item's visible
   * position.
   *
   * @return This item's visible position in a bottom navigation.
   */
  private int getItemVisiblePosition() {
    ViewGroup parent = (ViewGroup) getParent();
    int index = parent.indexOfChild(this);
    int visiblePosition = 0;
    for (int i = 0; i < index; i++) {
      View child = parent.getChildAt(i);
      if (child instanceof NavigationBarItemView && child.getVisibility() == View.VISIBLE) {
        visiblePosition++;
      }
    }
    return visiblePosition;
  }

  private static void setViewLayoutParams(@NonNull View view, int topMargin, int gravity) {
    LayoutParams viewParams = (LayoutParams) view.getLayoutParams();
    viewParams.topMargin = topMargin;
    viewParams.gravity = gravity;
    view.setLayoutParams(viewParams);
  }

  private static void setViewScaleValues(
      @NonNull View view, float scaleX, float scaleY, int visibility) {
    view.setScaleX(scaleX);
    view.setScaleY(scaleY);
    view.setVisibility(visibility);
  }

  private static void updateViewPaddingBottom(@NonNull View view, int paddingBottom) {
    view.setPadding(
        view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), paddingBottom);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    smallLabel.setEnabled(enabled);
    largeLabel.setEnabled(enabled);
    icon.setEnabled(enabled);

    if (enabled) {
      ViewCompat.setPointerIcon(
          this, PointerIconCompat.getSystemIcon(getContext(), PointerIconCompat.TYPE_HAND));
    } else {
      ViewCompat.setPointerIcon(this, null);
    }
  }

  @Override
  @NonNull
  public int[] onCreateDrawableState(final int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (itemData != null && itemData.isCheckable() && itemData.isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }

  @Override
  public void setShortcut(boolean showShortcut, char shortcutKey) {}

  @Override
  public void setIcon(@Nullable Drawable iconDrawable) {
    if (iconDrawable == originalIconDrawable) {
      return;
    }

    // Save the original icon to check if it has changed in future calls of this method.
    originalIconDrawable = iconDrawable;
    if (iconDrawable != null) {
      Drawable.ConstantState state = iconDrawable.getConstantState();
      iconDrawable =
          DrawableCompat.wrap(state == null ? iconDrawable : state.newDrawable()).mutate();
      wrappedIconDrawable = iconDrawable;
      if (iconTint != null) {
        DrawableCompat.setTintList(wrappedIconDrawable, iconTint);
      }
    }
    this.icon.setImageDrawable(iconDrawable);
  }

  @Override
  public boolean prefersCondensedTitle() {
    return false;
  }

  @Override
  public boolean showsIcon() {
    return true;
  }

  public void setIconTintList(@Nullable ColorStateList tint) {
    iconTint = tint;
    if ((itemData != null || wrappedIconDrawable != null)
            && itemData != null && wrappedIconDrawable != null) {
      DrawableCompat.setTintList(wrappedIconDrawable, iconTint);
      wrappedIconDrawable.invalidateSelf();
    }
  }

  public void setIconSize(int iconSize) {
    LayoutParams iconParams = (LayoutParams) icon.getLayoutParams();
    iconParams.width = iconSize;
    iconParams.height = iconSize;
    icon.setLayoutParams(iconParams);
  }

  public void seslSetLabelTextAppearance(@StyleRes int labelTextAppearance) {
    mLargeLabelAppearance = labelTextAppearance;
    mSmallLabelAppearance = labelTextAppearance;
    TextViewCompat.setTextAppearance(smallLabel, labelTextAppearance);
    calculateTextScaleFactors(smallLabel.getTextSize(), largeLabel.getTextSize());
    setLargeTextSize(mLargeLabelAppearance, largeLabel);
    setLargeTextSize(mSmallLabelAppearance, smallLabel);
  }

  public void setTextAppearanceInactive(@StyleRes int inactiveTextAppearance) {
    TextViewCompat.setTextAppearance(smallLabel, inactiveTextAppearance);
    calculateTextScaleFactors(smallLabel.getTextSize(), largeLabel.getTextSize());
  }

  public void setTextAppearanceActive(@StyleRes int activeTextAppearance) {
    TextViewCompat.setTextAppearance(largeLabel, activeTextAppearance);
    calculateTextScaleFactors(smallLabel.getTextSize(), largeLabel.getTextSize());
  }

  public void setTextColor(@Nullable ColorStateList color) {
    if (color != null) {
      smallLabel.setTextColor(color);
      largeLabel.setTextColor(color);
    }
  }

  private void calculateTextScaleFactors(float smallLabelSize, float largeLabelSize) {
    if (largeLabelSize == 0f || smallLabelSize == 0f) {
      Log.e(TAG, "LabelSize is invalid");
      scaleUpFactor = 1f;
      scaleDownFactor = 1f;
      shiftAmount = 0f;
    } else {
      shiftAmount = smallLabelSize - largeLabelSize;
      scaleUpFactor = 1f * largeLabelSize / smallLabelSize;
      scaleDownFactor = 1f * smallLabelSize / largeLabelSize;
      if (scaleUpFactor >= Float.MAX_VALUE || scaleUpFactor <= -Float.MAX_VALUE) {
        Log.e(TAG, "scaleUpFactor is invalid");
        scaleUpFactor = 1f;
        shiftAmount = 0f;
      }
      if (scaleDownFactor >= Float.MAX_VALUE || scaleDownFactor <= -Float.MAX_VALUE) {
        Log.e(TAG, "scaleDownFactor is invalid");
        scaleDownFactor = 1f;
        shiftAmount = 0f;
      }
    }
  }

  public void setItemBackground(int background) {
    Drawable backgroundDrawable =
        background == 0 ? null : ContextCompat.getDrawable(getContext(), background);
    setItemBackground(backgroundDrawable);
  }

  public void setItemBackground(@Nullable Drawable background) {
    if (background != null && background.getConstantState() != null) {
      background = background.getConstantState().newDrawable().mutate();
    }
    ViewCompat.setBackground(this, background);
  }

  void setBadge(@NonNull BadgeDrawable badgeDrawable) {
    this.badgeDrawable = badgeDrawable;
    if (icon != null) {
      tryAttachBadgeToAnchor(icon);
    }
  }

  @Nullable
  public BadgeDrawable getBadge() {
    return this.badgeDrawable;
  }

  void removeBadge() {
    tryRemoveBadgeFromAnchor(icon);
  }

  private boolean hasBadge() {
    return badgeDrawable != null;
  }

  private void tryUpdateBadgeBounds(View anchorView) {
    if (!hasBadge()) {
      return;
    }
    BadgeUtils.setBadgeDrawableBounds(
        badgeDrawable, anchorView, getCustomParentForBadge(anchorView));
  }

  private void tryAttachBadgeToAnchor(@Nullable View anchorView) {
    if (!hasBadge()) {
      return;
    }
    if (anchorView != null) {
      // Avoid clipping a badge if it's displayed.
      setClipChildren(false);
      setClipToPadding(false);

      BadgeUtils.attachBadgeDrawable(
          badgeDrawable, anchorView, getCustomParentForBadge(anchorView));
    }
  }

  private void tryRemoveBadgeFromAnchor(@Nullable View anchorView) {
    if (!hasBadge()) {
      return;
    }
    if (anchorView != null) {
      // Clip children / view to padding when no badge is displayed.
      setClipChildren(true);
      setClipToPadding(true);

      BadgeUtils.detachBadgeDrawable(badgeDrawable, anchorView);
    }
    badgeDrawable = null;
  }

  @Nullable
  private FrameLayout getCustomParentForBadge(View anchorView) {
    if (anchorView == icon) {
      return BadgeUtils.USE_COMPAT_PARENT ? ((FrameLayout) icon.getParent()) : null;
    }
    // TODO(b/138148581): Support displaying a badge on label-only bottom navigation views.
    return null;
  }

  private int getSuggestedIconWidth() {
    int badgeWidth =
        badgeDrawable == null
            ? 0
            : badgeDrawable.getMinimumWidth() - badgeDrawable.getHorizontalOffset();

    // Account for the fact that the badge may fit within the left or right margin. Give the same
    // space of either side so that icon position does not move if badge gravity is changed.
    LayoutParams iconParams = (LayoutParams) icon.getLayoutParams();
    return max(badgeWidth, iconParams.leftMargin)
        + icon.getMeasuredWidth()
        + max(badgeWidth, iconParams.rightMargin);
  }

  private int getSuggestedIconHeight() {
    int badgeHeight = 0;
    if (badgeDrawable != null) {
      badgeHeight = badgeDrawable.getMinimumHeight() / 2;
    }

    // Account for the fact that the badge may fit within the top margin. Bottom margin is ignored
    // because the icon view will be aligned to the baseline of the label group. But give space for
    // the badge at the bottom as well, so that icon does not move if badge gravity is changed.
    LayoutParams iconParams = (LayoutParams) icon.getLayoutParams();
    return max(badgeHeight, iconParams.topMargin) + icon.getMeasuredWidth() + badgeHeight;
  }

  /**
   * Returns the unique identifier to the drawable resource that must be used to render background
   * of the menu item view. Override this if the subclassed menu item requires a different
   * background resource to be set.
   */
  @DrawableRes
  protected int getItemBackgroundResId() {
    return R.drawable.mtrl_navigation_bar_item_background;
  }

  /**
   * Returns the unique identifier to the dimension resource that will specify the default margin
   * this menu item view. Override this if the subclassed menu item requires a different default
   * margin value.
   */
  @DimenRes
  protected int getItemDefaultMarginResId() {
    return R.dimen.mtrl_navigation_bar_item_default_margin;
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    setLargeTextSize(mLargeLabelAppearance, largeLabel);
    setLargeTextSize(mSmallLabelAppearance, smallLabel);
  }

  @RestrictTo(LIBRARY_GROUP_PREFIX)
  void setShowButtonShape(int textColor, ColorStateList bgColor) {
    Drawable background = getResources()
            .getDrawable(R.drawable.sesl_bottom_nav_show_button_shapes_background);
    if (Build.VERSION.SDK_INT >= 28) {
      smallLabel.setTextColor(textColor);
      largeLabel.setTextColor(textColor);
      smallLabel.setBackground(background);
      largeLabel.setBackground(background);
      smallLabel.setBackgroundTintList(bgColor);
      largeLabel.setBackgroundTintList(bgColor);
    } else {
      ViewCompat.setBackground(this, background);
    }
  }

  private void setLargeTextSize(int resId, TextView textView) {
    if (textView != null) {
      TypedArray a = getContext().obtainStyledAttributes(resId, R.styleable.TextAppearance);
      TypedValue outValue = a.peekValue(R.styleable.TextAppearance_android_textSize);
      a.recycle();
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
              TypedValue.complexToFloat(outValue.data)
                      * Math.min(getResources().getConfiguration().fontScale, MAX_FONT_SCALE));
    }
  }

  /**
   * Returns the unique identifier to the layout resource that must be used to render the items in
   * this menu item view.
   */
  @LayoutRes
  protected abstract int getItemLayoutResId();
}
