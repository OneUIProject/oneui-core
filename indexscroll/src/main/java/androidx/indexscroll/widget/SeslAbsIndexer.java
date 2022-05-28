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

package androidx.indexscroll.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.RestrictTo;
import androidx.indexscroll.widget.SeslIndexScrollView.IndexScroll;

import java.text.Collator;
import java.util.HashMap;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY)
public abstract class SeslAbsIndexer extends DataSetObserver {
    private final String TAG = "SeslAbsIndexer";

    private final boolean DEBUG = false;

    private static final char DIGIT_CHAR = '#';
    private static final char FAVORITE_CHAR = '★';
    private static final String GROUP_CHAR = "\ud83d\udc65︎";
    private static final char GROUP_CHECKER = '\ud83d';
    private static final char SYMBOL_BASE_CHAR = '!';
    private static final char SYMBOL_CHAR = '&';

    static final String INDEXSCROLL_INDEX_COUNTS = "indexscroll_index_counts";
    static final String INDEXSCROLL_INDEX_TITLES = "indexscroll_index_titles";

    private SparseIntArray mAlphaMap;
    private CharSequence mAlphabet;
    private String[] mAlphabetArray;
    private Bundle mBundle;
    protected Collator mCollator;
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private String[] mLangAlphabetArray;
    private HashMap<Integer, Integer> mLangIndexMap = new HashMap<>();

    private int mAlphabetLength;
    private int[] mCachingValue;
    private int mFavoriteItemCount = 0;
    private int mProfileItemCount = 0;
    private int mGroupItemCount = 0;
    private int mDigitItemCount = 0;

    private boolean mUseFavoriteIndex = false;
    private boolean mRegisteredDataSetObservable = false;
    private boolean mUseGroupIndex = false;
    private boolean mUseDigitIndex = false;
    private boolean mIsInitialized = false;

    protected abstract Bundle getBundle();

    protected abstract String getItemAt(int pos);

    protected abstract int getItemCount();

    protected abstract boolean isDataToBeIndexedAvailable();

    SeslAbsIndexer(CharSequence indexCharacters) {
        mUseFavoriteIndex = false;
        mProfileItemCount = 0;
        mFavoriteItemCount = 0;
        initIndexer(indexCharacters);
    }

    SeslAbsIndexer(String[] indexCharacters, int aLangIndex) {
        mUseFavoriteIndex = false;
        mProfileItemCount = 0;
        mFavoriteItemCount = 0;
        mLangAlphabetArray = indexCharacters;
        setIndexerArray();
    }

    String[] getLangAlphabetArray() {
        return mLangAlphabetArray;
    }

    int getCachingValue(int index) {
        if (index >= 0 && index < mAlphabetLength) {
            return mCachingValue[index];
        }
        return -1;
    }

    int getIndexByPosition(int position) {
        if (mCachingValue == null) {
            return IndexScroll.NO_SELECTED_INDEX;
        }

        int lastIndex = IndexScroll.NO_SELECTED_INDEX;
        for (int i = 0; i < mAlphabetLength; i++) {
            lastIndex = i;

            if (mCachingValue[i] == position) {
                return i;
            }

            if (mCachingValue[i] > position) {
                return i - 1;
            }
        }

        return lastIndex;
    }

    private void setIndexerArray() {
        StringBuilder indexerString = new StringBuilder();

        if (mUseFavoriteIndex) {
            indexerString.append(FAVORITE_CHAR);
        }

        if (mUseGroupIndex) {
            indexerString.append(GROUP_CHECKER);
        }

        int langIndex = 0;
        while (langIndex < mLangAlphabetArray.length) {
            for (int i = 0; i < mLangAlphabetArray[langIndex].length(); i++) {
                mLangIndexMap.put(indexerString.length(), Integer.valueOf(langIndex));
                indexerString.append(mLangAlphabetArray[langIndex].charAt(i));
            }
            langIndex++;
        }

        if (mUseDigitIndex) {
            mLangIndexMap.put(indexerString.length(), Integer.valueOf(langIndex - 1));
            indexerString.append(DIGIT_CHAR);
        }

        if (indexerString.length() != 0) {
            initIndexer(indexerString.toString());
        } else {
            Log.w("SeslAbsIndexer",
                    "The array received from App is empty. " +
                    "Indexer must be initialized through additional API.");
        }
    }

