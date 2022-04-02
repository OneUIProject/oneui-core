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
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.apppickerview.R;
import androidx.recyclerview.widget.RecyclerView;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
class ListAdapter extends AbsAdapter {
    private static final int TYPE_HEADER = 256;
    private static final int TYPE_ITEM = 257;
    private static final int TYPE_SEPARATOR = 259;
    private static final int TYPE_FOOTER = 258;

    public ListAdapter(Context context, int type, int order,
                       AppPickerIconLoader iconLoader) {
        super(context, type, order, iconLoader);
    }

    @Override
    void onBindViewHolderAction(final AppPickerView.ViewHolder holder, int position, String packageName) {
        if (getItemViewType(position) != TYPE_SEPARATOR) {
            switch (mType) {
                case AppPickerView.TYPE_LIST: {
                    holder.getWidgetContainer().setVisibility(View.GONE);
                    holder.getLeftConatiner().setVisibility(View.GONE);
                }
                break;

                case AppPickerView.TYPE_LIST_ACTION_BUTTON: {
                    holder.getWidgetContainer().setVisibility(View.VISIBLE);
                    holder.getActionButton().setVisibility(View.VISIBLE);
                }
                break;

                case AppPickerView.TYPE_LIST_CHECKBOX:
                case AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS: {
                    holder.getLeftConatiner().setVisibility(View.VISIBLE);
                    holder.getWidgetContainer().setVisibility(View.GONE);

                    holder.getItem().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.getCheckBox()
                                    .setChecked(!holder.getCheckBox().isChecked());
                        }
                    });

                    AccessibilityManager am = (AccessibilityManager) mContext
                            .getSystemService(Context.ACCESSIBILITY_SERVICE);
                    if (am.isEnabled()) {
                        holder.getCheckBox().setFocusable(false);
                        holder.getCheckBox().setClickable(false);
                        holder.getItem().setContentDescription(null);
                    }
                }
                break;

                case AppPickerView.TYPE_LIST_RADIOBUTTON: {
                    holder.getLeftConatiner().setVisibility(View.VISIBLE);
                    holder.getWidgetContainer().setVisibility(View.GONE);

                    holder.getItem().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.getRadioButton()
                                    .setChecked(!holder.getRadioButton().isChecked());
                        }
                    });

                    AccessibilityManager am = (AccessibilityManager) mContext
                            .getSystemService(Context.ACCESSIBILITY_SERVICE);
                    if (am.isEnabled()) {
                        holder.getRadioButton().setFocusable(false);
                        holder.getRadioButton().setClickable(false);
                        holder.getItem().setContentDescription(null);
                    }
                }
                break;

                case AppPickerView.TYPE_LIST_SWITCH:
                case AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS: {
                    holder.getLeftConatiner().setVisibility(View.GONE);
                    holder.getWidgetContainer().setVisibility(View.VISIBLE);

                    holder.getItem().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.getSwitch()
                                    .setChecked(!holder.getSwitch().isChecked());
                        }
                    });

                    AccessibilityManager am = (AccessibilityManager) mContext
                            .getSystemService(Context.ACCESSIBILITY_SERVICE);
                    if (am.isEnabled()) {
                        holder.getSwitch().setFocusable(false);
                        holder.getSwitch().setClickable(false);
                        holder.getItem().setContentDescription(null);
                    }
                }
                break;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutResId = R.layout.app_picker_list;
        if (viewType == TYPE_HEADER && hasAllAppsInList()) {
            layoutResId = R.layout.app_picker_list_header;
        } else if (viewType == TYPE_FOOTER) {
            layoutResId = R.layout.app_picker_list_footer;
        } else if (viewType == TYPE_SEPARATOR) {
            layoutResId = R.layout.app_picker_list_separator;
        }

        View itemView = LayoutInflater.from(mContext)
                .inflate(layoutResId, parent, false);

        ViewGroup widgetFrame = itemView.findViewById(R.id.widget_frame);
        if (widgetFrame != null) {
            switch (mType) {
                case AppPickerView.TYPE_LIST:
                case AppPickerView.TYPE_LIST_SWITCH:
                case AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS:
                    LayoutInflater.from(mContext)
                            .inflate(R.layout.app_picker_frame_switch, widgetFrame, true);
                    break;
                case AppPickerView.TYPE_LIST_ACTION_BUTTON:
                    LayoutInflater.from(mContext)
                            .inflate(R.layout.app_picker_frame_actionbutton, widgetFrame, true);
                    break;
                case AppPickerView.TYPE_LIST_CHECKBOX:
                case AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS:
                    LayoutInflater.from(mContext)
                            .inflate(R.layout.app_picker_frame_checkbox, itemView.findViewById(R.id.left_frame), true);
                    break;
                case AppPickerView.TYPE_LIST_RADIOBUTTON:
                    itemView.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.app_picker_list_radio_padding_start),
                            0,
                            mContext.getResources().getDimensionPixelSize(R.dimen.app_picker_list_padding_end),
                            0);
                    LayoutInflater.from(mContext)
                            .inflate(R.layout.app_picker_frame_radiobutton, itemView.findViewById(R.id.left_frame), true);
                    break;
            }
        }

        limitFontLarge(itemView.findViewById(R.id.title));
        limitFontLarge(itemView.findViewById(R.id.summary));

        if (viewType == TYPE_HEADER && hasAllAppsInList()) {
            return new AppPickerView.HeaderViewHolder(itemView);
        }
        if (viewType == TYPE_FOOTER) {
            return new AppPickerView.FooterViewHolder(itemView);
        }
        if (viewType == TYPE_SEPARATOR) {
            return new AppPickerView.SeparatorViewHolder(itemView);
        }
        return new AppPickerView.ViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        if (getAppInfo(position).isSeparator()) {
            return TYPE_SEPARATOR;
        }
        if ((position == 0
                || (getAppInfo(position).isSeparator() && position == 1))
                && hasAllAppsInList()) {
            return TYPE_HEADER;
        }
        if (position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }
}
