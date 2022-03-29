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
import android.content.res.Resources;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.picker.R;

import java.lang.reflect.Array;
import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

class SeslColorSwatchView extends View {
    private static String TAG = "SeslColorSwatchView";

    private static final float STROKE_WIDTH = 0.25f;
    private static final int ROUNDED_CORNER_RADIUS = 4;

    private static final int SWATCH_ITEM_ROW = 10;
    private static final int SWATCH_ITEM_COLUMN = 11;

    private static final float SWATCH_ITEM_SIZE_ROUNDING_VALUE = 4.5f;

    private static final int MAX_SWATCH_VIEW_ID = 110;

    Paint mBackgroundPaint;
    private GradientDrawable mCursorDrawable;
    private Point mCursorIndex;
    private Rect mCursorRect;
    private OnColorSwatchChangedListener mListener;
    private Resources mResources;
    private Rect mShadowRect;
    Paint mStrokePaint;
    private RectF mSwatchRect;
    private RectF mSwatchRectBackground;
    private SeslColorSwatchViewTouchHelper mTouchHelper;
    Paint shadow;

    private float[] corners;
    private float mSwatchItemHeight;
    private float mSwatchItemWidth;

    private int ROUNDED_CORNER_RADIUS_IN_Px = 0;
    private int currentCursorColor;
    private int mSelectedVirtualViewId = NO_ID;

    private boolean mFromUser = false;
    private boolean mIsColorInSwatch = true;

    private int[][] mColorSwatch = new int[][] {
            new int[] {-1, -3355444, -5000269, -6710887, -8224126, -10066330, -11711155, -13421773, -15066598, -16777216},
            new int[] {-22360, -38037, -49859, -60396, -65536, -393216, -2424832, -5767168, -10747904, -13434880},
            new int[] {-11096, -19093, -25544, -30705, -32768, -361216, -2396672, -5745664, -10736128, -13428224},
            new int[] {-88, -154, -200, -256, -328704, -329216, -2368768, -6053120, -10724352, -13421824},
            new int[] {-5701720, -10027162, -13041864, -16056566, -16711936, -16713216, -16721152, -16735488, -16753664, -16764160},
            new int[] {-5701685, -10027101, -13041784, -15728785, -16711834, -16714398, -16721064, -16735423, -16753627, -16764140},
            new int[] {-5701633, -10027009, -12713985, -16056321, -16711681, -16714251, -16720933, -16735325, -16753572, -16764109},
            new int[] {-5712641, -9718273, -13067009, -15430913, -16744193, -16744966, -16748837, -16755544, -16764575, -16770509},
            new int[] {-5723905, -9737217, -13092609, -16119041, -16776961, -16776966, -16776997, -16777048, -16777119, -16777165},
            new int[] {-3430145, -5870593, -7849729, -9498625, -10092289, -10223366, -11009829, -12386136, -14352292, -15466445},
            new int[] {-22273, -39169, -50945, -61441, -65281, -392966, -2424613, -5767000, -10420127, -13434829}
    };

    private int[][] mColorBrightness = new int[][] {
            new int[] {100, 80, 70, 60, 51, 40, 30, 20, 10, 0},
            new int[] {83, 71, 62, 54, 50, 49, 43, 33, 18, 10},
            new int[] {83, 71, 61, 53, 50, 49, 43, 33, 18, 10},
            new int[] {83, 70, 61, 50, 51, 49, 43, 32, 18, 10},
            new int[] {83, 70, 61, 52, 50, 49, 43, 32, 18, 10},
            new int[] {83, 70, 61, 53, 50, 48, 43, 32, 18, 10},
            new int[] {83, 70, 62, 52, 50, 48, 43, 32, 18, 10},
            new int[] {83, 71, 61, 54, 50, 49, 43, 33, 19, 10},
            new int[] {83, 71, 61, 52, 50, 49, 43, 33, 19, 10},
            new int[] {83, 71, 61, 53, 50, 49, 43, 33, 18, 10},
            new int[] {83, 70, 61, 53, 50, 49, 43, 33, 19, 10}
    };

    private StringBuilder[][] mColorSwatchDescription
            = (StringBuilder[][]) Array.newInstance(StringBuilder.class,
            SWATCH_ITEM_COLUMN, SWATCH_ITEM_ROW);

    interface OnColorSwatchChangedListener {
        void onColorSwatchChanged(int color);
    }

