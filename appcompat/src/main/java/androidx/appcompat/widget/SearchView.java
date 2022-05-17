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

package androidx.appcompat.widget;

import static android.widget.ListPopupWindow.INPUT_METHOD_NEEDED;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.appcompat.widget.SuggestionsAdapter.getColumnString;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.util.SeslMisc;
import androidx.appcompat.view.CollapsibleActionView;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.view.ViewCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.customview.view.AbsSavedState;
import androidx.reflect.os.SeslBuildReflector;
import androidx.reflect.view.inputmethod.SeslInputMethodManagerReflector;
import androidx.reflect.widget.SeslTextViewReflector;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.WeakHashMap;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

/**
 * Samsung SearchView class.
 */
public class SearchView extends LinearLayoutCompat implements CollapsibleActionView {

    static final boolean DBG = false;
    static final String LOG_TAG = "SearchView";

    private static final int SEP_VERSION_SUPPORTING_SVI_SEARCH_QUERY = 110100;

    private static final String SVI_PACKAGE = "com.samsung.android.svoiceime";
    private static final String AUTHORITY_SVI_APP = "com.samsung.android.svoiceime.provider";
    private static final int SVI_VERSION_SUPPORTING_SEARCH_QUERY = 220002001;
    private static final String SVI_ACTION = "samsung.svoiceime.action.RECOGNIZE_SPEECH";
    private static final String SVI_INTENT_EXTRA = "samsung.svoiceime.extra.LANGUAGE";

    private static final String KEY_SVI_APP_LOCALE = "is_svoice_locale_supported";

    public static final int FLAG_MUTABLE = 0x2000000;

    /**
     * Private constant for removing the microphone in the keyboard.
     */
    private static final String IME_OPTION_NO_MICROPHONE = "nm";

    private Context mContext;
    private InputMethodManager mImm;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final ImageView mBackButton;
    final SearchAutoComplete mSearchSrcTextView;
    private final View mSearchEditFrame;
    private final View mSearchPlate;
    private final View mSubmitArea;
    final ImageView mSearchButton;
    final ImageView mGoButton;
    final ImageView mCloseButton;
    final ImageView mVoiceButton;
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final ImageView mMoreButton;
    private final View mDropDownAnchor;

    private UpdatableTouchDelegate mTouchDelegate;
    private Rect mSearchSrcTextViewBounds = new Rect();
    private Rect mSearchSrtTextViewBoundsExpanded = new Rect();
    private int[] mTemp = new int[2];
    private int[] mTemp2 = new int[2];

    /** Icon optionally displayed when the SearchView is collapsed. */
    private final ImageView mCollapsedIcon;

    /** Drawable used as an EditText hint. */
    private final Drawable mSearchHintIcon;

    // Resources used by SuggestionsAdapter to display suggestions.
    private final int mSuggestionRowLayout;
    private final int mSuggestionCommitIconResId;

    // Intents used for voice searching.
    private final Intent mVoiceWebSearchIntent;
    private final Intent mVoiceAppSearchIntent;
    private final Intent mSVoiceSearchIntent;

    private final CharSequence mDefaultQueryHint;

    private OnQueryTextListener mOnQueryChangeListener;
    private OnCloseListener mOnCloseListener;
    OnFocusChangeListener mOnQueryTextFocusChangeListener;
    private OnSuggestionListener mOnSuggestionListener;
    private OnClickListener mOnSearchClickListener;

    private boolean mIsLightTheme = false;
    private boolean mIconifiedByDefault;
    private boolean mIconified;
    CursorAdapter mSuggestionsAdapter;
    private boolean mSubmitButtonEnabled;
    private CharSequence mQueryHint;
    private boolean mQueryRefinement;
    private boolean mClearingFocus;
    private int mMaxWidth;
    private boolean mVoiceButtonEnabled;
    private CharSequence mOldQueryText;
    private CharSequence mUserQuery;
    private boolean mExpandedInActionView;
    private int mCollapsedImeOptions;
    private boolean mUseSVI = false;

    SearchableInfo mSearchable;
    private Bundle mAppSearchData;

    private Typeface mBoldTypeface;
    private int mSearchIconResId;

    static final PreQAutoCompleteTextViewReflector PRE_API_29_HIDDEN_METHOD_INVOKER =
            (Build.VERSION.SDK_INT < 29) ? new PreQAutoCompleteTextViewReflector() : null;

    private final Runnable mUpdateDrawableStateRunnable = new Runnable() {
        @Override
        public void run() {
            updateFocusedState();
        }
    };

