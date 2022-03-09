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
import static androidx.appcompat.widget.ThemeUtils.getDisabledThemeAttrColor;
import static androidx.appcompat.widget.ThemeUtils.getThemeAttrColor;
import static androidx.appcompat.widget.ThemeUtils.getThemeAttrColorStateList;
import static androidx.core.graphics.ColorUtils.compositeColors;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class AppCompatDrawableManager {
    private static final String TAG = "AppCompatDrawableManag";
    private static final boolean DEBUG = false;
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

    private static AppCompatDrawableManager INSTANCE;

    public static synchronized void preload() {
        if (INSTANCE == null) {
            INSTANCE = new AppCompatDrawableManager();
            INSTANCE.mResourceManager = ResourceManagerInternal.get();
            INSTANCE.mResourceManager.setHooks(new ResourceManagerInternal.ResourceManagerHooks() {
                /**
                 * Drawables which should be tinted using a state list containing values of
                 * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated}
                 * for the checked state.
                 */
                private final int[] TINT_CHECKABLE_BUTTON_LIST = {
                        R.drawable.abc_btn_check_material_anim,
                        R.drawable.abc_btn_radio_material_anim
                };

                private ColorStateList createDefaultButtonColorStateList(@NonNull Context context) {
                    return createButtonColorStateList(context,
                            getThemeAttrColor(context, R.attr.colorButtonNormal));
                }

                private ColorStateList createBorderlessButtonColorStateList(
                        @NonNull Context context) {
                    // We ignore the custom tint for borderless buttons
                    return createButtonColorStateList(context, Color.TRANSPARENT);
                }

                private ColorStateList createColoredButtonColorStateList(
                        @NonNull Context context) {
                    return createButtonColorStateList(context,
                            getThemeAttrColor(context, R.attr.colorAccent));
                }

                private ColorStateList createButtonColorStateList(@NonNull final Context context,
                        @ColorInt final int baseColor) {
                    final int[][] states = new int[4][];
                    final int[] colors = new int[4];
                    int i = 0;

                    final int colorControlHighlight = getThemeAttrColor(context,
                            R.attr.colorControlHighlight);
                    final int disabledColor = getDisabledThemeAttrColor(context,
                            R.attr.colorButtonNormal);

                    // Disabled state
                    states[i] = ThemeUtils.DISABLED_STATE_SET;
                    colors[i] = disabledColor;
                    i++;

                    states[i] = ThemeUtils.PRESSED_STATE_SET;
                    colors[i] = compositeColors(colorControlHighlight, baseColor);
                    i++;

                    states[i] = ThemeUtils.FOCUSED_STATE_SET;
                    colors[i] = compositeColors(colorControlHighlight, baseColor);
                    i++;

                    // Default enabled state
                    states[i] = ThemeUtils.EMPTY_STATE_SET;
                    colors[i] = baseColor;
                    i++;

                    return new ColorStateList(states, colors);
                }

                private ColorStateList createSwitchThumbColorStateList(Context context) {
                    final int[][] states = new int[3][];
                    final int[] colors = new int[3];
                    int i = 0;

                    final ColorStateList thumbColor = getThemeAttrColorStateList(context,
                            R.attr.colorSwitchThumbNormal);

                    if (thumbColor != null && thumbColor.isStateful()) {
                        // If colorSwitchThumbNormal is a valid ColorStateList, extract the
                        // default and disabled colors from it

                        // Disabled state
                        states[i] = ThemeUtils.DISABLED_STATE_SET;
                        colors[i] = thumbColor.getColorForState(states[i], 0);
                        i++;

                        states[i] = ThemeUtils.CHECKED_STATE_SET;
                        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
                        i++;

                        // Default enabled state
                        states[i] = ThemeUtils.EMPTY_STATE_SET;
                        colors[i] = thumbColor.getDefaultColor();
                        i++;
                    } else {
                        // Else we'll use an approximation using the default disabled alpha

                        // Disabled state
                        states[i] = ThemeUtils.DISABLED_STATE_SET;
                        colors[i] = getDisabledThemeAttrColor(context,
                                R.attr.colorSwitchThumbNormal);
                        i++;

                        states[i] = ThemeUtils.CHECKED_STATE_SET;
                        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
                        i++;

                        // Default enabled state
                        states[i] = ThemeUtils.EMPTY_STATE_SET;
                        colors[i] = getThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
                        i++;
                    }

                    return new ColorStateList(states, colors);
                }

                @Override
                public Drawable createDrawableFor(@NonNull ResourceManagerInternal resourceManager,
                        @NonNull Context context, int resId) {
                    return null;
                }

                private LayerDrawable getRatingBarLayerDrawable(
                        @NonNull ResourceManagerInternal resourceManager,
                        @NonNull Context context, @DimenRes int dimenResId) {
                    int starSize = context.getResources().getDimensionPixelSize(dimenResId);

                    Drawable star = resourceManager.getDrawable(context,
                            R.drawable.abc_star_black_48dp);
                    Drawable halfStar = resourceManager.getDrawable(context,
                            R.drawable.abc_star_half_black_48dp);

                    BitmapDrawable starBitmapDrawable;
                    BitmapDrawable tiledStarBitmapDrawable;
                    if ((star instanceof BitmapDrawable) && (star.getIntrinsicWidth() == starSize)
                            && (star.getIntrinsicHeight() == starSize)) {
                        // no need for extra conversion
                        starBitmapDrawable = (BitmapDrawable) star;

                        tiledStarBitmapDrawable =
                                new BitmapDrawable(starBitmapDrawable.getBitmap());
                    } else {
                        Bitmap bitmapStar = Bitmap.createBitmap(starSize, starSize,
                                Bitmap.Config.ARGB_8888);
                        Canvas canvasStar = new Canvas(bitmapStar);
                        star.setBounds(0, 0, starSize, starSize);
                        star.draw(canvasStar);
                        starBitmapDrawable = new BitmapDrawable(bitmapStar);

                        tiledStarBitmapDrawable = new BitmapDrawable(bitmapStar);
                    }
                    tiledStarBitmapDrawable.setTileModeX(Shader.TileMode.REPEAT);

                    BitmapDrawable halfStarBitmapDrawable;
                    if ((halfStar instanceof BitmapDrawable)
                            && (halfStar.getIntrinsicWidth() == starSize)
                            && (halfStar.getIntrinsicHeight() == starSize)) {
                        // no need for extra conversion
                        halfStarBitmapDrawable = (BitmapDrawable) halfStar;
                    } else {
                        Bitmap bitmapHalfStar = Bitmap.createBitmap(starSize, starSize,
                                Bitmap.Config.ARGB_8888);
                        Canvas canvasHalfStar = new Canvas(bitmapHalfStar);
                        halfStar.setBounds(0, 0, starSize, starSize);
                        halfStar.draw(canvasHalfStar);
                        halfStarBitmapDrawable = new BitmapDrawable(bitmapHalfStar);
                    }

                    LayerDrawable result = new LayerDrawable(new Drawable[]{
                            starBitmapDrawable, halfStarBitmapDrawable, tiledStarBitmapDrawable
                    });
                    result.setId(0, android.R.id.background);
                    result.setId(1, android.R.id.secondaryProgress);
                    result.setId(2, android.R.id.progress);
                    return result;
                }

                private void setPorterDuffColorFilter(Drawable d, int color, PorterDuff.Mode mode) {
                    if (DrawableUtils.canSafelyMutateDrawable(d)) {
                        d = d.mutate();
                    }
                    d.setColorFilter(getPorterDuffColorFilter(color, mode == null ? DEFAULT_MODE
                            : mode));
                }

                @Override
                public boolean tintDrawable(@NonNull Context context, int resId,
                        @NonNull Drawable drawable) {
                    return false;
                }

                private boolean arrayContains(int[] array, int value) {
                    for (int id : array) {
                        if (id == value) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public ColorStateList getTintListForDrawableRes(@NonNull Context context,
                        int resId) {
                    return null;
                }

                @Override
                public boolean tintDrawableUsingColorFilter(@NonNull Context context,
                        int resId, @NonNull Drawable drawable) {
                    PorterDuff.Mode tintMode = DEFAULT_MODE;
                    return false;
                }

                @Override
                public PorterDuff.Mode getTintModeForDrawableRes(int resId) {
                    return null;
                }
            });
        }
    }

    /**
     * Returns the singleton instance of this class.
     */
    public static synchronized AppCompatDrawableManager get() {
        if (INSTANCE == null) {
            preload();
        }
        return INSTANCE;
    }

    private ResourceManagerInternal mResourceManager;

    public synchronized Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        return mResourceManager.getDrawable(context, resId);
    }

    synchronized Drawable getDrawable(@NonNull Context context, @DrawableRes int resId,
            boolean failIfNotKnown) {
        return mResourceManager.getDrawable(context, resId, failIfNotKnown);
    }

    public synchronized void onConfigurationChanged(@NonNull Context context) {
        mResourceManager.onConfigurationChanged(context);
    }

    synchronized Drawable onDrawableLoadedFromResources(@NonNull Context context,
            @NonNull VectorEnabledTintResources resources, @DrawableRes final int resId) {
        return mResourceManager.onDrawableLoadedFromResources(context, resources, resId);
    }

    boolean tintDrawableUsingColorFilter(@NonNull Context context,
            @DrawableRes final int resId, @NonNull Drawable drawable) {
        return mResourceManager.tintDrawableUsingColorFilter(context, resId, drawable);
    }

    synchronized ColorStateList getTintList(@NonNull Context context, @DrawableRes int resId) {
        return mResourceManager.getTintList(context, resId);
    }

    static void tintDrawable(Drawable drawable, TintInfo tint, int[] state) {
        ResourceManagerInternal.tintDrawable(drawable, tint, state);
    }

    public static synchronized PorterDuffColorFilter getPorterDuffColorFilter(
            int color, PorterDuff.Mode mode) {
        return ResourceManagerInternal.getPorterDuffColorFilter(color, mode);
    }
}