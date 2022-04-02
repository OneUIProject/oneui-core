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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ComponentName;
import android.content.Context;
import android.icu.text.AlphabeticIndex;
import android.os.Build;
import android.os.LocaleList;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.apppickerview.R;
import androidx.recyclerview.widget.RecyclerView;
import androidx.reflect.text.SeslTextUtilsReflector;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public abstract class AbsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements Filterable, SectionIndexer {
    private static final String TAG = "AppPickerViewAdapter";

    private final int MAX_OFFSET = 200;

    private AppPickerIconLoader mAppPickerIconLoader;
    protected Context mContext;
    private AppPickerView.OnBindListener mOnBindListener;
    private AppPickerView.OnSearchFilterListener mOnSearchFilterListener;
    private List<AppPickerView.AppLabelInfo> mDataSet = new ArrayList<>();
    private List<AppPickerView.AppLabelInfo> mDataSetFiltered = new ArrayList<>();
    private Map<String, Integer> mSectionMap = new HashMap<>();
    private String[] mSections = new String[0];
    private String mSearchText = "";

    private boolean mHideAllApps = false;

    private int mForegroundColor;
    private int mOrder;
    private int[] mPositionToSectionIndex;
    protected int mType;

    public AbsAdapter(Context context, int type, int order,
                      AppPickerIconLoader iconLoader) {
        mContext = context;
        mType = type;
        mOrder = order;
        mAppPickerIconLoader = iconLoader;

        TypedValue outValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.colorPrimary, outValue, true);
        mForegroundColor = outValue.resourceId != 0 ?
                mContext.getResources().getColor(outValue.resourceId) : outValue.data;
    }

    static AbsAdapter getAppPickerAdapter(Context context, List<String> packageNamesList, int type,
                                          int order, List<AppPickerView.AppLabelInfo> labelInfoList,
                                          AppPickerIconLoader iconLoader,
                                          List<ComponentName> activityNamesList) {
        final AbsAdapter adapter;
        if (type >= AppPickerView.TYPE_GRID) {
            adapter = new GridAdapter(context, type, order, iconLoader);
        } else {
            adapter = new ListAdapter(context, type, order, iconLoader);
        }
        adapter.setHasStableIds(true);
        adapter.resetPackages(packageNamesList, false, labelInfoList, activityNamesList);
        return adapter;
    }

    List<AppPickerView.AppLabelInfo> getDataSet() {
        return mDataSet;
    }

    void resetPackages(List<String> packageNamesList, boolean dataSetchanged,
                       List<AppPickerView.AppLabelInfo> labelInfoList,
                       List<ComponentName> activityNamesList) {
        Log.i(TAG, "Start resetpackage dataSetchanged : " + dataSetchanged);

        mDataSet.clear();
        mDataSet.addAll(DataManager.resetPackages(mContext, packageNamesList, labelInfoList, activityNamesList));

        if (Build.VERSION.SDK_INT >= 24) {
            if (getAppLabelComparator(mOrder) != null) {
                mDataSet.sort(getAppLabelComparator(mOrder));
            }
        }

        if (hasAllAppsInList()) {
            if (mDataSet != null && mDataSet.size() > 0) {
                mDataSet.add(0,
                        new AppPickerView.AppLabelInfo(AppPickerView.ALL_APPS_STRING,
                                "", ""));
            }
        }
        mDataSetFiltered.clear();
        mDataSetFiltered.addAll(mDataSet);

        refreshSectionMap();

        if (dataSetchanged) {
            notifyDataSetChanged();
        }

        Log.i(TAG, "End resetpackage");
    }

    void addPackage(int position, String label) {
        mDataSet.add(position,
                new AppPickerView.AppLabelInfo("", label, ""));
        mDataSetFiltered.clear();
        mDataSetFiltered.addAll(mDataSet);

        refreshSectionMap();
        notifyItemInserted(position);
    }

    void addSeparator(int position) {
        mDataSet.add(position,
                new AppPickerView.AppLabelInfo(AppPickerView.KEY_APP_SEPARATOR,
                        "", "").setSeparator(true));
        mDataSetFiltered.clear();
        mDataSetFiltered.addAll(mDataSet);

        refreshSectionMap();
        notifyItemInserted(position);
    }

    public void setOrder(int order) {
        mOrder = order;
        if (Build.VERSION.SDK_INT >= 24) {
            if (getAppLabelComparator(order) != null) {
                mDataSet.sort(getAppLabelComparator(order));
                mDataSetFiltered.sort(getAppLabelComparator(order));
            }
        }

        refreshSectionMap();
        notifyDataSetChanged();
    }

    private Comparator<AppPickerView.AppLabelInfo> getAppLabelComparator(int order) {
        switch (order) {
            case AppPickerView.ORDER_ASCENDING:
                return APP_LABEL_ASCENDING;
            case AppPickerView.ORDER_ASCENDING_IGNORE_CASE:
                return APP_LABEL_ASCENDING_IGNORE_CASE;
            case AppPickerView.ORDER_DESCENDING:
                return APP_LABEL_DESCENDING;
            case AppPickerView.ORDER_DESCENDING_IGNORE_CASE:
                return APP_LABEL_DESCENDING_IGNORE_CASE;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final String packageName = mDataSetFiltered.get(position).getPackageName();
        final String activityName = mDataSetFiltered.get(position).getActivityName();

        AppPickerView.ViewHolder vh = (AppPickerView.ViewHolder) holder;
        if (!(holder instanceof AppPickerView.HeaderViewHolder)
                && !(holder instanceof AppPickerView.SeparatorViewHolder)) {
            if (mAppPickerIconLoader != null) {
                mAppPickerIconLoader.loadIcon(packageName, activityName, vh.getAppIcon());
            }

            final String label = mDataSetFiltered.get(position).getLabel();
            if (mSearchText.length() > 0) {
                // TODO rework this method
                // kang
                SpannableString spannableString = new SpannableString(label);
                StringTokenizer stringTokenizer = new StringTokenizer(mSearchText);

                while (stringTokenizer.hasMoreTokens()) {
                    String nextToken = stringTokenizer.nextToken();
                    int i3 = 0;
                    String str = label;
                    do {
                        char[] semGetPrefixCharForSpan
                                = SeslTextUtilsReflector
                                .semGetPrefixCharForSpan(vh.getAppLabel().getPaint(),
                                        str, nextToken.toCharArray());
                        if (semGetPrefixCharForSpan != null) {
                            nextToken = new String(semGetPrefixCharForSpan);
                        }

                        int i2;
                        String lowerCase = str.toLowerCase();
                        if (str.length() == lowerCase.length()) {
                            i2 = lowerCase.indexOf(nextToken.toLowerCase());
                        } else {
                            i2 = str.indexOf(nextToken);
                        }

                        int length = nextToken.length() + i2;
                        if (i2 < 0) {
                            break;
                        }

                        int i4 = i2 + i3;
                        i3 += length;
                        spannableString.setSpan(
                                new ForegroundColorSpan(this.mForegroundColor), i4, i3, 17);
                        str = str.substring(length);
                        if (str.toLowerCase().indexOf(nextToken.toLowerCase()) != -1) {
                            break;
                        }
                    } while (i3 < MAX_OFFSET);
                }
                // kang
                vh.getAppLabel().setText(spannableString);
                vh.getItem().setContentDescription(spannableString);
            } else {
                vh.getAppLabel().setText(label);
                vh.getItem().setContentDescription(label);
            }
        }

        onBindViewHolderAction(vh, position, packageName);

        if (mOnBindListener != null) {
            mOnBindListener.onBindViewHolder(vh, position, packageName);
        }
    }

    abstract void onBindViewHolderAction(AppPickerView.ViewHolder holder, int position,
                                         String packageName);

    @Override
    public int getItemCount() {
        return mDataSetFiltered.size();
    }

    public AppPickerView.AppLabelInfo getAppInfo(int position) {
        return mDataSetFiltered.get(position);
    }

    public void setOnBindListener(@NonNull AppPickerView.OnBindListener listener) {
        mOnBindListener = listener;
    }

    public void setOnSearchFilterListener(AppPickerView.OnSearchFilterListener listener) {
        mOnSearchFilterListener = listener;
    }

    protected boolean hasAllAppsInList() {
        return (mType == AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS
                    || mType == AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS)
                && !mHideAllApps;
    }

    @Override
    public long getItemId(int position) {
        return mDataSetFiltered.get(position).hashCode();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final String searchText = constraint.toString();
                Filter.FilterResults results = new Filter.FilterResults();

                if (searchText.isEmpty()) {
                    mSearchText = "";
                    results.values = mDataSet;
                } else {
                    mSearchText = searchText;

                    ArrayList dataSetFiltered = new ArrayList();
                    for (AppPickerView.AppLabelInfo labelInfo : mDataSet) {
                        if (!AppPickerView.ALL_APPS_STRING.equals(labelInfo.getPackageName())) {
                            final String label = labelInfo.getLabel();
                            if (!TextUtils.isEmpty(label)) {
                                StringTokenizer tokenizer = new StringTokenizer(searchText.toLowerCase());
                                boolean showItem = true;
                                String lowerCase = label.toLowerCase();

                                while (tokenizer.hasMoreTokens()) {
                                    if (!lowerCase.contains(tokenizer.nextToken())) {
                                        showItem = false;
                                        break;
                                    }
                                }

                                if (showItem) {
                                    dataSetFiltered.add(labelInfo);
                                }
                            }
                        }
                    }

                    results.values = dataSetFiltered;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if ("".equals(mSearchText)) {
                    mHideAllApps = false;
                } else {
                    mHideAllApps = true;
                }
                mDataSetFiltered.clear();
                mDataSetFiltered.addAll((ArrayList) results.values);

                refreshSectionMap();
                notifyDataSetChanged();

                if (mOnSearchFilterListener != null) {
                    mOnSearchFilterListener.onSearchFilterCompleted(getItemCount());
                }
            }
        };
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionIndex >= mSections.length) {
            return 0;
        }
        return mSectionMap.get(mSections[sectionIndex]);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position >= mPositionToSectionIndex.length) {
            return 0;
        }
        return mPositionToSectionIndex[position];
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    private void refreshSectionMap() {
        mSectionMap.clear();
        ArrayList sections = new ArrayList();
        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList locales = mContext.getResources().getConfiguration().getLocales();
            if (locales.size() == 0) {
                locales = new LocaleList(Locale.ENGLISH);
            }

            AlphabeticIndex alphabeticIndex = new AlphabeticIndex(locales.get(0));
            for (int i = 1; i < locales.size(); i++) {
                alphabeticIndex.addLabels(locales.get(i));
            }
            alphabeticIndex.addLabels(Locale.ENGLISH);

            AlphabeticIndex.ImmutableIndex immutableIndex = alphabeticIndex.buildImmutableIndex();

            mPositionToSectionIndex = new int[mDataSetFiltered.size()];

            for (int i = 0; i < mDataSetFiltered.size(); i++) {
                String label = mDataSetFiltered.get(i).getLabel();
                if (TextUtils.isEmpty(label)) {
                    label = "";
                }
                label = immutableIndex.getBucket(immutableIndex.getBucketIndex(label)).getLabel();
                if (!mSectionMap.containsKey(label)) {
                    sections.add(label);
                    mSectionMap.put(label, i);
                }
                mPositionToSectionIndex[i] = sections.size() - 1;
            }

            mSections = new String[sections.size()];
            sections.toArray(mSections);
        }
    }

    protected float limitFontScale(@NonNull TextView textView) {
        final float currentFontScale
                = textView.getResources().getConfiguration().fontScale;
        int i = (currentFontScale > 1.3f ? 1 : (currentFontScale == 1.3f ? 0 : -1));
        final float textSize = textView.getTextSize();
        return i > 0 ? (textSize / currentFontScale) * 1.3f : textSize;
    }

    protected void limitFontLarge(TextView textView) {
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, limitFontScale(textView));
        }
    }

    protected void limitFontLarge2LinesHeight(TextView textView) {
        if (textView != null) {
            float limitFontScale = limitFontScale(textView);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, limitFontScale);
            textView.setMinHeight(Math.round((limitFontScale * 2.0f) + 0.5f));
        }
    }

    private static Comparator<AppPickerView.AppLabelInfo> APP_LABEL_ASCENDING
            = new Comparator<AppPickerView.AppLabelInfo>() {
        public int compare(AppPickerView.AppLabelInfo a, AppPickerView.AppLabelInfo b) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            collator.setStrength(Collator.TERTIARY);
            return collator.compare(a.getLabel(), b.getLabel());
        }
    };

    private static Comparator<AppPickerView.AppLabelInfo> APP_LABEL_ASCENDING_IGNORE_CASE
            = new Comparator<AppPickerView.AppLabelInfo>() {
        public int compare(AppPickerView.AppLabelInfo a, AppPickerView.AppLabelInfo b) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            collator.setStrength(Collator.PRIMARY);
            return collator.compare(a.getLabel(), b.getLabel());
        }
    };

    private static Comparator<AppPickerView.AppLabelInfo> APP_LABEL_DESCENDING
            = new Comparator<AppPickerView.AppLabelInfo>() {
        public int compare(AppPickerView.AppLabelInfo a, AppPickerView.AppLabelInfo b) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            collator.setStrength(Collator.TERTIARY);
            return collator.compare(b.getLabel(), a.getLabel());
        }
    };

    private static Comparator<AppPickerView.AppLabelInfo> APP_LABEL_DESCENDING_IGNORE_CASE
            = new Comparator<AppPickerView.AppLabelInfo>() {
        public int compare(AppPickerView.AppLabelInfo a, AppPickerView.AppLabelInfo b) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            collator.setStrength(Collator.PRIMARY);
            return collator.compare(b.getLabel(), a.getLabel());
        }
    };
}