    void setOnColorSwatchChangedListener(OnColorSwatchChangedListener listener) {
        mListener = listener;
    }

    public SeslColorSwatchView(Context context) {
        this(context, null);
    }

    public SeslColorSwatchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeslColorSwatchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslColorSwatchView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mResources = context.getResources();

        initCursorDrawable();
        initAccessibility();

        mSwatchItemHeight
                = mResources.getDimension(R.dimen.sesl_color_picker_oneui_3_color_swatch_view_height) / SWATCH_ITEM_ROW;
        mSwatchItemWidth
                = mResources.getDimension(R.dimen.sesl_color_picker_oneui_3_color_swatch_view_width) / SWATCH_ITEM_COLUMN;

        mSwatchRect
                = new RectF(SWATCH_ITEM_SIZE_ROUNDING_VALUE,
                SWATCH_ITEM_SIZE_ROUNDING_VALUE,
                mResources.getDimensionPixelSize(
                        R.dimen.sesl_color_picker_oneui_3_color_swatch_view_width) + SWATCH_ITEM_SIZE_ROUNDING_VALUE,
                mResources.getDimensionPixelSize(
                        R.dimen.sesl_color_picker_oneui_3_color_swatch_view_height) + SWATCH_ITEM_SIZE_ROUNDING_VALUE);
        mSwatchRectBackground
                = new RectF(0.0f,
                0.0f,
                mResources.getDimensionPixelSize(
                        R.dimen.sesl_color_picker_oneui_3_color_swatch_view_width_background),
                mResources.getDimensionPixelSize(
                        R.dimen.sesl_color_picker_oneui_3_color_swatch_view_height_background));

        mCursorIndex = new Point(-1, -1);

        ROUNDED_CORNER_RADIUS_IN_Px = dpToPx(ROUNDED_CORNER_RADIUS);

