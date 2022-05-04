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

package androidx.apppickerview.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.util.SeslRoundedCorner;
import androidx.appcompat.util.SeslSubheaderRoundedCorner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.apppickerview.R;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class AppPickerView extends RecyclerView
        implements RecyclerView.RecyclerListener {
    private static final String TAG = "AppPickerView";
    private static final boolean DEBUG = false;

    public static final String ALL_APPS_STRING = "all_apps";
    public static final String KEY_APP_SEPARATOR = "app_picker_separator";

    public static final int ORDER_NONE = 0;
    public static final int ORDER_ASCENDING = 1;
    public static final int ORDER_ASCENDING_IGNORE_CASE = 2;
    public static final int ORDER_DESCENDING = 3;
    public static final int ORDER_DESCENDING_IGNORE_CASE = 4;

    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            ORDER_NONE,
            ORDER_ASCENDING,
            ORDER_ASCENDING_IGNORE_CASE,
            ORDER_DESCENDING,
            ORDER_DESCENDING_IGNORE_CASE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AppPickerOrder {
    }

    public static final int TYPE_LIST = 0;
    public static final int TYPE_LIST_ACTION_BUTTON = 1;
    public static final int TYPE_LIST_CHECKBOX = 2;
    public static final int TYPE_LIST_CHECKBOX_WITH_ALL_APPS = 3;
    public static final int TYPE_LIST_RADIOBUTTON = 4;
    public static final int TYPE_LIST_SWITCH = 5;
    public static final int TYPE_LIST_SWITCH_WITH_ALL_APPS = 6;
    public static final int TYPE_GRID = 7;
    public static final int TYPE_GRID_CHECKBOX = 8;

    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            TYPE_LIST,
            TYPE_LIST_ACTION_BUTTON,
            TYPE_LIST_CHECKBOX,
            TYPE_LIST_CHECKBOX_WITH_ALL_APPS,
            TYPE_LIST_RADIOBUTTON,
            TYPE_LIST_SWITCH,
            TYPE_LIST_SWITCH_WITH_ALL_APPS,
            TYPE_GRID,
            TYPE_GRID_CHECKBOX
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AppPickerType {
    }

    private AbsAdapter mAdapter;
    private AppPickerIconLoader mAppPickerIconLoader;
    private Context mContext;
    private RecyclerView.ItemDecoration mGridSpacingDecoration;
    private SeslSubheaderRoundedCorner mRoundedCorner;
    public ArrayList<Integer> mSeparators;

    private int mOrder;
    private int mSpanCount = 4;
    private int mType;

    public interface OnBindListener {
        void onBindViewHolder(ViewHolder holder, int position, String packageName);
    }

    public interface OnSearchFilterListener {
        void onSearchFilterCompleted(int itemCount);
    }

    public AppPickerView(@NonNull Context context) {
        this(context, null);
    }

    public AppPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppPickerView(@NonNull Context context, @Nullable AttributeSet attrs,
                         int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setRecyclerListener(this);
        mAppPickerIconLoader = new AppPickerIconLoader(mContext);
    }

    public void setAppPickerView(@AppPickerType int type) {
        setAppPickerView(type, null, ORDER_ASCENDING_IGNORE_CASE, null);
    }

    public void setAppPickerView(@AppPickerType int type,  @AppPickerOrder int order) {
        setAppPickerView(type, null, order, null);
    }

    public void setAppPickerView(@AppPickerType int type, List<String> packageNamesList) {
        setAppPickerView(type, packageNamesList, ORDER_ASCENDING_IGNORE_CASE, null);
    }

    public void setAppPickerView(@AppPickerType int type, List<String> packageNamesList,
                                 List<AppLabelInfo> labelInfoList) {
        setAppPickerView(type, packageNamesList, ORDER_ASCENDING_IGNORE_CASE, labelInfoList);
    }

    public void setAppPickerView(@AppPickerType int type, List<String> packageNamesList,
                                 @AppPickerOrder int order) {
        setAppPickerView(type, packageNamesList, order, null);
    }

    public void setAppPickerView(@AppPickerType int type, List<String> packageNamesList,
                                 @AppPickerOrder int order, List<AppLabelInfo> labelInfoList) {
        setAppPickerView(type, packageNamesList, order, labelInfoList, null);
    }

    public void setAppPickerView(List<ComponentName> activityNamesList, @AppPickerType int type) {
        setAppPickerView(type, null, ORDER_ASCENDING_IGNORE_CASE, null, activityNamesList);
    }

    public void setAppPickerView(@AppPickerType int type, @AppPickerOrder int order,
                                 List<ComponentName> activityNamesList) {
        setAppPickerView(type, null, order, null, activityNamesList);
    }

    public void setAppPickerView(@AppPickerType int type, @AppPickerOrder int order,
                                 List<AppLabelInfo> labelInfoList,
                                 List<ComponentName> activityNamesList) {
        setAppPickerView(type, null, order, labelInfoList, activityNamesList);
    }

    private void setAppPickerView(@AppPickerType int type, List<String> packageNamesList,
                                  @AppPickerOrder int order, List<AppLabelInfo> labelInfoList,
                                  List<ComponentName> activityNamesList) {
        TypedValue outValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.roundedCornerColor, outValue, true);

        mRoundedCorner = new SeslSubheaderRoundedCorner(mContext);
        mRoundedCorner.setRoundedCorners(SeslRoundedCorner.ROUNDED_CORNER_ALL);
        if (outValue.resourceId > 0) {
            mRoundedCorner.setRoundedCornerColor(SeslRoundedCorner.ROUNDED_CORNER_ALL,
                    getResources().getColor(outValue.resourceId, null));
        }

        if (packageNamesList == null && activityNamesList == null) {
            packageNamesList = getInstalledPackages(mContext);
        }

        mType = type;
        mOrder = order;
        mAdapter = AbsAdapter.getAppPickerAdapter(
                mContext, packageNamesList, type, order, labelInfoList, mAppPickerIconLoader, activityNamesList);

        switch (mType) {
            case TYPE_LIST:
            case TYPE_LIST_ACTION_BUTTON:
            case TYPE_LIST_CHECKBOX:
            case TYPE_LIST_CHECKBOX_WITH_ALL_APPS:
            case TYPE_LIST_RADIOBUTTON:
            case TYPE_LIST_SWITCH:
            case TYPE_LIST_SWITCH_WITH_ALL_APPS:
                addItemDecoration(new ListDividerItemDecoration(mContext, mType));
                break;
            case TYPE_GRID:
            case TYPE_GRID_CHECKBOX:
                if (mGridSpacingDecoration == null) {
                    mGridSpacingDecoration
                            = new GridSpacingItemDecoration(mSpanCount, 8, true);
                    addItemDecoration(mGridSpacingDecoration);
                }
                break;
        }

        setLayoutManager(getLayoutManager(type));
        setAdapter(mAdapter);
        seslSetGoToTopEnabled(true);
        seslSetFastScrollerEnabled(true);
        seslSetFillBottomEnabled(true);

        mSeparators = new ArrayList<>();
    }

    public int getType() {
        return mType;
    }

    public void setOnBindListener(@NonNull OnBindListener listener) {
        if (mAdapter != null) {
            mAdapter.setOnBindListener(listener);
        }
    }

    public AppLabelInfo getAppLabelInfo(int position) {
        if (mAdapter != null) {
            return mAdapter.getAppInfo(position);
        }
        return null;
    }

    public static List<AppLabelInfo> getInstalledPackagesWithLabel(Context context) {
        return DataManager.resetPackages(context, getInstalledPackages(context));
    }

    public static List<AppLabelInfo> getAppLabelinfoList(Context context,
                                                         List<String> packageNamesList) {
        return DataManager.resetPackages(context, packageNamesList);
    }

    public static List<AppLabelInfo> getAppLabelinfoList(List<ComponentName> activityNamesList,
                                                         Context context) {
        return DataManager.resetPackages(activityNamesList, context);
    }

    public static List<String> getInstalledPackages(Context context) {
        List<ApplicationInfo> installedApplications
                = context.getPackageManager().getInstalledApplications(0);
        ArrayList<String> packagesList = new ArrayList<>();
        for (ApplicationInfo appInfo : installedApplications) {
            packagesList.add(appInfo.packageName);
        }
        return packagesList;
    }

    public int getAppLabelOrder() {
        return mOrder;
    }

    public void setAppLabelOrder(@AppPickerOrder int order) {
        mOrder = order;
        mAdapter.setOrder(order);
    }

    public void addPackage(int position, String label) {
        mAdapter.addPackage(position, label);
    }

    public void addSeparator(int position) {
        mSeparators.add(Integer.valueOf(position));
        Collections.sort(mSeparators);
        mAdapter.addSeparator(position);

        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager != null && layoutManager instanceof GridLayoutManager) {
            ((GridLayoutManager) layoutManager)
                    .setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            if (position < mAdapter.getDataSet().size()
                                    && mAdapter.getDataSet().get(position).isSeparator()) {
                                return mSpanCount;
                            }
                            return 1;
                        }
                    });
        }
    }

    public void resetPackages(List<String> packageNamesList) {
        mAdapter.resetPackages(packageNamesList, true, null, null);
    }

    public void resetPackages(List<String> packageNamesList, List<AppLabelInfo> labelInfoList) {
        mAdapter.resetPackages(packageNamesList, true, labelInfoList, null);
    }

    public void resetComponentName(List<ComponentName> activityNamesList) {
        mAdapter.resetPackages(null, true, null, activityNamesList);
    }

    public void resetComponentName(List<ComponentName> activityNamesList,
                                   List<AppLabelInfo> labelInfoList) {
        mAdapter.resetPackages(null, true, labelInfoList, activityNamesList);
    }

    public void setSearchFilter(String constraint) {
        setSearchFilter(constraint, null);
    }

    public void setSearchFilter(String constraint, OnSearchFilterListener listener) {
        if (listener != null) {
            mAdapter.setOnSearchFilterListener(listener);
        }
        mAdapter.getFilter().filter(constraint);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        ViewHolder vh = (ViewHolder) holder;

        ImageButton actionButton = vh.getActionButton();
        if (actionButton != null && actionButton.hasOnClickListeners()) {
            actionButton.setOnClickListener(null);
        }

        ImageView appIcon = vh.getAppIcon();
        if (appIcon != null && appIcon.hasOnClickListeners()) {
            appIcon.setOnClickListener(null);
        }

        CheckBox checkBox = vh.getCheckBox();
        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener(null);
        }

        View itemView = vh.getItem();
        if (itemView != null && itemView.hasOnClickListeners()) {
            itemView.setOnClickListener(null);
        }

        SwitchCompat switchCompat = vh.getSwitch();
        if (switchCompat != null) {
            switchCompat.setOnCheckedChangeListener(null);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAppPickerIconLoader.startIconLoaderThread();
    }

    @Override
    protected void onDetachedFromWindow() {
        mAppPickerIconLoader.stopIconLoaderThread();
        super.onDetachedFromWindow();
    }

    private RecyclerView.LayoutManager getLayoutManager(int type) {
        switch (type) {
            case TYPE_GRID:
            case TYPE_GRID_CHECKBOX:
                return new GridLayoutManager(mContext, mSpanCount);
            default:
                return new LinearLayoutManager(mContext);
        }
    }

    private class ListDividerItemDecoration extends RecyclerView.ItemDecoration {
        private int mType;
        private Drawable mDivider;
        private int mDividerLeft;

        public ListDividerItemDecoration(Context context, int type) {
            mType = type;

            TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});
            mDivider = a.getDrawable(0);
            a.recycle();

            mDividerLeft = (int) getResources()
                    .getDimension(R.dimen.app_picker_list_icon_frame_width);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);

            for (int i = 0; i < parent.getChildCount() - 1; i++) {
                View child = parent.getChildAt(i);
                if (!(parent.getChildViewHolder(child) instanceof SeparatorViewHolder)) {
                    final int left = i == 0 && mType == TYPE_LIST_SWITCH_WITH_ALL_APPS
                            ? parent.getPaddingLeft() : mDividerLeft;
                    final int top = child.getBottom()
                            + ((RecyclerView.LayoutParams) child.getLayoutParams()).bottomMargin;
                    final int right = parent.getWidth() - parent.getPaddingRight();
                    final int bottom = mDivider.getIntrinsicHeight() + top;

                    mDivider.setBounds(left, top, right, bottom);
                    mDivider.draw(c);
                }
            }
        }

        @Override
        public void seslOnDispatchDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.seslOnDispatchDraw(c, parent, state);
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (parent.getChildViewHolder(child) instanceof SeparatorViewHolder) {
                    mRoundedCorner.drawRoundedCorner(child, c);
                }
            }
        }
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private float density;
        private boolean includeEdge;
        private int spacing;
        private int spacingTop;
        private int spanCount;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            density = Resources.getSystem().getDisplayMetrics().density;
            this.spacing = (int) (spacing * density);
            spacingTop = (int) (density * 12.0f);
            this.includeEdge = includeEdge;
        }

        // TODO rework this method
        // kang
        @Override
        public void getItemOffsets(Rect var1, View var2, RecyclerView var3, State var4) {
            /* var1 = outRect; var2 = view; var3 = parent; var4 = state; */

            int var5 = var3.getChildAdapterPosition(var2);
            int var6 = var5 % this.spanCount;
            Iterator var10 = mSeparators.iterator();
            int var7 = -1;

            int var8;
            int var9;
            while(true) {
                var8 = var7;
                var7 = var7;
                if (!var10.hasNext()) {
                    break;
                }

                var9 = (Integer)var10.next();
                var7 = var9;
                if (var9 >= var5) {
                    var7 = var9;
                    break;
                }
            }

            if (this.includeEdge) {
                if (var5 == var7) {
                    return;
                }

                var9 = this.spacing;
                var7 = this.spanCount;
                var1.left = var9 - var6 * var9 / var7;
                var1.right = (var6 + 1) * var9 / var7;
                if (var8 != -1) {
                    if (var5 - var8 - 1 < var7) {
                        var1.top = this.spacingTop;
                    }
                } else if (var5 < var7) {
                    var1.top = this.spacingTop;
                }

                var1.bottom = this.spacingTop;
            } else {
                var7 = this.spacing;
                var8 = this.spanCount;
                var1.left = var6 * var7 / var8;
                var1.right = var7 - (var6 + 1) * var7 / var8;
            }
        }
        // kang

        @Override
        public void seslOnDispatchDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.seslOnDispatchDraw(c, parent, state);
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (parent.getChildViewHolder(child) instanceof SeparatorViewHolder) {
                    mRoundedCorner.drawRoundedCorner(child, c);
                }
            }
        }
    }

    public void refreshUI() {
        post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run refreshUI");
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
            }
        });
    }

    public void refreshUI(final int position) {
        post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run refreshUI by position");
                mAdapter.notifyItemChanged(position);
            }
        });
    }

    public void refresh() {
        post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run refresh");
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public void setGridSpanCount(int spanCount) {
        mSpanCount = spanCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton mActionButton;
        private final ImageView mAppIcon;
        private final ViewGroup mAppIconContainer;
        private final TextView mAppName;
        private final CheckBox mCheckBox;
        private final ViewGroup mLeftContainer;
        private final RadioButton mRadioButton;
        private final TextView mSummary;
        private final SwitchCompat mSwitch;
        private final ViewGroup mTitleContainer;
        private final ViewGroup mWidgetContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            mAppName = itemView.findViewById(R.id.title);
            mAppIcon = itemView.findViewById(R.id.icon);
            mAppIconContainer = itemView.findViewById(R.id.icon_frame);
            mTitleContainer = itemView.findViewById(R.id.title_frame);
            mSummary = itemView.findViewById(R.id.summary);
            mLeftContainer = itemView.findViewById(R.id.left_frame);
            mCheckBox = itemView.findViewById(R.id.check_widget);
            mRadioButton = itemView.findViewById(R.id.radio_widget);
            mWidgetContainer = itemView.findViewById(R.id.widget_frame);
            mSwitch = itemView.findViewById(R.id.switch_widget);
            mActionButton = itemView.findViewById(R.id.image_button);
        }

        public TextView getAppLabel() {
            return mAppName;
        }

        public ImageView getAppIcon() {
            return mAppIcon;
        }

        public ViewGroup getAppIconContainer() {
            return mAppIconContainer;
        }

        public ViewGroup getTitleContainer() {
            return mTitleContainer;
        }

        public TextView getSummary() {
            return mSummary;
        }

        public View getLeftConatiner() {
            return mLeftContainer;
        }

        public CheckBox getCheckBox() {
            return mCheckBox;
        }

        public RadioButton getRadioButton() {
            return mRadioButton;
        }

        public ViewGroup getWidgetContainer() {
            return mWidgetContainer;
        }

        public SwitchCompat getSwitch() {
            return mSwitch;
        }

        public ImageButton getActionButton() {
            return mActionButton;
        }

        public View getItem() {
            return itemView;
        }
    }

    public static class HeaderViewHolder extends ViewHolder {
        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class FooterViewHolder extends ViewHolder {
        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class SeparatorViewHolder extends ViewHolder {
        private final TextView mSeparatorText;

        public SeparatorViewHolder(View itemView) {
            super(itemView);
            mSeparatorText = itemView.findViewById(R.id.separator);
        }

        public void setSeparatorHeight(int height) {
            mSeparatorText.setHeight(height);
        }

        public void setSeparatorText(String separatorText) {
            ViewGroup.LayoutParams lp = mSeparatorText.getLayoutParams();
            lp.height = LayoutParams.WRAP_CONTENT;
            mSeparatorText.setLayoutParams(lp);
            mSeparatorText.setText(separatorText);
        }

        public TextView getSeparatorText() {
            return mSeparatorText;
        }
    }

    public static class AppLabelInfo {
        private String mActivityName;
        private boolean mIsSeparator = false;
        private String mLabel;
        private String mPackageName;

        public AppLabelInfo(String packageName, String label,
                            String activityName) {
            mPackageName = packageName;
            mLabel = label;
            mActivityName = activityName;
        }

        public String getPackageName() {
            return mPackageName;
        }

        public void setPackageName(String packageName) {
            mPackageName = packageName;
        }

        public String getActivityName() {
            return mActivityName;
        }

        public void setActivityName(String activityName) {
            mActivityName = activityName;
        }

        public String getLabel() {
            return mLabel;
        }

        public void setLabel(String label) {
            mLabel = label;
        }

        public boolean isSeparator() {
            return mIsSeparator;
        }

        public AppLabelInfo setSeparator(boolean isSeparator) {
            mIsSeparator = isSeparator;
            return this;
        }

        public String toString() {
            return "[AppLabel] label=" + mLabel + ", packageName=" + mPackageName;
        }
    }
}
