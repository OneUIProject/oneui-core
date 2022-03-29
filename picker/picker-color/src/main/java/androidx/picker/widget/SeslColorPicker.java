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

package androidx.picker.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.picker.R;

import java.util.ArrayList;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslColorPicker extends LinearLayout {
    private static final float SCALE_LARGE = 1.2f;

    static final int RECENT_COLOR_SLOT_COUNT = 6;

    private static final int CURRENT_COLOR_VIEW = 0;
    private static final int NEW_COLOR_VIEW = 1;

    private static final int RIPPLE_EFFECT_OPACITY = 61;

    private SeslColorSwatchView mColorSwatchView;
    private final Context mContext;
    private GradientDrawable mCurrentColorBackground;
    private View mCurrentColorContainer;
    private ImageView mCurrentColorView;
    private OnColorChangedListener mOnColorChangedListener;
    private SeslOpacitySeekBar mOpacitySeekBar;
    private FrameLayout mOpacitySeekBarContainer;
    private PickedColor mPickedColor;
    private View mPickedColorContainer;
    private ImageView mPickedColorView;
    private final SeslRecentColorInfo mRecentColorInfo;
    private LinearLayout mRecentColorListLayout;
    private final ArrayList<Integer> mRecentColorValues;
    private View mRecentlyDivider;
    private TextView mRecentlyText;
    private final Resources mResources;
    private GradientDrawable mSelectedColorBackground;
    private String[] mColorDescription = null;

    private float mCurrentFontScale;

    private boolean mIsLightTheme;
    private boolean mIsInputFromUser = false;
    private boolean mIsOpacityBarEnabled = false;

    private final int[] mSmallestWidthDp = {320, 360, 411};

    private final View.OnClickListener mImageButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            for (int i = 0; i < mRecentColorValues.size() && i < RECENT_COLOR_SLOT_COUNT; i++) {
                if (mRecentColorListLayout.getChildAt(i).equals(view)) {
                    mIsInputFromUser = true;

                    final int color = mRecentColorValues.get(i);
                    mPickedColor.setColor(color);
                    mapColorOnColorWheel(color);
                    if (mOnColorChangedListener != null) {
                        mOnColorChangedListener.onColorChanged(color);
                    }
                }
            }
        }
    };

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public SeslColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mResources = getResources();

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.isLightTheme, outValue, true);
        mIsLightTheme = outValue.data != 0;
        mCurrentFontScale = mResources.getConfiguration().fontScale;

        LayoutInflater.from(context).inflate(R.layout.sesl_color_picker_layout, this);

        mRecentColorInfo = new SeslRecentColorInfo();
        mRecentColorValues = mRecentColorInfo.getRecentColorInfo();
        mPickedColor = new PickedColor();

        initDialogPadding();
        initCurrentColorView();
        initColorSwatchView();
        initOpacitySeekBar();
        initRecentColorLayout();
        updateCurrentColor();
        setInitialColors();
    }

    private void initDialogPadding() {
        if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            DisplayMetrics dm = mResources.getDisplayMetrics();

            final float density = dm.density;
            if (density % 1.0f != 0.0f) {
                final float screenWidth = dm.widthPixels;
                final int widthDp = (int) (screenWidth / density);

                if (isContains(widthDp)) {
                    final int opacitySeekbarWidth
                            = mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_seekbar_width);
                    if (screenWidth
                            < (mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_dialog_padding_left) * 2) + opacitySeekbarWidth) {
                        final int horizontalPadding = (int) ((screenWidth - opacitySeekbarWidth) / 2.0f);
                        final int paddingTop = mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_dialog_padding_top);
                        final int paddingBottom = mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_dialog_padding_bottom);

                        LinearLayout colorPickerMainContainer = findViewById(R.id.sesl_color_picker_main_content_container);
                        colorPickerMainContainer.setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom);
                    }
                }
            }
        }
    }

    private boolean isContains(int value) {
        for (int i : mSmallestWidthDp) {
            if (i == value) {
                return true;
            }
        }
        return false;
    }

    private void initCurrentColorView() {
        mCurrentColorView = findViewById(R.id.sesl_color_picker_current_color_view);
        mPickedColorView = findViewById(R.id.sesl_color_picker_picked_color_view);

        final int textColor = mResources.getColor(mIsLightTheme
                ? R.color.sesl_color_picker_selected_color_item_text_color_light
                : R.color.sesl_color_picker_selected_color_item_text_color_dark);
        TextView currentColorText = (TextView) findViewById(R.id.sesl_color_picker_current_color_text);
        currentColorText.setTextColor(textColor);
        TextView pickedColorText = (TextView) findViewById(R.id.sesl_color_picker_picked_color_text);
        pickedColorText.setTextColor(textColor);

        if (mCurrentFontScale > SCALE_LARGE) {
            final float selectedColorTextSize
                    = mResources.getDimensionPixelOffset(R.dimen.sesl_color_picker_selected_color_text_size);
            currentColorText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (float) Math.floor(Math.ceil(selectedColorTextSize / mCurrentFontScale) * (double) SCALE_LARGE));
            pickedColorText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (float) Math.floor(Math.ceil(selectedColorTextSize / mCurrentFontScale) * (double) SCALE_LARGE));
        }

        mCurrentColorContainer = findViewById(R.id.sesl_color_picker_current_color_focus);
        mPickedColorContainer = findViewById(R.id.sesl_color_picker_picked_color_focus);
        mSelectedColorBackground = (GradientDrawable) mPickedColorView.getBackground();

        final Integer pickedIntegerColor = mPickedColor.getColor();
        if (pickedIntegerColor != null) {
            mSelectedColorBackground.setColor(pickedIntegerColor);
        }

        mCurrentColorBackground = (GradientDrawable) mCurrentColorView.getBackground();
    }

    private void initColorSwatchView() {
        mColorSwatchView = findViewById(R.id.sesl_color_picker_color_swatch_view);
        mColorSwatchView.setOnColorSwatchChangedListener(
                new SeslColorSwatchView.OnColorSwatchChangedListener() {
                    @Override
                    public void onColorSwatchChanged(int color) {
                        mIsInputFromUser = true;
                        mPickedColor.setColor(color);
                        updateCurrentColor();
                    }
                });
    }

    private void initOpacitySeekBar() {
        mOpacitySeekBar = findViewById(R.id.sesl_color_picker_opacity_seekbar);
        mOpacitySeekBarContainer = findViewById(R.id.sesl_color_picker_opacity_seekbar_container);

        if (!mIsOpacityBarEnabled) {
            mOpacitySeekBar.setVisibility(View.GONE);
            mOpacitySeekBarContainer.setVisibility(View.GONE);
        }

        mOpacitySeekBar.init(mPickedColor.getColor());

        mOpacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mIsInputFromUser = true;
                }

                mPickedColor.setAlpha(progress);

                final Integer pickedIntegerColor = mPickedColor.getColor();
                if (pickedIntegerColor != null) {
                    if (mSelectedColorBackground != null) {
                        mSelectedColorBackground.setColor(pickedIntegerColor);
                    }
                    if (mOnColorChangedListener != null) {
                        mOnColorChangedListener.onColorChanged(pickedIntegerColor);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        
        mOpacitySeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_DOWN;
            }
        });

        mOpacitySeekBarContainer.setContentDescription(mResources.getString(R.string.sesl_color_picker_opacity)
                + ", " + mResources.getString(R.string.sesl_color_picker_slider)
                + ", " + mResources.getString(R.string.sesl_color_picker_double_tap_to_select));
    }

    private void initRecentColorLayout() {
        mRecentColorListLayout = findViewById(R.id.sesl_color_picker_used_color_item_list_layout);
        mRecentlyText = findViewById(R.id.sesl_color_picker_used_color_divider_text);
        mRecentlyDivider = findViewById(R.id.sesl_color_picker_recently_divider);
        mColorDescription = new String[] {
                mResources.getString(R.string.sesl_color_picker_color_one),
                mResources.getString(R.string.sesl_color_picker_color_two),
                mResources.getString(R.string.sesl_color_picker_color_three),
                mResources.getString(R.string.sesl_color_picker_color_four),
                mResources.getString(R.string.sesl_color_picker_color_five),
                mResources.getString(R.string.sesl_color_picker_color_six)
        };

        final int emptyColor = ContextCompat.getColor(mContext, mIsLightTheme ?
                R.color.sesl_color_picker_used_color_item_empty_slot_color_light
                : R.color.sesl_color_picker_used_color_item_empty_slot_color_dark);
        for (int i = 0; i < RECENT_COLOR_SLOT_COUNT; i++) {
            View recentColorSlot = mRecentColorListLayout.getChildAt(i);
            setImageColor(recentColorSlot, emptyColor);
            recentColorSlot.setFocusable(false);
            recentColorSlot.setClickable(false);
        }

        if (mCurrentFontScale > SCALE_LARGE) {
            final int selectedColorTextSize
                    = mResources.getDimensionPixelOffset(R.dimen.sesl_color_picker_selected_color_text_size);
            mRecentlyText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (float) Math.floor(Math.ceil(selectedColorTextSize / mCurrentFontScale) * (double) SCALE_LARGE));
        }

        final int recentlyTextColor = ContextCompat.getColor(mContext, mIsLightTheme ?
                R.color.sesl_color_picker_used_color_text_color_light
                : R.color.sesl_color_picker_used_color_text_color_dark);
        mRecentlyText.setTextColor(recentlyTextColor);
        mRecentlyDivider.getBackground().setTint(recentlyTextColor);
    }

    public void updateRecentColorLayout() {
        final int size = mRecentColorValues != null
                ? mRecentColorValues.size() : 0;

        final String defaultDescription = ", "
                + mResources.getString(R.string.sesl_color_picker_option);

        for (int i = 0; i < RECENT_COLOR_SLOT_COUNT; i++) {
            View recentColorSlot = mRecentColorListLayout.getChildAt(i);
            if (i < size) {
                final int color = mRecentColorValues.get(i);
                setImageColor(recentColorSlot, color);

                StringBuilder recentDescription = new StringBuilder();
                recentDescription.append(
                        (CharSequence) mColorSwatchView.getColorSwatchDescriptionAt(color));
                recentDescription.insert(0,
                        mColorDescription[i] + defaultDescription + ", ");
                recentColorSlot.setContentDescription(recentDescription);

                recentColorSlot.setFocusable(true);
                recentColorSlot.setClickable(true);
            }
        }

        if (mRecentColorInfo.getCurrentColor() != null) {
            final int currentColor = mRecentColorInfo.getCurrentColor();
            mCurrentColorBackground.setColor(currentColor);
            setCurrentColorViewDescription(currentColor, CURRENT_COLOR_VIEW);
            mSelectedColorBackground.setColor(currentColor);
            mapColorOnColorWheel(currentColor);
        } else if (size != 0) {
            final int firstColor = mRecentColorValues.get(0);
            mCurrentColorBackground.setColor(firstColor);
            setCurrentColorViewDescription(firstColor, CURRENT_COLOR_VIEW);
            mSelectedColorBackground.setColor(firstColor);
            mapColorOnColorWheel(firstColor);
        }

        if (mRecentColorInfo.getNewColor() != null) {
            final int newColor = mRecentColorInfo.getNewColor();
            mSelectedColorBackground.setColor(newColor);
            mapColorOnColorWheel(newColor);
        }
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mOnColorChangedListener = listener;
    }

    private void setInitialColors() {
        final Integer pickedIntegerColor = mPickedColor.getColor();
        if (pickedIntegerColor != null) {
            mapColorOnColorWheel(pickedIntegerColor);
        }
    }

    private void updateCurrentColor() {
        final Integer pickedIntegerColor = mPickedColor.getColor();
        if (pickedIntegerColor != null) {
            if (mOpacitySeekBar != null) {
                mOpacitySeekBar.changeColorBase(pickedIntegerColor);
            }

            if (mSelectedColorBackground != null) {
                mSelectedColorBackground.setColor(pickedIntegerColor);
                setCurrentColorViewDescription(pickedIntegerColor, NEW_COLOR_VIEW);
            }

            if (mOnColorChangedListener != null) {
                mOnColorChangedListener.onColorChanged(pickedIntegerColor);
            }
        }
    }

    private void setImageColor(View button, Integer color) {
        GradientDrawable gradientDrawable
                = (GradientDrawable) mContext.getDrawable(mIsLightTheme ?
                R.drawable.sesl_color_picker_used_color_item_slot_light
                : R.drawable.sesl_color_picker_used_color_item_slot_dark);
        if (color != null) {
            gradientDrawable.setColor(color);
        }

        final int rippleColor = Color.argb(RIPPLE_EFFECT_OPACITY, 0, 0, 0);
        ColorStateList myList = new ColorStateList(new int[][]{new int[0]}, new int[]{rippleColor});
        button.setBackground(new RippleDrawable(myList, gradientDrawable, null));
        button.setOnClickListener(mImageButtonClickListener);
    }

    private void mapColorOnColorWheel(int color) {
        mPickedColor.setColor(color);

        if (mColorSwatchView != null) {
            mColorSwatchView.updateCursorPosition(color);
        }

        if (mOpacitySeekBar != null) {
            mOpacitySeekBar.restoreColor(color);
        }

        if (mSelectedColorBackground != null) {
            mSelectedColorBackground.setColor(color);
            setCurrentColorViewDescription(color, NEW_COLOR_VIEW);
        }
    }

    private void setCurrentColorViewDescription(int color, int flag) {
        StringBuilder description = new StringBuilder();
        StringBuilder colorDescription = mColorSwatchView.getColorSwatchDescriptionAt(color);
        if (colorDescription != null) {
            description.append(", ")
                    .append((CharSequence) colorDescription);
        }

        switch (flag) {
            case CURRENT_COLOR_VIEW:
                description.insert(0,
                        mResources.getString(R.string.sesl_color_picker_current));
                mCurrentColorContainer.setContentDescription(description);
                break;
            case NEW_COLOR_VIEW:
                description.insert(0,
                        mResources.getString(R.string.sesl_color_picker_new));
                mPickedColorContainer.setContentDescription(description);
                break;
        }
    }

    public void saveSelectedColor() {
        final Integer pickedIntegerColor = mPickedColor.getColor();
        if (pickedIntegerColor != null) {
            mRecentColorInfo.saveSelectedColor(pickedIntegerColor);
        }
    }

    public SeslRecentColorInfo getRecentColorInfo() {
        return mRecentColorInfo;
    }

    public boolean isUserInputValid() {
        return mIsInputFromUser;
    }

    public void setOpacityBarEnabled(boolean enabled) {
        mIsOpacityBarEnabled = enabled;
        if (enabled) {
            mOpacitySeekBar.setVisibility(View.VISIBLE);
            mOpacitySeekBarContainer.setVisibility(View.VISIBLE);
        }
    }

    private static class PickedColor {
        private Integer mColor = null;
        private int mAlpha = 255;
        private float[] mHsv = new float[3];

        public void setColor(int color) {
            mColor = color;
            mAlpha = Color.alpha(mColor);
            Color.colorToHSV(mColor, mHsv);
        }

        public Integer getColor() {
            return mColor;
        }

        public void setHS(float hue, float saturation) {
            mHsv[0] = hue;
            mHsv[1] = saturation;
            mHsv[2] = 1.0f;
            mColor = Color.HSVToColor(mAlpha, mHsv);
        }

        public void setV(float value) {
            mHsv[2] = value;
            mColor = Color.HSVToColor(mAlpha, mHsv);
        }

        public void setAlpha(int alpha) {
            mAlpha = alpha;
            mColor = Color.HSVToColor(alpha, mHsv);
        }

        public float getV() {
            return mHsv[2];
        }

        public int getAlpha() {
            return mAlpha;
        }
    }
}