        mStrokePaint = new Paint();
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(
                mResources.getColor(R.color.sesl_color_picker_stroke_color_swatchview));
        mStrokePaint.setStrokeWidth(STROKE_WIDTH);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(
                mResources.getColor(R.color.sesl_color_picker_transparent));

    }

    private void initAccessibility() {
        mTouchHelper = new SeslColorSwatchViewTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private void initCursorDrawable() {
        mCursorDrawable
                = (GradientDrawable) mResources.getDrawable(R.drawable.sesl_color_swatch_view_cursor);
        mCursorRect = new Rect();
        mShadowRect = new Rect();
        shadow = new Paint();
        shadow.setStyle(Paint.Style.FILL);
        shadow.setColor(mResources.getColor(R.color.sesl_color_picker_shadow));
        shadow.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();

        canvas.drawRoundRect(mSwatchRectBackground,
                ROUNDED_CORNER_RADIUS_IN_Px,
                ROUNDED_CORNER_RADIUS_IN_Px,
                mBackgroundPaint);

        for (int i = 0; i < SWATCH_ITEM_COLUMN; i++) {
            for (int j = 0; j < SWATCH_ITEM_ROW; j++) {
                paint.setColor(mColorSwatch[i][j]);
                if (i == 0 && j == 0) {
                    corners = new float[] {
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0
                    };

                    Path path = new Path();
                    path.addRoundRect((int) (i * mSwatchItemWidth + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (j * mSwatchItemHeight + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemWidth * (i + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemHeight * (j + 1)+ SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            corners,
                            Path.Direction.CW);
                    canvas.drawPath(path, paint);
                } else if (i == 0 && j == 9) {
                    corners = new float[] {
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            ROUNDED_CORNER_RADIUS_IN_Px
                    };

                    Path path = new Path();
                    path.addRoundRect((int) (i * mSwatchItemWidth + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (j * mSwatchItemHeight + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemWidth * (i + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemHeight * (j + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            corners,
                            Path.Direction.CW);
                    canvas.drawPath(path, paint);
                } else if (i == 10 && j == 0) {
                    corners = new float[] {
                            0,
                            0,
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            0,
                            0,
                            0,
                            0
                    };

                    Path path = new Path();
                    path.addRoundRect((int) (i * mSwatchItemWidth + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (j * mSwatchItemHeight + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemWidth * (i + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemHeight * (j + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            corners,
                            Path.Direction.CW);
                    canvas.drawPath(path, paint);
                } else if (i == 10 && j == 9) {
                    corners = new float[] {
                            0,
                            0,
                            0,
                            0,
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            ROUNDED_CORNER_RADIUS_IN_Px,
                            0,
                            0
                    };

                    Path path = new Path();
                    path.addRoundRect((int) (i * mSwatchItemWidth + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (j * mSwatchItemHeight + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemWidth * (i + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemHeight * (j + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            corners,
                            Path.Direction.CW);
                    canvas.drawPath(path, paint);
                } else {
                    canvas.drawRect((int) (i * mSwatchItemWidth + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (j * mSwatchItemHeight + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemWidth * (i + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            (int) (mSwatchItemHeight * (j + 1) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                            paint);
                }
            }
        }

        canvas.drawRoundRect(mSwatchRect,
                ROUNDED_CORNER_RADIUS_IN_Px,
                ROUNDED_CORNER_RADIUS_IN_Px,
                mStrokePaint);

        if (mIsColorInSwatch) {
            canvas.drawRect(mShadowRect, shadow);

            if (mCursorIndex.y == 8 || mCursorIndex.y == 9) {
                mCursorDrawable
                        = (GradientDrawable) mResources.getDrawable(R.drawable.sesl_color_swatch_view_cursor);
            } else {
                mCursorDrawable
                        = (GradientDrawable) mResources.getDrawable(R.drawable.sesl_color_swatch_view_cursor_gray);
            }
            mCursorDrawable.setColor(currentCursorColor);
            mCursorDrawable.setBounds(mCursorRect);
            mCursorDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE
                && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        if (setCursorIndexAt(event.getX(), event.getY())
                || !mIsColorInSwatch) {
            currentCursorColor = mColorSwatch[mCursorIndex.x][mCursorIndex.y];
            currentCursorColor = ColorUtils.setAlphaComponent(currentCursorColor, 255);
            setShadowRect(mShadowRect);
            setCursorRect(mCursorRect);
            setSelectedVirtualViewId();
            invalidate();
            if (mListener != null) {
                mListener.onColorSwatchChanged(mColorSwatch[mCursorIndex.x][mCursorIndex.y]);
            }
        }

        return true;
    }

    Point getCursorIndexAt(int color) {
        final int nonAlphaColor
                = Color.argb(255, (color >> 16) & 255, (color >> 8) & 255, color & 255);
        Point cursorIndex = new Point(-1, -1);

        mFromUser = false;
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 10; j++) {
                if (mColorSwatch[i][j] == nonAlphaColor) {
                    cursorIndex.set(i, j);
                    mFromUser = true;
                }
            }
        }

        mIsColorInSwatch = true;
        if (!mFromUser && !mCursorIndex.equals(-1, -1)) {
            mIsColorInSwatch = false;
            invalidate();
        }

        return cursorIndex;
    }

    StringBuilder getColorSwatchDescriptionAt(int color) {
        Point cursorIndex = getCursorIndexAt(color);

        if (!mFromUser) {
            return null;
        }

        if (mColorSwatchDescription[cursorIndex.x][cursorIndex.y]
                == null) {
            return mTouchHelper.getItemDescription(cursorIndex.x
                    + (cursorIndex.y * SWATCH_ITEM_COLUMN));
        }

        return mColorSwatchDescription[cursorIndex.x][cursorIndex.y];
    }

    private boolean setCursorIndexAt(float x, float y) {
        final float widthMax = mSwatchItemWidth * SWATCH_ITEM_COLUMN;
        final float heightMax = mSwatchItemHeight * SWATCH_ITEM_ROW;

        if (x >= widthMax) {
            x = widthMax - 1.0f;
        } else if (x < 0.0f) {
            x = 0.0f;
        }

        if (y >= heightMax) {
            y = heightMax - 1.0f;
        } else if (y < 0.0f) {
            y = 0.0f;
        }

        Point perCursor = new Point(mCursorIndex.x, mCursorIndex.y);
        mCursorIndex.set((int) (x / mSwatchItemWidth),
                (int) (y / mSwatchItemHeight));
        return !perCursor.equals(mCursorIndex);
    }

    private void setCursorIndexAt(int color) {
        Point cursorIndex = getCursorIndexAt(color);
        if (mFromUser) {
            mCursorIndex.set(cursorIndex.x, cursorIndex.y);
        }
    }

    private void setSelectedVirtualViewId() {
        mSelectedVirtualViewId = (mCursorIndex.y * SWATCH_ITEM_COLUMN)
                + mCursorIndex.x;
    }

    public void updateCursorPosition(int color) {
        setCursorIndexAt(color);
        if (mFromUser) {
            currentCursorColor = ColorUtils.setAlphaComponent(color, 255);
            setShadowRect(mShadowRect);
            setCursorRect(mCursorRect);
            invalidate();
            setSelectedVirtualViewId();
        } else {
            mSelectedVirtualViewId = NO_ID;
        }
    }

    private void setCursorRect(Rect rect) {
        rect.set((int) (((mCursorIndex.x - 0.05d) * mSwatchItemWidth) + 4.5d),
                (int) (((mCursorIndex.y - 0.05d) * mSwatchItemHeight) + 4.5d),
                (int) (((mCursorIndex.x + 1 + 0.05d) * mSwatchItemWidth) + 4.5d),
                (int) (((mCursorIndex.y + 1 + 0.05d) * mSwatchItemHeight) + 4.5d));
    }

    private void setShadowRect(Rect rect) {
        rect.set((int) ((mCursorIndex.x * mSwatchItemWidth) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                (int) ((mCursorIndex.y * mSwatchItemHeight) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                (int) (((mCursorIndex.x + 1 + 0.05d) * mSwatchItemWidth) + 4.5d),
                (int) (((mCursorIndex.y + 1 + 0.1d) * mSwatchItemHeight) + 4.5d));
    }

    private class SeslColorSwatchViewTouchHelper extends ExploreByTouchHelper {
        private final Rect mVirtualViewRect = new Rect();

        private int mVirtualCursorIndexX;
        private int mVirtualCursorIndexY;

        private String[][] mColorDescription = new String[][] {
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_white),
                        mResources.getString(R.string.sesl_color_picker_light_gray),
                        mResources.getString(R.string.sesl_color_picker_gray),
                        mResources.getString(R.string.sesl_color_picker_dark_gray),
                        mResources.getString(R.string.sesl_color_picker_black)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_red),
                        mResources.getString(R.string.sesl_color_picker_red),
                        mResources.getString(R.string.sesl_color_picker_dark_red)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_orange),
                        mResources.getString(R.string.sesl_color_picker_orange),
                        mResources.getString(R.string.sesl_color_picker_dark_orange)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_yellow),
                        mResources.getString(R.string.sesl_color_picker_yellow),
                        mResources.getString(R.string.sesl_color_picker_dark_yellow)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_green),
                        mResources.getString(R.string.sesl_color_picker_green),
                        mResources.getString(R.string.sesl_color_picker_dark_green)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_spring_green),
                        mResources.getString(R.string.sesl_color_picker_spring_green),
                        mResources.getString(R.string.sesl_color_picker_dark_spring_green)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_cyan),
                        mResources.getString(R.string.sesl_color_picker_cyan),
                        mResources.getString(R.string.sesl_color_picker_dark_cyan)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_azure),
                        mResources.getString(R.string.sesl_color_picker_azure),
                        mResources.getString(R.string.sesl_color_picker_dark_azure)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_blue),
                        mResources.getString(R.string.sesl_color_picker_blue),
                        mResources.getString(R.string.sesl_color_picker_dark_blue)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_violet),
                        mResources.getString(R.string.sesl_color_picker_violet),
                        mResources.getString(R.string.sesl_color_picker_dark_violet)
                },
                new String[] {
                        mResources.getString(R.string.sesl_color_picker_light_magenta),
                        mResources.getString(R.string.sesl_color_picker_magenta),
                        mResources.getString(R.string.sesl_color_picker_dark_magenta)
                }
        };

        SeslColorSwatchViewTouchHelper(View host) {
            super(host);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            setVirtualCursorIndexAt(x, y);
            return getFocusedVirtualViewId();
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int i = 0; i < MAX_SWATCH_VIEW_ID; i++) {
                virtualViewIds.add(i);
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId,
                                                     AccessibilityEvent event) {
            event.setContentDescription(getItemDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId,
                                                    AccessibilityNodeInfoCompat node) {
            setVirtualCursorIndexAt(virtualViewId);
            setVirtualCursorRect(mVirtualViewRect);

            node.setContentDescription(getItemDescription(virtualViewId));
            node.setBoundsInParent(mVirtualViewRect);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            node.setClassName(Button.class.getName());

            if (mSelectedVirtualViewId != NO_ID
                    && virtualViewId == mSelectedVirtualViewId) {
                node.addAction(AccessibilityNodeInfoCompat.ACTION_SELECT);
                node.setClickable(true);
                node.setCheckable(true);
                node.setChecked(true);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                setVirtualCursorIndexAt(virtualViewId);
                onVirtualViewClick(mColorSwatch[mVirtualCursorIndexX][mVirtualCursorIndexY]);
            }
            return false;
        }

        private int getFocusedVirtualViewId() {
            return mVirtualCursorIndexX + (mVirtualCursorIndexY * SWATCH_ITEM_COLUMN);
        }

        private void setVirtualCursorIndexAt(float x, float y) {
            final float widthMax = mSwatchItemWidth * SWATCH_ITEM_COLUMN;
            final float heightMax = mSwatchItemHeight * SWATCH_ITEM_ROW;

            if (x >= widthMax) {
                x = widthMax - 1.0f;
            } else if (x < 0.0f) {
                x = 0.0f;
            }
            if (y >= heightMax) {
                y = heightMax - 1.0f;
            } else if (y < 0.0f) {
                y = 0.0f;
            }

            mVirtualCursorIndexX = (int) (x / mSwatchItemWidth);
            mVirtualCursorIndexY = (int) (y / mSwatchItemHeight);
        }

        private void setVirtualCursorIndexAt(int id) {
            mVirtualCursorIndexX = id % SWATCH_ITEM_COLUMN;
            mVirtualCursorIndexY = id / SWATCH_ITEM_COLUMN;
        }

        private void setVirtualCursorRect(Rect rect) {
            rect.set((int) ((mVirtualCursorIndexX * mSwatchItemWidth) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                    (int) ((mVirtualCursorIndexY * mSwatchItemHeight) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                    (int) (((mVirtualCursorIndexX + 1) * mSwatchItemWidth) + SWATCH_ITEM_SIZE_ROUNDING_VALUE),
                    (int) (((mVirtualCursorIndexY + 1) * mSwatchItemHeight) + SWATCH_ITEM_SIZE_ROUNDING_VALUE));
        }

        private StringBuilder getItemDescription(int id) {
            setVirtualCursorIndexAt(id);

            if (mColorSwatchDescription[mVirtualCursorIndexX][mVirtualCursorIndexY] == null) {
                StringBuilder description = new StringBuilder();

                if (mVirtualCursorIndexX == 0) {
                    if (mVirtualCursorIndexY == 0) {
                        description.append(mColorDescription[mVirtualCursorIndexX][0]);
                    } else if (mVirtualCursorIndexY < 3) {
                        description.append(mColorDescription[mVirtualCursorIndexX][1]);
                    } else if (mVirtualCursorIndexY < 6) {
                        description.append(mColorDescription[mVirtualCursorIndexX][2]);
                    } else if (mVirtualCursorIndexY < 9) {
                        description.append(mColorDescription[mVirtualCursorIndexX][3]);
                    } else {
                        description.append(mColorDescription[mVirtualCursorIndexX][4]);
                    }
                } else {
                    if (mVirtualCursorIndexY < 3) {
                        description.append(mColorDescription[mVirtualCursorIndexX][0]);
                    } else if (mVirtualCursorIndexY < 6) {
                        description.append(mColorDescription[mVirtualCursorIndexX][1]);
                    } else {
                        description.append(mColorDescription[mVirtualCursorIndexX][2]);
                    }
                }

                if (mVirtualCursorIndexX != 3 || mVirtualCursorIndexY != 3) {
                    if (mVirtualCursorIndexX == 0
                            && mVirtualCursorIndexY == 4) {
                        description.append(", ")
                                .append(mColorBrightness[mVirtualCursorIndexX][mVirtualCursorIndexY]);
                    } else if (mVirtualCursorIndexY != 4) {
                        description.append(", ")
                                .append(mColorBrightness[mVirtualCursorIndexX][mVirtualCursorIndexY]);
                    }
                }

                mColorSwatchDescription[mVirtualCursorIndexX][mVirtualCursorIndexY] = description;
            }

            return mColorSwatchDescription[mVirtualCursorIndexX][mVirtualCursorIndexY];
        }

        private void onVirtualViewClick(int color) {
            if (mListener != null) {
                mListener.onColorSwatchChanged(color);
            }
            mTouchHelper.sendEventForVirtualView(mSelectedVirtualViewId,
                    AccessibilityEvent.TYPE_VIEW_CLICKED);
        }
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return mTouchHelper.dispatchHoverEvent(event)
                || super.dispatchHoverEvent(event);
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
