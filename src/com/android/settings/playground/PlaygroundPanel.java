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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
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

/**
 * Playground Panel Settings
 */
public class PlaygroundPanel extends SettingsPreferenceFragment
implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "Playground";

    private static final String PROXIMITY_DISABLE_PREF = "proximity_disable";
    private static final String PROXIMITY_DISABLE_PROP = "gsm.proximity.enable";
    private static final String PROXIMITY_DISABLE_DEFAULT = "true";
    private static final String TILED_RENDERING_PREF = "tiled_rendering";
    private static final String TILED_RENDERING_PROP = "debug.enabletr";
    private static final String TILED_RENDERING_DEFAULT = "false";
    private static final String KEY_COMPATIBILITY_MODE = "compatibility_mode";
    private static final String KEY_SHUTTER_SOUND = "shutter_sound";
    private static final String BOOT_SOUND_PREF = "boot_sound";
    private static final String BOOT_SOUND_PROP = "ro.config.play.bootsound";
    private static final String BOOT_SOUND_DEFAULT = "1";
    private static final String INSTALL_LOCATION = "install_location";
    private static final String COMP_TYPE_PREF = "composition_type";
    private static final String COMP_TYPE_PROP = "debug.composition.type";
    private static final String COMP_TYPE_DEFAULT = "gpu";
    private static final String COMP_BYPASS_PREF = "composition_bypass";
    private static final String COMP_BYPASS_PROP = "ro.sf.compbypass.enable";
    private static final String COMP_BYPASS_DEFAULT = "1";
    private static final String KEY_EXTERNAL_CACHE = "external_cache";
    private static final String KEY_NAVIGATION_BAR = "forced_navi_bar";
    private static final String PREF_CARRIER_TEXT = "custom_carrier_text";
    private static final String MODIFY_CARRIER_TEXT = "notification_drawer_carrier_text";
    private static final String FAST_CHARGE_PREF = "force_fast_charge";
    private static final String KEY_LCD_DENSITY = "lcd_density";
    private static final String KEY_DUAL_PANE = "dual_pane";

    private CheckBoxPreference mTiledRenderingPref;
    private CheckBoxPreference mNotificationCarrierText;
    private CheckBoxPreference mDisableProximityPref;
    private CheckBoxPreference mCompositionBypass;
    private CheckBoxPreference mNavigationBar;
    private CheckBoxPreference mBootSoundPref;
    private CheckBoxPreference mCompatibilityMode;
    private CheckBoxPreference mShutterSound;
    private CheckBoxPreference mFastCharge;
    private CheckBoxPreference mDualPane;
    private CheckBoxPreference mStatusbarOverride;
    private ListPreference mCompositionType;
    private ListPreference mInstallLocation;
    private Preference mExternalCache;
    private Preference mCarrier;

    String mCarrierText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {

            addPreferencesFromResource(R.xml.playground_panel);

            PreferenceScreen prefSet = getPreferenceScreen();

            mDisableProximityPref = (CheckBoxPreference) prefSet.findPreference(PROXIMITY_DISABLE_PREF);
            mTiledRenderingPref = (CheckBoxPreference) prefSet.findPreference(TILED_RENDERING_PREF);
            mCompositionBypass = (CheckBoxPreference) prefSet.findPreference(COMP_BYPASS_PREF);
            mNavigationBar = (CheckBoxPreference) prefSet.findPreference(KEY_NAVIGATION_BAR);
            mCompatibilityMode = (CheckBoxPreference) prefSet.findPreference(KEY_COMPATIBILITY_MODE);
            mNotificationCarrierText = (CheckBoxPreference) prefSet.findPreference(MODIFY_CARRIER_TEXT);
            mBootSoundPref = (CheckBoxPreference) prefSet.findPreference(BOOT_SOUND_PREF);
            mShutterSound = (CheckBoxPreference) prefSet.findPreference(KEY_SHUTTER_SOUND);
            mCompositionType = (ListPreference) prefSet.findPreference(COMP_TYPE_PREF);
            mInstallLocation = (ListPreference) prefSet.findPreference(INSTALL_LOCATION);
            mExternalCache = prefSet.findPreference(KEY_EXTERNAL_CACHE);
            mCarrier = (Preference) prefSet.findPreference(PREF_CARRIER_TEXT);
            mFastCharge = (CheckBoxPreference) prefSet.findPreference(FAST_CHARGE_PREF);
            mDualPane = (CheckBoxPreference) prefSet.findPreference(KEY_DUAL_PANE);

            String disableProximity = SystemProperties.get(PROXIMITY_DISABLE_PROP, PROXIMITY_DISABLE_DEFAULT);
            mDisableProximityPref.setChecked("true".equals(disableProximity));

            String tiledRendering = SystemProperties.get(TILED_RENDERING_PROP, TILED_RENDERING_DEFAULT);
            mTiledRenderingPref.setChecked("true".equals(tiledRendering));

            String compositionBypass = SystemProperties.get(COMP_BYPASS_PROP, COMP_BYPASS_DEFAULT);
            mDisableProximityPref.setChecked("1".equals(compositionBypass));

            String bootSound = SystemProperties.get(BOOT_SOUND_PROP, BOOT_SOUND_DEFAULT);
            mBootSoundPref.setChecked("1".equals(bootSound));

            mCompositionType.setOnPreferenceChangeListener(this);
            mCompositionType.setValue(SystemProperties.get(COMP_TYPE_PROP, SystemProperties.get(COMP_TYPE_PROP, COMP_TYPE_DEFAULT)));
            mCompositionType.setOnPreferenceChangeListener(this);

            //String currentInstall = new CMDProcessor().su.runWaitFor("pm getInstallLocation").stderr();
            String currentInstall = "0";
            mInstallLocation.setOnPreferenceChangeListener(this);
            mInstallLocation.setValue(currentInstall);
            mInstallLocation.setOnPreferenceChangeListener(this);

            if(getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar) == true) {
                getPreferenceScreen().removePreference(mNavigationBar);
            } else {
                mNavigationBar.setPersistent(true);
                mNavigationBar.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.NAVIGATION_BAR_VISIBLE, 0) == 1);
            }

            mCompatibilityMode.setPersistent(true);
            mCompatibilityMode.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.COMPATIBILITY_MODE, 1) != 0);

            mShutterSound.setPersistent(true);
            mShutterSound.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.SHUTTER_SOUND, 1) != 0);

            mNotificationCarrierText.setPersistent(true);
            mNotificationCarrierText.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.MODIFY_CARRIER_TEXT, 0) == 1));

            mDualPane.setPersistent(true);
            mDualPane.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.DUAL_PANE_SETTINGS, 0) == 1);

            mCarrier.setPersistent(true);

            updateCarrierText();

        }
    }
    
    private void exportCache(Preference preference) {
        if (!isSdPresent()) {
            preference.setSummary("Sdcard unavailable");
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
            preference.setSummary("Google cache set to external");
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

    private void enableFastCharge(int configuredSetting) {
		new CMDProcessor().su.runWaitFor("echo " + configuredSetting + " > /sys/kernel/fast_charge/force_fast_charge");
	}

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        Helpers.getMount("rw");
        if (preference == mTiledRenderingPref) {
            SystemProperties.set(COMP_BYPASS_PROP, mCompositionBypass.isChecked() ? "true" : "false");
        } else if (preference == mDisableProximityPref) {
            SystemProperties.set(PROXIMITY_DISABLE_PROP, mDisableProximityPref.isChecked() ? "true" : "false");
        } else if (preference == mCompositionBypass) {
            SystemProperties.set(COMP_BYPASS_PROP, mCompositionBypass.isChecked() ? "1" : "0");
        } else if (preference == mNavigationBar) {
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_VISIBLE, mNavigationBar.isChecked() ? 1 : 0);
        } else if (preference == mBootSoundPref) {
            SystemProperties.set(BOOT_SOUND_PROP, mBootSoundPref.isChecked() ? "1" : "0");
        } else if (preference == mCompatibilityMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.COMPATIBILITY_MODE, mCompatibilityMode.isChecked() ? 1 : 0);
        } else if (preference == mShutterSound) {
            Settings.System.putInt(getContentResolver(), Settings.System.SHUTTER_SOUND, mShutterSound.isChecked() ? 1 : 0);
        } else if (preference == mExternalCache) {
            exportCache(preference);
        } else if (preference == mNotificationCarrierText) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.MODIFY_CARRIER_TEXT, mNotificationCarrierText.isChecked() ? 1 : 0);
        } else if (preference == mFastCharge) {
            int fastCharge = mFastCharge.isChecked() ? 1 : 0;
            Settings.System.putInt(getContentResolver(), Settings.System.FAST_CHARGE, fastCharge);
        } else if (preference == mDualPane) {
            Settings.System.putInt(getContentResolver(), Settings.System.DUAL_PANE_SETTINGS, mDualPane.isChecked() ? 1 : 0);
        } else if (preference == mCarrier) {
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
        Helpers.getMount("ro");
        return true;
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Helpers.getMount("rw");
        if (preference == mCompositionType) {
            if (newValue != null) {
                SystemProperties.set(COMP_TYPE_PROP, (String)newValue);
                mCompositionType.setSummary("Set "+newValue+" composition (Requires reboot)");
                return true;
            }
        }
        if (preference == mInstallLocation) {
            if (newValue != null) {
                new CMDProcessor().su.runWaitFor("pm setInstallLocation "+newValue);
                String summary = "default location";
                if (newValue.equals("0")) {
                    summary = "app selection";
                } else if (newValue.equals("1")) {
                    summary = "internal only";
                } else if (newValue.equals("2")) {
                    summary = "external only";
                }
                mInstallLocation.setSummary("Install location is "+summary);
                return true;
            }
        }
        Helpers.getMount("ro");
        return false;
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
    
}
