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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.clipboard.ClipboardMonitor;
import com.android.myapidemo.smartisan.preferences.SwitchExPreference.SwitchPreferenceChangeListener;

public class OperationPreferencesFragment extends PreferenceFragment implements
        SwitchPreferenceChangeListener {

    static final String TAG = "OperationPreferencesFragment";

    private int mPaddingTop = 21;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.operation_preferences);

    }

    @Override
    public void onResume() {
        super.onResume();

        initSwitchExPreference(PreferenceKeys.PREF_MONITOR_CLICKBOARD, SwitchExPreference.BACKGROUND_SINGLE);
        initSwitchExPreference(PreferenceKeys.PREF_SHAKE_RESTORE, SwitchExPreference.BACKGROUND_SINGLE);
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

    private void initSwitchExPreference(String key, int type) {
        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPrefs = prefScreen.getSharedPreferences();
        SwitchExPreference prefetch = (SwitchExPreference) prefScreen.findPreference(key);
        boolean currentState = sharedPrefs.getBoolean(key, true);
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
        if (PreferenceKeys.PREF_MONITOR_CLICKBOARD.equals(key)) {
            if (prefetch.getCheckState())
                getActivity().startService(new Intent(getActivity(), ClipboardMonitor.class));
            else
                getActivity().stopService(new Intent(getActivity(), ClipboardMonitor.class));
        }
    }

    @Override
    public void onCheckStateChange(String key) {
        updateCheckState(key);
    }
}
