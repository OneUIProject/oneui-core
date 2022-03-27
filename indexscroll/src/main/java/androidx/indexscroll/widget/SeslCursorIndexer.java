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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RestrictTo;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslCursorIndexer extends SeslAbsIndexer {
    private final String TAG = "SeslCursorIndexer";

    private final boolean DEBUG = false;

    public static final String EXTRA_INDEX_COUNTS = "indexscroll_index_counts";
    public static final String EXTRA_INDEX_TITLES = "indexscroll_index_titles";

    protected Cursor mCursor;
    protected int mColumnIndex;
    protected int mSavedCursorPos;

    public SeslCursorIndexer(Cursor cursor, int sortedColumnIndex,
                             CharSequence indexCharacters) {
        super(indexCharacters);
        mCursor = cursor;
        mColumnIndex = sortedColumnIndex;
    }

    public SeslCursorIndexer(Cursor cursor, int sortedColumnIndex,
                             String[] indexCharacters, int aLangIndex) {
        super(indexCharacters, aLangIndex);
        mCursor = cursor;
        mColumnIndex = sortedColumnIndex;
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected boolean isDataToBeIndexedAvailable() {
        return getItemCount() > 0 && !mCursor.isClosed();
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected String getItemAt(int pos) {
        if (mCursor.isClosed()) {
            Log.d(TAG, "SeslCursorIndexer getItemAt : mCursor is closed.");
            return null;
        }

        if (DEBUG) {
            if (mColumnIndex < 0) {
                Log.d(TAG, "getItemAt() mColumnIndex : " + mColumnIndex);
            }
        }

        mCursor.moveToPosition(pos);

        try {
            return mCursor.getString(mColumnIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected int getItemCount() {
        if (!mCursor.isClosed()) {
            return mCursor.getCount();
        } else {
            Log.d(TAG, "SeslCursorIndexer getItemCount : mCursor is closed.");
            return 0;
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected Bundle getBundle() {
        if (mCursor.isClosed()) {
            return null;
        } else {
            Log.d(TAG, "Bundle was used by Indexer");
            return mCursor.getExtras();
        }
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onBeginTransaction() {
        mSavedCursorPos = mCursor.getPosition();
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onEndTransaction() {
        mCursor.moveToPosition(mSavedCursorPos);
    }

    public void setProfileItemsCount(int count) {
        setProfileItem(count);
    }

    public void setFavoriteItemsCount(int count) {
        setFavoriteItem(count);
    }

    public void setGroupItemsCount(int count) {
        setGroupItem(count);
    }

    public void setMiscItemsCount(int count) {
        setDigitItem(count);
    }
}
