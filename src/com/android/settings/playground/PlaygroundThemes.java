/*
 * Copyright (C) 2012 CyanogenMod
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

package com.android.settings.playground;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.content.res.Resources;
import android.text.Spannable;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.CMDProcessor;
import com.android.settings.Helpers;

public class PlaygroundThemes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ThemesSettings";

    private static final String KEY_LCD_DENSITY = "lcd_density";

    private static final String KEY_DUAL_PANE = "dual_pane";

    private static final String KEY_STATUSBAR_OVERRIDE = "statusbar_override";
    private static final String STATUSBAR_OVERRIDE_PROP = "ro.config.statusbar";
    private static final String STATUSBAR_OVERRIDE_DEFAULT = "1";

    private CheckBoxPreference mDualPane;
    private CheckBoxPreference mStatusbarOverride;

    private final Configuration mCurConfig = new Configuration();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.playground_themes);

        PreferenceScreen prefSet = getPreferenceScreen();

        mDualPane = (CheckBoxPreference) prefSet.findPreference(KEY_DUAL_PANE);
        mStatusbarOverride = (CheckBoxPreference) prefSet.findPreference(KEY_STATUSBAR_OVERRIDE);

        mDualPane.setPersistent(true);
        mDualPane.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.DUAL_PANE_SETTINGS, 0) == 1);

        String statusbarOverride = SystemProperties.get(STATUSBAR_OVERRIDE_PROP, STATUSBAR_OVERRIDE_DEFAULT);
        mStatusbarOverride.setChecked("1".equals(statusbarOverride));
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDualPane) {
            Settings.System.putInt(getContentResolver(), Settings.System.DUAL_PANE_SETTINGS, mDualPane.isChecked() ? 1 : 0);
        } else if (preference == mStatusbarOverride) {
            String statusbarOverrideCheck = mStatusbarOverride.isChecked() ? "1" : "0";
            SystemProperties.set(STATUSBAR_OVERRIDE_PROP, statusbarOverrideCheck);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        return true;
    }
}