    void setProfileItem(int count) {
        if (count >= 0) {
            mProfileItemCount = count;
        }
    }

    void setFavoriteItem(int count) {
        if (count > 0) {
            mFavoriteItemCount = count;
            mUseFavoriteIndex = true;
            setIndexerArray();
        }
    }

    void setGroupItem(int count) {
        if (count > 0) {
            mGroupItemCount = count;
            mUseGroupIndex = true;
            setIndexerArray();
        }
    }

    void setDigitItem(int count) {
        if (count > 0) {
            mDigitItemCount = count;
            mUseDigitIndex = true;
            setIndexerArray();
        }
    }

    private void initIndexer(CharSequence alphabet) {
        if (alphabet == null || alphabet.length() == 0) {
            throw new IllegalArgumentException("Invalid indexString :"
                    + ((Object) alphabet));
        }

        mAlphabet = alphabet;
        mAlphabetLength = alphabet.length();
        mCachingValue = new int[mAlphabetLength];
        mAlphabetArray = new String[mAlphabetLength];

        for (int i = 0; i < mAlphabetLength; i++) {
            if (mUseGroupIndex && mAlphabet.charAt(i) == GROUP_CHECKER) {
                mAlphabetArray[i] = GROUP_CHAR;
            } else {
                mAlphabetArray[i] = Character.toString(mAlphabet.charAt(i));
            }
        }

        mAlphaMap = new SparseIntArray(mAlphabetLength);

        mCollator = Collator.getInstance();
        mCollator.setStrength(Collator.PRIMARY);

        mIsInitialized = true;
    }

    boolean isInitialized() {
        return mIsInitialized;
    }

    String[] getAlphabetArray() {
        return mAlphabetArray;
    }

    private int compare(String word, String indexString) {
        return mCollator.compare(word, indexString);
    }

    void cacheIndexInfo() {
        if (isDataToBeIndexedAvailable() && getItemCount() != 0) {
            mBundle = getBundle();

            if (mBundle != null) {
                if (mBundle.containsKey(INDEXSCROLL_INDEX_TITLES)
                        && mBundle.containsKey(INDEXSCROLL_INDEX_COUNTS)) {
                    getBundleInfo();
                    return;
                }
            }

            onBeginTransaction();
            for (int i = 0; i < mAlphabetLength; i++) {
                mCachingValue[i] = getPositionForString("" + mAlphabet.charAt(i));
            }
            onEndTransaction();
        }
    }