    private Runnable mReleaseCursorRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSuggestionsAdapter instanceof SuggestionsAdapter) {
                mSuggestionsAdapter.changeCursor(null);
            }
        }
    };

    // A weak map of drawables we've gotten from other packages, so we don't load them
    // more than once.
    private final WeakHashMap<String, Drawable.ConstantState> mOutsideDrawablesCache =
            new WeakHashMap<String, Drawable.ConstantState>();

    public interface OnPrivateImeCommandListener {
        boolean onPrivateIMECommand(String action, Bundle data);
    }

    /**
     * Callbacks for changes to the query text.
     */
    public interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         *
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         *
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);
    }

    public interface OnCloseListener {

        /**
         * The user is attempting to close the SearchView.
         *
         * @return true if the listener wants to override the default behavior of clearing the
         * text field and dismissing it, false otherwise.
         */
        boolean onClose();
    }

    /**
     * Callback interface for selection events on suggestions. These callbacks
     * are only relevant when a SearchableInfo has been specified by {@link #setSearchableInfo}.
     */
    public interface OnSuggestionListener {

        /**
         * Called when a suggestion was selected by navigating to it.
         * @param position the absolute position in the list of suggestions.
         *
         * @return true if the listener handles the event and wants to override the default
         * behavior of possibly rewriting the query based on the selected item, false otherwise.
         */
        boolean onSuggestionSelect(int position);

        /**
         * Called when a suggestion was clicked.
         * @param position the absolute position of the clicked item in the list of suggestions.
         *
         * @return true if the listener handles the event and wants to override the default
         * behavior of launching any intent or submitting a search query specified on that item.
         * Return false otherwise.
         */
        boolean onSuggestionClick(int position);
    }

    public SearchView(@NonNull Context context) {
        this(context, null);
    }

    public SearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.searchViewStyle);
    }

    public SearchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context,
                attrs, R.styleable.SearchView, defStyleAttr, 0);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final int layoutResId = a.getResourceId(
                R.styleable.SearchView_layout, R.layout.sesl_search_view);
        inflater.inflate(layoutResId, this, true);

        mContext = context;

        mSearchSrcTextView = findViewById(R.id.search_src_text);
        mSearchSrcTextView.setSearchView(this);

        mSearchEditFrame = findViewById(R.id.search_edit_frame);
        mSearchPlate = findViewById(R.id.search_plate);
        mSubmitArea = findViewById(R.id.submit_area);
        mSearchButton = findViewById(R.id.search_button);
        mGoButton = findViewById(R.id.search_go_btn);
        mCloseButton = findViewById(R.id.search_close_btn);
        mVoiceButton = findViewById(R.id.search_voice_btn);
        mMoreButton = findViewById(R.id.search_more_btn);
        mBackButton = findViewById(R.id.search_back_btn);
        mCollapsedIcon = findViewById(R.id.search_mag_icon);

        // Set up icons and backgrounds.
        ViewCompat.setBackground(mSearchPlate,
                a.getDrawable(R.styleable.SearchView_queryBackground));
        ViewCompat.setBackground(mSubmitArea,
                a.getDrawable(R.styleable.SearchView_submitBackground));
        mSearchIconResId = a.getResourceId(R.styleable.SearchView_searchIcon, 0);
        mSearchButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_searchIcon));
        mGoButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_goIcon));
        mCloseButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_closeIcon));
        mVoiceButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_voiceIcon));
        mCollapsedIcon.setImageDrawable(a.getDrawable(R.styleable.SearchView_searchIcon));

        mSearchHintIcon = a.getDrawable(R.styleable.SearchView_searchHintIcon);

        TooltipCompat.setTooltipText(mSearchButton,
                mSearchButton.getContentDescription());
        TooltipCompat.setTooltipText(mCloseButton,
                mCloseButton.getContentDescription());
        TooltipCompat.setTooltipText(mGoButton,
                mGoButton.getContentDescription());
        TooltipCompat.setTooltipText(mVoiceButton,
                mVoiceButton.getContentDescription());
        TooltipCompat.setTooltipText(mMoreButton,
                mMoreButton.getContentDescription());
        TooltipCompat.setTooltipText(mBackButton,
                mBackButton.getContentDescription());

        // Extract dropdown layout resource IDs for later use.
        mSuggestionRowLayout = a.getResourceId(R.styleable.SearchView_suggestionRowLayout,
                R.layout.sesl_search_dropdown_item_icons_2line);
        mSuggestionCommitIconResId = a.getResourceId(R.styleable.SearchView_commitIcon, 0);

        mSearchButton.setOnClickListener(mOnClickListener);
        mCloseButton.setOnClickListener(mOnClickListener);
        mGoButton.setOnClickListener(mOnClickListener);
        mVoiceButton.setOnClickListener(mOnClickListener);
        mSearchSrcTextView.setOnClickListener(mOnClickListener);

        mSearchSrcTextView.addTextChangedListener(mTextWatcher);
        mSearchSrcTextView.setOnEditorActionListener(mOnEditorActionListener);
        mSearchSrcTextView.setOnItemClickListener(mOnItemClickListener);
        mSearchSrcTextView.setOnItemSelectedListener(mOnItemSelectedListener);
        mSearchSrcTextView.setOnKeyListener(mTextKeyListener);

        // Inform any listener of focus changes
        mSearchSrcTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnQueryTextFocusChangeListener != null) {
                    mOnQueryTextFocusChangeListener.onFocusChange(SearchView.this, hasFocus);
                }
            }
        });
        setIconifiedByDefault(a.getBoolean(R.styleable.SearchView_iconifiedByDefault, true));

        final int maxWidth = a.getDimensionPixelSize(R.styleable.SearchView_android_maxWidth, -1);
        if (maxWidth != -1) {
            setMaxWidth(maxWidth);
        }

        mDefaultQueryHint = a.getText(R.styleable.SearchView_defaultQueryHint);
        mQueryHint = a.getText(R.styleable.SearchView_queryHint);

        final int imeOptions = a.getInt(R.styleable.SearchView_android_imeOptions, -1);
        if (imeOptions != -1) {
            setImeOptions(imeOptions);
        }

        final int inputType = a.getInt(R.styleable.SearchView_android_inputType, -1);
        if (inputType != -1) {
            setInputType(inputType);
        }

        boolean focusable = true;
        focusable = a.getBoolean(R.styleable.SearchView_android_focusable, focusable);
        setFocusable(focusable);

        mCollapsedIcon.setImageDrawable(a.getDrawable(R.styleable.SearchView_searchIcon));
        mSearchButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_searchIcon));

        mIsLightTheme = SeslMisc.isLightTheme(mContext);

        final Resources resources = mContext.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mBoldTypeface = Typeface.create(resources.getString(R.string.sesl_font_family_medium), Typeface.BOLD);
        } else {
            mBoldTypeface = Typeface.create(resources.getString(R.string.sesl_font_family_regular), Typeface.BOLD);
        }
        mSearchSrcTextView.setTypeface(mBoldTypeface);

        if (mIsLightTheme) {
            if (mSearchPlate.getBackground() == null) {
                mSearchSrcTextView.setTextColor(resources.getColor(R.color.sesl_search_view_text_color));
                mSearchSrcTextView.setHintTextColor(resources.getColor(R.color.sesl_search_view_hint_text_color));

                mGoButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color));
                mCloseButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color));
                mVoiceButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color));
                mMoreButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color));
                if (mBackButton.getDrawable().equals(R.drawable.sesl_search_icon_background_borderless)) {
                    mBackButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color));
                }
                mSearchButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color));
            } else {
                mSearchSrcTextView.setTextColor(resources.getColor(R.color.sesl_search_view_background_text_color_light));
                mSearchSrcTextView.setHintTextColor(resources.getColor(R.color.sesl_search_view_background_hint_text_color_light));

                mGoButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_light));
                mCloseButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_light));
                mVoiceButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_light));
                mMoreButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_light));
                if (mBackButton.getDrawable().equals(R.drawable.sesl_search_icon_background_borderless)) {
                    mBackButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_light));
                }
                mSearchButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_light));
            }
        } else {
            if (mSearchPlate.getBackground() == null) {
                mSearchSrcTextView.setTextColor(resources.getColor(R.color.sesl_search_view_text_color_dark));
                mSearchSrcTextView.setHintTextColor(resources.getColor(R.color.sesl_search_view_hint_text_color_dark));

                mGoButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color_dark));
                mCloseButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color_dark));
                mVoiceButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color_dark));
                mMoreButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color_dark));
                if (mBackButton.getDrawable().equals(R.drawable.sesl_search_icon_background_borderless)) {
                    mBackButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color_dark));
                }
                mSearchButton.setColorFilter(resources.getColor(R.color.sesl_search_view_icon_color_dark));
            } else {
                mSearchSrcTextView.setTextColor(resources.getColor(R.color.sesl_search_view_background_text_color_dark));
                mSearchSrcTextView.setHintTextColor(resources.getColor(R.color.sesl_search_view_background_hint_text_color_dark));

                mGoButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_dark));
                mCloseButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_dark));
                mVoiceButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_dark));
                mMoreButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_dark));
                if (mBackButton.getDrawable().equals(R.drawable.sesl_search_icon_background_borderless)) {
                    mBackButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_dark));
                }
                mSearchButton.setColorFilter(resources.getColor(R.color.sesl_search_view_background_icon_color_dark));
            }
        }

        a.recycle();

        // Save voice intent for later queries/launching
        mVoiceWebSearchIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        mVoiceWebSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mVoiceWebSearchIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        mVoiceAppSearchIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mVoiceAppSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mSVoiceSearchIntent = new Intent(SVI_ACTION);
        mSVoiceSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mSVoiceSearchIntent.putExtra(SVI_INTENT_EXTRA,
                Locale.getDefault().toString());

        mDropDownAnchor = findViewById(mSearchSrcTextView.getDropDownAnchor());
        if (mDropDownAnchor != null) {
            mDropDownAnchor.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    adjustDropDownSizeAndPosition();
                }
            });
        }

        updateViewsVisibility(mIconifiedByDefault);
        updateQueryHint();

        mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        final int SEM_AUTOFILL_ID = SeslTextViewReflector.getField_SEM_AUTOFILL_ID();
        if (SEM_AUTOFILL_ID != 0) {
            SeslTextViewReflector.semSetActionModeMenuItemEnabled(mSearchSrcTextView,
                    SEM_AUTOFILL_ID, false);
        }

        seslCheckMaxFont();
    }

    int getSuggestionRowLayout() {
        return mSuggestionRowLayout;
    }

    int getSuggestionCommitIconResId() {
        return mSuggestionCommitIconResId;
    }

    /**
     * Sets the SearchableInfo for this SearchView. Properties in the SearchableInfo are used
     * to display labels, hints, suggestions, create intents for launching search results screens
     * and controlling other affordances such as a voice button.
     *
     * @param searchable a SearchableInfo can be retrieved from the SearchManager, for a specific
     * activity or a global search provider.
     */
    public void setSearchableInfo(SearchableInfo searchable) {
        mSearchable = searchable;
        if (mSearchable != null) {
            updateSearchAutoComplete();
            updateQueryHint();
        }
        // Cache the voice search capability
        mVoiceButtonEnabled = hasVoiceSearch();

        updateViewsVisibility(isIconified());
    }

    /**
     * Sets the APP_DATA for legacy SearchDialog use.
     * @param appSearchData bundle provided by the app when launching the search dialog
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

    /**
     * Sets the IME options on the query text field.
     *
     * @see TextView#setImeOptions(int)
     * @param imeOptions the options to set on the query text field
     *
     * {@link android.R.attr#imeOptions}
     */
    public void setImeOptions(int imeOptions) {
        mSearchSrcTextView.setImeOptions(imeOptions);
    }

    /**
     * Returns the IME options set on the query text field.
     * @return the ime options
     * @see TextView#setImeOptions(int)
     *
     * {@link android.R.attr#imeOptions}
     */
    public int getImeOptions() {
        return mSearchSrcTextView.getImeOptions();
    }

    /**
     * Sets the input type on the query text field.
     *
     * @see TextView#setInputType(int)
     * @param inputType the input type to set on the query text field
     *
     * {@link android.R.attr#inputType}
     */
    public void setInputType(int inputType) {
        mSearchSrcTextView.setInputType(inputType);
    }

    /**
     * Returns the input type set on the query text field.
     * @return the input type
     *
     * {@link android.R.attr#inputType}
     */
    public int getInputType() {
        return mSearchSrcTextView.getInputType();
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        // If it is not iconified, then give the focus to the text field
        if (!isIconified()) {
            boolean result = mSearchSrcTextView.requestFocus(direction, previouslyFocusedRect);
            if (result) {
                updateViewsVisibility(false);
            }
            return result;
        } else {
            return super.requestFocus(direction, previouslyFocusedRect);
        }
    }

    @Override
    public void clearFocus() {
        mClearingFocus = true;
        super.clearFocus();
        mSearchSrcTextView.clearFocus();
        mSearchSrcTextView.setImeVisibility(false);
        mClearingFocus = false;
    }

    @Override
    public boolean performLongClick() {
        TooltipCompat.seslSetNextTooltipForceBelow(true);
        TooltipCompat.seslSetNextTooltipForceActionBarPosX(true);
        return super.performLongClick();
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param listener the listener object that receives callbacks when the user performs
     * actions in the SearchView such as clicking on buttons or typing a query.
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Sets a listener to inform when the user closes the SearchView.
     *
     * @param listener the listener to call when the user closes the SearchView.
     */
    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    /**
     * Sets a listener to inform when the focus of the query text field changes.
     *
     * @param listener the listener to inform of focus changes.
     */
    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }

    /**
     * Sets a listener to inform when a suggestion is focused or clicked.
     *
     * @param listener the listener to inform of suggestion selection events.
     */
    public void setOnSuggestionListener(OnSuggestionListener listener) {
        mOnSuggestionListener = listener;
    }

    /**
     * Sets a listener to inform when the search button is pressed. This is only
     * relevant when the text field is not visible by default. Calling {@link #setIconified
     * setIconified(false)} can also cause this listener to be informed.
     *
     * @param listener the listener to inform when the search button is clicked or
     * the text field is programmatically de-iconified.
     */
    public void setOnSearchClickListener(OnClickListener listener) {
        mOnSearchClickListener = listener;
    }

    /**
     * Returns the query string currently in the text field.
     *
     * @return the query string
     */
    public CharSequence getQuery() {
        return mSearchSrcTextView.getText();
    }

    /**
     * Sets a query string in the text field and optionally submits the query as well.
     *
     * @param query the query string. This replaces any query text already present in the
     * text field.
     * @param submit whether to submit the query right now or only update the contents of
     * text field.
     */
    public void setQuery(CharSequence query, boolean submit) {
        mSearchSrcTextView.setText(query);
        if (query != null) {
            mSearchSrcTextView.setSelection(mSearchSrcTextView.length());
            mUserQuery = query;
        }

        // If the query is not empty and submit is requested, submit the query
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /**
     * Sets the hint text to display in the query text field. This overrides
     * any hint specified in the {@link SearchableInfo}.
     * <p>
     * This value may be specified as an empty string to prevent any query hint
     * from being displayed.
     *
     * @param hint the hint text to display or {@code null} to clear
     * {@link androidx.appcompat.R.attr#queryHint}
     */
    public void setQueryHint(@Nullable CharSequence hint) {
        mQueryHint = hint;
        updateQueryHint();
    }

    /**
     * Returns the hint text that will be displayed in the query text field.
     * <p>
     * The displayed query hint is chosen in the following order:
     * <ol>
     * <li>Non-null value set with {@link #setQueryHint(CharSequence)}
     * <li>Value specified in XML using {@code app:queryHint}
     * <li>Valid string resource ID exposed by the {@link SearchableInfo} via
     *     {@link SearchableInfo#getHintId()}
     * <li>Default hint provided by the theme against which the view was
     *     inflated
     * </ol>
     *
     *
     *
     * @return the displayed query hint text, or {@code null} if none set
     * {@link androidx.appcompat.R.attr#queryHint}
     */
    @Nullable
    public CharSequence getQueryHint() {
        final CharSequence hint;
        if (mQueryHint != null) {
            hint = mQueryHint;
        } else if (mSearchable != null && mSearchable.getHintId() != 0) {
            hint = getContext().getText(mSearchable.getHintId());
        } else {
            hint = mDefaultQueryHint;
        }
        return hint;
    }

    /**
     * Sets the default or resting state of the search field. If true, a single search icon is
     * shown by default and expands to show the text field and other buttons when pressed. Also,
     * if the default state is iconified, then it collapses to that state when the close button
     * is pressed. Changes to this property will take effect immediately.
     *
     * <p>The default value is true.</p>
     *
     * @param iconified whether the search field should be iconified by default
     *
     * {@link androidx.appcompat.R.attr#iconifiedByDefault}
     */
    public void setIconifiedByDefault(boolean iconified) {
        if (mIconifiedByDefault == iconified) return;
        mIconifiedByDefault = iconified;
        updateViewsVisibility(iconified);
        updateQueryHint();
    }

    /**
     * Returns the default iconified state of the search field.
     * @return
     *
     * {@link androidx.appcompat.R.attr#iconifiedByDefault}
     */
    public boolean isIconfiedByDefault() {
        return mIconifiedByDefault;
    }

    /**
     * Iconifies or expands the SearchView. Any query text is cleared when iconified. This is
     * a temporary state and does not override the default iconified state set by
     * {@link #setIconifiedByDefault(boolean)}. If the default state is iconified, then
     * a false here will only be valid until the user closes the field. And if the default
     * state is expanded, then a true here will only clear the text field and not close it.
     *
     * @param iconify a true value will collapse the SearchView to an icon, while a false will
     * expand it.
     */
    public void setIconified(boolean iconify) {
        if (iconify) {
            onCloseClicked();
        } else {
            onSearchClicked();
        }
    }

    /**
     * Returns the current iconified state of the SearchView.
     *
     * @return true if the SearchView is currently iconified, false if the search field is
     * fully visible.
     */
    public boolean isIconified() {
        return mIconified;
    }

    /**
     * Enables showing a submit button when the query is non-empty. In cases where the SearchView
     * is being used to filter the contents of the current activity and doesn't launch a separate
     * results activity, then the submit button should be disabled.
     *
     * @param enabled true to show a submit button for submitting queries, false if a submit
     * button is not required.
     */
    public void setSubmitButtonEnabled(boolean enabled) {
        mSubmitButtonEnabled = enabled;
        updateViewsVisibility(isIconified());
    }

    /**
     * Returns whether the submit button is enabled when necessary or never displayed.
     *
     * @return whether the submit button is enabled automatically when necessary
     */
    public boolean isSubmitButtonEnabled() {
        return mSubmitButtonEnabled;
    }

    /**
     * Specifies if a query refinement button should be displayed alongside each suggestion
     * or if it should depend on the flags set in the individual items retrieved from the
     * suggestions provider. Clicking on the query refinement button will replace the text
     * in the query text field with the text from the suggestion. This flag only takes effect
     * if a SearchableInfo has been specified with {@link #setSearchableInfo(SearchableInfo)}
     * and not when using a custom adapter.
     *
     * @param enable true if all items should have a query refinement button, false if only
     * those items that have a query refinement flag set should have the button.
     *
     * @see SearchManager#SUGGEST_COLUMN_FLAGS
     * @see SearchManager#FLAG_QUERY_REFINEMENT
     */
    public void setQueryRefinementEnabled(boolean enable) {
        mQueryRefinement = enable;
        if (mSuggestionsAdapter instanceof SuggestionsAdapter) {
            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    enable ? SuggestionsAdapter.REFINE_ALL : SuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    /**
     * Returns whether query refinement is enabled for all items or only specific ones.
     * @return true if enabled for all items, false otherwise.
     */
    public boolean isQueryRefinementEnabled() {
        return mQueryRefinement;
    }

    /**
     * You can set a custom adapter if you wish. Otherwise the default adapter is used to
     * display the suggestions from the suggestions provider associated with the SearchableInfo.
     *
     * @see #setSearchableInfo(SearchableInfo)
     */
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        mSuggestionsAdapter = adapter;

        mSearchSrcTextView.setAdapter(mSuggestionsAdapter);
    }

    /**
     * Returns the adapter used for suggestions, if any.
     * @return the suggestions adapter
     */
    public CursorAdapter getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    /**
     * Makes the view at most this many pixels wide
     *
     * {@link android.R.attr#maxWidth}
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
    }

    /**
     * Gets the specified maximum width in pixels, if set. Returns zero if
     * no maximum width was specified.
     * @return the maximum width of the view
     *
     * {@link android.R.attr#maxWidth}
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Let the standard measurements take effect in iconified state.
        if (isIconified()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {
            case MeasureSpec.AT_MOST:
                // If there is an upper limit, don't exceed maximum width (explicit or implicit)
                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                }
                break;
            case MeasureSpec.EXACTLY:
                // If an exact width is specified, still don't exceed any specified maximum width
                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                // Use maximum width, if specified, else preferred width
                width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
                break;
        }
        widthMode = MeasureSpec.EXACTLY;

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                height = Math.min(getPreferredHeight(), height);
                break;
            case MeasureSpec.UNSPECIFIED:
                height = getPreferredHeight();
                break;
        }
        heightMode = MeasureSpec.EXACTLY;

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode),
                MeasureSpec.makeMeasureSpec(height, heightMode));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            // Expand mSearchSrcTextView touch target to be the height of the parent in order to
            // allow it to be up to 48dp.
            getChildBoundsWithinSearchView(mSearchSrcTextView, mSearchSrcTextViewBounds);
            mSearchSrtTextViewBoundsExpanded.set(
                    mSearchSrcTextViewBounds.left, 0, mSearchSrcTextViewBounds.right, bottom - top);
            if (mTouchDelegate == null) {
                mTouchDelegate = new UpdatableTouchDelegate(mSearchSrtTextViewBoundsExpanded,
                        mSearchSrcTextViewBounds, mSearchSrcTextView);
                setTouchDelegate(mTouchDelegate);
            } else {
                mTouchDelegate.setBounds(mSearchSrtTextViewBoundsExpanded, mSearchSrcTextViewBounds);
            }
        }
    }

    private void getChildBoundsWithinSearchView(View view, Rect rect) {
        view.getLocationInWindow(mTemp);
        getLocationInWindow(mTemp2);
        final int top = mTemp[1] - mTemp2[1];
        final int left = mTemp[0] - mTemp2[0];
        rect.set(left, top, left + view.getWidth(), top + view.getHeight());
    }

    private int getPreferredWidth() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.sesl_search_view_preferred_width);
    }

    private int getPreferredHeight() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.sesl_search_view_preferred_height);
    }

    private void updateViewsVisibility(final boolean collapsed) {
        mIconified = collapsed;
        // Visibility of views that are visible when collapsed
        final int visCollapsed = collapsed ? VISIBLE : GONE;
        // Is there text in the query
        final boolean hasText = !TextUtils.isEmpty(mSearchSrcTextView.getText());

        mSearchButton.setVisibility(visCollapsed);
        updateSubmitButton(hasText);
        mSearchEditFrame.setVisibility(collapsed ? GONE : VISIBLE);

        mCollapsedIcon.setVisibility(GONE);

        updateCloseButton();
        updateVoiceButton(!hasText);
        updateSubmitArea();
    }

    private boolean hasVoiceSearch() {
        if (mSearchable != null && mSearchable.getVoiceSearchEnabled()) {
            Intent testIntent = null;
            if (mSearchable.getVoiceSearchLaunchWebSearch()) {
                testIntent = mVoiceWebSearchIntent;
            } else if (mSearchable.getVoiceSearchLaunchRecognizer()) {
                if (mUseSVI) {
                    testIntent = mSVoiceSearchIntent;
                } else {
                    testIntent = mVoiceAppSearchIntent;
                }
            }
            if (testIntent != null) {
                ResolveInfo ri = getContext().getPackageManager().resolveActivity(testIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return ri != null;
            }
        }
        return false;
    }

    private boolean isSubmitAreaEnabled() {
        return (mSubmitButtonEnabled || mVoiceButtonEnabled) && !isIconified();
    }

    private void updateSubmitButton(boolean hasText) {
        int visibility = GONE;
        if (mSubmitButtonEnabled && isSubmitAreaEnabled() && hasFocus()
                && (hasText || !mVoiceButtonEnabled)) {
            visibility = VISIBLE;
        }
        mGoButton.setVisibility(visibility);
    }

    private void updateSubmitArea() {
        int visibility = GONE;
        if (isSubmitAreaEnabled()
                && (mGoButton.getVisibility() == VISIBLE
                        || mVoiceButton.getVisibility() == VISIBLE)) {
            visibility = VISIBLE;
        }
        mSubmitArea.setVisibility(visibility);
    }

    private void updateCloseButton() {
        final boolean hasText = !TextUtils.isEmpty(mSearchSrcTextView.getText());
        mCloseButton.setVisibility(hasText ? VISIBLE : GONE);
        final Drawable closeButtonImg = mCloseButton.getDrawable();
        if (closeButtonImg != null){
            closeButtonImg.setState(hasText ? ENABLED_STATE_SET : EMPTY_STATE_SET);
        }
    }

    private void postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable);
    }

    void updateFocusedState() {
        final boolean focused = mSearchSrcTextView.hasFocus();
        final int[] stateSet = focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET;
        final Drawable searchPlateBg = mSearchPlate.getBackground();
        if (searchPlateBg != null) {
            searchPlateBg.setState(stateSet);
        }
        final Drawable submitAreaBg = mSubmitArea.getBackground();
        if (submitAreaBg != null) {
            submitAreaBg.setState(stateSet);
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mUpdateDrawableStateRunnable);
        post(mReleaseCursorRunnable);
        super.onDetachedFromWindow();
    }

    /**
     * Called by the SuggestionsAdapter
     */
    void onQueryRefine(CharSequence queryText) {
        setQuery(queryText);
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mSearchButton) {
                onSearchClicked();
            } else if (v == mCloseButton) {
                onCloseClicked();
            } else if (v == mGoButton) {
                onSubmitQuery();
            } else if (v == mVoiceButton) {
                onVoiceClicked();
            } else if (v == mSearchSrcTextView) {
                forceSuggestionQuery();
            }
        }
    };

    /**
     * React to the user typing "enter" or other hardwired keys while typing in
     * the search box. This handles these special keys while the edit box has
     * focus.
     */
    View.OnKeyListener mTextKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (mContext.getPackageManager().hasSystemFeature("com.sec.feature.folder_type")) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    imm.viewClicked(v);
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                }
            }

            // guard against possible race conditions
            if (mSearchable == null) {
                return false;
            }

            if (DBG) {
                Log.d(LOG_TAG, "mTextListener.onKey(" + keyCode + "," + event + "), selection: "
                        + mSearchSrcTextView.getListSelection());
            }

            // If a suggestion is selected, handle enter, search key, and action keys
            // as presses on the selected suggestion
            if (mSearchSrcTextView.isPopupShowing()
                    && mSearchSrcTextView.getListSelection() != ListView.INVALID_POSITION) {
                return onSuggestionsKey(v, keyCode, event);
            }

            // If there is text in the query box, handle enter, and action keys
            // The search key is handled by the dialog's onKeyDown().
            if (!mSearchSrcTextView.isEmpty() && event.hasNoModifiers()) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        v.cancelLongPress();

                        // Launch as a regular search.
                        launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, mSearchSrcTextView.getText()
                                .toString());
                        return true;
                    }
                }
            }
            return false;
        }
    };

    /**
     * React to the user typing while in the suggestions list. First, check for
     * action keys. If not handled, try refocusing regular characters into the
     * EditText.
     */
    boolean onSuggestionsKey(View v, int keyCode, KeyEvent event) {
        // guard against possible race conditions (late arrival after dismiss)
        if (mSearchable == null) {
            return false;
        }
        if (mSuggestionsAdapter == null) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.hasNoModifiers()) {
            // First, check for enter or search (both of which we'll treat as a
            // "click")
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH
                    || keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                int position = mSearchSrcTextView.getListSelection();
                return onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
            }

            // Next, check for left/right moves, which we use to "return" the
            // user to the edit view
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // give "focus" to text editor, with cursor at the beginning if
                // left key, at end if right key
                // TODO: Reverse left/right for right-to-left languages, e.g.
                // Arabic
                int selPoint = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : mSearchSrcTextView
                        .length();
                mSearchSrcTextView.setSelection(selPoint);
                mSearchSrcTextView.setListSelection(0);
                mSearchSrcTextView.clearListSelection();
                mSearchSrcTextView.ensureImeVisible();

                return true;
            }

            // Next, check for an "up and out" move
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && 0 == mSearchSrcTextView.getListSelection()) {
                // TODO: restoreUserQuery();
                // let ACTV complete the move
                return false;
            }
        }
        return false;
    }

    private CharSequence getDecoratedHint(CharSequence hintText) {
        if (mIconifiedByDefault) {
            return hintText;
        }

        final int textSize = (int) (mSearchSrcTextView.getTextSize() * 1.25);
        mSearchHintIcon.setBounds(0, 0, textSize, textSize);

        final SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.setSpan(new ImageSpan(mSearchHintIcon), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(hintText);
        return ssb;
    }

    private void updateQueryHint() {
        final CharSequence hint = getQueryHint();
        mSearchSrcTextView.setHint(hint == null ? "" : hint);
    }

    /**
     * Updates the auto-complete text view.
     */
    private void updateSearchAutoComplete() {
        mSearchSrcTextView.setThreshold(mSearchable.getSuggestThreshold());
        mSearchSrcTextView.setImeOptions(mSearchable.getImeOptions());
        int inputType = mSearchable.getInputType();
        // We only touch this if the input type is set up for text (which it almost certainly
        // should be, in the case of search!)
        if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            // The existence of a suggestions authority is the proxy for "suggestions
            // are available here"
            inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            if (mSearchable.getSuggestAuthority() != null) {
                inputType |= InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            }
        }
        mSearchSrcTextView.setInputType(inputType);
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.changeCursor(null);
        }
        // attach the suggestions adapter, if suggestions are available
        // The existence of a suggestions authority is the proxy for "suggestions available here"
        if (mSearchable.getSuggestAuthority() != null) {
            mSuggestionsAdapter = new SuggestionsAdapter(getContext(),
                    this, mSearchable, mOutsideDrawablesCache);
            mSearchSrcTextView.setAdapter(mSuggestionsAdapter);
            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    mQueryRefinement ? SuggestionsAdapter.REFINE_ALL
                    : SuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    /**
     * Update the visibility of the voice button.  There are actually two voice search modes,
     * either of which will activate the button.
     * @param empty whether the search query text field is empty. If it is, then the other
     * criteria apply to make the voice button visible.
     */
    private void updateVoiceButton(boolean empty) {
        int visibility = GONE;
        if (mVoiceButtonEnabled && !isIconified() && empty) {
            visibility = VISIBLE;
            mGoButton.setVisibility(GONE);
        }
        mVoiceButton.setVisibility(visibility);
    }

    private final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {

        /**
         * Called when the input method default action key is pressed.
         */
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            onSubmitQuery();
            return true;
        }
    };

    void onTextChanged(CharSequence newText) {
        CharSequence text = mSearchSrcTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        updateSubmitButton(hasText);
        updateVoiceButton(!hasText);
        updateCloseButton();
        updateSubmitArea();
        if (!TextUtils.equals(newText, mOldQueryText)) {
            mOldQueryText = newText.toString();
            if (mOnQueryChangeListener != null) {
                mOnQueryChangeListener.onQueryTextChange(newText.toString());
            }
        }

    }

    void onSubmitQuery() {
        CharSequence query = mSearchSrcTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null
                    || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString());
                }
                mSearchSrcTextView.setImeVisibility(false);
                dismissSuggestions();
            }
        }
    }

    private void dismissSuggestions() {
        mSearchSrcTextView.dismissDropDown();
    }

    void onCloseClicked() {
        CharSequence text = mSearchSrcTextView.getText();
        if (TextUtils.isEmpty(text)) {
            if (mIconifiedByDefault) {
                // If the app doesn't override the close behavior
                if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
                    // hide the keyboard and remove focus
                    clearFocus();
                    // collapse the search field
                    updateViewsVisibility(true);
                }
            }
        } else {
            mSearchSrcTextView.setText("");
            mSearchSrcTextView.requestFocus();
            if (SeslInputMethodManagerReflector.isAccessoryKeyboardState(mImm) != 0) {
                mSearchSrcTextView.setImeVisibility(false);
            } else {
                mSearchSrcTextView.setImeVisibility(true);
            }
        }
    }

    void onSearchClicked() {
        updateViewsVisibility(false);
        mSearchSrcTextView.requestFocus();
        if (SeslInputMethodManagerReflector.isAccessoryKeyboardState(mImm) != 0) {
            mSearchSrcTextView.setImeVisibility(false);
        } else {
            mSearchSrcTextView.setImeVisibility(true);
        }
        if (mOnSearchClickListener != null) {
            mOnSearchClickListener.onClick(this);
        }
    }

    void onVoiceClicked() {
        // guard against possible race conditions
        if (mSearchable == null) {
            return;
        }
        SearchableInfo searchable = mSearchable;
        try {
            if (mUseSVI) {
                if (searchable.getVoiceSearchLaunchWebSearch()) {
                    Intent webSearchIntent = createVoiceWebSearchIntent(mVoiceWebSearchIntent,
                            searchable);
                    mContext.startActivity(webSearchIntent);
                } else if (searchable.getVoiceSearchLaunchRecognizer()) {
                    Intent sVoiceIntent = createSVoiceSearchIntent(mSVoiceSearchIntent,
                            searchable);
                    mContext.startActivity(sVoiceIntent);
                }
            } else {
                if (searchable.getVoiceSearchLaunchWebSearch()) {
                    Intent webSearchIntent = createVoiceWebSearchIntent(mVoiceWebSearchIntent,
                            searchable);
                    mContext.startActivity(webSearchIntent);
                } else if (searchable.getVoiceSearchLaunchRecognizer()) {
                    Intent appSearchIntent = createVoiceAppSearchIntent(mVoiceAppSearchIntent,
                            searchable);
                    mContext.startActivity(appSearchIntent);
                }
            }
        } catch (ActivityNotFoundException e) {
            // Should not happen, since we check the availability of
            // voice search before showing the button. But just in case...
            Log.w(LOG_TAG, "Could not find voice search activity");
        }
    }

    void onTextFocusChanged() {
        updateViewsVisibility(isIconified());
        // Delayed update to make sure that the focus has settled down and window focus changes
        // don't affect it. A synchronous update was not working.
        postUpdateFocusedState();
        if (mSearchSrcTextView.hasFocus()) {
            forceSuggestionQuery();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (SeslInputMethodManagerReflector.isAccessoryKeyboardState(mImm) == 0) {
            postUpdateFocusedState();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActionViewCollapsed() {
        setQuery("", false);
        clearFocus();
        updateViewsVisibility(true);
        mSearchSrcTextView.setImeOptions(mCollapsedImeOptions);
        mExpandedInActionView = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActionViewExpanded() {
        if (mExpandedInActionView) return;

        mExpandedInActionView = true;
        mCollapsedImeOptions = mSearchSrcTextView.getImeOptions();
        mSearchSrcTextView.setImeOptions(mCollapsedImeOptions | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mSearchSrcTextView.setText("");
        setIconified(false);
    }

    static class SavedState extends AbsSavedState {
        boolean isIconified;

        SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            isIconified = (Boolean) source.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeValue(isIconified);
        }

        @Override
        public String toString() {
            return "SearchView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " isIconified=" + isIconified + "}";
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isIconified = isIconified();
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        updateViewsVisibility(ss.isIconified);
        requestLayout();
    }

    void adjustDropDownSizeAndPosition() {
        if (mDropDownAnchor.getWidth() > 1) {
            int anchorPadding = 0;
            Rect dropDownPadding = new Rect();
            final boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
            int iconOffset = 0;
            if (mSearchSrcTextView.getDropDownBackground() != null) {
                mSearchSrcTextView.getDropDownBackground().getPadding(dropDownPadding);
            }
            int offset;
            if (isLayoutRtl) {
                offset = - dropDownPadding.left;
            } else {
                offset = anchorPadding - (dropDownPadding.left + iconOffset);
            }
            mSearchSrcTextView.setDropDownHorizontalOffset(offset);
            final int width = mDropDownAnchor.getWidth() + dropDownPadding.left
                    + dropDownPadding.right + iconOffset + anchorPadding;
            mSearchSrcTextView.setDropDownWidth(width);
            if (mSearchSrcTextView.isPopupShowing()) {
                mSearchSrcTextView.showDropDown();
            }
        }
    }

    boolean onItemClicked(int position, int actionKey, String actionMsg) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionClick(position)) {
            launchSuggestion(position, KeyEvent.KEYCODE_UNKNOWN, null);
            mSearchSrcTextView.setImeVisibility(false);
            dismissSuggestions();
            return true;
        }
        return false;
    }

    boolean onItemSelected(int position) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionSelect(position)) {
            rewriteQueryFromSuggestion(position);
            return true;
        }
        return false;
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        /**
         * Implements OnItemClickListener
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemClick() position " + position);
            onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
        }
    };

    private final OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {

        /**
         * Implements OnItemSelectedListener
         */
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemSelected() position " + position);
            SearchView.this.onItemSelected(position);
        }

        /**
         * Implements OnItemSelectedListener
         */
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            if (DBG)
                Log.d(LOG_TAG, "onNothingSelected()");
        }
    };

    /**
     * Query rewriting.
     */
    private void rewriteQueryFromSuggestion(int position) {
        CharSequence oldQuery = mSearchSrcTextView.getText();
        Cursor c = mSuggestionsAdapter.getCursor();
        if (c == null) {
            return;
        }
        if (c.moveToPosition(position)) {
            // Get the new query from the suggestion.
            CharSequence newQuery = mSuggestionsAdapter.convertToString(c);
            if (newQuery != null) {
                // The suggestion rewrites the query.
                // Update the text field, without getting new suggestions.
                setQuery(newQuery);
            } else {
                // The suggestion does not rewrite the query, restore the user's query.
                setQuery(oldQuery);
            }
        } else {
            // We got a bad position, restore the user's query.
            setQuery(oldQuery);
        }
    }

    /**
     * Launches an intent based on a suggestion.
     *
     * @param position The index of the suggestion to create the intent from.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return true if a successful launch, false if could not (e.g. bad position).
     */
    private boolean launchSuggestion(int position, int actionKey, String actionMsg) {
        Cursor c = mSuggestionsAdapter.getCursor();
        if ((c != null) && c.moveToPosition(position)) {

            Intent intent = createIntentFromSuggestion(c, actionKey, actionMsg);

            // launch the intent
            launchIntent(intent);

            return true;
        }
        return false;
    }

    /**
     * Launches an intent, including any special intent handling.
     */
    private void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            // If the intent was created from a suggestion, it will always have an explicit
            // component here.
            getContext().startActivity(intent);
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Failed launch activity: " + intent, ex);
        }
    }

    /**
     * Sets the text in the query box, without updating the suggestions.
     */
    private void setQuery(CharSequence query) {
        mSearchSrcTextView.setText(query);
        // Move the cursor to the end
        mSearchSrcTextView.setSelection(TextUtils.isEmpty(query) ? 0 : query.length());
    }

    void launchQuerySearch(int actionKey, String actionMsg, String query) {
        String action = Intent.ACTION_SEARCH;
        Intent intent = createIntent(action, null, null, query, actionKey, actionMsg);
        getContext().startActivity(intent);
    }

    /**
     * Constructs an intent from the given information and the search dialog state.
     *
     * @param action Intent action.
     * @param data Intent data, or <code>null</code>.
     * @param extraData Data for {@link SearchManager#EXTRA_DATA_KEY} or <code>null</code>.
     * @param query Intent query, or <code>null</code>.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return The intent.
     */
    private Intent createIntent(String action, Uri data, String extraData, String query,
            int actionKey, String actionMsg) {
        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want. We don't want to do this in in-app search though,
        // as it can be destructive to the activity stack.
        if (data != null) {
            intent.setData(data);
        }
        intent.putExtra(SearchManager.USER_QUERY, mUserQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey);
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg);
        }
        intent.setComponent(mSearchable.getSearchActivity());
        return intent;
    }

    /**
     * Create and return an Intent that can launch the voice search activity for web search.
     */
    private Intent createVoiceWebSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        Intent voiceIntent = new Intent(baseIntent);
        ComponentName searchActivity = searchable.getSearchActivity();
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());
        return voiceIntent;
    }

    /**
     * Create and return an Intent that can launch the voice search activity, perform a specific
     * voice transcription, and forward the results to the searchable activity.
     *
     * @param baseIntent The voice app search intent to start from
     * @return A completely-configured intent ready to send to the voice search activity
     */
    private Intent createVoiceAppSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        ComponentName searchActivity = searchable.getSearchActivity();

        // create the necessary intent to set up a search-and-forward operation
        // in the voice search system.   We have to keep the bundle separate,
        // because it becomes immutable once it enters the PendingIntent
        Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
        queryIntent.setComponent(searchActivity);

        PendingIntent pending;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                    PendingIntent.FLAG_ONE_SHOT | FLAG_MUTABLE);
        } else {
            pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                    PendingIntent.FLAG_ONE_SHOT);
        }

        // Now set up the bundle that will be inserted into the pending intent
        // when it's time to do the search.  We always build it here (even if empty)
        // because the voice search activity will always need to insert "QUERY" into
        // it anyway.
        Bundle queryExtras = new Bundle();
        if (mAppSearchData != null) {
            queryExtras.putParcelable(SearchManager.APP_DATA, mAppSearchData);
        }

        // Now build the intent to launch the voice search.  Add all necessary
        // extras to launch the voice recognizer, and then all the necessary extras
        // to forward the results to the searchable activity
        Intent voiceIntent = new Intent(baseIntent);

        // Add all of the configuration options supplied by the searchable's metadata
        String languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
        String prompt = null;
        String language = null;
        int maxResults = 1;

        Resources resources = getResources();
        if (searchable.getVoiceLanguageModeId() != 0) {
            languageModel = resources.getString(searchable.getVoiceLanguageModeId());
        }
        if (searchable.getVoicePromptTextId() != 0) {
            prompt = resources.getString(searchable.getVoicePromptTextId());
        }
        if (searchable.getVoiceLanguageId() != 0) {
            language = resources.getString(searchable.getVoiceLanguageId());
        }
        if (searchable.getVoiceMaxResults() != 0) {
            maxResults = searchable.getVoiceMaxResults();
        }

        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());

        // Add the values that configure forwarding the results
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras);

        return voiceIntent;
    }

    /**
     * When a particular suggestion has been selected, perform the various lookups required
     * to use the suggestion.  This includes checking the cursor for suggestion-specific data,
     * and/or falling back to the XML for defaults;  It also creates REST style Uri data when
     * the suggestion includes a data id.
     *
     * @param c The suggestions cursor, moved to the row of the user's selection
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return An intent for the suggestion at the cursor's position.
     */
    private Intent createIntentFromSuggestion(Cursor c, int actionKey, String actionMsg) {
        try {
            // use specific action if supplied, or default action if supplied, or fixed default
            String action = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);

            if (action == null) {
                action = mSearchable.getSuggestIntentAction();
            }
            if (action == null) {
                action = Intent.ACTION_SEARCH;
            }

            // use specific data if supplied, or default data if supplied
            String data = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
            if (data == null) {
                data = mSearchable.getSuggestIntentData();
            }
            // then, if an ID was provided, append it.
            if (data != null) {
                String id = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
                if (id != null) {
                    data = data + "/" + Uri.encode(id);
                }
            }
            Uri dataUri = (data == null) ? null : Uri.parse(data);

            String query = getColumnString(c, SearchManager.SUGGEST_COLUMN_QUERY);
            String extraData = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);

            return createIntent(action, dataUri, extraData, query, actionKey, actionMsg);
        } catch (RuntimeException e ) {
            int rowNum;
            try {
                rowNum = c.getPosition();
            } catch (RuntimeException e2 ) {
                rowNum = -1;
            }
            Log.w(LOG_TAG, "Search suggestions cursor at row " + rowNum +
                            " returned exception.", e);
            return null;
        }
    }

    void forceSuggestionQuery() {
        if (Build.VERSION.SDK_INT >= 29) {
            mSearchSrcTextView.refreshAutoCompleteResults();
        } else {
            PRE_API_29_HIDDEN_METHOD_INVOKER.doBeforeTextChanged(mSearchSrcTextView);
            PRE_API_29_HIDDEN_METHOD_INVOKER.doAfterTextChanged(mSearchSrcTextView);
        }
    }

    static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Callback to watch the text field for empty/non-empty
     */
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int before, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start,
                int before, int after) {
            SearchView.this.onTextChanged(s);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private static class UpdatableTouchDelegate extends TouchDelegate {
        /**
         * View that should receive forwarded touch events
         */
        private final View mDelegateView;

        /**
         * Bounds in local coordinates of the containing view that should be mapped to the delegate
         * view. This rect is used for initial hit testing.
         */
        private final Rect mTargetBounds;

        /**
         * Bounds in local coordinates of the containing view that are actual bounds of the delegate
         * view. This rect is used for event coordinate mapping.
         */
        private final Rect mActualBounds;

        /**
         * mTargetBounds inflated to include some slop. This rect is to track whether the motion events
         * should be considered to be be within the delegate view.
         */
        private final Rect mSlopBounds;

        private final int mSlop;

        /**
         * True if the delegate had been targeted on a down event (intersected mTargetBounds).
         */
        private boolean mDelegateTargeted;

        public UpdatableTouchDelegate(Rect targetBounds, Rect actualBounds, View delegateView) {
            super(targetBounds, delegateView);
            mSlop = ViewConfiguration.get(delegateView.getContext()).getScaledTouchSlop();
            mTargetBounds = new Rect();
            mSlopBounds = new Rect();
            mActualBounds = new Rect();
            setBounds(targetBounds, actualBounds);
            mDelegateView = delegateView;
        }

        public void setBounds(Rect desiredBounds, Rect actualBounds) {
            mTargetBounds.set(desiredBounds);
            mSlopBounds.set(desiredBounds);
            mSlopBounds.inset(-mSlop, -mSlop);
            mActualBounds.set(actualBounds);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            boolean sendToDelegate = false;
            boolean hit = true;
            boolean handled = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mTargetBounds.contains(x, y)) {
                        mDelegateTargeted = true;
                        sendToDelegate = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_MOVE:
                    sendToDelegate = mDelegateTargeted;
                    if (sendToDelegate) {
                        if (!mSlopBounds.contains(x, y)) {
                            hit = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    sendToDelegate = mDelegateTargeted;
                    mDelegateTargeted = false;
                    break;
            }
            if (sendToDelegate) {
                if (hit && !mActualBounds.contains(x, y)) {
                    // Offset event coordinates to be in the center of the target view since we
                    // are within the targetBounds, but not inside the actual bounds of
                    // mDelegateView
                    event.setLocation(mDelegateView.getWidth() / 2,
                            mDelegateView.getHeight() / 2);
                } else {
                    // Offset event coordinates to the target view coordinates.
                    event.setLocation(x - mActualBounds.left, y - mActualBounds.top);
                }

                handled = mDelegateView.dispatchTouchEvent(event);
            }
            return handled;
        }
    }

    /**
     * Local subclass for AutoCompleteTextView.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static class SearchAutoComplete extends AppCompatAutoCompleteTextView {

        private int mThreshold;
        private SearchView mSearchView;

        @Nullable
        private OnPrivateImeCommandListener mOnAppPrivateCommandListener = null;

        private boolean mForceNotCallShowSoftInput;
        private boolean mHasPendingShowSoftInputRequest;
        final Runnable mRunShowSoftInputIfNecessary = new Runnable() {
            @Override
            public void run() {
                showSoftInputIfNecessary();
            }
        };

        public SearchAutoComplete(Context context) {
            this(context, null);
        }

        public SearchAutoComplete(Context context, AttributeSet attrs) {
            this(context, attrs, R.attr.autoCompleteTextViewStyle);
        }

        public SearchAutoComplete(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            mThreshold = getThreshold();
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    getSearchViewTextMinWidthDp(), metrics));
        }

        void setSearchView(SearchView searchView) {
            mSearchView = searchView;
        }

        @Override
        public void setThreshold(int threshold) {
            super.setThreshold(threshold);
            mThreshold = threshold;
        }

        /**
         * Returns true if the text field is empty, or contains only whitespace.
         */
        boolean isEmpty() {
            return TextUtils.getTrimmedLength(getText()) == 0;
        }

        /**
         * We override this method to avoid replacing the query box text when a
         * suggestion is clicked.
         */
        @Override
        protected void replaceText(CharSequence text) {
        }

        /**
         * We override this method to avoid an extra onItemClick being called on
         * the drop-down's OnItemClickListener by
         * {@link AutoCompleteTextView#onKeyUp(int, KeyEvent)} when an item is
         * clicked with the trackball.
         */
        @Override
        public void performCompletion() {
        }

        /**
         * We override this method to be sure and show the soft keyboard if
         * appropriate when the TextView has focus.
         */
        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);

            if (hasWindowFocus && mSearchView.hasFocus() && getVisibility() == VISIBLE) {
                // Since InputMethodManager#onPostWindowFocus() will be called after this callback,
                // it is a bit too early to call InputMethodManager#showSoftInput() here. We still
                // need to wait until the system calls back onCreateInputConnection() to call
                // InputMethodManager#showSoftInput().
                mHasPendingShowSoftInputRequest = true;

                // If in landscape mode, then make sure that the ime is in front of the dropdown.
                if (isLandscapeMode(getContext())) {
                    ensureImeVisible();
                }
            }
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            mSearchView.onTextFocusChanged();
        }

        /**
         * We override this method so that we can allow a threshold of zero,
         * which ACTV does not.
         */
        @Override
        public boolean enoughToFilter() {
            return mThreshold <= 0 || super.enoughToFilter();
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            return super.onKeyPreIme(keyCode, event);
        }

        /**
         * Get minimum width of the search view text entry area.
         */
        private int getSearchViewTextMinWidthDp() {
            final Configuration config = getResources().getConfiguration();
            final int widthDp = config.screenWidthDp;
            final int heightDp = config.screenHeightDp;

            if (widthDp >= 960 && heightDp >= 720
                    && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 256;
            } else if (widthDp >= 600 || (widthDp >= 640 && heightDp >= 480)) {
                return 192;
            }
            return 160;
        }

        /**
         * We override {@link View#onCreateInputConnection(EditorInfo)} as a signal to schedule a
         * pending {@link InputMethodManager#showSoftInput(View, int)} request (if any).
         */
        @Override
        public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
            final InputConnection ic = super.onCreateInputConnection(editorInfo);
            if (mHasPendingShowSoftInputRequest) {
                removeCallbacks(mRunShowSoftInputIfNecessary);
                post(mRunShowSoftInputIfNecessary);
            }
            return ic;
        }

        @Override
        public boolean onPrivateIMECommand(String action, Bundle data) {
            if (mOnAppPrivateCommandListener != null) {
                return mOnAppPrivateCommandListener.onPrivateIMECommand(action, data);
            }
            return super.onPrivateIMECommand(action, data);
        }

        void showSoftInputIfNecessary() {
            if (!mForceNotCallShowSoftInput && mHasPendingShowSoftInputRequest) {
                final InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(this, 0);
                mHasPendingShowSoftInputRequest = false;
            }
        }

        void setImeVisibility(final boolean visible) {
            final InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!visible) {
                mHasPendingShowSoftInputRequest = false;
                removeCallbacks(mRunShowSoftInputIfNecessary);
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
                return;
            }

            if (imm.isActive(this)) {
                // This means that SearchAutoComplete is already connected to the IME.
                // InputMethodManager#showSoftInput() is guaranteed to pass client-side focus check.
                mHasPendingShowSoftInputRequest = false;
                removeCallbacks(mRunShowSoftInputIfNecessary);
                imm.showSoftInput(this, 0);
                return;
            }

            // Otherwise, InputMethodManager#showSoftInput() should be deferred after
            // onCreateInputConnection().
            mHasPendingShowSoftInputRequest = true;
        }

        void ensureImeVisible() {
            if (Build.VERSION.SDK_INT >= 29) {
                setInputMethodMode(INPUT_METHOD_NEEDED);
                if (getFilter() != null && enoughToFilter()) {
                    showDropDown();
                }
            } else {
                PRE_API_29_HIDDEN_METHOD_INVOKER.ensureImeVisible(this);
            }
        }

        void setNotCallShowSoftInput(boolean notCall) {
            mForceNotCallShowSoftInput = notCall;
        }

        public void seslSetOnPrivateImeCommandListener(@Nullable OnPrivateImeCommandListener listener) {
            mOnAppPrivateCommandListener = listener;
        }
    }

    private static class PreQAutoCompleteTextViewReflector {
        private Method mDoBeforeTextChanged = null;
        private Method mDoAfterTextChanged = null;
        private Method mEnsureImeVisible = null;


        @SuppressWarnings("JavaReflectionMemberAccess")
        @SuppressLint({"DiscouragedPrivateApi", "SoonBlockedPrivateApi"})
        PreQAutoCompleteTextViewReflector() {
            preApi29Check();
            try {
                mDoBeforeTextChanged = AutoCompleteTextView.class
                        .getDeclaredMethod("doBeforeTextChanged");
                mDoBeforeTextChanged.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Ah well.
            }
            try {
                mDoAfterTextChanged = AutoCompleteTextView.class
                        .getDeclaredMethod("doAfterTextChanged");
                mDoAfterTextChanged.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Ah well.
            }
            try {
                mEnsureImeVisible = AutoCompleteTextView.class
                        .getMethod("ensureImeVisible", boolean.class);
                mEnsureImeVisible.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Ah well.
            }
        }

        void doBeforeTextChanged(AutoCompleteTextView view) {
            preApi29Check();
            if (mDoBeforeTextChanged != null) {
                try {
                    mDoBeforeTextChanged.invoke(view);
                } catch (Exception e) {
                }
            }
        }

        void doAfterTextChanged(AutoCompleteTextView view) {
            preApi29Check();
            if (mDoAfterTextChanged != null) {
                try {
                    mDoAfterTextChanged.invoke(view);
                } catch (Exception e) {
                }
            }
        }

        void ensureImeVisible(AutoCompleteTextView view) {
            preApi29Check();
            if (mEnsureImeVisible != null) {
                try {
                    mEnsureImeVisible.invoke(view, /* visible = */ true);
                } catch (Exception e) {
                }
            }
        }

        private static void preApi29Check() {
            if (Build.VERSION.SDK_INT >= 29) {
                throw new UnsupportedClassVersionError(
                        "This function can only be used for API Level < 29.");
            }
        }
    }

    @Override
    public void setBackground(Drawable background) {
        if (mSearchPlate != null) {
            ViewCompat.setBackground(mSearchPlate, background);
        }
    }

    @Override
    public void setBackgroundResource(int resid) {
        if (mSearchPlate != null) {
            ViewCompat.setBackground(mSearchPlate,
                    getContext().getResources().getDrawable(resid));
        }
    }

    @Override
    public void setElevation(float elevation) {
        if (mSearchPlate != null) {
            ViewCompat.setElevation(mSearchPlate, elevation);
        }
    }

    public AutoCompleteTextView seslGetAutoCompleteView() {
        return mSearchSrcTextView;
    }

    public ImageView seslGetUpButton() {
        return mBackButton;
    }

    public ImageView seslGetOverflowMenuButton() {
        return mMoreButton;
    }

    public void seslSetUpButtonIcon(Drawable drawable) {
        if (mBackButton != null) {
            mBackButton.setImageDrawable(drawable);
        }
    }

    public void seslSetOverflowMenuButtonIcon(Drawable drawable) {
        if (mMoreButton != null) {
            mMoreButton.setImageDrawable(drawable);
        }
    }

    public void seslSetUpButtonVisibility(int visibility) {
        if (mBackButton != null) {
            mBackButton.setVisibility(visibility);
        }
    }

    public void seslSetOverflowMenuButtonVisibility(int visibility) {
        if (mMoreButton != null) {
            mMoreButton.setVisibility(visibility);
        }
    }

    public void seslSetOnUpButtonClickListener(View.OnClickListener listener) {
        if (mBackButton != null) {
            mBackButton.setOnClickListener(listener);
        }
    }

    public void seslSetOnOverflowMenuButtonClickListener(View.OnClickListener listener) {
        if (mMoreButton != null) {
            mMoreButton.setOnClickListener(listener);
        }
    }

    private Intent createSVoiceSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        ComponentName searchActivity = searchable.getSearchActivity();

        Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
        queryIntent.setComponent(searchActivity);

        PendingIntent pending;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                    PendingIntent.FLAG_ONE_SHOT | FLAG_MUTABLE);
        } else {
            pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                    PendingIntent.FLAG_ONE_SHOT);
        }

        Bundle queryExtras = new Bundle();
        if (mAppSearchData != null) {
            queryExtras.putParcelable(SearchManager.APP_DATA, mAppSearchData);
        }

        Intent voiceIntent = new Intent(baseIntent);

        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());

        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras);

        return voiceIntent;
    }

    public boolean seslSetSviEnabled(boolean enabled) {
        if (SeslBuildReflector.SeslVersionReflector.getField_SEM_PLATFORM_INT()
                < SEP_VERSION_SUPPORTING_SVI_SEARCH_QUERY) {
            Log.w(LOG_TAG, "seslSetSviEnabled: SEP Version is not supported");
            return false;
        }

        mUseSVI = enabled;

        if (enabled) {
            try {
                PackageManager packageManager = getContext().getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(SVI_PACKAGE, 0);

                final long version = packageInfo != null ? PackageInfoCompat.getLongVersionCode(packageInfo) : -1L;
                if (version < SVI_VERSION_SUPPORTING_SEARCH_QUERY) {
                    Log.w(LOG_TAG, "seslSetSviEnabled: not supported SVI version");
                    mUseSVI = false;
                }

                if (!isSystemLocaleSupported()) {
                    Log.w(LOG_TAG, "seslSetSviEnabled: not supported system locale");
                    mUseSVI = false;
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "Exception " + e);
                mUseSVI = false;
            }
        }

        return mUseSVI;
    }

    public boolean seslIsSviEnabled() {
        return mUseSVI;
    }

    private boolean isSystemLocaleSupported() {
        int isLocalSupported = 0;

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Uri.parse("content://" + AUTHORITY_SVI_APP + "/" + KEY_SVI_APP_LOCALE),
                    null, null, null, null);
        } catch (Exception e) {
            Log.w(LOG_TAG, "isSystemLocaleSupported: exception!!" + e);
        }

        if (cursor == null) {
            if (cursor != null) {
                cursor.close();
            }
            return false;
        }

        while (cursor.moveToNext()) {
            isLocalSupported = cursor.getInt(cursor.getColumnIndex(KEY_SVI_APP_LOCALE));
        }

        if (cursor != null) {
            cursor.close();
        }

        return isLocalSupported == 1;
    }

    public void seslSetNotCallShowSoftInput(boolean notCall) {
        mSearchSrcTextView.setNotCallShowSoftInput(notCall);
    }

    public void seslSetOnPrivateImeCommandListener(@Nullable OnPrivateImeCommandListener listener) {
        mSearchSrcTextView.seslSetOnPrivateImeCommandListener(listener);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        seslCheckMaxFont();
    }

    private void seslCheckMaxFont() {
        final float currentFontScale = getContext().getResources().getConfiguration().fontScale;
        final int searchSrcTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.sesl_search_view_search_text_size);

        if (currentFontScale > 1.3f) {
            mSearchSrcTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (searchSrcTextSize / currentFontScale) * 1.3f);
        } else {
            mSearchSrcTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, searchSrcTextSize);
        }
    }
}
