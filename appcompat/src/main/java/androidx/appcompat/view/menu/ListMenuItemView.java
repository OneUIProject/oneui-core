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

package androidx.appcompat.view.menu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.widget.SeslDropDownItemTextView;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.view.ViewCompat;

import java.text.NumberFormat;
import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * The item view for each item in the ListView-based MenuViews.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ListMenuItemView extends LinearLayout
        implements MenuView.ItemView, AbsListView.SelectionBoundsAdjuster {
    private static final String TAG = "ListMenuItemView";
    private static final int BADGE_LIMIT_NUMBER = 99;
    private MenuItemImpl mItemData;

    private NumberFormat mNumberFormat;

    private ImageView mIconView;
    private RadioButton mRadioButton;
    private RelativeLayout mTitleParent;
    private TextView mTitleView;
    private CheckBox mCheckBox;
    private TextView mBadgeView;
    private TextView mShortcutView;
    private ImageView mSubMenuArrowView;
    private ImageView mGroupDivider;
    private LinearLayout mContent;
    private SeslDropDownItemTextView mDropDownItemTextView;

    private Drawable mBackground;
    private int mTextAppearance;
    private Context mTextAppearanceContext;
    private boolean mPreserveIconSpacing;
    private Drawable mSubMenuArrow;
    private boolean mHasListDivider;
    private boolean mIsSubMenu = false;

    private LayoutInflater mInflater;

    private boolean mForceShowIcon;

    public ListMenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.listMenuViewStyle);
    }

    public ListMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(),
                attrs, R.styleable.MenuView, defStyleAttr, 0);

        mBackground = a.getDrawable(R.styleable.MenuView_android_itemBackground);
        mTextAppearance = a.getResourceId(R.styleable.
                MenuView_android_itemTextAppearance, -1);
        mPreserveIconSpacing = a.getBoolean(
                R.styleable.MenuView_preserveIconSpacing, false);
        mTextAppearanceContext = context;
        mSubMenuArrow = a.getDrawable(R.styleable.MenuView_subMenuArrow);

        final TypedArray b = context.getTheme()
                .obtainStyledAttributes(null, new int[] { android.R.attr.divider },
                        R.attr.dropDownListViewStyle, 0);
        mHasListDivider = b.hasValue(0);

        a.recycle();
        b.recycle();

        mNumberFormat = NumberFormat.getInstance(Locale.getDefault());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ViewCompat.setBackground(this, mBackground);

        mDropDownItemTextView = findViewById(R.id.sub_menu_title);;

        mIsSubMenu = mDropDownItemTextView != null;

        if (!mIsSubMenu) {
            mTitleView = findViewById(R.id.title);
            if (mTextAppearance != -1) {
                mTitleView.setTextAppearance(mTextAppearanceContext,
                        mTextAppearance);
            }
            if (mTitleView != null) {
                mTitleView.setSingleLine(false);
                mTitleView.setMaxLines(2);
            }

            mShortcutView = findViewById(R.id.shortcut);
            mSubMenuArrowView = findViewById(R.id.submenuarrow);
            if (mSubMenuArrowView != null) {
                mSubMenuArrowView.setImageDrawable(mSubMenuArrow);
            }
            mGroupDivider = findViewById(R.id.group_divider);

            mContent = findViewById(R.id.content);
            mTitleParent = findViewById(R.id.title_parent);
        }
    }

    @Override
    public void initialize(MenuItemImpl itemData, int menuType) {
        mItemData = itemData;

        setVisibility(itemData.isVisible() ? View.VISIBLE : View.GONE);

        setTitle(itemData.getTitleForItemView(this));
        setCheckable(itemData.isCheckable());
        setShortcut(itemData.shouldShowShortcut(), itemData.getShortcut());
        setIcon(itemData.getIcon());
        setEnabled(itemData.isEnabled());
        setSubMenuArrowVisible(itemData.hasSubMenu());
        setContentDescription(itemData.getContentDescription());
        setBadgeText(itemData.getBadgeText());
    }

    private void addContentView(View v) {
        addContentView(v, -1);
    }

    private void addContentView(View v, int index) {
        if (mContent != null) {
            mContent.addView(v, index);
        } else {
            addView(v, index);
        }
    }

    public void setForceShowIcon(boolean forceShow) {
        mPreserveIconSpacing = mForceShowIcon = forceShow;
    }

    @Override
    public void setTitle(CharSequence title) {
        if (mIsSubMenu) {
            if (title != null) {
                mDropDownItemTextView.setText(title);

                if (mDropDownItemTextView.getVisibility() != VISIBLE)
                    mDropDownItemTextView.setVisibility(VISIBLE);
            } else {
                if (mDropDownItemTextView.getVisibility() != GONE)
                    mDropDownItemTextView.setVisibility(GONE);
            }
        } else {
            if (title != null) {
                mTitleView.setText(title);

                if (mTitleView.getVisibility() != VISIBLE) mTitleView.setVisibility(VISIBLE);
            } else {
                if (mTitleView.getVisibility() != GONE) mTitleView.setVisibility(GONE);
            }
        }
    }

    @Override
    public MenuItemImpl getItemData() {
        return mItemData;
    }

    @Override
    public void setCheckable(boolean checkable) {
        if (!checkable && mRadioButton == null && mCheckBox == null) {
            return;
        }

        if (!mIsSubMenu) {
            // Depending on whether its exclusive check or not, the checkbox or
            // radio button will be the one in use (and the other will be otherCompoundButton)
            final CompoundButton compoundButton;
            final CompoundButton otherCompoundButton;

            if (mItemData.isExclusiveCheckable()) {
                if (mRadioButton == null) {
                    insertRadioButton();
                }
                compoundButton = mRadioButton;
                otherCompoundButton = mCheckBox;
            } else {
                if (mCheckBox == null) {
                    insertCheckBox();
                }
                compoundButton = mCheckBox;
                otherCompoundButton = mRadioButton;
            }

            if (checkable) {
                compoundButton.setChecked(mItemData.isChecked());

                final int newVisibility = checkable ? VISIBLE : GONE;
                if (compoundButton.getVisibility() != newVisibility) {
                    compoundButton.setVisibility(newVisibility);
                }

                // Make sure the other compound button isn't visible
                if (otherCompoundButton != null && otherCompoundButton.getVisibility() != GONE) {
                    otherCompoundButton.setVisibility(GONE);
                }
            } else {
                if (mCheckBox != null) {
                    mCheckBox.setVisibility(GONE);
                }
                if (mRadioButton != null) {
                    mRadioButton.setVisibility(GONE);
                }
            }
        } else {
            if (checkable) {
                mDropDownItemTextView.setChecked(mItemData.isChecked());
            }
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (mIsSubMenu) {
            mDropDownItemTextView.setChecked(checked);
            return;
        }

        CompoundButton compoundButton;

        if (mItemData.isExclusiveCheckable()) {
            if (mRadioButton == null) {
                insertRadioButton();
            }
            compoundButton = mRadioButton;
        } else {
            if (mCheckBox == null) {
                insertCheckBox();
            }
            compoundButton = mCheckBox;
        }

        compoundButton.setChecked(checked);
    }

    private void setSubMenuArrowVisible(boolean hasSubmenu) {
        if (mSubMenuArrowView != null && !mIsSubMenu) {
            mSubMenuArrowView.setVisibility(hasSubmenu ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setShortcut(boolean showShortcut, char shortcutKey) {
        if (mIsSubMenu) {
            return;
        }

        final int newVisibility = (showShortcut && mItemData.shouldShowShortcut())
                ? VISIBLE : GONE;

        if (newVisibility == VISIBLE) {
            mShortcutView.setText(mItemData.getShortcutLabel());
        }

        if (mShortcutView.getVisibility() != newVisibility) {
            mShortcutView.setVisibility(newVisibility);
        }
    }

    @Override
    public void setIcon(Drawable icon) {
        if (mIsSubMenu) {
            return;
        }

        final boolean showIcon = mItemData.shouldShowIcon() || mForceShowIcon;
        if (!showIcon && !mPreserveIconSpacing) {
            return;
        }

        if (mIconView == null && icon == null && !mPreserveIconSpacing) {
            return;
        }

        if (mIconView == null) {
            insertIconView();
        }

        if (icon != null || mPreserveIconSpacing) {
            mIconView.setImageDrawable(showIcon ? icon : null);

            if (mIconView.getVisibility() != VISIBLE) {
                mIconView.setVisibility(VISIBLE);
            }
        } else {
            mIconView.setVisibility(GONE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIconView != null && mPreserveIconSpacing && !mIsSubMenu) {
            // Enforce minimum icon spacing
            ViewGroup.LayoutParams lp = getLayoutParams();
            LayoutParams iconLp = (LayoutParams) mIconView.getLayoutParams();
            if (lp.height > 0 && iconLp.width <= 0) {
                iconLp.width = lp.height;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void insertIconView() {
        if (!mIsSubMenu) {
            LayoutInflater inflater = getInflater();
            mIconView = (ImageView) inflater.inflate(R.layout.abc_list_menu_item_icon,
                    this, false);
            addContentView(mIconView, 0);
        }
    }

    private void insertRadioButton() {
        LayoutInflater inflater = getInflater();
        mRadioButton =
                (RadioButton) inflater.inflate(R.layout.sesl_list_menu_item_radio,
                        this, false);
        addContentView(mRadioButton);
    }

    private void insertCheckBox() {
        LayoutInflater inflater = getInflater();
        mCheckBox =
                (CheckBox) inflater.inflate(R.layout.sesl_list_menu_item_checkbox,
                        this, false);
        addContentView(mCheckBox);
    }

    @Override
    public boolean prefersCondensedTitle() {
        return false;
    }

    @Override
    public boolean showsIcon() {
        return mForceShowIcon;
    }

    private LayoutInflater getInflater() {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(getContext());
        }
        return mInflater;
    }

    /**
     * Enable or disable group dividers for this view.
     */
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        // If mHasListDivider is true, disabling the groupDivider.
        // Otherwise, checking enbling it according to groupDividerEnabled flag.
        if (mGroupDivider != null) {
            mGroupDivider.setVisibility(!mHasListDivider
                    && groupDividerEnabled ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void adjustListItemSelectionBounds(Rect rect) {
        if (mGroupDivider != null && mGroupDivider.getVisibility() == View.VISIBLE) {
            // groupDivider is a part of ListMenuItemView.
            // If ListMenuItem with divider enabled is hovered/clicked, divider also gets selected.
            // Clipping the selector bounds from the top divider portion when divider is enabled,
            // so that divider does not get selected on hover or click.
            final LayoutParams lp = (LayoutParams) mGroupDivider.getLayoutParams();
            rect.top += mGroupDivider.getHeight() + lp.topMargin + lp.bottomMargin;
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo nodeInfo) {
        super.onInitializeAccessibilityNodeInfo(nodeInfo);

        if (mBadgeView != null && mBadgeView.getVisibility() == View.VISIBLE
                && mBadgeView.getWidth() > 0) {
            if (!TextUtils.isEmpty(getContentDescription())) {
                nodeInfo.setContentDescription(getContentDescription());
            } else {
                nodeInfo.setContentDescription(((Object) mItemData.getTitle()) + " , "
                        + getResources().getString(R.string.sesl_action_menu_overflow_badge_description));
            }
        }
    }

    private void setBadgeText(String text) {
        if (mBadgeView == null) {
            mBadgeView = findViewById(R.id.menu_badge);
        }

        if (mBadgeView == null) {
            Log.i(TAG, "SUB_MENU_ITEM_LAYOUT case, mBadgeView is null");
            return;
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBadgeView.getLayoutParams();

        if (isNumericValue(text)) {
            final String localeFormattedNumber = mNumberFormat.format(Math.min(Integer.parseInt(text), BADGE_LIMIT_NUMBER));
            mBadgeView.setText(localeFormattedNumber);
            lp.width = (int) (getResources().getDimension(R.dimen.sesl_badge_default_width)
                    + (localeFormattedNumber.length() * getResources().getDimension(R.dimen.sesl_badge_additional_width)));
            mBadgeView.setLayoutParams(lp);
        } else {
            mBadgeView.setText(text);
            if (text != null && !text.isEmpty()) {
                mBadgeView.measure(0, 0);
                int measuredWidth = mBadgeView.getMeasuredWidth();
                if (lp.width != measuredWidth) {
                    lp.width = measuredWidth
                            + ((int) getResources().getDimension(R.dimen.sesl_badge_additional_width));
                    mBadgeView.setLayoutParams(lp);
                }
            }
        }

        if (text != null && mTitleParent != null) {
            mTitleParent.setPaddingRelative(0,
                    0,
                    lp.width + getResources().getDimensionPixelSize(R.dimen.sesl_menu_item_badge_end_margin),
                    0);
        }

        mBadgeView.setVisibility(text != null ? VISIBLE : GONE);
    }

    private boolean isNumericValue(String str) {
        if (str == null) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

