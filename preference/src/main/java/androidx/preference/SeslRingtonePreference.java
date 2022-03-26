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

package androidx.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.reflect.content.SeslContextReflector;
import androidx.reflect.media.SeslAudioAttributesReflector;
import androidx.reflect.media.SeslRingtoneManagerReflector;
import androidx.reflect.os.SeslUserHandleReflector;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

public class SeslRingtonePreference extends Preference {
    private Context mUserContext;

    private int mUserId;

    private int mRingtoneType;
    private boolean mShowDefault;
    private boolean mShowSilent;

    public SeslRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RingtonePreference, defStyleAttr, defStyleRes);

        mRingtoneType = a.getInt(R.styleable.RingtonePreference_android_ringtoneType,
                RingtoneManager.TYPE_RINGTONE);
        mShowDefault = a.getBoolean(R.styleable.RingtonePreference_android_showDefault, true);
        mShowSilent = a.getBoolean(R.styleable.RingtonePreference_android_showSilent, true);
        setIntent(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER));
        setUserId(SeslUserHandleReflector.myUserId());

        a.recycle();
    }

    public SeslRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeslRingtonePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.ringtonePreferenceStyle);
    }

    public SeslRingtonePreference(Context context) {
        this(context, null);
    }

    public void setUserId(int userId) {
        mUserId = userId;
        mUserContext = createPackageContextAsUser(getContext(), mUserId);
    }

    public int getUserId() {
        return mUserId;
    }

    private Context createPackageContextAsUser(Context context, int userId) {
        return SeslContextReflector.createPackageContextAsUser(context, context.getPackageName(),
                0, UserHandle.getUserHandleForUid(userId));
    }

    public int getRingtoneType() {
        return mRingtoneType;
    }

    public void setRingtoneType(int type) {
        mRingtoneType = type;
    }

    public boolean getShowDefault() {
        return mShowDefault;
    }

    public void setShowDefault(boolean showDefault) {
        mShowDefault = showDefault;
    }

    public boolean getShowSilent() {
        return this.mShowSilent;
    }

    public void setShowSilent(boolean showSilent) {
        mShowSilent = showSilent;
    }

    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                onRestoreRingtone());
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,
                mShowDefault);

        if (mShowDefault) {
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(getRingtoneType()));
        }

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,
                mShowSilent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                mRingtoneType);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                getTitle());
        ringtonePickerIntent.putExtra(SeslRingtoneManagerReflector.getField_EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS(),
                SeslAudioAttributesReflector.getField_FLAG_BYPASS_INTERRUPTION_POLICY());
    }

    protected void onSaveRingtone(Uri ringtoneUri) {
        persistString(ringtoneUri != null ? ringtoneUri.toString() : "");
    }

    protected Uri onRestoreRingtone() {
        String uriString = getPersistedString(null);
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueObj) {
        String defaultValue = (String) defaultValueObj;
        if (!restorePersistedValue && !TextUtils.isEmpty(defaultValue)) {
            onSaveRingtone(Uri.parse(defaultValue));
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return true;
        }
        Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (!callChangeListener(uri != null ? uri.toString() : "")) {
            return true;
        }
        onSaveRingtone(uri);
        return true;
    }
}
