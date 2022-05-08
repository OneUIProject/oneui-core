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

package com.google.android.material.tabs;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.animation.SeslAnimationUtils;
import androidx.appcompat.util.SeslMisc;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.google.android.material.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY)
public class SeslTabRoundRectIndicator extends SeslAbsIndicatorView {
  private static final int DURATION_PRESS = 50;
  private static final int DURATION_RELEASE = 350;
  private static final float SCALE_MINOR = 0.95f;

  private AnimationSet mPressAnimationSet;

  public SeslTabRoundRectIndicator(Context context) {
    this(context, null);
  }

  public SeslTabRoundRectIndicator(Context context,
                                   @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SeslTabRoundRectIndicator(Context context,
                                   @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public SeslTabRoundRectIndicator(Context context,
                                   @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    final boolean isLightTheme = SeslMisc.isLightTheme(context);
    ViewCompat.setBackground(this,
            ContextCompat.getDrawable(context, isLightTheme ?
                    R.drawable.sesl_tablayout_subtab_indicator_background_light
                    : R.drawable.sesl_tablayout_subtab_indicator_background_dark));
    onSetSelectedIndicatorColor(getResources().getColor(isLightTheme ?
            R.color.sesl_tablayout_subtab_background_stroke_color_light
            : R.color.sesl_tablayout_subtab_background_stroke_color_dark));
  }

  @Override
  protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);
    if (visibility != VISIBLE && !isSelected()) {
      onHide();
    }
  }

  @Override
  void onHide() {
    AlphaAnimation anim = new AlphaAnimation(0f, 0f);
    anim.setDuration(0);
    anim.setFillAfter(true);
    startAnimation(anim);
    setAlpha(0f);
  }

  @Override
  void onShow() {
    setAlpha(1f);
    AlphaAnimation anim = new AlphaAnimation(1f, 1f);
    anim.setDuration(0);
    anim.setFillAfter(true);
    startAnimation(anim);
  }

  @Override
  void startPressEffect() {
    setAlpha(1f);

    mPressAnimationSet = new AnimationSet(false);
    mPressAnimationSet.setStartOffset(50);
    mPressAnimationSet.setFillAfter(true);
    mPressAnimationSet.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        mPressAnimationSet = null;
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });

    ScaleAnimation scaleAnim
            = new ScaleAnimation(1.0f, SCALE_MINOR,
            1.0f, SCALE_MINOR,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
    scaleAnim.setDuration(DURATION_PRESS);
    scaleAnim.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_80);
    scaleAnim.setFillAfter(true);
    mPressAnimationSet.addAnimation(scaleAnim);

    if (!isSelected()) {
      AlphaAnimation alphaAnim = new AlphaAnimation(0f, 1f);
      alphaAnim.setDuration(DURATION_PRESS);
      alphaAnim.setFillAfter(true);
      alphaAnim.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_80);
      mPressAnimationSet.addAnimation(alphaAnim);
    }

    startAnimation(mPressAnimationSet);
  }

  @Override
  void startReleaseEffect() {
    setAlpha(1f);

    AnimationSet animSet = new AnimationSet(false);
    animSet.setFillAfter(true);

    ScaleAnimation scaleAnim
            = new ScaleAnimation(SCALE_MINOR, 1.0f,
            SCALE_MINOR, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
    scaleAnim.setDuration(DURATION_RELEASE);
    scaleAnim.setInterpolator(SeslAnimationUtils.SINE_IN_OUT_80);
    scaleAnim.setFillAfter(true);

    animSet.addAnimation(scaleAnim);
    startAnimation(animSet);
  }

  @Override
  void onSetSelectedIndicatorColor(int color) {
    if (!(getBackground() instanceof NinePatchDrawable)) {
      if (Build.VERSION.SDK_INT >= 22) {
        getBackground().setTint(color);
      } else {
        getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
      }
      if (!isSelected()) {
        setHide();
      }
    }
  }
}
