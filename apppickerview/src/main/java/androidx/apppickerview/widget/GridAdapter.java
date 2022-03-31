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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RestrictTo;
import androidx.apppickerview.R;
import androidx.recyclerview.widget.RecyclerView;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
class GridAdapter extends AbsAdapter {
    private static final int TYPE_HEADER = 256;
    private static final int TYPE_ITEM = 257;
    private static final int TYPE_SEPARATOR = 259;

    public GridAdapter(Context context, int type, int order,
                       AppPickerIconLoader iconLoader) {
        super(context, type, order, iconLoader);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_SEPARATOR) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View itemView = inflater.inflate(R.layout.app_picker_list_separator, parent, false);
            return new AppPickerView.SeparatorViewHolder(itemView);
        } else {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View itemView = inflater.inflate(R.layout.app_picker_grid, parent, false);
            if (mType == AppPickerView.TYPE_GRID) {
                itemView.findViewById(R.id.check_widget).setVisibility(View.GONE);
            }
            limitFontLarge2LinesHeight(itemView.findViewById(R.id.title));
            return new AppPickerView.ViewHolder(itemView);
        }
    }

    @Override
    void onBindViewHolderAction(AppPickerView.ViewHolder holder, int position, String packageName) {
    }

    @Override
    public int getItemViewType(int position) {
        if (getAppInfo(position).isSeparator()) {
            return TYPE_SEPARATOR;
        }
        return TYPE_ITEM;
    }
}