    // TODO rework this method
    // kang
    private int getPositionForString(String var1) {
        /* var1 = searchString */
        SparseIntArray var2 = this.mAlphaMap;
        int var3 = this.getItemCount();
        if (var3 != 0 && this.mAlphabet != null) {
            int var4 = var3;
            if (var1 != null) {
                if (var1.length() == 0) {
                    var4 = var3;
                } else {
                    char var5 = var1.charAt(0);
                    var4 = var2.get(var5, -2147483648);
                    int var6;
                    int var7;
                    if (-2147483648 != var4) {
                        var6 = Math.abs(var4);
                        var7 = var3;
                    } else {
                        CharSequence var8;
                        label116: {
                            var6 = this.mAlphabet.toString().indexOf(var5);
                            if (var6 > 0) {
                                var8 = this.mAlphabet;
                                var4 = var6 - 1;
                                if (var5 > var8.charAt(var4)) {
                                    var4 = var2.get(this.mAlphabet.charAt(var4), -2147483648);
                                    if (var4 != -2147483648) {
                                        var4 = Math.abs(var4);
                                        break label116;
                                    }
                                }
                            }

                            var4 = 0;
                        }

                        label110: {
                            if (var6 < this.mAlphabet.length() - 1) {
                                var8 = this.mAlphabet;
                                ++var6;
                                if (var5 < var8.charAt(var6)) {
                                    var6 = var2.get(this.mAlphabet.charAt(var6), -2147483648);
                                    if (var6 != -2147483648) {
                                        var6 = Math.abs(var6);
                                        break label110;
                                    }
                                }
                            }

                            var6 = var3;
                        }

                        var7 = var6;
                        var6 = var4;
                    }

                    char var9 = var1.charAt(0);
                    String var14;
                    if (var9 == '&') {
                        var14 = "!";
                    } else {
                        var14 = var1;
                    }

                    int var10;
                    if (var9 == 9733) {
                        var10 = this.mProfileItemCount;
                        var4 = var6;
                        if (var6 < var10) {
                            var4 = var10;
                        }
                    } else {
                        int var11;
                        if (var9 == '\ud83d') {
                            var11 = this.mProfileItemCount;
                            var10 = this.mFavoriteItemCount;
                            var4 = var6;
                            if (var6 < var11 + var10) {
                                var4 = var11 + var10;
                            }
                        } else {
                            var11 = this.mProfileItemCount;
                            var10 = this.mFavoriteItemCount;
                            int var12 = this.mGroupItemCount;
                            var4 = var6;
                            if (var6 < var11 + var10 + var12) {
                                var4 = var11 + var10 + var12;
                            }
                        }
                    }

                    var7 -= this.mDigitItemCount;
                    var6 = var4;
                    if (var9 == '#') {
                        var6 = var7;
                    }

                    var4 = (var7 + var6) / 2;

                    label101: {
                        while(var4 >= var6 && var4 < var7) {
                            String var13 = this.getItemAt(var4);
                            if (var13 != null && !var13.equals("")) {
                                var10 = this.compare(var13, var14);
                                if (var9 == 9733 || var9 == '&' || var9 == '#' || var9 == '\ud83d') {
                                    var10 = 1;
                                }

                                label87: {
                                    if (var10 != 0) {
                                        if (var10 < 0) {
                                            var6 = var4 + 1;
                                            if (var6 >= var3) {
                                                break label101;
                                            }
                                            break label87;
                                        }
                                    } else if (var6 == var4) {
                                        break;
                                    }

                                    var7 = var4;
                                }

                                var4 = (var6 + var7) / 2;
                            } else {
                                if (var4 <= var6) {
                                    break;
                                }

                                --var4;
                            }
                        }

                        var3 = var4;
                    }

                    var4 = var3;
                    if (var1.length() == 1) {
                        var2.put(var5, var3);
                        var4 = var3;
                    }
                }
            }

            return var4;
        } else {
            return 0;
        }
    }
    // kang

    private void getBundleInfo() {
        final String[] sections = mBundle.getStringArray(INDEXSCROLL_INDEX_TITLES);
        final int[] counts = mBundle.getIntArray(INDEXSCROLL_INDEX_COUNTS);

        int basePosition = mProfileItemCount;
        int baseSectionIndex = 0;
        for (int index = 0; index < mAlphabetLength; index++) {
            final char targetChar = mAlphabet.charAt(index);
            mCachingValue[index] = basePosition;

            if (DEBUG) {
                Log.d(TAG, "Get index info from bundle (" + index + ") : "
                        + targetChar + " = " + basePosition);
            }

            if (targetChar == FAVORITE_CHAR) {
                basePosition += mFavoriteItemCount;
            } else if (targetChar == GROUP_CHECKER) {
                basePosition += mGroupItemCount;
            }

            int sectionIndex = baseSectionIndex;
            while (true) {
                if (sectionIndex >= sections.length) {
                    break;
                } else if (targetChar == sections[sectionIndex].charAt(0)) {
                    basePosition += counts[sectionIndex];
                    baseSectionIndex = sectionIndex;
                    break;
                } else {
                    sectionIndex++;
                }
            }
        }
    }

    @Override
    public void onChanged() {
        super.onChanged();
        mAlphaMap.clear();
        mDataSetObservable.notifyChanged();
    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
        mAlphaMap.clear();
        mDataSetObservable.notifyInvalidated();
    }

    void registerDataSetObserver(DataSetObserver observer) {
        if (!mRegisteredDataSetObservable) {
            mRegisteredDataSetObservable = true;
            mDataSetObservable.registerObserver(observer);
        } else {
            Log.e("SeslAbsIndexer", "Observer " + observer + " is already registered.");
        }
    }

    void unregisterDataSetObserver(DataSetObserver observer) {
        if (mRegisteredDataSetObservable) {
            mRegisteredDataSetObservable = false;
            mDataSetObservable.unregisterObserver(observer);
        } else {
            Log.e("SeslAbsIndexer", "Observer " + observer + " was not registered.");
        }
    }

    void onBeginTransaction() {
    }

    void onEndTransaction() {
    }
}
