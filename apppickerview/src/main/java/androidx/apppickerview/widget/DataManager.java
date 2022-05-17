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
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
class DataManager {
    private static final String TAG = "DataManager";

    private static final int MAX_APP_LIST_COUNT = 10000;

    private static final Uri APP_LIST_PROVIDER_CONTENT_URI
            = Uri.parse("content://com.samsung.android.settings.applist");
    private static final String KEY_APP_LABEL = "app_title";
    private static final String KEY_PACKAGE_NAME = "package_name";

    private static final boolean sIsSupportQUERY = Build.VERSION.SDK_INT >= 26;
    private static final boolean sIsSupportSCS = Build.VERSION.SDK_INT > 29;

    DataManager() {
    }

    public static List<AppPickerView.AppLabelInfo> resetPackages(Context context,
                                                                 List<String> list) {
        return resetPackages(context, list, null, null);
    }

    public static List<AppPickerView.AppLabelInfo> resetPackages(List<ComponentName> list,
                                                                 Context context) {
        return resetPackages(context, null, null, list);
    }

    // kang
    public static List<AppPickerView.AppLabelInfo> resetPackages(Context context,
                                                     List<String> list,
                                                     List<AppPickerView.AppLabelInfo> list2,
                                                     List<ComponentName> list3) {
        HashMap hashMap;
        boolean z = list3 != null;
        HashMap<String, String> labelFromSCS = sIsSupportQUERY ?
                getLabelFromSCS(context, z) : z ? null : loadLabelFromSettings(context);
        if (list2 != null) {
            hashMap = new HashMap();
            for (AppPickerView.AppLabelInfo appLabelInfo : list2) {
                String packageName = appLabelInfo.getPackageName();
                if (appLabelInfo.getActivityName() != null
                        && !appLabelInfo.getActivityName().equals("")) {
                    packageName = packageName + "/" + appLabelInfo.getActivityName();
                }
                hashMap.put(packageName, appLabelInfo.getLabel());
            }
        } else {
            hashMap = null;
        }
        ArrayList arrayList = new ArrayList();
        if (z) {
            for (ComponentName componentName : list3) {
                String str = componentName.getPackageName() + "/" + componentName.getClassName();
                String str2 = hashMap != null ? (String) hashMap.get(str) : null;
                if (str2 == null && labelFromSCS != null) {
                    str2 = labelFromSCS.get(str);
                }
                if (str2 == null) {
                    str2 = getLabelFromPackageManager(context, componentName);
                }
                arrayList.add(
                        new AppPickerView.AppLabelInfo(componentName.getPackageName(),
                                str2, componentName.getClassName()));
            }
        } else {
            for (String str3 : list) {
                String str4 = hashMap != null ? (String) hashMap.get(str3) : null;
                if (str4 == null && labelFromSCS != null) {
                    str4 = labelFromSCS.get(str3);
                }
                if (str4 == null) {
                    str4 = getLabelFromPackageManager(context, str3);
                }
                arrayList.add(new AppPickerView.AppLabelInfo(str3, str4, ""));
            }
        }
        return arrayList;
    }
    // kang

    // kang
    private static HashMap<String, String> loadLabelFromSettings(Context context) {
        Cursor query = context.getContentResolver().query(APP_LIST_PROVIDER_CONTENT_URI,
                null, null, null, null);
        HashMap<String, String> hashMap = new HashMap<>();
        if (query != null && query.moveToFirst()) {
            do {
                hashMap.put(query.getString(query.getColumnIndex(KEY_PACKAGE_NAME)),
                        query.getString(query.getColumnIndex(KEY_APP_LABEL)));
            } while (query.moveToNext());
            query.close();
        }
        return hashMap;
    }
    // kang

    // kang
    private static HashMap<String, String> getLabelFromSCS(Context context, boolean z) {
        String str;
        HashMap<String, String> hashMap = new HashMap<>();
        Cursor cursor = null;
        try {
            try {
                Uri withAppendedPath = Uri.withAppendedPath(Uri.parse("content://"
                            + (sIsSupportSCS ?
                            "com.samsung.android.scs.ai.search/v1"
                            : "com.samsung.android.bixby.service.bixbysearch/v1")),
                        "application");
                Bundle bundle = new Bundle();
                bundle.putString("android:query-arg-sql-selection", "*");
                bundle.putBoolean("query-arg-all-apps", true);
                bundle.putInt("android:query-arg-limit", MAX_APP_LIST_COUNT);
                cursor = context.getContentResolver().query(withAppendedPath,
                        null, bundle, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String string = cursor.getString(cursor.getColumnIndex("label"));
                        if (z) {
                            str = cursor.getString(cursor.getColumnIndex("componentName"))
                                    + "/" + cursor.getString(cursor.getColumnIndex("packageName"));
                        } else {
                            str = cursor.getString(cursor.getColumnIndex("packageName"));
                        }
                        hashMap.put(str, string);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hashMap;
    }
    // kang

    // kang
    private static String getLabelFromPackageManager(Context context, String str) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 0);
            return applicationInfo != null ?
                    (String) packageManager.getApplicationLabel(applicationInfo) : "Unknown";
        } catch (PackageManager.NameNotFoundException unused) {
            Log.i(TAG, "can't find label for " + str);
            return "Unknown";
        }
    }
    // kang

    // kang
    private static String getLabelFromPackageManager(Context context, ComponentName componentName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
            return activityInfo != null ?
                    activityInfo.loadLabel(packageManager).toString() : "Unknown";
        } catch (PackageManager.NameNotFoundException unused) {
            Log.i(TAG, "can't find label for " + componentName);
            return "Unknown";
        }
    }
    // kang
}
