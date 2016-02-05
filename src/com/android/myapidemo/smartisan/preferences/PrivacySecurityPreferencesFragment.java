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
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.preferences.SwitchExPreference.SwitchPreferenceChangeListener;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class PrivacySecurityPreferencesFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, SwitchPreferenceChangeListener {

    private int mPaddingTop = 21;
    private int mPaddingBottom = 21;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.privacy_security_preferences);

        Preference e = findPreference(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY);
        e.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        Preference e = findPreference(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY);
        e.setOnPreferenceChangeListener(this);

        initSwitchExPreference(PreferenceKeys.PREF_SHOW_SECURITY_WARNINGS,
                SwitchExPreference.BACKGROUND_BOTTOM);
        initSwitchExPreference(PreferenceKeys.PREF_ACCEPT_COOKIES,
                SwitchExPreference.BACKGROUND_TOP);
        initSwitchExPreference(PreferenceKeys.PREF_SAVE_FORMDATA,
                SwitchExPreference.BACKGROUND_TOP);
        initSwitchExPreference(PreferenceKeys.PREF_ENABLE_GEOLOCATION,
                SwitchExPreference.BACKGROUND_TOP);
        initSwitchExPreference(PreferenceKeys.PREF_REMEMBER_PASSWORDS,
                SwitchExPreference.BACKGROUND_TOP);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0, mPaddingTop, 0, mPaddingBottom);
            lv.setSelector(R.color.transparent);
            lv.setDivider(null);
        }
        return v;
    }

    private void initSwitchExPreference(String key, int type) {
        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPrefs = prefScreen.getSharedPreferences();
        SwitchExPreference prefetch = (SwitchExPreference) prefScreen.findPreference(key);
        boolean currentState = sharedPrefs.getBoolean(key, false);
        prefetch.setBackgroundType(type);
        prefetch.setDefaultValue(currentState);
        prefetch.setCheckState(currentState);
        prefetch.setCheckedChangeListener(this);
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
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (pref.getKey().equals(PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY)
                && ((Boolean) objValue).booleanValue() == true) {
            // Need to tell the browser to remove the parent/child relationship
            // between tabs
            getActivity().setResult(Activity.RESULT_OK, (new Intent()).putExtra(Intent.EXTRA_TEXT,
                    pref.getKey()));
            return true;
        }
        return false;
    }

    @Override
    public void onCheckStateChange(String key) {
        updateCheckState(key);
    }
}
