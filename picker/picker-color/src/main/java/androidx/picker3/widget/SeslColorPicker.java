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

package androidx.picker3.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.picker.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslColorPicker extends LinearLayout
        implements View.OnClickListener {
    static int RECENT_COLOR_SLOT_COUNT = 6;

    private static final int CURRENT_COLOR_VIEW = 0;
    private static final int NEW_COLOR_VIEW = 1;

    private static final int RIPPLE_EFFECT_OPACITY = 61;

    private String beforeValue;
    private EditText mColorPickerBlueEditText;
    private EditText mColorPickerGreenEditText;
    private EditText mColorPickerHexEditText;
    private EditText mColorPickerOpacityEditText;
    private EditText mColorPickerRedEditText;
    private EditText mColorPickerSaturationEditText;
    private TextView mColorPickerTabSpectrumText;
    private TextView mColorPickerTabSwatchesText;
    private SeslColorSpectrumView mColorSpectrumView;
    private SeslColorSwatchView mColorSwatchView;
    private final Context mContext;
    private GradientDrawable mCurrentColorBackground;
    private ImageView mCurrentColorView;
    private SeslGradientColorSeekBar mGradientColorSeekBar;
    private LinearLayout mGradientSeekBarContainer;
    private OnColorChangedListener mOnColorChangedListener;
    private LinearLayout mOpacityLayout;
    private SeslOpacitySeekBar mOpacitySeekBar;
    private FrameLayout mOpacitySeekBarContainer;
    private PickedColor mPickedColor;
    private ImageView mPickedColorView;
    private final SeslRecentColorInfo mRecentColorInfo;
    private LinearLayout mRecentColorListLayout;
    private final ArrayList<Integer> mRecentColorValues;
    private final Resources mResources;
    private GradientDrawable mSelectedColorBackground;
    private FrameLayout mSpectrumViewContainer;
    private FrameLayout mSwatchViewContainer;
    private LinearLayout mTabLayoutContainer;
    ArrayList<EditText> editTexts = new ArrayList<>();
    private String[] mColorDescription = null;

    private boolean mFlagVar;
    private boolean mIsLightTheme;
    private boolean mShowOpacitySeekbar;
    private boolean mIsInputFromUser = false;
    private boolean mIsOpacityBarEnabled = false;
    boolean mIsSpectrumSelected = false;
    private boolean mfromEditText = false;
    private boolean mfromSaturationSeekbar = false;
    private boolean mfromSpectrumTouch = false;
    private boolean mfromRGB = false;
    private boolean mTextFromRGB = false;

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
                    updateHexAndRGBValues(color);

                    if (mGradientColorSeekBar != null) {
                        final int progress = mGradientColorSeekBar.getProgress();
                        mColorPickerSaturationEditText.setText(
                                "" + String.format(Locale.getDefault(), "%d", progress));
                        mColorPickerSaturationEditText.setSelection(String.valueOf(progress).length());
                    }

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

        LayoutInflater.from(context).inflate(R.layout.sesl_color_picker_oneui_3_layout, this);

        mRecentColorInfo = new SeslRecentColorInfo();
        mRecentColorValues = mRecentColorInfo.getRecentColorInfo();
        mTabLayoutContainer = findViewById(R.id.sesl_color_picker_tab_layout);
        mPickedColor = new PickedColor();

        initDialogPadding();
        initCurrentColorView();
        initColorSwatchView();
        initGradientColorSeekBar();
        initColorSpectrumView();
        initOpacitySeekBar(mShowOpacitySeekbar);
        initRecentColorLayout();
        updateCurrentColor();
        setInitialColors();
        initCurrentColorValuesLayout();
    }

    public void setOnlySpectrumMode() {
        mTabLayoutContainer.setVisibility(View.GONE);

        initColorSpectrumView();
        if (!mIsSpectrumSelected) {
            mIsSpectrumSelected = true;
        }

        mSwatchViewContainer.setVisibility(View.GONE);
        mSpectrumViewContainer.setVisibility(View.VISIBLE);

        mColorPickerHexEditText.setInputType(InputType.TYPE_NULL);
        mColorPickerRedEditText.setInputType(InputType.TYPE_NULL);
        mColorPickerBlueEditText.setInputType(InputType.TYPE_NULL);
        mColorPickerGreenEditText.setInputType(InputType.TYPE_NULL);
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
                            < (mResources.getDimensionPixelSize(R.dimen.sesl_color_picker_oneui_3_dialog_padding_left) * 2) + opacitySeekbarWidth) {
                        final int horizontalPadding = (int) ((screenWidth - opacitySeekbarWidth) / 2.0f);
                        final int paddingTop = mResources.getDimensionPixelSize(
                                R.dimen.sesl_color_picker_oneui_3_dialog_padding_top);
                        final int paddingBottom = mResources.getDimensionPixelSize(
                                R.dimen.sesl_color_picker_oneui_3_dialog_padding_bottom);

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

        mColorPickerTabSwatchesText = findViewById(R.id.sesl_color_picker_swatches_text_view);
        mColorPickerTabSpectrumText = findViewById(R.id.sesl_color_picker_spectrum_text_view);

        mColorPickerOpacityEditText = findViewById(R.id.sesl_color_seek_bar_opacity_value_edit_view);
        mColorPickerSaturationEditText = findViewById(R.id.sesl_color_seek_bar_saturation_value_edit_view);

        mColorPickerOpacityEditText.setPrivateImeOptions("disableDirectWriting=true;");
        mColorPickerSaturationEditText.setPrivateImeOptions("disableDirectWriting=true;");

        if (mIsLightTheme) {
            mColorPickerTabSwatchesText.setBackgroundResource(R.drawable.sesl_color_picker_tab_selector_bg);
        } else {
            mColorPickerTabSwatchesText.setBackgroundResource(R.drawable.sesl_color_picker_tab_selector_bg_dark);
        }

        mColorPickerTabSwatchesText.setTextAppearance(R.style.TabTextSelected);
        if (mIsLightTheme) {
            mColorPickerTabSwatchesText.setTextColor(getResources().getColor(R.color.sesl_dialog_body_text_color_light));
        } else {
            mColorPickerTabSwatchesText.setTextColor(getResources().getColor(R.color.sesl_dialog_body_text_color_dark));
        }
        if (mIsLightTheme) {
            mColorPickerTabSpectrumText.setTextColor(getResources().getColor(R.color.sesl_secondary_text_color_light));
        } else {
            mColorPickerTabSpectrumText.setTextColor(getResources().getColor(R.color.sesl_secondary_text_color_dark));
        }

        mColorPickerOpacityEditText.setTag(1);

        mFlagVar = true;

        mSelectedColorBackground = (GradientDrawable) mPickedColorView.getBackground();

        final Integer pickedIntegerColor = mPickedColor.getColor();
        if (pickedIntegerColor != null) {
            mSelectedColorBackground.setColor(pickedIntegerColor);
        }

        mCurrentColorBackground = (GradientDrawable) mCurrentColorView.getBackground();

        mColorPickerTabSwatchesText.setOnClickListener(this);
        mColorPickerTabSpectrumText.setOnClickListener(this);

        mColorPickerOpacityEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mOpacitySeekBar != null && s.toString().trim().length() > 0) {
                    final int progress = Integer.valueOf(s.toString());
                    if (progress <= 100) {
                        mColorPickerOpacityEditText.setTag(0);
                        mOpacitySeekBar.setProgress((progress * 255) / 100);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (Integer.parseInt(s.toString()) > 100) {
                        mColorPickerOpacityEditText.setText(
                                "" + String.format(Locale.getDefault(), "%d", 100));
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                mColorPickerOpacityEditText.setSelection(mColorPickerOpacityEditText.getText().length());
            }
        });

        mColorPickerOpacityEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!mColorPickerOpacityEditText.hasFocus()
                        && mColorPickerOpacityEditText.getText().toString().isEmpty()) {
                    mColorPickerOpacityEditText.setText(
                            "" + String.format(Locale.getDefault(), "%d", 0));
                }
            }
        });

        mColorPickerOpacityEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    mColorPickerHexEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.sesl_color_picker_swatches_text_view) {
            mColorPickerTabSwatchesText.setSelected(true);
            
            if (mIsLightTheme) {
                mColorPickerTabSwatchesText.setBackgroundResource(R.drawable.sesl_color_picker_tab_selector_bg);
            } else {
                mColorPickerTabSwatchesText.setBackgroundResource(R.drawable.sesl_color_picker_tab_selector_bg_dark);
            }
            
            mColorPickerTabSpectrumText.setSelected(false);
            mColorPickerTabSpectrumText.setBackgroundResource(0);
            
            if (mIsLightTheme) {
                mColorPickerTabSwatchesText.setTextColor(getResources().getColor(R.color.sesl_dialog_body_text_color_light));
            } else {
                mColorPickerTabSwatchesText.setTextColor(getResources().getColor(R.color.sesl_dialog_body_text_color_dark));
            }
            mColorPickerTabSwatchesText.setTextAppearance(R.style.TabTextSelected);
            
            if (mIsLightTheme) {
                mColorPickerTabSpectrumText.setTextColor(getResources().getColor(R.color.sesl_secondary_text_color_light));
            } else {
                mColorPickerTabSpectrumText.setTextColor(getResources().getColor(R.color.sesl_secondary_text_color_dark));
            }
            mColorPickerTabSpectrumText.setTypeface(Typeface.DEFAULT);
            
            mSwatchViewContainer.setVisibility(View.VISIBLE);
            mSpectrumViewContainer.setVisibility(View.GONE);

            if (mResources.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE
                    || isTablet(mContext)) {
                mGradientSeekBarContainer.setVisibility(View.GONE);
            } else {
                mGradientSeekBarContainer.setVisibility(View.INVISIBLE);
            }
        } else if (viewId == R.id.sesl_color_picker_spectrum_text_view) {
            mColorPickerTabSwatchesText.setSelected(false);
            mColorPickerTabSpectrumText.setSelected(true);

            if (mIsLightTheme) {
                mColorPickerTabSpectrumText.setBackgroundResource(R.drawable.sesl_color_picker_tab_selector_bg);
            } else {
                mColorPickerTabSpectrumText.setBackgroundResource(R.drawable.sesl_color_picker_tab_selector_bg_dark);
            }

            mColorPickerTabSwatchesText.setBackgroundResource(0);

            initColorSpectrumView();

            if (mIsLightTheme) {
                mColorPickerTabSpectrumText.setTextColor(getResources().getColor(R.color.sesl_dialog_body_text_color_light));
            } else {
                mColorPickerTabSpectrumText.setTextColor(getResources().getColor(R.color.sesl_dialog_body_text_color_dark));
            }

            mColorPickerTabSpectrumText.setTextAppearance(R.style.TabTextSelected);

            if (mIsLightTheme) {
                mColorPickerTabSwatchesText.setTextColor(getResources().getColor(R.color.sesl_secondary_text_color_light));
            } else {
                mColorPickerTabSwatchesText.setTextColor(getResources().getColor(R.color.sesl_secondary_text_color_dark));
            }
            mColorPickerTabSwatchesText.setTypeface(Typeface.DEFAULT);

            mSwatchViewContainer.setVisibility(View.GONE);
            mSpectrumViewContainer.setVisibility(View.VISIBLE);
            mGradientSeekBarContainer.setVisibility(View.VISIBLE);
        }
    }

    private void initColorSwatchView() {
        mColorSwatchView = findViewById(R.id.sesl_color_picker_color_swatch_view);
        mSwatchViewContainer = findViewById(R.id.sesl_color_picker_color_swatch_view_container);
        mColorSwatchView.setOnColorSwatchChangedListener(new SeslColorSwatchView.OnColorSwatchChangedListener() {
            @Override
            public void onColorSwatchChanged(int color) {
                mIsInputFromUser = true;
                try {
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getWindowToken(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mPickedColor.setColorWithAlpha(color, mOpacitySeekBar.getProgress());
                updateCurrentColor();
                updateHexAndRGBValues(color);
            }
        });
    }

    private void initColorSpectrumView() {
        mColorSpectrumView = findViewById(R.id.sesl_color_picker_color_spectrum_view);
        mSpectrumViewContainer = findViewById(R.id.sesl_color_picker_color_spectrum_view_container);

        mColorPickerSaturationEditText.setText(
                "" + String.format(Locale.getDefault(), "%d", mGradientColorSeekBar.getProgress()));

        mColorSpectrumView.setOnSpectrumColorChangedListener(new SeslColorSpectrumView.SpectrumColorChangedListener() {
            @Override
            public void onSpectrumColorChanged(float hue, float saturation) {
                mIsInputFromUser = true;

                try {
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getWindowToken(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mPickedColor.setHS(hue, saturation, mOpacitySeekBar.getProgress());
                updateCurrentColor();
                updateHexAndRGBValues(mPickedColor.getColor());
            }
        });

        mColorPickerSaturationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mTextFromRGB) {
                    try {
                        if (mGradientColorSeekBar != null && s.toString().trim().length() > 0) {
                            final int progress = Integer.valueOf(s.toString());
                            mfromEditText = true;
                            mFlagVar = false;
                            if (progress <= 100) {
                                mColorPickerSaturationEditText.setTag(0);
                                mGradientColorSeekBar.setProgress(progress);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mTextFromRGB) {
                    try {
                        if (Integer.parseInt(s.toString()) > 100) {
                            mColorPickerSaturationEditText.setText(
                                    "" + String.format(Locale.getDefault(), "%d", 100));
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    mColorPickerSaturationEditText.setSelection(mColorPickerSaturationEditText.getText().length());
                }
            }
        });

        mColorPickerSaturationEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!mColorPickerSaturationEditText.hasFocus()
                        && mColorPickerSaturationEditText.getText().toString().isEmpty()) {
                    mColorPickerSaturationEditText.setText(
                            "" + String.format(Locale.getDefault(), "%d", 0));
                }
            }
        });
    }

    private void initGradientColorSeekBar() {
        mGradientSeekBarContainer = findViewById(R.id.sesl_color_picker_saturation_layout);

        mGradientColorSeekBar = findViewById(R.id.sesl_color_picker_saturation_seekbar);
        mGradientColorSeekBar.init(mPickedColor.getColor());
        mGradientColorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mIsInputFromUser = true;
                    mfromSaturationSeekbar = true;
                }

                final float value = (float) seekBar.getProgress() / (float) seekBar.getMax();

                if (progress >= 0 && mFlagVar) {
                    mColorPickerSaturationEditText.setText(
                            "" + String.format(Locale.getDefault(), "%d", progress));
                    mColorPickerSaturationEditText.setSelection(String.valueOf(progress).length());
                }

                if (mfromRGB) {
                    mTextFromRGB = true;
                    mColorPickerSaturationEditText.setText(
                            "" + String.format(Locale.getDefault(), "%d", progress));
                    mColorPickerSaturationEditText.setSelection(String.valueOf(progress).length());
                    mTextFromRGB = false;
                }

                mPickedColor.setV(value);

                final int pickedIntegerColor = mPickedColor.getColor();
                if (mfromEditText) {
                    updateHexAndRGBValues(pickedIntegerColor);
                    mfromEditText = false;
                }
                if (mSelectedColorBackground != null) {
                    mSelectedColorBackground.setColor(pickedIntegerColor);
                }

                if (mOpacitySeekBar != null) {
                    mOpacitySeekBar.changeColorBase(pickedIntegerColor, mPickedColor.getAlpha());
                }

                if (mOnColorChangedListener != null) {
                    mOnColorChangedListener.onColorChanged(pickedIntegerColor);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mfromSaturationSeekbar = false;
            }
        });

        mGradientColorSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                mFlagVar = true;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mGradientColorSeekBar.setSelected(true);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mGradientColorSeekBar.setSelected(false);
                        return false;
                }
                return false;
            }
        });

        FrameLayout opacitySeekBarContainer = findViewById(R.id.sesl_color_picker_saturation_seekbar_container);
        opacitySeekBarContainer.setContentDescription(mResources.getString(R.string.sesl_color_picker_hue_and_saturation)
                + ", " + mResources.getString(R.string.sesl_color_picker_slider)
                + ", " + mResources.getString(R.string.sesl_color_picker_double_tap_to_select));
    }

    public void initOpacitySeekBar(boolean enabled) {
        mOpacitySeekBar = findViewById(R.id.sesl_color_picker_opacity_seekbar);
        mOpacitySeekBarContainer = findViewById(R.id.sesl_color_picker_opacity_seekbar_container);
        mOpacityLayout = findViewById(R.id.sesl_color_picker_opacity_layout);

        if (enabled) {
            mOpacityLayout.setVisibility(View.VISIBLE);
        } else {
            mOpacityLayout.setVisibility(View.GONE);
        }

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

                if (progress >= 0
                        && Integer.valueOf(mColorPickerOpacityEditText.getTag().toString()) == 1) {
                    mColorPickerOpacityEditText.setText(
                            "" + String.format(Locale.getDefault(), "%d", ((int) Math.ceil((progress * 100) / 255.0f))));
                }

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
                mColorPickerOpacityEditText.setTag(1);
                return event.getAction() == MotionEvent.ACTION_DOWN;
            }
        });

        mOpacitySeekBarContainer.setContentDescription(mResources.getString(R.string.sesl_color_picker_opacity)
                        + ", " + mResources.getString(R.string.sesl_color_picker_slider)
                        + ", " + mResources.getString(R.string.sesl_color_picker_double_tap_to_select));
    }

    private void initCurrentColorValuesLayout() {
        mColorPickerHexEditText = findViewById(R.id.sesl_color_hex_edit_text);
        mColorPickerRedEditText = findViewById(R.id.sesl_color_red_edit_text);
        mColorPickerBlueEditText = findViewById(R.id.sesl_color_blue_edit_text);
        mColorPickerGreenEditText = findViewById(R.id.sesl_color_green_edit_text);

        mColorPickerRedEditText.setPrivateImeOptions("disableDirectWriting=true;");
        mColorPickerBlueEditText.setPrivateImeOptions("disableDirectWriting=true;");
        mColorPickerGreenEditText.setPrivateImeOptions("disableDirectWriting=true;");

        editTexts.add(mColorPickerRedEditText);
        editTexts.add(mColorPickerGreenEditText);
        editTexts.add(mColorPickerBlueEditText);

        setTextWatcher();

        mColorPickerBlueEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mColorPickerBlueEditText.clearFocus();
                }
                return false;
            }
        });
    }

    private void setTextWatcher() {
        mColorPickerHexEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final int hexColorLength = s.toString().trim().length();
                if (hexColorLength > 0 && hexColorLength == 6) {
                    final int parsedColor = Color.parseColor("#" + s);
                    if (!mColorPickerRedEditText.getText().toString().trim()
                            .equalsIgnoreCase("" + Color.red(parsedColor))) {
                        mColorPickerRedEditText.setText("" + Color.red(parsedColor));
                    }

                    if (!mColorPickerGreenEditText.getText().toString().trim()
                            .equalsIgnoreCase("" + Color.green(parsedColor))) {
                        mColorPickerGreenEditText.setText("" + Color.green(parsedColor));
                    }

                    if (!mColorPickerBlueEditText.getText().toString().trim()
                            .equalsIgnoreCase("" + Color.blue(parsedColor))) {
                        mColorPickerBlueEditText.setText("" + Color.blue(parsedColor));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                mIsInputFromUser = true;
            }
        });

        beforeValue = "";

        Iterator<EditText> it = editTexts.iterator();
        while (it.hasNext()) {
            final EditText editText = it.next();
            editText.addTextChangedListener(new TextWatcher() {
                @Override // android.text.TextWatcher
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    beforeValue = s.toString().trim();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    final String value = s.toString();
                    if (!value.equalsIgnoreCase(beforeValue) && value.trim().length() > 0) {
                        updateHexData();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        if (Integer.parseInt(s.toString()) > 255) {
                            if (editText == editTexts.get(0)) {
                                mColorPickerRedEditText.setText("255");
                            }
                            if (editText == editTexts.get(1)) {
                                mColorPickerGreenEditText.setText("255");
                            }
                            if (editText == editTexts.get(2)) {
                                mColorPickerBlueEditText.setText("255");
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();

                        if (editText == editTexts.get(0)) {
                            mColorPickerRedEditText.setText("0");
                        }
                        if (editText == editTexts.get(1)) {
                            mColorPickerGreenEditText.setText("0");
                        }
                        if (editText == editTexts.get(2)) {
                            mColorPickerBlueEditText.setText("0");
                        }
                    }

                    mIsInputFromUser = true;
                    mfromRGB = true;

                    mColorPickerRedEditText.setSelection(mColorPickerRedEditText.getText().length());
                    mColorPickerGreenEditText.setSelection(mColorPickerGreenEditText.getText().length());
                    mColorPickerBlueEditText.setSelection(mColorPickerBlueEditText.getText().length());
                }
            });
        }
    }

    private void updateHexData() {
        final int red = Integer.valueOf(mColorPickerRedEditText.getText().toString().trim().length() > 0 ?
                mColorPickerRedEditText.getText().toString().trim() : "0");
        final int green = Integer.valueOf(mColorPickerGreenEditText.getText().toString().trim().length() > 0 ?
                mColorPickerGreenEditText.getText().toString().trim() : "0");
        final int blue = Integer.valueOf(mColorPickerBlueEditText.getText().toString().trim().length() > 0 ?
                mColorPickerBlueEditText.getText().toString().trim() : "0");

        final int color = ((red & 255) << 16) | ((mOpacitySeekBar.getProgress() & 255) << 24)
                | ((green & 255) << 8) | (blue & 255);

        final String colorStr = String.format("%08x", color & (-1));
        mColorPickerHexEditText.setText("" + colorStr.substring(2, colorStr.length()).toUpperCase());
        mColorPickerHexEditText.setSelection(mColorPickerHexEditText.getText().length());

        if (!mfromSaturationSeekbar && !mfromSpectrumTouch) {
            mapColorOnColorWheel(color);
        }

        if (mOnColorChangedListener != null) {
            mOnColorChangedListener.onColorChanged(color);
        }
    }

    private void updateHexAndRGBValues(int color) {
        if (color != 0) {
            final String format = String.format("%08x", color & -1);
            final String colorStr = format.substring(2, format.length());
            mColorPickerHexEditText.setText("" + colorStr.toUpperCase());
            mColorPickerHexEditText.setSelection(mColorPickerHexEditText.getText().length());

            final int parsedColor = Color.parseColor("#" + colorStr);
            mColorPickerRedEditText.setText("" + Color.red(parsedColor));
            mColorPickerBlueEditText.setText("" + Color.blue(parsedColor));
            mColorPickerGreenEditText.setText("" + Color.green(parsedColor));
        }
    }

    private void initRecentColorLayout() {
        mRecentColorListLayout = findViewById(R.id.sesl_color_picker_used_color_item_list_layout);

        mColorDescription = new String[] {
                mResources.getString(R.string.sesl_color_picker_color_one),
                mResources.getString(R.string.sesl_color_picker_color_two),
                mResources.getString(R.string.sesl_color_picker_color_three),
                mResources.getString(R.string.sesl_color_picker_color_four),
                mResources.getString(R.string.sesl_color_picker_color_five),
                mResources.getString(R.string.sesl_color_picker_color_six),
                mResources.getString(R.string.sesl_color_picker_color_seven)
        };

        final int emptyColor = ContextCompat.getColor(mContext, mIsLightTheme ?
                R.color.sesl_color_picker_used_color_item_empty_slot_color_light
                : R.color.sesl_color_picker_used_color_item_empty_slot_color_dark);

        if (mResources.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE
                || isTablet(mContext)) {
            RECENT_COLOR_SLOT_COUNT = 6;
        } else {
            RECENT_COLOR_SLOT_COUNT = 7;
        }

        for (int i = 0; i < RECENT_COLOR_SLOT_COUNT; i++) {
            View recentColorSlot = mRecentColorListLayout.getChildAt(i);
            setImageColor(recentColorSlot, emptyColor);
            recentColorSlot.setFocusable(false);
            recentColorSlot.setClickable(false);
        }
    }

    public void updateRecentColorLayout() {
        final int size = mRecentColorValues != null
                ? mRecentColorValues.size() : 0;

        final String defaultDescription = ", "
                + mResources.getString(R.string.sesl_color_picker_option);

        if (mResources.getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            RECENT_COLOR_SLOT_COUNT = 7;
        } else {
            RECENT_COLOR_SLOT_COUNT = 6;
        }

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
            updateHexAndRGBValues(mCurrentColorBackground.getColor().getDefaultColor());
        } else if (size != 0) {
            final int firstColor = mRecentColorValues.get(0);
            mCurrentColorBackground.setColor(firstColor);
            setCurrentColorViewDescription(firstColor, CURRENT_COLOR_VIEW);
            mSelectedColorBackground.setColor(firstColor);
            mapColorOnColorWheel(firstColor);
            updateHexAndRGBValues(mCurrentColorBackground.getColor().getDefaultColor());
        }

        if (mRecentColorInfo.getNewColor() != null) {
            final int newColor = mRecentColorInfo.getNewColor();
            mSelectedColorBackground.setColor(newColor);
            mapColorOnColorWheel(newColor);
            updateHexAndRGBValues(mSelectedColorBackground.getColor().getDefaultColor());
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
                mOpacitySeekBar.changeColorBase(pickedIntegerColor, mPickedColor.getAlpha());

                final int progress = mOpacitySeekBar.getProgress();
                mColorPickerOpacityEditText.setText(
                        "" + String.format(Locale.getDefault(), "%d", progress));
                mColorPickerOpacityEditText.setSelection(String.valueOf(progress).length());
            }

            if (mSelectedColorBackground != null) {
                mSelectedColorBackground.setColor(pickedIntegerColor);
                setCurrentColorViewDescription(pickedIntegerColor, NEW_COLOR_VIEW);
            }

            if (mOnColorChangedListener != null) {
                mOnColorChangedListener.onColorChanged(pickedIntegerColor);
            }

            if (mColorSpectrumView != null) {
                mColorSpectrumView.updateCursorColor(pickedIntegerColor);
                mColorSpectrumView.setColor(pickedIntegerColor);
            }

            if (mGradientColorSeekBar != null) {
                final int progress = mGradientColorSeekBar.getProgress();
                mGradientColorSeekBar.changeColorBase(pickedIntegerColor);
                mfromSpectrumTouch = true;
                mColorPickerSaturationEditText.setText(
                        "" + String.format(Locale.getDefault(), "%d", progress));
                mColorPickerSaturationEditText.setSelection(String.valueOf(progress).length());
                mfromSpectrumTouch = false;
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

        if (mColorSpectrumView != null) {
            mColorSpectrumView.setColor(color);
        }

        if (mGradientColorSeekBar != null) {
            mGradientColorSeekBar.restoreColor(color);
        }

        if (mOpacitySeekBar != null) {
            mOpacitySeekBar.restoreColor(color);
        }

        if (mSelectedColorBackground != null) {
            mSelectedColorBackground.setColor(color);
            setCurrentColorViewDescription(color, NEW_COLOR_VIEW);
        }

        if (mColorSpectrumView != null) {
            final float value = mPickedColor.getV();
            final int alpha = mPickedColor.getAlpha();
            mPickedColor.setV(1.0f);
            mPickedColor.setAlpha(255);
            mColorSpectrumView.updateCursorColor(mPickedColor.getColor());
            mPickedColor.setV(value);
            mPickedColor.setAlpha(alpha);
        }


        if (mOpacitySeekBar != null) {
            final int progress = (int) Math.ceil((mOpacitySeekBar.getProgress() * 100) / 255.0f);
            mColorPickerOpacityEditText.setText(
                    "" + String.format(Locale.getDefault(), "%d", progress));
            mColorPickerOpacityEditText.setSelection(String.valueOf(progress).length());
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
                break;
            case NEW_COLOR_VIEW:
                description.insert(0,
                        mResources.getString(R.string.sesl_color_picker_new));
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

        public void setColorWithAlpha(int color, int alpha) {
            mColor = color;
            mAlpha = (int) Math.ceil((alpha * 100) / 255.0f);
            Color.colorToHSV(mColor, mHsv);
        }

        public Integer getColor() {
            return mColor;
        }

        public void setHS(float hue, float saturation, int alpha) {
            mHsv[0] = hue;
            mHsv[1] = saturation;
            mHsv[2] = 1.0f;
            mColor = Color.HSVToColor(mAlpha, mHsv);
            mAlpha = (int) Math.ceil((alpha * 100) / 255.0f);
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

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & 15) >= 3;
    }
}
