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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.OverScroller;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.util.SeslMisc;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.picker.R;
import androidx.picker.util.SeslAnimationListener;
import androidx.picker.widget.SeslSpinningDatePickerSpinner.OnScrollListener;
import androidx.reflect.content.res.SeslCompatibilityInfoReflector;
import androidx.reflect.content.res.SeslConfigurationReflector;
import androidx.reflect.graphics.SeslPaintReflector;
import androidx.reflect.lunarcalendar.SeslFeatureReflector;
import androidx.reflect.lunarcalendar.SeslSolarLunarConverterReflector;
import androidx.reflect.media.SeslAudioManagerReflector;
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector;
import androidx.reflect.view.SeslViewReflector;

import java.io.File;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dalvik.system.PathClassLoader;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

class SeslSpinningDatePickerSpinnerDelegate extends SeslSpinningDatePickerSpinner.AbsDatePickerDelegate {
    private static final int DEFAULT_CHANGE_VALUE_BY = 1;

    private static final int DECREASE_BUTTON = 1;
    private static final int INCREASE_BUTTON = 3;

    private static final int INPUT = 2;

    private static final int PICKER_VIBRATE_INDEX = 32;

    private static final int DEFAULT_START_YEAR = 1902;
    private static final int DEFAULT_END_YEAR = 2100;

    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 300;
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 4;
    private static final int SELECTOR_MIDDLE_ITEM_INDEX = 2;
    private static final int SELECTOR_WHEEL_ITEM_COUNT = 5;

    private static final int SIZE_UNSPECIFIED = -1;
    private static final int TEXT_GAP_COUNT = 3;
    private static final int HCF_UNFOCUSED_TEXT_SIZE_DIFF = 2;

    private static final int SNAP_SCROLL_DURATION = 500;
    private static final int START_ANIMATION_SCROLL_DURATION = 857;
    private static final int START_ANIMATION_SCROLL_DURATION_2016B = 557;

    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;
    private static final int LONG_PRESSED_SCROLL_COUNT = 10;

    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT = 2;

    private AccessibilityManager mAccessibilityManager;
    private AccessibilityNodeProviderImpl mAccessibilityNodeProvider;
    private final Scroller mAdjustScroller;
    private SeslAnimationListener mAnimationListener;
    private AudioManager mAudioManager;
    private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;
    private ValueAnimator mColorInAnimator;
    private ValueAnimator mColorOutAnimator;
    private final Scroller mCustomScroller;
    private final Typeface mDefaultTypeface;
    private ValueAnimator mFadeInAnimator;
    private ValueAnimator mFadeOutAnimator;
    private Scroller mFlingScroller;
    private SeslSpinningDatePickerSpinner.Formatter mFormatter;
    private OverScroller mGravityScroller;
    private HapticPreDrawListener mHapticPreDrawListener;
    private Typeface mHcfFocusedTypefaceBold;
    private FloatValueHolder mHolder;
    private final EditText mInputText;
    private SeslSpinningDatePickerSpinner.OnScrollListener mOnScrollListener;
    private SeslSpinningDatePickerSpinner.OnSpinnerDateClickListener mOnSpinnerDateClickListener;
    private SeslSpinningDatePickerSpinner.OnValueChangeListener mOnValueChangeListener;
    private final Typeface mLegacyTypeface;
    private final Scroller mLinearScroller;
    private String[] mLongMonths;
    private Calendar mMaxValue;
    private Calendar mMinValue;
    private PathClassLoader mPathClassLoader = null;
    private String mPickerContentDescription;
    private Typeface mPickerSubTypeface;
    private Typeface mPickerTypeface;
    private final PressedStateHelper mPressedStateHelper;
    private final HashMap<Calendar, String> mSelectorIndexToStringCache = new HashMap<>();
    private final Calendar[] mSelectorIndices = new Calendar[SELECTOR_WHEEL_ITEM_COUNT];
    private Paint mSelectorWheelPaint;
    private String[] mShortMonths;
    private Object mSolarLunarConverter = null;
    private SpringAnimation mSpringAnimation;
    private Calendar mValue;
    private VelocityTracker mVelocityTracker;
    private final Drawable mVirtualButtonFocusedDrawable;

    private int mBottomSelectionDividerBottom;
    private int mChangeValueBy = DEFAULT_CHANGE_VALUE_BY;
    private int mCurrentScrollOffset;
    private final int mHcfUnfocusedTextSizeDiff;
    private int mInitialScrollOffset = Integer.MIN_VALUE;
    private int mLastFocusedChildVirtualViewId;
    private int mLastHoveredChildVirtualViewId;
    private int mLongPressCount;
    private final int mMaxHeight;
    private int mMaxWidth;
    private int mMaximumFlingVelocity;
    private final int mMinHeight;
    private final int mMinWidth;
    private int mMinimumFlingVelocity;
    private int mModifiedTxtHeight;
    private int mPickerSoundIndex;
    private int mPickerVibrateIndex;
    private int mPreviousScrollerY;
    private final int mSelectionDividerHeight;
    private int mSelectorElementHeight;
    private int mSelectorTextGapHeight;
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private int mTextColor;
    private final int mTextColorIdle;
    private final int mTextColorScrolling;
    private int mTextSize;
    private int mTopSelectionDividerTop;
    private int mTouchSlop;
    private int mValueChangeOffset;

    private float mActivatedAlpha = 0.4f;
    private float mAlpha = 0.1f;
    private final float mHeightRatio;
    private float mIdleAlpha = 0.1f;
    private float mInitialAlpha = 1.0f;
    private float mLastDownEventY;
    private float mLastDownOrMoveEventY;
    private float mPreviousSpringY;

    private final boolean mComputeMaxWidth;
    private boolean mCustomTypefaceSet = false;
    private boolean mDecrementVirtualButtonPressed;
    private boolean mIgnoreMoveEvents;
    private boolean mIncrementVirtualButtonPressed;
    private boolean mIsHcfEnabled;
    private boolean mIsLeapMonth;
    private boolean mIsLongClicked = false;
    private boolean mIsLongPressed = false;
    private boolean mIsLunar;
    private boolean mIsStartingAnimation = false;
    private boolean mIsValueChanged = false;
    private boolean mLongPressed_FIRST_SCROLL;
    private boolean mLongPressed_SECOND_SCROLL;
    private boolean mLongPressed_THIRD_SCROLL;
    private boolean mPerformClickOnTap;
    private boolean mReservedStartAnimation = false;
    private boolean mSkipNumbers;
    private boolean mSpringFlingRunning;
    private boolean mWrapSelectorWheel;
    private boolean mWrapSelectorWheelPreferred = true;

    private long mLastDownEventTime;
    private long mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;

    private final PathInterpolator ALPHA_PATH_INTERPOLATOR = new PathInterpolator(
            0.17f, 0.17f, 0.83f, 0.83f);
    private final PathInterpolator SIZE_PATH_INTERPOLATOR = new PathInterpolator(
            0.5f, 0.0f, 0.4f, 1.0f);

