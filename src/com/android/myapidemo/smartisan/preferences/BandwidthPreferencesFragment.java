/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.preferences.SwitchExPreference.SwitchPreferenceChangeListener;

public class BandwidthPreferencesFragment extends PreferenceFragment implements
        SwitchPreferenceChangeListener {

    static final String TAG = "BandwidthPreferencesFragment";

    private int mPaddingTop = 21;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.bandwidth_preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceScreen prefScreen = getPreferenceScreen();

        SharedPreferences sharedPrefs = prefScreen.getSharedPreferences();

        if (sharedPrefs.contains(PreferenceKeys.PREF_LOAD_IMAGES)) {
            SwitchExPreference prefetch = (SwitchExPreference) prefScreen.findPreference(
                    PreferenceKeys.PREF_LOAD_IMAGES);
            boolean currentState = sharedPrefs.getBoolean(PreferenceKeys.PREF_LOAD_IMAGES, false);
            prefetch.setBackgroundType(SwitchExPreference.BACKGROUND_TOP);
            prefetch.setDefaultValue(currentState);
            prefetch.setCheckState(currentState);
            prefetch.setCheckedChangeListener(this);
        }

        if (!sharedPrefs.contains(PreferenceKeys.PREF_DATA_PRELOAD)) {
            // set default value for preload setting
            ListPreference preload = (ListPreference) prefScreen.findPreference(
                    PreferenceKeys.PREF_DATA_PRELOAD);
            if (preload != null) {
                preload.setValue(BrowserSettings.getInstance().getDefaultPreloadSetting());
            }
        }
        if (!sharedPrefs.contains(PreferenceKeys.PREF_LINK_PREFETCH)) {
            // set default value for link prefetch setting
            ListPreference prefetch = (ListPreference) prefScreen.findPreference(
                    PreferenceKeys.PREF_LINK_PREFETCH);
            if (prefetch != null) {
                prefetch.setValue(BrowserSettings.getInstance().getDefaultLinkPrefetchSetting());
            }
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
