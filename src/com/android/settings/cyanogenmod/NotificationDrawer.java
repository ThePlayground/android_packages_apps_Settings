/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Spannable;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationDrawer extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String PREF_CARRIER_TEXT = "custom_carrier_text";
    private static final String MODIFY_CARRIER_TEXT = "notification_drawer_carrier_text";

    private CheckBoxPreference mNotificationCarrierText;

    private Preference mCarrier;

    String mCarrierText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_drawer);

        PreferenceScreen prefSet = getPreferenceScreen();

        mNotificationCarrierText = (CheckBoxPreference) prefSet.findPreference(MODIFY_CARRIER_TEXT);

        mCarrier = (Preference) prefSet.findPreference(PREF_CARRIER_TEXT);
        
        mNotificationCarrierText.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.MODIFY_CARRIER_TEXT, 0) == 1));
        
        updateCarrierText();
    }

    private void updateCarrierText() {
        mCarrierText = Settings.System.getString(getContentResolver(), Settings.System.CUSTOM_CARRIER_TEXT);
        if (mCarrierText == null || mCarrierText == "") {
            mCarrier.setSummary(" ");
        } else {
            mCarrier.setSummary(mCarrierText);
        }
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	// Stub for future preferences
    	return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mNotificationCarrierText) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.MODIFY_CARRIER_TEXT, mNotificationCarrierText.isChecked() ? 1 : 0);
            return true;
        }  else if (preference == mCarrier) {
            AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
            ad.setTitle(R.string.carrier_text);
            ad.setMessage(R.string.carrier_text_message);
            final EditText text = new EditText(getActivity());
            ad.setView(text);
            ad.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) text.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(), Settings.System.CUSTOM_CARRIER_TEXT, value);
                    updateCarrierText();
                }
            });
            ad.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton){
                    mNotificationCarrierText.setChecked(false);
                }
            });
            ad.show();
        }
        return false;
    }
}