    private ValueAnimator.AnimatorUpdateListener mColorUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mTextColor = (int) animation.getAnimatedValue();
            mDelegator.invalidate();
        }
    };

    private ValueAnimator.AnimatorUpdateListener mUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mAlpha = (float) animation.getAnimatedValue();
            mDelegator.invalidate();
        }
    };

    private DynamicAnimation.OnAnimationUpdateListener mSpringAnimationUpdateListener
            = new DynamicAnimation.OnAnimationUpdateListener() {
        @Override
        public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
            final float y = value - mPreviousSpringY;
            if (!mSpringFlingRunning && Math.round(y) == 0) {
                animation.cancel();
                ensureScrollWheelAdjusted();
            } else {
                if (Math.round(y) == 0) {
                    mSpringFlingRunning = false;
                }
                scrollBy(0, Math.round(y));
                mPreviousSpringY = value;
                mDelegator.invalidate();
            }
        }
    };

    private DynamicAnimation.OnAnimationEndListener mSpringAnimationEndListener
            = new DynamicAnimation.OnAnimationEndListener() {
        @Override
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value,
                                   float velocity) {
            mSpringFlingRunning = false;
            mGravityScroller.forceFinished(true);
            startFadeAnimation(true);
        }
    };

    public SeslSpinningDatePickerSpinnerDelegate(SeslSpinningDatePickerSpinner spinningDatePickerSpinner,
                                                 Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(spinningDatePickerSpinner, context);

        final Resources resources = mContext.getResources();

        final int defaultHeight = resources
                .getDimensionPixelSize(R.dimen.sesl_number_picker_spinner_height);
        final int defaultWidth = resources
                .getDimensionPixelSize(R.dimen.sesl_number_picker_spinner_width);
        final float defaultEditTextHeight = resources
                .getDimension(R.dimen.sesl_number_picker_spinner_edit_text_height);

        mHeightRatio = defaultEditTextHeight / defaultHeight;

        TypedArray a = context
                .obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes);
        mMinHeight = a.getDimensionPixelSize(R.styleable.NumberPicker_internalMinHeight,
                SIZE_UNSPECIFIED);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.NumberPicker_internalMaxHeight,
                defaultHeight);
        mMinWidth = a.getDimensionPixelSize(R.styleable.NumberPicker_internalMinWidth,
                defaultWidth);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.NumberPicker_internalMaxWidth,
                SIZE_UNSPECIFIED);
        a.recycle();

        mValue = getCalendarForLocale(mValue, Locale.getDefault());
        mMinValue = getCalendarForLocale(mMinValue, Locale.getDefault());
        mMaxValue = getCalendarForLocale(mMaxValue, Locale.getDefault());

        TypedArray a2 = context.obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyleAttr, defStyleRes);
        mMinValue.set(a2.getInt(R.styleable.DatePicker_android_startYear, DEFAULT_START_YEAR),
                Calendar.JANUARY, 1);
        mMaxValue.set(a2.getInt(R.styleable.DatePicker_android_endYear, DEFAULT_END_YEAR),
                Calendar.DECEMBER, 31);

        if (mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED
                && mMinHeight > mMaxHeight) {
            throw new IllegalArgumentException("minHeight > maxHeight");
        }
        if (mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED
                && mMinWidth > mMaxWidth) {
            throw new IllegalArgumentException("minWidth > maxWidth");
        }

        mSelectionDividerHeight = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT, resources.getDisplayMetrics());
        mComputeMaxWidth = mMaxWidth == SIZE_UNSPECIFIED;

        TypedValue typedValue = new TypedValue();
        final int colorPrimaryDark;
        context.getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        if (typedValue.resourceId != 0) {
            colorPrimaryDark = ResourcesCompat.getColor(resources, typedValue.resourceId, null);
        } else {
            colorPrimaryDark = typedValue.data;
        }
        mVirtualButtonFocusedDrawable = new ColorDrawable((colorPrimaryDark & 0xffffff) | 0x33000000);

        if (!SeslMisc.isLightTheme(mContext)) {
            mIdleAlpha = 0.2f;
            mAlpha = 0.2f;
        }

        mPressedStateHelper = new PressedStateHelper();
        mDelegator.setWillNotDraw(false);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sesl_spinning_date_picker_spinner, mDelegator, true);

        mInputText = mDelegator.findViewById(R.id.datepicker_input);
        mInputText.setIncludeFontPadding(false);

        mDefaultTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
        mLegacyTypeface = Typeface.create("sec-roboto-condensed-light", Typeface.BOLD);
        mPickerTypeface = Typeface.create("sec-roboto-light", Typeface.BOLD);
        if (mDefaultTypeface.equals(mPickerTypeface)) {
            if (!mLegacyTypeface.equals(mPickerTypeface)) {
                mPickerTypeface = mLegacyTypeface;
            } else {
                mPickerTypeface = Typeface.create("sans-serif-thin", Typeface.BOLD);
            }
        }
        mPickerSubTypeface = Typeface.create(mPickerTypeface, Typeface.NORMAL);

        final boolean isDexMode = SeslConfigurationReflector
                .isDexEnabled(resources.getConfiguration());
        if (!isDexMode) {
            final String themeTypeFace = Settings.System.getString(mContext.getContentResolver(),
                    "theme_font_clock");
            if (themeTypeFace != null && !themeTypeFace.isEmpty()) {
                mPickerTypeface = getFontTypeface(themeTypeFace);
                mPickerSubTypeface = Typeface.create(mPickerTypeface, Typeface.NORMAL);
            }
        } else {
            mIdleAlpha = 0.2f;
            mAlpha = 0.2f;
        }

        if (isCharacterNumberLanguage()) {
            mPickerTypeface = mDefaultTypeface;
            mPickerSubTypeface = Typeface.create(mDefaultTypeface, Typeface.NORMAL);
        }

        mIsHcfEnabled = isHighContrastFontEnabled();
        mHcfFocusedTypefaceBold = Typeface.create(mPickerTypeface, Typeface.BOLD);
        mHcfUnfocusedTextSizeDiff = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        HCF_UNFOCUSED_TEXT_SIZE_DIFF, mContext.getResources().getDisplayMetrics());
        setInputTextTypeface();

        final ColorStateList colors = mInputText.getTextColors();
        final int[] enabledStateSet = mDelegator.getEnableStateSet();

        if (Build.VERSION.SDK_INT > 29) {
            mTextColorIdle = colors.getColorForState(enabledStateSet, Color.WHITE);
        } else {
            mTextColorIdle = ResourcesCompat
                    .getColor(resources, R.color.sesl_number_picker_text_color_scroll, context.getTheme());
        }
        mTextColorScrolling = ResourcesCompat
                .getColor(resources, R.color.sesl_number_picker_text_color_scroll, context.getTheme());
        mTextColor = mTextColorIdle;

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity() / 4;
        mTextSize = (int) mInputText.getTextSize();

        mSelectorWheelPaint = new Paint();
        mSelectorWheelPaint.setAntiAlias(true);
        mSelectorWheelPaint.setTextAlign(Paint.Align.CENTER);
        mSelectorWheelPaint.setTextSize(mTextSize);
        mSelectorWheelPaint.setTypeface(mPickerTypeface);
        mSelectorWheelPaint.setColor(mTextColor);
        mInitialAlpha = mSelectorWheelPaint.getAlpha() / 255.0f;

        mCustomScroller = new Scroller(mContext, SIZE_PATH_INTERPOLATOR, true);
        mLinearScroller = new Scroller(mContext, null, true);
        mFlingScroller = new Scroller(mContext, null, true);
        mAdjustScroller = new Scroller(mContext,
                new PathInterpolator(0.4f, 0.0f, 0.3f, 1.0f));
        mGravityScroller = new OverScroller(mContext, new DecelerateInterpolator());

        mHolder = new FloatValueHolder();
        mSpringAnimation = new SpringAnimation(mHolder);
        mSpringAnimation.setSpring(new SpringForce());
        mSpringAnimation.setMinimumVisibleChange(1.0f);
        mSpringAnimation.addUpdateListener(mSpringAnimationUpdateListener);
        mSpringAnimation.addEndListener(mSpringAnimationEndListener);
        mSpringAnimation.getSpring().setStiffness(7.0f);
        mSpringAnimation.getSpring().setDampingRatio(0.99f);

        setFormatter(SeslSpinningDatePickerSpinner.getDateFormatter());
        mDelegator.setVerticalScrollBarEnabled(false);
        if (mDelegator.getImportantForAccessibility()
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            mDelegator.setImportantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHapticPreDrawListener = new HapticPreDrawListener();
        mPickerVibrateIndex = SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(PICKER_VIBRATE_INDEX);
        mPickerSoundIndex = SeslAudioManagerReflector.getField_SOUND_TIME_PICKER_SCROLL();

        mDelegator.setFocusableInTouchMode(false);
        mDelegator.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 26) {
            mDelegator.setDefaultFocusHighlightEnabled(false);
        }

        mPickerContentDescription = "";

        SeslViewReflector.semSetDirectPenInputEnabled(mInputText, false);

        mAccessibilityManager
                = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);

        mFadeOutAnimator = ValueAnimator.ofFloat(mActivatedAlpha, mIdleAlpha);
        mFadeOutAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mFadeOutAnimator.setDuration(200);
        mFadeOutAnimator.setStartDelay(100);
        mFadeOutAnimator.addUpdateListener(mUpdateListener);
        mFadeInAnimator = ValueAnimator.ofFloat(mIdleAlpha, mActivatedAlpha);
        mFadeInAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mFadeInAnimator.setDuration(200);
        mFadeInAnimator.addUpdateListener(mUpdateListener);

        mColorInAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                mTextColorIdle, mTextColorScrolling);
        mColorInAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mColorInAnimator.setDuration(200);
        mColorInAnimator.addUpdateListener(mColorUpdateListener);
        mColorOutAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                mTextColorScrolling, mTextColorIdle);
        mColorOutAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mColorOutAnimator.setDuration(200);
        mColorOutAnimator.setStartDelay(100);
        mColorOutAnimator.addUpdateListener(mColorUpdateListener);

        mShortMonths = new DateFormatSymbols().getShortMonths();
        mLongMonths = new DateFormatSymbols().getMonths();
    }

    @Override
    public void setPickerContentDescription(String name) {
        mPickerContentDescription = name;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int msrdWdth = mDelegator.getMeasuredWidth();
        final int msrdHght = mDelegator.getMeasuredHeight();
        final int inptTxtMsrdWdth = mInputText.getMeasuredWidth();
        final int inptTxtMsrdHght = Math.max(mInputText.getMeasuredHeight(),
                (int) Math.floor(msrdHght * mHeightRatio));
        mModifiedTxtHeight = inptTxtMsrdHght;

        final int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
        final int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
        final int inptTxtRight = inptTxtLeft + inptTxtMsrdWdth;
        final int inptTxtBottom = inptTxtTop + inptTxtMsrdHght;
        mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom);

        if (changed) {
            initializeSelectorWheel();
            if (mModifiedTxtHeight > mSelectorElementHeight) {
                mTopSelectionDividerTop = mValueChangeOffset;
                mBottomSelectionDividerBottom = mValueChangeOffset * 2;
            } else {
                mTopSelectionDividerTop = inptTxtTop;
                mBottomSelectionDividerBottom = inptTxtBottom;
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth);
        final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
        mDelegator.superOnMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
        final int widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth,
                mDelegator.getMeasuredWidth(), widthMeasureSpec);
        final int heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight,
                mDelegator.getMeasuredHeight(), heightMeasureSpec);
        mDelegator.setMeasuredDimensionWrapper(widthSize, heightSize);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        return false;
    }

    private boolean moveToFinalScrollerPosition(Scroller scroller) {
        scroller.forceFinished(true);
        int amountToScroll = scroller.getFinalY() - scroller.getCurrY();
        if (mSelectorElementHeight == 0) {
            return false;
        }
        int futureScrollOffset = mCurrentScrollOffset + amountToScroll;
        int overshootAdjustment = mInitialScrollOffset - futureScrollOffset;
        if (overshootAdjustment != 0) {
            overshootAdjustment %= mSelectorElementHeight;
            if (Math.abs(overshootAdjustment) > mSelectorElementHeight / 2) {
                if (overshootAdjustment > 0) {
                    overshootAdjustment -= mSelectorElementHeight;
                } else {
                    overshootAdjustment += mSelectorElementHeight;
                }
            }
            amountToScroll += overshootAdjustment;
            scrollBy(0, amountToScroll);
            return true;
        }
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!mIsStartingAnimation) {
            if (!mFlingScroller.isFinished()) {
                mFlingScroller.forceFinished(true);
            }
            if (!mAdjustScroller.isFinished()) {
                mAdjustScroller.forceFinished(true);
            }
            if (!mGravityScroller.isFinished()) {
                mGravityScroller.forceFinished(true);
            }
            if (mSpringAnimation.isRunning()) {
                mSpringAnimation.cancel();
                mSpringFlingRunning = false;
            }
            ensureScrollWheelAdjusted();
        }

        mIsHcfEnabled = isHighContrastFontEnabled();
        mSelectorWheelPaint.setTextSize(mTextSize);
        mSelectorWheelPaint.setTypeface(mPickerTypeface);
        setInputTextTypeface();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mDelegator.isEnabled() || mIsStartingAnimation
                || event.getActionMasked() != MotionEvent.ACTION_DOWN ) {
            return false;
        }

        removeAllCallbacks();
        mLastDownOrMoveEventY = mLastDownEventY = event.getY();
        mLastDownEventTime = event.getEventTime();
        mIgnoreMoveEvents = false;
        mPerformClickOnTap = false;
        mIsValueChanged = false;
        if (mLastDownEventY < mTopSelectionDividerTop) {
            startFadeAnimation(false);
            if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                mPressedStateHelper.buttonPressDelayed(PressedStateHelper.BUTTON_DECREMENT);
            }
        } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
            startFadeAnimation(false);
            if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                mPressedStateHelper.buttonPressDelayed(PressedStateHelper.BUTTON_INCREMENT);
            }
        }
        mDelegator.getParent().requestDisallowInterceptTouchEvent(true);
        if (!mFlingScroller.isFinished()) {
            mFlingScroller.forceFinished(true);
            mAdjustScroller.forceFinished(true);
            if (mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                mFlingScroller.abortAnimation();
                mAdjustScroller.abortAnimation();
            }
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        } else if (mSpringAnimation.isRunning()) {
            mGravityScroller.forceFinished(true);
            mAdjustScroller.forceFinished(true);
            mSpringAnimation.cancel();
            mSpringFlingRunning = false;
            if (mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                mGravityScroller.abortAnimation();
                mAdjustScroller.abortAnimation();
            }
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        } else if (!mAdjustScroller.isFinished()) {
            mFlingScroller.forceFinished(true);
            mAdjustScroller.forceFinished(true);
        } else if (mLastDownEventY < mTopSelectionDividerTop) {
            postChangeCurrentByOneFromLongPress(false, ViewConfiguration.getLongPressTimeout());
        } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
            postChangeCurrentByOneFromLongPress(true, ViewConfiguration.getLongPressTimeout());
        } else {
            mPerformClickOnTap = true;
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mDelegator.isEnabled() || mIsStartingAnimation) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_UP: {
                removeChangeCurrentByOneFromLongPress();
                mPressedStateHelper.cancel();
                VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                int eventY = (int) event.getY();
                int deltaMoveY = (int) Math.abs(eventY - mLastDownEventY);
                if (Math.abs(initialVelocity) <= mMinimumFlingVelocity) {
                    final long eventTime = event.getEventTime() - mLastDownEventTime;
                    if (deltaMoveY > mTouchSlop
                            || eventTime >= ViewConfiguration.getLongPressTimeout()) {
                        if (mIsLongClicked) {
                            mIsLongClicked = false;
                        }
                        ensureScrollWheelAdjusted(deltaMoveY);
                        startFadeAnimation(true);
                    } else if (mPerformClickOnTap) {
                        mPerformClickOnTap = false;
                        performClick();
                    } else {
                        if (eventY > mBottomSelectionDividerBottom) {
                            changeValueByOne(true);
                            mPressedStateHelper.buttonTapped(PressedStateHelper.BUTTON_INCREMENT);
                        } else if (eventY < mTopSelectionDividerTop) {
                            changeValueByOne(false);
                            mPressedStateHelper.buttonTapped(PressedStateHelper.BUTTON_DECREMENT);
                        } else {
                            ensureScrollWheelAdjusted(deltaMoveY);
                        }
                        startFadeAnimation(true);
                    }
                    mIsValueChanged = false;
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                } else if (deltaMoveY > mTouchSlop || !mPerformClickOnTap) {
                    fling(initialVelocity);
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                } else {
                    mPerformClickOnTap = false;
                    performClick();
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            } break;
            case MotionEvent.ACTION_MOVE: {
                if (mIgnoreMoveEvents) {
                    break;
                }
                float currentMoveY = event.getY();
                if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    int deltaDownY = (int) Math.abs(currentMoveY - mLastDownEventY);
                    if (deltaDownY > mTouchSlop) {
                        removeAllCallbacks();
                        startFadeAnimation(false);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    }
                } else {
                    int deltaMoveY = (int) ((currentMoveY - mLastDownOrMoveEventY));
                    scrollBy(0, deltaMoveY);
                    mDelegator.invalidate();
                }
                mLastDownOrMoveEventY = currentMoveY;
            } break;
            case MotionEvent.ACTION_CANCEL: {
                ensureScrollWheelAdjusted();
                startFadeAnimation(true);
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            } break;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (!mDelegator.isEnabled() || mIsStartingAnimation) {
            return false;
        }

        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0
                && event.getAction() == MotionEvent.ACTION_SCROLL) {
            float axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (axisValue != 0.0f) {
                startFadeAnimation(false);
                changeValueByOne(axisValue < 0.0f);
                startFadeAnimation(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!mCustomTypefaceSet) {
            if (isCharacterNumberLanguage()) {
                mInputText.setIncludeFontPadding(true);
                mPickerTypeface = mDefaultTypeface;
                mPickerSubTypeface = Typeface.create(mDefaultTypeface, Typeface.NORMAL);
                mHcfFocusedTypefaceBold = Typeface.create(mPickerTypeface, Typeface.BOLD);
                setInputTextTypeface();
            } else {
                mInputText.setIncludeFontPadding(false);
                setInputTextTypeface();
                tryComputeMaxWidth();
            }
        }
    }

    @Override
    public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            InputMethodManager inputMethodManager
                    = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            }

            mLastFocusedChildVirtualViewId = 1;
            if (!mWrapSelectorWheel && getValue().equals(getMinValue())) {
                mLastFocusedChildVirtualViewId = 2;
            }

            if (mAccessibilityManager.isEnabled()) {
                AccessibilityNodeProviderImpl provider
                        = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                if (provider != null) {
                    provider.performAction(mLastFocusedChildVirtualViewId, 64, null);
                }
            }
        } else {
            if (mAccessibilityManager.isEnabled()) {
                AccessibilityNodeProviderImpl provider
                        = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                if (provider != null) {
                    provider.performAction(mLastFocusedChildVirtualViewId, 128, null);
                }
            }
            mLastFocusedChildVirtualViewId = View.NO_ID;
            mLastHoveredChildVirtualViewId = Integer.MIN_VALUE;
        }

        mDelegator.invalidate();
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int action = event.getAction();
        final int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    switch (action) {
                        case KeyEvent.ACTION_DOWN:
                            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                if (mLastFocusedChildVirtualViewId == 1) {
                                    mLastFocusedChildVirtualViewId = 2;
                                    mDelegator.invalidate();
                                    return true;
                                } else if (mLastFocusedChildVirtualViewId == 2) {
                                    if (!mWrapSelectorWheel && getValue().equals(getMinValue())) {
                                        return false;
                                    }
                                    mLastFocusedChildVirtualViewId = 3;
                                    mDelegator.invalidate();
                                    return true;
                                }
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                if (mLastFocusedChildVirtualViewId != 2) {
                                    if (mLastFocusedChildVirtualViewId == 3) {
                                        mLastFocusedChildVirtualViewId = 2;
                                        mDelegator.invalidate();
                                        return true;
                                    }
                                } else if (!mWrapSelectorWheel && getValue().equals(getMinValue())) {
                                    return false;
                                } else {
                                    mLastFocusedChildVirtualViewId = 1;
                                    mDelegator.invalidate();
                                    return true;
                                }
                            }
                            return false;
                        case KeyEvent.ACTION_UP:
                            if (mAccessibilityManager.isEnabled()) {
                                AccessibilityNodeProviderImpl provider
                                        = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                                if (provider != null) {
                                    provider.performAction(mLastFocusedChildVirtualViewId, 64, null);
                                }
                                return true;
                            }
                            return false;
                    }
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            View leftView = mDelegator.focusSearch(View.FOCUS_LEFT);
                            if (leftView != null) {
                                leftView.requestFocus(View.FOCUS_LEFT);
                            }
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            View rightView = mDelegator.focusSearch(View.FOCUS_RIGHT);
                            if (rightView != null) {
                                rightView.requestFocus(View.FOCUS_RIGHT);
                            }
                            return true;
                        }
                    }
                    return false;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    break;
                default:
                    return false;
            }
        }

        if (action == KeyEvent.ACTION_DOWN) {
            if (mLastFocusedChildVirtualViewId == 2) {
                performClick();
                removeAllCallbacks();
            } else if (mFlingScroller.isFinished()) {
                if (mLastFocusedChildVirtualViewId == 1) {
                    startFadeAnimation(false);
                    changeValueByOne(false);
                    Calendar minValue = (Calendar) getMinValue().clone();
                    minValue.add(Calendar.DAY_OF_MONTH, 1);
                    if (!mWrapSelectorWheel && getValue().equals(minValue)) {
                        mLastFocusedChildVirtualViewId = 2;
                    }
                    startFadeAnimation(true);
                } else if (mLastFocusedChildVirtualViewId == 3) {
                    startFadeAnimation(false);
                    changeValueByOne(true);
                    Calendar maxValue = (Calendar) getMaxValue().clone();
                    maxValue.add(Calendar.DAY_OF_MONTH, -1);
                    if (!mWrapSelectorWheel && getValue().equals(maxValue)) {
                        mLastFocusedChildVirtualViewId = 2;
                    }
                    startFadeAnimation(true);
                }
            }
        }
        return false;
    }

    @Override
    public void dispatchTrackballEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (mAccessibilityManager.isEnabled()) {
            final int eventY = (int) event.getY();

            int index = 2;
            if (eventY <= mTopSelectionDividerTop) {
                index = 1;
            } else if (mBottomSelectionDividerBottom <= eventY) {
                index = 3;
            }

            final int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_HOVER_MOVE:
                case MotionEvent.ACTION_HOVER_ENTER: {
                    updateHoveredVirtualView(index);
                    return index != Integer.MIN_VALUE;
                }
                case MotionEvent.ACTION_HOVER_EXIT: {
                    if (mLastHoveredChildVirtualViewId != Integer.MIN_VALUE) {
                        updateHoveredVirtualView(Integer.MIN_VALUE);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateHoveredVirtualView(int virtualViewId) {
        if (mLastHoveredChildVirtualViewId != virtualViewId) {
            final int previousVirtualViewId = mLastHoveredChildVirtualViewId;
            mLastHoveredChildVirtualViewId = virtualViewId;
            AccessibilityNodeProviderImpl provider
                    = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
            provider.sendAccessibilityEventForVirtualView(virtualViewId, 128);
            provider.sendAccessibilityEventForVirtualView(previousVirtualViewId, 256);
        }
    }

    @Override
    public void setSkipValuesOnLongPressEnabled(boolean enabled) {
        mSkipNumbers = enabled;
    }

    private void playSoundAndHapticFeedback() {
        mAudioManager.playSoundEffect(mPickerSoundIndex);
        if (!mHapticPreDrawListener.mSkipHapticCalls) {
            mDelegator.performHapticFeedback(mPickerVibrateIndex);
            mHapticPreDrawListener.mSkipHapticCalls = true;
        }
    }

    private static class HapticPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        boolean mSkipHapticCalls;

        private HapticPreDrawListener() {
            mSkipHapticCalls = false;
        }

        @Override
        public boolean onPreDraw() {
            mSkipHapticCalls = false;
            return true;
        }
    }

    @Override
    public void computeScroll() {
        if (!mSpringFlingRunning) {
            Scroller scroller = mFlingScroller;
            if (scroller.isFinished()) {
                scroller = mAdjustScroller;
                if (scroller.isFinished()) {
                    return;
                }
            }
            scroller.computeScrollOffset();
            int currentScrollerY = scroller.getCurrY();
            if (mPreviousScrollerY == 0) {
                mPreviousScrollerY = scroller.getStartY();
            }
            scrollBy(0, currentScrollerY - mPreviousScrollerY);
            mPreviousScrollerY = currentScrollerY;
            if (scroller.isFinished()) {
                onScrollerFinished(scroller);
            } else {
                mDelegator.invalidate();
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled && mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
            stopScrollAnimation();
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    // TODO rework this method
    // kang
    public void scrollBy(int var1, int var2) {
        /* var1 = x; var2 = y; */
        Calendar[] var3 = this.mSelectorIndices;
        if (var2 != 0 && this.mSelectorElementHeight > 0) {
            var1 = var2;
            if (!this.mWrapSelectorWheel) {
                var1 = var2;
                if (this.mCurrentScrollOffset + var2 > this.mInitialScrollOffset) {
                    var1 = var2;
                    if (var3[2].compareTo(this.mMinValue) <= 0) {
                        this.stopFlingAnimation();
                        var1 = this.mInitialScrollOffset - this.mCurrentScrollOffset;
                    }
                }
            }

            var2 = var1;
            if (!this.mWrapSelectorWheel) {
                var2 = var1;
                if (this.mCurrentScrollOffset + var1 < this.mInitialScrollOffset) {
                    var2 = var1;
                    if (var3[2].compareTo(this.mMaxValue) >= 0) {
                        this.stopFlingAnimation();
                        var2 = this.mInitialScrollOffset - this.mCurrentScrollOffset;
                    }
                }
            }

            this.mCurrentScrollOffset += var2;

            while(true) {
                var1 = this.mCurrentScrollOffset;
                if (var1 - this.mInitialScrollOffset < this.mValueChangeOffset) {
                    while(true) {
                        var1 = this.mCurrentScrollOffset;
                        if (var1 - this.mInitialScrollOffset > -this.mValueChangeOffset) {
                            return;
                        }

                        this.mCurrentScrollOffset = var1 + this.mSelectorElementHeight;
                        this.incrementSelectorIndices(var3);
                        if (!this.mIsStartingAnimation) {
                            this.setValueInternal(var3[2], true);
                            this.mIsValueChanged = true;
                            var1 = this.mLongPressCount;
                            if (var1 > 0) {
                                this.mLongPressCount = var1 - 1;
                            } else {
                                this.playSoundAndHapticFeedback();
                            }
                        }

                        if (!this.mWrapSelectorWheel && var3[2].compareTo(this.mMaxValue) >= 0) {
                            this.mCurrentScrollOffset = this.mInitialScrollOffset;
                        }
                    }
                }

                this.mCurrentScrollOffset = var1 - this.mSelectorElementHeight;
                this.decrementSelectorIndices(var3);
                if (!this.mIsStartingAnimation) {
                    this.setValueInternal(var3[2], true);
                    this.mIsValueChanged = true;
                    var1 = this.mLongPressCount;
                    if (var1 > 0) {
                        this.mLongPressCount = var1 - 1;
                    } else {
                        this.playSoundAndHapticFeedback();
                    }
                }

                if (!this.mWrapSelectorWheel && var3[2].compareTo(this.mMinValue) <= 0) {
                    this.mCurrentScrollOffset = this.mInitialScrollOffset;
                }
            }
        }
    }
    // kang

    @Override
    public int computeVerticalScrollOffset() {
        return mCurrentScrollOffset;
    }

    @Override
    public int computeVerticalScrollRange() {
        return (((int) TimeUnit.MILLISECONDS
                .toDays(mMaxValue.getTimeInMillis() - mMinValue.getTimeInMillis())) + 1)
                * mSelectorElementHeight;
    }

    @Override
    public int computeVerticalScrollExtent() {
        return mDelegator.getHeight();
    }

    @Override
    public void setOnValueChangedListener(
            SeslSpinningDatePickerSpinner.OnValueChangeListener onValueChangeListener) {
        mOnValueChangeListener = onValueChangeListener;
    }

    @Override
    public void setOnScrollListener(
            SeslSpinningDatePickerSpinner.OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    @Override
    public void setOnSpinnerDateClickListener(
            SeslSpinningDatePickerSpinner.OnSpinnerDateClickListener onSpinnerDateClickListener) {
        mOnSpinnerDateClickListener = onSpinnerDateClickListener;
    }

    @Override
    public SeslSpinningDatePickerSpinner.OnSpinnerDateClickListener getOnSpinnerDateClickListener() {
        return mOnSpinnerDateClickListener;
    }

    @Override
    public void setFormatter(SeslSpinningDatePickerSpinner.Formatter formatter) {
        if (formatter == mFormatter) {
            return;
        }
        mFormatter = formatter;
        initializeSelectorWheelIndices();
    }

    @Override
    public void setValue(Calendar value) {
        if (!mFlingScroller.isFinished() || mSpringAnimation.isRunning()) {
            stopScrollAnimation();
        }
        setValueInternal(value, false);
    }

    @Override
    public void performClick() {
        stopScrollAnimation();

        if (mOnSpinnerDateClickListener != null) {
            Calendar value = null;
            SeslSpinningDatePicker.LunarDate lunarDate;
            if (mIsLunar) {
                lunarDate = new SeslSpinningDatePicker.LunarDate();
                value = convertSolarToLunar(mValue, lunarDate);
            } else {
                lunarDate = null;
            }
            if (!mIsLunar) {
                value = mValue;
            }

            mOnSpinnerDateClickListener.onSpinnerDateClicked(value, lunarDate);
        }
    }

    @Override
    public void performClick(boolean toIncrement) {
        changeValueByOne(toIncrement);
    }

    @Override
    public void performLongClick() {
        mIgnoreMoveEvents = true;
        mIsLongClicked = true;
    }

    // TODO rework this method
    // kang
    private void tryComputeMaxWidth() {
        if (this.mComputeMaxWidth) {
            byte var1 = 0;
            float var2 = 0.0F;
            int var3 = 0;

            float var4;
            float var5;
            float var6;
            for(var4 = 0.0F; var3 <= 9; var4 = var6) {
                var5 = this.mSelectorWheelPaint.measureText(formatNumberWithLocale(var3));
                var6 = var4;
                if (var5 > var4) {
                    var6 = var5;
                }

                ++var3;
            }

            float var7 = (float)((int)((float)2 * var4));
            String[] var8 = (new android.icu.text.DateFormatSymbols(Locale.getDefault())).getShortWeekdays();
            int var9 = var8.length;
            var3 = 0;

            String var10;
            for(var6 = 0.0F; var3 < var9; var6 = var4) {
                var10 = var8[var3];
                var5 = this.mSelectorWheelPaint.measureText(var10);
                var4 = var6;
                if (var5 > var6) {
                    var4 = var5;
                }

                ++var3;
            }

            var8 = (new android.icu.text.DateFormatSymbols(Locale.getDefault())).getShortMonths();
            var9 = var8.length;

            for(var3 = var1; var3 < var9; var2 = var4) {
                var10 = var8[var3];
                var5 = this.mSelectorWheelPaint.measureText(var10);
                var4 = var2;
                if (var5 > var2) {
                    var4 = var5;
                }

                ++var3;
            }

            int var11 = (int)(var7 + var6 + var2 + this.mSelectorWheelPaint.measureText(" ") * 2.0F + this.mSelectorWheelPaint.measureText(",")) + this.mInputText.getPaddingLeft() + this.mInputText.getPaddingRight();
            var3 = var11;
            if (this.isHighContrastFontEnabled()) {
                var3 = var11 + (int)Math.ceil((double)(SeslPaintReflector.getHCTStrokeWidth(this.mSelectorWheelPaint) / 2.0F)) * 13;
            }

            if (this.mMaxWidth != var3) {
                var11 = this.mMinWidth;
                if (var3 > var11) {
                    this.mMaxWidth = var3;
                } else {
                    this.mMaxWidth = var11;
                }

                this.mDelegator.invalidate();
            }

        }
    }
    // kang

    @Override
    public boolean getWrapSelectorWheel() {
        return mWrapSelectorWheel;
    }

    @Override
    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mWrapSelectorWheelPreferred = wrapSelectorWheel;
        updateWrapSelectorWheel();
    }

    private void updateWrapSelectorWheel() {
        boolean wrapSelectorWheel = true;
        final boolean wrappingAllowed = ((int) TimeUnit.MILLISECONDS
                .toDays(mMaxValue.getTimeInMillis() - mMinValue.getTimeInMillis()))
                >= mSelectorIndices.length;
        if (!wrappingAllowed || !mWrapSelectorWheelPreferred) {
            wrapSelectorWheel = false;
        }
        if (mWrapSelectorWheel != wrapSelectorWheel) {
            mWrapSelectorWheel = wrapSelectorWheel;
            initializeSelectorWheelIndices();
            mDelegator.invalidate();
        }
    }

    @Override
    public void setOnLongPressUpdateInterval(long interval) {
        mLongPressUpdateInterval = interval;
    }

    @Override
    public Calendar getValue() {
        return mValue;
    }

    @Override
    public Calendar getMinValue() {
        return mMinValue;
    }

    @Override
    public void setMinValue(Calendar minValue) {
        if (mMinValue.equals(minValue)) {
            return;
        }
        clearCalendar(mMinValue, minValue);
        if (mMinValue.compareTo(mValue) > 0) {
            clearCalendar(mValue, mMinValue);
        }
        updateWrapSelectorWheel();
        initializeSelectorWheelIndices();
        tryComputeMaxWidth();
        mDelegator.invalidate();
    }

    @Override
    public Calendar getMaxValue() {
        return mMaxValue;
    }

    @Override
    public void setMaxValue(Calendar maxValue) {
        if (mMaxValue.equals(maxValue)) {
            return;
        }
        clearCalendar(mMaxValue, maxValue);
        if (mMaxValue.compareTo(mValue) < 0) {
            clearCalendar(mValue, mMaxValue);
        }
        updateWrapSelectorWheel();
        initializeSelectorWheelIndices();
        tryComputeMaxWidth();
        mDelegator.invalidate();
    }

    @Override
    public int getMaxHeight() {
        return 0;
    }

    @Override
    public int getMaxWidth() {
        return 0;
    }

    @Override
    public int getMinHeight() {
        return 0;
    }

    @Override
    public int getMinWidth() {
        return 0;
    }

    @Override
    public void setTextSize(float size) {
        final int scaledSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, size, mContext.getResources().getDisplayMetrics());
        mTextSize = scaledSize;
        mSelectorWheelPaint.setTextSize(scaledSize);
        mInputText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        tryComputeMaxWidth();
    }

    @Override
    public void setSubTextSize(float size) {
    }

    @Override
    public void setTextTypeface(Typeface typeface) {
        mCustomTypefaceSet = true;
        mPickerTypeface = typeface;
        mPickerSubTypeface = Typeface.create(typeface, Typeface.NORMAL);
        mSelectorWheelPaint.setTypeface(mPickerTypeface);
        mHcfFocusedTypefaceBold = Typeface.create(mPickerTypeface, Typeface.BOLD);
        setInputTextTypeface();
        tryComputeMaxWidth();
    }

    private void setInputTextTypeface() {
        if (mIsHcfEnabled) {
            mInputText.setTypeface(mHcfFocusedTypefaceBold);
        } else {
            mInputText.setTypeface(mPickerTypeface);
        }
    }

    private void setHcfTextAppearance(boolean bold) {
        if (mIsHcfEnabled) {
            if (bold) {
                mSelectorWheelPaint.setTypeface(mHcfFocusedTypefaceBold);
            } else {
                mSelectorWheelPaint.setTypeface(mPickerSubTypeface);
            }
        }
    }

    private static Typeface getFontTypeface(String fontFile) {
        if (!new File(fontFile).exists()) {
            return null;
        }
        try {
            return Typeface.createFromFile(fontFile);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getPaintFlags() {
        return mSelectorWheelPaint.getFlags();
    }

    @Override
    public void setPaintFlags(int flags) {
        if (mSelectorWheelPaint.getFlags() != flags) {
            mSelectorWheelPaint.setFlags(flags);
            mInputText.setPaintFlags(flags);
            tryComputeMaxWidth();
        }
    }

    @Override
    public void startAnimation(int delayTime, SeslAnimationListener listener) {
        mAnimationListener = listener;
        mAlpha = mActivatedAlpha;

        mDelegator.post(new Runnable() {
            @Override
            public void run() {
                if (mSelectorElementHeight == 0) {
                    mReservedStartAnimation = true;
                    return;
                }
                mIsStartingAnimation = true;
                mFlingScroller = mCustomScroller;
                scrollBy(0, mSelectorElementHeight * 5);
                mDelegator.invalidate();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int forwardDistance = (int) (mSelectorElementHeight * 5.4d);
                                if (!moveToFinalScrollerPosition(mFlingScroller)) {
                                    moveToFinalScrollerPosition(mAdjustScroller);
                                }
                                mPreviousScrollerY = 0;
                                mFlingScroller.startScroll(0, 0, 0, -forwardDistance, START_ANIMATION_SCROLL_DURATION_2016B);
                                mDelegator.invalidate();

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        moveToFinalScrollerPosition(mFlingScroller);
                                        mFlingScroller.abortAnimation();
                                        mAdjustScroller.abortAnimation();
                                        ensureScrollWheelAdjusted();
                                        mFlingScroller = mLinearScroller;
                                        mIsStartingAnimation = false;
                                        mDelegator.invalidate();
                                        startFadeAnimation(true);
                                        if (mAnimationListener != null) {
                                            mAnimationListener.onAnimationEnd();
                                        }
                                    }
                                }, START_ANIMATION_SCROLL_DURATION);
                            }
                        }, 100);
                    }
                }, delayTime);
            }
        });
    }

    private void stopScrollAnimation() {
        mFlingScroller.abortAnimation();
        mAdjustScroller.abortAnimation();
        mGravityScroller.abortAnimation();
        mSpringAnimation.cancel();
        mSpringFlingRunning = false;
        if (!mIsStartingAnimation && !moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller);
        }
        ensureScrollWheelAdjusted();
    }

    private void stopFlingAnimation() {
        mFlingScroller.abortAnimation();
        mAdjustScroller.abortAnimation();
        mGravityScroller.abortAnimation();
        mSpringAnimation.cancel();
        mSpringFlingRunning = false;
    }

    private void startFadeAnimation(boolean fadeOut) {
        if (fadeOut) {
            mFadeOutAnimator.setStartDelay(mFlingScroller.getDuration() + 100);
            mColorOutAnimator.setStartDelay((mFlingScroller.isFinished() ?
                    0 : mFlingScroller.getDuration()) + 100);
            mColorOutAnimator.start();
            mFadeOutAnimator.start();
        } else {
            mFadeInAnimator.setFloatValues(mAlpha, mActivatedAlpha);
            mColorInAnimator.setIntValues(mTextColor, mTextColorScrolling);
            mColorOutAnimator.cancel();
            mFadeOutAnimator.cancel();
            mColorInAnimator.start();
            mFadeInAnimator.start();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        mGravityScroller.abortAnimation();
        mSpringAnimation.cancel();
        mSpringFlingRunning = false;
        removeAllCallbacks();
        mDelegator.getViewTreeObserver().removeOnPreDrawListener(mHapticPreDrawListener);
    }

    @Override
    public void onAttachedToWindow() {
        mDelegator.getViewTreeObserver().addOnPreDrawListener(mHapticPreDrawListener);
    }

    // TODO rework this method
    // kang
    @Override
    public void onDraw(Canvas var1) {
        int var2 = this.mDelegator.getRight();
        int var3 = this.mDelegator.getLeft();
        int var4 = this.mDelegator.getBottom();
        float var5 = (float)(var2 - var3) / 2.0F;
        float var6 = (float)(this.mCurrentScrollOffset - this.mSelectorElementHeight);
        Drawable var7 = this.mVirtualButtonFocusedDrawable;
        if (var7 != null && this.mScrollState == 0) {
            var3 = this.mLastFocusedChildVirtualViewId;
            if (var3 != 1) {
                if (var3 != 2) {
                    if (var3 == 3) {
                        var7.setState(this.mDelegator.getDrawableState());
                        this.mVirtualButtonFocusedDrawable.setBounds(0, this.mBottomSelectionDividerBottom, var2, var4);
                        this.mVirtualButtonFocusedDrawable.draw(var1);
                    }
                } else {
                    var7.setState(this.mDelegator.getDrawableState());
                    this.mVirtualButtonFocusedDrawable.setBounds(0, this.mTopSelectionDividerTop, var2, this.mBottomSelectionDividerBottom);
                    this.mVirtualButtonFocusedDrawable.draw(var1);
                }
            } else {
                var7.setState(this.mDelegator.getDrawableState());
                this.mVirtualButtonFocusedDrawable.setBounds(0, 0, var2, this.mTopSelectionDividerTop);
                this.mVirtualButtonFocusedDrawable.draw(var1);
            }
        }

        Calendar[] var17 = this.mSelectorIndices;
        int var8 = var17.length;

        for(var3 = 0; var3 < var8; ++var3) {
            Calendar var9 = var17[var3];
            String var18 = (String)this.mSelectorIndexToStringCache.get(var9);
            float var10 = this.mAlpha;
            float var11 = this.mIdleAlpha;
            float var12 = var10;
            if (var10 < var11) {
                var12 = var11;
            }

            label38: {
                int var13 = (int)((this.mSelectorWheelPaint.descent() - this.mSelectorWheelPaint.ascent()) / 2.0F + var6 - this.mSelectorWheelPaint.descent());
                int var14 = this.mTopSelectionDividerTop;
                int var15 = this.mInitialScrollOffset;
                if (var6 >= (float)(var14 - var15)) {
                    int var16 = this.mBottomSelectionDividerBottom;
                    if (var6 <= (float)(var15 + var16)) {
                        if (var6 <= (float)(var14 + var16) / 2.0F) {
                            var1.save();
                            var1.clipRect(0, this.mTopSelectionDividerTop, var2, this.mBottomSelectionDividerBottom);
                            this.mSelectorWheelPaint.setColor(this.mTextColor);
                            this.mSelectorWheelPaint.setTypeface(this.mPickerTypeface);
                            var11 = (float)var13;
                            var1.drawText(var18, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                            var1.save();
                            var1.clipRect(0, 0, var2, this.mTopSelectionDividerTop);
                            this.mSelectorWheelPaint.setTypeface(this.mPickerSubTypeface);
                            this.mSelectorWheelPaint.setAlpha((int)(var12 * 255.0F * this.mInitialAlpha));
                            var1.drawText(var18, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                        } else {
                            var1.save();
                            var1.clipRect(0, this.mTopSelectionDividerTop, var2, this.mBottomSelectionDividerBottom);
                            this.mSelectorWheelPaint.setTypeface(this.mPickerTypeface);
                            this.mSelectorWheelPaint.setColor(this.mTextColor);
                            var11 = (float)var13;
                            var1.drawText(var18, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                            var1.save();
                            var1.clipRect(0, this.mBottomSelectionDividerBottom, var2, var4);
                            this.mSelectorWheelPaint.setAlpha((int)(var12 * 255.0F * this.mInitialAlpha));
                            this.mSelectorWheelPaint.setTypeface(this.mPickerSubTypeface);
                            var1.drawText(var18, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                        }
                        break label38;
                    }
                }

                var1.save();
                this.mSelectorWheelPaint.setAlpha((int)(var12 * 255.0F * this.mInitialAlpha));
                this.mSelectorWheelPaint.setTypeface(this.mPickerSubTypeface);
                var1.drawText(var18, var5, (float)var13, this.mSelectorWheelPaint);
                var1.restore();
            }

            var6 += (float)this.mSelectorElementHeight;
        }

    }
    // kang

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(SeslSpinningDatePickerSpinner.class.getName());
        event.setScrollable(true);
        event.setScrollY(((int) TimeUnit.MILLISECONDS
                .toDays(mValue.getTimeInMillis() - mMinValue.getTimeInMillis())) * mSelectorElementHeight);
        event.setMaxScrollY(((int) TimeUnit.MILLISECONDS
                .toDays(mMaxValue.getTimeInMillis() - mMinValue.getTimeInMillis())) * mSelectorElementHeight);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeProviderImpl provider
                = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
        event.getText().add(provider.getVirtualCurrentButtonText());
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new AccessibilityNodeProviderImpl();
        }
        return mAccessibilityNodeProvider;
    }

    @Override
    public void setLunar(boolean isLunar, boolean isLeapMonth) {
        mIsLunar = isLunar;
        mIsLeapMonth = isLeapMonth;

        if (isLunar) {
            if (mSolarLunarConverter == null) {
                mPathClassLoader = SeslSpinningDatePicker.LunarUtils.getPathClassLoader(mContext);
                mSolarLunarConverter = SeslFeatureReflector.getSolarLunarConverter(mPathClassLoader);
            }
        } else {
            mPathClassLoader = null;
            mSolarLunarConverter = null;
        }
    }

    private int makeMeasureSpec(int measureSpec, int maxSize) {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec;
        }
        final int size = View.MeasureSpec.getSize(measureSpec);
        final int mode = View.MeasureSpec.getMode(measureSpec);
        switch (mode) {
            case View.MeasureSpec.EXACTLY:
                return measureSpec;
            case View.MeasureSpec.AT_MOST:
                return View.MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), View.MeasureSpec.EXACTLY);
            case View.MeasureSpec.UNSPECIFIED:
                return View.MeasureSpec.makeMeasureSpec(maxSize, View.MeasureSpec.EXACTLY);
            default:
                throw new IllegalArgumentException("Unknown measure mode: " + mode);
        }
    }

    private int resolveSizeAndStateRespectingMinSize(int minSize, int measuredSize, int measureSpec) {
        if (minSize != SIZE_UNSPECIFIED) {
            final int desiredWidth = Math.max(minSize, measuredSize);
            return View.resolveSizeAndState(desiredWidth, measureSpec, 0);
        } else {
            return measuredSize;
        }
    }

    private void initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear();
        Calendar[] selectorIndices = mSelectorIndices;
        Calendar current = getValue();
        for (int i = 0; i < mSelectorIndices.length; i++) {
            Calendar value = (Calendar) current.clone();
            value.add(Calendar.DAY_OF_MONTH, i - 2);
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            selectorIndices[i] = value;
            ensureCachedScrollSelectorValue(selectorIndices[i]);
        }
    }

    private void setValueInternal(Calendar current, boolean notifyChange) {
        if (mWrapSelectorWheel) {
            current = getWrappedSelectorIndex(current);
        } else {
            if (current.compareTo(mMinValue) < 0) {
                current = (Calendar) mMinValue.clone();
            }
            if (current.compareTo(mMaxValue) > 0) {
                current = (Calendar) mMaxValue.clone();
            }
        }
        Calendar previous = (Calendar) mValue.clone();
        clearCalendar(mValue, current);
        if (notifyChange) {
            notifyChange(previous);
        }
        initializeSelectorWheelIndices();
        mDelegator.invalidate();
    }

    private void changeValueByOne(boolean increment) {
        if (!moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller);
        }
        mPreviousScrollerY = 0;
        mChangeValueBy = DEFAULT_CHANGE_VALUE_BY;
        if (mLongPressed_FIRST_SCROLL) {
            mLongPressed_FIRST_SCROLL = false;
            mLongPressed_SECOND_SCROLL = true;
        } else if (mLongPressed_SECOND_SCROLL) {
            mLongPressed_SECOND_SCROLL = false;
            mLongPressed_THIRD_SCROLL = true;

            if (getValue().get(Calendar.DAY_OF_MONTH) % LONG_PRESSED_SCROLL_COUNT == 0) {
                mChangeValueBy = LONG_PRESSED_SCROLL_COUNT;
            } else {
                if (increment) {
                    mChangeValueBy = LONG_PRESSED_SCROLL_COUNT
                            - (getValue().get(Calendar.DAY_OF_MONTH) % LONG_PRESSED_SCROLL_COUNT);
                } else {
                    mChangeValueBy = getValue().get(Calendar.DAY_OF_MONTH) % LONG_PRESSED_SCROLL_COUNT;
                }
            }
        } else if (mLongPressed_THIRD_SCROLL) {
            mChangeValueBy = LONG_PRESSED_SCROLL_COUNT;
        }

        int duration = SNAP_SCROLL_DURATION;
        if (mIsLongPressed && mSkipNumbers) {
            duration = 200;
            mLongPressUpdateInterval = 600;
        } else if (mIsLongPressed) {
            duration = 100;
            mChangeValueBy = DEFAULT_CHANGE_VALUE_BY;
            mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;
        }
        mLongPressCount = mChangeValueBy - 1;

        if (increment) {
            mFlingScroller.startScroll(0, 0,
                    0, (-mSelectorElementHeight) * mChangeValueBy, duration);
        } else {
            mFlingScroller.startScroll(0, 0,
                    0, mSelectorElementHeight * mChangeValueBy, duration);
        }
        mDelegator.invalidate();
    }

    private void initializeSelectorWheel() {
        if (mIsStartingAnimation) {
            if (!moveToFinalScrollerPosition(mFlingScroller)) {
                moveToFinalScrollerPosition(mAdjustScroller);
            }
            stopScrollAnimation();
        } else if (!mIsStartingAnimation) {
            initializeSelectorWheelIndices();
        }
        int totalTextHeight = mTextSize * 3;
        float totalTextGapHeight = (mDelegator.getBottom() - mDelegator.getTop()) - totalTextHeight;
        mSelectorTextGapHeight = (int) ((totalTextGapHeight / 3) + 0.5f);
        mSelectorElementHeight = mTextSize + mSelectorTextGapHeight;
        mValueChangeOffset = (mModifiedTxtHeight > mSelectorElementHeight) ?
                mDelegator.getHeight() / 3 : mModifiedTxtHeight;
        mInitialScrollOffset = (mInputText.getTop() + (mModifiedTxtHeight / 2)) - mSelectorElementHeight;
        mCurrentScrollOffset = mInitialScrollOffset;
        ((SeslSpinningDatePickerSpinner.CustomEditText) mInputText).setEditTextPosition(
                ((int) (((mSelectorWheelPaint.descent() - mSelectorWheelPaint.ascent()) / 2.0f) - mSelectorWheelPaint.descent()))
                        - (mInputText.getBaseline() - (mModifiedTxtHeight / 2)));
        if (mReservedStartAnimation) {
            startAnimation(0, mAnimationListener);
            mReservedStartAnimation = false;
        }
    }

    private void onScrollerFinished(Scroller scroller) {
        if (scroller == mFlingScroller) {
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    private void onScrollStateChange(int scrollState) {
        if (mScrollState == scrollState) {
            return;
        }
        mScrollState = scrollState;
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChange(mDelegator, scrollState);
        }
    }

    // TODO rework this method
    // kang
    private void fling(int velocityY) {
        if (!this.mWrapSelectorWheel && velocityY > 0 && getValue().equals(getMinValue())) {
            startFadeAnimation(true);
        } else if (this.mWrapSelectorWheel || velocityY >= 0 || !getValue().equals(getMaxValue())) {
            this.mPreviousScrollerY = 0;
            float f = velocityY;
            Math.round((Math.abs(velocityY) / this.mMaximumFlingVelocity) * f);
            this.mPreviousSpringY = this.mCurrentScrollOffset;
            this.mSpringAnimation.setStartVelocity(f);
            this.mGravityScroller.forceFinished(true);
            this.mGravityScroller.fling(0, this.mCurrentScrollOffset, 0, velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            int round = Math.round((this.mGravityScroller.getFinalY() + this.mCurrentScrollOffset) / this.mSelectorElementHeight);
            int i2 = this.mSelectorElementHeight;
            int i3 = this.mInitialScrollOffset;
            int i4 = (round * i2) + i3;
            int i;
            if (velocityY > 0) {
                i = Math.max(i4, i2 + i3);
            } else {
                i = Math.min(i4, (-i2) + i3);
            }
            this.mSpringAnimation.setStartValue(this.mCurrentScrollOffset);
            this.mSpringFlingRunning = true;
            this.mSpringAnimation.animateToFinalPosition(i);
            this.mDelegator.invalidate();
        } else {
            startFadeAnimation(true);
        }
    }
    // kang

    private Calendar getWrappedSelectorIndex(Calendar selectorIndex) {
        if (selectorIndex.compareTo(mMaxValue) > 0) {
            Calendar minValue = (Calendar) mMinValue.clone();
            minValue.add(Calendar.DAY_OF_MONTH, ((int) TimeUnit.MILLISECONDS
                    .toDays(selectorIndex.getTimeInMillis() - mMinValue.getTimeInMillis()))
                    % (((int) TimeUnit.MILLISECONDS
                        .toDays(mMaxValue.getTimeInMillis() - mMinValue.getTimeInMillis())) + 1));
            return minValue;
        } else if (selectorIndex.compareTo(mMinValue) < 0) {
            Calendar maxValue = (Calendar) mMaxValue.clone();
            maxValue.add(Calendar.DAY_OF_MONTH, -(((int) TimeUnit.MILLISECONDS
                    .toDays(mMaxValue.getTimeInMillis() - selectorIndex.getTimeInMillis()))
                    % (((int) TimeUnit.MILLISECONDS
                    .toDays(mMaxValue.getTimeInMillis() - mMinValue.getTimeInMillis())) + 1)));
            return maxValue;
        }
        return selectorIndex;
    }

    private void incrementSelectorIndices(Calendar[] selectorIndices) {
        System.arraycopy(selectorIndices, 1, selectorIndices,
                0, selectorIndices.length - 1);
        Calendar nextScrollSelectorIndex = (Calendar) selectorIndices[selectorIndices.length - 2].clone();
        nextScrollSelectorIndex.add(Calendar.DAY_OF_MONTH, 1);
        if (mWrapSelectorWheel && nextScrollSelectorIndex.compareTo(mMaxValue) > 0) {
            clearCalendar(nextScrollSelectorIndex, mMinValue);
        }
        selectorIndices[selectorIndices.length - 1] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    private void decrementSelectorIndices(Calendar[] selectorIndices) {
        System.arraycopy(selectorIndices, 0, selectorIndices,
                1, selectorIndices.length - 1);
        Calendar nextScrollSelectorIndex = (Calendar) selectorIndices[1].clone();
        nextScrollSelectorIndex.add(Calendar.DAY_OF_MONTH, -1);
        if (mWrapSelectorWheel && nextScrollSelectorIndex.compareTo(mMinValue) < 0) {
            clearCalendar(nextScrollSelectorIndex, mMaxValue);
        }
        selectorIndices[0] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    private void ensureCachedScrollSelectorValue(Calendar selectorIndex) {
        HashMap<Calendar, String> cache = mSelectorIndexToStringCache;
        String scrollSelectorValue = cache.get(selectorIndex);
        if (scrollSelectorValue != null) {
            return;
        }
        if (selectorIndex.compareTo(mMinValue) < 0 || selectorIndex.compareTo(mMaxValue) > 0) {
            scrollSelectorValue = "";
        } else {
            if (mIsLunar) {
                scrollSelectorValue = formatDateForLunar(selectorIndex);
            } else {
                scrollSelectorValue = formatDate(selectorIndex);
            }
        }
        cache.put(selectorIndex, scrollSelectorValue);
    }

    // TODO rework this method
    // kang
    private String formatDateForLunar(Calendar calendar) {
        String str;
        int i;
        Calendar calendar2 = (Calendar) calendar.clone();
        SeslSpinningDatePicker.LunarDate lunarDate = new SeslSpinningDatePicker.LunarDate();
        convertSolarToLunar(calendar, lunarDate);
        SeslSpinningDatePickerSpinner.Formatter formatter = this.mFormatter;
        if (formatter == null) {
            str = formatDateWithLocale(calendar2);
        } else if (formatter instanceof SeslSpinningDatePickerSpinner.DateFormatter) {
            str = ((SeslSpinningDatePickerSpinner.DateFormatter) formatter).format(calendar2, this.mContext);
        } else {
            str = formatter.format(calendar2);
        }
        String dayWithLocale = getDayWithLocale(lunarDate.day);
        String formatDayWithLocale = formatDayWithLocale(calendar2);
        String monthWithLocale = getMonthWithLocale(lunarDate.month);
        String formatMonthWithLocale = formatMonthWithLocale(calendar2);
        StringBuilder sb = new StringBuilder(str);
        if (Locale.getDefault().getLanguage() == "vi") {
            i = sb.lastIndexOf(" " + formatDayWithLocale) + 1;
        } else {
            i = sb.lastIndexOf(formatDayWithLocale);
        }
        if (i != -1) {
            sb.replace(i, formatDayWithLocale.length() + i, dayWithLocale);
        }
        int lastIndexOf = sb.lastIndexOf(formatMonthWithLocale);
        if (lastIndexOf != -1) {
            sb.replace(lastIndexOf, formatMonthWithLocale.length() + lastIndexOf, monthWithLocale);
        }
        return sb.toString();
    }
    // kang

    // TODO rework this method
    // kang
    private String formatDateForLunarForAccessibility(Calendar calendar) {
        String str;
        Calendar calendar2 = (Calendar) calendar.clone();
        SeslSpinningDatePicker.LunarDate lunarDate = new SeslSpinningDatePicker.LunarDate();
        convertSolarToLunar(calendar, lunarDate);
        SeslSpinningDatePickerSpinner.Formatter formatter = this.mFormatter;
        if (formatter == null) {
            str = formatDateWithLocaleForAccessibility(calendar2);
        } else if (formatter instanceof SeslSpinningDatePickerSpinner.DateFormatter) {
            str = ((SeslSpinningDatePickerSpinner.DateFormatter) formatter).formatForAccessibility(calendar2, this.mContext);
        } else {
            str = formatter.format(calendar2);
        }
        String dayWithLocale = getDayWithLocale(lunarDate.day);
        String formatDayWithLocale = formatDayWithLocale(calendar2);
        String monthWithLocaleForAccessibility = getMonthWithLocaleForAccessibility(lunarDate.month);
        String formatMonthWithLocaleForAccessibility = formatMonthWithLocaleForAccessibility(calendar2);
        StringBuilder sb = new StringBuilder(str);
        int lastIndexOf = sb.lastIndexOf(formatDayWithLocale);
        if (lastIndexOf != -1) {
            sb.replace(lastIndexOf, formatDayWithLocale.length() + lastIndexOf, dayWithLocale);
        }
        int lastIndexOf2 = sb.lastIndexOf(formatMonthWithLocaleForAccessibility);
        if (lastIndexOf2 != -1) {
            sb.replace(lastIndexOf2, formatMonthWithLocaleForAccessibility.length() + lastIndexOf2, monthWithLocaleForAccessibility);
        }
        return sb.toString();
    }
    // kang

    private String formatDate(Calendar value) {
        if (mFormatter != null) {
            if (mFormatter instanceof SeslSpinningDatePickerSpinner.DateFormatter) {
                return ((SeslSpinningDatePickerSpinner.DateFormatter) mFormatter)
                        .format(value, mContext);
            } else {
                return mFormatter.format(value);
            }
        } else {
            return formatDateWithLocale(value);
        }
    }

    private String formatDateForAccessibility(Calendar value) {
        if (mFormatter != null) {
            if (mFormatter instanceof SeslSpinningDatePickerSpinner.DateFormatter) {
                return ((SeslSpinningDatePickerSpinner.DateFormatter) mFormatter)
                        .formatForAccessibility(value, mContext);
            } else {
                return mFormatter.format(value);
            }
        } else {
            return formatDateWithLocale(value);
        }
    }

    private void validateInputTextView(View v) {
        String str = String.valueOf(((TextView) v).getText());
        Calendar current = getSelectedPos(str);
        if (!TextUtils.isEmpty(str) && !mValue.equals(current)) {
            setValueInternal(current, true);
        }
    }

    private void notifyChange(Calendar previous) {
        if (mAccessibilityManager.isEnabled() && !mIsStartingAnimation) {
            Calendar value = getWrappedSelectorIndex(mValue);
            String text = null;
            if (value.compareTo(mMaxValue) <= 0) {
                if (this.mIsLunar) {
                    text = formatDateForLunarForAccessibility(value);
                } else {
                    text = formatDateForAccessibility(value);
                }
            }
            mDelegator.sendAccessibilityEvent(4);
        }

        if (mOnValueChangeListener != null) {
            if (mIsLunar) {
                SeslSpinningDatePicker.LunarDate lunarDate = new SeslSpinningDatePicker.LunarDate();
                mOnValueChangeListener.onValueChange(mDelegator,
                        convertSolarToLunar(previous, null),
                        convertSolarToLunar(mValue, lunarDate), lunarDate.isLeapMonth, lunarDate);
            } else {
                mOnValueChangeListener.onValueChange(mDelegator,
                        previous, mValue, false, null);
            }
        }
    }

    private void postChangeCurrentByOneFromLongPress(boolean increment, long delayMillis) {
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
        } else {
            mDelegator.removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        mIsLongPressed = true;
        mLongPressed_FIRST_SCROLL = true;
        mChangeCurrentByOneFromLongPressCommand.setStep(increment);
        mDelegator.postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis);
    }

    private void removeChangeCurrentByOneFromLongPress() {
        if (mIsLongPressed) {
            mIsLongPressed = false;
            mCurrentScrollOffset = mInitialScrollOffset;
        }
        mLongPressed_FIRST_SCROLL = false;
        mLongPressed_SECOND_SCROLL = false;
        mLongPressed_THIRD_SCROLL = false;
        mChangeValueBy = DEFAULT_CHANGE_VALUE_BY;
        mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            mDelegator.removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
    }

    private void removeAllCallbacks() {
        if (mIsLongPressed) {
            mIsLongPressed = false;
            mCurrentScrollOffset = mInitialScrollOffset;
        }
        mLongPressed_FIRST_SCROLL = false;
        mLongPressed_SECOND_SCROLL = false;
        mLongPressed_THIRD_SCROLL = false;
        mChangeValueBy = DEFAULT_CHANGE_VALUE_BY;
        mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            mDelegator.removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        mPressedStateHelper.cancel();
    }

    private Calendar getSelectedPos(String value) {
        Calendar calendar = SeslSpinningDatePickerSpinner.getDateFormatter().parse(value);
        return calendar == null ? (Calendar) mMinValue.clone() : calendar;
    }

    private boolean ensureScrollWheelAdjusted() {
        return ensureScrollWheelAdjusted(0);
    }

    private boolean ensureScrollWheelAdjusted(int distance) {
        if (mInitialScrollOffset == Integer.MIN_VALUE) {
            return false;
        }

        int deltaY = mInitialScrollOffset - mCurrentScrollOffset;
        if (deltaY != 0) {
            mPreviousScrollerY = 0;

            if (!mIsValueChanged && distance != 0) {
                if (Math.abs(distance) < mSelectorElementHeight) {
                    deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;

                    mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
                    mDelegator.invalidate();
                    mIsValueChanged = false;
                    return true;
                }
            }

            if (Math.abs(deltaY) > mSelectorElementHeight / 2) {
                deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;
            }

            mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            mDelegator.invalidate();
            mIsValueChanged = false;
            return true;
        }
        mIsValueChanged = false;
        return false;
    }

    class PressedStateHelper implements Runnable {
        static final int BUTTON_DECREMENT = 2;
        static final int BUTTON_INCREMENT = 1;
        private final int MODE_PRESS = 1;
        private final int MODE_TAPPED = 2;
        private int mManagedButton;
        private int mMode;

        void cancel() {
            final int mRight = mDelegator.getRight();
            final int mBottom = mDelegator.getBottom();
            mMode = 0;
            mManagedButton = 0;
            mDelegator.removeCallbacks(this);
            if (mIncrementVirtualButtonPressed) {
                mIncrementVirtualButtonPressed = false;
                mDelegator.invalidate(0, mBottomSelectionDividerBottom,
                        mRight, mBottom);
            }
            if (mDecrementVirtualButtonPressed) {
                mDecrementVirtualButtonPressed = false;
                mDelegator.invalidate(0, 0,
                        mRight, mTopSelectionDividerTop);
            }
        }

        void buttonPressDelayed(int button) {
            cancel();
            mMode = MODE_PRESS;
            mManagedButton = button;
            mDelegator.postDelayed(this, ViewConfiguration.getTapTimeout());
        }

        void buttonTapped(int button) {
            cancel();
            mMode = MODE_TAPPED;
            mManagedButton = button;
            mDelegator.post(this);
        }

        @Override
        public void run() {
            final int mRight = mDelegator.getRight();
            final int mBottom = mDelegator.getBottom();
            switch (mMode) {
                case MODE_PRESS: {
                    switch (mManagedButton) {
                        case BUTTON_INCREMENT: {
                            mIncrementVirtualButtonPressed = true;
                            mDelegator.invalidate(0, mBottomSelectionDividerBottom,
                                    mRight, mBottom);
                        } break;
                        case BUTTON_DECREMENT: {
                            mDecrementVirtualButtonPressed = true;
                            mDelegator.invalidate(0, 0,
                                    mRight, mTopSelectionDividerTop);
                        }
                    }
                } break;
                case MODE_TAPPED: {
                    switch (mManagedButton) {
                        case BUTTON_INCREMENT: {
                            if (!mIncrementVirtualButtonPressed) {
                                mDelegator.postDelayed(this, ViewConfiguration.getPressedStateDuration());
                            }
                            mIncrementVirtualButtonPressed ^= true;
                            mDelegator.invalidate(0, mBottomSelectionDividerBottom,
                                    mRight, mBottom);
                        } break;
                        case BUTTON_DECREMENT: {
                            if (!mDecrementVirtualButtonPressed) {
                                mDelegator.postDelayed(this,
                                        ViewConfiguration.getPressedStateDuration());
                            }
                            mDecrementVirtualButtonPressed ^= true;
                            mDelegator.invalidate(0, 0,
                                    mRight, mTopSelectionDividerTop);
                        }
                    }
                } break;
            }
        }
    }

    class ChangeCurrentByOneFromLongPressCommand implements Runnable {
        private boolean mIncrement;

        private void setStep(boolean increment) {
            mIncrement = increment;
        }

        @Override
        public void run() {
            changeValueByOne(mIncrement);
            mDelegator.postDelayed(this, mLongPressUpdateInterval);
        }
    }

    class AccessibilityNodeProviderImpl extends AccessibilityNodeProvider {
        private static final int UNDEFINED = Integer.MIN_VALUE;
        private static final int VIRTUAL_VIEW_ID_DECREMENT = 1;
        private static final int VIRTUAL_VIEW_ID_CENTER = 2;
        private static final int VIRTUAL_VIEW_ID_INCREMENT = 3;
        private final Rect mTempRect = new Rect();
        private final int[] mTempArray = new int[2];
        private int mAccessibilityFocusedView = Integer.MIN_VALUE;

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            final int mLeft = mDelegator.getLeft();
            final int mRight = mDelegator.getRight();
            final int mTop = mDelegator.getTop();
            final int mBottom = mDelegator.getBottom();
            final int mScrollX = mDelegator.getScrollX();
            final int mScrollY = mDelegator.getScrollY();

            if (mLastFocusedChildVirtualViewId != View.NO_ID
                    || mLastHoveredChildVirtualViewId != Integer.MIN_VALUE) {
                switch (virtualViewId) {
                    case View.NO_ID:
                        return createAccessibilityNodeInfoForDatePickerWidget(
                                mScrollX,
                                mScrollY,
                                mScrollX + (mRight - mLeft),
                                mScrollY + (mBottom - mTop));
                    case VIRTUAL_VIEW_ID_DECREMENT:
                        return createAccessibilityNodeInfoForVirtualButton(
                                VIRTUAL_VIEW_ID_DECREMENT,
                                getVirtualDecrementButtonText(),
                                mScrollX,
                                mScrollY,
                                mScrollX + (mRight - mLeft),
                                mTopSelectionDividerTop + mSelectionDividerHeight);
                    case VIRTUAL_VIEW_ID_CENTER:
                        return createAccessibiltyNodeInfoForCenter(
                                mScrollX,
                                mTopSelectionDividerTop + mSelectionDividerHeight,
                                mScrollX + (mRight - mLeft),
                                mBottomSelectionDividerBottom - mSelectionDividerHeight);
                    case VIRTUAL_VIEW_ID_INCREMENT:
                        return createAccessibilityNodeInfoForVirtualButton(
                                VIRTUAL_VIEW_ID_INCREMENT,
                                getVirtualIncrementButtonText(),
                                mScrollX,
                                mBottomSelectionDividerBottom - mSelectionDividerHeight,
                                mScrollX + (mRight - mLeft),
                                mScrollY + (mBottom - mTop));
                }
            }

            AccessibilityNodeInfo info = super.createAccessibilityNodeInfo(virtualViewId);
            if (info == null) {
                return AccessibilityNodeInfo.obtain();
            }
            return info;
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();
            switch (virtualViewId) {
                case View.NO_ID: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_DECREMENT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_CENTER, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INCREMENT, result);
                    return result;
                }
                case VIRTUAL_VIEW_ID_DECREMENT:
                case VIRTUAL_VIEW_ID_CENTER:
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, virtualViewId, result);
                    return result;
                }
            }
            return super.findAccessibilityNodeInfosByText(searched, virtualViewId);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (mIsStartingAnimation) {
                return false;
            }

            final int mRight = mDelegator.getRight();
            final int mBottom = mDelegator.getBottom();

            switch (virtualViewId) {
                case View.NO_ID: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                SeslViewReflector.requestAccessibilityFocus(mDelegator);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                SeslViewReflector.clearAccessibilityFocus(mDelegator);
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
                            if (mDelegator.isEnabled()
                                    && (getWrapSelectorWheel() || getValue().compareTo(getMaxValue()) < 0)) {
                                startFadeAnimation(false);
                                changeValueByOne(true);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                            if (mDelegator.isEnabled()
                                    && (getWrapSelectorWheel() || getValue().compareTo(getMinValue()) > 0)) {
                                startFadeAnimation(false);
                                changeValueByOne(false);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                    }
                } break;
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (mDelegator.isEnabled()) {
                                startFadeAnimation(false);
                                changeValueByOne(false);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mDelegator.invalidate(0, 0, mRight, mTopSelectionDividerTop);
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mDelegator.invalidate(0, 0, mRight, mTopSelectionDividerTop);
                                return true;
                            }
                        } return false;
                    }
                } return false;
                case VIRTUAL_VIEW_ID_CENTER: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (mDelegator.isEnabled()) {
                                performClick();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mDelegator.invalidate(0, mTopSelectionDividerTop, mRight, mBottomSelectionDividerBottom);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mDelegator.invalidate(0, mTopSelectionDividerTop, mRight, mBottomSelectionDividerBottom);
                                return true;
                            }
                        } return false;
                    }
                } return false;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (mDelegator.isEnabled()) {
                                startFadeAnimation(false);
                                changeValueByOne(true);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mDelegator.invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mDelegator.invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                                return true;
                            }
                        } return false;
                    }
                } return false;
            }
            return super.performAction(virtualViewId, action, arguments);
        }

        void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
            switch (virtualViewId) {
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    if (hasVirtualDecrementButton()) {
                        sendAccessibilityEventForVirtualButton(
                                virtualViewId, eventType, getVirtualDecrementButtonText());
                    }
                } break;
                case VIRTUAL_VIEW_ID_CENTER: {
                    sendAccessibilityEventForCenter(eventType);
                } break;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    if (hasVirtualIncrementButton()) {
                        sendAccessibilityEventForVirtualButton(
                                virtualViewId, eventType, getVirtualIncrementButtonText());
                    }
                } break;
            }
        }

        private void sendAccessibilityEventForCenter(int eventType) {
            if (mAccessibilityManager.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                event.setPackageName(mContext.getPackageName());
                event.getText().add(getVirtualCurrentButtonText()
                        + mContext.getString(R.string.sesl_date_picker_switch_to_calendar_description));
                event.setEnabled(mDelegator.isEnabled());
                event.setSource(mDelegator, VIRTUAL_VIEW_ID_CENTER);
                mDelegator.requestSendAccessibilityEvent(mDelegator, event);
            }
        }

        private void sendAccessibilityEventForVirtualButton(int virtualViewId, int eventType, String text) {
            if (mAccessibilityManager.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                event.setClassName(Button.class.getName());
                event.setPackageName(mContext.getPackageName());
                event.getText().add(text);
                event.setEnabled(mDelegator.isEnabled());
                event.setSource(mDelegator, virtualViewId);
                mDelegator.requestSendAccessibilityEvent(mDelegator, event);
            }
        }

        private void findAccessibilityNodeInfosByTextInChild(String searchedLowerCase, int virtualViewId, List<AccessibilityNodeInfo> outResult) {
            switch (virtualViewId) {
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    String text = getVirtualDecrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_DECREMENT));
                    }
                } return;
                case VIRTUAL_VIEW_ID_CENTER: {
                    String text = getVirtualCurrentButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_CENTER));
                    }
                } return;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    String text = getVirtualIncrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INCREMENT));
                    }
                } return;
            }
        }

        private AccessibilityNodeInfo createAccessibiltyNodeInfoForCenter(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setPackageName(mContext.getPackageName());
            info.setSource(mDelegator, VIRTUAL_VIEW_ID_CENTER);
            info.setParent(mDelegator);
            info.setText(getVirtualCurrentButtonText()
                    + mContext.getString(R.string.sesl_date_picker_switch_to_calendar_description));
            info.setClickable(true);
            info.setEnabled(mDelegator.isEnabled());
            if (mAccessibilityFocusedView != VIRTUAL_VIEW_ID_CENTER) {
                info.setAccessibilityFocused(false);
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            } else {
                info.setAccessibilityFocused(true);
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(mDelegator.isVisibleToUserWrapper(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            mDelegator.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);
            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForVirtualButton(int virtualViewId, String text, int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(Button.class.getName());
            info.setPackageName(mContext.getPackageName());
            info.setSource(mDelegator, virtualViewId);
            info.setParent(mDelegator);
            info.setText(text);
            info.setClickable(true);
            info.setLongClickable(true);
            info.setEnabled(mDelegator.isEnabled());
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(mDelegator.isVisibleToUserWrapper(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            mDelegator.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);

            if (mAccessibilityFocusedView != virtualViewId) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            } else {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (mDelegator.isEnabled()) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForDatePickerWidget(
                int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(SeslSpinningDatePickerSpinner.class.getName());
            info.setPackageName(mContext.getPackageName());
            info.setSource(mDelegator);

            if (hasVirtualDecrementButton()) {
                info.addChild(mDelegator, VIRTUAL_VIEW_ID_DECREMENT);
            }
            info.addChild(mDelegator, VIRTUAL_VIEW_ID_CENTER);
            if (hasVirtualIncrementButton()) {
                info.addChild(mDelegator, VIRTUAL_VIEW_ID_INCREMENT);
            }

            info.setParent((View) mDelegator.getParentForAccessibility());
            info.setEnabled(mDelegator.isEnabled());
            info.setScrollable(true);

            final float applicationScale
                    = SeslCompatibilityInfoReflector.getField_applicationScale(mContext.getResources());

            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            scaleRect(boundsInParent, applicationScale);
            info.setBoundsInParent(boundsInParent);

            info.setVisibleToUser(mDelegator.isVisibleToUserWrapper());

            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            mDelegator.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            scaleRect(boundsInParent, applicationScale);
            info.setBoundsInScreen(boundsInScreen);

            if (mAccessibilityFocusedView != View.NO_ID) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            } else {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (mDelegator.isEnabled()) {
                if (getWrapSelectorWheel() || getValue().compareTo(getMaxValue()) < 0) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
                if (getWrapSelectorWheel() || getValue().compareTo(getMinValue()) > 0) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            }

            return info;
        }

        private void scaleRect(@NonNull Rect rect, float scale) {
            if (scale != 1.0f) {
                rect.left = (int) ((((float) rect.left) * scale) + 0.5f);
                rect.top = (int) ((((float) rect.top) * scale) + 0.5f);
                rect.right = (int) ((((float) rect.right) * scale) + 0.5f);
                rect.bottom = (int) ((((float) rect.bottom) * scale) + 0.5f);
            }
        }

        private boolean hasVirtualDecrementButton() {
            return getWrapSelectorWheel() || getValue().compareTo(getMinValue()) > 0;
        }

        private boolean hasVirtualIncrementButton() {
            return getWrapSelectorWheel() || getValue().compareTo(getMaxValue()) < 0;
        }

        private String getVirtualDecrementButtonText() {
            Calendar value = (Calendar) mValue.clone();
            value.add(Calendar.DAY_OF_MONTH, -1);
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value.compareTo(mMinValue) >= 0) {
                return mIsLunar
                        ? formatDateForLunarForAccessibility(value) :
                        formatDateForAccessibility(value) + ", "
                                + mPickerContentDescription + ", ";
            }
            return null;
        }

        private String getVirtualIncrementButtonText() {
            Calendar value = (Calendar) mValue.clone();
            value.add(Calendar.DAY_OF_MONTH, 1);
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value.compareTo(mMaxValue) <= 0) {
                return mIsLunar
                        ? formatDateForLunarForAccessibility(value) :
                        formatDateForAccessibility(value) + ", "
                                + mPickerContentDescription + ", ";
            }
            return null;
        }

        private String getVirtualCurrentButtonText() {
            Calendar value = (Calendar) mValue.clone();
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value.compareTo(mMaxValue) <= 0) {
                return mIsLunar
                        ? formatDateForLunarForAccessibility(value) :
                        formatDateForAccessibility(value) + ", "
                                + mPickerContentDescription + ", ";
            }
            return null;
        }
    }

    private void clearCalendar(Calendar oldCalendar, Calendar newCalendar) {
        oldCalendar.set(Calendar.YEAR, newCalendar.get(Calendar.YEAR));
        oldCalendar.set(Calendar.MONTH, newCalendar.get(Calendar.MONTH));
        oldCalendar.set(Calendar.DAY_OF_MONTH, newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    private static String formatDateWithLocale(Calendar value) {
        return new SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                .format(value.getTime());
    }

    private static String formatDateWithLocaleForAccessibility(Calendar value) {
        return new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(value.getTime());
    }

    private static String formatDayWithLocale(Calendar value) {
        return new SimpleDateFormat("d", Locale.getDefault())
                .format(value.getTime());
    }

    private static String getDayWithLocale(int value) {
        return String.format(Locale.getDefault(), "%d", value);
    }

    private static String formatMonthWithLocale(Calendar value) {
        return new SimpleDateFormat("MMM", Locale.getDefault())
                .format(value.getTime());
    }

    private static String formatMonthWithLocaleForAccessibility(Calendar value) {
        return new SimpleDateFormat("MMMM", Locale.getDefault())
                .format(value.getTime());
    }

    private String getMonthWithLocale(int value) {
        return mShortMonths[value];
    }

    private String getMonthWithLocaleForAccessibility(int value) {
        return mLongMonths[value];
    }

    private static String formatNumberWithLocale(int value) {
        return String.format(Locale.getDefault(), "%d", value);
    }

    private boolean isCharacterNumberLanguage() {
        final String language = Locale.getDefault().getLanguage();
        return "ar".equals(language) || "fa".equals(language) || "my".equals(language);
    }

    private boolean needCompareEqualMonthLanguage() {
        return "vi".equals(Locale.getDefault().getLanguage());
    }

    @Override
    public Calendar convertLunarToSolar(Calendar calendar, int year, int monthOfYear, int dayOfMonth) {
        Calendar newCalendar = (Calendar) calendar.clone();
        SeslSolarLunarConverterReflector
                .convertLunarToSolar(mPathClassLoader, mSolarLunarConverter, year, monthOfYear, dayOfMonth, mIsLeapMonth);
        newCalendar.set(SeslSolarLunarConverterReflector.getYear(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getMonth(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getDay(mPathClassLoader, mSolarLunarConverter));
        return newCalendar;
    }

    @Override
    public Calendar convertSolarToLunar(Calendar calendar, SeslSpinningDatePicker.LunarDate lunarDate) {
        Calendar newCalendar = (Calendar) calendar.clone();
        SeslSolarLunarConverterReflector
                .convertSolarToLunar(mPathClassLoader, mSolarLunarConverter,
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        newCalendar.set(SeslSolarLunarConverterReflector.getYear(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getMonth(mPathClassLoader, mSolarLunarConverter),
                SeslSolarLunarConverterReflector.getDay(mPathClassLoader, mSolarLunarConverter));
        if (lunarDate != null) {
            lunarDate.day = SeslSolarLunarConverterReflector.getDay(mPathClassLoader, mSolarLunarConverter);
            lunarDate.month = SeslSolarLunarConverterReflector.getMonth(mPathClassLoader, mSolarLunarConverter);
            lunarDate.year = SeslSolarLunarConverterReflector.getYear(mPathClassLoader, mSolarLunarConverter);
            lunarDate.isLeapMonth = SeslSolarLunarConverterReflector.isLeapMonth(mPathClassLoader, mSolarLunarConverter);
        }
        return newCalendar;
    }

    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        Calendar newCalendar = Calendar.getInstance(locale);
        if (oldCalendar != null) {
            newCalendar.setTimeInMillis(oldCalendar.getTimeInMillis());
        }
        newCalendar.set(Calendar.HOUR_OF_DAY, 12);
        newCalendar.set(Calendar.MINUTE, 0);
        newCalendar.set(Calendar.SECOND, 0);
        newCalendar.set(Calendar.MILLISECOND, 0);
        return newCalendar;
    }

    private boolean isHighContrastFontEnabled() {
        return SeslViewReflector.isHighContrastTextEnabled(mInputText);
    }
}
