/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.myapidemo.smartisan.preferences;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.BrowserActivity;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.preferences.SwitchExPreference.SwitchPreferenceChangeListener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebStorage;
import android.widget.ListView;

import java.util.Map;
import java.util.Set;


public class AdvancedPreferencesFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, SwitchPreferenceChangeListener {

    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences mSharedPrefs;
    private int mPaddingTop = 21;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.advanced_preferences);
        Preference e = findPreference(PreferenceKeys.PREF_DEFAULT_ZOOM);
        e.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e);

        e = findPreference(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING);
        e.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e);

        e = findPreference(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES);
        e.setOnPreferenceChangeListener(this);

    }

    void updateListPreferenceSummary(ListPreference e) {
        if (e.getKey().equals(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING)) {
            e.setSummary(e.getValue());
        } else {
            e.setSummary(e.getEntry());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            lv.setPadding(0, mPaddingTop, 0, 0);
            lv.setSelector(R.color.transparent);
            lv.setDivider(null);
        }
        return v;
    }

    /*
     * We need to set the PreferenceScreen state in onResume(), as the number of
     * origins with active features (WebStorage, Geolocation etc) could have
     * changed after calling the WebsiteSettingsActivity.
     */
    @Override
    public void onResume() {
        super.onResume();
        final PreferenceScreen websiteSettings = (PreferenceScreen) findPreference(
                PreferenceKeys.PREF_WEBSITE_SETTINGS);
        if (websiteSettings != null) {
            websiteSettings.setEnabled(false);
            WebStorage.getInstance().getOrigins(new ValueCallback<Map>() {
                @Override
                public void onReceiveValue(Map webStorageOrigins) {
                    if ((webStorageOrigins != null) && !webStorageOrigins.isEmpty()) {
                        websiteSettings.setEnabled(true);
                    }
                }
            });
            GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() {
                @Override
                public void onReceiveValue(Set<String> geolocationOrigins) {
                    if ((geolocationOrigins != null) && !geolocationOrigins.isEmpty()) {
                        websiteSettings.setEnabled(true);
                    }
                }
            });
        }
        mPreferenceScreen = getPreferenceScreen();
        mSharedPrefs = mPreferenceScreen.getSharedPreferences();

        initSwitchExPreference(PreferenceKeys.PREF_ENABLE_JAVASCRIPT,
                SwitchExPreference.BACKGROUND_SINGLE);
        initSwitchExPreference(PreferenceKeys.PREF_BLOCK_POPUP_WINDOWS,
                SwitchExPreference.BACKGROUND_MIDDLE);
    }

    public void initSwitchExPreference(String key, int type) {
        SwitchExPreference prefetch = (SwitchExPreference) mPreferenceScreen.findPreference(key);
        boolean currentState = mSharedPrefs.getBoolean(key, false);
        prefetch.setBackgroundType(type);
        prefetch.setDefaultValue(currentState);
        prefetch.setCheckState(currentState);
        prefetch.setCheckedChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            // We aren't attached, so don't accept preferences changes from the
            // invisible UI.
            Log.w("PageContentPreferencesFragment",
                    "onPreferenceChange called from detached fragment!");
            return false;
        }

        if (pref.getKey().equals(PreferenceKeys.PREF_DEFAULT_ZOOM)) {
            pref.setSummary(getVisualDefaultZoomName((String) objValue));
            return true;
        } else if (pref.getKey().equals(PreferenceKeys.PREF_DEFAULT_TEXT_ENCODING)) {
            pref.setSummary((String) objValue);
            return true;
        } else if (pref.getKey().equals(PreferenceKeys.PREF_RESET_DEFAULT_PREFERENCES)) {
            Boolean value = (Boolean) objValue;
            if (value.booleanValue() == true) {
                startActivity(new Intent(BrowserActivity.ACTION_RESTART, null,
                        getActivity(), BrowserActivity.class));
                return true;
            }
        }
        return false;
    }

    private CharSequence getVisualDefaultZoomName(String enumName) {
        Resources res = getActivity().getResources();
        CharSequence[] visualNames = res.getTextArray(R.array.pref_default_zoom_choices);
        CharSequence[] enumNames = res.getTextArray(R.array.pref_default_zoom_values);

        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }

        int length = enumNames.length;
        for (int i = 0; i < length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }

    private void updateCheckState(String key) {
        PreferenceScreen prefScreen = getPreferenceScreen();
        SwitchExPreference prefetch = (SwitchExPreference) prefScreen.findPreference(
                key);
        Editor ed = BrowserSettings.getInstance().getPreferences().edit();
        ed.putBoolean(key, prefetch.getCheckState());
        ed.apply();
    }

    @Override
    public void onCheckStateChange(String key) {
        updateCheckState(key);
    }
}
