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

import android.os.Bundle;

import androidx.annotation.RestrictTo;

import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslArrayIndexer extends SeslAbsIndexer {
    private final String TAG = "SeslArrayIndexer";
    private final boolean DEBUG = false;
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected List<String> mData;

    public SeslArrayIndexer(List<String> listData, CharSequence indexCharacters) {
        super(indexCharacters);
        mData = listData;
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected int getItemCount() {
        return mData.size();
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected String getItemAt(int pos) {
        return mData.get(pos);
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected Bundle getBundle() {
        return null;
    }

    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected boolean isDataToBeIndexedAvailable() {
        return getItemCount() > 0;
    }
}
