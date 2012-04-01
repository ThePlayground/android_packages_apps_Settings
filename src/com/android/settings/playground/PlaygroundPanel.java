/*
 * Copyright (C) 2012 Twisted Playground
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.*;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.Spannable;
import android.util.Log;
import android.view.*;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.CMDProcessor;
import com.android.settings.cyanogenmod.Helpers;

/**
 * Performance Settings
 */
public class PlaygroundPanel extends SettingsPreferenceFragment
implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "Playground";
    
    private static final String PROXIMITY_DISABLE_PREF = "proximity_disable";
    private static final String PROXIMITY_DISABLE_PROP = "gsm.proximity.enable";
    private static final String PROXIMITY_DISABLE_DEFAULT = "1";
    
    private static final String TILED_RENDERING_PREF = "tiled_rendering";
    private static final String TILED_RENDERING_PROP = "debug.enabletr";
    private static final String TILED_RENDERING_DEFAULT = "false";
    
    private static final String KEY_COMPATIBILITY_MODE = "compatibility_mode";

    private static final String KEY_EXTERNAL_CACHE = "external_cache";
    
    private static final String KEY_DUAL_PANE = "dual_pane";

    private static final String PREF_CARRIER_TEXT = "custom_carrier_text";
    private static final String MODIFY_CARRIER_TEXT = "notification_drawer_carrier_text";
    
    private CheckBoxPreference mTiledRenderingPref;
    private CheckBoxPreference mNotificationCarrierText;
    private CheckBoxPreference mDisableProximityPref;
    private CheckBoxPreference mCompatibilityMode;
    private CheckBoxPreference mDualPane;
    private Preference mExternalCache;
    private Preference mCarrier;

    String mCarrierText = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getPreferenceManager() != null) {
            
            addPreferencesFromResource(R.xml.playground_panel);
            
            PreferenceScreen prefSet = getPreferenceScreen();
            
            mDisableProximityPref = (CheckBoxPreference) findPreference(PROXIMITY_DISABLE_PREF);
            String disableProximity = SystemProperties.get(PROXIMITY_DISABLE_PROP, PROXIMITY_DISABLE_DEFAULT);
            mDisableProximityPref.setChecked("1".equals(disableProximity));
            
            mTiledRenderingPref = (CheckBoxPreference) prefSet.findPreference(TILED_RENDERING_PREF);
            String tiledRendering = SystemProperties.get(TILED_RENDERING_PROP, TILED_RENDERING_DEFAULT);
            if (tiledRendering != null) {
                mTiledRenderingPref.setChecked("true".equals(tiledRendering));
            } else {
                prefSet.removePreference(mTiledRenderingPref);
            }
            
            mCompatibilityMode = (CheckBoxPreference) findPreference(KEY_COMPATIBILITY_MODE);
            mCompatibilityMode.setPersistent(true);
            mCompatibilityMode.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.COMPATIBILITY_MODE, 1) != 0);
            
            mDualPane = (CheckBoxPreference) findPreference(KEY_DUAL_PANE);
            if (mDualPane != null) {
                mDualPane.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.DUAL_PANE_SETTINGS, 0) == 1);
            }

            mExternalCache = findPreference(KEY_EXTERNAL_CACHE);

            mNotificationCarrierText = (CheckBoxPreference) prefSet.findPreference(MODIFY_CARRIER_TEXT);
            
            mCarrier = (Preference) prefSet.findPreference(PREF_CARRIER_TEXT);
            
            mNotificationCarrierText.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.MODIFY_CARRIER_TEXT, 0) == 1));
            
            updateCarrierText();
            
        }
    }

    private void exportCache(Preference preference) {
        Helpers.getMount("rw");
        if (!isSdPresent()) {
            preference.setSummary("Sdcard Unavailable");
        } else {
            String extCache = "/cache/files/maps/";
            File extDir = new File(Environment.getExternalStorageDirectory()
                                      + extCache);
            if (!extDir.mkdirs()) {
                extDir.mkdirs();
            }
            String webCache = "/cache/webviewCache/";
            File extWeb = new File(Environment.getExternalStorageDirectory()
                                   + webCache);
            if (!extWeb.mkdirs()) {
                extWeb.mkdirs();
            }
            String streetCache = "/cache/streetCache/";
            File extStreet = new File(Environment.getExternalStorageDirectory()
                                   + streetCache);
            if (!extStreet.mkdirs()) {
                extStreet.mkdirs();
            }
            String marketCache = "/cache/marketCache/";
            File extMarket = new File(Environment.getExternalStorageDirectory()
                                   + marketCache);
            if (!extMarket.mkdirs()) {
                extMarket.mkdirs();
            }

            List<String> rmCache = new ArrayList<String>();
            List<String> lnCache = new ArrayList<String>();
            rmCache.add("busybox rm -rf /data/data/com.android.browser/cache/webviewCache");
            lnCache.add("busybox ln -s /sdcard/cache/webviewCache /data/data/com.android.browser/cache/webviewCache");
            rmCache.add("busybox rm -rf /data/data/com.google.android.gm/cache/webviewCache");
            lnCache.add("busybox ln -s /sdcard/cache/webviewCache /data/data/com.google.android.gm/cache/webviewCache");
            rmCache.add("busybox rm -rf /data/data/com.google.android.voicesearch/cache/webviewCache");
            lnCache.add("busybox ln -s /sdcard/cache/webviewCache /data/data/com.google.android.voicesearch/cache/webviewCache");
            rmCache.add("busybox rm -rf /data/data/com.google.android.apps.maps/files");
            lnCache.add("busybox ln -s /sdcard/cache/files/maps /data/data/com.google.android.apps.maps/files");
            rmCache.add("busybox rm -rf /data/data/com.google.android.street/cache");
            lnCache.add("busybox ln -s /sdcard/cache/streetCache/data/data/com.google.android.street/cache");
            rmCache.add("busybox rm -rf /data/data/com.android.vending/cache");
            lnCache.add("busybox ln -s /sdcard/cache/marketCache /data/data/com.android.vending/cache");
            for (int i = 0; i < rmCache.size(); i++) {
                new CMDProcessor().su.runWaitFor(rmCache.get(i));
                new CMDProcessor().su.runWaitFor(lnCache.get(i));
            }
            preference.setSummary("External Cache Active");
            Helpers.getMount("ro");
        }
    }

    private void updateCarrierText() {
        mCarrierText = Settings.System.getString(getContentResolver(), Settings.System.CUSTOM_CARRIER_TEXT);
        if (mCarrierText == null || mCarrierText == "") {
            mCarrier.setSummary(" ");
        } else {
            mCarrier.setSummary(mCarrierText);
        }
    }

    public static boolean isSdPresent() {
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mTiledRenderingPref) {
            SystemProperties.set(TILED_RENDERING_PROP,
                                 mTiledRenderingPref.isChecked() ? "true" : "false");
        } else if (preference == mDisableProximityPref) {
            SystemProperties.set(PROXIMITY_DISABLE_PROP,
                                 mDisableProximityPref.isChecked() ? "1" : "0");
        } else if (preference == mCompatibilityMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.COMPATIBILITY_MODE, mCompatibilityMode.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDualPane) {
            Settings.System.putInt(getContentResolver(), Settings.System.DUAL_PANE_SETTINGS, mDualPane.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mExternalCache) {
            exportCache(preference);
            return true;
        } else if (preference == mNotificationCarrierText) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.MODIFY_CARRIER_TEXT, mNotificationCarrierText.isChecked() ? 1 : 0);
            return true;
        }  else if (preference == mCarrier) {
            AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
            ad.setTitle(R.string.carrier_text_title);
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
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        
        return true;
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Stub for future preferences
        return false;
    }
    
}
